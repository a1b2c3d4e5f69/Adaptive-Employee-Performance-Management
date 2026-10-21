package com.aepm.logic.ai;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

public final class AIValidationSmokeTest {

    private AIValidationSmokeTest() {
    }

    public static void main(String[] args) {

        expectAccepted(
                "Valid KPI-only snapshot",
                validSnapshot()
        );

        JsonObject preliminary = validSnapshot();
        preliminary.getAsJsonObject("additional_metrics")
                .addProperty("eligible_for_ai_generation", false);

        expectRejected(
                "Preliminary snapshot",
                preliminary
        );

        JsonObject unverifiedKpi = validSnapshot();
        unverifiedKpi.getAsJsonObject("additional_metrics")
                .addProperty("kpi_period_verified", false);

        expectRejected(
                "Unverified KPI",
                unverifiedKpi
        );

        JsonObject invalidKpi = validSnapshot();
        invalidKpi.addProperty("kpi_score", 101);

        expectRejected(
                "KPI above 100",
                invalidKpi
        );

        JsonObject invalidTaskCounts = validSnapshot();
        invalidTaskCounts.addProperty("completed_tasks", 1);

        expectRejected(
                "Completed tasks exceed total tasks",
                invalidTaskCounts
        );

        JsonObject invalidRate = validSnapshot();
        invalidRate.addProperty("task_completion_rate", 0);

        expectRejected(
                "Zero tasks must have a null completion rate",
                invalidRate
        );

        JsonObject invalidPeriod = validSnapshot();
        invalidPeriod.addProperty("period_start", "2025-02-01");

        expectRejected(
                "Reporting dates are reversed",
                invalidPeriod
        );

        JsonObject invalidFlag = validSnapshot();
        invalidFlag.getAsJsonObject("additional_metrics")
                .addProperty("eligible_for_ai_generation", "true");

        expectRejected(
                "Eligibility must be a boolean",
                invalidFlag
        );

        JsonObject missingEvidence = validSnapshot();
        missingEvidence.add("kpi_score", JsonNull.INSTANCE);
        missingEvidence.getAsJsonObject("additional_metrics")
                .addProperty("kpi_period_verified", false);

        expectRejected(
                "No task or KPI evidence",
                missingEvidence
        );

        System.out.println();
        System.out.println("All 9 validation checks passed.");
    }

    private static JsonObject validSnapshot() {

        JsonObject snapshot = new JsonObject();

        // Synthetic test identity; never save this fixture.
        snapshot.addProperty(
                "employee_id",
                "11111111-1111-4111-8111-111111111111"
        );

        snapshot.addProperty("period_start", "2025-01-01");
        snapshot.addProperty("period_end", "2025-01-31");

        snapshot.addProperty("kpi_score", 75.0);
        snapshot.add("total_tasks", JsonNull.INSTANCE);
        snapshot.add("completed_tasks", JsonNull.INSTANCE);
        snapshot.add("escalation_count", JsonNull.INSTANCE);

        snapshot.add("task_completion_rate", JsonNull.INSTANCE);
        snapshot.add("attendance_rate", JsonNull.INSTANCE);
        snapshot.add("missed_deadlines", JsonNull.INSTANCE);

        JsonObject extra = new JsonObject();

        extra.addProperty("eligible_for_ai_generation", true);
        extra.addProperty("kpi_period_verified", true);
        extra.addProperty("attendance_rate_verified", false);
        extra.addProperty("missed_deadlines_verified", false);
        extra.addProperty("historical_task_state_verified", false);
        extra.addProperty("ai_analysis_scope", "kpi_only");

        snapshot.add("additional_metrics", extra);

        return snapshot;
    }

    private static void expectAccepted(
            String name,
            JsonObject snapshot
    ) {
        try {
            AIPerformanceDataValidator.validateForGeneration(
                    snapshot
            );
        } catch (RuntimeException ex) {
            throw new AssertionError(
                    "FAIL: " + name + " should have passed.",
                    ex
            );
        }

        System.out.println("PASS: " + name);
    }

    private static void expectRejected(
            String name,
            JsonObject snapshot
    ) {
        try {
            AIPerformanceDataValidator.validateForGeneration(
                    snapshot
            );
        } catch (IllegalArgumentException ex) {
            System.out.println("PASS: " + name);
            return;
        }

        throw new AssertionError(
                "FAIL: " + name + " should have been rejected."
        );
    }
}