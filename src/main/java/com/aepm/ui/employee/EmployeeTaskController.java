package com.aepm.ui.employee;

import com.aepm.logic.task.TaskProgressService;
import com.aepm.logic.task.TaskService;
import com.aepm.model.Task;
import com.aepm.model.User;
import com.aepm.ui.navigation.NavigationManager;
import com.aepm.util.SessionManager;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import javafx.scene.control.cell.PropertyValueFactory;

import javafx.stage.Stage;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import java.util.List;

public class EmployeeTaskController {

    // =========================================================
    // TOP BAR
    // =========================================================

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label topNameLabel;


    // =========================================================
    // TABLE
    // =========================================================

    @FXML
    private TableView<Task> taskTable;

    @FXML
    private TableColumn<Task, String> titleColumn;

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
    private Label selectedStatusLabel;

    @FXML
    private Label selectedProgressLabel;


    // =========================================================
    // PROGRESS INPUT
    // =========================================================

    @FXML
    private TextField progressField;

    @FXML
    private TextArea progressNoteArea;


    // =========================================================
    // BUTTONS
    // =========================================================

    @FXML
    private Button acceptButton;

    @FXML
    private Button startButton;

    @FXML
    private Button updateProgressButton;

    @FXML
    private Button completeButton;


    // =========================================================
    // MESSAGE
    // =========================================================

    @FXML
    private Label messageLabel;


    // =========================================================
    // SERVICES
    // =========================================================

    private final TaskService taskService =
            new TaskService();

    private final TaskProgressService taskProgressService =
            new TaskProgressService();


    // =========================================================
    // SELECTED TASK
    // =========================================================

    private Task selectedTask;


    private static final ZoneId MALAYSIA_ZONE =
            ZoneId.of("Asia/Kuala_Lumpur");


    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {

        loadCurrentUser();

        setupTable();

        configureSelection();

        clearSelection();

        loadTasks();
    }


    // =========================================================
    // LOAD CURRENT USER
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
                        ? "Employee"
                        : user.getFullName();


        welcomeLabel.setText(
                "Welcome, " + name
        );


