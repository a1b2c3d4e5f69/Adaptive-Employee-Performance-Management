package com.aepm.data.repository;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;
import com.aepm.util.SessionManager;
import com.google.gson.*;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public final class AIHistoricalProgressRepository {

    public record HistoricalProgress(
            BigDecimal percentage,
            Instant recordedAt
    ) {
    }

    public Optional<HistoricalProgress> findLatestBefore(
            UUID taskId,
            UUID employeeId,
            Instant cutoffExclusive
    ) throws Exception {

        if (taskId == null
                || employeeId == null
                || cutoffExclusive == null) {
            throw new IllegalArgumentException(
                    "Task, employee and cutoff are required."
            );
        }

        if (cutoffExclusive.isAfter(Instant.now())) {
            throw new IllegalArgumentException(
                    "Historical cutoff cannot be in the future."
            );
        }

        String cutoff = URLEncoder.encode(
                cutoffExclusive.toString(),
                StandardCharsets.UTF_8
        );

        String url = AppConfig.REST_URL
                + "/task_progress_tbl"
                + "?select=progress_percentage,updated_at"
                + "&task_id=eq." + taskId
                + "&employee_id=eq." + employeeId
                + "&updated_at=lt." + cutoff
                + "&order=updated_at.desc,progress_id.desc"
                + "&limit=2";

        var client = SupabaseClient.getInstance();

        HttpRequest request = client
                .createAuthenticatedRequest(
                        url,
                        SessionManager.getAccessToken()
                )
                .GET()
                .build();

        HttpResponse<String> response = client
                .getHttpClient()
                .send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        if (response.statusCode() != 200) {
            throw new IllegalStateException(
                    "Cannot load historical progress (HTTP "
                    + response.statusCode() + ")."
            );
        }

        JsonElement parsed = JsonParser.parseString(
                response.body()
        );

        if (!parsed.isJsonArray()) {
            throw new IllegalStateException(
                    "Historical progress response must be an array."
            );
        }

        JsonArray rows = parsed.getAsJsonArray();

        if (rows.isEmpty()) {
            // No visible evidence; never assume zero progress.
            return Optional.empty();
        }

        HistoricalProgress latest = read(rows.get(0));

        if (!latest.recordedAt().isBefore(cutoffExclusive)) {
            throw new IllegalStateException(
                    "Progress record is outside the cutoff."
            );
        }

        if (rows.size() > 1) {
            HistoricalProgress previous = read(rows.get(1));

            if (previous.recordedAt().isAfter(latest.recordedAt())) {
                throw new IllegalStateException(
                        "Historical progress ordering is invalid."
                );
            }

            if (previous.recordedAt().equals(latest.recordedAt())) {
                throw new IllegalStateException(
                        "Multiple progress records share the latest "
                        + "timestamp; historical ordering is ambiguous."
                );
            }
        }

        return Optional.of(latest);
    }

    private static HistoricalProgress read(JsonElement element) {
        if (!element.isJsonObject()) {
            throw new IllegalStateException(
                    "Invalid historical progress record."
            );
        }

        JsonObject row = element.getAsJsonObject();
        JsonElement progress = row.get("progress_percentage");
        JsonElement timestamp = row.get("updated_at");

        if (progress == null
                || !progress.isJsonPrimitive()
                || !progress.getAsJsonPrimitive().isNumber()) {
            throw new IllegalStateException(
                    "Historical progress must be a JSON number."
            );
        }

        BigDecimal percentage;

        try {
            percentage = new BigDecimal(progress.getAsString());
        } catch (NumberFormatException ex) {
            throw new IllegalStateException(
                    "Historical progress must be finite.",
                    ex
            );
        }

        if (percentage.compareTo(BigDecimal.ZERO) < 0
                || percentage.compareTo(
                        BigDecimal.valueOf(100)) > 0) {
            throw new IllegalStateException(
                    "Historical progress must be between 0 and 100."
            );
        }

        if (timestamp == null
                || !timestamp.isJsonPrimitive()
                || !timestamp.getAsJsonPrimitive().isString()) {
            throw new IllegalStateException(
                    "Historical progress timestamp is required."
            );
        }

        Instant recordedAt;

        try {
            recordedAt = OffsetDateTime.parse(
                    timestamp.getAsString()
            ).toInstant();
        } catch (java.time.format.DateTimeParseException ex) {
            throw new IllegalStateException(
                    "Historical progress timestamp is invalid.",
                    ex
            );
        }

        return new HistoricalProgress(percentage, recordedAt);
    }
}