package com.aepm.logic.dashboard;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

public final class DashboardDataService {

    private final SupabaseClient client =
            SupabaseClient.getInstance();

    private static final ZoneId MY =
            ZoneId.of("Asia/Kuala_Lumpur");

    public record TaskStats(
            int total,
            int completed,
            int inProgress,
            int overdue,
            int other,
            double completionRate,
            Set<String> taskIds,
            int pending
    ) {}

    public record EscalationStats(
            int active,
            int level1,
            int level2,
            int level3
    ) {}

    public JSONArray readAll(
            String token,
            String table,
            String columns,
            String filter
    ) throws Exception {

        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Login required");
        }

        JSONArray all = new JSONArray();
        int offset = 0;
        final int pageSize = 500;

        while (true) {

            String url = AppConfig.REST_URL + "/" + table
                    + "?select=" + URLEncoder.encode(
                            columns, StandardCharsets.UTF_8)
                    + (filter == null ? "" : filter)
                    + "&limit=" + pageSize
                    + "&offset=" + offset;

            HttpRequest request = client
                    .createAuthenticatedRequest(url, token)
                    .GET()
                    .build();

            HttpResponse<String> response = client
                    .getHttpClient()
                    .send(request,
                            HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        table + " HTTP " + response.statusCode()
                                + ": " + response.body()
                );
            }

            JSONArray page = new JSONArray(response.body());

            for (int i = 0; i < page.length(); i++) {
                all.put(page.getJSONObject(i));
            }

