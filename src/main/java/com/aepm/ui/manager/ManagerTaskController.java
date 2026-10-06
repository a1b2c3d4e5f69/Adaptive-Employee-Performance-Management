package com.aepm.ui.manager;

import com.aepm.logic.task.TaskCategoryService;
import com.aepm.logic.task.TaskProgressService;
import com.aepm.logic.task.TaskService;
import com.aepm.logic.user.UserService;

import com.aepm.model.Task;
import com.aepm.model.TaskCategory;
import com.aepm.model.TaskProgress;
import com.aepm.model.User;

import com.aepm.ui.navigation.NavigationManager;

import com.aepm.util.ManagerTaskEditContext;
import com.aepm.util.SessionManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import javafx.scene.control.cell.PropertyValueFactory;

import javafx.stage.Stage;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ManagerTaskController {

    // =========================================================
    // TOP BAR
    // =========================================================

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label topNameLabel;


    // =========================================================
    // TASK TABLE
    // =========================================================

    @FXML
    private TableView<Task> taskTable;

    @FXML
    private TableColumn<Task, String> titleColumn;

    @FXML
    private TableColumn<Task, String> categoryColumn;

    @FXML
    private TableColumn<Task, String> employeeColumn;

    @FXML
    private TableColumn<Task, String> priorityColumn;

    @FXML
    private TableColumn<Task, String> difficultyColumn;

    @FXML
    private TableColumn<Task, String> statusColumn;

    @FXML
    private TableColumn<Task, Double> progressColumn;

    @FXML
    private TableColumn<Task, OffsetDateTime> dueDateColumn;


    // =========================================================
    // SELECTED TASK
    // =========================================================

    @FXML
    private Label selectedTaskLabel;

    @FXML
    private Label selectedCategoryLabel;

    @FXML
    private Label selectedEmployeeLabel;

    @FXML
    private Label selectedStatusLabel;

    @FXML
    private Label selectedProgressLabel;


    // =========================================================
    // PROGRESS HISTORY
    // =========================================================

    @FXML
    private TableView<TaskProgress> historyTable;

    @FXML
    private TableColumn<TaskProgress, Double> historyProgressColumn;

    @FXML
    private TableColumn<TaskProgress, String> historyNoteColumn;

    @FXML
    private TableColumn<TaskProgress, OffsetDateTime> historyDateColumn;


    // =========================================================
    // MESSAGE
    // =========================================================

    @FXML
    private Label messageLabel;

    @FXML
    private Label historyMessageLabel;


    // =========================================================
    // SERVICES
    // =========================================================

    private final TaskService taskService =
            new TaskService();

    private final TaskProgressService taskProgressService =
            new TaskProgressService();

    private final TaskCategoryService categoryService =
            new TaskCategoryService();

    private final UserService userService =
            new UserService();


    // =========================================================
    // LOOKUP MAPS
    // =========================================================

    private final Map<String, String> employeeDisplayMap =
            new HashMap<>();

    private final Map<Long, String> categoryDisplayMap =
            new HashMap<>();


    private Task selectedTask;


    private static final ZoneId MALAYSIA_ZONE =
            ZoneId.of("Asia/Kuala_Lumpur");


    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {

        loadCurrentUser();

        setupTaskTable();

        setupHistoryTable();

        configureTaskSelection();

        clearSelectedTask();

        loadReferenceDataAndTasks();
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
    // TASK TABLE
    // =========================================================

    private void setupTaskTable() {

        // TASK
        titleColumn.setCellValueFactory(
                new PropertyValueFactory<>(
                        "taskTitle"
                )
        );


        // CATEGORY
        categoryColumn.setCellValueFactory(
                cellData -> {

                    Task task =
                            cellData.getValue();


                    if (
                            task == null
                                    || task.getCategoryId() == null
                    ) {

                        return new SimpleStringProperty(
                                "-"
                        );
                    }


                    String categoryName =
                            categoryDisplayMap.get(
                                    task.getCategoryId()
                            );


                    return new SimpleStringProperty(
                            categoryName == null
                                    ? "-"
                                    : categoryName
                    );
                }
        );


        // EMPLOYEE
        employeeColumn.setCellValueFactory(
                cellData -> {

                    Task task =
                            cellData.getValue();


                    if (
                            task == null
                                    || task.getAssignedEmployeeId() == null
                    ) {

                        return new SimpleStringProperty(
                                "-"
                        );
                    }


                    String id =
                            task.getAssignedEmployeeId()
                                    .toString();


                    return new SimpleStringProperty(
                            employeeDisplayMap.getOrDefault(
                                    id,
                                    id
                            )
                    );
                }
        );


        priorityColumn.setCellValueFactory(
                new PropertyValueFactory<>(
                        "priority"
                )
        );


        difficultyColumn.setCellValueFactory(
                new PropertyValueFactory<>(
                        "difficultyLevel"
                )
        );


        statusColumn.setCellValueFactory(
                new PropertyValueFactory<>(
                        "taskStatus"
                )
        );


        progressColumn.setCellValueFactory(
                new PropertyValueFactory<>(
                        "progress"
                )
        );


        dueDateColumn.setCellValueFactory(
                new PropertyValueFactory<>(
                        "dueDate"
                )
        );


        // PROGRESS
        progressColumn.setCellFactory(
                column ->
                        new TableCell<>() {

                            @Override
                            protected void updateItem(
                                    Double value,
                                    boolean empty
                            ) {

                                super.updateItem(
                                        value,
                                        empty
                                );


                                setText(
                                        empty || value == null
                                                ? "-"
                                                : String.format(
                                                        "%.0f%%",
                                                        value
                                                )
                                );
                            }
                        }
        );


        // DUE DATE
        dueDateColumn.setCellFactory(
                column ->
                        new TableCell<>() {

                            private final DateTimeFormatter formatter =
                                    DateTimeFormatter.ofPattern(
                                            "dd MMM yyyy HH:mm"
                                    );


                            @Override
                            protected void updateItem(
                                    OffsetDateTime value,
                                    boolean empty
                            ) {

                                super.updateItem(
                                        value,
                                        empty
                                );


                                if (
                                        empty
                                                || value == null
                                ) {

                                    setText("-");

                                } else {

                                    setText(
                                            value
                                                    .atZoneSameInstant(
                                                            MALAYSIA_ZONE
                                                    )
                                                    .format(
                                                            formatter
                                                    )
                                    );
                                }
                            }
                        }
        );


        taskTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );
    }


    // =========================================================
    // HISTORY TABLE
    // =========================================================

    private void setupHistoryTable() {

        historyProgressColumn.setCellValueFactory(
                new PropertyValueFactory<>(
                        "progressPercentage"
                )
        );


        historyNoteColumn.setCellValueFactory(
                new PropertyValueFactory<>(
                        "progressNote"
                )
        );


        historyDateColumn.setCellValueFactory(
                new PropertyValueFactory<>(
                        "updatedAt"
                )
        );


        historyProgressColumn.setCellFactory(
                column ->
                        new TableCell<>() {

                            @Override
                            protected void updateItem(
                                    Double value,
                                    boolean empty
                            ) {

                                super.updateItem(
                                        value,
                                        empty
                                );


                                setText(
                                        empty || value == null
                                                ? "-"
                                                : String.format(
                                                        "%.0f%%",
                                                        value
                                                )
                                );
                            }
                        }
        );


        historyDateColumn.setCellFactory(
                column ->
                        new TableCell<>() {

                            private final DateTimeFormatter formatter =
                                    DateTimeFormatter.ofPattern(
                                            "dd MMM yyyy HH:mm"
                                    );


                            @Override
                            protected void updateItem(
                                    OffsetDateTime value,
                                    boolean empty
                            ) {

                                super.updateItem(
                                        value,
                                        empty
                                );


                                if (
                                        empty
                                                || value == null
                                ) {

                                    setText("-");

                                } else {

                                    setText(
                                            value
                                                    .atZoneSameInstant(
                                                            MALAYSIA_ZONE
                                                    )
                                                    .format(
                                                            formatter
                                                    )
                                    );
                                }
                            }
                        }
        );


        historyTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );
    }


    // =========================================================
    // TASK SELECTION
    // =========================================================

    private void configureTaskSelection() {

        taskTable
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> {

                            if (newValue != null) {

                                selectedTask =
                                        newValue;


                                displaySelectedTask(
                                        newValue
                                );


                                loadProgressHistory(
                                        newValue
                                );
                            }
                        }
                );
    }


    // =========================================================
    // DISPLAY SELECTED TASK
    // =========================================================

    private void displaySelectedTask(
            Task task
    ) {

        selectedTaskLabel.setText(
                task.getTaskTitle()
        );


        // CATEGORY
        if (task.getCategoryId() == null) {

            selectedCategoryLabel.setText(
                    "-"
            );

        } else {

            selectedCategoryLabel.setText(
                    categoryDisplayMap.getOrDefault(
                            task.getCategoryId(),
                            "-"
                    )
            );
        }


        // EMPLOYEE
        if (
                task.getAssignedEmployeeId() == null
        ) {

            selectedEmployeeLabel.setText(
                    "-"
            );

        } else {

            String employeeId =
                    task.getAssignedEmployeeId()
                            .toString();


            selectedEmployeeLabel.setText(
                    employeeDisplayMap.getOrDefault(
                            employeeId,
                            employeeId
                    )
            );
        }


        selectedStatusLabel.setText(
                task.getTaskStatus()
        );


        selectedProgressLabel.setText(
                String.format(
                        "%.0f%%",
                        task.getProgress()
                )
        );
    }


    // =========================================================
    // CLEAR SELECTED TASK
    // =========================================================

    private void clearSelectedTask() {

        selectedTask =
                null;


        selectedTaskLabel.setText(
                "No task selected"
        );


        selectedCategoryLabel.setText(
                "-"
        );


        selectedEmployeeLabel.setText(
                "-"
        );


        selectedStatusLabel.setText(
                "-"
        );


        selectedProgressLabel.setText(
                "-"
        );


        historyTable
                .getItems()
                .clear();


        showHistoryMessage(
                "Select a task to view progress history."
        );
    }


    // =========================================================
    // LOAD USERS + CATEGORIES + TASKS
    // =========================================================

    private void loadReferenceDataAndTasks() {

        hideMessage();


        javafx.concurrent.Task<Void> referenceTask =
                new javafx.concurrent.Task<>() {

                    @Override
                    protected Void call()
                            throws Exception {

                        // =====================================
                        // EMPLOYEES
                        // =====================================

                        List<User> users =
                                userService
                                        .getAllUsers();


                        employeeDisplayMap.clear();


                        if (users != null) {

                            for (User user : users) {

                                if (
                                        user == null
                                                || user.getRoleId() != 3
                                ) {

                                    continue;
                                }


                                if (
                                        user.getUserId() == null
                                                || user.getUserId().isBlank()
                                ) {

                                    continue;
                                }


                                String staffId =
                                        user.getEmployeeId() == null
                                                || user.getEmployeeId().isBlank()
                                                ? "-"
                                                : user.getEmployeeId();


                                String name =
                                        user.getFullName() == null
                                                || user.getFullName().isBlank()
                                                ? "-"
                                                : user.getFullName();


                                employeeDisplayMap.put(
                                        user.getUserId(),
                                        staffId
                                                + " - "
                                                + name
                                );
                            }
                        }


                        // =====================================
                        // CATEGORIES
                        // =====================================

                        List<TaskCategory> categories =
                                categoryService
                                        .getActiveCategories();


                        categoryDisplayMap.clear();


                        if (categories != null) {

                            for (
                                    TaskCategory category
                                    : categories
                            ) {

                                if (
                                        category == null
                                                || category.getCategoryId() == null
                                ) {

                                    continue;
                                }


                                categoryDisplayMap.put(
                                        category.getCategoryId(),
                                        category.getCategoryName()
                                );
                            }
                        }


                        return null;
                    }
                };


        referenceTask.setOnSucceeded(
                event -> loadTasks()
        );


        referenceTask.setOnFailed(
                event -> {

                    Throwable error =
                            referenceTask.getException();


                    if (error != null) {

                        error.printStackTrace();
                    }


                    // Still load tasks
                    loadTasks();
                }
        );


        Thread thread =
                new Thread(
                        referenceTask
                );


        thread.setDaemon(true);

        thread.start();
    }


    // =========================================================
    // LOAD TASKS
    // =========================================================

    private void loadTasks() {

        javafx.concurrent.Task<List<Task>> loadTask =
                new javafx.concurrent.Task<>() {

                    @Override
                    protected List<Task> call()
                            throws Exception {

                        return taskService
                                .getAllTasks();
                    }
                };


        loadTask.setOnSucceeded(
                event -> {

                    taskTable
                            .getItems()
                            .clear();


                    List<Task> tasks =
                            loadTask.getValue();


                    if (tasks != null) {

                        taskTable
                                .getItems()
                                .addAll(
                                        tasks
                                );
                    }


                    taskTable.refresh();

                    clearSelectedTask();


                    if (
                            tasks == null
                                    || tasks.isEmpty()
                    ) {

                        showMessage(
                                "No tasks found."
                        );

                    } else {

                        hideMessage();
                    }
                }
        );


        loadTask.setOnFailed(
                event -> {

                    Throwable error =
                            loadTask.getException();


                    showMessage(
                            error == null
                                    ? "Unable to load tasks."
                                    : error.getMessage()
                    );
                }
        );


        Thread thread =
                new Thread(
                        loadTask
                );


        thread.setDaemon(true);

        thread.start();
    }


    // =========================================================
    // PROGRESS HISTORY
    // =========================================================

    private void loadProgressHistory(
            Task task
    ) {

        historyTable
                .getItems()
                .clear();


        showHistoryMessage(
                "Loading progress history..."
        );


        javafx.concurrent.Task<List<TaskProgress>> historyTask =
                new javafx.concurrent.Task<>() {

                    @Override
                    protected List<TaskProgress> call()
                            throws Exception {

                        return taskProgressService
                                .getProgressHistory(
                                        task.getTaskId()
                                );
                    }
                };


        historyTask.setOnSucceeded(
                event -> {

                    List<TaskProgress> history =
                            historyTask.getValue();


                    historyTable
                            .getItems()
                            .clear();


                    if (history != null) {

                        historyTable
                                .getItems()
                                .addAll(
                                        history
                                );
                    }


                    historyTable.refresh();


                    if (
                            history == null
                                    || history.isEmpty()
                    ) {

                        showHistoryMessage(
                                "No progress history yet."
                        );

                    } else {

                        hideHistoryMessage();
                    }
                }
        );


        historyTask.setOnFailed(
                event -> {

                    Throwable error =
                            historyTask.getException();


                    showHistoryMessage(
                            error == null
                                    ? "Unable to load progress history."
                                    : error.getMessage()
                    );
                }
        );


        Thread thread =
                new Thread(
                        historyTask
                );


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

        navigate(
                event,
                "/fxml/manager/managerCreateTask.fxml"
        );
    }


    // =========================================================
    // EDIT
    // =========================================================

    @FXML
    private void handleEditTask(
            ActionEvent event
    ) {

        if (selectedTask == null) {

            showMessage(
                    "Please select a task first."
            );

            return;
        }


        if (
                "Completed".equals(
                        selectedTask.getTaskStatus()
                )
                        || "Cancelled".equals(
                        selectedTask.getTaskStatus()
                )
        ) {

            showMessage(
                    "Completed or cancelled tasks cannot be edited."
            );

            return;
        }


        ManagerTaskEditContext.setTask(
                selectedTask
        );


        navigate(
                event,
                "/fxml/manager/managerEditTask.fxml"
        );
    }


    // =========================================================
    // CANCEL
    // =========================================================

    @FXML
    private void handleCancelTask() {

        if (selectedTask == null) {

            showMessage(
                    "Please select a task first."
            );

            return;
        }


        if (
                "Completed".equals(
                        selectedTask.getTaskStatus()
                )
        ) {

            showMessage(
                    "Completed tasks cannot be cancelled."
            );

            return;
        }


        if (
                "Cancelled".equals(
                        selectedTask.getTaskStatus()
                )
        ) {

            showMessage(
                    "This task is already cancelled."
            );

            return;
        }


        Alert confirmation =
                new Alert(
                        Alert.AlertType.CONFIRMATION
                );


        confirmation.setTitle(
                "Cancel Task"
        );


        confirmation.setHeaderText(
                "Cancel "
                        + selectedTask.getTaskTitle()
                        + "?"
        );


        confirmation.setContentText(
                "This task will be marked as Cancelled."
        );


        Optional<ButtonType> result =
                confirmation.showAndWait();


        if (
                result.isEmpty()
                        || result.get() != ButtonType.OK
        ) {

            return;
        }


        javafx.concurrent.Task<Void> cancelTask =
                new javafx.concurrent.Task<>() {

                    @Override
                    protected Void call()
                            throws Exception {

                        taskService.cancelTask(
                                selectedTask.getTaskId()
                        );


                        return null;
                    }
                };


        cancelTask.setOnSucceeded(
                event -> loadReferenceDataAndTasks()
        );


        cancelTask.setOnFailed(
                event -> {

                    Throwable error =
                            cancelTask.getException();


                    showMessage(
                            error == null
                                    ? "Unable to cancel task."
                                    : error.getMessage()
                    );
                }
        );


        Thread thread =
                new Thread(
                        cancelTask
                );


        thread.setDaemon(true);

        thread.start();
    }


    // =========================================================
    // REFRESH
    // =========================================================

    @FXML
    private void handleRefresh() {

        loadReferenceDataAndTasks();
    }


    // =========================================================
    // NAVIGATION
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
    private void handleTaskManagement() {
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

        SessionManager.clearSession();


        navigate(
                event,
                "/fxml/auth/login.fxml"
        );
    }


    // =========================================================
    // MESSAGE
    // =========================================================

    private void showMessage(
            String value
    ) {

        messageLabel.setText(
                value
        );


        messageLabel.setVisible(
                true
        );


        messageLabel.setManaged(
                true
        );
    }


    private void hideMessage() {

        messageLabel.setText(
                ""
        );


        messageLabel.setVisible(
                false
        );


        messageLabel.setManaged(
                false
        );
    }


    private void showHistoryMessage(
            String value
    ) {

        historyMessageLabel.setText(
                value
        );


        historyMessageLabel.setVisible(
                true
        );


        historyMessageLabel.setManaged(
                true
        );
    }


    private void hideHistoryMessage() {

        historyMessageLabel.setText(
                ""
        );


        historyMessageLabel.setVisible(
                false
        );


        historyMessageLabel.setManaged(
                false
        );
    }


    // =========================================================
    // NAVIGATION HELPER
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