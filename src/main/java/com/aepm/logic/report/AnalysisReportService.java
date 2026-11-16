package com.aepm.logic.report;

import com.aepm.data.report.AnalysisReportRepository;
import com.aepm.model.EmployeeAnalysisResult;
import com.aepm.model.TeamAnalysisResult;

import com.aepm.util.SessionManager;

import java.util.List;

public class AnalysisReportService {

    private final AnalysisReportRepository repository =
            new AnalysisReportRepository();


    public List<EmployeeAnalysisResult> getIndividualResults()
            throws Exception {

        return repository.getAll(
                token()
        );
    }


    public EmployeeAnalysisResult getEmployee(
            String employeeNumber
    ) throws Exception {

        if (
                employeeNumber == null
                        || employeeNumber.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Employee ID is required."
            );
        }


        return getIndividualResults()
                .stream()
                .filter(
                        result ->
                                employeeNumber.equalsIgnoreCase(
                                        result.getEmployeeId()
                                )
                )
                .findFirst()
                .orElse(
                        null
                );
    }


    public TeamAnalysisResult getTeamSummary()
            throws Exception {

        List<EmployeeAnalysisResult> employees =
                getIndividualResults();


        TeamAnalysisResult team =
                new TeamAnalysisResult();

        team.setEmployees(
                employees
        );

        team.setEmployeeCount(
                employees.size()
        );


        if (employees.isEmpty()) {
            return team;
        }


        team.setAverageKpi(
                employees
                        .stream()
                        .mapToDouble(
                                EmployeeAnalysisResult::getOverallKpi
                        )
                        .average()
                        .orElse(
                                0
                        )
        );


        team.setAverageProductivity(
                employees
                        .stream()
                        .mapToDouble(
                                EmployeeAnalysisResult::getProductivity
                        )
                        .average()
                        .orElse(
                                0
                        )
        );


        team.setAverageTimeliness(
                employees
                        .stream()
                        .mapToDouble(
                                EmployeeAnalysisResult::getTimeliness
                        )
                        .average()
                        .orElse(
                                0
                        )
        );


        team.setAverageCompletion(
                employees
                        .stream()
                        .mapToDouble(
                                EmployeeAnalysisResult::getCompletion
                        )
                        .average()
                        .orElse(
                                0
                        )
        );


        team.setAverageAttendance(
                employees
                        .stream()
                        .mapToDouble(
                                EmployeeAnalysisResult::getAttendance
                        )
                        .average()
                        .orElse(
                                0
                        )
        );


        team.setTotalTasks(
                employees
                        .stream()
                        .mapToInt(
                                EmployeeAnalysisResult::getTotalTasks
                        )
                        .sum()
        );


        team.setCompletedTasks(
                employees
                        .stream()
                        .mapToInt(
                                EmployeeAnalysisResult::getCompletedTasks
                        )
                        .sum()
        );


        team.setOverdueTasks(
                employees
                        .stream()
                        .mapToInt(
                                EmployeeAnalysisResult::getOverdueTasks
                        )
                        .sum()
        );


        return team;
    }


    private String token() {

        String token =
                SessionManager
                        .getAccessToken();

        if (
                token == null
                        || token.isBlank()
        ) {
            throw new IllegalStateException(
                    "No access token found."
            );
        }

        return token;
    }
}