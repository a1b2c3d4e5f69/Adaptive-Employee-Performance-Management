package com.aepm.data.repository;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;
import com.aepm.model.Holiday;
import com.aepm.util.SessionManager;
import com.google.gson.*;

import java.net.http.*;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class HolidayRepository {

    private final SupabaseClient client =
            SupabaseClient.getInstance();

    private String token() {
        String value = SessionManager.getAccessToken();

        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Please login first.");
        }

        return value;
    }

    private String baseUrl() {
        return AppConfig.REST_URL + "/holiday_tbl";
    }

    private HttpResponse<String> send(HttpRequest request)
            throws Exception {

        HttpResponse<String> response =
                client.getHttpClient().send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        System.out.println("===== HOLIDAY API =====");
        System.out.println("HTTP Status: " + response.statusCode());
        System.out.println("Response: " + response.body());

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {
            throw new IllegalStateException(
                    "Supabase error (" + response.statusCode()
                    + "): " + response.body()
            );
        }

        return response;
    }

    public List<Holiday> getAllHolidays() throws Exception {

        HttpRequest request = client
                .createAuthenticatedRequest(
                        baseUrl()
                        + "?select=*&order=start_date.asc",
                        token()
                )
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();

        JsonArray array = JsonParser.parseString(
                send(request).body()
        ).getAsJsonArray();

        List<Holiday> holidays = new ArrayList<>();

        for (JsonElement element : array) {

            JsonObject obj = element.getAsJsonObject();

            String description =
                    obj.has("description")
                    && !obj.get("description").isJsonNull()
                    ? obj.get("description").getAsString()
                    : "";

            holidays.add(new Holiday(
                    obj.get("holiday_id").getAsLong(),
                    obj.get("holiday_name").getAsString(),
                    LocalDate.parse(
                            obj.get("start_date").getAsString()
                    ),
                    LocalDate.parse(
                            obj.get("end_date").getAsString()
                    ),
                    description,
                    obj.get("is_non_working_day").getAsBoolean()
            ));
        }

        return holidays;
    }

    private JsonObject toJson(Holiday holiday) {

        JsonObject body = new JsonObject();

        body.addProperty(
                "holiday_name", holiday.getHolidayName()
        );
        body.addProperty(
                "start_date", holiday.getStartDate().toString()
        );
        body.addProperty(
                "end_date", holiday.getEndDate().toString()
        );
        body.addProperty(
                "description", holiday.getDescription()
        );
        body.addProperty(
                "is_non_working_day", holiday.isNonWorkingDay()
        );

        return body;
    }

    public void addHoliday(Holiday holiday) throws Exception {

        HttpRequest request = client
                .createAuthenticatedRequest(baseUrl(), token())
                .timeout(Duration.ofSeconds(20))
                .header("Prefer", "return=representation")
                .POST(HttpRequest.BodyPublishers.ofString(
                        toJson(holiday).toString()
                ))
                .build();

        JsonArray result = JsonParser.parseString(
                send(request).body()
        ).getAsJsonArray();

        if (result.size() != 1) {
            throw new IllegalStateException(
                    "Holiday was not added."
            );
        }
    }

    public void updateHoliday(Holiday holiday) throws Exception {

        HttpRequest request = client
                .createAuthenticatedRequest(
                        baseUrl() + "?holiday_id=eq."
                        + holiday.getHolidayId(),
                        token()
                )
                .timeout(Duration.ofSeconds(20))
                .header("Prefer", "return=representation")
                .method(
                        "PATCH",
                        HttpRequest.BodyPublishers.ofString(
                                toJson(holiday).toString()
                        )
                )
                .build();

        JsonArray result = JsonParser.parseString(
                send(request).body()
        ).getAsJsonArray();

        if (result.size() != 1) {
            throw new IllegalStateException(
                    "Holiday was not updated. Check RLS permissions."
            );
        }
    }

    public void deleteHoliday(long holidayId) throws Exception {

        HttpRequest request = client
                .createAuthenticatedRequest(
                        baseUrl() + "?holiday_id=eq." + holidayId,
                        token()
                )
                .timeout(Duration.ofSeconds(20))
                .header("Prefer", "return=representation")
                .DELETE()
                .build();

        JsonArray result = JsonParser.parseString(
                send(request).body()
        ).getAsJsonArray();

        if (result.size() != 1) {
            throw new IllegalStateException(
                    "Holiday was not deleted. Check RLS permissions."
            );
        }
    }
}