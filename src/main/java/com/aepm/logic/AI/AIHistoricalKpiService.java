package com.aepm.logic.ai;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;
import com.google.gson.*;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class AIHistoricalKpiService {

    private static final String[] SCORE_FIELDS = {
            "overall_kpi_score",
            "productivity_score",
            "timeliness_score",
            "completion_rate",
            "attendance_score"
    };

    public JsonObject load(
            UUID employeeId,
            LocalDate currentStart,
            boolean enabled,
            String accessToken
    ) throws Exception {

        JsonObject history = new JsonObject();
        JsonArray records = new JsonArray();

        history.addProperty("enabled", enabled);
        history.addProperty(
                "selection_rule",
                "Up to 12 non-overlapping earlier periods, selected "
                + "from the 100 most recent candidate KPI records. "
                + "For identical periods, use the latest calculation."
        );
        history.add("records", records);

        // Do not query historical KPI when the setting is disabled.
        if (!enabled) {
            history.addProperty("status", "disabled");
            history.addProperty("record_count", 0);
            return history;
        }

        if (employeeId == null || currentStart == null
                || accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException(
                    "Employee, evaluation start and session are required."
            );
        }

        String url = AppConfig.REST_URL
                + "/kpi_result_tbl"
                + "?select=result_id,employee_id,"
                + "evaluation_start_date,evaluation_end_date,"
                + "calculated_at,overall_kpi_score,"
                + "productivity_score,timeliness_score,"
                + "completion_rate,attendance_score"
                + "&employee_id=eq." + employeeId
                + "&evaluation_end_date=lt." + currentStart
                + "&order=evaluation_end_date.desc,"
                + "evaluation_start_date.desc,"
                + "calculated_at.desc.nullslast,result_id.desc"
                + "&limit=100";

        var client = SupabaseClient.getInstance();

        HttpRequest request = client.createAuthenticatedRequest(
                url, accessToken
        )
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        HttpResponse<String> response = client.getHttpClient().send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {
            throw new IllegalStateException(
                    "Unable to load historical KPI (HTTP "
                    + response.statusCode()
                    + "). AI generation was stopped."
            );
        }

        JsonElement parsed = JsonParser.parseString(response.body());

        if (!parsed.isJsonArray()) {
            throw new IllegalStateException(
                    "Historical KPI returned an invalid response."
            );
        }

        LocalDate boundary = currentStart;

        for (JsonElement element : parsed.getAsJsonArray()) {

            JsonObject row = element.getAsJsonObject();

            if (!employeeId.toString().equals(
                    row.get("employee_id").getAsString())) {
                throw new IllegalStateException(
                        "Historical KPI contains another employee."
                );
            }

            LocalDate start = LocalDate.parse(
                    row.get("evaluation_start_date").getAsString()
            );
            LocalDate end = LocalDate.parse(
                    row.get("evaluation_end_date").getAsString()
            );

            if (end.isBefore(start) || !end.isBefore(currentStart)) {
                throw new IllegalStateException(
                        "Historical KPI contains an invalid period."
                );
            }

            // Skip duplicate and overlapping periods.
            if (!end.isBefore(boundary)) {
                continue;
            }

            JsonObject item = new JsonObject();

            long resultId = row.get("result_id").getAsLong();

            if (resultId <= 0) {
                throw new IllegalStateException(
                        "Historical KPI has an invalid result ID."
                );
            }

            item.addProperty("result_id", resultId);
            item.addProperty("period_start", start.toString());
            item.addProperty("period_end", end.toString());

            String calculatedAt =
                    row.get("calculated_at").getAsString();

            // Require a valid saved calculation timestamp.
            OffsetDateTime.parse(calculatedAt);

            item.addProperty("calculated_at", calculatedAt);

            for (String field : SCORE_FIELDS) {
                JsonElement value = row.get(field);

                if (value == null
                        || !value.isJsonPrimitive()
                        || !value.getAsJsonPrimitive().isNumber()) {
                    throw new IllegalStateException(
                            "Historical KPI requires numeric " + field + "."
                    );
                }

                double score = value.getAsDouble();

                if (!Double.isFinite(score)
                        || score < 0 || score > 100) {
                    throw new IllegalStateException(
                            "Historical KPI " + field
                            + " must be between 0 and 100."
                    );
                }

                item.addProperty(field, score);
            }

            records.add(item);
            boundary = start;

            if (records.size() == 12) {
                break;
            }
        }

        history.addProperty("record_count", records.size());
        history.addProperty(
                "status",
                records.isEmpty() ? "no_data" : "available"
        );

        return history;
    }
}