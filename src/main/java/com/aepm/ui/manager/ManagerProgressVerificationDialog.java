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

import javafx.scene.layout.GridPane;
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
                900
        );


        pane.setPrefHeight(
                820
        );


        pane.setStyle(
                "-fx-background-color: #F4F8F7;"
                        + "-fx-font-family: 'Plus Jakarta Sans';"
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


        Button defaultClose =
                (Button) pane.lookupButton(
                        closeType
                );


        defaultClose.setVisible(
                false
        );


        defaultClose.setManaged(
                false
        );


        // =====================================================
        // HEADER
        // =====================================================

        Label icon =
                new Label(
                        "▤"
                );


        icon.setAlignment(
                Pos.CENTER
        );


        icon.setMinSize(
                54,
                54
        );


        icon.setMaxSize(
                54,
                54
        );


        icon.setStyle(
                "-fx-background-color: #DFF7F2;"
                        + "-fx-background-radius: 15;"
                        + "-fx-text-fill: #0D9488;"
                        + "-fx-font-size: 22px;"
                        + "-fx-font-weight: bold;"
        );


        Label heading =
                new Label(
                        "Progress Verification"
                );


        heading.setStyle(
                "-fx-font-size: 26px;"
                        + "-fx-font-weight: 800;"
                        + "-fx-text-fill: #10264A;"
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
                        + "-fx-text-fill: #7184A4;"
        );


        VBox headingText =
                new VBox(
                        5,
                        heading,
                        subtitle
                );


        HBox header =
                new HBox(
                        16,
                        icon,
                        headingText
                );


        header.setAlignment(
                Pos.CENTER_LEFT
        );


        header.setPadding(
                new Insets(
                        18,
                        22,
                        12,
                        22
                )
        );


        // =====================================================
        // TASK TITLE + BADGES
        // =====================================================

        Label taskTitle =
                new Label(
                        item.getTaskTitle()
                );


        taskTitle.setWrapText(
                true
        );


        taskTitle.setStyle(
                "-fx-font-size: 22px;"
                        + "-fx-font-weight: 800;"
                        + "-fx-text-fill: #10264A;"
        );


        Label statusBadge =
                badge(
                        "PENDING REVIEW",
                        "#E7F7ED",
                        "#16834A"
                );


        Label currentBadge =
                badge(
                        item.getCurrentProgress()
                                + "% CURRENT",
                        "#E8F0FF",
                        "#2563EB"
                );


        Label requestedBadge =
                badge(
                        item.getRequestedProgress()
                                + "% REQUESTED",
                        "#FFF2DC",
                        "#A75A08"
                );


        HBox badges =
                new HBox(
                        10,
                        statusBadge,
                        currentBadge,
                        requestedBadge
                );


        VBox taskHeader =
                new VBox(
                        12,
                        taskTitle,
                        badges
                );


        // =====================================================
        // EMPLOYEE / SUBMISSION INFO
        // =====================================================

        Label employeeSectionTitle =
                sectionTitle(
                        "EMPLOYEE & SUBMISSION"
                );


        VBox employeeBox =
                infoCard(
                        "Employee Name",
                        item.getEmployeeName()
                );


        VBox submittedBox =
                infoCard(
                        "Submitted",
                        date(
                                item.getSubmittedAt()
                        )
                );


        HBox employeeRow =
                infoRow(
                        employeeBox,
                        submittedBox
                );


        VBox employeeSection =
                new VBox(
                        10,
                        employeeSectionTitle,
                        employeeRow
                );


        // =====================================================
        // PROGRESS VERIFICATION PANEL
        // =====================================================

        Label verificationTitle =
                sectionTitle(
                        "PROGRESS VERIFICATION"
                );


        VBox currentBox =
                infoCard(
                        "Actual Verified Progress",
                        item.getCurrentProgress()
                                + "%"
                );


        VBox requestedBox =
                infoCard(
                        "Requested Progress",
                        item.getRequestedProgress()
                                + "%"
                );


        VBox deadlineBox =
                infoCard(
                        "Review Deadline",
                        date(
                                item.getReviewDeadline()
                        )
                );


        VBox statusBox =
                infoCard(
                        "Verification Status",
                        "Pending Manager Review"
                );


        GridPane progressGrid =
                new GridPane();


        progressGrid.setHgap(
                14
        );


        progressGrid.setVgap(
                12
        );


        progressGrid.add(
                currentBox,
                0,
                0
        );


        progressGrid.add(
                requestedBox,
                1,
                0
        );


        progressGrid.add(
                deadlineBox,
                0,
                1
        );


        progressGrid.add(
                statusBox,
                1,
                1
        );


        javafx.scene.layout.ColumnConstraints column1 =
                new javafx.scene.layout.ColumnConstraints();


        column1.setPercentWidth(
                50
        );


        javafx.scene.layout.ColumnConstraints column2 =
                new javafx.scene.layout.ColumnConstraints();


        column2.setPercentWidth(
                50
        );


        progressGrid
                .getColumnConstraints()
                .addAll(
                        column1,
                        column2
                );


        Label noteLabel =
                new Label(
                        "PROGRESS NOTE"
                );


        noteLabel.setStyle(
                "-fx-font-size: 11px;"
                        + "-fx-font-weight: 700;"
                        + "-fx-text-fill: #7184A4;"
        );


        Label note =
                new Label(
                        empty(
                                item.getProgressNote()
                        )
                );


        note.setWrapText(
                true
        );


        note.setMaxWidth(
                Double.MAX_VALUE
        );


        note.setStyle(
                "-fx-font-size: 13px;"
                        + "-fx-font-weight: 600;"
                        + "-fx-text-fill: #334B70;"
        );


        VBox noteBox =
                new VBox(
                        7,
                        noteLabel,
                        note
                );


        noteBox.setPadding(
                new Insets(
                        14
                )
        );


        noteBox.setStyle(
                "-fx-background-color: #FFFFFF;"
                        + "-fx-background-radius: 10;"
                        + "-fx-border-color: #DCE7E4;"
                        + "-fx-border-radius: 10;"
        );


        VBox verificationPanel =
                new VBox(
                        12,
                        verificationTitle,
                        progressGrid,
                        noteBox
                );


        verificationPanel.setPadding(
                new Insets(
                        18
                )
        );


        verificationPanel.setStyle(
                "-fx-background-color: #EAF6F4;"
                        + "-fx-background-radius: 12;"
                        + "-fx-border-color: #C9E7E1;"
                        + "-fx-border-radius: 12;"
        );


        // =====================================================
        // EVIDENCE
        // =====================================================

        Label evidenceTitle =
                sectionTitle(
                        "SUPPORTING EVIDENCE"
                );


        VBox evidenceList =
                new VBox(
                        8
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
                            + "-fx-font-size: 12px;"
            );


            evidenceList
                    .getChildren()
                    .add(
                            noEvidence
                    );

        } else {

            for (
                    EvidenceItem file
                    : item.getEvidence()
            ) {

                evidenceList
                        .getChildren()
                        .add(
                                evidenceRow(
                                        owner,
                                        file
                                )
                        );
            }
        }


        VBox evidenceSection =
                new VBox(
                        10,
                        evidenceTitle,
                        evidenceList
                );


        // =====================================================
        // COMMENT
        // =====================================================

        Label commentLabel =
                sectionTitle(
                        "MANAGER REVIEW COMMENT"
                );


        TextArea comment =
                new TextArea();


        comment.setPromptText(
                "Record your approval comment, rejection reason or revision instruction..."
        );


        comment.setPrefRowCount(
                4
        );


        comment.setWrapText(
                true
        );


        comment.setStyle(
                "-fx-background-color: #FFFFFF;"
                        + "-fx-border-color: #C9DAD6;"
                        + "-fx-border-radius: 8;"
                        + "-fx-background-radius: 8;"
                        + "-fx-font-size: 12px;"
                        + "-fx-focus-color: transparent;"
                        + "-fx-faint-focus-color: transparent;"
        );


        VBox commentSection =
                new VBox(
                        10,
                        commentLabel,
                        comment
                );


        // =====================================================
        // ACTION BUTTONS
        // =====================================================

        Button reject =
                actionButton(
                        "Reject",
                        "#FCE8E8",
                        "#C2413A"
                );


        Button revision =
                actionButton(
                        "Request Revision",
                        "#FFF1E6",
                        "#C76824"
                );


        Button approve =
                actionButton(
                        "Approve",
                        "#E7F7ED",
                        "#16834A"
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


        HBox actions =
                new HBox(
                        10,
                        reject,
                        revision,
                        approve
                );


        actions.setAlignment(
                Pos.CENTER_LEFT
        );


        // =====================================================
        // MAIN CARD
        // =====================================================

        VBox mainCard =
                new VBox(
                        18,
                        taskHeader,
                        employeeSection,
                        verificationPanel,
                        evidenceSection,
                        commentSection,
                        actions
                );


        mainCard.setPadding(
                new Insets(
                        20
                )
        );


        mainCard.setStyle(
                "-fx-background-color: #FFFFFF;"
                        + "-fx-background-radius: 14;"
                        + "-fx-border-color: #DCE7E4;"
                        + "-fx-border-radius: 14;"
        );


        VBox body =
                new VBox(
                        8,
                        header,
                        mainCard
                );


        body.setPadding(
                new Insets(
                        10,
                        16,
                        16,
                        16
                )
        );


        ScrollPane scroll =
                new ScrollPane(
                        body
                );


        scroll.setFitToWidth(
                true
        );


        scroll.setHbarPolicy(
                ScrollPane.ScrollBarPolicy.NEVER
        );


        scroll.setVbarPolicy(
                ScrollPane.ScrollBarPolicy.AS_NEEDED
        );


        scroll.setStyle(
                "-fx-background: #F4F8F7;"
                        + "-fx-background-color: #F4F8F7;"
                        + "-fx-border-color: transparent;"
        );


        // =====================================================
        // FOOTER
        // =====================================================

        Button closeButton =
                new Button(
                        "Close"
                );


        closeButton.setStyle(
                "-fx-background-color: #0F766E;"
                        + "-fx-text-fill: #FFFFFF;"
                        + "-fx-font-weight: 700;"
                        + "-fx-background-radius: 9;"
                        + "-fx-padding: 11 30;"
                        + "-fx-cursor: hand;"
        );


        closeButton.setOnAction(
                event ->
                        dialog.close()
        );


        HBox footer =
                new HBox(
                        closeButton
                );


        footer.setAlignment(
                Pos.CENTER_RIGHT
        );


        footer.setPadding(
                new Insets(
                        14,
                        20,
                        14,
                        20
                )
        );


        footer.setStyle(
                "-fx-background-color: #FFFFFF;"
                        + "-fx-border-color: #DCE7E4 transparent transparent transparent;"
                        + "-fx-border-width: 1 0 0 0;"
        );


        VBox layout =
                new VBox(
                        scroll,
                        footer
                );


        VBox.setVgrow(
                scroll,
                Priority.ALWAYS
        );


        pane.setContent(
                layout
        );


        dialog.showAndWait();
    }


    // =========================================================
    // UI HELPERS
    // =========================================================

    private static Label sectionTitle(
            String text
    ) {

        Label label =
                new Label(
                        text
                );


        label.setStyle(
                "-fx-font-size: 11px;"
                        + "-fx-font-weight: 700;"
                        + "-fx-text-fill: #7184A4;"
        );


        return label;
    }


    private static Label badge(
            String text,
            String background,
            String foreground
    ) {

        Label label =
                new Label(
                        text
                );


        label.setStyle(
                "-fx-background-color: "
                        + background
                        + ";"
                        + "-fx-text-fill: "
                        + foreground
                        + ";"
                        + "-fx-background-radius: 20;"
                        + "-fx-padding: 6 13;"
                        + "-fx-font-size: 11px;"
                        + "-fx-font-weight: 700;"
        );


        return label;
    }


    private static VBox infoCard(
            String title,
            String value
    ) {

        Label titleLabel =
                new Label(
                        title
                );


        titleLabel.setStyle(
                "-fx-font-size: 11px;"
                        + "-fx-text-fill: #7184A4;"
        );


        Label valueLabel =
                new Label(
                        value == null
                                || value.isBlank()
                                ? "-"
                                : value
                );


        valueLabel.setWrapText(
                true
        );


        valueLabel.setStyle(
                "-fx-font-size: 13px;"
                        + "-fx-font-weight: 700;"
                        + "-fx-text-fill: #10264A;"
        );


        VBox box =
                new VBox(
                        7,
                        titleLabel,
                        valueLabel
                );


        box.setPadding(
                new Insets(
                        13
                )
        );


        box.setMaxWidth(
                Double.MAX_VALUE
        );


        box.setStyle(
                "-fx-background-color: #F8FAFD;"
                        + "-fx-background-radius: 10;"
                        + "-fx-border-color: #E3EAF4;"
                        + "-fx-border-radius: 10;"
        );


        return box;
    }


    private static HBox infoRow(
            VBox left,
            VBox right
    ) {

        HBox row =
                new HBox(
                        14,
                        left,
                        right
                );


        left.setMinWidth(
                0
        );


        right.setMinWidth(
                0
        );


        left.setMaxWidth(
                Double.MAX_VALUE
        );


        right.setMaxWidth(
                Double.MAX_VALUE
        );


        HBox.setHgrow(
                left,
                Priority.ALWAYS
        );


        HBox.setHgrow(
                right,
                Priority.ALWAYS
        );


        return row;
    }


    private static Button actionButton(
            String text,
            String background,
            String foreground
    ) {

        Button button =
                new Button(
                        text
                );


        button.setStyle(
                "-fx-background-color: "
                        + background
                        + ";"
                        + "-fx-text-fill: "
                        + foreground
                        + ";"
                        + "-fx-font-weight: 700;"
                        + "-fx-background-radius: 9;"
                        + "-fx-padding: 10 16;"
                        + "-fx-cursor: hand;"
        );


        return button;
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


        name.setWrapText(
                true
        );


        name.setStyle(
                "-fx-font-weight: 700;"
                        + "-fx-text-fill: #10264A;"
                        + "-fx-font-size: 12px;"
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
                        + "-fx-text-fill: #7184A4;"
        );


        VBox text =
                new VBox(
                        3,
                        name,
                        info
                );


        Button open =
                new Button(
                        "Open Evidence"
                );


        open.setStyle(
                "-fx-background-color: #0F766E;"
                        + "-fx-text-fill: white;"
                        + "-fx-background-radius: 8;"
                        + "-fx-font-weight: 700;"
                        + "-fx-padding: 9 15;"
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
                        12,
                        text,
                        spacer,
                        open
                );


        row.setAlignment(
                Pos.CENTER_LEFT
        );


        row.setPadding(
                new Insets(
                        12
                )
        );


        row.setStyle(
                "-fx-background-color: #F8FAFD;"
                        + "-fx-border-color: #DCE7E4;"
                        + "-fx-background-radius: 10;"
                        + "-fx-border-radius: 10;"
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
    // FORMAT HELPERS
    // =========================================================

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


    // =========================================================
    // ERROR
    // =========================================================

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
