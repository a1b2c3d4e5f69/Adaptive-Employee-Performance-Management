package com.aepm.logic.ai;

import com.aepm.config.AppConfig;
import com.aepm.data.repository.AITaskMetricsRepository;
import com.aepm.data.supabase.SupabaseClient;
import com.aepm.util.SessionManager;
import com.google.gson.*;
import com.aepm.data.repository.AIPerformanceSnapshotRepository;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

public class AIPerformanceAnalysisService {

    private final AITaskMetricsRepository taskRepository =
            new AITaskMetricsRepository();

    public record PerformanceAnalysis(
            UUID employeeId,
            LocalDate periodStart,
            LocalDate periodEnd,
            Double kpiScore,
            Double previousKpiScore,
            Double kpiChange,
            int totalTasks,
            int completedTasks,
            Double taskCompletionRate,
            Double averageTaskProgress,
            int attendanceRecords,
            int daysWithCheckIn,
            int escalationCount,
            int highestEscalationLevel,
            int missedDeadlineEvents,
            boolean sufficientData
    ) {}

    public PerformanceAnalysis analyse(
            UUID employeeId,
            LocalDate start,
            LocalDate end
    ) throws Exception {

        if (employeeId == null || start == null
                || end == null || end.isBefore(start)) {
            throw new IllegalArgumentException(
                    "Invalid employee or reporting period."
            );
        }

        if (end.isAfter(LocalDate.now(
                java.time.ZoneId.of("Asia/Kuala_Lumpur")))) {
            throw new IllegalArgumentException(
                    "Reporting period cannot end in the future."
            );
        }

        // 1. Actual task metrics
        var tasks = taskRepository.calculate(
                employeeId, start, end
        );

        // 2. KPI results matched to the exact reporting period.
        JsonArray kpiRows = getRows(
                "kpi_result_tbl",
                "select=overall_kpi_score,evaluation_start_date,"
                + "evaluation_end_date"
                + "&employee_id=eq." + employeeId
                + "&evaluation_start_date=eq." + start
                + "&evaluation_end_date=eq." + end
                + "&limit=2"
        );

        if (kpiRows.size() > 1) {
            throw new IllegalStateException(
                    "Multiple KPI results exist for this employee "
                    + "and reporting period. Resolve the duplicate "
                    + "before generating performance analysis."
            );
        }

        Double currentKpi = null;

        if (!kpiRows.isEmpty()) {
            JsonObject row = kpiRows.get(0).getAsJsonObject();

            if (!start.toString().equals(
                    getRequiredKpiString(row, "evaluation_start_date"))
                    || !end.toString().equals(
                            getRequiredKpiString(
                                    row, "evaluation_end_date"))) {
                throw new IllegalStateException(
                        "Returned KPI dates do not match "
                        + "the requested reporting period."
                );
            }

            currentKpi = readValidatedKpiScore(row);
        }

        // A comparable previous period has not been established.
        // Preserve unknown comparison values as null.
        Double previousKpi = null;
        Double kpiChange = null;

        // 3. Attendance records
        JsonArray attendanceRows = getRows(
                "attendance_record_tbl",
                "select=attendance_id,attendance_date,"
                + "check_in_time,attendance_status"
                + "&employee_id=eq." + employeeId
                + "&attendance_date=gte." + start
                + "&attendance_date=lte." + end
                + "&limit=1000"
        );

        if (attendanceRows.size() >= 1000) {
            throw new IllegalStateException(
                    "Attendance result limit reached. "
                    + "Pagination is required."
            );
        }

        int daysWithCheckIn = 0;
        java.util.Set<String> attendedDates =
                new java.util.HashSet<>();

        for (JsonElement element : attendanceRows) {
            JsonObject row = element.getAsJsonObject();

            if (hasValue(row, "check_in_time")
                    && hasValue(row, "attendance_date")) {
                attendedDates.add(
                        row.get("attendance_date").getAsString()
                );
            }
        }

        daysWithCheckIn = attendedDates.size();

        // 4. Actual escalation events
        JsonArray escalationRows = getRows(
                "task_escalation_tbl",
                "select=escalation_id,current_level,"
                + "missed_deadline_count,triggered_at"
                + "&employee_id=eq." + employeeId
                + "&triggered_at=gte."
                + encode(start + "T00:00:00+08:00")
                + "&triggered_at=lt."
                + encode(end.plusDays(1) + "T00:00:00+08:00")
                + "&limit=1000"
        );

        if (escalationRows.size() >= 1000) {
            throw new IllegalStateException(
                    "Escalation result limit reached. "
                    + "Pagination is required."
            );
        }

        int highestLevel = 0;
        int missedEvents = 0;

        for (JsonElement element : escalationRows) {
            JsonObject row = element.getAsJsonObject();

            highestLevel = Math.max(
                    highestLevel,
                    nullableInt(row, "current_level", 0)
            );

            // Each row represents an escalation event.
            // Do not sum cumulative missed_deadline_count.
            missedEvents++;
        }

        // Zero tasks and no KPI are treated as
        // insufficient performance evidence.
        boolean sufficientData =
                tasks.totalTasks() > 0
                || currentKpi != null;

        return new PerformanceAnalysis(
                employeeId,
                start,
                end,
                currentKpi,
                previousKpi,
                kpiChange,
                tasks.totalTasks(),
                tasks.completedTasks(),
                tasks.totalTasks() == 0
                        ? null
                        : tasks.completionRate(),
                tasks.totalTasks() == 0
                        ? null
                        : tasks.averageProgress(),
                attendanceRows.size(),
                daysWithCheckIn,
                escalationRows.size(),
                highestLevel,
                missedEvents,
                sufficientData
        );
    }

