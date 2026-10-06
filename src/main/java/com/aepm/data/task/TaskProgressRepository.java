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


        if (
                response.statusCode() < 200
                        || response.statusCode() >= 300
        ) {

            throw new RuntimeException(
                    "Unable to save progress history: "
                            + response.body()
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