        topNameLabel.setText(
                name
        );
    }


    // =========================================================
    // SETUP TABLE
    // =========================================================

    private void setupTable() {

        titleColumn.setCellValueFactory(
                new PropertyValueFactory<>(
                        "taskTitle"
                )
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


        // =====================================================
        // PROGRESS DISPLAY
        // =====================================================

        progressColumn.setCellFactory(
                column ->
                        new TableCell<>() {

                            @Override
                            protected void updateItem(
                                    Double progress,
                                    boolean empty
                            ) {

                                super.updateItem(
                                        progress,
                                        empty
                                );


                                if (
                                        empty
                                                || progress == null
                                ) {

                                    setText("-");

                                } else {

                                    setText(
                                            String.format(
                                                    "%.0f%%",
                                                    progress
                                            )
                                    );
                                }
                            }
                        }
        );


        // =====================================================
        // DUE DATE DISPLAY
        // =====================================================

        dueDateColumn.setCellFactory(
                column ->
                        new TableCell<>() {

                            private final DateTimeFormatter formatter =
                                    DateTimeFormatter.ofPattern(
                                            "dd MMM yyyy HH:mm"
                                    );


                            @Override
                            protected void updateItem(
                                    OffsetDateTime date,
                                    boolean empty
                            ) {

                                super.updateItem(
                                        date,
                                        empty
                                );


                                if (
                                        empty
                                                || date == null
                                ) {

                                    setText("-");

                                    return;
                                }


                                setText(
                                        date
                                                .atZoneSameInstant(
                                                        MALAYSIA_ZONE
                                                )
                                                .format(
                                                        formatter
                                                )
                                );
                            }
                        }
        );


        taskTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );
    }


    // =========================================================
    // TABLE SELECTION
    // =========================================================

    private void configureSelection() {

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


        selectedStatusLabel.setText(
                task.getTaskStatus()
        );


        selectedProgressLabel.setText(
                String.format(
                        "%.0f%%",
                        task.getProgress()
                )
        );


        progressField.setText(
                String.format(
                        "%.0f",
                        task.getProgress()
                )
        );


        progressNoteArea.clear();


        updateButtonStates(
                task
        );
    }


    // =========================================================
    // BUTTON STATES
    // =========================================================

    private void updateButtonStates(
            Task task
    ) {

        String status =
                task.getTaskStatus();


        acceptButton.setDisable(
                !"Pending".equals(status)
        );


        startButton.setDisable(
                !"Accepted".equals(status)
        );


        updateProgressButton.setDisable(
                !"In Progress".equals(status)
        );


        completeButton.setDisable(
                !"In Progress".equals(status)
        );


        boolean canUpdateProgress =
                "In Progress".equals(status);


        progressField.setDisable(
                !canUpdateProgress
        );


        progressNoteArea.setDisable(
                !canUpdateProgress
        );
    }


    // =========================================================
    // CLEAR SELECTION
    // =========================================================

    private void clearSelection() {

        selectedTask =
                null;


        selectedTaskLabel.setText(
                "No task selected"
        );


        selectedStatusLabel.setText(
                "-"
        );


        selectedProgressLabel.setText(
                "-"
        );


        progressField.clear();

        progressNoteArea.clear();


        progressField.setDisable(
                true
        );


        progressNoteArea.setDisable(
                true
        );


        acceptButton.setDisable(
                true
        );


        startButton.setDisable(
                true
        );


        updateProgressButton.setDisable(
                true
        );


        completeButton.setDisable(
                true
        );
    }


    // =========================================================
    // LOAD MY TASKS
    // =========================================================

    private void loadTasks() {

        hideMessage();


        javafx.concurrent.Task<List<Task>> loadTask =
                new javafx.concurrent.Task<>() {

                    @Override
                    protected List<Task> call()
                            throws Exception {

                        return taskService
                                .getMyTasks();
                    }
                };


        loadTask.setOnSucceeded(
                event -> {

                    List<Task> tasks =
                            loadTask.getValue();


                    taskTable
                            .getItems()
                            .clear();


                    if (tasks != null) {

                        taskTable
                                .getItems()
                                .addAll(
                                        tasks
                                );
                    }


                    taskTable.refresh();

                    clearSelection();


                    if (
                            tasks == null
                                    || tasks.isEmpty()
                    ) {

                        showMessage(
                                "No tasks assigned to you."
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


                    if (error != null) {

                        error.printStackTrace();
                    }


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


        thread.setDaemon(
                true
        );


        thread.start();
    }


    // =========================================================
    // ACCEPT TASK
    // =========================================================

    @FXML
    private void handleAcceptTask() {

        if (!checkSelectedTask()) {
            return;
        }


        runTaskAction(
                () -> {

                    taskService.acceptTask(
                            selectedTask.getTaskId()
                    );
                },
                "Task accepted successfully."
        );
    }


    // =========================================================
    // START TASK
    // =========================================================

    @FXML
    private void handleStartTask() {

        if (!checkSelectedTask()) {
            return;
        }


        runTaskAction(
                () -> {

                    taskService.startTask(
                            selectedTask.getTaskId()
                    );
                },
                "Task started successfully."
        );
    }


    // =========================================================
    // UPDATE PROGRESS
    // =========================================================

    @FXML
    private void handleUpdateProgress() {

        if (!checkSelectedTask()) {
            return;
        }


        String progressText =
                progressField
                        .getText()
                        .trim();


        String note =
                progressNoteArea
                        .getText()
                        .trim();


        // =====================================================
        // VALIDATE PROGRESS
        // =====================================================

        if (progressText.isBlank()) {

            showMessage(
                    "Please enter progress percentage."
            );

            return;
        }


        double progress;


        try {

            progress =
                    Double.parseDouble(
                            progressText
                    );

        } catch (NumberFormatException e) {

            showMessage(
                    "Progress must be a number."
            );

            return;
        }


        if (
                progress < 0
                        || progress > 100
        ) {

            showMessage(
                    "Progress must be between 0 and 100."
            );

            return;
        }


        // =====================================================
        // DO NOT ALLOW PROGRESS TO GO BACKWARDS
        // =====================================================

        if (
                progress
                        < selectedTask.getProgress()
        ) {

            showMessage(
                    "New progress cannot be lower than current progress."
            );

            return;
        }


        // =====================================================
        // NOTE REQUIRED
        // =====================================================

        if (note.isBlank()) {

            showMessage(
                    "Please enter a progress note."
            );

            return;
        }


        double finalProgress =
                progress;


        runTaskAction(
                () -> {

                    // ==========================================
                    // 1. UPDATE MAIN TASK
                    // ==========================================

                    taskService.updateProgress(
                            selectedTask.getTaskId(),
                            finalProgress
                    );


                    // ==========================================
                    // 2. SAVE PROGRESS HISTORY
                    // ==========================================

                    taskProgressService.addProgress(
                            selectedTask.getTaskId(),
                            finalProgress,
                            note
                    );
                },
                "Task progress updated successfully."
        );
    }


    // =========================================================
    // COMPLETE TASK
    // =========================================================

    @FXML
    private void handleCompleteTask() {

        if (!checkSelectedTask()) {
            return;
        }


        runTaskAction(
                () -> {

                    // ==========================================
                    // COMPLETE MAIN TASK
                    // ==========================================

                    taskService.completeTask(
                            selectedTask.getTaskId()
                    );


                    // ==========================================
                    // SAVE FINAL 100% HISTORY
                    // ==========================================

                    taskProgressService.addProgress(
                            selectedTask.getTaskId(),
                            100,
                            "Task completed."
                    );
                },
                "Task completed successfully."
        );
    }


    // =========================================================
    // REFRESH
    // =========================================================

    @FXML
    private void handleRefresh() {

        loadTasks();
    }


    // =========================================================
    // CHECK SELECTED TASK
    // =========================================================

    private boolean checkSelectedTask() {

        if (selectedTask == null) {

            showMessage(
                    "Please select a task first."
            );

            return false;
        }


        return true;
    }


    // =========================================================
    // RUN BACKGROUND ACTION
    // =========================================================

    private void runTaskAction(
            TaskAction action,
            String successMessage
    ) {

        disableActionButtons(
                true
        );


        hideMessage();


        javafx.concurrent.Task<Void> actionTask =
                new javafx.concurrent.Task<>() {

                    @Override
                    protected Void call()
                            throws Exception {

                        action.run();

                        return null;
                    }
                };


        actionTask.setOnSucceeded(
                event -> {

                    /*
                     * Reload latest task data after success.
                     */

                    loadTasks();
                }
        );


        actionTask.setOnFailed(
                event -> {

                    disableActionButtons(
                            false
                    );


                    Throwable error =
                            actionTask.getException();


                    if (error != null) {

                        error.printStackTrace();
                    }


                    showMessage(
                            error == null
                                    ? "Unable to update task."
                                    : error.getMessage()
                    );
                }
        );


        Thread thread =
                new Thread(
                        actionTask
                );


        thread.setDaemon(
                true
        );


        thread.start();
    }


    // =========================================================
    // DISABLE BUTTONS
    // =========================================================

    private void disableActionButtons(
            boolean disabled
    ) {

        acceptButton.setDisable(
                disabled
        );


        startButton.setDisable(
                disabled
        );


        updateProgressButton.setDisable(
                disabled
        );


        completeButton.setDisable(
                disabled
        );


        progressField.setDisable(
                disabled
        );


        progressNoteArea.setDisable(
                disabled
        );
    }


    // =========================================================
    // TASK ACTION
    // =========================================================

    @FunctionalInterface
    private interface TaskAction {

        void run()
                throws Exception;
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
                "/fxml/employee/employeeHome.fxml"
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
                "/fxml/employee/employeeProfile.fxml"
        );
    }


    // =========================================================
    // MY TASKS
    // =========================================================

    @FXML
    private void handleTasks() {

        // Already on My Tasks page.
    }


    // =========================================================
    // KPI
    // =========================================================

    @FXML
    private void handleKpi() {

        System.out.println(
                "Employee KPI not implemented yet."
        );
    }


    // =========================================================
    // REPORTS
    // =========================================================

    @FXML
    private void handleReports() {

        System.out.println(
                "Employee Reports not implemented yet."
        );
    }


    // =========================================================
    // ANONYMOUS REPORT
    // =========================================================

    @FXML
    private void handleAnonymousReport() {

        System.out.println(
                "Anonymous Report not implemented yet."
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
    // MESSAGE
    // =========================================================

    private void showMessage(
            String message
    ) {

        messageLabel.setText(
                message
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