
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

public class DeadlineEscalationService {

    private final DeadlineEscalationPreviewService previewService =
        new DeadlineEscalationPreviewService();

    private final TaskEscalationRepository historyRepository =
        new TaskEscalationRepository();

    private final EscalationRuleRepository ruleRepository =
        new EscalationRuleRepository();

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

    public ExecutionResult execute(String token)
            throws Exception {

        List<DeadlineEscalationPreviewService.Candidate> candidates =
            previewService.preview(token);

        List<EscalationRule> rules =
            ruleRepository.findAll(token);

        int created = 0;
        int skipped = 0;
        int failed = 0;

        for (var candidate : candidates) {

            try {
                // Phase 2 only supports Reminder.
                if (!"Reminder".equalsIgnoreCase(
                        candidate.actionType())) {
                    skipped++;
                    continue;
                }

                EscalationRule rule = rules.stream()
                    .filter(r -> r.ruleId() == candidate.ruleId())
                    .filter(r -> "Active".equalsIgnoreCase(r.status()))
                    .filter(r -> r.missedDeadlineCount() == 1)
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

                historyRepository.create(
                    token,
                    candidate.taskId(),
                    task.getString("assigned_employee_id"),
                    candidate.ruleId(),
                    rule.escalationLevel(),
                    1,
                    deadline
                );

                created++;

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
