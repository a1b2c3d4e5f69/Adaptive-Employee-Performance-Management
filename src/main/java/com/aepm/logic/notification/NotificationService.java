package com.aepm.logic.notification;

import com.aepm.data.repository.NotificationRepository;

public class NotificationService {

    private final NotificationRepository repository =
            new NotificationRepository();

    /**
     * Send a notification to an employee when a
     * deadline escalation is created.
     */
    public void sendDeadlineReminder(
            String token,
            String employeeId,
            String taskId,
            String taskTitle,
            long escalationId
    ) throws Exception {

        if (employeeId == null || employeeId.isBlank()) {
            throw new IllegalArgumentException(
                "Employee ID is required."
            );
        }

        String title = "Task Deadline Missed";

        String message =
            "Your task \"" + taskTitle
            + "\" has passed its deadline. "
            + "Please review and update your task progress.";

        repository.create(
            token,
            employeeId,
            title,
            message,
            "DeadlineEscalation",
            taskId,
            escalationId
        );
    }
}
