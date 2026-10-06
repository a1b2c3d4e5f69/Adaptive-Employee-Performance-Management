package com.aepm.logic.task;

import com.aepm.data.task.TaskProgressRepository;
import com.aepm.model.TaskProgress;
import com.aepm.model.User;
import com.aepm.util.SessionManager;

import java.util.List;
import java.util.UUID;

public class TaskProgressService {

    private final TaskProgressRepository repository =
            new TaskProgressRepository();


    // =========================================================
    // ADD PROGRESS HISTORY
    // =========================================================

    public void addProgress(
            UUID taskId,
            double progress,
            String note
    ) throws Exception {

        User currentUser =
                SessionManager.getCurrentUser();


        if (currentUser == null) {

            throw new IllegalStateException(
                    "No logged-in user found."
            );
        }


        if (
                progress < 0
                        || progress > 100
        ) {

            throw new IllegalArgumentException(
                    "Progress must be between 0 and 100."
            );
        }


        TaskProgress taskProgress =
                new TaskProgress();


        taskProgress.setTaskId(
                taskId
        );


        taskProgress.setEmployeeId(
                UUID.fromString(
                        currentUser.getUserId()
                )
        );


        taskProgress.setProgressPercentage(
                progress
        );


        taskProgress.setProgressNote(
                note == null
                        || note.isBlank()
                        ? null
                        : note.trim()
        );


        repository.addProgressHistory(
                taskProgress,
                SessionManager.getAccessToken()
        );
    }


    // =========================================================
    // GET HISTORY
    // =========================================================

    public List<TaskProgress> getProgressHistory(
            UUID taskId
    ) throws Exception {

        return repository
                .getProgressHistory(
                        taskId,
                        SessionManager.getAccessToken()
                );
    }
} 