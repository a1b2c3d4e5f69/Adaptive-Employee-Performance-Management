package com.aepm.logic.deadline;

import com.aepm.config.AppConfig;
import com.aepm.data.repository.EscalationRuleRepository;
import com.aepm.data.repository.TaskEscalationRepository;
import com.aepm.data.supabase.SupabaseClient;
import com.aepm.model.EscalationRule;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.Instant;
import java.util.List;
import com.aepm.data.repository.NotificationRepository;

public class DeadlineEscalationService {

    private final DeadlineEscalationPreviewService previewService =
        new DeadlineEscalationPreviewService();

    private final TaskEscalationRepository historyRepository =
        new TaskEscalationRepository();

    private final EscalationRuleRepository ruleRepository =
        new EscalationRuleRepository();

    private final NotificationRepository notificationRepository =
        new NotificationRepository();

    private final SupabaseClient client =
        SupabaseClient.getInstance();

    public record ExecutionResult(
        int created,
        int skipped,
        int failed
    ) {}

    private JSONObject loadTask(
            String token, String taskId) throws Exception {

        String encodedId = URLEncoder.encode(
            taskId, StandardCharsets.UTF_8
        );

        String url = AppConfig.REST_URL
            + "/task_tbl?select=task_id,assigned_employee_id,"
            + "due_date,task_status,completion_date"
            + "&task_id=eq." + encodedId;

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
                "Unable to load task: "
                    + response.statusCode()
                    + " " + response.body()
            );
        }

        JSONArray rows = new JSONArray(response.body());

        return rows.isEmpty() ? null : rows.getJSONObject(0);
    }

    private String findManagerId(
            String token,
            String employeeId
    ) throws Exception {

        String encodedId = URLEncoder.encode(
            employeeId,
            StandardCharsets.UTF_8
        );

        String url = AppConfig.REST_URL
            + "/user_tbl"
            + "?select=manager_id"
            + "&user_id=eq." + encodedId
            + "&limit=1";

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
                "Unable to load employee manager: "
                    + response.body()
            );
        }

        JSONArray rows = new JSONArray(response.body());

        if (rows.isEmpty()) {
            return null;
        }

        JSONObject employee = rows.getJSONObject(0);

        if (employee.isNull("manager_id")) {
            return null;
        }

        return employee.getString("manager_id");
    }

    private List<String> findActiveAdminIds(String token) throws Exception {

        // AEPM: Administrator role_id = 1
        String url = AppConfig.REST_URL
            + "/user_tbl"
            + "?select=user_id"
            + "&role_id=eq.1"
            + "&account_status=eq.Active";

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
                "Unable to load administrators: "
                    + response.statusCode()
                    + " " + response.body()
            );
        }

        JSONArray rows = new JSONArray(response.body());

        List<String> adminIds = new java.util.ArrayList<>();

        for (int i = 0; i < rows.length(); i++) {
            String adminId = rows.getJSONObject(i)
                .optString("user_id", "");

            if (!adminId.isBlank()) {
                adminIds.add(adminId);
            }
        }

        return adminIds;
    }


    private void notifyAdminsForLevel3(
            String token,
            String taskId,
            String taskTitle,
            String employeeId,
            long escalationId
    ) throws Exception {

        List<String> adminIds = findActiveAdminIds(token);

        for (String adminId : adminIds) {

            try {
                String eventKey =
                    "escalation:admin:"
                        + adminId
                        + ":"
                        + escalationId;

                String title =
                    "Level 3 - Employee Performance Risk";

                String message =
                    "Performance Risk Alert: Task \""
                        + taskTitle
                        + "\" has reached Level 3. "
                        + "The employee has repeatedly missed "
                        + "the task deadline. "
                        + "Please review the employee's "
                        + "performance risk history.";

                boolean inserted = notificationRepository.createOnce(
                    token,
                    adminId,
                    title,
                    message,
                    "Deadline Escalation",
                    taskId,
                    escalationId,
                    eventKey
                );

                if (inserted) {
                    System.out.println(
                        "[Notification] Admin notified: "
                            + adminId
                            + " / Escalation "
                            + escalationId
                    );
                }

            } catch (Exception ex) {

                System.err.println(
                    "[Notification] Admin notification failed: "
                        + adminId
                        + " / "
                        + ex.getMessage()
                );
            }
        }
    }

    private boolean managerNotificationExists(
            String token,
            String managerId,
            long escalationId
    ) throws Exception {

        String url = AppConfig.REST_URL
            + "/notification_tbl"
            + "?select=notification_id"
            + "&recipient_id=eq."
            + URLEncoder.encode(managerId, StandardCharsets.UTF_8)
            + "&related_escalation_id=eq." + escalationId
            + "&limit=1";

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
                "Unable to check manager notification: "
                    + response.body()
            );
        }

        return !new JSONArray(response.body()).isEmpty();
    }

    public int recoverManagerNotifications(String token) throws Exception {

        String url = AppConfig.REST_URL
            + "/task_escalation_tbl"
            + "?select=escalation_id,task_id,employee_id,"
            + "current_level,previous_deadline,status"
            + "&status=eq.Active"
            + "&order=escalation_id.desc"
            + "&limit=500";

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
                "Unable to load escalation history: "
                    + response.body()
            );
        }

        JSONArray history = new JSONArray(response.body());
        int recovered = 0;

        for (int i = 0; i < history.length(); i++) {

            JSONObject row = history.getJSONObject(i);

            try {
                long escalationId = row.getLong("escalation_id");
                String taskId = row.getString("task_id");
                String employeeId = row.getString("employee_id");

                JSONObject task = loadTask(token, taskId);

                if (task == null
                        || !task.isNull("completion_date")) {
                    continue;
                }

                String status = task.optString(
                    "task_status", ""
                );

                if ("Completed".equalsIgnoreCase(status)
                        || "Cancelled".equalsIgnoreCase(status)) {
                    continue;
                }

                // Only recover notifications for tasks
                // that are currently overdue.
                if (task.isNull("due_date")) {
                    continue;
                }

                Instant dueInstant = OffsetDateTime.parse(
                    task.getString("due_date")
                ).toInstant();

                if (!dueInstant.isBefore(Instant.now())) {
                    continue;
                }

                if (row.isNull("previous_deadline")) {
                    continue;
                }

                Instant escalationDeadline = OffsetDateTime.parse(
                    row.getString("previous_deadline")
                ).toInstant();

                if (!escalationDeadline.equals(dueInstant)) {
                    continue;
                }

                String managerId = findManagerId(
                    token, employeeId
                );

                if (managerId == null || managerId.isBlank()) {
                    continue;
                }

                if (managerNotificationExists(
                        token, managerId, escalationId)) {
                    continue;
                }

                boolean isLevel3 = row.optInt("current_level", 0) == 3;
                String eventKey = "escalation:manager:" + managerId + ":" + escalationId;

                String title = isLevel3
                    ? "Level 3 - Employee Performance Risk"
                    : "Employee Task Escalation";

                String message = isLevel3
                    ? "Performance Risk Alert: An employee's task remains "
                        + "overdue after the Level 2 follow-up period. "
                        + "Please review the performance risk."
                    : "An assigned employee has an overdue task. "
                        + "Please review the task and take action.";

                boolean inserted = notificationRepository.createOnce(
                    token,
                    managerId,
                    title,
                    message,
                    "Deadline Escalation",
                    taskId,
                    escalationId,
                    eventKey
                );


                if (inserted) {
                    recovered++;

                    System.out.println(
                        "[Recovery] Manager notification created: "
                            + managerId
                            + " / Escalation " + escalationId
                    );
                }

            } catch (Exception ex) {
                System.err.println(
                    "[Recovery] Failed: " + ex.getMessage()
                );
            }
        }

        return recovered;
    }

    public ExecutionResult execute(String token) throws Exception {

        try {
            int recovered = recoverManagerNotifications(token);

            System.out.println(
                "[Escalation] Manager notifications recovered: "
                    + recovered
            );

        } catch (Exception ex) {
            System.err.println(
                "[Escalation] Recovery failed: "
                    + ex.getMessage()
            );
        }

        List<DeadlineEscalationPreviewService.Candidate> candidates =
            previewService.preview(token);

        List<EscalationRule> rules =
            ruleRepository.findAll(token);

        int created = 0;
        int skipped = 0;
        int failed = 0;

        for (var candidate : candidates) {

            try {
                EscalationRule rule = rules.stream()
                    .filter(r -> r.ruleId() == candidate.ruleId())
                    .filter(r -> "Active".equalsIgnoreCase(r.status()))
                    .filter(r -> r.missedDeadlineCount() == candidate.level()  && r.escalationLevel() == candidate.level())
                    .findFirst()
                    .orElse(null);

                if (rule == null) {
                    skipped++;
                    continue;
                }

                JSONObject task = loadTask(
                    token, candidate.taskId()
                );

                if (task == null
                        || task.isNull("due_date")
                        || task.isNull("assigned_employee_id")
                        || !task.isNull("completion_date")) {

                    skipped++;
                    continue;
                }

                String status = task.optString(
                    "task_status", ""
                );

                if ("Completed".equalsIgnoreCase(status)
                        || "Cancelled".equalsIgnoreCase(status)) {

                    skipped++;
                    continue;
                }

                String deadline = task.getString("due_date");

                Instant dueInstant =
                    OffsetDateTime.parse(deadline).toInstant();

                if (!dueInstant.isBefore(Instant.now())) {
                    skipped++;
                    continue;
                }

                boolean exists =
                    historyRepository.alreadyTriggered(
                        token,
                        candidate.taskId(),
                        candidate.ruleId(),
                        deadline
                    );

                if (exists) {
                    skipped++;
                    continue;
                }

                long escalationId = historyRepository.create(
                    token,
                    candidate.taskId(),
                    task.getString("assigned_employee_id"),
                    candidate.ruleId(),
                    rule.escalationLevel(),
                    candidate.level(),
                    deadline
                );

                created++;

                // Create a notification after escalation succeeds.
                String employeeId = task.getString("assigned_employee_id");
                String taskTitle = candidate.taskTitle();
                // 1. Notify Employee
                try {
                    String employeeEventKey =
                        "escalation:employee:" + escalationId;

                    boolean isLevel3 = candidate.level() == 3;

                    String employeeTitle = isLevel3
                        ? "Level 3 Performance Risk Alert"
                        : "Task Deadline Missed";

                    String employeeMessage = isLevel3
                        ? "Your task \"" + taskTitle
                            + "\" remains overdue 48 hours after "
                            + "the Level 2 manager review. "
                            + "Please take immediate action."
                        : "Your task \"" + taskTitle
                            + "\" has missed its deadline. "
                            + "Please review and complete it.";

                    notificationRepository.createOnce(
                        token,
                        employeeId,
                        employeeTitle,
                        employeeMessage,
                        "Deadline Escalation",
                        candidate.taskId(),
                        escalationId,
                        employeeEventKey
                    );

                    System.out.println(
                        "[Notification] Employee notified: "
                            + employeeId
                    );

                } catch (Exception ex) {
                    System.err.println(
                        "[Notification] Employee notification failed: "
                            + ex.getMessage()
                    );
                }

                
                // 2. Notify Manager
                try {
                    String managerId = findManagerId(
                        token, employeeId
                    );

                    if (managerId != null && !managerId.isBlank()) {

                        String managerEventKey =
                            "escalation:manager:"
                            + managerId + ":" + escalationId;

                        boolean isLevel3 = candidate.level() == 3;

                        String managerTitle = isLevel3
                            ? "Level 3 - Employee Performance Risk"
                            : "Employee Task Escalation";

                        String managerMessage = isLevel3
                            ? "Performance Risk Alert: The task \""
                                + taskTitle
                                + "\" remains overdue 48 hours after "
                                + "the Level 2 manager decision. "
                                + "Please review the employee's "
                                + "performance risk."
                            : "The task \"" + taskTitle
                                + "\" assigned to your employee "
                                + "has missed its deadline. "
                                + "Please review the task.";

                        notificationRepository.createOnce(
                            token,
                            managerId,
                            managerTitle,
                            managerMessage,
                            "Deadline Escalation",
                            candidate.taskId(),
                            escalationId,
                            managerEventKey
                        );

                        System.out.println(
                            "[Notification] Manager notified: "
                                + managerId
                        );

                    } else {
                        System.out.println(
                            "[Notification] No manager assigned "
                                + "to employee: " + employeeId
                        );
                    }

                } catch (Exception ex) {
                    System.err.println(
                        "[Notification] Manager notification failed: "
                            + ex.getMessage()
                    );
                }

                // 3. Notify Admin only for Level 3 Performance Risk
                if (candidate.level() == 3) {

                    try {

                        notifyAdminsForLevel3(
                            token,
                            candidate.taskId(),
                            taskTitle,
                            employeeId,
                            escalationId
                        );

                    } catch (Exception ex) {

                        System.err.println(
                            "[Notification] Level 3 Admin notification failed: "
                                + ex.getMessage()
                        );
                    }
                }

                System.out.println(
                    "[Escalation] Created: "
                        + candidate.taskId()
                        + " / " + candidate.ruleName()
                );

            } catch (Exception ex) {

                failed++;

                System.err.println(
                    "[Escalation] Failed for task "
                        + candidate.taskId()
                        + ": " + ex.getMessage()
                );
            }
        }

        return new ExecutionResult(
            created, skipped, failed
        );
    }
}
