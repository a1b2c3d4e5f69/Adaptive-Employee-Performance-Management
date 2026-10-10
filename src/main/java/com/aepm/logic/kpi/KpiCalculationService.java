package com.aepm.logic.kpi;

import com.aepm.data.kpi.KpiResultRepository;

import com.aepm.logic.task.TaskService;

import com.aepm.model.KpiComponent;
import com.aepm.model.KpiConfig;
import com.aepm.model.KpiResult;
import com.aepm.model.Task;
import com.aepm.model.User;

import com.aepm.util.SessionManager;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import java.util.List;

public class KpiCalculationService {

    private final KpiResultRepository resultRepository =
            new KpiResultRepository();

    private final KpiConfigService configService =
            new KpiConfigService();

    private final KpiComponentService componentService =
            new KpiComponentService();

    private final TaskService taskService =
            new TaskService();


    // =========================================================
    // CALCULATE KPI
    // =========================================================

    public KpiResult calculateKpi(
            String employeeId,
            LocalDate startDate,
            LocalDate endDate
    ) throws Exception {

        if (
                employeeId == null
                        || employeeId.isBlank()
        ) {

            throw new IllegalArgumentException(
                    "Employee is required."
            );
        }


        if (
                startDate == null
                        || endDate == null
        ) {

            throw new IllegalArgumentException(
                    "Evaluation period is required."
            );
        }


        if (
                endDate.isBefore(
                        startDate
                )
        ) {

            throw new IllegalArgumentException(
                    "Evaluation end date cannot be before start date."
            );
        }


        // =====================================================
        // ACTIVE KPI CONFIG
        // =====================================================

        List<KpiConfig> configs =
                configService
                        .getActiveConfigs();


        if (
                configs == null
                        || configs.isEmpty()
        ) {

            throw new IllegalStateException(
                    "No active KPI configuration is available."
            );
        }


        KpiConfig config =
                configs.get(0);


        // =====================================================
        // KPI COMPONENTS
        // =====================================================

        List<KpiComponent> components =
                componentService
                        .getActiveComponentsByConfig(
                                config.getConfigId()
                        );


        if (
                components == null
                        || components.isEmpty()
        ) {

            throw new IllegalStateException(
                    "No active KPI components are configured."
            );
        }


        // =====================================================
        // EMPLOYEE TASKS
        // =====================================================

        List<Task> allTasks =
                taskService
                        .getAllTasks();


        List<Task> employeeTasks =
                allTasks.stream()
                        .filter(
                                task ->
                                        task.getAssignedEmployeeId() != null
                                                && employeeId.equals(
                                                task.getAssignedEmployeeId()
                                                        .toString()
                                        )
                        )
                        .filter(
                                task ->
                                        task.getAssignedDate() != null
                        )
                        .filter(
                                task -> {

                                    LocalDate taskDate =
                                            task.getAssignedDate()
                                                    .toLocalDate();

                                    return !taskDate.isBefore(
                                            startDate
                                    )
                                            && !taskDate.isAfter(
                                            endDate
                                    );
                                }
                        )
                        .toList();


        if (employeeTasks.isEmpty()) {

            throw new IllegalStateException(
                    "No task data is available for the selected employee and evaluation period."
            );
        }


        // =====================================================
        // COMPONENT SCORES
        // =====================================================

        double productivityScore =
                calculateProductivity(
                        employeeTasks
                );


        double completionRate =
                calculateCompletionRate(
                        employeeTasks
                );


        double timelinessScore =
                calculateTimeliness(
                        employeeTasks
                );


        // Temporary until attendance module is connected.
        double attendanceScore =
                0;


        // =====================================================
        // OVERALL KPI
        // =====================================================

        double overallScore =
                calculateOverallScore(
                        components,
                        productivityScore,
                        timelinessScore,
                        completionRate,
                        attendanceScore
                );


        // =====================================================
        // PERFORMANCE STATUS
        // =====================================================

        String performanceStatus;


        if (
                overallScore
                        < config.getRiskThreshold()
        ) {

            performanceStatus =
                    "At Risk";

        } else {

            performanceStatus =
                    "Normal";
        }


        // =====================================================
        // RESULT
        // =====================================================

        KpiResult result =
                new KpiResult();


        result.setEmployeeId(
                employeeId
        );

        result.setConfigId(
                config.getConfigId()
        );

        result.setEvaluationStartDate(
                startDate
        );

        result.setEvaluationEndDate(
                endDate
        );

        result.setProductivityScore(
                round(
                        productivityScore
                )
        );

        result.setTimelinessScore(
                round(
                        timelinessScore
                )
        );

        result.setCompletionRate(
                round(
                        completionRate
                )
        );

        result.setAttendanceScore(
                round(
                        attendanceScore
                )
        );

        result.setOverallKpiScore(
                round(
                        overallScore
                )
        );

        result.setPerformanceStatus(
                performanceStatus
        );

        result.setCalculatedAt(
                OffsetDateTime.now()
        );


        resultRepository.createResult(
                result,
                requireAccessToken()
        );


        return result;
    }


