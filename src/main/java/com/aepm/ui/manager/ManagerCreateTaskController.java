package com.aepm.ui.manager;

import com.aepm.logic.task.TaskService;
import com.aepm.logic.user.UserService;
import com.aepm.model.Role;
import com.aepm.model.User;
import com.aepm.ui.components.UIHelper;
import com.aepm.ui.navigation.NavigationManager;
import com.aepm.util.SessionManager;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
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
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import java.util.List;

public class ManagerCreateTaskController {

    // =========================================================
    // TOP BAR
    // =========================================================

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label topNameLabel;


    // =========================================================
    // FORM
    // =========================================================

    @FXML
    private TextField titleField;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private ComboBox<User> employeeComboBox;

    @FXML
    private ComboBox<String> priorityComboBox;

    @FXML
    private ComboBox<String> difficultyComboBox;

    @FXML
    private DatePicker dueDatePicker;

    @FXML
    private TextArea completionCriteriaArea;


    // =========================================================
    // MESSAGE / BUTTON
    // =========================================================

    @FXML
    private Label messageLabel;

    @FXML
    private Button createButton;


    private final TaskService taskService =
            new TaskService();

    private final UserService userService =
            new UserService();


    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {

        UIHelper.hideMessage(
                messageLabel
        );


        loadCurrentUser();

        configurePriority();

        configureDifficulty();

        configureEmployeeComboBox();

        loadEmployees();


        dueDatePicker.setValue(
                LocalDate.now()
                        .plusDays(1)
        );
    }


    // =========================================================
    // CURRENT USER
    // =========================================================

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


    // =========================================================
    // PRIORITY
    // =========================================================

    private void configurePriority() {

        priorityComboBox.setItems(
                FXCollections.observableArrayList(
                        "Low",
                        "Medium",
                        "High",
                        "Urgent"
                )
        );


        priorityComboBox.setValue(
                "Medium"
        );
    }


    // =========================================================
    // DIFFICULTY
    // =========================================================

    private void configureDifficulty() {

        difficultyComboBox.setItems(
                FXCollections.observableArrayList(
                        "Easy",
                        "Medium",
                        "Hard",
                        "Advanced"
                )
        );


        difficultyComboBox.setValue(
                "Medium"
        );
    }


    // =========================================================
    // EMPLOYEE COMBO BOX DISPLAY
    // =========================================================

    private void configureEmployeeComboBox() {

        employeeComboBox.setConverter(
                new StringConverter<>() {

                    @Override
                    public String toString(
                            User user
                    ) {

                        if (user == null) {
                            return "";
                        }


                        String employeeId =
                                user.getEmployeeId() == null
                                        ? "-"
                                        : user.getEmployeeId();


                        String name =
                                user.getFullName() == null
                                        ? "-"
                                        : user.getFullName();


                        return employeeId
                                + " - "
                                + name;
                    }


                    @Override
                    public User fromString(
                            String string
                    ) {

                        return null;
                    }
                }
        );
    }


    // =========================================================
    // LOAD EMPLOYEES
    // =========================================================

    private void loadEmployees() {

        employeeComboBox.setDisable(
                true
        );


        Task<List<User>> task =
                new Task<>() {

                    @Override
                    protected List<User> call()
                            throws Exception {

                        return userService
                                .getAllUsers();
                    }
                };


        task.setOnSucceeded(
                event -> {

                    employeeComboBox
                            .getItems()
                            .clear();


                    List<User> users =
                            task.getValue();


                    if (users != null) {

                        for (User user : users) {

                            boolean isEmployee =
                                    user.getRole() == Role.EMPLOYEE
                                            || user.getRoleId() == 3;


                            boolean isActive =
                                    "Active".equalsIgnoreCase(
                                            user.getAccountStatus()
                                    );


                            if (
                                    isEmployee
                                            && isActive
                            ) {

                                employeeComboBox
                                        .getItems()
                                        .add(
                                                user
                                        );
                            }
                        }
                    }


                    employeeComboBox.setDisable(
                            false
                    );


                    if (
                            employeeComboBox
                                    .getItems()
                                    .isEmpty()
                    ) {

                        UIHelper.showError(
                                messageLabel,
                                "No active employees found."
                        );
                    }
                }
        );


        task.setOnFailed(
                event -> {

                    employeeComboBox.setDisable(
                            false
                    );


                    Throwable error =
                            task.getException();


                    UIHelper.showError(
                            messageLabel,
                            error == null
                                    ? "Unable to load employees."
                                    : error.getMessage()
                    );
                }
        );


        Thread thread =
                new Thread(
                        task
                );


        thread.setDaemon(
                true
        );


        thread.start();
    }