    private JsonArray getRows(
            String table,
            String query
    ) throws Exception {

        String url = AppConfig.REST_URL
                + "/" + table + "?" + query;

        HttpRequest request =
                SupabaseClient.getInstance()
                .createAuthenticatedRequest(
                        url,
                        SessionManager.getAccessToken()
                )
                .GET()
                .build();

        HttpResponse<String> response =
                SupabaseClient.getInstance()
                .getHttpClient()
                .send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        if (response.statusCode() != 200) {
            throw new IllegalStateException(
                    "Cannot read " + table
                    + " (HTTP " + response.statusCode()
                    + "): " + response.body()
            );
        }

        return JsonParser.parseString(
                response.body()
        ).getAsJsonArray();
    }

    private boolean hasValue(
            JsonObject obj,
            String key
    ) {
        return obj.has(key)
                && !obj.get(key).isJsonNull();
    }

    private Double nullableDouble(
            JsonObject obj,
            String key
    ) {
        return hasValue(obj, key)
                ? obj.get(key).getAsDouble()
                : null;
    }

    private int nullableInt(
            JsonObject obj,
            String key,
            int fallback
    ) {
        return hasValue(obj, key)
                ? obj.get(key).getAsInt()
                : fallback;
    }

    private String encode(String value) {
        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8
        );
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public JsonObject analyseAndSave(
            UUID employeeId,
            LocalDate start,
            LocalDate end
    ) throws Exception {

        PerformanceAnalysis analysis =
                analyse(employeeId, start, end);

        AIPerformanceSnapshotRepository repository =
                new AIPerformanceSnapshotRepository();

        return repository.save(analysis);
    }

    private String getRequiredKpiString(
        JsonObject row,
        String key
    ) {
        JsonElement value = row.get(key);

        if (value == null
                || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isString()
                || value.getAsString().isBlank()) {
            throw new IllegalStateException(
                    "KPI validation failed: "
                    + key + " must be a nonblank string."
            );
        }

        return value.getAsString();
    }

    private Double readValidatedKpiScore(JsonObject row) {
        JsonElement value = row.get("overall_kpi_score");

        if (value == null || value.isJsonNull()) {
            return null;
        }

        if (!value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalStateException(
                    "KPI validation failed: "
                    + "overall_kpi_score must be a JSON number."
            );
        }

        java.math.BigDecimal score;

        try {
            score = new java.math.BigDecimal(value.getAsString());
        } catch (NumberFormatException ex) {
            throw new IllegalStateException(
                    "KPI validation failed: "
                    + "overall_kpi_score must be finite.",
                    ex
            );
        }

        if (score.compareTo(java.math.BigDecimal.ZERO) < 0
                || score.compareTo(
                        java.math.BigDecimal.valueOf(100)) > 0) {
            throw new IllegalStateException(
                    "KPI validation failed: "
                    + "overall_kpi_score must be between 0 and 100."
            );
        }

        return score.doubleValue();
    }

