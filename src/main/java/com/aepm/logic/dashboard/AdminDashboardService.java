package com.aepm.logic.dashboard;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public final class AdminDashboardService {

    private final DashboardDataService data =
            new DashboardDataService();

    private final DashboardKpiService kpi =
            new DashboardKpiService();

    public record Summary(
            int employees,
            int managers,
            int totalTasks,
            int completed,
            int inProgress,
            int overdue,
            int otherTasks,
            int activeEscalations,
            int level1,
            int level2,
            int level3,
            double completionRate,
            int pending,
            Double averageKpi,
            int evaluatedEmployees,
            int atRiskEmployees
    ) {}

    public Summary load(String token) throws Exception {

        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Login required");
        }

        JSONArray users = data.readAll(
                token,
                "user_tbl",
                "user_id,role_id",
                null
        );

        JSONArray tasks = data.readAll(
                token,
                "task_tbl",
                "task_id,task_status,due_date,completion_date",
                null
        );

        JSONArray escalations = data.readAll(
                token,
                "task_escalation_tbl",
                "current_level,status",
                null
        );

        int employees = 0;
        int managers = 0;

        Set<String> employeeIds = new HashSet<>();

        for (int i = 0; i < users.length(); i++) {

            JSONObject user = users.getJSONObject(i);

            long role = user.optLong("role_id", 0);

            if (role == 3) {
                employees++;
                employeeIds.add(
                        user.optString("user_id", "")
                );
            } else if (role == 2) {
                managers++;
            }
        }

        DashboardDataService.TaskStats taskStats =
                data.calculateTasks(tasks);

        DashboardDataService.EscalationStats escalationStats =
                data.calculateEscalations(escalations);

        DashboardKpiService.KpiStats kpiStats =
                kpi.summarize(employeeIds);

        return new Summary(
                employees,
                managers,
                taskStats.total(),
                taskStats.completed(),
                taskStats.inProgress(),
                taskStats.overdue(),
                taskStats.other(),
                escalationStats.active(),
                escalationStats.level1(),
                escalationStats.level2(),
                escalationStats.level3(),
                taskStats.completionRate(),
                taskStats.pending(),
                kpiStats.averageScore(),
                kpiStats.evaluated(),
                kpiStats.atRisk()
        );
    }
}
