package com.aepm.data.task;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;
import com.aepm.model.TaskProgress;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TaskProgressRepository {

    private final SupabaseClient supabaseClient =
            SupabaseClient.getInstance();


    public void addProgressHistory(
            TaskProgress progress,
            String accessToken
    ) throws Exception {

        if (progress == null) {
            throw new IllegalArgumentException(
                "Progress record is required."
            );
        }

        if (progress.getTaskId() == null) {
            throw new IllegalArgumentException(
                "Task ID is required."
            );
        }

        if (progress.getEmployeeId() == null) {
            throw new IllegalArgumentException(
                "Employee ID is required."
            );
        }

        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException(
                "Your session has expired. Please sign in again."
            );
        }

        double percentage = progress.getProgressPercentage();

        if (!Double.isFinite(percentage)
                || percentage < 0
                || percentage > 100) {
            throw new IllegalArgumentException(
                "Progress must be a finite number between 0 and 100."
            );
        }
        JSONObject body =
                new JSONObject();

        body.put(
                "task_id",
                progress.getTaskId().toString()
        );

        body.put(
                "employee_id",
                progress.getEmployeeId().toString()
        );

        body.put(
                "progress_percentage",
                progress.getProgressPercentage()
        );

        body.put(
                "progress_note",
                progress.getProgressNote() == null
                        ? JSONObject.NULL
                        : progress.getProgressNote()
        );


        String url =
                AppConfig.REST_URL
                        + "/task_progress_tbl";


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
                                HttpRequest.BodyPublishers.ofString(
                                        body.toString()
                                )
                        )
                        .build();


        HttpResponse<String> response =
                supabaseClient
                        .getHttpClient()
                        .send(
                                request,
                                HttpResponse.BodyHandlers.ofString()
                        );


        if (response.statusCode() < 200
                || response.statusCode() >= 300) {
        throw new IllegalStateException(
                "Unable to save progress history (HTTP "
                + response.statusCode() + ")."
        );
        }

        JSONArray savedRows;

        try {
        savedRows = new JSONArray(response.body());
        } catch (org.json.JSONException ex) {
        throw new IllegalStateException(
                "Progress save response was invalid. "
                + "Reload history before retrying.",
                ex
        );
        }

        if (savedRows.length() != 1) {
        throw new IllegalStateException(
                "Expected one saved progress record. "
                + "Reload history before retrying."
        );
        }

        try {
        JSONObject saved = savedRows.getJSONObject(0);

        UUID savedTaskId = UUID.fromString(
                saved.getString("task_id")
        );

        UUID savedEmployeeId = UUID.fromString(
                saved.getString("employee_id")
        );

        double savedPercentage = saved.getDouble(
                "progress_percentage"
        );

        OffsetDateTime savedAt = OffsetDateTime.parse(
                saved.getString("updated_at")
        );

        if (!progress.getTaskId().equals(savedTaskId)
                || !progress.getEmployeeId().equals(savedEmployeeId)) {
                throw new IllegalStateException(
                        "Saved progress belongs to a different task "
                        + "or employee. Reload history before retrying."
                );
        }

        if (!Double.isFinite(savedPercentage)
                || Math.abs(savedPercentage - percentage) > 0.000001) {
                throw new IllegalStateException(
                        "Saved progress differs from the submitted value. "
                        + "Reload history before retrying."
                );
        }

        // Copy the database-generated identity and timestamp.
        progress.setProgressId(saved.getLong("progress_id"));
        progress.setUpdatedAt(savedAt);

        } catch (org.json.JSONException
                | IllegalArgumentException
                | java.time.format.DateTimeParseException ex) {
        throw new IllegalStateException(
                "Saved progress response contains invalid fields. "
                + "Reload history before retrying.",
                ex
        );
        }
    }


    public List<TaskProgress> getProgressHistory(
            UUID taskId,
            String accessToken
    ) throws Exception {

        String url =
                AppConfig.REST_URL
                        + "/task_progress_tbl"
                        + "?select=*"
                        + "&task_id=eq."
                        + taskId
                        + "&order=updated_at.asc";


        HttpRequest request =
                supabaseClient
                        .createAuthenticatedRequest(
                                url,
                                accessToken
                        )
                        .GET()
                        .build();


        HttpResponse<String> response =
                supabaseClient
                        .getHttpClient()
                        .send(
                                request,
                                HttpResponse.BodyHandlers.ofString()
                        );


        if (
                response.statusCode() < 200
                        || response.statusCode() >= 300
        ) {

            throw new RuntimeException(
                    "Unable to load progress history: "
                            + response.body()
            );
        }


        JSONArray array =
                new JSONArray(
                        response.body()
                );


        List<TaskProgress> result =
                new ArrayList<>();


        for (
                int i = 0;
                i < array.length();
                i++
        ) {

            JSONObject json =
                    array.getJSONObject(i);


            TaskProgress progress =
                    new TaskProgress();


            progress.setProgressId(
                    json.getLong(
                            "progress_id"
                    )
            );


            progress.setTaskId(
                    UUID.fromString(
                            json.getString(
                                    "task_id"
                            )
                    )
            );


            progress.setEmployeeId(
                    UUID.fromString(
                            json.getString(
                                    "employee_id"
                            )
                    )
            );


            progress.setProgressPercentage(
                    json.getDouble(
                            "progress_percentage"
                    )
            );


            if (
                    json.has("progress_note")
                            && !json.isNull("progress_note")
            ) {

                progress.setProgressNote(
                        json.getString(
                                "progress_note"
                        )
                );
            }


            if (
                    json.has("updated_at")
                            && !json.isNull("updated_at")
            ) {

                progress.setUpdatedAt(
                        OffsetDateTime.parse(
                                json.getString(
                                        "updated_at"
                                )
                        )
                );
            }


            result.add(
                    progress
            );
        }


        return result;
    }
}