    public JsonObject analyseKpiAndSave(
            UUID employeeId,
            LocalDate start,
            LocalDate end,
            Long kpiResultId
    ) throws Exception {

        if (kpiResultId == null || kpiResultId <= 0) {
            throw new IllegalArgumentException(
                    "A valid selected KPI result ID is required."
            );
        }

        if (employeeId == null || start == null || end == null
                || end.isBefore(start)) {
            throw new IllegalArgumentException(
                    "Invalid employee or reporting period."
            );
        }

        if (end.isAfter(LocalDate.now(
                java.time.ZoneId.of("Asia/Kuala_Lumpur")))) {
            throw new IllegalArgumentException(
                    "Reporting period cannot end in the future."
            );
        }

        JsonArray rows = getRows(
                "kpi_result_tbl",
                "select=result_id,overall_kpi_score,"
                + "productivity_score,timeliness_score,"
                + "completion_rate,attendance_score,"
                + "evaluation_start_date,evaluation_end_date"
                + "&result_id=eq." + kpiResultId
                + "&employee_id=eq." + employeeId
                + "&evaluation_start_date=eq." + start
                + "&evaluation_end_date=eq." + end
                + "&limit=2"
        );

        if (rows.isEmpty()) {
            throw new IllegalStateException(
                    "The selected KPI result is unavailable or does not match "
                    + "this employee and evaluation period. Refresh and select again."
            );
        }

        if (rows.size() != 1) {
            throw new IllegalStateException(
                    "Expected exactly one selected KPI result."
            );
        }

        JsonObject row = rows.get(0).getAsJsonObject();

        if (!start.toString().equals(
                getRequiredKpiString(row, "evaluation_start_date"))
                || !end.toString().equals(
                        getRequiredKpiString(row, "evaluation_end_date"))) {
            throw new IllegalStateException(
                    "KPI dates do not match the requested reporting period."
            );
        }

        Double score = readValidatedKpiScore(row);

        if (score == null) {
            throw new IllegalStateException(
                    "The matching KPI result has no score."
            );
        }

        JsonObject payload = new JsonObject();

        payload.addProperty("employee_id", employeeId.toString());
        payload.addProperty("period_start", start.toString());
        payload.addProperty("period_end", end.toString());
        payload.addProperty("kpi_score", score);

        for (String key : new String[] {
                "total_tasks",
                "completed_tasks",
                "task_completion_rate",
                "attendance_rate",
                "missed_deadlines",
                "escalation_count"
        }) {
            payload.add(key, JsonNull.INSTANCE);
        }

        JsonObject metrics = new JsonObject();

        metrics.addProperty("analysis_version", "kpi-breakdown-v1");

        JsonObject breakdown = new JsonObject();

        for (String field : new String[] {
                "productivity_score",
                "timeliness_score",
                "completion_rate",
                "attendance_score"
        }) {
            JsonElement value = row.get(field);

            if (value == null || value.isJsonNull()) {
                throw new IllegalStateException(
                        "The saved KPI result is missing " + field
                        + ". Recalculate the KPI first."
                );
            }

            breakdown.add(field, value.deepCopy());
        }

        metrics.add("kpi_breakdown", breakdown);
        metrics.addProperty("ai_analysis_scope", "kpi_only");
        metrics.addProperty("kpi_period_verified", true);
        metrics.addProperty("eligible_for_ai_generation", true);

        metrics.addProperty("historical_task_state_verified", false);
        metrics.addProperty("attendance_rate_verified", false);
        metrics.addProperty("missed_deadlines_verified", false);

        metrics.addProperty("verified_kpi_records", 1);
        metrics.add("kpi_result_id", row.get("result_id").deepCopy());

        metrics.addProperty(
                "data_quality_note",
                "Uses the saved KPI result and component scores for "
                + "the exact reporting period. "
                + "Attendance score represents recorded attendance rate, "
                + "not punctuality. Daily attendance records, late-day counts, "
                + "early departures, grading targets and historical trends "
                + "are not included."
        );

        payload.add("additional_metrics", metrics);

        // Validate before writing an eligible snapshot.
        payload = AIPerformanceDataValidator.validateForGeneration(
                payload
        );

        // Refresh the snapshot for this employee and reporting period.
        payload.addProperty(
                "calculated_at",
                java.time.Instant.now().toString()
        );

        String url = AppConfig.REST_URL
                + "/ai_performance_snapshot_tbl"
                + "?on_conflict=employee_id,period_start,period_end"
                + "&select=*";

        var client = SupabaseClient.getInstance();

        HttpRequest request = client
                .createAuthenticatedRequest(
                        url,
                        SessionManager.getAccessToken()
                )
                .header("Content-Type", "application/json")
                .header(
                        "Prefer",
                        "resolution=merge-duplicates,return=representation"
                )
                .timeout(java.time.Duration.ofSeconds(30))
                .POST(
                        HttpRequest.BodyPublishers.ofString(
                                payload.toString()
                        )
                )
                .build();

        HttpResponse<String> response = client.getHttpClient().send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {
            throw new IllegalStateException(
                    "Unable to save KPI snapshot (HTTP "
                    + response.statusCode() + "): "
                    + response.body()
            );
        }

        JsonElement parsed = JsonParser.parseString(response.body());

        if (!parsed.isJsonArray()) {
            throw new IllegalStateException(
                    "Snapshot save returned an invalid response."
            );
        }

        JsonArray savedRows = parsed.getAsJsonArray();

        if (savedRows.size() != 1
                || !savedRows.get(0).isJsonObject()) {
            throw new IllegalStateException(
                    "Expected one saved snapshot. Check database permissions."
            );
        }

        return savedRows.get(0).getAsJsonObject();
    }
}
