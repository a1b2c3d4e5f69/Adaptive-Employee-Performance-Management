package com.aepm.data.repository;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;
import com.aepm.logic.ai.AIPerformanceAnalysisService.PerformanceAnalysis;
import com.aepm.util.SessionManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.UUID;

public class AIPerformanceSnapshotRepository {

    public JsonObject save(
            PerformanceAnalysis analysis
    ) throws Exception {

        if (analysis == null) {
            throw new IllegalArgumentException(
                    "Performance analysis cannot be null."
            );
        }

        UUID employeeId = analysis.employeeId();
        LocalDate start = analysis.periodStart();
        LocalDate end = analysis.periodEnd();

        if (employeeId == null || start == null
                || end == null || end.isBefore(start)) {
            throw new IllegalArgumentException(
                    "Invalid performance snapshot period."
            );
        }

        if (!analysis.sufficientData()) {
            throw new IllegalStateException(
                    "Insufficient performance data. "
                    + "Snapshot was not saved."
            );
        }

        /*
         * IMPORTANT:
         * The current analysis service retrieves the latest
         * KPI ending before the requested period end.
         *
         * That does not prove the KPI belongs to this
         * exact reporting period.
         *
         * Therefore, do not persist it as period KPI yet.
         */
        JsonObject payload = new JsonObject();

        payload.addProperty(
                "employee_id", employeeId.toString()
        );

        payload.addProperty(
                "period_start", start.toString()
        );

        payload.addProperty(
                "period_end", end.toString()
        );

        Double verifiedKpi = analysis.kpiScore();

        if (verifiedKpi == null) {
            payload.add(
                    "kpi_score",
                    com.google.gson.JsonNull.INSTANCE
            );
        } else {
            if (!Double.isFinite(verifiedKpi)
                    || verifiedKpi < 0
                    || verifiedKpi > 100) {
                throw new IllegalStateException(
                        "Cannot save an invalid KPI score."
                );
            }

            payload.addProperty("kpi_score", verifiedKpi);
        }

        if (analysis.taskCompletionRate() != null) {
            payload.addProperty(
                    "task_completion_rate",
                    analysis.taskCompletionRate()
            );
        } else {
            payload.add(
                    "task_completion_rate",
                    com.google.gson.JsonNull.INSTANCE
            );
        }

        // Attendance rate requires expected working days,
        // holidays and applicable leave information.
        payload.add(
                "attendance_rate",
                com.google.gson.JsonNull.INSTANCE
        );

        payload.addProperty(
                "total_tasks", analysis.totalTasks()
        );

        payload.addProperty(
                "completed_tasks", analysis.completedTasks()
        );

        // The current analysis does not yet provide a
        // verified distinct missed-deadline count.
        payload.add(
            "missed_deadlines",
            com.google.gson.JsonNull.INSTANCE
        );

        // Actual number of escalation records found.
        payload.addProperty(
                "escalation_count",
                analysis.escalationCount()
        );

        JsonObject extra = new JsonObject();

        extra.addProperty(
            "analysis_version", "3.6-preliminary"
        );

        extra.addProperty(
            "task_cohort",
            "tasks_assigned_within_reporting_period"
        );

        extra.addProperty(
            "attendance_records",
            analysis.attendanceRecords()
        );

        extra.addProperty(
            "days_with_check_in",
            analysis.daysWithCheckIn()
        );

        extra.addProperty(
            "highest_escalation_level",
            analysis.highestEscalationLevel()
        );

        extra.addProperty(
            "escalation_event_count",
            analysis.escalationCount()
        );

        extra.addProperty(
            "missed_deadlines_verified", false
        );

        // The updated analyse() method supplies only exact-period KPI.
        extra.addProperty(
            "kpi_period_verified",
            verifiedKpi != null
        );

        extra.addProperty(
            "attendance_rate_verified", false
        );

        extra.addProperty(
            "historical_task_state_verified", false
        );

        boolean eligibleForKpiOnly =
                verifiedKpi != null
                && analysis.totalTasks() == 0
                && analysis.completedTasks() == 0;

        extra.addProperty(
                "eligible_for_ai_generation",
                eligibleForKpiOnly
        );

        extra.addProperty(
                "ai_analysis_scope",
                eligibleForKpiOnly ? "kpi_only" : "not_eligible"
        );

        extra.addProperty(
            "data_quality_note",
            "KPI is included only when matched to the exact "
            + "reporting period. Attendance denominator, "
            + "historical task state and distinct missed "
            + "deadline count still require verification."
        );

        if (analysis.averageTaskProgress() != null) {
            extra.addProperty(
                    "average_task_progress",
                    analysis.averageTaskProgress()
            );
        }

        payload.add("additional_metrics", extra);

        if (eligibleForKpiOnly) {
            // Validate the complete snapshot before saving it as eligible.
            com.aepm.logic.ai.AIPerformanceDataValidator
                    .validateForGeneration(payload);
        }

        String url = AppConfig.REST_URL
                + "/ai_performance_snapshot_tbl"
                + "?on_conflict=employee_id,period_start,period_end"
                + "&select=*";

        HttpRequest request =
                SupabaseClient.getInstance()
                .createAuthenticatedRequest(
                        url,
                        SessionManager.getAccessToken()
                )
                .header(
                        "Prefer",
                        "resolution=merge-duplicates,"
                        + "return=representation"
                )
                .POST(
                        HttpRequest.BodyPublishers.ofString(
                                payload.toString()
                        )
                )
                .build();

        HttpResponse<String> response =
                SupabaseClient.getInstance()
                .getHttpClient()
                .send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {
            throw new IllegalStateException(
                    "Failed to save AI snapshot (HTTP "
                    + response.statusCode()
                    + "): " + response.body()
            );
        }

        JsonArray rows = JsonParser
                .parseString(response.body())
                .getAsJsonArray();

        if (rows.isEmpty()) {
            throw new IllegalStateException(
                    "Snapshot request returned no record. "
                    + "Check Supabase RLS permissions."
            );
        }

        return rows.get(0).getAsJsonObject();
    }
}
