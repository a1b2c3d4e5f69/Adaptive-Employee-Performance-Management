package com.aepm.model;

public record EscalationRule(
    long ruleId,
    String ruleName,
    int escalationLevel,
    int missedDeadlineCount,
    String actionType,
    int extensionDays,
    String description,
    String status,
    String createdBy
) {}
