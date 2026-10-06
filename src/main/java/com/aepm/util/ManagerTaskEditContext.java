package com.aepm.util;

import com.aepm.model.Task;

public final class ManagerTaskEditContext {

    private static Task selectedTask;


    private ManagerTaskEditContext() {
    }


    public static void setTask(
            Task task
    ) {

        selectedTask =
                task;
    }


    public static Task getTask() {

        return selectedTask;
    }


    public static void clear() {

        selectedTask =
                null;
    }
}