package com.aepm.data.task;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;
import com.aepm.model.EmployeeDailyTask;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EmployeeDailyTaskRepository {

    private final SupabaseClient supabaseClient =
            SupabaseClient.getInstance();


    // =========================================================
    // GET DAILY TASKS BY DATE
    // =========================================================

    public List<EmployeeDailyTask> getTasksForDate(
            UUID employeeId,
            LocalDate date,
            String accessToken
    ) throws Exception {

        String url =
                AppConfig.REST_URL
                        + "/employee_daily_task_tbl"
                        + "?select=*"
                        + "&employee_id=eq." + employeeId
                        + "&task_date=eq." + date
                        + "&order=due_time.asc.nullslast,created_at.desc";


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
                "Unable to load daily tasks"
        );


        return mapList(response.body());
    }


    // =========================================================
    // CREATE
    // =========================================================

    public EmployeeDailyTask createTask(
            EmployeeDailyTask task,
            String accessToken
    ) throws Exception {

        JSONObject body =
                buildBody(task);


        String url =
                AppConfig.REST_URL
                        + "/employee_daily_task_tbl";


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
                "Unable to create daily task"
        );


        return first(response.body());
    }


    // =========================================================
    // UPDATE
    // =========================================================

    public EmployeeDailyTask updateTask(
            EmployeeDailyTask task,
            String accessToken
    ) throws Exception {

        JSONObject body =
                buildBody(task);


        String url =
                AppConfig.REST_URL
                        + "/employee_daily_task_tbl"
                        + "?daily_task_id=eq."
                        + task.getDailyTaskId();


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
                "Unable to update daily task"
        );


        return first(response.body());
    }


    // =========================================================
    // UPDATE STATUS
    // =========================================================

    public EmployeeDailyTask updateStatus(
            UUID dailyTaskId,
            String status,
            String accessToken
    ) throws Exception {

        JSONObject body =
                new JSONObject();

        body.put(
                "task_status",
                status
        );


        String url =
                AppConfig.REST_URL
                        + "/employee_daily_task_tbl"
                        + "?daily_task_id=eq."
                        + dailyTaskId;


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
                "Unable to update daily task status"
        );


        return first(response.body());
    }


    // =========================================================
    // DELETE
    // =========================================================

    public void deleteTask(
            UUID dailyTaskId,
            String accessToken
    ) throws Exception {

        String url =
                AppConfig.REST_URL
                        + "/employee_daily_task_tbl"
                        + "?daily_task_id=eq."
                        + dailyTaskId;


        HttpRequest request =
                supabaseClient
                        .createAuthenticatedRequest(
                                url,
                                accessToken
                        )
                        .DELETE()
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
                "Unable to delete daily task"
        );
    }


    // =========================================================
    // JSON BODY
    // =========================================================

    private JSONObject buildBody(
            EmployeeDailyTask task
    ) {

        JSONObject body =
                new JSONObject();


        body.put(
                "employee_id",
                task.getEmployeeId().toString()
        );


        body.put(
                "task_title",
                task.getTaskTitle()
        );


        body.put(
                "task_description",
                task.getTaskDescription() == null
                        || task.getTaskDescription().isBlank()
                        ? JSONObject.NULL
                        : task.getTaskDescription()
        );


        body.put(
                "category",
                task.getCategory()
        );


        body.put(
                "priority",
                task.getPriority()
        );


        body.put(
                "task_date",
                task.getTaskDate().toString()
        );


        body.put(
                "due_time",
                task.getDueTime() == null
                        ? JSONObject.NULL
                        : task.getDueTime().toString()
        );


        body.put(
                "task_status",
                task.getTaskStatus()
        );


        return body;
    }


    // =========================================================
    // MAPPING
    // =========================================================

    private List<EmployeeDailyTask> mapList(
            String response
    ) {

        JSONArray array =
                new JSONArray(response);


        List<EmployeeDailyTask> tasks =
                new ArrayList<>();


        for (
                int i = 0;
                i < array.length();
                i++
        ) {

            tasks.add(
                    map(
                            array.getJSONObject(i)
                    )
            );
        }


        return tasks;
    }


    private EmployeeDailyTask first(
            String response
    ) {

        JSONArray array =
                new JSONArray(response);


        if (array.length() == 0) {

            throw new RuntimeException(
                    "No daily task returned from server."
            );
        }


        return map(
                array.getJSONObject(0)
        );
    }


    private EmployeeDailyTask map(
            JSONObject data
    ) {

        EmployeeDailyTask task =
                new EmployeeDailyTask();


        task.setDailyTaskId(
                UUID.fromString(
                        data.getString(
                                "daily_task_id"
                        )
                )
        );


        task.setEmployeeId(
                UUID.fromString(
                        data.getString(
                                "employee_id"
                        )
                )
        );


        task.setTaskTitle(
                data.getString(
                        "task_title"
                )
        );


        task.setTaskDescription(
                optionalString(
                        data,
                        "task_description"
                )
        );


        task.setCategory(
                data.optString(
                        "category",
                        "Other"
                )
        );


        task.setPriority(
                data.optString(
                        "priority",
                        "Medium"
                )
        );


        task.setTaskDate(
                LocalDate.parse(
                        data.getString(
                                "task_date"
                        )
                )
        );


        String dueTime =
                optionalString(
                        data,
                        "due_time"
                );


        if (
                dueTime != null
                        && !dueTime.isBlank()
        ) {

            task.setDueTime(
                    LocalTime.parse(
                            dueTime
                    )
            );
        }


        task.setTaskStatus(
                data.optString(
                        "task_status",
                        "Pending"
                )
        );


        task.setCompletedAt(
                parseOffsetDateTime(
                        data,
                        "completed_at"
                )
        );


        task.setCreatedAt(
                parseOffsetDateTime(
                        data,
                        "created_at"
                )
        );


        task.setUpdatedAt(
                parseOffsetDateTime(
                        data,
                        "updated_at"
                )
        );


        return task;
    }


    private String optionalString(
            JSONObject data,
            String key
    ) {

        if (
                !data.has(key)
                        || data.isNull(key)
        ) {

            return null;
        }


        return data.optString(
                key,
                null
        );
    }


    private OffsetDateTime parseOffsetDateTime(
            JSONObject data,
            String key
    ) {

        String value =
                optionalString(
                        data,
                        key
                );


        if (
                value == null
                        || value.isBlank()
        ) {

            return null;
        }


        return OffsetDateTime.parse(value);
    }


    // =========================================================
    // HTTP VALIDATION
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
}