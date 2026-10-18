package com.aepm.ui.manager;

import com.aepm.logic.task.TaskCategoryService;
import com.aepm.logic.task.TaskService;
import com.aepm.logic.user.UserService;

import com.aepm.model.Role;
import com.aepm.model.TaskCategory;
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

    @FXML
    private Label topRoleLabel;
    
    @FXML
    private Label welcomeLabel;

    @FXML
    private Label topNameLabel;


    @FXML
    private TextField titleField;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private ComboBox<User> employeeComboBox;

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
    private Button createButton;


    private final TaskService taskService =
            new TaskService();

    private final TaskCategoryService categoryService =
            new TaskCategoryService();

    private final UserService userService =
            new UserService();


    @FXML
    private void handleDeadlineEscalation(ActionEvent event) {    
        Stage stage = (Stage) ((Node) event.getSource())
                .getScene()
                .getWindow();
    
        NavigationManager.navigate(
                stage,
                "/fxml/manager/managerDeadlineEscalation.fxml"
        );
    }

    @FXML
    public void initialize() {

        UIHelper.hideMessage(
                messageLabel
        );

        updateManagerRoleLabel();
        loadCurrentUser();

        configurePriority();

        configureDifficulty();

        configureEmployeeComboBox();

        configureCategoryComboBox();

        loadEmployees();

        loadCategories();


        dueDatePicker.setValue(
                LocalDate.now()
                        .plusDays(1)
        );
    }


    // =========================================================
    // USER
    // =========================================================

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
    // EMPLOYEE DISPLAY
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


                        String staffId =
                                user.getEmployeeId() == null
                                        ? "-"
                                        : user.getEmployeeId();


                        String name =
                                user.getFullName() == null
                                        ? "-"
                                        : user.getFullName();


                        return staffId
                                + " - "
                                + name;
                    }


                    @Override
                    public User fromString(
                            String value
                    ) {

                        return null;
                    }
                }
        );
    }


    // =========================================================
    // CATEGORY DISPLAY
    // =========================================================

    private void configureCategoryComboBox() {

        categoryComboBox.setConverter(
                new StringConverter<>() {

                    @Override
                    public String toString(
                            TaskCategory category
                    ) {

                        if (category == null) {
                            return "";
                        }


                        return category.getCategoryName();
                    }


                    @Override
                    public TaskCategory fromString(
                            String value
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

                            boolean employee =
                                    user.getRole() == Role.EMPLOYEE
                                            || user.getRoleId() == 3;


                            boolean active =
                                    "Active".equalsIgnoreCase(
                                            user.getAccountStatus()
                                    );


                            if (
                                    employee
                                            && active
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
                new Thread(task);


        thread.setDaemon(true);

        thread.start();
    }


    // =========================================================
    // LOAD CATEGORIES
    // =========================================================

    private void loadCategories() {

        categoryComboBox.setDisable(
                true
        );


        Task<List<TaskCategory>> task =
                new Task<>() {

                    @Override
                    protected List<TaskCategory> call()
                            throws Exception {

                        return categoryService
                                .getActiveCategories();
                    }
                };


        task.setOnSucceeded(
                event -> {

                    categoryComboBox
                            .getItems()
                            .setAll(
                                    task.getValue()
                            );


                    categoryComboBox.setDisable(
                            false
                    );


                    if (
                            !categoryComboBox
                                    .getItems()
                                    .isEmpty()
                    ) {

                        categoryComboBox
                                .getSelectionModel()
                                .selectFirst();
                    }
                }
        );


        task.setOnFailed(
                event -> {

                    categoryComboBox.setDisable(
                            false
                    );


                    Throwable error =
                            task.getException();


                    UIHelper.showError(
                            messageLabel,
                            error == null
                                    ? "Unable to load task categories."
                                    : error.getMessage()
                    );
                }
        );


        Thread thread =
                new Thread(task);


        thread.setDaemon(true);

        thread.start();
    }


    // =========================================================
    // CREATE
    // =========================================================

    @FXML
    private void handleCreateTask(
            ActionEvent event
    ) {

        String title =
                titleField
                        .getText()
                        .trim();


        User employee =
                employeeComboBox
                        .getValue();


        TaskCategory category =
                categoryComboBox
                        .getValue();


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


        if (employee == null) {

            UIHelper.showError(
                    messageLabel,
                    "Please select an employee."
            );

            return;
        }


        if (category == null) {

            UIHelper.showError(
                    messageLabel,
                    "Please select a task category."
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


        createButton.setDisable(
                true
        );


        Task<Void> createTask =
                new Task<>() {

                    @Override
                    protected Void call()
                            throws Exception {

                        taskService.createTask(
                                title,
                                descriptionArea
                                        .getText()
                                        .trim(),
                                category.getCategoryId(),
                                java.util.UUID.fromString(
                                        employee.getUserId()
                                ),
                                priorityComboBox.getValue(),
                                difficultyComboBox.getValue(),
                                dueDateTime,
                                completionCriteriaArea
                                        .getText()
                                        .trim()
                        );


                        return null;
                    }
                };


        createTask.setOnSucceeded(
                e -> {

                    navigate(
                            event,
                            "/fxml/manager/managerTask.fxml"
                    );
                }
        );


        createTask.setOnFailed(
                e -> {

                    createButton.setDisable(
                            false
                    );


                    Throwable error =
                            createTask.getException();


                    UIHelper.showError(
                            messageLabel,
                            error == null
                                    ? "Unable to create task."
                                    : error.getMessage()
                    );
                }
        );


        Thread thread =
                new Thread(createTask);


        thread.setDaemon(true);

        thread.start();
    }


    @FXML
    private void handleBack(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/manager/managerTask.fxml"
        );
    }


    @FXML
    private void handleHome(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/manager/managerHome.fxml"
        );
    }


    @FXML
    private void handleProfile(
            ActionEvent event
    ) {

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
        private void handlePerformance(
                ActionEvent event
        ) {

        Stage stage =
                (Stage) ((Node) event.getSource())
                        .getScene()
                        .getWindow();


        NavigationManager.navigate(
                stage,
                "/fxml/manager/managerPerformance.fxml"
        );
        }


    @FXML
    private void handleReports() {
    }


    @FXML
    private void handleDashboard(ActionEvent event) {
    
        navigate(
                event,
                "/fxml/manager/managerDashboard.fxml"
        );
    }

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

    private void updateManagerRoleLabel() {
        User manager = SessionManager.getCurrentUser();

        if (manager == null) {
                topRoleLabel.setText("MANAGER");
                return;
        }

        String managerId = manager.getEmployeeId();

        if (managerId != null && !managerId.isBlank()) {
                topRoleLabel.setText("MANAGER  •  " + managerId);
        } else {
                topRoleLabel.setText("MANAGER");
        }
    }
}