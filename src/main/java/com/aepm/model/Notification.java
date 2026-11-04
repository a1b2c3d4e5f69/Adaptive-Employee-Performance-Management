package com.aepm.model;

public record Notification(
    String notificationId,
    String recipientId,
    String title,
    String message,
    String notificationType,
    String relatedTaskId,
    Long relatedEscalationId,
    boolean isRead,
    String createdAt,
    String readAt
) {}
