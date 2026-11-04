package com.aepm.logic.dashboard;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.UUID;

public final class EmployeeDashboardService {

    private final DashboardDataService data =
            new DashboardDataService();

    private final DashboardKpiService kpi =
            new DashboardKpiService();

    public record Summary(
            int totalTasks,
            int pending,
            int completed,
            int inProgress,
            int overdue,
            int other,
            double completionRate,
            Double kpiScore,
            String performanceStatus,
            int dueSoon,
            int activeEscalations,
            int level1,
            int level2,
            int level3,
            String reminders
    ) {}

    public Summary load(
            String token,
            String employeeUserId
    ) throws Exception {

        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Login required");
        }

        UUID.fromString(employeeUserId);

        JSONArray tasks = data.readAll(
                token,
                "task_tbl",
                "task_id,task_title,task_status,due_date,completion_date",
                "&assigned_employee_id=eq." + employeeUserId
        );

        DashboardDataService.TaskStats taskStats =
                data.calculateTasks(tasks);

        int dueSoon = 0;
        StringBuilder reminders = new StringBuilder();

        for (int i = 0; i < tasks.length(); i++) {

            JSONObject task = tasks.getJSONObject(i);

            String status =
                    task.optString("task_status", "");

            if (status.equalsIgnoreCase("Completed")
                    || status.equalsIgnoreCase("Cancelled")
                    || status.equalsIgnoreCase("Canceled")
                    || (task.has("completion_date")
                    && !task.isNull("completion_date"))) {
                continue;
            }

            String due =
                    task.optString("due_date", "");

            if (data.isDueSoon(due)) {

                dueSoon++;

                if (reminders.length() < 350) {
                    String taskName = task.optString("task_title", "Untitled Task");

                    if (taskName.isBlank()) {
                        taskName = "Untitled Task";
                    }

                    String dueDisplay = due.length() >= 10
                            ? due.substring(0, 10)
                            : due;

                    reminders.append("• ")
                            .append(taskName)
                            .append("  |  Due: ")
                            .append(dueDisplay)
                            .append("\n");
                }
            }
        }

        JSONArray escalations =
                data.getEscalationsForTasks(
                        token, taskStats.taskIds()
                );

        DashboardDataService.EscalationStats escalationStats =
                data.calculateEscalations(escalations);

        DashboardKpiService.PersonalKpi personalKpi =
                kpi.personal(employeeUserId);

        String reminderText = reminders.length() == 0
                ? "No deadlines in the next 3 days."
                : reminders.toString();

        return new Summary(
                taskStats.total(),
                taskStats.pending(),
                taskStats.completed(),
                taskStats.inProgress(),
                taskStats.overdue(),
                taskStats.other(),
                taskStats.completionRate(),
                personalKpi.score(),
                personalKpi.status(),
                dueSoon,
                escalationStats.active(),
                escalationStats.level1(),
                escalationStats.level2(),
                escalationStats.level3(),
                reminderText
        );
    }
}
