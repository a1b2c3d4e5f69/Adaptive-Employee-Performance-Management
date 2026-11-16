package com.aepm.ui.employee;

import com.aepm.data.task.TaskAttachmentRepository;

import com.aepm.logic.task.TaskProgressService;
import com.aepm.logic.task.TaskService;

import com.aepm.model.Task;
import com.aepm.model.TaskAttachment;

import com.aepm.ui.navigation.NavigationManager;

import com.aepm.util.SessionManager;

import javafx.fxml.FXML;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import javafx.scene.layout.VBox;

import javafx.stage.Stage;

import javafx.event.ActionEvent;
import javafx.scene.Node;

import java.awt.Desktop;

import java.nio.file.Files;
import java.nio.file.Path;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import java.util.List;
import com.aepm.logic.task.EmployeeProgressSubmissionService;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import java.io.File;


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
    // ATTACHMENT
    // =========================================================

    @FXML
    private VBox attachmentSection;

    @FXML
    private Label attachmentNameLabel;

    @FXML
    private Label attachmentSizeLabel;

    


    private TaskAttachment currentAttachment;


    private final TaskAttachmentRepository
            attachmentRepository =
            new TaskAttachmentRepository();


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

    @FXML
    private Button uploadEvidenceButton;

    @FXML
    private Label evidenceFileNameLabel;

    @FXML
    private HBox evidencePreviewBox;

    @FXML
    private Label evidenceNameLabel;

    @FXML
    private Label evidenceSizeLabel;


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
    // SERVICE
    // =========================================================

    private final TaskService taskService =
            new TaskService();

    private final TaskProgressService taskProgressService =
            new TaskProgressService();

    private final EmployeeProgressSubmissionService
        progressSubmissionService =
        new EmployeeProgressSubmissionService();


    // =========================================================
    // DATA
    // =========================================================

    private Task task;

    private Stage stage;

    private Runnable refreshCallback;

    private File selectedEvidenceFile;


    private static final ZoneId MALAYSIA_ZONE =
            ZoneId.of(
                    "Asia/Kuala_Lumpur"
            );


    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern(
                    "dd MMM yyyy HH:mm"
            );


    // =========================================================
    // ATTENDANCE NAVIGATION
    // =========================================================

    @FXML
    private void handleAttendance(
            ActionEvent event
    ) {

        Stage currentStage =
                (Stage) (
                        (Node) event.getSource()
                )
                        .getScene()
                        .getWindow();


        NavigationManager.navigate(
                currentStage,
                "/fxml/employee/employeeAttendance.fxml"
        );
    }


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


        // =====================================================
        // NEW: LOAD OPTIONAL TASK ATTACHMENT
        // =====================================================

        loadTaskAttachment();
    }


    // =========================================================
    // DISPLAY TASK
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

        selectedEvidenceFile =
        null;


        if (evidenceFileNameLabel != null) {

        evidenceFileNameLabel.setText(
                "No file selected"
        );
        }


        if (evidencePreviewBox != null) {

        evidencePreviewBox.setVisible(
                false
        );

        evidencePreviewBox.setManaged(
                false
        );
        }

        updateButtonState();


        hideMessage();
    }


    // =========================================================
    // LOAD TASK ATTACHMENT
    // =========================================================

    private void loadTaskAttachment() {

        hideAttachment();


        if (
                task == null
                        || task.getTaskId() == null
        ) {

            return;
        }


        String accessToken =
                SessionManager.getAccessToken();


        if (
                accessToken == null
                        || accessToken.isBlank()
        ) {

            return;
        }


        javafx.concurrent.Task<List<TaskAttachment>>
                backgroundTask =
                new javafx.concurrent.Task<>() {

                    @Override
                    protected List<TaskAttachment> call()
                            throws Exception {

                        return attachmentRepository
                                .getByTask(
                                        task.getTaskId(),
                                        accessToken
                                );
                    }
                };


        backgroundTask.setOnSucceeded(
                event -> {

                    List<TaskAttachment> attachments =
                            backgroundTask.getValue();


                    if (
                            attachments == null
                                    || attachments.isEmpty()
                    ) {

                        hideAttachment();

                        return;
                    }


                    // Currently Create Task accepts one optional
                    // attachment, so display the first record.
                    currentAttachment =
                            attachments.get(
                                    0
                            );


                    attachmentNameLabel.setText(
                            safe(
                                    currentAttachment
                                            .getFileName()
                            )
                    );


                    attachmentSizeLabel.setText(
                            formatFileSize(
                                    currentAttachment
                                            .getFileSize()
                            )
                    );


                    attachmentSection.setVisible(
                            true
                    );


                    attachmentSection.setManaged(
                            true
                    );
                }
        );


        backgroundTask.setOnFailed(
                event -> {

                    Throwable error =
                            backgroundTask
                                    .getException();


                    if (error != null) {

                        System.err.println(
                                "[Task Attachment] Unable to load attachment: "
                                        + error.getMessage()
                        );

                        error.printStackTrace();
                    }


                    hideAttachment();
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
    // OPEN ATTACHMENT
    // =========================================================

    @FXML
    private void handleOpenAttachment() {

        if (currentAttachment == null) {

            showMessage(
                    "No attachment is available."
            );

            return;
        }


        String accessToken =
                SessionManager.getAccessToken();


        if (
                accessToken == null
                        || accessToken.isBlank()
        ) {

            showMessage(
                    "Session expired. Please login again."
            );

            return;
        }


        if (
                !Desktop.isDesktopSupported()
        ) {

            showMessage(
                    "Opening files is not supported on this device."
            );

            return;
        }


        hideMessage();


        javafx.concurrent.Task<Path>
                backgroundTask =
                new javafx.concurrent.Task<>() {

                    @Override
                    protected Path call()
                            throws Exception {


                        byte[] data =
                                attachmentRepository
                                        .download(
                                                currentAttachment,
                                                accessToken
                                        );


                        if (
                                data == null
                                        || data.length == 0
                        ) {

                            throw new IllegalStateException(
                                    "The attachment is empty."
                            );
                        }


                        String originalName =
                                currentAttachment
                                        .getFileName();


                        String safeName =
                                originalName == null
                                        || originalName.isBlank()
                                        ? "attachment"
                                        : originalName
                                        .replaceAll(
                                                "[^a-zA-Z0-9._-]",
                                                "_"
                                        );


                        String extension =
                                getFileExtension(
                                        safeName
                                );


                        Path tempFile;


                        if (extension.isBlank()) {

                            tempFile =
                                    Files.createTempFile(
                                            "aepm-task-attachment-",
                                            ".tmp"
                                    );

                        } else {

                            tempFile =
                                    Files.createTempFile(
                                            "aepm-task-attachment-",
                                            extension
                                    );
                        }


                        Files.write(
                                tempFile,
                                data
                        );


                        tempFile
                                .toFile()
                                .deleteOnExit();


                        return tempFile;
                    }
                };


        backgroundTask.setOnSucceeded(
                event -> {

                    try {

                        Path downloadedFile =
                                backgroundTask
                                        .getValue();


                        Desktop
                                .getDesktop()
                                .open(
                                        downloadedFile
                                                .toFile()
                                );

                    } catch (Exception error) {

                        error.printStackTrace();


                        showMessage(
                                "Unable to open the attachment."
                        );
                    }
                }
        );


        backgroundTask.setOnFailed(
                event -> {

                    Throwable error =
                            backgroundTask
                                    .getException();


                    if (error != null) {

                        error.printStackTrace();
                    }


                    showMessage(
                            error == null
                                    ? "Unable to download the attachment."
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
    // HIDE ATTACHMENT
    // =========================================================

    private void hideAttachment() {

        currentAttachment =
                null;


        if (attachmentNameLabel != null) {

            attachmentNameLabel.setText(
                    ""
            );
        }


        if (attachmentSizeLabel != null) {

            attachmentSizeLabel.setText(
                    ""
            );
        }


        if (attachmentSection != null) {

            attachmentSection.setVisible(
                    false
            );


            attachmentSection.setManaged(
                    false
            );
        }
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
                "Pending"
                        .equalsIgnoreCase(
                                status
                        );


        boolean accepted =
                "Accepted"
                        .equalsIgnoreCase(
                                status
                        );


        boolean inProgress =
                "In Progress"
                        .equalsIgnoreCase(
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

        uploadEvidenceButton.setDisable(
                !inProgress
        );


        progressUpdateSection.setDisable(
                !inProgress
        );
    }


    // =========================================================
    // ACCEPT TASK
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
    // START TASK
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
        // CHOOSE PROGRESS EVIDENCE
        // =========================================================

        @FXML
        private void handleChooseEvidence() {

                FileChooser chooser =
                        new FileChooser();


                chooser.setTitle(
                        "Select Progress Evidence"
                );


                chooser.getExtensionFilters()
                        .addAll(

                                new FileChooser.ExtensionFilter(
                                        "Supported Evidence",
                                        "*.pdf",
                                        "*.png",
                                        "*.jpg",
                                        "*.jpeg",
                                        "*.doc",
                                        "*.docx",
                                        "*.xls",
                                        "*.xlsx",
                                        "*.ppt",
                                        "*.pptx",
                                        "*.txt",
                                        "*.csv",
                                        "*.zip"
                                ),

                                new FileChooser.ExtensionFilter(
                                        "All Files",
                                        "*.*"
                                )
                        );


                File file =
                        chooser.showOpenDialog(
                                progressUpdateSection
                                        .getScene()
                                        .getWindow()
                        );


                if (file == null) {

                        return;
                }


                long maxSize =
                        10L
                                * 1024L
                                * 1024L;


                if (file.length() <= 0) {

                        showMessage(
                                "The selected evidence file is empty."
                        );

                        return;
                }


                if (file.length() > maxSize) {

                        showMessage(
                                "Evidence file cannot exceed 10 MB."
                        );

                        return;
                }


                selectedEvidenceFile =
                        file;


                evidenceFileNameLabel.setText(
                        file.getName()
                );


                evidenceNameLabel.setText(
                        file.getName()
                );


                evidenceSizeLabel.setText(
                        formatEvidenceSize(
                                file.length()
                        )
                );


                evidencePreviewBox.setVisible(
                        true
                );


                evidencePreviewBox.setManaged(
                        true
                );


                hideMessage();
     }

     // =========================================================
        // REMOVE PROGRESS EVIDENCE
        // =========================================================

        @FXML
        private void handleRemoveEvidence() {

                selectedEvidenceFile =
                        null;


                evidenceFileNameLabel.setText(
                        "No file selected"
                );


                evidenceNameLabel.setText(
                        ""
                );


                evidenceSizeLabel.setText(
                        ""
                );


                evidencePreviewBox.setVisible(
                        false
                );


                evidencePreviewBox.setManaged(
                        false
                );
        }

        // =========================================================
        // FORMAT EVIDENCE SIZE
        // =========================================================

        private String formatEvidenceSize(
                long bytes
        ) {

        if (bytes < 1024) {

                return bytes
                        + " B";
        }


        if (
                bytes
                        < 1024L
                        * 1024L
        ) {

                return String.format(
                        "%.1f KB",
                        bytes / 1024.0
                );
        }


        return String.format(
                "%.1f MB",
                bytes
                        / (
                        1024.0
                                * 1024.0
                )
        );
        }


    // =========================================================
    // UPDATE PROGRESS
    // =========================================================

    // =========================================================
        // SUBMIT PROGRESS FOR VERIFICATION
        // =========================================================

        @FXML
        private void handleUpdateProgress() {

        if (task == null) {

                return;
        }


        // =====================================================
        // TASK STATUS
        // =====================================================

        if (
                !"In Progress".equalsIgnoreCase(
                        task.getTaskStatus()
                )
        ) {

                showMessage(
                        "Only an In Progress task can submit progress."
                );

                return;
        }


        // =====================================================
        // PROGRESS
        // =====================================================

        String progressText =
                progressField
                        .getText()
                        .trim();


        if (progressText.isBlank()) {

                showMessage(
                        "Please enter requested progress percentage."
                );

                return;
        }


        int proposedProgress;


        try {

                double input =
                        Double.parseDouble(
                                progressText
                        );


                if (
                        input
                                != Math.floor(
                                input
                        )
                ) {

                showMessage(
                        "Please enter a whole number for progress."
                );

                return;
                }


                proposedProgress =
                        (int) input;


        } catch (NumberFormatException e) {

                showMessage(
                        "Progress must be a valid number."
                );

                return;
        }


        if (
                proposedProgress < 0
                        || proposedProgress > 100
        ) {

                showMessage(
                        "Progress must be between 0 and 100."
                );

                return;
        }


        int currentProgress =
                (int) Math.round(
                        task.getProgress()
                );


        if (
                proposedProgress
                        <= currentProgress
        ) {

                showMessage(
                        "Requested progress must be higher than the current verified progress of "
                                + currentProgress
                                + "%."
                );

                return;
        }


        // =====================================================
        // NOTE
        // =====================================================

        String note =
                progressNoteArea
                        .getText()
                        .trim();


        if (note.isBlank()) {

                showMessage(
                        "Please describe the work you completed."
                );

                return;
        }


        if (note.length() < 20) {

                showMessage(
                        "Please provide a more detailed progress note (at least 20 characters)."
                );

                return;
        }


        // =====================================================
        // EVIDENCE
        // =====================================================

        if (selectedEvidenceFile == null) {

                showMessage(
                        "Please upload supporting evidence before submitting progress."
                );

                return;
        }


        // =====================================================
        // DISABLE FORM
        // =====================================================

        updateProgressButton.setDisable(
                true
        );

        uploadEvidenceButton.setDisable(
                true
        );

        progressField.setDisable(
                true
        );

        progressNoteArea.setDisable(
                true
        );


        hideMessage();


        // =====================================================
        // BACKGROUND SUBMISSION
        // =====================================================

        javafx.concurrent.Task<
                EmployeeProgressSubmissionService.SubmissionResult
                > submitTask =

                new javafx.concurrent.Task<>() {

                        @Override
                        protected EmployeeProgressSubmissionService
                                .SubmissionResult call()
                                throws Exception {

                        return progressSubmissionService
                                .submitProgress(

                                        task.getTaskId(),

                                        proposedProgress,

                                        note,

                                        selectedEvidenceFile
                                );
                        }
                };


        // =====================================================
        // SUCCESS
        // =====================================================

        submitTask.setOnSucceeded(
                event -> {

                        EmployeeProgressSubmissionService
                                .SubmissionResult result =
                                submitTask.getValue();


                        progressField.setText(
                                String.valueOf(
                                        result.getCurrentProgress()
                                )
                        );


                        progressNoteArea.clear();


                        handleRemoveEvidence();


                        String deadline =
                                result
                                        .getVerificationDeadline()
                                        .atZoneSameInstant(
                                                MALAYSIA_ZONE
                                        )
                                        .format(
                                                DateTimeFormatter
                                                        .ofPattern(
                                                                "dd MMM yyyy, hh:mm a"
                                                        )
                                        );


                        showMessage(
                                "Progress submitted successfully.\n"
                                        + "Requested Progress: "
                                        + result.getRequestedProgress()
                                        + "%\n"
                                        + "Actual Verified Progress: "
                                        + result.getCurrentProgress()
                                        + "%\n"
                                        + "Status: "
                                        + result.getStatus()
                                        + "\n"
                                        + "Manager Review Deadline: "
                                        + deadline
                        );


                        /*
                        * IMPORTANT:
                        *
                        * Do NOT update progressBar here.
                        *
                        * Actual progress has NOT been approved yet.
                        */


                        updateProgressButton.setDisable(
                                false
                        );


                        uploadEvidenceButton.setDisable(
                                false
                        );


                        progressField.setDisable(
                                false
                        );


                        progressNoteArea.setDisable(
                                false
                        );


                        if (refreshCallback != null) {

                        refreshCallback.run();
                        }
                }
        );


        // =====================================================
        // FAILURE
        // =====================================================

        submitTask.setOnFailed(
                event -> {

                        Throwable error =
                                submitTask.getException();


                        if (error != null) {

                        error.printStackTrace();
                        }


                        showMessage(
                                error == null
                                        ? "Unable to submit progress for verification."
                                        : error.getMessage()
                        );


                        updateProgressButton.setDisable(
                                false
                        );


                        uploadEvidenceButton.setDisable(
                                false
                        );


                        progressField.setDisable(
                                false
                        );


                        progressNoteArea.setDisable(
                                false
                        );


                        updateButtonState();
                }
        );


        Thread thread =
                new Thread(
                        submitTask
                );


        thread.setDaemon(
                true
        );


        thread.start();
    }

        @FXML
        private void handleCompleteTask() {

        if (task == null) {

                return;
        }


        if (
                !"In Progress".equalsIgnoreCase(
                        task.getTaskStatus()
                )
        ) {

                showMessage(
                        "Only an In Progress task can request completion."
                );

                return;
        }

        progressField.setText(
                "100"
        );


        showMessage(
                "To complete this task, enter your completion note, "
                        + "upload supporting evidence, then click "
                        + "\"Submit for Verification\"."
        );


        progressNoteArea.requestFocus();
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


        javafx.concurrent.Task<Void>
                backgroundTask =
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

                    if (
                            refreshCallback != null
                    ) {

                        refreshCallback.run();
                    }


                    if (
                            stage != null
                    ) {

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

        if (
                stage != null
        ) {

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


    // =========================================================
    // FILE SIZE
    // =========================================================

    private String formatFileSize(
            long bytes
    ) {

        if (bytes <= 0) {
            return "";
        }


        if (bytes < 1024) {

            return bytes
                    + " B";
        }


        if (
                bytes
                        < 1024L
                        * 1024L
        ) {

            return String.format(
                    "%.1f KB",
                    bytes / 1024.0
            );
        }


        return String.format(
                "%.1f MB",
                bytes
                        / (
                        1024.0
                                * 1024.0
                )
        );
    }


    // =========================================================
    // GET EXTENSION
    // =========================================================

    private String getFileExtension(
            String filename
    ) {

        if (
                filename == null
                        || filename.isBlank()
        ) {

            return "";
        }


        int index =
                filename.lastIndexOf(
                        '.'
                );


        if (
                index < 0
                        || index
                        == filename.length() - 1
        ) {

            return "";
        }


        return filename.substring(
                index
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


    // =========================================================
    // ACTION
    // =========================================================

    @FunctionalInterface
    private interface TaskAction {

        void run()
                throws Exception;
    }
}