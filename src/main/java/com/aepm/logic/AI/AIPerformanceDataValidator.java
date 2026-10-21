package com.aepm.logic.ai;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import com.google.gson.JsonElement;

public final class AIPerformanceDataValidator {

    private static final ZoneId ZONE =
            ZoneId.of("Asia/Kuala_Lumpur");

    private AIPerformanceDataValidator() {
    }

    public static JsonObject validateForGeneration(
            JsonObject snapshot
    ) {
        if (snapshot == null) {
            throw invalid("Performance snapshot is required.");
        }

        // Validate a copy so the caller's object is not modified.
        JsonObject data = snapshot.deepCopy();

        String employeeId = requiredString(data, "employee_id");

        try {
            UUID parsed = UUID.fromString(employeeId);

            if (!parsed.toString().equalsIgnoreCase(employeeId)) {
                throw invalid("employee_id must be a canonical UUID.");
            }
        } catch (IllegalArgumentException ex) {
            throw invalid("employee_id must be a valid UUID.");
        }

        LocalDate start = requiredDate(data, "period_start");
        LocalDate end = requiredDate(data, "period_end");

        if (end.isBefore(start)) {
            throw invalid("period_end cannot precede period_start.");
        }

        if (end.isAfter(LocalDate.now(ZONE))) {
            throw invalid("period_end cannot be in the future.");
        }

        JsonElement scopeMetrics = data.get("additional_metrics");

        if (scopeMetrics != null && scopeMetrics.isJsonObject()) {
            JsonObject metrics = scopeMetrics.getAsJsonObject();
            JsonElement scope = metrics.get("ai_analysis_scope");

            if (scope != null
                    && scope.isJsonPrimitive()
                    && scope.getAsJsonPrimitive().isString()
                    && "kpi_only".equals(scope.getAsString())) {

                validateKpiOnly(data, metrics);
                return data;
            }
        }

        int total = requiredCount(data, "total_tasks");
        int completed = requiredCount(data, "completed_tasks");
        requiredCount(data, "escalation_count");

        if (completed > total) {
            throw invalid("completed_tasks cannot exceed total_tasks.");
        }

        Double completionRate =
                optionalPercentage(data, "task_completion_rate");

        optionalPercentage(data, "kpi_score");
        optionalPercentage(data, "attendance_rate");

        if (hasValue(data, "missed_deadlines")) {
            requiredCount(data, "missed_deadlines");
        }

        if (total == 0) {
            if (completionRate != null) {
                throw invalid(
                        "task_completion_rate must be null "
                        + "when total_tasks is zero."
                );
            }
        } else {
            if (completionRate == null) {
                throw invalid(
                        "task_completion_rate is required "
                        + "when total_tasks is greater than zero."
                );
            }

            double expected = Math.round(
                    completed * 100.0 / total * 100.0
            ) / 100.0;

            if (Math.abs(completionRate - expected) > 0.005) {
                throw invalid(
                        "task_completion_rate does not match "
                        + "completed_tasks / total_tasks."
                );
            }
        }

        JsonElement extraElement = data.get("additional_metrics");

        if (extraElement == null || !extraElement.isJsonObject()) {
            throw invalid("additional_metrics must be a JSON object.");
        }

        JsonObject extra = extraElement.getAsJsonObject();

        boolean eligible = requiredBoolean(
                extra, "eligible_for_ai_generation"
        );

        boolean kpiVerified = requiredBoolean(
                extra, "kpi_period_verified"
        );

        boolean attendanceVerified = requiredBoolean(
                extra, "attendance_rate_verified"
        );

        boolean deadlinesVerified = requiredBoolean(
                extra, "missed_deadlines_verified"
        );

        boolean taskStateVerified = requiredBoolean(
                extra, "historical_task_state_verified"
        );

        requireVerifiedMetric(data, "kpi_score", kpiVerified);
        requireVerifiedMetric(
                data, "attendance_rate", attendanceVerified
        );
        requireVerifiedMetric(
                data, "missed_deadlines", deadlinesVerified
        );

        // Counts alone do not prove historical task state.
        if (total > 0 && !taskStateVerified) {
            throw invalid(
                    "Task state has not been verified "
                    + "for the reporting period."
            );
        }

        Double averageProgress = optionalPercentage(
                extra, "average_task_progress"
        );

        if (averageProgress != null
                && (total == 0 || !taskStateVerified)) {
            throw invalid(
                    "average_task_progress requires tasks "
                    + "and verified task state."
            );
        }

        if (hasValue(extra, "attendance_records")) {
            int records = requiredCount(
                    extra, "attendance_records"
            );

            if (hasValue(extra, "days_with_check_in")
                    && requiredCount(
                            extra, "days_with_check_in"
                    ) > records) {
                throw invalid(
                        "days_with_check_in cannot exceed "
                        + "attendance_records."
                );
            }
        } else if (hasValue(extra, "days_with_check_in")) {
            throw invalid(
                    "days_with_check_in requires attendance_records."
            );
        }

        if (hasValue(extra, "highest_escalation_level")) {
            requiredCount(extra, "highest_escalation_level");
        }

        if (hasValue(extra, "escalation_event_count")
                && requiredCount(extra, "escalation_event_count")
                != requiredCount(data, "escalation_count")) {
            throw invalid(
                    "escalation_event_count conflicts "
                    + "with escalation_count."
            );
        }

        boolean hasTaskEvidence = total > 0 && taskStateVerified;
        boolean hasKpiEvidence =
                hasValue(data, "kpi_score") && kpiVerified;

        if (!hasTaskEvidence && !hasKpiEvidence) {
            throw invalid(
                    "Insufficient verified task or KPI evidence."
            );
        }

        if (!eligible) {
            throw invalid(
                    "This snapshot is preliminary and "
                    + "is not eligible for AI generation."
            );
        }

        return data;
    }

