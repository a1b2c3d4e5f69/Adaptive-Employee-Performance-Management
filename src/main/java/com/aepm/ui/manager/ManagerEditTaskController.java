package com.aepm.ui.manager;

import com.aepm.logic.task.TaskCategoryService;
import com.aepm.logic.task.TaskService;

import com.aepm.model.Task;
import com.aepm.model.TaskCategory;
import com.aepm.model.User;

import com.aepm.ui.components.UIHelper;
import com.aepm.ui.navigation.NavigationManager;

import com.aepm.util.ManagerTaskEditContext;
import com.aepm.util.SessionManager;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;

import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import java.util.List;

public class ManagerEditTaskController {

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label topNameLabel;


    @FXML
    private TextField titleField;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private ComboBox<TaskCategory> categoryComboBox;

    @FXML
    private ComboBox<String> priorityComboBox;

    @FXML
    private ComboBox<String> difficultyComboBox;

    @FXML
    private DatePicker dueDatePicker;

    @FXML
    private TextArea completionCriteriaArea;


    @FXML
    private Label messageLabel;

    @FXML
    private Button saveButton;


    private final TaskService taskService =
            new TaskService();

    private final TaskCategoryService categoryService =
            new TaskCategoryService();


    private Task task;


    @FXML
    public void initialize() {

        UIHelper.hideMessage(
                messageLabel
        );


        loadCurrentUser();

        configureOptions();

        loadCategories();
    }


    private void loadCurrentUser() {

        User user =
                SessionManager.getCurrentUser();


        if (user == null) {
            return;
        }


        String name =
                user.getFullName() == null
                        ? "Manager"
                        : user.getFullName();


        welcomeLabel.setText(
                "Welcome, " + name
        );


        topNameLabel.setText(
                name
        );
    }


    private void configureOptions() {

        priorityComboBox.setItems(
                FXCollections.observableArrayList(
                        "Low",
                        "Medium",
                        "High",
                        "Urgent"
                )
        );


        difficultyComboBox.setItems(
                FXCollections.observableArrayList(
                        "Easy",
                        "Medium",
                        "Hard",
                        "Advanced"
                )
        );
    }


    // =========================================================
    // LOAD CATEGORIES THEN TASK
    // =========================================================

    private void loadCategories() {

        categoryComboBox.setDisable(
                true
        );


        javafx.concurrent.Task<List<TaskCategory>> categoryTask =
                new javafx.concurrent.Task<>() {

                    @Override
                    protected List<TaskCategory> call()
                            throws Exception {

                        return categoryService
                                .getActiveCategories();
                    }
                };


        categoryTask.setOnSucceeded(
                event -> {

                    categoryComboBox
                            .getItems()
                            .setAll(
                                    categoryTask.getValue()
                            );


                    categoryComboBox.setDisable(
                            false
                    );


                    loadTask();
                }
        );


        categoryTask.setOnFailed(
                event -> {

                    categoryComboBox.setDisable(
                            false
                    );


                    Throwable error =
                            categoryTask.getException();


                    UIHelper.showError(
                            messageLabel,
                            error == null
                                    ? "Unable to load categories."
                                    : error.getMessage()
                    );


                    loadTask();
                }
        );


        Thread thread =
                new Thread(
                        categoryTask
                );


        thread.setDaemon(true);

        thread.start();
    }


    // =========================================================
    // LOAD TASK
    // =========================================================

    private void loadTask() {

        task =
                ManagerTaskEditContext
                        .getTask();


        if (task == null) {

            UIHelper.showError(
                    messageLabel,
                    "No task selected."
            );


            saveButton.setDisable(
                    true
            );


            return;
        }


        titleField.setText(
                safe(
                        task.getTaskTitle()
                )
        );


        descriptionArea.setText(
                safe(
                        task.getTaskDescription()
                )
        );


        priorityComboBox.setValue(
                task.getPriority()
        );


        difficultyComboBox.setValue(
                task.getDifficultyLevel()
        );


        completionCriteriaArea.setText(
                safe(
                        task.getCompletionCriteria()
                )
        );


        if (
                task.getDueDate() != null
        ) {

            LocalDate date =
                    task.getDueDate()
                            .atZoneSameInstant(
                                    ZoneId.of(
                                            "Asia/Kuala_Lumpur"
                                    )
                            )
                            .toLocalDate();


            dueDatePicker.setValue(
                    date
            );
        }


        // =====================================================
        // SELECT CURRENT CATEGORY
        // =====================================================

        if (
                task.getCategoryId() != null
        ) {

            for (
                    TaskCategory category
                    : categoryComboBox.getItems()
            ) {

                if (
                        task.getCategoryId()
                                .equals(
                                        category.getCategoryId()
                                )
                ) {

                    categoryComboBox.setValue(
                            category
                    );

                    break;
                }
            }
        }
    }


