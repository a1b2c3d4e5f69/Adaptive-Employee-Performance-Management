package com.aepm.data.task;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;
import com.aepm.model.TaskAssignment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import java.time.OffsetDateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TaskAssignmentRepository {

    private final SupabaseClient supabaseClient =
            SupabaseClient.getInstance();


    public void assignEmployees(
            UUID taskId,
            List<UUID> employeeIds,
            UUID assignedBy,
            String accessToken
    ) throws Exception {

        if (taskId == null) {
            throw new IllegalArgumentException(
                    "Task ID is required."
            );
        }

        if (
                employeeIds == null
                        || employeeIds.isEmpty()
        ) {
            throw new IllegalArgumentException(
                    "At least one employee is required."
            );
        }

        JSONArray body =
                new JSONArray();

        for (UUID employeeId : employeeIds) {

            JSONObject row =
                    new JSONObject();

            row.put(
                    "task_id",
                    taskId.toString()
            );

            row.put(
                    "employee_id",
                    employeeId.toString()
            );

            row.put(
                    "assigned_by",
                    assignedBy == null
                            ? JSONObject.NULL
                            : assignedBy.toString()
            );

            row.put(
                    "assignment_status",
                    "Pending"
            );

            row.put(
                    "individual_progress",
                    0
            );

            body.put(
                    row
            );
        }


        String url =
                AppConfig.REST_URL
                        + "/task_assignment_tbl";


        HttpRequest request =
                supabaseClient
                        .createAuthenticatedRequest(
                                url,
                                accessToken
                        )
                        .header(
                                "Prefer",
                                "return=representation,resolution=merge-duplicates"
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


        validate(
                response,
                "Unable to assign team members"
        );
    }


    public List<TaskAssignment> getAssignments(
            UUID taskId,
            String accessToken
    ) throws Exception {

        String url =
                AppConfig.REST_URL
                        + "/task_assignment_tbl"
                        + "?select=*,user_tbl!task_assignment_tbl_employee_id_fkey("
                        + "user_id,employee_id,username,full_name)"
                        + "&task_id=eq."
                        + taskId
                        + "&order=created_at.asc";


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


        validate(
                response,
                "Unable to load task members"
        );


        JSONArray array =
                new JSONArray(
                        response.body()
                );


        List<TaskAssignment> result =
                new ArrayList<>();


        for (int i = 0; i < array.length(); i++) {

            JSONObject json =
                    array.getJSONObject(i);

            TaskAssignment assignment =
                    map(
                            json
                    );

            result.add(
                    assignment
            );
        }


        return result;
    }


    public List<UUID> getTaskIdsForEmployee(
            UUID employeeId,
            String accessToken
    ) throws Exception {

        String url =
                AppConfig.REST_URL
                        + "/task_assignment_tbl"
                        + "?select=task_id"
                        + "&employee_id=eq."
                        + employeeId;


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


        validate(
                response,
                "Unable to load employee task assignments"
        );


        JSONArray array =
                new JSONArray(
                        response.body()
                );


        List<UUID> ids =
                new ArrayList<>();


        for (int i = 0; i < array.length(); i++) {

            String value =
                    array
                            .getJSONObject(i)
                            .optString(
                                    "task_id",
                                    null
                            );

            if (
                    value != null
                            && !value.isBlank()
            ) {
                ids.add(
                        UUID.fromString(
                                value
                        )
                );
            }
        }


        return ids;
    }


    public void updateProgress(
            UUID taskId,
            UUID employeeId,
            int progress,
            String accessToken
    ) throws Exception {

        JSONObject body =
                new JSONObject();

        body.put(
                "p_task_id",
                taskId.toString()
        );

        body.put(
                "p_employee_id",
                employeeId.toString()
        );

        body.put(
                "p_progress",
                progress
        );


        String url =
                AppConfig.REST_URL
                        + "/rpc/aepm_update_task_assignment_progress";


        HttpRequest request =
                supabaseClient
                        .createAuthenticatedRequest(
                                url,
                                accessToken
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


        validate(
                response,
                "Unable to update team task progress"
        );
    }


    private TaskAssignment map(
            JSONObject json
    ) {

        TaskAssignment assignment =
                new TaskAssignment();

        assignment.setAssignmentId(
                uuid(
                        json,
                        "assignment_id"
                )
        );

        assignment.setTaskId(
                uuid(
                        json,
                        "task_id"
                )
        );

        assignment.setEmployeeId(
                uuid(
                        json,
                        "employee_id"
                )
        );

        assignment.setAssignedBy(
                uuid(
                        json,
                        "assigned_by"
                )
        );

        assignment.setAssignmentStatus(
                json.optString(
                        "assignment_status",
                        "Pending"
                )
        );

        assignment.setIndividualProgress(
                json.optInt(
                        "individual_progress",
                        0
                )
        );

        assignment.setDueDate(date(json, "due_date"));

        assignment.setAcceptedAt(
                date(
                        json,
                        "accepted_at"
                )
        );

        assignment.setCompletedAt(
                date(
                        json,
                        "completed_at"
                )
        );

        assignment.setCreatedAt(
                date(
                        json,
                        "created_at"
                )
        );

        assignment.setUpdatedAt(
                date(
                        json,
                        "updated_at"
                )
        );


        JSONObject employee =
                json.optJSONObject(
                        "user_tbl"
                );

        if (employee != null) {

            String fullName =
                    employee.optString(
                            "full_name",
                            ""
                    );

            if (fullName.isBlank()) {
                fullName =
                        employee.optString(
                                "username",
                                "Employee"
                        );
            }

            assignment.setEmployeeName(
                    fullName
            );

            assignment.setEmployeeNumber(
                    employee.optString(
                            "employee_id",
                            ""
                    )
            );
        }


        return assignment;
    }


    private UUID uuid(
            JSONObject json,
            String key
    ) {

        String value =
                json.optString(
                        key,
                        null
                );

        if (
                value == null
                        || value.isBlank()
        ) {
            return null;
        }

        return UUID.fromString(
                value
        );
    }


    private OffsetDateTime date(
            JSONObject json,
            String key
    ) {

        String value =
                json.optString(
                        key,
                        null
                );

        if (
                value == null
                        || value.isBlank()
        ) {
            return null;
        }

        return OffsetDateTime.parse(
                value
        );
    }


    private void validate(
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