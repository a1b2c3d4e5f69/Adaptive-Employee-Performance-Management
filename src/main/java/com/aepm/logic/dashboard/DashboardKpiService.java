package com.aepm.logic.dashboard;

import com.aepm.logic.kpi.KpiCalculationService;
import com.aepm.model.KpiResult;

import java.time.OffsetDateTime;
import java.util.*;

public final class DashboardKpiService {

    private final KpiCalculationService service =
            new KpiCalculationService();

    public record KpiStats(
            int evaluated,
            int atRisk,
            Double averageScore
    ) {}

    public record PersonalKpi(
            Double score,
            String status
    ) {}

    private Map<String, KpiResult> getLatestResults()
            throws Exception {

        Map<String, KpiResult> latest = new HashMap<>();

        List<KpiResult> results =
                service.getAllResults();

        for (KpiResult result : results) {

            if (result == null
                    || result.getEmployeeId() == null) {
                continue;
            }

            String employeeId = result.getEmployeeId();

            KpiResult previous = latest.get(employeeId);

            OffsetDateTime currentDate =
                    result.getCalculatedAt();

            OffsetDateTime previousDate =
                    previous == null
                            ? null
                            : previous.getCalculatedAt();

            if (previous == null
                    || (currentDate != null
                    && (previousDate == null
                    || currentDate.isAfter(previousDate)))) {

                latest.put(employeeId, result);
            }
        }

        return latest;
    }

    public KpiStats summarize(
            Set<String> employeeIds
    ) throws Exception {

        Map<String, KpiResult> latest =
                getLatestResults();

        int evaluated = 0;
        int atRisk = 0;
        double totalScore = 0;

        for (String employeeId : employeeIds) {

            KpiResult result = latest.get(employeeId);

            if (result == null) continue;

            evaluated++;
            totalScore += result.getOverallKpiScore();

            if ("At Risk".equalsIgnoreCase(
                    result.getPerformanceStatus())) {
                atRisk++;
            }
        }

        Double average = evaluated == 0
                ? null
                : totalScore / evaluated;

        return new KpiStats(
                evaluated,
                atRisk,
                average
        );
    }

    public PersonalKpi personal(
            String employeeId
    ) throws Exception {

        KpiResult result =
                getLatestResults().get(employeeId);

        if (result == null) {
            return new PersonalKpi(
                    null,
                    "No KPI evaluation yet"
            );
        }

        return new PersonalKpi(
                result.getOverallKpiScore(),
                result.getPerformanceStatus()
        );
    }
}
