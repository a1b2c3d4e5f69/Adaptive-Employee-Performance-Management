package com.aepm.ui.employee;

import com.aepm.data.task.TaskAttachmentRepository;

import com.aepm.logic.task.TaskService;

import com.aepm.model.Task;
import com.aepm.model.TaskAttachment;

import com.aepm.ui.navigation.NavigationManager;

import com.aepm.util.SessionManager;

import javafx.fxml.FXML;

import javafx.scene.control.Alert;
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

    private EmployeeProgressSubmissionService.RevisionSubmission
            revisionSubmission;

    private VBox revisionNoticeBox;

    private Label revisionInstructionLabel;


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

        // Check whether this task has a manager-requested revision.
        loadRevisionRequiredSubmission();
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
    // LOAD REVISION REQUIRED SUBMISSION
    // =========================================================

    private void loadRevisionRequiredSubmission() {

        revisionSubmission =
                null;

        if (
                task == null
                        || task.getTaskId() == null
        ) {
            return;
        }

        javafx.concurrent.Task<
                EmployeeProgressSubmissionService.RevisionSubmission
                > backgroundTask =
                new javafx.concurrent.Task<>() {

                    @Override
                    protected EmployeeProgressSubmissionService
                            .RevisionSubmission call()
                            throws Exception {

                        return progressSubmissionService
                                .getRevisionRequiredSubmission(
                                        task.getTaskId()
                                );
                    }
                };

        backgroundTask.setOnSucceeded(
                event -> {

                    revisionSubmission =
                            backgroundTask.getValue();

                    if (revisionSubmission == null) {

                        hideRevisionNotice();

                        updateProgressButton.setText(
                                "Submit for Verification"
                        );

                        return;
                    }

                    progressField.setText(
                            String.valueOf(
                                    revisionSubmission
                                            .getProposedProgress()
                            )
                    );

                    progressNoteArea.setText(
                            revisionSubmission
                                    .getProgressNote()
                    );

                    updateProgressButton.setText(
                            "Resubmit for Verification"
                    );

                    String instruction =
                            revisionSubmission
                                    .getManagerInstruction();

                    showRevisionNotice(
                            instruction
                    );

                    updateButtonState();
                }
        );

        backgroundTask.setOnFailed(
                event -> {

                    Throwable error =
                            backgroundTask
                                    .getException();

                    if (error != null) {

                        System.err.println(
                                "[Progress Revision] Unable to load revision: "
                                        + error.getMessage()
                        );
                    }
                }
        );

        Thread thread =
                new Thread(
                        backgroundTask,
                        "progress-revision-load"
                );

        thread.setDaemon(
                true
        );

        thread.start();
    }



    // =========================================================
    // REVISION REQUIRED NOTICE
    // =========================================================

    private void showRevisionNotice(
            String managerInstruction
    ) {

        if (progressUpdateSection == null) {
            return;
        }

        if (revisionNoticeBox == null) {

            Label title =
                    new Label(
                            "Revision Required"
                    );

            title.setStyle(
                    "-fx-font-size: 14px;"
                            + "-fx-font-weight: 800;"
                            + "-fx-text-fill: #9A3412;"
            );

            revisionInstructionLabel =
                    new Label();

            revisionInstructionLabel.setWrapText(
                    true
            );

            revisionInstructionLabel.setMaxWidth(
                    Double.MAX_VALUE
            );

            revisionInstructionLabel.setStyle(
                    "-fx-font-size: 12px;"
                            + "-fx-text-fill: #7C2D12;"
            );

            revisionNoticeBox =
                    new VBox(
                            6,
                            title,
                            revisionInstructionLabel
                    );

            revisionNoticeBox.setPadding(
                    new javafx.geometry.Insets(
                            14
                    )
            );

            revisionNoticeBox.setMaxWidth(
                    Double.MAX_VALUE
            );

            revisionNoticeBox.setStyle(
                    "-fx-background-color: #FFF7ED;"
                            + "-fx-background-radius: 10;"
                            + "-fx-border-color: #FDBA74;"
                            + "-fx-border-radius: 10;"
                            + "-fx-border-width: 1;"
            );
        }

        String instruction =
                managerInstruction == null
                        || managerInstruction.isBlank()
                        ? "Your manager requested changes to this progress submission. "
                            + "Please update the progress note or upload clearer supporting evidence."
                        : "Manager Instruction: "
                            + managerInstruction.trim();

        revisionInstructionLabel.setText(
                instruction
        );

        if (
                !progressUpdateSection
                        .getChildren()
                        .contains(
                                revisionNoticeBox
                        )
        ) {

            int insertIndex =
                    Math.min(
                            1,
                            progressUpdateSection
                                    .getChildren()
                                    .size()
                    );

            progressUpdateSection
                    .getChildren()
                    .add(
                            insertIndex,
                            revisionNoticeBox
                    );
        }

        revisionNoticeBox.setVisible(
                true
        );

        revisionNoticeBox.setManaged(
                true
        );
    }


    private void hideRevisionNotice() {

        if (revisionNoticeBox != null) {

            revisionNoticeBox.setVisible(
                    false
            );

            revisionNoticeBox.setManaged(
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

        updateProgressButton.setText(
                revisionSubmission == null
                        ? "Submitting..."
                        : "Resubmitting..."
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


        showMessage(
                "Submitting progress and evidence for verification..."
        );


        // =====================================================
        // EVIDENCE SNAPSHOT
        // =====================================================

        final File evidenceSnapshot =
                selectedEvidenceFile;

        final boolean revisionMode =
                revisionSubmission != null;


        // =====================================================
        // BACKGROUND SUBMISSION
        // =====================================================

        javafx.concurrent.Task<
                EmployeeProgressSubmissionService.SubmissionResult
                > submitTask =

                new javafx.concurrent.Task<EmployeeProgressSubmissionService.SubmissionResult>() {

                        @Override
                        protected EmployeeProgressSubmissionService
                                .SubmissionResult call()
                                throws Exception {

                        if (revisionSubmission != null) {

                                return progressSubmissionService
                                        .resubmitRevision(

                                                revisionSubmission
                                                        .getSubmissionId(),

                                                task.getTaskId(),

                                                proposedProgress,

                                                note,

                                                evidenceSnapshot
                                        );
                        }

                        return progressSubmissionService
                                .submitProgress(

                                        task.getTaskId(),

                                        proposedProgress,

                                        note,

                                        evidenceSnapshot
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


                        updateProgressButton.setText(
                                "Submit for Verification"
                        );

                        revisionSubmission =
                                null;

                        hideRevisionNotice();


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
                                                DateTimeFormatter.ofPattern(
                                                        "dd MMM yyyy, hh:mm a"
                                                )
                                        );


                        showMessage(
                                "Progress submitted successfully. "
                                        + "Requested: "
                                        + result.getRequestedProgress()
                                        + "% | Actual: "
                                        + result.getCurrentProgress()
                                        + "% | Status: "
                                        + result.getStatus()
                        );


                        Alert successAlert =
                                new Alert(
                                        Alert.AlertType.INFORMATION
                                );


                        successAlert.setTitle(
                                revisionMode
                                        ? "Revision Resubmitted"
                                        : "Progress Submitted"
                        );


                        successAlert.setHeaderText(
                                revisionMode
                                        ? "Revised progress resubmitted for verification"
                                        : "Progress submitted for verification"
                        );


                        successAlert.setContentText(
                                "Requested Progress: "
                                        + result.getRequestedProgress()
                                        + "%\n"
                                        + "Actual Verified Progress: "
                                        + result.getCurrentProgress()
                                        + "%\n\n"
                                        + "Status: "
                                        + result.getStatus()
                                        + "\n"
                                        + "Manager Review Deadline: "
                                        + deadline
                        );


                        if (stage != null) {

                                successAlert.initOwner(
                                        stage
                                );
                        }


                        successAlert.showAndWait();


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


                updateProgressButton.setText(
                        revisionSubmission == null
                                ? "Submit for Verification"
                                : "Resubmit for Verification"
                );


                if (error != null) {

                        error.printStackTrace();
                }


                String errorMessage =
                        error == null
                                ? "Unable to submit progress for verification."
                                : (
                                error.getMessage() == null
                                        || error.getMessage().isBlank()
                                        ? error.toString()
                                        : error.getMessage()
                        );


                // =================================================
                // ALREADY PENDING
                // =================================================

                if (
                        errorMessage.contains(
                                "already have a progress submission waiting for verification"
                        )
                ) {

                        String pendingMessage =
                                "Your progress submission is currently pending verification.\n\n"
                                        + "Please wait for your manager to review the submission. "
                                        + "If the manager does not respond before the review deadline, "
                                        + "the system may perform automatic verification.";


                        showMessage(
                                "Progress verification is currently pending."
                        );


                        Alert pendingAlert =
                                new Alert(
                                        Alert.AlertType.INFORMATION
                                );


                        pendingAlert.setTitle(
                                "Verification Pending"
                        );


                        pendingAlert.setHeaderText(
                                "Progress submission is under review"
                        );


                        pendingAlert.setContentText(
                                pendingMessage
                        );


                        if (stage != null) {

                        pendingAlert.initOwner(
                                stage
                        );
                        }


                        pendingAlert.showAndWait();


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


                        return;
                }


                // =================================================
                // REAL ERROR
                // =================================================

                showMessage(
                        errorMessage
                );


                Alert errorAlert =
                        new Alert(
                                Alert.AlertType.ERROR
                        );


                errorAlert.setTitle(
                        "Submission Failed"
                );


                errorAlert.setHeaderText(
                        "Unable to submit progress"
                );


                errorAlert.setContentText(
                        errorMessage
                );


                if (stage != null) {

                        errorAlert.initOwner(
                                stage
                        );
                }


                errorAlert.showAndWait();


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
        // =========================================================
        // IMPORTANT: START BACKGROUND SUBMISSION TASK
        // =========================================================

        Thread submitThread =
                new Thread(
                        submitTask
                );


        submitThread.setDaemon(
                true
        );


        submitThread.start();
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