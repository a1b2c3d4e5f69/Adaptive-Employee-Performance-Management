package com.aepm.logic.deadline;

import com.aepm.config.AppConfig;
import com.aepm.data.repository.NotificationRepository;
import com.aepm.data.supabase.SupabaseClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.Instant;

public class TaskDeadlineReminderService {

    private static final ZoneId MALAYSIA =
        ZoneId.of("Asia/Kuala_Lumpur");

    private static final String REMINDER_TYPE =
        "Due Today Reminder";

    private final SupabaseClient client =
        SupabaseClient.getInstance();

    private final NotificationRepository notificationRepository =
        new NotificationRepository();

    public record ReminderResult(
        int sent,
        int skipped,
        int failed
    ) {}

    private String encode(String value) {
        return URLEncoder.encode(
            value, StandardCharsets.UTF_8
        );
    }

    private JSONArray getRows(
            String token, String url) throws Exception {

        HttpRequest request = client
            .createAuthenticatedRequest(url, token)
            .GET()
            .build();

        HttpResponse<String> response =
            client.getHttpClient().send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {

            throw new Exception(
                "Supabase error " + response.statusCode()
                    + ": " + response.body()
            );
        }

        return new JSONArray(response.body());
    }

    private boolean alreadyRemindedToday(
            String token,
            String employeeId,
            String taskId,
            Instant dayStart,
            Instant nextDayStart
    ) throws Exception {

        String url = AppConfig.REST_URL
            + "/notification_tbl"
            + "?select=notification_id"
            + "&recipient_id=eq." + encode(employeeId)
            + "&related_task_id=eq." + encode(taskId)
            + "&notification_type=eq."
            + encode(REMINDER_TYPE)
            + "&created_at=gte."
            + encode(dayStart.toString())
            + "&created_at=lt."
            + encode(nextDayStart.toString())
            + "&limit=1";

        return !getRows(token, url).isEmpty();
    }

    public ReminderResult execute(String token)
            throws Exception {

        LocalDate today =
            LocalDate.now(MALAYSIA);

        ZonedDateTime start =
            today.atStartOfDay(MALAYSIA);

        ZonedDateTime end =
            today.plusDays(1).atStartOfDay(MALAYSIA);

        Instant dayStart = start.toInstant();
        Instant nextDayStart = end.toInstant();

        String url = AppConfig.REST_URL
            + "/task_tbl?select=*"
            + "&due_date=gte."
            + encode(dayStart.toString())
            + "&due_date=lt."
            + encode(nextDayStart.toString());

        JSONArray tasks = getRows(token, url);

        int sent = 0;
        int skipped = 0;
        int failed = 0;

        for (int i = 0; i < tasks.length(); i++) {

            JSONObject task = tasks.getJSONObject(i);

            try {
                String taskId =
                    task.getString("task_id");

                String employeeId =
                    task.optString(
                        "assigned_employee_id", ""
                    );

                String status =
                    task.optString("task_status", "");

                if (employeeId.isBlank()
                        || "Completed".equalsIgnoreCase(status)
                        || "Cancelled".equalsIgnoreCase(status)
                        || !task.isNull("completion_date")) {

                    skipped++;
                    continue;
                }

                if (alreadyRemindedToday(
                        token,
                        employeeId,
                        taskId,
                        dayStart,
                        nextDayStart)) {

                    skipped++;
                    continue;
                }

                String taskTitle =
                    task.optString("task_title", "");

                if (taskTitle.isBlank()) {
                    taskTitle =
                        task.optString("task_name", "");
                }

                if (taskTitle.isBlank()) {
                    taskTitle = "Your assigned task";
                }

                String eventKey =
                    "due-today:"
                        + taskId
                        + ":"
                        + today;

                boolean inserted =
                    notificationRepository.createOnce(
                        token,
                        employeeId,
                        "Task Due Today",
                        "Your task \"" + taskTitle
                            + "\" is due today. "
                            + "Please complete it before the deadline.",
                        REMINDER_TYPE,
                        taskId,
                        null,
                        eventKey
                    );

                if (inserted) {
                    sent++;

                    System.out.println(
                        "[Reminder] Sent once for task "
                            + taskId
                    );
                } else {
                    skipped++;

                    System.out.println(
                        "[Reminder] Duplicate skipped for task "
                            + taskId
                    );
                }

            } catch (Exception ex) {

                failed++;

                System.err.println(
                    "[Reminder] Failed: "
                        + ex.getMessage()
                );
            }
        }

        return new ReminderResult(
            sent, skipped, failed
        );
    }
}
