package com.aepm.logic.deadline;

import java.time.Instant;
import com.aepm.config.AppConfig;
import com.aepm.data.repository.EscalationRuleRepository;
import com.aepm.data.supabase.SupabaseClient;
import com.aepm.model.EscalationRule;

import org.json.JSONArray;
import org.json.JSONObject;
import java.time.Duration;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class DeadlineEscalationPreviewService {

    private final SupabaseClient client =
        SupabaseClient.getInstance();

    private final EscalationRuleRepository ruleRepository =
        new EscalationRuleRepository();

    public record Candidate(
        String taskId,
        String taskTitle,
        String ruleName,
        long ruleId,
        int level,
        String actionType
    ) {}

    private JSONArray getRows(String token, String url)
            throws Exception {

        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                "Please log in again."
            );
        }

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

            throw new IllegalStateException(
                "Supabase HTTP " + response.statusCode()
                    + ": " + response.body()
            );
        }

        return new JSONArray(response.body());
    }

    public List<Candidate> preview(String token)
            throws Exception {

        String base = AppConfig.REST_URL;

        String now = URLEncoder.encode(
            OffsetDateTime.now(ZoneOffset.UTC).toString(),
            StandardCharsets.UTF_8
        );

        JSONArray tasks = getRows(
            token,
            base + "/task_tbl"
                + "?select=task_id,task_title,due_date,"
                + "assigned_employee_id,task_status,completion_date"
                + "&due_date=lt." + now
                + "&completion_date=is.null"
                + "&task_status=not.in.(Completed,Cancelled)"
        );

        List<EscalationRule> active =
            ruleRepository.findAll(token)
                .stream()
                .filter(r ->
                    "Active".equalsIgnoreCase(r.status())
                )
                .sorted(
                    Comparator.comparingInt(
                        EscalationRule::escalationLevel
                    )
                )
                .toList();

        List<Candidate> candidates = new ArrayList<>();

        for (int i = 0; i < tasks.length(); i++) {

            JSONObject task = tasks.getJSONObject(i);

            String taskId = task.getString("task_id");
            String dueDate = task.getString("due_date");

            String encodedId = URLEncoder.encode(
                taskId, StandardCharsets.UTF_8
            );

            JSONArray history = getRows(
                token,
                base + "/task_escalation_tbl"
                    + "?select=escalation_id,rule_id,current_level,"
                    + "previous_deadline,new_deadline,"
                    + "manager_decision,status,decided_at"
                    + "&task_id=eq." + encodedId
                    + "&order=escalation_id.desc"
            );

            // Find the highest escalation level already recorded.
            int highestLevel = 0;
            JSONObject latestEscalation = null;

            for (int j = 0; j < history.length(); j++) {
                JSONObject event = history.getJSONObject(j);

                int eventLevel = event.optInt("current_level", 0);

                if (eventLevel > highestLevel) {
                    highestLevel = eventLevel;
                    latestEscalation = event;
                }
            }

            int nextLevel = highestLevel + 1;

            // First missed deadline -> Level 1.
            if (highestLevel == 0) {
                nextLevel = 1;
            } else {
                // An unresolved escalation must not create a new level.
                if (latestEscalation == null ||
                    !"Resolved".equalsIgnoreCase(
                        latestEscalation.optString("status", "")
                    )) {
                    continue;
                }

                // For now, Level 2 requires an approved Level 1 extension.
                // The task's current deadline must match that extension.
                if (highestLevel == 1) {
                    if (!"Approved".equalsIgnoreCase(
                            latestEscalation.optString("manager_decision", "")
                        ) ||
                        latestEscalation.isNull("new_deadline")) {
                        continue;
                    }

                    boolean matchesApprovedDeadline =
                        OffsetDateTime.parse(
                            latestEscalation.getString("new_deadline")
                        ).toInstant().equals(
                            OffsetDateTime.parse(dueDate).toInstant()
                        );

                    if (!matchesApprovedDeadline) {
                        continue;
                    }
                
                } else if (highestLevel == 2) {

                    // Level 3: wait 48 hours after Level 2 resolution.
                    // No new deadline is required.

                    if (latestEscalation.isNull("decided_at")) {
                        continue;
                    }

                    Instant resolvedTime = OffsetDateTime.parse(
                        latestEscalation.getString("decided_at")
                    ).toInstant();

                    Instant eligibleTime = resolvedTime.plus(
                        Duration.ofHours(48)
                    );

                    // Wait until the 48-hour follow-up period ends.
                    if (Instant.now().isBefore(eligibleTime)) {
                        continue;
                    }

                    // Level 3 should only be triggered once per task.
                    boolean level3Exists = false;

                    for (int j = 0; j < history.length(); j++) {
                        JSONObject event = history.getJSONObject(j);

                        if (event.optInt("current_level", 0) >= 3) {
                            level3Exists = true;
                            break;
                        }
                    }

                    if (level3Exists) {
                        continue;
                    }

                } else {
                    // Stop after Level 3.
                    continue;
                }
            }

            // Select only the rule for the next level.
            for (EscalationRule rule : active) {

                if (rule.escalationLevel() != nextLevel ||
                    rule.missedDeadlineCount() != nextLevel) {
                    continue;
                }

                // One deadline event must not be counted twice,
                // even if different escalation rules are involved.
                boolean deadlineAlreadyCounted = false;

                for (int j = 0; j < history.length(); j++) {

                    JSONObject event = history.getJSONObject(j);

                    if (event.isNull("previous_deadline")) {
                        continue;
                    }

                    boolean sameDeadline =
                        OffsetDateTime.parse(
                            event.getString("previous_deadline")
                        ).toInstant().equals(
                            OffsetDateTime.parse(dueDate).toInstant()
                        );

                    if (sameDeadline) {
                        deadlineAlreadyCounted = true;
                        break;
                    }
                }

                if (deadlineAlreadyCounted && nextLevel != 3) {
                    continue;
                }

                boolean alreadyTriggered = false;

                for (int j = 0; j < history.length(); j++) {
                    JSONObject event = history.getJSONObject(j);

                    if (event.optLong("rule_id", -1) == rule.ruleId()
                        && !event.isNull("previous_deadline")
                        && OffsetDateTime.parse(
                            event.getString("previous_deadline")
                        ).toInstant().equals(
                            OffsetDateTime.parse(dueDate).toInstant()
                        )) {

                        alreadyTriggered = true;
                        break;
                    }
                }

                if (!alreadyTriggered) {
                    candidates.add(new Candidate(
                        taskId,
                        task.optString("task_title", ""),
                        rule.ruleName(),
                        rule.ruleId(),
                        rule.escalationLevel(),
                        rule.actionType()
                    ));
                }
            } 

        } 

        return candidates;
    } 

} 