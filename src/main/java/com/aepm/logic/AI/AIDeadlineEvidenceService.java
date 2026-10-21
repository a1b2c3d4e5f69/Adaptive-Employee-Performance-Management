package com.aepm.logic.ai;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;
import com.google.gson.*;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.*;
import java.util.UUID;

public final class AIDeadlineEvidenceService {

    private static final ZoneId MYT =
            ZoneId.of("Asia/Kuala_Lumpur");

    public JsonObject load(
            UUID employeeId,
            LocalDate start,
            LocalDate end,
            boolean enabled,
            String accessToken
    ) throws Exception {

        JsonObject evidence = new JsonObject();
        JsonArray records = new JsonArray();

        evidence.addProperty("enabled", enabled);
        evidence.add("records", records);

        if (!enabled) {
            evidence.addProperty("status", "disabled");
            return evidence;
        }

        if (employeeId == null || start == null || end == null
                || end.isBefore(start)
                || accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException(
                    "Valid employee, dates and session are required."
            );
        }

        if (!end.isBefore(LocalDate.now(MYT))) {
            throw new IllegalArgumentException(
                    "Deadline evidence requires a completed evaluation period."
            );
        }

        Instant from = start.atStartOfDay(MYT).toInstant();
        Instant cutoff = end.plusDays(1)
                .atStartOfDay(MYT).toInstant();

        String url = AppConfig.REST_URL
                + "/task_tbl"
                + "?select=task_id,task_title,assigned_employee_id,"
                + "assigned_date,due_date,completion_date"
                + "&assigned_employee_id=eq." + employeeId
                + "&assigned_date=gte." + from
                + "&assigned_date=lt." + cutoff
                + "&order=assigned_date.asc,task_id.asc"
                + "&limit=100";

        var client = SupabaseClient.getInstance();

        HttpRequest request = client.createAuthenticatedRequest(
                url, accessToken
        )
                .header("Prefer", "count=exact")
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        HttpResponse<String> response = client.getHttpClient().send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {
            throw new IllegalStateException(
                    "Unable to load deadline evidence (HTTP "
                    + response.statusCode() + ")."
            );
        }

        JsonElement parsed = JsonParser.parseString(response.body());

        if (!parsed.isJsonArray()) {
            throw new IllegalStateException(
                    "Deadline evidence returned an invalid response."
            );
        }

        JsonArray rows = parsed.getAsJsonArray();

        // Do not silently analyse a truncated set of tasks.
        String range = response.headers()
                .firstValue("Content-Range")
                .orElse("");

        int slash = range.lastIndexOf('/');

        if (slash < 0 || range.endsWith("*")) {
            throw new IllegalStateException(
                    "Unable to verify the total task count."
            );
        }

        long total = Long.parseLong(range.substring(slash + 1));

        if (total > 100 || total != rows.size()) {
            throw new IllegalStateException(
                    "Deadline analysis supports up to 100 tasks per request. "
                    + "Select a smaller evaluation period."
            );
        }

        int late = 0;
        int noCompletion = 0;
        int onTime = 0;
        int notDue = 0;
        int noDeadline = 0;

        for (JsonElement element : rows) {
            JsonObject row = element.getAsJsonObject();

            if (!employeeId.toString().equals(
                    row.get("assigned_employee_id").getAsString())) {
                throw new IllegalStateException(
                        "Deadline evidence contains another employee."
                );
            }

            String taskId = row.get("task_id").getAsString();
            UUID.fromString(taskId);

            Instant assigned = timestamp(row, "assigned_date");
            Instant due = timestamp(row, "due_date");
            Instant completed = timestamp(row, "completion_date");

            if (assigned == null
                    || assigned.isBefore(from)
                    || !assigned.isBefore(cutoff)
                    || (due != null && due.isBefore(assigned))
                    || (completed != null
                        && completed.isBefore(assigned))) {
                throw new IllegalStateException(
                        "Task " + taskId + " has inconsistent dates."
                );
            }

            String classification;

            if (due == null) {
                classification = "No deadline";
                noDeadline++;
            } else if (!due.isBefore(cutoff)) {
                classification = "Not due by evaluation end";
                notDue++;
            } else if (completed == null) {
                classification =
                        "Deadline passed; no completion recorded";
                noCompletion++;
            } else if (completed.isAfter(due)) {
                classification = "Completed late";
                late++;
            } else {
                classification = "Completed on time";
                onTime++;
            }

            JsonObject item = new JsonObject();
            item.addProperty("task_id", taskId);
            JsonElement titleValue = row.get("task_title");

            String taskTitle = titleValue == null || titleValue.isJsonNull()
                    ? ""
                    : titleValue.getAsString().trim();

            item.addProperty(
                    "task_title",
                    taskTitle.isBlank() ? "Untitled task" : taskTitle
            );
            
            item.addProperty("assigned_at_myt", localTime(assigned));
            item.addProperty("due_at_myt", localTime(due));
            item.addProperty("completed_at_myt", localTime(completed));
            item.addProperty("classification", classification);
            records.add(item);
        }

        evidence.addProperty(
                "status", rows.isEmpty() ? "no_data" : "available"
        );
        evidence.addProperty("period_start", start.toString());
        evidence.addProperty("period_end", end.toString());
        evidence.addProperty("read_at", Instant.now().toString());
        evidence.addProperty("task_count", rows.size());
        evidence.addProperty("completed_late_count", late);
        evidence.addProperty(
                "deadline_passed_no_completion_count", noCompletion
        );
        evidence.addProperty("completed_on_time_count", onTime);
        evidence.addProperty("not_due_count", notDue);
        evidence.addProperty("no_deadline_count", noDeadline);

        evidence.addProperty(
                "limitations",
                "Current stored dates for tasks assigned during the "
                + "selected period, interpreted in Asia/Kuala_Lumpur. "
                + "Only deadlines before the day after period_end "
                + "are classified as due. Completion may have been "
                + "recorded after the evaluation period. "
                + "A missing completion timestamp does not prove "
                + "the historical task status. Deadline revisions "
                + "and historical assignments are not available. "
                + "These records were read at generation time and "
                + "may differ from data used for the saved KPI."
        );

        return evidence;
    }

    private static Instant timestamp(JsonObject row, String key) {
        JsonElement value = row.get(key);

        return value == null || value.isJsonNull()
                ? null
                : OffsetDateTime.parse(value.getAsString()).toInstant();
    }

    private static String localTime(Instant value) {
        return value == null
                ? null
                : value.atZone(MYT).toOffsetDateTime().toString();
    }
}