    // =========================================================
    // CREATE TASK
    // =========================================================

    @FXML
    private void handleCreateTask(
            ActionEvent event
    ) {

        String title =
                titleField
                        .getText()
                        .trim();


        String description =
                descriptionArea
                        .getText()
                        .trim();


        User employee =
                employeeComboBox
                        .getValue();


        String priority =
                priorityComboBox
                        .getValue();


        String difficulty =
                difficultyComboBox
                        .getValue();


        LocalDate dueDate =
                dueDatePicker
                        .getValue();


        String criteria =
                completionCriteriaArea
                        .getText()
                        .trim();


        if (title.isBlank()) {

            UIHelper.showError(
                    messageLabel,
                    "Task title is required."
            );

            return;
        }


        if (employee == null) {

            UIHelper.showError(
                    messageLabel,
                    "Please select an employee."
            );

            return;
        }


        if (dueDate == null) {

            UIHelper.showError(
                    messageLabel,
                    "Please select a due date."
            );

            return;
        }


        /*
         * Due date stored as timestamp with timezone.
         * For now selected date = 11:59 PM.
         */
        OffsetDateTime dueDateTime =
                dueDate
                        .atTime(
                                23,
                                59
                        )
                        .atZone(
                                ZoneId.systemDefault()
                        )
                        .toOffsetDateTime();


        createButton.setDisable(
                true
        );


        UIHelper.hideMessage(
                messageLabel
        );


        Task<Void> task =
                new Task<>() {

                    @Override
                    protected Void call()
                            throws Exception {

                        taskService.createTask(
                                title,
                                description,
                                null,
                                java.util.UUID.fromString(
                                        employee.getUserId()
                                ),
                                priority,
                                difficulty,
                                dueDateTime,
                                criteria
                        );


                        return null;
                    }
                };


        task.setOnSucceeded(
                e -> {

                    createButton.setDisable(
                            false
                    );


                    UIHelper.showSuccess(
                            messageLabel,
                            "Task created successfully."
                    );


                    navigate(
                            event,
                            "/fxml/manager/managerTask.fxml"
                    );
                }
        );


        task.setOnFailed(
                e -> {

                    createButton.setDisable(
                            false
                    );


                    Throwable error =
                            task.getException();


                    UIHelper.showError(
                            messageLabel,
                            error == null
                                    ? "Unable to create task."
                                    : error.getMessage()
                    );
                }
        );


        Thread thread =
                new Thread(
                        task
                );


        thread.setDaemon(
                true
        );


        thread.start();
    }


    // =========================================================
    // BACK
    // =========================================================

    @FXML
    private void handleBack(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/manager/managerTask.fxml"
        );
    }


    // =========================================================
    // HOME
    // =========================================================

    @FXML
    private void handleHome(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/manager/managerHome.fxml"
        );
    }


    // =========================================================
    // PROFILE
    // =========================================================

    @FXML
    private void handleProfile(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/manager/managerProfile.fxml"
        );
    }


    // =========================================================
    // TASK MANAGEMENT
    // =========================================================

    @FXML
    private void handleTaskManagement(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/manager/managerTask.fxml"
        );
    }


    // =========================================================
    // OTHER MENU
    // =========================================================

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


    // =========================================================
    // LOGOUT
    // =========================================================

    @FXML
    private void handleLogout(
            ActionEvent event
    ) {

        SessionManager.clearSession();


        navigate(
                event,
                "/fxml/auth/login.fxml"
        );
    }


    // =========================================================
    // NAVIGATION
    // =========================================================

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