    private static void requireVerifiedMetric(
            JsonObject data,
            String key,
            boolean verified
    ) {
        if (hasValue(data, key) && !verified) {
            throw invalid(key + " contains an unverified value.");
        }

        if (verified && !hasValue(data, key)) {
            throw invalid(
                    key + " is marked verified but has no value."
            );
        }
    }

    private static String requiredString(
            JsonObject object,
            String key
    ) {
        JsonElement value = object.get(key);

        if (value == null
                || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isString()
                || value.getAsString().isBlank()) {
            throw invalid(key + " must be a nonblank string.");
        }

        return value.getAsString();
    }

    private static LocalDate requiredDate(
            JsonObject object,
            String key
    ) {
        String value = requiredString(object, key);

        try {
            LocalDate date = LocalDate.parse(value);

            if (!date.toString().equals(value)) {
                throw invalid(key + " must use YYYY-MM-DD.");
            }

            return date;
        } catch (java.time.format.DateTimeParseException ex) {
            throw invalid(key + " must be a valid YYYY-MM-DD date.");
        }
    }

    private static boolean requiredBoolean(
            JsonObject object,
            String key
    ) {
        JsonElement value = object.get(key);

        if (value == null
                || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isBoolean()) {
            throw invalid(key + " must be a JSON boolean.");
        }

        return value.getAsBoolean();
    }

    private static BigDecimal requiredNumber(
            JsonObject object,
            String key
    ) {
        JsonElement value = object.get(key);

        if (value == null || !value.isJsonPrimitive()) {
            throw invalid(key + " must be a JSON number.");
        }

        JsonPrimitive primitive = value.getAsJsonPrimitive();

        if (!primitive.isNumber()) {
            throw invalid(key + " must be a JSON number.");
        }

        try {
            // Rejects NaN and infinity as well.
            return new BigDecimal(primitive.getAsString());
        } catch (NumberFormatException ex) {
            throw invalid(key + " must be a finite number.");
        }
    }

    private static int requiredCount(
            JsonObject object,
            String key
    ) {
        BigDecimal value = requiredNumber(object, key);

        try {
            int count = value.intValueExact();

            if (count < 0) {
                throw invalid(key + " cannot be negative.");
            }

            return count;
        } catch (ArithmeticException ex) {
            throw invalid(
                    key + " must be a whole number "
                    + "within the Java int range."
            );
        }
    }

    private static Double optionalPercentage(
            JsonObject object,
            String key
    ) {
        if (!hasValue(object, key)) {
            return null;
        }

        BigDecimal value = requiredNumber(object, key);

        if (value.compareTo(BigDecimal.ZERO) < 0
                || value.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw invalid(key + " must be between 0 and 100.");
        }

        return value.doubleValue();
    }

    private static boolean hasValue(
            JsonObject object,
            String key
    ) {
        JsonElement value = object.get(key);
        return value != null && !value.isJsonNull();
    }

    private static IllegalArgumentException invalid(
            String message
    ) {
        return new IllegalArgumentException(
                "Performance data validation failed: " + message
        );
    }

    private static void validateKpiOnly(
            JsonObject data,
            JsonObject metrics
    ) {
        Double score = optionalPercentage(data, "kpi_score");

        if (score == null) {
            throw invalid("KPI-only analysis requires a KPI score.");
        }

        JsonElement breakdownElement = metrics.get("kpi_breakdown");

        if (breakdownElement == null
                || !breakdownElement.isJsonObject()) {
            throw invalid(
                    "KPI breakdown is missing. Generate a new snapshot."
            );
        }

        JsonObject breakdown = breakdownElement.getAsJsonObject();

        for (String field : new String[] {
                "productivity_score",
                "timeliness_score",
                "completion_rate",
                "attendance_score"
        }) {
            JsonElement value = breakdown.get(field);

            if (value == null
                    || !value.isJsonPrimitive()
                    || !value.getAsJsonPrimitive().isNumber()) {
                throw invalid("KPI breakdown requires a numeric " + field + ".");
            }

            double number = value.getAsDouble();

            if (!Double.isFinite(number) || number < 0 || number > 100) {
                throw invalid(
                        "KPI breakdown " + field
                        + " must be between 0 and 100."
                );
            }
        }

        if (!requiredBoolean(metrics, "kpi_period_verified")) {
            throw invalid("KPI has not been matched to the reporting period.");
        }

        if (!requiredBoolean(metrics, "eligible_for_ai_generation")) {
            throw invalid("Snapshot is not eligible for AI generation.");
        }

        String[] excludedFields = {
                "total_tasks",
                "completed_tasks",
                "task_completion_rate",
                "attendance_rate",
                "missed_deadlines",
                "escalation_count"
        };

        for (String key : excludedFields) {
            if (hasValue(data, key)) {
                throw invalid(
                        "KPI-only snapshot must leave " + key
                        + " unknown. Recalculate this snapshot."
                );
            }
        }

        if (hasValue(metrics, "average_task_progress")) {
            throw invalid(
                    "KPI-only analysis must not include task progress."
            );
        }

        if (requiredBoolean(metrics, "historical_task_state_verified")
                || requiredBoolean(metrics, "attendance_rate_verified")
                || requiredBoolean(metrics, "missed_deadlines_verified")) {
            throw invalid(
                    "KPI-only analysis must not mark excluded metrics as verified."
            );
        }
    }
}