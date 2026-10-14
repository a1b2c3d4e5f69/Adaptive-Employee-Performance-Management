package com.aepm.ui.employee;

import com.aepm.logic.task.TaskProgressService;
import com.aepm.logic.task.TaskService;

import com.aepm.model.Task;

import javafx.fxml.FXML;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import javafx.scene.layout.VBox;

import javafx.stage.Stage;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import com.aepm.ui.navigation.NavigationManager;

public class EmployeeTaskDetailController {

    // =========================================================
    // HEADER
    // =========================================================

    @FXML
    private Label difficultyBadgeLabel;

    @FXML
    private Label statusBadgeLabel;


    // =========================================================
    // TASK
    // =========================================================

    @FXML
    private Label taskTitleLabel;

    @FXML
    private Label descriptionLabel;

    @FXML
    private Label categoryLabel;

    @FXML
    private Label priorityLabel;

    @FXML
    private Label assignedDateLabel;

    @FXML
    private Label dueDateLabel;

    @FXML
    private Label difficultyLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private Label criteriaLabel;


    // =========================================================
    // PROGRESS
    // =========================================================

    @FXML
    private Label progressValueLabel;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private VBox progressUpdateSection;

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

    @FXML
        private void handleAttendance(ActionEvent event) {

        Stage stage = (Stage) ((Node) event.getSource())
                .getScene()
                .getWindow();

        NavigationManager.navigate(
                stage,
                "/fxml/employee/employeeAttendance.fxml"
        );
    }
    // =========================================================
    // SERVICE
    // =========================================================

    private final TaskService taskService =
            new TaskService();

    private final TaskProgressService taskProgressService =
            new TaskProgressService();


    // =========================================================
    // DATA
    // =========================================================

    private Task task;

    private Stage stage;

    private Runnable refreshCallback;


    private static final ZoneId MALAYSIA_ZONE =
            ZoneId.of(
                    "Asia/Kuala_Lumpur"
            );


    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern(
                    "dd MMM yyyy HH:mm"
            );


    // =========================================================
    // SET TASK
    // =========================================================

    public void setTask(
            Task task,
            String categoryName,
            Stage stage,
            Runnable refreshCallback
    ) {

        this.task =
                task;

        this.stage =
                stage;

        this.refreshCallback =
                refreshCallback;


        displayTask(
                categoryName
        );
    }


    // =========================================================
    // DISPLAY
    // =========================================================

    private void displayTask(
            String categoryName
    ) {

        if (task == null) {
            return;
        }


        taskTitleLabel.setText(
                safe(
                        task.getTaskTitle()
                )
        );


        descriptionLabel.setText(
                emptyToDash(
                        task.getTaskDescription()
                )
        );


        categoryLabel.setText(
                emptyToDash(
                        categoryName
                )
        );


        priorityLabel.setText(
                safe(
                        task.getPriority()
                )
        );


        difficultyLabel.setText(
                safe(
                        task.getDifficultyLevel()
                )
        );


        difficultyBadgeLabel.setText(
                safe(
                        task.getDifficultyLevel()
                )
        );


        statusLabel.setText(
                safe(
                        task.getTaskStatus()
                )
        );


        statusBadgeLabel.setText(
                safe(
                        task.getTaskStatus()
                )
        );


        assignedDateLabel.setText(
                task.getAssignedDate() == null
                        ? "-"
                        : formatDateTime(
                                task.getAssignedDate()
                        )
        );


        dueDateLabel.setText(
                task.getDueDate() == null
                        ? "-"
                        : formatDateTime(
                                task.getDueDate()
                        )
        );


        criteriaLabel.setText(
                emptyToDash(
                        task.getCompletionCriteria()
                )
        );


        double progress =
                task.getProgress();


        progressValueLabel.setText(
                String.format(
                        "%.0f%%",
                        progress
                )
        );


        progressBar.setProgress(
                toProgress(
                        progress
                )
        );


        progressField.setText(
                String.format(
                        "%.0f",
                        progress
                )
        );


        progressNoteArea.clear();


        updateButtonState();


        hideMessage();
    }


    // =========================================================
    // BUTTON STATE
    // =========================================================

