package com.aepm.logic.deadline;

import com.aepm.data.repository.EscalationRuleRepository;
import com.aepm.model.EscalationRule;

import java.util.List;

public class EscalationRuleService {

    private final EscalationRuleRepository repository =
        new EscalationRuleRepository();

    private void checkToken(String token) {

        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                "Please log in again."
            );
        }
    }

    private void validate(EscalationRule rule) {

        if (rule.ruleName() == null ||
            rule.ruleName().isBlank() ||
            rule.actionType() == null ||
            rule.actionType().isBlank() ||
            rule.status() == null ||
            rule.status().isBlank()) {

            throw new IllegalArgumentException(
                "Please complete all required escalation rule fields."
            );
        }

        if (rule.escalationLevel() < 1 ||
            rule.missedDeadlineCount() < 1 ||
            rule.extensionDays() < 0) {

            throw new IllegalArgumentException(
                "Invalid escalation rule configuration."
            );
        }

        if (!rule.status().equals("Active") &&
            !rule.status().equals("Inactive")) {

            throw new IllegalArgumentException(
                "Status must be Active or Inactive."
            );
        }
    }

    public List<EscalationRule> getAll(String token)
            throws Exception {

        checkToken(token);
        return repository.findAll(token);
    }

    public void create(String token, EscalationRule rule)
            throws Exception {

        checkToken(token);
        validate(rule);
        repository.create(token, rule);
    }

    public void update(String token, EscalationRule rule)
            throws Exception {

        checkToken(token);
        validate(rule);
        repository.update(token, rule);
    }

    public void delete(String token, long ruleId)
            throws Exception {

        checkToken(token);
        repository.delete(token, ruleId);
    }
}
