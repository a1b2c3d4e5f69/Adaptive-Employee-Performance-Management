package com.aepm.data.repository;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;
import com.aepm.model.EscalationRule;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class EscalationRuleRepository {

    private static final String URL =
        AppConfig.REST_URL + "/escalation_rule_tbl";

    private final SupabaseClient client =
        SupabaseClient.getInstance();

    private String send(
            String token,
            String method,
            String endpoint,
            JSONObject payload) throws Exception {

        HttpRequest.Builder builder =
            client.createAuthenticatedRequest(endpoint, token)
                .header("Prefer", "return=representation");

        HttpRequest request;

        switch (method) {
            case "GET":
                request = builder.GET().build();
                break;
            case "DELETE":
                request = builder.DELETE().build();
                break;
            default:
                request = builder.method(
                    method,
                    HttpRequest.BodyPublishers.ofString(
                        payload.toString()
                    )
                ).build();
        }

        HttpResponse<String> response =
            client.getHttpClient().send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );

        if (response.statusCode() < 200 ||
            response.statusCode() >= 300) {

            throw new Exception(
                "Supabase error " + response.statusCode()
                + ": " + response.body()
            );
        }

        return response.body();
    }

    public List<EscalationRule> findAll(String token)
            throws Exception {

        JSONArray array = new JSONArray(send(
            token,
            "GET",
            URL + "?select=*&order=escalation_level.asc",
            null
        ));

        List<EscalationRule> rules = new ArrayList<>();

        for (int i = 0; i < array.length(); i++) {

            JSONObject row = array.getJSONObject(i);

            rules.add(new EscalationRule(
                row.getLong("rule_id"),
                row.getString("rule_name"),
                row.getInt("escalation_level"),
                row.getInt("missed_deadline_count"),
                row.getString("action_type"),
                row.getInt("extension_days"),
                row.optString("description", ""),
                row.getString("status"),
                row.optString("created_by", "")
            ));
        }

        return rules;
    }

    private JSONObject toJson(EscalationRule rule) {

        return new JSONObject()
            .put("rule_name", rule.ruleName())
            .put("escalation_level", rule.escalationLevel())
            .put("missed_deadline_count",
                rule.missedDeadlineCount())
            .put("action_type", rule.actionType())
            .put("extension_days", rule.extensionDays())
            .put("description", rule.description())
            .put("status", rule.status());
    }

    public void create(String token, EscalationRule rule)
            throws Exception {

        JSONObject body = toJson(rule);

        if (rule.createdBy() != null &&
            !rule.createdBy().isBlank()) {

            body.put("created_by", rule.createdBy());
        }

        JSONArray result = new JSONArray(send(
            token, "POST", URL, body
        ));

        if (result.isEmpty()) {
            throw new Exception("Rule creation failed.");
        }
    }

    public void update(String token, EscalationRule rule)
            throws Exception {

        JSONArray result = new JSONArray(send(
            token,
            "PATCH",
            URL + "?rule_id=eq." + rule.ruleId(),
            toJson(rule)
        ));

        if (result.isEmpty()) {
            throw new Exception("Rule not found or update denied.");
        }
    }

    public void delete(String token, long ruleId)
            throws Exception {

        JSONArray result = new JSONArray(send(
            token,
            "DELETE",
            URL + "?rule_id=eq." + ruleId,
            null
        ));

        if (result.isEmpty()) {
            throw new Exception("Rule not found or delete denied.");
        }
    }
}
