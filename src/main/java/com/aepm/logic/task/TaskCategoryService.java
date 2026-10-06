package com.aepm.logic.task;

import com.aepm.data.task.TaskCategoryRepository;
import com.aepm.model.TaskCategory;
import com.aepm.util.SessionManager;

import java.util.List;

public class TaskCategoryService {

    private final TaskCategoryRepository repository =
            new TaskCategoryRepository();


    public List<TaskCategory> getActiveCategories()
            throws Exception {

        String accessToken =
                SessionManager.getAccessToken();


        if (
                accessToken == null
                        || accessToken.isBlank()
        ) {

            throw new IllegalStateException(
                    "User session has expired."
            );
        }


        return repository
                .getActiveCategories(
                        accessToken
                );
    }
}