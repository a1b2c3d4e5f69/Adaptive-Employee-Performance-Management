package com.aepm.data.repository;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

public class TaskEscalationRepository {

    private static final String BASE_URL =
        AppConfig.REST_URL + "/task_escalation_tbl";

    private final SupabaseClient client =
        SupabaseClient.getInstance();

    private String encode(String value) {
        return URLEncoder.encode(
            value, StandardCharsets.UTF_8
        );
    }

    private JSONArray send(
            String token,
            String url,
            String method,
            JSONObject body) throws Exception {

        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                "Login session is missing."
            );
        }

        HttpRequest.Builder builder =
            client.createAuthenticatedRequest(url, token)
                .header("Prefer", "return=representation");

        HttpRequest request;

        if ("POST".equals(method)) {
            request = builder.POST(
                HttpRequest.BodyPublishers.ofString(
                    body.toString()
                )
            ).build();
        } else {
            request = builder.GET().build();
        }

        HttpResponse<String> response =
            client.getHttpClient().send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {
            throw new Exception(
                "Supabase error " + response.statusCode()
                    + ": " + response.body()
            );
        }

        return new JSONArray(response.body());
    }

    public boolean alreadyTriggered(
            String token,
            String taskId,
            long ruleId,
            String previousDeadline) throws Exception {

        String url = BASE_URL
            + "?select=escalation_id,previous_deadline"
            + "&task_id=eq." + encode(taskId)
            + "&rule_id=eq." + ruleId;

        JSONArray rows = send(token, url, "GET", null);

        OffsetDateTime expected =
            OffsetDateTime.parse(previousDeadline);

        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);

            if (row.isNull("previous_deadline")) {
                continue;
            }

            OffsetDateTime saved = OffsetDateTime.parse(
                row.getString("previous_deadline")
            );

            if (saved.toInstant().equals(
                    expected.toInstant())) {
                return true;
            }
        }

        return false;
    }

    public long create(
            String token,
            String taskId,
            String employeeId,
            long ruleId,
            int level,
            int missedCount,
            String previousDeadline) throws Exception {

        JSONObject body = new JSONObject()
            .put("task_id", taskId)
            .put("employee_id", employeeId)
            .put("rule_id", ruleId)
            .put("current_level", level)
            .put("missed_deadline_count", missedCount)
            .put("previous_deadline", previousDeadline)
            .put("status", "Active");

        JSONArray result = send(
            token,
            BASE_URL,
            "POST",
            body
        );

        if (result.isEmpty()) {
            throw new Exception(
                "Escalation history was not created."
            );
        }

        JSONObject createdRecord = result.getJSONObject(0);

        if (!createdRecord.has("escalation_id")
                || createdRecord.isNull("escalation_id")) {
            throw new Exception(
                "Escalation was created but escalation_id was not returned."
            );
        }

        long escalationId =
            createdRecord.getLong("escalation_id");

        System.out.println(
            "[Escalation] Created escalation_id: "
                + escalationId
        );

        return escalationId;
    }

}
