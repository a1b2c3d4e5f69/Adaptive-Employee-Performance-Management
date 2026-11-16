package com.aepm.logic.task;

import com.aepm.data.repository.NotificationRepository;

import com.aepm.data.task.TaskAssignmentRepository;
import com.aepm.data.task.TaskChatRepository;
import com.aepm.data.task.TaskRepository;

import com.aepm.model.Task;
import com.aepm.model.TaskAssignment;
import com.aepm.model.TaskChatMessage;
import com.aepm.model.User;

import com.aepm.util.SessionManager;

import java.time.OffsetDateTime;

import java.util.List;
import java.util.UUID;


public class TeamTaskService {


    // =========================================================
    // REPOSITORIES
    // =========================================================

    private final TaskRepository taskRepository =
            new TaskRepository();


    private final TaskAssignmentRepository
            assignmentRepository =
            new TaskAssignmentRepository();


    private final TaskChatRepository
            chatRepository =
            new TaskChatRepository();


    private final NotificationRepository
            notificationRepository =
            new NotificationRepository();


    // =========================================================
    // CREATE TEAM TASK
    // =========================================================

    public UUID createTeamTask(
            String title,
            String description,
            Long categoryId,
            List<UUID> employeeIds,
            String priority,
            String difficultyLevel,
            OffsetDateTime dueDate,
            String completionCriteria
    ) throws Exception {


        // =====================================================
        // CURRENT MANAGER
        // =====================================================

        User currentUser =
                SessionManager.getCurrentUser();


        if (currentUser == null) {

            throw new IllegalStateException(
                    "No logged-in user found."
            );
        }


        // =====================================================
        // VALIDATION
        // =====================================================

        if (
                title == null
                        || title.isBlank()
        ) {

            throw new IllegalArgumentException(
                    "Task title is required."
            );
        }


        if (categoryId == null) {

            throw new IllegalArgumentException(
                    "Task category is required."
            );
        }


        if (
                employeeIds == null
                        || employeeIds.size() < 2
        ) {

            throw new IllegalArgumentException(
                    "A team task requires at least 2 employees."
            );
        }


        if (dueDate == null) {

            throw new IllegalArgumentException(
                    "Due date is required."
            );
        }


        if (
                dueDate.isBefore(
                        OffsetDateTime.now()
                )
        ) {

            throw new IllegalArgumentException(
                    "Due date cannot be in the past."
            );
        }


        // =====================================================
        // REMOVE DUPLICATE EMPLOYEES
        // =====================================================

        List<UUID> uniqueEmployeeIds =
                employeeIds
                        .stream()
                        .filter(
                                id -> id != null
                        )
                        .distinct()
                        .toList();


        if (
                uniqueEmployeeIds.size() < 2
        ) {

            throw new IllegalArgumentException(
                    "A team task requires at least 2 different employees."
            );
        }


        // =====================================================
        // SESSION
        // =====================================================

        String accessToken =
                getAccessToken();


        UUID managerId =
                UUID.fromString(
                        currentUser.getUserId()
                );


        // =====================================================
        // CREATE ONE MASTER TASK
        // =====================================================

        Task task =
                new Task();


        task.setTaskTitle(
                title.trim()
        );


        task.setTaskDescription(
                cleanText(
                        description
                )
        );


        task.setCategoryId(
                categoryId
        );


        /*
         * IMPORTANT:
         *
         * Team task has multiple employees.
         * Therefore employee members are stored in
         * task_assignment_tbl instead.
         */
        task.setAssignedEmployeeId(
                null
        );


        task.setAssignedBy(
                managerId
        );


        task.setCreatedBy(
                managerId
        );


        task.setPriority(
                validatePriority(
                        priority
                )
        );


        task.setDifficultyLevel(
                validateDifficulty(
                        difficultyLevel
                )
        );


        task.setTaskStatus(
                "Pending"
        );


        task.setProgress(
                0
        );


        task.setDueDate(
                dueDate
        );


        task.setCompletionCriteria(
                cleanText(
                        completionCriteria
                )
        );


        // =====================================================
        // SAVE MASTER TASK
        // =====================================================

        UUID taskId =
                taskRepository.createTask(
                        task,
                        accessToken
                );


        if (taskId == null) {

            throw new IllegalStateException(
                    "Unable to create team task."
            );
        }


        // =====================================================
        // ASSIGN MULTIPLE EMPLOYEES
        // =====================================================

        assignmentRepository.assignEmployees(
                taskId,
                uniqueEmployeeIds,
                managerId,
                accessToken
        );


        // =====================================================
        // SEND NOTIFICATION TO EACH MEMBER
        // =====================================================

        for (
                UUID employeeId
                : uniqueEmployeeIds
        ) {

            try {

                notificationRepository.createOnce(
                        accessToken,

                        employeeId.toString(),

                        "New Team Task Assigned",

                        "You have been assigned to the team task: "
                                + task.getTaskTitle(),

                        "TASK_ASSIGNED",

                        taskId.toString(),

                        null,

                        "team_task_assigned:"
                                + taskId
                                + ":"
                                + employeeId
                );


            } catch (Exception error) {

                /*
                 * Task creation already succeeded.
                 * Notification failure should not delete
                 * or fail the team task.
                 */

                System.err.println(
                        "[Team Task Notification] Failed for "
                                + employeeId
                                + ": "
                                + error.getMessage()
                );
            }
        }


        System.out.println(
                "[Team Task] Created "
                        + taskId
                        + " with "
                        + uniqueEmployeeIds.size()
                        + " members."
        );


        // Needed by ManagerCreateTaskController
        // for optional attachment upload.
        return taskId;
    }