    // =========================================================
    // SAVE
    // =========================================================

    @FXML
    private void handleSave(
            ActionEvent event
    ) {

        if (task == null) {
            return;
        }


        TaskCategory category =
                categoryComboBox
                        .getValue();


        if (category == null) {

            UIHelper.showError(
                    messageLabel,
                    "Please select a category."
            );

            return;
        }


        LocalDate dueDate =
                dueDatePicker
                        .getValue();


        if (dueDate == null) {

            UIHelper.showError(
                    messageLabel,
                    "Due date is required."
            );

            return;
        }


        task.setTaskTitle(
                titleField
                        .getText()
                        .trim()
        );


        task.setTaskDescription(
                descriptionArea
                        .getText()
                        .trim()
        );


        task.setCategoryId(
                category.getCategoryId()
        );


        task.setPriority(
                priorityComboBox
                        .getValue()
        );


        task.setDifficultyLevel(
                difficultyComboBox
                        .getValue()
        );


        OffsetDateTime dueDateTime =
                dueDate
                        .atTime(
                                23,
                                59
                        )
                        .atZone(
                                ZoneId.of(
                                        "Asia/Kuala_Lumpur"
                                )
                        )
                        .toOffsetDateTime();


        task.setDueDate(
                dueDateTime
        );


        task.setCompletionCriteria(
                completionCriteriaArea
                        .getText()
                        .trim()
        );


        saveButton.setDisable(
                true
        );


        javafx.concurrent.Task<Void> saveTask =
                new javafx.concurrent.Task<>() {

                    @Override
                    protected Void call()
                            throws Exception {

                        taskService.updateTask(
                                task
                        );


                        return null;
                    }
                };


        saveTask.setOnSucceeded(
                event2 -> {

                    ManagerTaskEditContext.clear();


                    navigate(
                            event,
                            "/fxml/manager/managerTask.fxml"
                    );
                }
        );


        saveTask.setOnFailed(
                event2 -> {

                    saveButton.setDisable(
                            false
                    );


                    Throwable error =
                            saveTask.getException();


                    UIHelper.showError(
                            messageLabel,
                            error == null
                                    ? "Unable to update task."
                                    : error.getMessage()
                    );
                }
        );


        Thread thread =
                new Thread(saveTask);


        thread.setDaemon(true);

        thread.start();
    }


    @FXML
    private void handleBack(
            ActionEvent event
    ) {

        ManagerTaskEditContext.clear();

        navigate(
                event,
                "/fxml/manager/managerTask.fxml"
        );
    }


    @FXML
    private void handleHome(ActionEvent event) {

        ManagerTaskEditContext.clear();

        navigate(
                event,
                "/fxml/manager/managerHome.fxml"
        );
    }


    @FXML
    private void handleProfile(ActionEvent event) {

        ManagerTaskEditContext.clear();

        navigate(
                event,
                "/fxml/manager/managerProfile.fxml"
        );
    }


    @FXML
    private void handleTaskManagement(
            ActionEvent event
    ) {

        handleBack(
                event
        );
    }


    @FXML
    private void handlePerformance() {
    }


    @FXML
    private void handleReports() {
    }


    @FXML
    private void handleDashboard() {
    }


    @FXML
    private void handleLogout(
            ActionEvent event
    ) {

        ManagerTaskEditContext.clear();

        SessionManager.clearSession();


        navigate(
                event,
                "/fxml/auth/login.fxml"
        );
    }


    private String safe(
            String value
    ) {

        return value == null
                ? ""
                : value;
    }


    private void navigate(
            ActionEvent event,
            String path
    ) {

        Stage stage =
                (Stage) ((Node) event.getSource())
                        .getScene()
                        .getWindow();


        NavigationManager.navigate(
                stage,
                path
        );
    }
}