package com.aepm.logic.attendance;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;
import com.aepm.util.SessionManager;
import org.json.JSONObject;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

public final class AttendanceKpiService {

    public double calculateScore(
            String employeeId,
            LocalDate start,
            LocalDate end
    ) throws Exception {

        String token = SessionManager.getAccessToken();

        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Please sign in again.");
        }

        UUID employeeUuid = UUID.fromString(employeeId);

        JSONObject body = new JSONObject();
        body.put("p_employee_id", employeeUuid.toString());
        body.put("p_start", start.toString());
        body.put("p_end", end.toString());

        SupabaseClient client = SupabaseClient.getInstance();

        HttpRequest request = client.createAuthenticatedRequest(
                AppConfig.SUPABASE_URL
                        + "/rest/v1/rpc/calculate_attendance_kpi",
                token
        )
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = client.getHttpClient().send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {

            String message = "Attendance calculation failed (HTTP "
                    + response.statusCode() + ").";

            try {
                message = new JSONObject(response.body())
                        .optString("message", message);
            } catch (org.json.JSONException ignored) {
                // Preserve the HTTP error if the response is not JSON.
            }

            throw new IllegalStateException(message);
        }

        JSONObject result = new JSONObject(response.body());
        double score = result.getDouble("attendance_rate");

        if (!Double.isFinite(score) || score < 0 || score > 100) {
            throw new IllegalStateException(
                    "Invalid attendance score returned."
            );
        }

        return score;
    }
}