package com.aepm.ui.manager;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;

import com.aepm.logic.task.ManagerProgressVerificationService;
import com.aepm.logic.task.ManagerProgressVerificationService.EvidenceItem;
import com.aepm.logic.task.ManagerProgressVerificationService.VerificationItem;

import com.aepm.util.SessionManager;

import javafx.concurrent.Task;

import javafx.geometry.Insets;
import javafx.geometry.Pos;

import javafx.scene.control.*;

import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import javafx.stage.Window;

import java.awt.Desktop;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.nio.file.Files;
import java.nio.file.Path;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import java.util.UUID;


public final class ManagerProgressVerificationDialog {

    private static final String EVIDENCE_BUCKET =
            "task-progress-evidence";


    private static final ZoneId MALAYSIA =
            ZoneId.of(
                    "Asia/Kuala_Lumpur"
            );


    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern(
                    "dd MMM yyyy, hh:mm a"
            );


    private ManagerProgressVerificationDialog() {
    }


    // =========================================================
    // SHOW
    // =========================================================

    public static void show(
            Window owner,
            String submissionId
    ) {

        if (
                submissionId == null
                        || submissionId.isBlank()
        ) {

            showError(
                    owner,
                    "Unable to Review",
                    "Progress submission reference is missing."
            );

            return;
        }


        UUID id;


        try {

            id =
                    UUID.fromString(
                            submissionId
                    );

        } catch (Exception e) {

            showError(
                    owner,
                    "Unable to Review",
                    "Invalid progress submission reference."
            );

            return;
        }


        ManagerProgressVerificationService service =
                new ManagerProgressVerificationService();


        Dialog<Void> loadingDialog =
                new Dialog<>();


        if (owner != null) {

            loadingDialog.initOwner(
                    owner
            );
        }


        loadingDialog.setTitle(
                "Progress Verification"
        );


        loadingDialog
                .getDialogPane()
                .getButtonTypes()
                .add(
                        ButtonType.CANCEL
                );


        ProgressIndicator indicator =
                new ProgressIndicator();


        Label loadingLabel =
                new Label(
                        "Loading progress submission..."
                );


        VBox loading =
                new VBox(
                        12,
                        indicator,
                        loadingLabel
                );


        loading.setAlignment(
                Pos.CENTER
        );


        loading.setPadding(
                new Insets(
                        35
                )
        );


        loadingDialog
                .getDialogPane()
                .setContent(
                        loading
                );


        Task<VerificationItem> task =
                new Task<>() {

                    @Override
                    protected VerificationItem call()
                            throws Exception {

                        return service
                                .getSubmissionForReview(
                                        id
                                );
                    }
                };


        task.setOnSucceeded(
                event -> {

                    loadingDialog.close();


                    showReviewDialog(
                            owner,
                            task.getValue(),
                            service
                    );
                }
        );


        task.setOnFailed(
                event -> {

                    loadingDialog.close();


                    Throwable error =
                            task.getException();


                    if (error != null) {

                        error.printStackTrace();
                    }


                    showError(
                            owner,
                            "Unable to Review",
                            error == null
                                    ? "Unable to load the progress submission."
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


        loadingDialog.show();
    }


    // =========================================================
    // REVIEW DIALOG
    // =========================================================

    private static void showReviewDialog(
            Window owner,
            VerificationItem item,
            ManagerProgressVerificationService service
    ) {

        Dialog<Void> dialog =
                new Dialog<>();


        if (owner != null) {

            dialog.initOwner(
                    owner
            );
        }


        dialog.setTitle(
                "Progress Verification"
        );


        DialogPane pane =
                dialog.getDialogPane();


        pane.setPrefWidth(
                760
        );


        pane.setPrefHeight(
                690
        );


        pane.setStyle(
                "-fx-background-color: #F6F8FC;"
        );


        ButtonType closeType =
                new ButtonType(
                        "Close",
                        ButtonBar.ButtonData.CANCEL_CLOSE
                );


        pane
                .getButtonTypes()
                .add(
                        closeType
                );


        // =====================================================
        // HEADER
        // =====================================================

        Label heading =
                new Label(
                        "Progress Verification"
                );


        heading.setStyle(
                "-fx-font-size: 23px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-text-fill: #0F2345;"
        );


        Label subtitle =
                new Label(
                        "Review the employee's submitted progress and supporting evidence."
                );


        subtitle.setWrapText(
                true
        );


        subtitle.setStyle(
                "-fx-font-size: 12px;"
                        + "-fx-text-fill: #64748B;"
        );


        VBox header =
                new VBox(
                        5,
                        heading,
                        subtitle
                );


        // =====================================================
        // BASIC INFO
        // =====================================================

        VBox employeeBox =
                field(
                        "Employee",
                        item.getEmployeeName()
                );


        VBox taskBox =
                field(
                        "Task",
                        item.getTaskTitle()
                );


        HBox basicRow =
                new HBox(
                        12,
                        employeeBox,
                        taskBox
                );


        HBox.setHgrow(
                employeeBox,
                Priority.ALWAYS
        );


        HBox.setHgrow(
                taskBox,
                Priority.ALWAYS
        );


        employeeBox.setMaxWidth(
                Double.MAX_VALUE
        );


        taskBox.setMaxWidth(
                Double.MAX_VALUE
        );


        // =====================================================
        // PROGRESS
        // =====================================================

        VBox current =
                progressField(
                        "Actual Verified Progress",
                        item.getCurrentProgress()
                );


        VBox requested =
                progressField(
                        "Requested Progress",
                        item.getRequestedProgress()
                );


        HBox progressRow =
                new HBox(
                        12,
                        current,
                        requested
                );


        HBox.setHgrow(
                current,
                Priority.ALWAYS
        );


        HBox.setHgrow(
                requested,
                Priority.ALWAYS
        );


        current.setMaxWidth(
                Double.MAX_VALUE
        );


        requested.setMaxWidth(
                Double.MAX_VALUE
        );


        // =====================================================
        // NOTE
        // =====================================================

        VBox noteBox =
                field(
                        "Progress Note",
                        empty(
                                item.getProgressNote()
                        )
                );


        // =====================================================
        // DATE
        // =====================================================

        VBox submitted =
                field(
                        "Submitted",
                        date(
                                item.getSubmittedAt()
                        )
                );


        VBox deadline =
                field(
                        "Review Deadline",
                        date(
                                item.getReviewDeadline()
                        )
                );


        HBox dates =
                new HBox(
                        12,
                        submitted,
                        deadline
                );


        HBox.setHgrow(
                submitted,
                Priority.ALWAYS
        );


        HBox.setHgrow(
                deadline,
                Priority.ALWAYS
        );


        submitted.setMaxWidth(
                Double.MAX_VALUE
        );


        deadline.setMaxWidth(
                Double.MAX_VALUE
        );


        // =====================================================
        // EVIDENCE
        // =====================================================

        VBox evidence =
                new VBox(
                        8
                );


        Label evidenceTitle =
                new Label(
                        "Supporting Evidence"
                );


        evidenceTitle.setStyle(
                "-fx-font-size: 12px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-text-fill: #334155;"
        );


        evidence
                .getChildren()
                .add(
                        evidenceTitle
                );


        if (
                item.getEvidence() == null
                        || item
                        .getEvidence()
                        .isEmpty()
        ) {

            Label noEvidence =
                    new Label(
                            "No evidence uploaded."
                    );


            noEvidence.setStyle(
                    "-fx-text-fill: #DC2626;"
            );


            evidence
                    .getChildren()
                    .add(
                            noEvidence
                    );

        } else {

            for (
                    EvidenceItem file
                    : item.getEvidence()
            ) {

                evidence
                        .getChildren()
                        .add(
                                evidenceRow(
                                        owner,
                                        file
                                )
                        );
            }
        }


        // =====================================================
        // COMMENT
        // =====================================================

        Label commentLabel =
                new Label(
                        "Review Comment"
                );


        commentLabel.setStyle(
                "-fx-font-size: 12px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-text-fill: #334155;"
        );


        TextArea comment =
                new TextArea();


        comment.setPromptText(
                "Enter approval comment, rejection reason or revision instruction..."
        );


        comment.setPrefRowCount(
                3
        );


        comment.setWrapText(
                true
        );


        VBox commentBox =
                new VBox(
                        6,
                        commentLabel,
                        comment
                );


        // =====================================================
        // ACTION BUTTONS
        // =====================================================

        Button reject =
                button(
                        "Reject",
                        "#DC2626"
                );


        Button revision =
                button(
                        "Request Revision",
                        "#D97706"
                );


        Button approve =
                button(
                        "Approve",
                        "#16A34A"
                );


        reject.setOnAction(
                event -> {

                    String reason =
                            comment
                                    .getText()
                                    .trim();


                    if (reason.isBlank()) {

                        showError(
                                owner,
                                "Reason Required",
                                "Please enter a rejection reason."
                        );

                        return;
                    }


                    runAction(
                            owner,
                            dialog,
                            () ->
                                    service.reject(
                                            item.getSubmissionId(),
                                            reason
                                    ),
                            "Progress submission rejected."
                    );
                }
        );


        revision.setOnAction(
                event -> {

                    String instruction =
                            comment
                                    .getText()
                                    .trim();


                    if (instruction.isBlank()) {

                        showError(
                                owner,
                                "Instruction Required",
                                "Please enter revision instructions."
                        );

                        return;
                    }


                    runAction(
                            owner,
                            dialog,
                            () ->
                                    service.requestRevision(
                                            item.getSubmissionId(),
                                            instruction
                                    ),
                            "Revision requested successfully."
                    );
                }
        );


        approve.setOnAction(
                event -> {

                    runAction(
                            owner,
                            dialog,
                            () ->
                                    service.approve(
                                            item.getSubmissionId(),
                                            comment
                                                    .getText()
                                                    .trim()
                                    ),
                            "Progress approved successfully."
                    );
                }
        );


        Region spacer =
                new Region();


        HBox.setHgrow(
                spacer,
                Priority.ALWAYS
        );


        HBox actions =
                new HBox(
                        10,
                        spacer,
                        reject,
                        revision,
                        approve
                );


        actions.setAlignment(
                Pos.CENTER_RIGHT
        );


        // =====================================================
        // ROOT
        // =====================================================

        VBox content =
                new VBox(
                        14,
                        header,
                        basicRow,
                        progressRow,
                        noteBox,
                        dates,
                        evidence,
                        commentBox,
                        actions
                );


        content.setPadding(
                new Insets(
                        20
                )
        );


        ScrollPane scroll =
                new ScrollPane(
                        content
                );


        scroll.setFitToWidth(
                true
        );


        scroll.setHbarPolicy(
                ScrollPane.ScrollBarPolicy.NEVER
        );


        scroll.setStyle(
                "-fx-background: transparent;"
                        + "-fx-background-color: transparent;"
        );


        pane.setContent(
                scroll
        );


        dialog.showAndWait();
    }


    // =========================================================
    // EVIDENCE ROW
    // =========================================================

    private static HBox evidenceRow(
            Window owner,
            EvidenceItem item
    ) {

        Label name =
                new Label(
                        item.getFileName()
                );


        name.setStyle(
                "-fx-font-weight: bold;"
                        + "-fx-text-fill: #0F172A;"
        );


        Label info =
                new Label(
                        fileSize(
                                item.getFileSize()
                        )
                                + (
                                item.getFileType() == null
                                        || item
                                        .getFileType()
                                        .isBlank()

                                ? ""

                                : " • "
                                        + item.getFileType()
                        )
                );


        info.setStyle(
                "-fx-font-size: 11px;"
                        + "-fx-text-fill: #64748B;"
        );


        VBox text =
                new VBox(
                        2,
                        name,
                        info
                );


        Button open =
                new Button(
                        "Open Evidence"
                );


        open.setStyle(
                "-fx-background-color: #2563EB;"
                        + "-fx-text-fill: white;"
                        + "-fx-background-radius: 7;"
                        + "-fx-cursor: hand;"
        );


        open.setOnAction(
                event ->
                        openEvidence(
                                owner,
                                item,
                                open
                        )
        );


        Region spacer =
                new Region();


        HBox.setHgrow(
                spacer,
                Priority.ALWAYS
        );


        HBox row =
                new HBox(
                        10,
                        text,
                        spacer,
                        open
                );


        row.setAlignment(
                Pos.CENTER_LEFT
        );


        row.setPadding(
                new Insets(
                        10
                )
        );


        row.setStyle(
                "-fx-background-color: #F8FAFC;"
                        + "-fx-border-color: #E2E8F0;"
                        + "-fx-background-radius: 8;"
                        + "-fx-border-radius: 8;"
        );


        return row;
    }


    // =========================================================
    // OPEN EVIDENCE
    // =========================================================

    private static void openEvidence(
            Window owner,
            EvidenceItem item,
            Button button
    ) {

        button.setDisable(
                true
        );


        Task<Path> task =
                new Task<>() {

                    @Override
                    protected Path call()
                            throws Exception {

                        String token =
                                SessionManager
                                        .getAccessToken();


                        String url =
                                AppConfig.STORAGE_URL
                                        + "/object/"
                                        + EVIDENCE_BUCKET
                                        + "/"
                                        + item.getFilePath();


                        SupabaseClient client =
                                SupabaseClient
                                        .getInstance();


                        HttpRequest request =
                                client
                                        .createAuthenticatedRequest(
                                                url,
                                                token
                                        )
                                        .GET()
                                        .build();


                        HttpResponse<byte[]> response =
                                client
                                        .getHttpClient()
                                        .send(
                                                request,
                                                HttpResponse.BodyHandlers
                                                        .ofByteArray()
                                        );


                        if (
                                response.statusCode() < 200
                                        || response.statusCode() >= 300
                        ) {

                            throw new IllegalStateException(
                                    "Unable to download evidence. HTTP "
                                            + response.statusCode()
                            );
                        }


                        String suffix =
                                suffix(
                                        item.getFileName()
                                );


                        Path file =
                                Files.createTempFile(
                                        "aepm-evidence-",
                                        suffix
                                );


                        Files.write(
                                file,
                                response.body()
                        );


                        file
                                .toFile()
                                .deleteOnExit();


                        return file;
                    }
                };


        task.setOnSucceeded(
                event -> {

                    button.setDisable(
                            false
                    );


                    try {

                        Desktop
                                .getDesktop()
                                .open(
                                        task.getValue()
                                                .toFile()
                                );

                    } catch (Exception e) {

                        showError(
                                owner,
                                "Unable to Open",
                                e.getMessage()
                        );
                    }
                }
        );


        task.setOnFailed(
                event -> {

                    button.setDisable(
                            false
                    );


                    Throwable error =
                            task.getException();


                    showError(
                            owner,
                            "Unable to Open",
                            error == null
                                    ? "Unable to open evidence."
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
    // RUN REVIEW
    // =========================================================

    private static void runAction(
            Window owner,
            Dialog<Void> dialog,
            ReviewAction action,
            String success
    ) {

        dialog
                .getDialogPane()
                .setDisable(
                        true
                );


        Task<Void> task =
                new Task<>() {

                    @Override
                    protected Void call()
                            throws Exception {

                        action.run();

                        return null;
                    }
                };


        task.setOnSucceeded(
                event -> {

                    dialog.close();


                    Alert alert =
                            new Alert(
                                    Alert.AlertType.INFORMATION
                            );


                    if (owner != null) {

                        alert.initOwner(
                                owner
                        );
                    }


                    alert.setTitle(
                            "Success"
                    );


                    alert.setHeaderText(
                            null
                    );


                    alert.setContentText(
                            success
                    );


                    alert.showAndWait();
                }
        );


        task.setOnFailed(
                event -> {

                    dialog
                            .getDialogPane()
                            .setDisable(
                                    false
                            );


                    Throwable error =
                            task.getException();


                    showError(
                            owner,
                            "Unable to Process",
                            error == null
                                    ? "Unable to process verification."
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
    // UI HELPERS
    // =========================================================

    private static VBox field(
            String name,
            String value
    ) {

        Label key =
                new Label(
                        name
                );


        key.setStyle(
                "-fx-font-size: 11px;"
                        + "-fx-text-fill: #64748B;"
        );


        Label val =
                new Label(
                        value
                );


        val.setWrapText(
                true
        );


        val.setStyle(
                "-fx-font-size: 13px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-text-fill: #0F172A;"
        );


        VBox box =
                new VBox(
                        4,
                        key,
                        val
                );


        box.setPadding(
                new Insets(
                        11
                )
        );


        box.setStyle(
                "-fx-background-color: #FFFFFF;"
                        + "-fx-border-color: #E2E8F0;"
                        + "-fx-background-radius: 8;"
                        + "-fx-border-radius: 8;"
        );


        return box;
    }


    private static VBox progressField(
            String title,
            int progress
    ) {

        Label key =
                new Label(
                        title
                );


        Label value =
                new Label(
                        progress
                                + "%"
                );


        value.setStyle(
                "-fx-font-size: 21px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-text-fill: #0F2345;"
        );


        ProgressBar bar =
                new ProgressBar(
                        Math.max(
                                0,
                                Math.min(
                                        1,
                                        progress / 100.0
                                )
                        )
                );


        bar.setMaxWidth(
                Double.MAX_VALUE
        );


        VBox box =
                new VBox(
                        5,
                        key,
                        value,
                        bar
                );


        box.setPadding(
                new Insets(
                        12
                )
        );


        box.setStyle(
                "-fx-background-color: #FFFFFF;"
                        + "-fx-border-color: #E2E8F0;"
                        + "-fx-background-radius: 8;"
                        + "-fx-border-radius: 8;"
        );


        return box;
    }


    private static Button button(
            String text,
            String color
    ) {

        Button button =
                new Button(
                        text
                );


        button.setStyle(
                "-fx-background-color: "
                        + color
                        + ";"
                        + "-fx-text-fill: white;"
                        + "-fx-font-weight: bold;"
                        + "-fx-background-radius: 8;"
                        + "-fx-padding: 9 15;"
                        + "-fx-cursor: hand;"
        );


        return button;
    }


    private static String date(
            String value
    ) {

        if (
                value == null
                        || value.isBlank()
        ) {

            return "-";
        }


        try {

            return OffsetDateTime
                    .parse(
                            value
                    )
                    .atZoneSameInstant(
                            MALAYSIA
                    )
                    .format(
                            FORMATTER
                    );

        } catch (Exception e) {

            return value;
        }
    }


    private static String fileSize(
            long bytes
    ) {

        if (bytes < 1024) {

            return bytes
                    + " B";
        }


        if (
                bytes
                        < 1024L * 1024L
        ) {

            return String.format(
                    "%.1f KB",
                    bytes / 1024.0
            );
        }


        return String.format(
                "%.1f MB",
                bytes / (1024.0 * 1024.0)
        );
    }


    private static String suffix(
            String name
    ) {

        if (
                name == null
                        || !name.contains(".")
        ) {

            return ".tmp";
        }


        return name.substring(
                name.lastIndexOf(".")
        );
    }


    private static String empty(
            String value
    ) {

        return value == null
                || value.isBlank()
                ? "-"
                : value;
    }


    private static void showError(
            Window owner,
            String title,
            String message
    ) {

        Alert alert =
                new Alert(
                        Alert.AlertType.ERROR
                );


        if (owner != null) {

            alert.initOwner(
                    owner
            );
        }


        alert.setTitle(
                title
        );


        alert.setHeaderText(
                null
        );


        alert.setContentText(
                message
        );


        alert.showAndWait();
    }


    @FunctionalInterface
    private interface ReviewAction {

        void run()
                throws Exception;
    }
}