package com.aepm.logic.dashboard;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class ManagerDashboardService {

    private final DashboardDataService data =
            new DashboardDataService();

    private final DashboardKpiService kpi =
            new DashboardKpiService();

    public record Summary(
            int teamMembers,
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
            int atRiskEmployees,
            int evaluatedEmployees,
            String escalationDetails
    ) {}

    public Summary load(
            String token,
            String managerUserId
    ) throws Exception {

        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Login required");
        }

        UUID managerId =
                UUID.fromString(managerUserId);

        JSONArray employees = data.readAll(
                token,
                "user_tbl",
                "user_id",
                "&manager_id=eq." + managerId
                        + "&role_id=eq.3"
        );

        Set<String> employeeIds = new HashSet<>();

        for (int i = 0; i < employees.length(); i++) {
            employeeIds.add(
                    employees.getJSONObject(i)
                            .getString("user_id")
            );
        }

        JSONArray tasks =
                data.getTasksForEmployees(
                        token, employeeIds
                );

        DashboardDataService.TaskStats taskStats =
                data.calculateTasks(tasks);

        JSONArray escalations =
                data.getEscalationsForTasks(
                        token, taskStats.taskIds()
                );

        DashboardDataService.EscalationStats escalationStats =
                data.calculateEscalations(escalations);

        DashboardKpiService.KpiStats kpiStats =
                kpi.summarize(employeeIds);

        String escalationDetails =
                "Level 1: " + escalationStats.level1()
                        + "   |   Level 2: "
                        + escalationStats.level2()
                        + "   |   Level 3: "
                        + escalationStats.level3();

        return new Summary(
                employeeIds.size(),
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
                kpiStats.atRisk(),
                kpiStats.evaluated(),
                escalationDetails
        );
    }
}
