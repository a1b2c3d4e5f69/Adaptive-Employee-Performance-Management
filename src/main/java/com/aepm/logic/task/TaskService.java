package com.aepm.logic.task;

import com.aepm.data.task.TaskRepository;
import com.aepm.model.Task;
import com.aepm.model.User;
import com.aepm.util.SessionManager;

import java.time.OffsetDateTime;
import com.aepm.data.repository.NotificationRepository;
import java.util.List;
import java.util.UUID;

public class TaskService {

    private final TaskRepository taskRepository =
            new TaskRepository();

    private final NotificationRepository notificationRepository =
        new NotificationRepository();
    // =========================================================
    // CREATE TASK
    // =========================================================

    public void createTask(
            String title,
            String description,
            Long categoryId,
            UUID employeeId,
            String priority,
            String difficultyLevel,
            OffsetDateTime dueDate,
            String completionCriteria
    ) throws Exception {

        User currentUser =
                SessionManager.getCurrentUser();


        if (currentUser == null) {

            throw new IllegalStateException(
                    "No logged-in user found."
            );
        }


        if (
                title == null
                        || title.isBlank()
        ) {

            throw new IllegalArgumentException(
                    "Task title is required."
            );
        }


        if (employeeId == null) {

            throw new IllegalArgumentException(
                    "Please select an employee."
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


        task.setAssignedEmployeeId(
                employeeId
        );


        UUID currentUserId =
                UUID.fromString(
                        currentUser.getUserId()
                );


        task.setAssignedBy(
                currentUserId
        );


        task.setCreatedBy(
                currentUserId
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


        // 1. Create task and get generated Task ID
        String accessToken = getAccessToken();

        UUID createdTaskId = taskRepository.createTask(
                task,
                accessToken
        );

        // 2. Send notification to assigned employee
        try {

                System.out.println(
                        "[Notification] Creating TASK_ASSIGNED for task: "
                        + createdTaskId
                );

                boolean created = notificationRepository.createOnce(
                        accessToken,
                        employeeId.toString(),
                        "New Task Assigned",
                        "You have been assigned a new task: "
                                + task.getTaskTitle(),
                        "TASK_ASSIGNED",
                        createdTaskId.toString(),
                        null,
                        "task_assigned:" + createdTaskId
                );

                System.out.println(
                        "[Notification] TASK_ASSIGNED created: " + created
                );

                } catch (Exception e) {

                System.err.println(
                        "[Notification] Failed to send task assignment: "
                        + e.getMessage()
                );

                e.printStackTrace();
                }
    }


    // =========================================================
    // GET ALL TASKS
    // =========================================================

    public List<Task> getAllTasks()
            throws Exception {

        return taskRepository
                .getAllTasks(
                        getAccessToken()
                );
    }


    // =========================================================
    // GET MY TASKS
    // =========================================================

    public List<Task> getMyTasks()
            throws Exception {

        User currentUser =
                SessionManager.getCurrentUser();


        if (currentUser == null) {

            throw new IllegalStateException(
                    "No logged-in user found."
            );
        }


        return taskRepository
                .getTasksByEmployee(
                        UUID.fromString(
                                currentUser.getUserId()
                        ),
                        getAccessToken()
                );
    }


    // =========================================================
    // GET TASKS BY EMPLOYEE
    // =========================================================

    public List<Task> getTasksByEmployee(
            UUID employeeId
    ) throws Exception {

        if (employeeId == null) {

            throw new IllegalArgumentException(
                    "Employee ID is required."
            );
        }


        return taskRepository
                .getTasksByEmployee(
                        employeeId,
                        getAccessToken()
                );
    }


    // =========================================================
    // ACCEPT TASK
    // =========================================================

    public void acceptTask(
            UUID taskId
    ) throws Exception {

        validateTaskId(
                taskId
        );


        taskRepository.updateTaskStatus(
                taskId,
                "Accepted",
                getAccessToken()
        );
    }


    // =========================================================
    // START TASK
    // =========================================================

    public void startTask(
            UUID taskId
    ) throws Exception {

        validateTaskId(
                taskId
        );


        taskRepository.updateTaskStatus(
                taskId,
                "In Progress",
                getAccessToken()
        );
    }


    // =========================================================
    // COMPLETE TASK
    // =========================================================

    public void completeTask(
            UUID taskId
    ) throws Exception {
    
        validateTaskId(taskId);
    
        String accessToken = getAccessToken();
    
        // 1. Get task details before updating
        Task task = taskRepository.getTaskById(
                taskId,
                accessToken
        );
    
        // 2. Update task status to Completed
        taskRepository.updateTaskStatus(
                taskId,
                "Completed",
                accessToken
        );
    
        // 3. Notify the Manager who assigned this task
        if (task.getAssignedBy() != null) {
    
            try {
    
                boolean created = notificationRepository.createOnce(
                        accessToken,
                        task.getAssignedBy().toString(),
                        "Task Completed",
                        "Employee has completed the task: "
                                + task.getTaskTitle()
                                + ". Please review the completed task.",
                        "TASK_COMPLETED",
                        taskId.toString(),
                        null,
                        "task_completed:" + taskId
                );
    
                System.out.println(
                        "[Notification] Task completed notification created: "
                                + created
                );
    
            } catch (Exception e) {
    
                System.err.println(
                        "[Notification] Failed to send task completed: "
                                + e.getMessage()
                );
            }
        }
    }


    // =========================================================
    // UPDATE PROGRESS
    // =========================================================

    public void updateProgress(
            UUID taskId,
            double progress
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


        taskRepository.updateTaskProgress(
                taskId,
                progress,
                getAccessToken()
        );
    }


    // =========================================================
    // UPDATE TASK
    // =========================================================

    public void updateTask(
            Task task
    ) throws Exception {

        if (task == null) {

            throw new IllegalArgumentException(
                    "Task is required."
            );
        }


        validateTaskId(
                task.getTaskId()
        );


        if (
                task.getTaskTitle() == null
                        || task.getTaskTitle().isBlank()
        ) {

            throw new IllegalArgumentException(
                    "Task title is required."
            );
        }


        if (task.getDueDate() == null) {

            throw new IllegalArgumentException(
                    "Due date is required."
            );
        }


        task.setTaskTitle(
                task.getTaskTitle()
                        .trim()
        );


        task.setTaskDescription(
                cleanText(
                        task.getTaskDescription()
                )
        );


        task.setCompletionCriteria(
                cleanText(
                        task.getCompletionCriteria()
                )
        );


        task.setPriority(
                validatePriority(
                        task.getPriority()
                )
        );


        task.setDifficultyLevel(
                validateDifficulty(
                        task.getDifficultyLevel()
                )
        );


        String accessToken = getAccessToken();

        // 1. Update task information
        taskRepository.updateTask(
                task,
                accessToken
        );

        System.out.println(
        "[DEBUG] updateTask called, Task ID: "
                + task.getTaskId()
        );

        System.out.println(
        "[DEBUG] Assigned Employee ID: "
                + task.getAssignedEmployeeId()
        );

        System.out.println(
                "[DEBUG] Task ID: " + task.getTaskId()
        );

        System.out.println(
                "[DEBUG] Assigned Employee ID: "
                        + task.getAssignedEmployeeId()
        );

        if (task.getAssignedEmployeeId() != null) {

        try {

                notificationRepository.create(
                        accessToken,
                        task.getAssignedEmployeeId().toString(),
                        "Task Updated",
                        "Your task \"" + task.getTaskTitle()
                                + "\" has been updated. Please review the changes.",
                        "TASK_UPDATED",
                        task.getTaskId().toString(),
                        null
                );

        } catch (Exception e) {

                System.err.println(
                        "[Notification] Failed to send task update: "
                                + e.getMessage()
                );
        }
        }
    }


    // =========================================================
    // CANCEL TASK
    // =========================================================

    public void cancelTask(
            UUID taskId
    ) throws Exception {

        validateTaskId(
                taskId
        );


        taskRepository.cancelTask(
                taskId,
                getAccessToken()
        );
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


        return switch (priority) {

            case "Low",
                 "Medium",
                 "High",
                 "Urgent" -> priority;

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


        return switch (difficulty) {

            case "Easy",
                 "Medium",
                 "Hard",
                 "Advanced" -> difficulty;

            default ->
                    throw new IllegalArgumentException(
                            "Invalid difficulty level."
                    );
        };
    }


    // =========================================================
    // CLEAN OPTIONAL TEXT
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