    // =========================================================
    // GET TEAM MEMBERS
    // =========================================================

    public List<TaskAssignment> getAssignments(
            UUID taskId
    ) throws Exception {


        validateTaskId(
                taskId
        );


        return assignmentRepository
                .getAssignments(
                        taskId,
                        getAccessToken()
                );
    }


    // =========================================================
    // GET TASK IDS FOR EMPLOYEE
    // =========================================================

    public List<UUID> getTaskIdsForEmployee(
            UUID employeeId
    ) throws Exception {


        if (employeeId == null) {

            throw new IllegalArgumentException(
                    "Employee ID is required."
            );
        }


        return assignmentRepository
                .getTaskIdsForEmployee(
                        employeeId,
                        getAccessToken()
                );
    }


    // =========================================================
    // UPDATE INDIVIDUAL TEAM MEMBER PROGRESS
    // =========================================================

    public void updateIndividualProgress(
            UUID taskId,
            int progress
    ) throws Exception {


        validateTaskId(
                taskId
        );


        if (
                progress < 0
                        || progress > 100
        ) {

            throw new IllegalArgumentException(
                    "Progress must be between 0 and 100."
            );
        }


        User currentUser =
                SessionManager.getCurrentUser();


        if (currentUser == null) {

            throw new IllegalStateException(
                    "No logged-in user found."
            );
        }


        UUID employeeId =
                UUID.fromString(
                        currentUser.getUserId()
                );


        assignmentRepository.updateProgress(
                taskId,
                employeeId,
                progress,
                getAccessToken()
        );
    }


    // =========================================================
    // GET CHAT MESSAGES
    //
    // TeamTaskChatDialog currently calls:
    // service.getMessages(taskId)
    // =========================================================

    public List<TaskChatMessage> getMessages(
            UUID taskId
    ) throws Exception {


        validateTaskId(
                taskId
        );


        return chatRepository
                .getMessages(
                        taskId,
                        getAccessToken()
                );
    }


    // =========================================================
    // SEND CHAT MESSAGE
    //
    // TeamTaskChatDialog currently calls:
    // service.sendMessage(taskId, message)
    // =========================================================

    public void sendMessage(
            UUID taskId,
            String message
    ) throws Exception {


        validateTaskId(
                taskId
        );


        if (
                message == null
                        || message.isBlank()
        ) {

            throw new IllegalArgumentException(
                    "Message cannot be empty."
            );
        }


        String cleaned =
                message.trim();


        if (
                cleaned.length() > 2000
        ) {

            throw new IllegalArgumentException(
                    "Message cannot exceed 2000 characters."
            );
        }


        User currentUser =
                SessionManager.getCurrentUser();


        if (currentUser == null) {

            throw new IllegalStateException(
                    "No logged-in user found."
            );
        }


        UUID senderId =
                UUID.fromString(
                        currentUser.getUserId()
                );


        chatRepository.sendMessage(
                taskId,
                senderId,
                cleaned,
                getAccessToken()
        );
    }


    // =========================================================
    // IS TEAM TASK
    // =========================================================

    public boolean isTeamTask(
            UUID taskId
    ) throws Exception {


        validateTaskId(
                taskId
        );


        List<TaskAssignment> assignments =
                getAssignments(
                        taskId
                );


        return assignments != null
                && assignments.size() > 1;
    }


    // =========================================================
    // VALIDATE TASK ID
    // =========================================================

    private void validateTaskId(
            UUID taskId
    ) {


        if (taskId == null) {

            throw new IllegalArgumentException(
                    "Task ID is required."
            );
        }
    }


    // =========================================================
    // CLEAN TEXT
    // =========================================================

    private String cleanText(
            String value
    ) {


        if (
                value == null
                        || value.isBlank()
        ) {

            return null;
        }


        return value.trim();
    }


    // =========================================================
    // PRIORITY
    // =========================================================

    private String validatePriority(
            String priority
    ) {


        if (
                priority == null
                        || priority.isBlank()
        ) {

            return "Medium";
        }


        String value =
                priority.trim();


        return switch (
                value.toLowerCase()
        ) {

            case "low" ->
                    "Low";

            case "medium" ->
                    "Medium";

            case "high" ->
                    "High";

            case "urgent" ->
                    "Urgent";

            default ->
                    throw new IllegalArgumentException(
                            "Invalid priority."
                    );
        };
    }


    // =========================================================
    // DIFFICULTY
    // =========================================================

    private String validateDifficulty(
            String difficulty
    ) {


        if (
                difficulty == null
                        || difficulty.isBlank()
        ) {

            return "Medium";
        }


        String value =
                difficulty.trim();


        return switch (
                value.toLowerCase()
        ) {

            case "easy" ->
                    "Easy";

            case "medium" ->
                    "Medium";

            case "hard" ->
                    "Hard";

            case "advanced" ->
                    "Advanced";

            default ->
                    throw new IllegalArgumentException(
                            "Invalid difficulty level."
                    );
        };
    }


    // =========================================================
    // ACCESS TOKEN
    // =========================================================

    private String getAccessToken() {


        String accessToken =
                SessionManager.getAccessToken();


        if (
                accessToken == null
                        || accessToken.isBlank()
        ) {

            throw new IllegalStateException(
                    "User session has expired. Please login again."
            );
        }


        return accessToken;
    }
}