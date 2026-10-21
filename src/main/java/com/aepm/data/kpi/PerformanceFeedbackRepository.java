package com.aepm.data.kpi;

import com.aepm.config.AppConfig;

import com.aepm.data.supabase.SupabaseClient;

import com.aepm.model.PerformanceFeedback;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.nio.charset.StandardCharsets;

import java.time.OffsetDateTime;

import java.util.ArrayList;
import java.util.List;

public class PerformanceFeedbackRepository {

    private final SupabaseClient supabaseClient =
            SupabaseClient.getInstance();


    // =========================================================
    // CREATE FEEDBACK
    // =========================================================

    public PerformanceFeedback createFeedback(
            PerformanceFeedback feedback,
            String accessToken
    ) throws Exception {

        if (feedback == null) {

            throw new IllegalArgumentException(
                    "Feedback is required."
            );
        }


        JSONObject body =
                new JSONObject();


        body.put(
                "result_id",
                feedback.getResultId()
        );


        body.put(
                "employee_id",
                feedback.getEmployeeId()
        );


        body.put(
                "manager_id",
                feedback.getManagerId()
        );


        body.put(
                "feedback_text",
                feedback.getFeedbackText()
        );


        String url =
                AppConfig.REST_URL
                        + "/performance_feedback_tbl";


        HttpRequest request =
                supabaseClient
                        .createAuthenticatedRequest(
                                url,
                                accessToken
                        )
                        .header(
                                "Prefer",
                                "return=representation"
                        )
                        .POST(
                                HttpRequest.BodyPublishers
                                        .ofString(
                                                body.toString()
                                        )
                        )
                        .build();


        HttpResponse<String> response =
                send(
                        request
                );


        validateResponse(
                response,
                "Unable to submit performance feedback"
        );


        JSONArray array =
                new JSONArray(
                        response.body()
                );


        if (array.isEmpty()) {

            throw new RuntimeException(
                    "Feedback was not returned after creation."
            );
        }


        return mapFeedback(
                array.getJSONObject(0)
        );
    }


    // =========================================================
    // GET FEEDBACK BY RESULT
    // =========================================================

    public List<PerformanceFeedback> getFeedbackByResult(
            Long resultId,
            String accessToken
    ) throws Exception {

        if (resultId == null) {

            return new ArrayList<>();
        }


        String url =
                AppConfig.REST_URL
                        + "/performance_feedback_tbl"
                        + "?select=*"
                        + "&result_id=eq."
                        + resultId
                        + "&order=created_at.desc";


        HttpRequest request =
                supabaseClient
                        .createAuthenticatedRequest(
                                url,
                                accessToken
                        )
                        .GET()
                        .build();


        HttpResponse<String> response =
                send(
                        request
                );


        validateResponse(
                response,
                "Unable to load performance feedback"
        );


        return mapFeedbackList(
                response.body()
        );
    }


    // =========================================================
    // GET FEEDBACK BY EMPLOYEE
    // =========================================================

    public List<PerformanceFeedback> getFeedbackByEmployee(
            String employeeId,
            String accessToken
    ) throws Exception {

        if (
                employeeId == null
                        || employeeId.isBlank()
        ) {

            return new ArrayList<>();
        }


        String encodedEmployeeId =
                URLEncoder.encode(
                        employeeId,
                        StandardCharsets.UTF_8
                );


        String url =
                AppConfig.REST_URL
                        + "/performance_feedback_tbl"
                        + "?select=*"
                        + "&employee_id=eq."
                        + encodedEmployeeId
                        + "&order=created_at.desc";


        HttpRequest request =
                supabaseClient
                        .createAuthenticatedRequest(
                                url,
                                accessToken
                        )
                        .GET()
                        .build();


        HttpResponse<String> response =
                send(
                        request
                );


        validateResponse(
                response,
                "Unable to load employee feedback"
        );


        return mapFeedbackList(
                response.body()
        );
    }


    // =========================================================
    // GET LATEST FEEDBACK BY RESULT
    // =========================================================

    public PerformanceFeedback getLatestFeedbackByResult(
            Long resultId,
            String accessToken
    ) throws Exception {

        if (resultId == null) {
            return null;
        }


        String url =
                AppConfig.REST_URL
                        + "/performance_feedback_tbl"
                        + "?select=*"
                        + "&result_id=eq."
                        + resultId
                        + "&order=created_at.desc"
                        + "&limit=1";


        HttpRequest request =
                supabaseClient
                        .createAuthenticatedRequest(
                                url,
                                accessToken
                        )
                        .GET()
                        .build();


        HttpResponse<String> response =
                send(
                        request
                );


        validateResponse(
                response,
                "Unable to load performance feedback"
        );


        JSONArray array =
                new JSONArray(
                        response.body()
                );


        if (array.isEmpty()) {
            return null;
        }


        return mapFeedback(
                array.getJSONObject(0)
        );
    }


    // =========================================================
    // CHECK EXISTING FEEDBACK
    // =========================================================

