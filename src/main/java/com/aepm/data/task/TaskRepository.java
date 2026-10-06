package com.aepm.data.task;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;
import com.aepm.model.Task;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.time.OffsetDateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TaskRepository {

    private final SupabaseClient supabaseClient =
            SupabaseClient.getInstance();


    // =========================================================
    // CREATE TASK
    // =========================================================

    public void createTask(
            Task task,
            String accessToken
    ) throws Exception {

        JSONObject body =
                new JSONObject();


        body.put(
                "task_title",
                task.getTaskTitle()
        );


        body.put(
                "task_description",
                task.getTaskDescription() == null
                        ? JSONObject.NULL
                        : task.getTaskDescription()
        );


        body.put(
                "category_id",
                task.getCategoryId() == null
                        ? JSONObject.NULL
                        : task.getCategoryId()
        );


        body.put(
                "assigned_employee_id",
                task.getAssignedEmployeeId() == null
                        ? JSONObject.NULL
                        : task.getAssignedEmployeeId().toString()
        );


        body.put(
                "assigned_by",
                task.getAssignedBy() == null
                        ? JSONObject.NULL
                        : task.getAssignedBy().toString()
        );


        body.put(
                "priority",
                task.getPriority()
        );


        body.put(
                "difficulty_level",
                task.getDifficultyLevel()
        );


        body.put(
                "task_status",
                task.getTaskStatus()
        );


        body.put(
                "progress",
                task.getProgress()
        );


        body.put(
                "due_date",
                task.getDueDate() == null
                        ? JSONObject.NULL
                        : task.getDueDate().toString()
        );


        body.put(
                "completion_criteria",
                task.getCompletionCriteria() == null
                        ? JSONObject.NULL
                        : task.getCompletionCriteria()
        );


        body.put(
                "created_by",
                task.getCreatedBy() == null
                        ? JSONObject.NULL
                        : task.getCreatedBy().toString()
        );


        String url =
                AppConfig.REST_URL
                        + "/task_tbl";


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


        validateResponse(
                response,
                "Unable to create task"
        );
    }


    // =========================================================
    // GET ALL TASKS
    // =========================================================

    public List<Task> getAllTasks(
            String accessToken
    ) throws Exception {

        String url =
                AppConfig.REST_URL
                        + "/task_tbl"
                        + "?select=*"
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
                supabaseClient
                        .getHttpClient()
                        .send(
                                request,
                                HttpResponse.BodyHandlers.ofString()
                        );


        validateResponse(
                response,
                "Unable to load tasks"
        );


        return mapTaskList(
                response.body()
        );
    }


    // =========================================================
    // GET TASKS BY EMPLOYEE
    // =========================================================

    public List<Task> getTasksByEmployee(
            UUID employeeId,
            String accessToken
    ) throws Exception {

        String url =
                AppConfig.REST_URL
                        + "/task_tbl"
                        + "?select=*"
                        + "&assigned_employee_id=eq."
                        + employeeId
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
                supabaseClient
                        .getHttpClient()
                        .send(
                                request,
                                HttpResponse.BodyHandlers.ofString()
                        );


        validateResponse(
                response,
                "Unable to load employee tasks"
        );


        return mapTaskList(
                response.body()
        );
    }


    // =========================================================
    // UPDATE TASK STATUS
    // =========================================================

    public void updateTaskStatus(
            UUID taskId,
            String status,
            String accessToken
    ) throws Exception {

        JSONObject body =
                new JSONObject();


        body.put(
                "task_status",
                status
        );


        body.put(
                "updated_at",
                OffsetDateTime.now().toString()
        );


        if ("Accepted".equals(status)) {

            body.put(
                    "acceptance_date",
                    OffsetDateTime.now().toString()
            );
        }


        if ("Completed".equals(status)) {

            body.put(
                    "completion_date",
                    OffsetDateTime.now().toString()
            );


            body.put(
                    "progress",
                    100
            );
        }


        String url =
                AppConfig.REST_URL
                        + "/task_tbl"
                        + "?task_id=eq."
                        + taskId;


        HttpRequest request =
                supabaseClient
                        .createAuthenticatedRequest(
                                url,
                                accessToken
                        )
                        .method(
                                "PATCH",
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


        validateResponse(
                response,
                "Unable to update task status"
        );
    }


    // =========================================================
    // UPDATE TASK PROGRESS
    // =========================================================

    public void updateTaskProgress(
            UUID taskId,
            double progress,
            String accessToken
    ) throws Exception {

        JSONObject body =
                new JSONObject();


        body.put(
                "progress",
                progress
        );


        body.put(
                "updated_at",
                OffsetDateTime.now().toString()
        );


        if (progress >= 100) {

            body.put(
                    "progress",
                    100
            );


            body.put(
                    "task_status",
                    "Completed"
            );


            body.put(
                    "completion_date",
                    OffsetDateTime.now().toString()
            );

        } else if (progress > 0) {

            body.put(
                    "task_status",
                    "In Progress"
            );
        }


        String url =
                AppConfig.REST_URL
                        + "/task_tbl"
                        + "?task_id=eq."
                        + taskId;


        HttpRequest request =
                supabaseClient
                        .createAuthenticatedRequest(
                                url,
                                accessToken
                        )
                        .method(
                                "PATCH",
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


        validateResponse(
                response,
                "Unable to update task progress"
        );
    }


    // =========================================================
    // UPDATE TASK INFORMATION
    // =========================================================

   // =========================================================
// UPDATE TASK INFORMATION
// =========================================================

public void updateTask(
        Task task,
        String accessToken
) throws Exception {

    JSONObject body =
            new JSONObject();


    body.put(
            "task_title",
            task.getTaskTitle()
    );


    body.put(
            "task_description",
            task.getTaskDescription() == null
                    ? JSONObject.NULL
                    : task.getTaskDescription()
    );

    body.put(
        "category_id",
        task.getCategoryId() == null
                ? JSONObject.NULL
                : task.getCategoryId()
    );


    body.put(
            "priority",
            task.getPriority()
    );


    body.put(
            "difficulty_level",
            task.getDifficultyLevel()
    );


    body.put(
            "due_date",
            task.getDueDate() == null
                    ? JSONObject.NULL
                    : task.getDueDate().toString()
    );


    body.put(
            "completion_criteria",
            task.getCompletionCriteria() == null
                    ? JSONObject.NULL
                    : task.getCompletionCriteria()
    );


    body.put(
            "updated_at",
            OffsetDateTime.now().toString()
    );


    String url =
            AppConfig.REST_URL
                    + "/task_tbl"
                    + "?task_id=eq."
                    + task.getTaskId();


    HttpRequest request =
            supabaseClient
                    .createAuthenticatedRequest(
                            url,
                            accessToken
                    )

                    // IMPORTANT:
                    // ask Supabase to return updated row
                    .header(
                            "Prefer",
                            "return=representation"
                    )

                    .method(
                            "PATCH",
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


    System.out.println(
            "UPDATE TASK STATUS: "
                    + response.statusCode()
    );


    System.out.println(
            "UPDATE TASK RESPONSE: "
                    + response.body()
    );


    if (
            response.statusCode() < 200
                    || response.statusCode() >= 300
    ) {

        throw new RuntimeException(
                "Unable to update task: "
                        + response.body()
        );
    }


    // =====================================================
    // IMPORTANT
    // Supabase returns [] if no row was actually updated
    // =====================================================

    JSONArray updatedRows =
            new JSONArray(
                    response.body()
            );


    if (updatedRows.isEmpty()) {

        throw new RuntimeException(
                "Task was not updated. "
                        + "Please check the Manager UPDATE RLS policy."
        );
    }
}

    // =========================================================
    // CANCEL TASK
    // =========================================================

    public void cancelTask(
            UUID taskId,
            String accessToken
    ) throws Exception {

        JSONObject body =
                new JSONObject();


        body.put(
                "task_status",
                "Cancelled"
        );


        body.put(
                "updated_at",
                OffsetDateTime.now().toString()
        );


        String url =
                AppConfig.REST_URL
                        + "/task_tbl"
                        + "?task_id=eq."
                        + taskId;


        HttpRequest request =
                supabaseClient
                        .createAuthenticatedRequest(
                                url,
                                accessToken
                        )
                        .method(
                                "PATCH",
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


        validateResponse(
                response,
                "Unable to cancel task"
        );
    }


    // =========================================================
    // MAP TASK LIST
    // =========================================================

    private List<Task> mapTaskList(
            String body
    ) {

        JSONArray array =
                new JSONArray(
                        body
                );


        List<Task> tasks =
                new ArrayList<>();


        for (
                int i = 0;
                i < array.length();
                i++
        ) {

            tasks.add(
                    mapTask(
                            array.getJSONObject(i)
                    )
            );
        }


        return tasks;
    }


    // =========================================================
    // MAP JSON -> TASK
    // =========================================================

    private Task mapTask(
            JSONObject json
    ) {

        Task task =
                new Task();


        task.setTaskId(
                UUID.fromString(
                        json.getString(
                                "task_id"
                        )
                )
        );


        task.setTaskTitle(
                json.optString(
                        "task_title",
                        ""
                )
        );


        task.setTaskDescription(
                getNullableString(
                        json,
                        "task_description"
                )
        );


        if (
                json.has("category_id")
                        && !json.isNull("category_id")
        ) {

            task.setCategoryId(
                    json.getLong(
                            "category_id"
                    )
            );
        }


        task.setAssignedEmployeeId(
                getNullableUUID(
                        json,
                        "assigned_employee_id"
                )
        );


        task.setAssignedBy(
                getNullableUUID(
                        json,
                        "assigned_by"
                )
        );


        task.setPriority(
                json.optString(
                        "priority",
                        "Medium"
                )
        );


        task.setDifficultyLevel(
                json.optString(
                        "difficulty_level",
                        "Medium"
                )
        );


        task.setTaskStatus(
                json.optString(
                        "task_status",
                        "Pending"
                )
        );


        task.setProgress(
                json.optDouble(
                        "progress",
                        0
                )
        );


        task.setAssignedDate(
                getNullableDateTime(
                        json,
                        "assigned_date"
                )
        );


        task.setDueDate(
                getNullableDateTime(
                        json,
                        "due_date"
                )
        );


        task.setAcceptanceDate(
                getNullableDateTime(
                        json,
                        "acceptance_date"
                )
        );


        task.setCompletionDate(
                getNullableDateTime(
                        json,
                        "completion_date"
                )
        );


        task.setCompletionCriteria(
                getNullableString(
                        json,
                        "completion_criteria"
                )
        );


        task.setCreatedBy(
                getNullableUUID(
                        json,
                        "created_by"
                )
        );


        task.setCreatedAt(
                getNullableDateTime(
                        json,
                        "created_at"
                )
        );


        task.setUpdatedAt(
                getNullableDateTime(
                        json,
                        "updated_at"
                )
        );


        return task;
    }


    // =========================================================
    // RESPONSE VALIDATION
    // =========================================================

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
    // HELPERS
    // =========================================================

    private String getNullableString(
            JSONObject json,
            String key
    ) {

        if (
                json.has(key)
                        && !json.isNull(key)
        ) {

            return json.getString(
                    key
            );
        }


        return null;
    }


    private UUID getNullableUUID(
            JSONObject json,
            String key
    ) {

        if (
                json.has(key)
                        && !json.isNull(key)
        ) {

            String value =
                    json.getString(
                            key
                    );


            if (
                    value != null
                            && !value.isBlank()
            ) {

                return UUID.fromString(
                        value
                );
            }
        }


        return null;
    }


    private OffsetDateTime getNullableDateTime(
            JSONObject json,
            String key
    ) {

        if (
                json.has(key)
                        && !json.isNull(key)
        ) {

            String value =
                    json.getString(
                            key
                    );


            if (
                    value != null
                            && !value.isBlank()
            ) {

                return OffsetDateTime.parse(
                        value
                );
            }
        }


        return null;
    }
}