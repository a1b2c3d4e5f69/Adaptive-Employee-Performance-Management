package com.aepm.logic.deadline;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;

import org.json.JSONObject;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class AutomatedNotificationRuleService {

    public record ExecutionResult(
            int sent,
            int skipped,
            int failed,
            boolean busy
    ) {}

    public ExecutionResult execute(String token) throws Exception {
        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                    "An authenticated administrator session is required."
            );
        }

        var client = SupabaseClient.getInstance();

        HttpRequest request = client.createAuthenticatedRequest(
                AppConfig.REST_URL
                        + "/rpc/run_notification_reminder_rules",
                token
        )
                .timeout(Duration.ofSeconds(90))
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();

        HttpResponse<String> response = client.getHttpClient().send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {
            throw new IllegalStateException(
                    "Automated notification rules failed (HTTP "
                    + response.statusCode() + ")."
            );
        }

        JSONObject result = new JSONObject(response.body());

        int failed = result.getInt("failed");

        if (failed > 0) {
            System.err.println(
                    "[Notification Rules] Configuration or rendering errors: "
                    + result.optJSONArray("errors")
            );
        }

        return new ExecutionResult(
                result.getInt("sent"),
                result.getInt("skipped"),
                failed,
                result.optBoolean("busy", false)
        );
    }
}