            if (page.length() < pageSize) break;
            offset += pageSize;
        }

        return all;
    }

    public boolean isOverdue(String due) {

        if (due == null || due.isBlank()
                || due.equalsIgnoreCase("null")) {
            return false;
        }

        try {
            return OffsetDateTime.parse(due)
                    .toInstant().isBefore(Instant.now());
        } catch (Exception ignored) {}

        try {
            return LocalDateTime.parse(
                    due.replace(' ', 'T'))
                    .atZone(MY).toInstant()
                    .isBefore(Instant.now());
        } catch (Exception ignored) {}

        try {
            return LocalDate.parse(due.substring(0, 10))
                    .isBefore(LocalDate.now(MY));
        } catch (Exception ignored) {}

        return false;
    }

    public boolean isDueSoon(String due) {

        if (due == null || due.isBlank()) return false;

        try {
            LocalDate date =
                    LocalDate.parse(due.substring(0, 10));

            LocalDate today = LocalDate.now(MY);

            return !date.isBefore(today)
                    && !date.isAfter(today.plusDays(3));

        } catch (Exception ignored) {
            return false;
        }
    }

    public TaskStats calculateTasks(JSONArray tasks) {

        int completed = 0;
        int inProgress = 0;
        int overdue = 0;
        int pending = 0;
        int other = 0;
        int cancelled = 0;

        Set<String> taskIds = new HashSet<>();

        for (int i = 0; i < tasks.length(); i++) {

            JSONObject task = tasks.getJSONObject(i);

            String id = task.optString("task_id", "");
            if (!id.isBlank()) taskIds.add(id);

            String status = task
                    .optString("task_status", "").trim();

            if (status.equalsIgnoreCase("Cancelled")
                    || status.equalsIgnoreCase("Canceled")) {
                cancelled++;
                other++;
                continue;
            }

            if (status.equalsIgnoreCase("Completed")
                    || (task.has("completion_date")
                    && !task.isNull("completion_date"))) {
                completed++;
                continue;
            }

            if (isOverdue(task.optString("due_date", ""))) {
                overdue++;
            }

            if (status.equalsIgnoreCase("In Progress")
                    || status.equalsIgnoreCase("InProgress")
                    || status.equalsIgnoreCase("In_Progress")) {
                inProgress++;
                continue;
            }

            if (status.equalsIgnoreCase("Pending")) {
                pending++;
                continue;
            }

            other++;
        }

        int eligible = tasks.length() - cancelled;

        double rate = eligible <= 0
                ? 0
                : completed * 100.0 / eligible;

        return new TaskStats(
                tasks.length(),
                completed,
                inProgress,
                overdue,
                other,
                rate,
                taskIds,
                pending
        );
    }

    public EscalationStats calculateEscalations(JSONArray escalations) {
        int active = 0;
        int level1 = 0;
        int level2 = 0;
        int level3 = 0;

        for (int i = 0; i < escalations.length(); i++) {
            JSONObject row = escalations.getJSONObject(i);

            String status = row.optString("status", "").trim();
            int level = row.optInt("current_level", 0);

            boolean isActive = "Active".equalsIgnoreCase(status);
            boolean isResolved = "Resolved".equalsIgnoreCase(status);

            if ((!isActive && !isResolved) || level < 1 || level > 3) {
                continue;
            }

            if (isActive) {
                active++;
            }

            switch (level) {
                case 1 -> level1++;
                case 2 -> level2++;
                case 3 -> level3++;
                default -> {}
            }
        }

        return new EscalationStats(active, level1, level2, level3);
    }

    public JSONArray getTasksForEmployees(
            String token,
            Set<String> employeeIds
    ) throws Exception {

        JSONArray result = new JSONArray();
        List<String> ids = new ArrayList<>(employeeIds);

        for (int i = 0; i < ids.size(); i += 40) {
            String employeeFilter = String.join(
                    ",",
                    ids.subList(i, Math.min(i + 40, ids.size()))
            );

            // Individual tasks.
            JSONArray individualTasks = readAll(
                    token,
                    "task_tbl",
                    "task_id,task_title,assigned_employee_id,"
                            + "assignment_type,task_status,due_date,completion_date",
                    "&assigned_employee_id=in.(" + employeeFilter + ")"
            );

            for (int j = 0; j < individualTasks.length(); j++) {
                JSONObject task = individualTasks.getJSONObject(j);

                if (!"TEAM".equalsIgnoreCase(
                        task.optString("assignment_type", ""))) {
                    result.put(task);
                }
            }

            // Team tasks: use each employee's own assignment.
            JSONArray assignments = readAll(
                    token,
                    "task_assignment_tbl",
                    "employee_id,assignment_status,individual_progress,"
                            + "due_date,completed_at,task_tbl!inner("
                            + "task_id,task_title,assignment_type,task_status)",
                    "&employee_id=in.(" + employeeFilter + ")"
                            + "&task_tbl.assignment_type=eq.TEAM"
            );

            for (int j = 0; j < assignments.length(); j++) {
                JSONObject assignment = assignments.getJSONObject(j);
                JSONObject parent = assignment.getJSONObject("task_tbl");

                String memberStatus = assignment.optString(
                        "assignment_status", "Pending"
                ).trim();

                String parentStatus = parent.optString(
                        "task_status", ""
                ).trim();

                boolean cancelled =
                        "Cancelled".equalsIgnoreCase(parentStatus)
                        || "Canceled".equalsIgnoreCase(parentStatus)
                        || "Rejected".equalsIgnoreCase(memberStatus);

                JSONObject task = new JSONObject();

                task.put("task_id", parent.getString("task_id"));
                task.put("task_title", parent.optString("task_title", ""));
                task.put("assigned_employee_id",
                        assignment.getString("employee_id"));

                task.put("task_status",
                        cancelled ? "Cancelled" : memberStatus);

                task.put("due_date",
                        assignment.isNull("due_date")
                                ? JSONObject.NULL
                                : assignment.get("due_date"));

                task.put("completion_date",
                        assignment.isNull("completed_at")
                                ? JSONObject.NULL
                                : assignment.get("completed_at"));

                task.put("progress",
                        assignment.optInt("individual_progress", 0));

                result.put(task);
            }
        }

        return result;
    }

    public JSONArray getEscalationsForTasks(
            String token,
            Set<String> taskIds
    ) throws Exception {

        JSONArray result = new JSONArray();
        List<String> ids = new ArrayList<>(taskIds);

        for (int i = 0; i < ids.size(); i += 40) {

            String filter = "&task_id=in.("
                    + String.join(",",
                    ids.subList(i, Math.min(i + 40, ids.size())))
                    + ")";

            JSONArray rows = readAll(
                    token,
                    "task_escalation_tbl",
                    "task_id,current_level,status",
                    filter
            );

            for (int j = 0; j < rows.length(); j++) {
                result.put(rows.getJSONObject(j));
            }
        }

        return result;
    }

    public JSONArray getEscalationsForEmployees(
            String token,
            Set<String> employeeIds
    ) throws Exception {

        JSONArray result = new JSONArray();
        List<String> ids = new ArrayList<>(employeeIds);

        for (int i = 0; i < ids.size(); i += 40) {
            String filter = "&employee_id=in.("
                    + String.join(",",
                        ids.subList(i, Math.min(i + 40, ids.size())))
                    + ")";

            JSONArray rows = readAll(
                    token,
                    "task_escalation_tbl",
                    "task_id,employee_id,current_level,status",
                    filter
            );

            for (int j = 0; j < rows.length(); j++) {
                result.put(rows.getJSONObject(j));
            }
        }

        return result;
    }
}
