package com.aepm.logic.task;

import com.aepm.data.task.TaskDependencyRepository;
import com.aepm.model.Role;
import com.aepm.model.TaskDependency;
import com.aepm.model.User;
import com.aepm.util.SessionManager;

import java.util.List;
import java.util.UUID;

public class TaskDependencyService {

    private final TaskDependencyRepository repository =
            new TaskDependencyRepository();


    // =========================================================
    // ADMIN - GET ALL
    // =========================================================

    public List<TaskDependency> getAllDependencies()
            throws Exception {

        requireAdmin();


        return repository
                .getAllDependencies(
                        requireAccessToken()
                );
    }


    // =========================================================
    // ACTIVE DEPENDENCIES
    // =========================================================

    public List<TaskDependency> getActiveDependencies()
            throws Exception {

        return repository
                .getActiveDependencies(
                        requireAccessToken()
                );
    }


    // =========================================================
    // CREATE
    // =========================================================

    public void createDependency(
            UUID prerequisiteTaskId,
            UUID nextTaskId
    ) throws Exception {

        User admin =
                requireAdmin();


        if (
                prerequisiteTaskId == null
                        || nextTaskId == null
        ) {

            throw new IllegalArgumentException(
                    "Both prerequisite task and next task are required."
            );
        }


        if (
                prerequisiteTaskId.equals(
                        nextTaskId
                )
        ) {

            throw new IllegalArgumentException(
                    "A task cannot depend on itself."
            );
        }


        String accessToken =
                requireAccessToken();


        boolean exists =
                repository.dependencyExists(
                        prerequisiteTaskId,
                        nextTaskId,
                        accessToken
                );


        if (exists) {

            throw new IllegalArgumentException(
                    "This task dependency already exists."
            );
        }


        TaskDependency dependency =
                new TaskDependency();


        dependency.setPrerequisiteTaskId(
                prerequisiteTaskId
        );


        dependency.setNextTaskId(
                nextTaskId
        );


        dependency.setRuleStatus(
                "Active"
        );


        dependency.setCreatedBy(
                UUID.fromString(
                        admin.getUserId()
                )
        );


        repository.createDependency(
                dependency,
                accessToken
        );
    }


    // =========================================================
    // ACTIVATE / DEACTIVATE
    // =========================================================

    public void updateRuleStatus(
            Long dependencyId,
            String status
    ) throws Exception {

        requireAdmin();


        if (dependencyId == null) {

            throw new IllegalArgumentException(
                    "Invalid task dependency."
            );
        }


        if (
                !"Active".equals(status)
                        && !"Inactive".equals(status)
        ) {

            throw new IllegalArgumentException(
                    "Invalid dependency status."
            );
        }


        repository.updateRuleStatus(
                dependencyId,
                status,
                requireAccessToken()
        );
    }


    // =========================================================
    // DELETE
    // =========================================================

    public void deleteDependency(
            Long dependencyId
    ) throws Exception {

        requireAdmin();


        if (dependencyId == null) {

            throw new IllegalArgumentException(
                    "Invalid task dependency."
            );
        }


        repository.deleteDependency(
                dependencyId,
                requireAccessToken()
        );
    }


    // =========================================================
    // TASK LOCK CHECK
    // =========================================================

    public List<TaskDependency> getDependenciesForTask(
            UUID taskId
    ) throws Exception {

        if (taskId == null) {

            throw new IllegalArgumentException(
                    "Invalid task."
            );
        }


        return repository
                .getDependenciesForNextTask(
                        taskId,
                        requireAccessToken()
                );
    }


    // =========================================================
    // SESSION
    // =========================================================

    private String requireAccessToken() {

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


    private User requireAdmin() {

        User user =
                SessionManager.getCurrentUser();


        if (user == null) {

            throw new IllegalStateException(
                    "User session has expired."
            );
        }


        boolean admin =
                user.getRole() == Role.ADMINISTRATOR
                        || user.getRoleId() == 1;


        if (!admin) {

            throw new SecurityException(
                    "Only administrators can configure progressive task rules."
            );
        }


        return user;
    }
}