    // =========================================================
    // PRODUCTIVITY
    // =========================================================

    private double calculateProductivity(
            List<Task> tasks
    ) {

        if (tasks.isEmpty()) {
            return 0;
        }


        double totalProgress =
                0;


        for (Task task : tasks) {

            totalProgress +=
                    task.getProgress();
        }


        return totalProgress
                / tasks.size();
    }


    // =========================================================
    // COMPLETION RATE
    // =========================================================

    private double calculateCompletionRate(
            List<Task> tasks
    ) {

        if (tasks.isEmpty()) {
            return 0;
        }


        long completed =
                tasks.stream()
                        .filter(
                                task ->
                                        "Completed".equalsIgnoreCase(
                                                task.getTaskStatus()
                                        )
                        )
                        .count();


        return (
                (double) completed
                        / tasks.size()
        ) * 100;
    }


    // =========================================================
    // TIMELINESS
    // =========================================================

    private double calculateTimeliness(
            List<Task> tasks
    ) {

        List<Task> completedTasks =
                tasks.stream()
                        .filter(
                                task ->
                                        "Completed".equalsIgnoreCase(
                                                task.getTaskStatus()
                                        )
                        )
                        .filter(
                                task ->
                                        task.getCompletionDate() != null
                                                && task.getDueDate() != null
                        )
                        .toList();


        if (completedTasks.isEmpty()) {
            return 0;
        }


        long completedOnTime =
                completedTasks.stream()
                        .filter(
                                task ->
                                        !task.getCompletionDate()
                                                .isAfter(
                                                        task.getDueDate()
                                                )
                        )
                        .count();


        return (
                (double) completedOnTime
                        / completedTasks.size()
        ) * 100;
    }


    // =========================================================
    // OVERALL
    // =========================================================

    private double calculateOverallScore(
            List<KpiComponent> components,
            double productivity,
            double timeliness,
            double completionRate,
            double attendance
    ) {

        double total =
                0;


        for (
                KpiComponent component
                : components
        ) {

            String name =
                    normalize(
                            component.getComponentName()
                    );


            double score;


            switch (name) {

                case "productivity" ->
                        score =
                                productivity;


                case "timeliness" ->
                        score =
                                timeliness;


                case "completion rate",
                     "task completion rate",
                     "completion" ->
                        score =
                                completionRate;


                case "attendance" ->
                        score =
                                attendance;


                default ->
                        throw new IllegalStateException(
                                "Unsupported KPI component: "
                                        + component.getComponentName()
                        );
            }


            total +=
                    score
                            * (
                            component.getWeighting()
                                    / 100.0
                    );
        }


        return total;
    }


    // =========================================================
    // RESULTS
    // =========================================================

    public List<KpiResult> getEmployeeResults(
            String employeeId
    ) throws Exception {

        return resultRepository
                .getResultsByEmployee(
                        employeeId,
                        requireAccessToken()
                );
    }


    public List<KpiResult> getAllResults()
            throws Exception {

        return resultRepository
                .getAllResults(
                        requireAccessToken()
                );
    }


    // =========================================================
    // HELPERS
    // =========================================================

    private String normalize(
            String value
    ) {

        if (value == null) {
            return "";
        }


        return value
                .trim()
                .toLowerCase();
    }


    private double round(
            double value
    ) {

        return Math.round(
                value * 100.0
        ) / 100.0;
    }


    private String requireAccessToken() {

        String token =
                SessionManager.getAccessToken();


        if (
                token == null
                        || token.isBlank()
        ) {

            throw new IllegalStateException(
                    "User session has expired."
            );
        }


        return token;
    }
}