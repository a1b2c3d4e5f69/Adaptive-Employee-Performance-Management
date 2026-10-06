package com.aepm.ui.manager;

import com.aepm.logic.task.TaskService;
import com.aepm.model.Task;
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


    private Task task;


    @FXML
    public void initialize() {

        UIHelper.hideMessage(
                messageLabel
        );


        loadCurrentUser();

        configureOptions();

        loadTask();
    }


    private void loadCurrentUser() {

        User user =
                SessionManager.getCurrentUser();


        if (user == null) {
            return;
        }


        String name =
                user.getFullName() == null
                        || user.getFullName().isBlank()
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

            LocalDate localDate =
                    task.getDueDate()
                            .atZoneSameInstant(
                                    ZoneId.of(
                                            "Asia/Kuala_Lumpur"
                                    )
                            )
                            .toLocalDate();


            dueDatePicker.setValue(
                    localDate
            );
        }
    }


    @FXML
    private void handleSave(
            ActionEvent event
    ) {

        if (task == null) {
            return;
        }


        String title =
                titleField
                        .getText()
                        .trim();


        LocalDate dueDate =
                dueDatePicker
                        .getValue();


        if (title.isBlank()) {

            UIHelper.showError(
                    messageLabel,
                    "Task title is required."
            );

            return;
        }


        if (dueDate == null) {

            UIHelper.showError(
                    messageLabel,
                    "Due date is required."
            );

            return;
        }


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


        task.setTaskTitle(
                title
        );


        task.setTaskDescription(
                descriptionArea
                        .getText()
                        .trim()
        );


        task.setPriority(
                priorityComboBox
                        .getValue()
        );


        task.setDifficultyLevel(
                difficultyComboBox
                        .getValue()
        );


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
                e -> {

                    saveButton.setDisable(
                            false
                    );


                    ManagerTaskEditContext.clear();


                    navigate(
                            event,
                            "/fxml/manager/managerTask.fxml"
                    );
                }
        );


        saveTask.setOnFailed(
                e -> {

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
                new Thread(
                        saveTask
                );


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
    private void handleHome(
            ActionEvent event
    ) {

        ManagerTaskEditContext.clear();


        navigate(
                event,
                "/fxml/manager/managerHome.fxml"
        );
    }


    @FXML
    private void handleProfile(
            ActionEvent event
    ) {

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
        navigate(
            event,
            "/fxml/manager/managerTask.fxml"
        );
    }


    @FXML
    private void handlePerformance() {

        System.out.println(
                "Performance not implemented yet."
        );
    }


    @FXML
    private void handleReports() {

        System.out.println(
                "Reports not implemented yet."
        );
    }


    @FXML
    private void handleDashboard() {

        System.out.println(
                "Dashboard not implemented yet."
        );
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