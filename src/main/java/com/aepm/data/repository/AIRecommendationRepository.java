package com.aepm.data.repository;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;
import com.google.gson.*;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

public final class AIRecommendationRepository {

    public JsonObject save(
            JsonObject snapshot,
            JsonObject recommendation,
            String modelName,
            UUID requestedBy,
            String accessToken
    ) throws Exception {

        if (accessToken == null || accessToken.isBlank()
                || requestedBy == null) {
            throw new IllegalStateException(
                    "A signed-in manager is required."
            );
        }

        UUID recommendationId = UUID.randomUUID();

        JsonObject metrics =
                snapshot.getAsJsonObject("additional_metrics");

        JsonObject body = new JsonObject();
        body.addProperty(
                "recommendation_id", recommendationId.toString()
        );
        body.add("employee_id", snapshot.get("employee_id").deepCopy());
        body.add("period_start", snapshot.get("period_start").deepCopy());
        body.add("period_end", snapshot.get("period_end").deepCopy());
        body.add("kpi_result_id", metrics.get("kpi_result_id").deepCopy());

        body.addProperty("requested_by", requestedBy.toString());
        body.addProperty("model_name", modelName);
        body.addProperty("status", "Generated");

        body.add(
                "overall_assessment",
                recommendation.get("overall_assessment").deepCopy()
        );

        body.addProperty(
                "strengths", listText(recommendation, "strengths")
        );
        body.addProperty(
                "areas_for_improvement",
                listText(recommendation, "areas_for_improvement")
        );
        body.addProperty(
                "suggested_training",
                listText(recommendation, "suggested_training")
        );
        body.addProperty(
                "suggested_interventions",
                listText(recommendation, "manager_interventions")
        );

        body.add(
                "explainable_recommendations",
                recommendation.getAsJsonArray("recommendations").deepCopy()
        );
        body.add(
                "manager_interventions",
                recommendation.getAsJsonArray("manager_interventions")
                        .deepCopy()
        );

        StringBuilder actions = new StringBuilder();
        for (JsonElement element :
                recommendation.getAsJsonArray("recommendations")) {
            if (!actions.isEmpty()) {
                actions.append("\n");
            }
            actions.append("• ").append(
                    element.getAsJsonObject()
                            .get("suggested_action").getAsString()
            );
        }
        body.addProperty("recommended_actions", actions.toString());

        // Preserve the original evidence and complete structured response.
        // A later snapshot refresh must not change this saved history.
        JsonObject evidence = new JsonObject();
        evidence.addProperty("format_version", "aepm-recommendation-v1");
        evidence.add("snapshot", snapshot.deepCopy());
        evidence.add("recommendation", recommendation.deepCopy());

        body.add("performance_evidence", evidence);
        body.add("performance_metrics", metrics.deepCopy());

        SupabaseClient client = SupabaseClient.getInstance();

        HttpRequest request = client.createAuthenticatedRequest(
                AppConfig.REST_URL
                    + "/ai_recommendation_tbl"
                    + "?select=recommendation_id,generated_at,model_name",
                accessToken
        )
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response;

        try {
            response = client.getHttpClient().send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(
                    "AI generation succeeded, but saving could not be "
                    + "confirmed. Check recommendation ID "
                    + recommendationId + " before generating again.",
                    ex
            );
        }

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {
            throw new IllegalStateException(
                    "AI generation succeeded, but saving failed (HTTP "
                    + response.statusCode()
                    + "). Check database permissions and constraints."
            );
        }

        JsonElement parsed = JsonParser.parseString(response.body());

        if (!parsed.isJsonArray()
                || parsed.getAsJsonArray().size() != 1) {
            throw new IllegalStateException(
                    "Saving returned an unexpected response. Check ID "
                    + recommendationId + " before generating again."
            );
        }

        JsonObject saved = parsed.getAsJsonArray()
                .get(0).getAsJsonObject();

        if (!recommendationId.toString().equals(
                saved.get("recommendation_id").getAsString())) {
            throw new IllegalStateException(
                    "The saved recommendation ID did not match."
            );
        }

        return saved.deepCopy();
    }

    private static String listText(JsonObject object, String key) {
        StringBuilder text = new StringBuilder();

        for (JsonElement element : object.getAsJsonArray(key)) {
            if (!text.isEmpty()) {
                text.append("\n");
            }
            text.append("• ").append(element.getAsString());
        }

        return text.toString();
    }

    public JsonArray getHistory(
            UUID employeeId,
            String accessToken
    ) throws Exception {

        if (employeeId == null
                || accessToken == null
                || accessToken.isBlank()) {
            throw new IllegalArgumentException(
                    "Employee and signed-in session are required."
            );
        }

        String url = AppConfig.REST_URL
                + "/ai_recommendation_tbl"
                + "?select=recommendation_id,employee_id,kpi_result_id,"
                + "period_start,period_end,generated_at,model_name,status,"
                + "performance_evidence"
                + "&employee_id=eq." + employeeId
                + "&status=in.(Generated,Viewed)"
                + "&order=generated_at.desc,recommendation_id.desc"
                + "&limit=50";

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
                    "Unable to load recommendation history (HTTP "
                    + response.statusCode() + ")."
            );
        }

        JsonElement parsed = JsonParser.parseString(response.body());

        if (!parsed.isJsonArray()) {
            throw new IllegalStateException(
                    "Recommendation history returned an invalid response."
            );
        }

        JsonArray rows = parsed.getAsJsonArray();

        for (JsonElement element : rows) {
            if (!element.isJsonObject()
                    || !element.getAsJsonObject().has("employee_id")
                    || !employeeId.toString().equals(
                            element.getAsJsonObject()
                                    .get("employee_id").getAsString()
                    )) {
                throw new IllegalStateException(
                        "Recommendation history contains an unexpected employee."
                );
            }
        }

        return rows;
    }

    public boolean hasRecommendation(
            UUID employeeId,
            Long kpiResultId,
            String accessToken
    ) throws Exception {

        if (employeeId == null
                || kpiResultId == null
                || kpiResultId <= 0
                || accessToken == null
                || accessToken.isBlank()) {
            throw new IllegalArgumentException(
                    "Employee, KPI result and session are required."
            );
        }

        String url = AppConfig.REST_URL
                + "/ai_recommendation_tbl"
                + "?select=recommendation_id"
                + "&employee_id=eq." + employeeId
                + "&kpi_result_id=eq." + kpiResultId
                + "&status=in.(Generated,Viewed)"
                + "&limit=1";

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
                    "Unable to check existing recommendations (HTTP "
                    + response.statusCode() + ")."
            );
        }

        JsonElement parsed = JsonParser.parseString(response.body());

        if (!parsed.isJsonArray()) {
            throw new IllegalStateException(
                    "Invalid response when checking recommendations."
            );
        }

        return !parsed.getAsJsonArray().isEmpty();
    }
}