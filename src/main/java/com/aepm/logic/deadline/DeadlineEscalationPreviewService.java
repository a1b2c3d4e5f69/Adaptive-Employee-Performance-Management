
package com.aepm.logic.deadline;

import com.aepm.config.AppConfig;
import com.aepm.data.repository.EscalationRuleRepository;
import com.aepm.data.supabase.SupabaseClient;
import com.aepm.model.EscalationRule;

import org.json.JSONArray;
import org.json.JSONObject;

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
                    + "?select=rule_id,previous_deadline"
                    + "&task_id=eq." + encodedId
            );

            for (EscalationRule rule : active) {

                // Phase 2: first missed deadline only.
                if (rule.missedDeadlineCount() != 1) {
                    continue;
                }

                boolean alreadyTriggered = false;

                for (int j = 0; j < history.length(); j++) {

                    JSONObject event =
                        history.getJSONObject(j);

                    if (event.optLong("rule_id", -1)
                            == rule.ruleId()
                        && !event.isNull("previous_deadline")
                        && OffsetDateTime.parse(
                            event.getString("previous_deadline")
                        ).toInstant().equals(
                            OffsetDateTime.parse(dueDate)
                                .toInstant()
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
