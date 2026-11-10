package com.aepm.logic.task;

import com.aepm.data.task.EmployeeDailyTaskRepository;
import com.aepm.model.EmployeeDailyTask;
import com.aepm.model.User;
import com.aepm.util.SessionManager;

import java.time.LocalDate;
import java.time.LocalTime;

import java.util.List;
import java.util.Set;
import java.util.UUID;


public class EmployeeDailyTaskService {

    private static final Set<String> CATEGORIES =
            Set.of(
                    "Email",
                    "Report",
                    "Meeting",
                    "Follow-up",
                    "Documentation",
                    "Development",
                    "Other"
            );


    private static final Set<String> PRIORITIES =
            Set.of(
                    "Low",
                    "Medium",
                    "High"
            );


    private final EmployeeDailyTaskRepository repository =
            new EmployeeDailyTaskRepository();


    // =========================================================
    // LOAD
    // =========================================================

    public List<EmployeeDailyTask> getTodayTasks()
            throws Exception {

        return getTasks(
                LocalDate.now()
        );
    }


    public List<EmployeeDailyTask> getTasks(
            LocalDate date
    ) throws Exception {

        if (date == null) {

            throw new IllegalArgumentException(
                    "Task date is required."
            );
        }


        return repository.getTasksForDate(
                currentEmployeeId(),
                date,
                accessToken()
        );
    }


    // =========================================================
    // CREATE
    // =========================================================

    public EmployeeDailyTask createTask(
            String title,
            String description,
            String category,
            String priority,
            LocalDate taskDate,
            LocalTime dueTime
    ) throws Exception {

        validate(
                title,
                category,
                priority,
                taskDate
        );


        EmployeeDailyTask task =
                new EmployeeDailyTask();


        task.setEmployeeId(
                currentEmployeeId()
        );


        task.setTaskTitle(
                title.trim()
        );


        task.setTaskDescription(
                normalizeOptional(
                        description
                )
        );


        task.setCategory(
                category
        );


        task.setPriority(
                priority
        );


        task.setTaskDate(
                taskDate
        );


        task.setDueTime(
                dueTime
        );


        task.setTaskStatus(
                "Pending"
        );


        return repository.createTask(
                task,
                accessToken()
        );
    }


    // =========================================================
    // UPDATE
    // =========================================================

    public EmployeeDailyTask updateTask(
            EmployeeDailyTask task,
            String title,
            String description,
            String category,
            String priority,
            LocalDate taskDate,
            LocalTime dueTime
    ) throws Exception {

        if (
                task == null
                        || task.getDailyTaskId() == null
        ) {

            throw new IllegalArgumentException(
                    "Select a daily task first."
            );
        }


        validate(
                title,
                category,
                priority,
                taskDate
        );


        task.setEmployeeId(
                currentEmployeeId()
        );


        task.setTaskTitle(
                title.trim()
        );


        task.setTaskDescription(
                normalizeOptional(
                        description
                )
        );


        task.setCategory(
                category
        );


        task.setPriority(
                priority
        );


        task.setTaskDate(
                taskDate
        );


        task.setDueTime(
                dueTime
        );


        return repository.updateTask(
                task,
                accessToken()
        );
    }


    // =========================================================
    // STATUS
    // =========================================================

    public EmployeeDailyTask markPending(
            UUID id
    ) throws Exception {

        return updateStatus(
                id,
                "Pending"
        );
    }


    public EmployeeDailyTask markInProgress(
            UUID id
    ) throws Exception {

        return updateStatus(
                id,
                "In Progress"
        );
    }


    public EmployeeDailyTask markCompleted(
            UUID id
    ) throws Exception {

        return updateStatus(
                id,
                "Completed"
        );
    }


    public EmployeeDailyTask cancelTask(
            UUID id
    ) throws Exception {

        return updateStatus(
                id,
                "Cancelled"
        );
    }


    private EmployeeDailyTask updateStatus(
            UUID id,
            String status
    ) throws Exception {

        if (id == null) {

            throw new IllegalArgumentException(
                    "Daily task is required."
            );
        }


        return repository.updateStatus(
                id,
                status,
                accessToken()
        );
    }


    // =========================================================
    // DELETE
    // =========================================================

    public void deleteTask(
            UUID id
    ) throws Exception {

        if (id == null) {

            throw new IllegalArgumentException(
                    "Daily task is required."
            );
        }


        repository.deleteTask(
                id,
                accessToken()
        );
    }


    // =========================================================
    // VALIDATION
    // =========================================================

    private void validate(
            String title,
            String category,
            String priority,
            LocalDate date
    ) {

        if (
                title == null
                        || title.isBlank()
        ) {

            throw new IllegalArgumentException(
                    "Task title is required."
            );
        }


        if (
                title.trim().length() > 150
        ) {

            throw new IllegalArgumentException(
                    "Task title cannot exceed 150 characters."
            );
        }


        if (!CATEGORIES.contains(category)) {

            throw new IllegalArgumentException(
                    "Invalid task category."
            );
        }


        if (!PRIORITIES.contains(priority)) {

            throw new IllegalArgumentException(
                    "Invalid task priority."
            );
        }


        if (date == null) {

            throw new IllegalArgumentException(
                    "Task date is required."
            );
        }
    }


    // =========================================================
    // CURRENT USER
    // =========================================================

    private UUID currentEmployeeId() {

        User currentUser =
                SessionManager.getCurrentUser();


        if (
                currentUser == null
                        || currentUser.getUserId() == null
                        || currentUser.getUserId().isBlank()
        ) {

            throw new IllegalStateException(
                    "Unable to identify current employee."
            );
        }


        if (
                currentUser.getRole() == null
                        || !"EMPLOYEE".equalsIgnoreCase(
                        currentUser.getRole().name()
                )
        ) {

            throw new SecurityException(
                    "Employee access required."
            );
        }


        return UUID.fromString(
                currentUser.getUserId()
        );
    }


    private String accessToken() {

        String token =
                SessionManager.getAccessToken();


        if (
                token == null
                        || token.isBlank()
        ) {

            throw new IllegalStateException(
                    "User session has expired."
            );
        }


        return token;
    }


    private String normalizeOptional(
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
}