    private void updateButtonState() {

        String status =
                task == null
                        ? ""
                        : task.getTaskStatus();


        boolean pending =
                "Pending".equalsIgnoreCase(
                        status
                );


        boolean accepted =
                "Accepted".equalsIgnoreCase(
                        status
                );


        boolean inProgress =
                "In Progress".equalsIgnoreCase(
                        status
                );


        acceptButton.setDisable(
                !pending
        );


        startButton.setDisable(
                !accepted
        );


        updateProgressButton.setDisable(
                !inProgress
        );


        completeButton.setDisable(
                !inProgress
        );


        progressField.setDisable(
                !inProgress
        );


        progressNoteArea.setDisable(
                !inProgress
        );


        progressUpdateSection.setDisable(
                !inProgress
        );
    }


    // =========================================================
    // ACCEPT
    // =========================================================

    @FXML
    private void handleAcceptTask() {

        if (task == null) {
            return;
        }


        runAction(
                () ->
                        taskService.acceptTask(
                                task.getTaskId()
                        )
        );
    }


    // =========================================================
    // START
    // =========================================================

    @FXML
    private void handleStartTask() {

        if (task == null) {
            return;
        }


        runAction(
                () ->
                        taskService.startTask(
                                task.getTaskId()
                        )
        );
    }


    // =========================================================
    // UPDATE PROGRESS
    // =========================================================

    @FXML
    private void handleUpdateProgress() {

        if (task == null) {
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


        if (
                progress
                        < task.getProgress()
        ) {

            showMessage(
                    "New progress cannot be lower than current progress."
            );

            return;
        }


        if (note.isBlank()) {

            showMessage(
                    "Please enter a progress note."
            );

            return;
        }


        double finalProgress =
                progress;


        runAction(
                () -> {

                    taskService.updateProgress(
                            task.getTaskId(),
                            finalProgress
                    );


                    taskProgressService.addProgress(
                            task.getTaskId(),
                            finalProgress,
                            note
                    );
                }
        );
    }


    // =========================================================
    // COMPLETE
    // =========================================================

    @FXML
    private void handleCompleteTask() {

        if (task == null) {
            return;
        }


        runAction(
                () -> {

                    taskService.completeTask(
                            task.getTaskId()
                    );


                    taskProgressService.addProgress(
                            task.getTaskId(),
                            100,
                            "Task completed."
                    );
                }
        );
    }


    // =========================================================
    // RUN ACTION
    // =========================================================

    private void runAction(
            TaskAction action
    ) {

        disableButtons(
                true
        );


        hideMessage();


        javafx.concurrent.Task<Void> backgroundTask =
                new javafx.concurrent.Task<>() {

                    @Override
                    protected Void call()
                            throws Exception {

                        action.run();

                        return null;
                    }
                };


        backgroundTask.setOnSucceeded(
                event -> {

                    if (refreshCallback != null) {

                        refreshCallback.run();
                    }


                    if (stage != null) {

                        stage.close();
                    }
                }
        );


        backgroundTask.setOnFailed(
                event -> {

                    disableButtons(
                            false
                    );


                    updateButtonState();


                    Throwable error =
                            backgroundTask
                                    .getException();


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
                        backgroundTask
                );


        thread.setDaemon(
                true
        );


        thread.start();
    }


    // =========================================================
    // DISABLE
    // =========================================================

    private void disableButtons(
            boolean disable
    ) {

        acceptButton.setDisable(
                disable
        );

        startButton.setDisable(
                disable
        );

        updateProgressButton.setDisable(
                disable
        );

        completeButton.setDisable(
                disable
        );

        progressField.setDisable(
                disable
        );

        progressNoteArea.setDisable(
                disable
        );
    }


    // =========================================================
    // CLOSE
    // =========================================================

    @FXML
    private void handleClose() {

        if (stage != null) {

            stage.close();
        }
    }


    // =========================================================
    // HELPERS
    // =========================================================

    private double toProgress(
            double value
    ) {

        double result =
                value / 100.0;


        if (result < 0) {
            return 0;
        }


        if (result > 1) {
            return 1;
        }


        return result;
    }


    private String formatDateTime(
            OffsetDateTime value
    ) {

        return value
                .atZoneSameInstant(
                        MALAYSIA_ZONE
                )
                .format(
                        DATE_FORMATTER
                );
    }


    private String safe(
            String value
    ) {

        return value == null
                || value.isBlank()
                ? "-"
                : value;
    }


    private String emptyToDash(
            String value
    ) {

        return value == null
                || value.isBlank()
                ? "-"
                : value;
    }


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


    @FunctionalInterface
    private interface TaskAction {

        void run()
                throws Exception;
    }
}