    public boolean hasFeedbackForResult(
            Long resultId,
            String managerId,
            String accessToken
    ) throws Exception {

        if (
                resultId == null
                        || managerId == null
                        || managerId.isBlank()
        ) {

            return false;
        }


        String encodedManagerId =
                URLEncoder.encode(
                        managerId,
                        StandardCharsets.UTF_8
                );


        String url =
                AppConfig.REST_URL
                        + "/performance_feedback_tbl"
                        + "?select=feedback_id"
                        + "&result_id=eq."
                        + resultId
                        + "&manager_id=eq."
                        + encodedManagerId
                        + "&limit=1";


        HttpRequest request =
                supabaseClient
                        .createAuthenticatedRequest(
                                url,
                                accessToken
                        )
                        .GET()
                        .build();


        HttpResponse<String> response =
                send(
                        request
                );


        validateResponse(
                response,
                "Unable to check existing feedback"
        );


        JSONArray array =
                new JSONArray(
                        response.body()
                );


        return !array.isEmpty();
    }


    // =========================================================
    // HTTP
    // =========================================================

    private HttpResponse<String> send(
            HttpRequest request
    ) throws Exception {

        return supabaseClient
                .getHttpClient()
                .send(
                        request,
                        HttpResponse.BodyHandlers
                                .ofString()
                );
    }


    private void validateResponse(
            HttpResponse<String> response,
            String message
    ) {

        if (
                response.statusCode() < 200
                        || response.statusCode() >= 300
        ) {

            throw new RuntimeException(
                    message
                            + ": "
                            + response.body()
            );
        }
    }


    // =========================================================
    // MAP LIST
    // =========================================================

    private List<PerformanceFeedback> mapFeedbackList(
            String body
    ) {

        JSONArray array =
                new JSONArray(
                        body
                );


        List<PerformanceFeedback> feedbackList =
                new ArrayList<>();


        for (
                int i = 0;
                i < array.length();
                i++
        ) {

            feedbackList.add(
                    mapFeedback(
                            array.getJSONObject(i)
                    )
            );
        }


        return feedbackList;
    }


    // =========================================================
    // MAP OBJECT
    // =========================================================

    private PerformanceFeedback mapFeedback(
            JSONObject json
    ) {

        PerformanceFeedback feedback =
                new PerformanceFeedback();


        if (
                json.has(
                        "feedback_id"
                )
                        && !json.isNull(
                        "feedback_id"
                )
        ) {

            feedback.setFeedbackId(
                    json.getLong(
                            "feedback_id"
                    )
            );
        }


        if (
                json.has(
                        "result_id"
                )
                        && !json.isNull(
                        "result_id"
                )
        ) {

            feedback.setResultId(
                    json.getLong(
                            "result_id"
                    )
            );
        }


        feedback.setEmployeeId(
                json.optString(
                        "employee_id",
                        ""
                )
        );


        feedback.setManagerId(
                json.optString(
                        "manager_id",
                        ""
                )
        );


        feedback.setFeedbackText(
                json.optString(
                        "feedback_text",
                        ""
                )
        );


        if (
                json.has(
                        "created_at"
                )
                        && !json.isNull(
                        "created_at"
                )
        ) {

            feedback.setCreatedAt(
                    OffsetDateTime.parse(
                            json.getString(
                                    "created_at"
                            )
                    )
            );
        }


        return feedback;
    }

    public PerformanceFeedback updateFeedback(
                PerformanceFeedback existing,
                String newText,
                String accessToken
        ) throws Exception {

        if (existing == null || existing.getFeedbackId() == null) {
                throw new IllegalArgumentException(
                        "An existing feedback record is required."
                );
        }

        if (newText == null || newText.isBlank()) {
                throw new IllegalArgumentException(
                        "Feedback cannot be empty."
                );
        }

        if (accessToken == null || accessToken.isBlank()) {
                throw new IllegalStateException(
                        "Please sign in again."
                );
        }

        JSONObject body = new JSONObject();

        body.put("p_feedback_id", existing.getFeedbackId());

        body.put(
                "p_original_text",
                existing.getFeedbackText() == null
                        ? JSONObject.NULL
                        : existing.getFeedbackText()
        );

        body.put("p_new_text", newText.trim());

        HttpRequest request = supabaseClient
                .createAuthenticatedRequest(
                        AppConfig.REST_URL
                                + "/rpc/update_own_performance_feedback",
                        accessToken
                )
                .header("Content-Type", "application/json")
                .timeout(java.time.Duration.ofSeconds(30))
                .POST(
                        HttpRequest.BodyPublishers.ofString(
                                body.toString()
                        )
                )
                .build();

        HttpResponse<String> response = send(request);

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {

                String message =
                        "Unable to update feedback (HTTP "
                        + response.statusCode() + ").";

                try {
                message = new JSONObject(response.body())
                        .optString("message", message);
                } catch (org.json.JSONException ignored) {
                // Keep the HTTP error when the response is not JSON.
                }

                throw new IllegalStateException(message);
        }

        JSONArray rows = new JSONArray(response.body());

        if (rows.length() != 1) {
                throw new IllegalStateException(
                        "Unexpected save response. Refresh to verify "
                        + "the saved feedback before retrying."
                );
        }

        return mapFeedback(rows.getJSONObject(0));
    }
}