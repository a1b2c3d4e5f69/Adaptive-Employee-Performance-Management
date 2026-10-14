package com.aepm.data.repository;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;
import com.aepm.model.WorkingHoursConfig;
import com.aepm.util.SessionManager;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class SystemConfigurationRepository {

    private final SupabaseClient client =
            SupabaseClient.getInstance();

    private String requireToken() {
        String token = SessionManager.getAccessToken();

        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                    "Please login before accessing configuration."
            );
        }

        return token;
    }

    public List<WorkingHoursConfig> getWorkingHours()
            throws Exception {

        String url = AppConfig.REST_URL
                + "/working_hours_tbl"
                + "?select=*"
                + "&order=working_hours_id.asc";

        HttpRequest request = client
                .createAuthenticatedRequest(url, requireToken())
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();

        HttpResponse<String> response = client
                .getHttpClient()
                .send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        System.out.println("===== SUPABASE WORKING HOURS =====");
        System.out.println("HTTP Status: " + response.statusCode());
        System.out.println("Response Body: " + response.body());

        checkResponse(response);

        JsonArray array = JsonParser
                .parseString(response.body())
                .getAsJsonArray();

        List<WorkingHoursConfig> result = new ArrayList<>();

        for (var item : array) {
            JsonObject obj = item.getAsJsonObject();

            result.add(new WorkingHoursConfig(
                    obj.get("working_hours_id").getAsLong(),
                    getString(obj, "day_of_week"),
                    getString(obj, "start_time"),
                    getString(obj, "end_time"),
                    getString(obj, "break_start_time"),
                    getString(obj, "break_end_time"),
                    obj.get("grace_period_minutes").getAsInt(),
                    obj.get("is_working_day").getAsBoolean()
            ));
        }

        return result;
    }

    public void updateWorkingHours(
            WorkingHoursConfig config
    ) throws Exception {

        String id = URLEncoder.encode(
                String.valueOf(config.getId()),
                StandardCharsets.UTF_8
        );

        String url = AppConfig.REST_URL
                + "/working_hours_tbl"
                + "?working_hours_id=eq." + id;

        JsonObject body = new JsonObject();

        body.addProperty(
                "is_working_day",
                config.isWorkingDay()
        );

        body.addProperty(
                "grace_period_minutes",
                config.getGracePeriodMinutes()
        );

        addNullable(body, "start_time", config.getStartTime());
        addNullable(body, "end_time", config.getEndTime());
        addNullable(body, "break_start_time",
                config.getBreakStartTime());
        addNullable(body, "break_end_time",
                config.getBreakEndTime());

        HttpRequest request = client
                .createAuthenticatedRequest(url, requireToken())
                .timeout(Duration.ofSeconds(20))
                .header("Prefer", "return=representation")
                .method(
                        "PATCH",
                        HttpRequest.BodyPublishers.ofString(
                                body.toString()
                        )
                )
                .build();

        HttpResponse<String> response = client
                .getHttpClient()
                .send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        System.out.println("===== SUPABASE WORKING HOURS =====");
        System.out.println("HTTP Status: " + response.statusCode());
        System.out.println("Response Body: " + response.body());

        checkResponse(response);

        JsonArray updated = JsonParser
                .parseString(response.body())
                .getAsJsonArray();

        if (updated.size() != 1) {
            throw new IllegalStateException(
                    "Configuration was not updated. "
                    + "Check administrator permissions and RLS."
            );
        }
    }

    private void addNullable(
            JsonObject body,
            String key,
            String value) {

        if (value == null || value.isBlank()) {
            body.add(key, com.google.gson.JsonNull.INSTANCE);
        } else {
            body.addProperty(key, value);
        }
    }

    private String getString(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull()
                ? obj.get(key).getAsString()
                : null;
    }

    private void checkResponse(
            HttpResponse<String> response) {

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {

            throw new IllegalStateException(
                    "Supabase error ("
                    + response.statusCode()
                    + "): "
                    + response.body()
            );
        }
    }
}