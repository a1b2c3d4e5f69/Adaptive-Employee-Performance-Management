package com.aepm.data.repository;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;
import com.aepm.util.SessionManager;
import com.google.gson.*;

import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.UUID;
import java.math.BigDecimal;

public class AITaskMetricsRepository {

    private static final ZoneId ZONE =
            ZoneId.of("Asia/Kuala_Lumpur");

    public record TaskMetrics(
            int totalTasks,
            int completedTasks,
            int overdueTasks,
            int completedLateTasks,
            double completionRate,
            double averageProgress
    ) {}

    public TaskMetrics calculate(
            UUID employeeId,
            LocalDate startDate,
            LocalDate endDate
    ) throws Exception {

        if (employeeId == null || startDate == null
                || endDate == null || endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "Invalid employee or analysis period."
            );
        }

        LocalDate today = LocalDate.now(ZONE);

        if (endDate.isAfter(today)) {
            throw new IllegalArgumentException(
                    "Analysis period cannot end in the future."
            );
        }

        String start = startDate.atStartOfDay(ZONE)
                .toOffsetDateTime().toString();

        String endExclusive = endDate.plusDays(1)
                .atStartOfDay(ZONE)
                .toOffsetDateTime().toString();

        String url = AppConfig.REST_URL + "/task_tbl"
                + "?select=task_id,task_status,progress,"
                + "assigned_date,due_date,completion_date"
                + "&assigned_employee_id=eq." + employeeId
                + "&assigned_date=gte." + encode(start)
                + "&assigned_date=lt." + encode(endExclusive)
                + "&limit=1000";

        HttpRequest request = SupabaseClient.getInstance()
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
                    "Failed to load tasks: "
                    + response.statusCode()
                    + " " + response.body()
            );
        }

        JsonArray tasks = JsonParser
                .parseString(response.body())
                .getAsJsonArray();

        if (tasks.size() >= 1000) {
            throw new IllegalStateException(
                    "Task data reached query limit. "
                    + "Pagination is required."
            );
        }

        int total = 0;
        int completed = 0;
        int overdue = 0;
        int completedLate = 0;
        double progressSum = 0;

        Instant periodEnd = endDate.plusDays(1)
                .atStartOfDay(ZONE)
                .toInstant();

        Instant now = Instant.now();

        for (JsonElement element : tasks) {

            JsonObject task = element.getAsJsonObject();

            String status = getString(
                    task, "task_status"
            ).trim();

            if ("Cancelled".equalsIgnoreCase(status)) {
                // No cancellation timestamp is available.
                // Historical cancellation status cannot be reconstructed.
                if (endDate.isBefore(today)) {
                    throw new IllegalStateException(
                            "Historical task analysis requires "
                            + "cancellation event timestamps."
                    );
                }
                continue;
            }

            if (status.isBlank()) {
                throw new IllegalStateException(
                        "Task data validation failed: task_status is required."
                );
            }

            Instant completion = parseInstant(
                    getString(task, "completion_date")
            );

            Instant due = parseInstant(
                    getString(task, "due_date")
            );

            Instant assigned = parseInstant(
                getString(task, "assigned_date")
            );

            if (assigned == null) {
                throw new IllegalStateException(
                        "Task data validation failed: assigned_date is required."
                );
            }

            Instant periodStart = startDate.atStartOfDay(ZONE).toInstant();

            if (assigned.isBefore(periodStart)
                    || !assigned.isBefore(periodEnd)) {
                throw new IllegalStateException(
                        "Task data validation failed: assigned_date "
                        + "is outside the requested reporting period."
                );
            }

            if (assigned.isAfter(now)) {
                throw new IllegalStateException(
                        "Task data validation failed: assigned_date "
                        + "cannot be in the future."
                );
            }

            if (completion != null) {
                if (completion.isBefore(assigned)) {
                    throw new IllegalStateException(
                            "Task data validation failed: completion_date "
                            + "cannot precede assigned_date."
                    );
                }

                if (completion.isAfter(now)) {
                    throw new IllegalStateException(
                            "Task data validation failed: completion_date "
                            + "cannot be in the future."
                    );
                }
            }

            boolean markedCompleted =
                    "Completed".equalsIgnoreCase(status);

            if (markedCompleted && completion == null) {
                throw new IllegalStateException(
                        "Completed task has no completion_date."
                );
            }

            if (completion != null && !markedCompleted) {
                throw new IllegalStateException(
                        "Task completion date conflicts with status."
                );
            }

            boolean finishedByPeriodEnd =
                    completion != null
                    && completion.isBefore(periodEnd);

            // A task completed after the reporting period
            // was still incomplete at period end.
            boolean finishedLater =
                    completion != null
                    && !completion.isBefore(periodEnd);

            if (finishedLater && endDate.isBefore(today)) {
                throw new IllegalStateException(
                        "Historical progress requires "
                        + "task progress snapshots."
                );
            }

            total++;

            if (finishedByPeriodEnd) {
                completed++;
            }

            if (due != null) {
                if (finishedByPeriodEnd) {
                    if (completion.isAfter(due)) {
                        completedLate++;
                    }
                } else if (
                        due.isBefore(periodEnd)
                        && due.isBefore(now)
                ) {
                    overdue++;
                }
            }

            double progress;

            if (finishedByPeriodEnd) {
                progress = 100;
            } else {
                if (endDate.isBefore(today)) {
                    throw new IllegalStateException(
                            "Historical task progress cannot "
                            + "be calculated from current progress."
                    );
                }

                progress = getDouble(task, "progress");
            }

                progressSum += progress;
        }

        return new TaskMetrics(
                total,
                completed,
                overdue,
                completedLate,
                round(total == 0
                        ? 0
                        : completed * 100.0 / total),
                round(total == 0
                        ? 0
                        : progressSum / total)
        );
    }

    private String encode(String value) {
        return URLEncoder.encode(
                value, StandardCharsets.UTF_8
        );
    }

    private String getString(JsonObject obj, String key) {
        JsonElement value = obj.get(key);

        if (value == null || value.isJsonNull()) {
            return "";
        }

        if (!value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalStateException(
                    "Task data validation failed: "
                    + key + " must be a string."
            );
        }

        return value.getAsString().trim();
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (java.time.format.DateTimeParseException ex) {
            throw new IllegalStateException(
                    "Task data validation failed: timestamps must "
                    + "contain a date, time and UTC offset.",
                    ex
            );
        }
    }

    private double getDouble(JsonObject obj, String key) {
        JsonElement value = obj.get(key);

        if (value == null
                || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalStateException(
                    "Task data validation failed: "
                    + key + " must be a JSON number."
            );
        }

        BigDecimal number;

        try {
            number = new BigDecimal(value.getAsString());
        } catch (NumberFormatException ex) {
            throw new IllegalStateException(
                    "Task data validation failed: "
                    + key + " must be a finite number.",
                    ex
            );
        }

        if (number.compareTo(BigDecimal.ZERO) < 0
                || number.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalStateException(
                    "Task data validation failed: "
                    + key + " must be between 0 and 100."
            );
        }

        return number.doubleValue();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
