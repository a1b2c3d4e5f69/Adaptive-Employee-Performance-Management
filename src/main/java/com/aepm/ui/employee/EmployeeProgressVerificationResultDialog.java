package com.aepm.ui.employee;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;
import com.aepm.util.SessionManager;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class EmployeeProgressVerificationResultDialog {

    private static final ZoneId MALAYSIA =
            ZoneId.of("Asia/Kuala_Lumpur");

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private EmployeeProgressVerificationResultDialog() {
    }

    public static void show(
            Window owner,
            String submissionId
    ) {

        if (submissionId == null || submissionId.isBlank()) {
            showError(
                    owner,
                    "Unable to Open",
                    "The progress submission reference is missing."
            );
            return;
        }

        UUID id;

        try {
            id = UUID.fromString(submissionId);
        } catch (Exception e) {
            showError(
                    owner,
                    "Unable to Open",
                    "Invalid progress submission reference."
            );
            return;
        }

        Dialog<Void> loadingDialog = new Dialog<>();

        if (owner != null) {
            loadingDialog.initOwner(owner);
        }

        loadingDialog.setTitle("Progress Verification Result");

        loadingDialog.getDialogPane()
                .getButtonTypes()
                .add(ButtonType.CANCEL);

        ProgressIndicator indicator = new ProgressIndicator();

        Label loadingLabel =
                new Label("Loading verification result...");

        VBox loading =
                new VBox(
                        12,
                        indicator,
                        loadingLabel
                );

        loading.setAlignment(Pos.CENTER);
        loading.setPadding(new Insets(35));

        loadingDialog.getDialogPane()
                .setContent(loading);

        Task<ResultData> task = new Task<>() {
            @Override
            protected ResultData call() throws Exception {
                return loadResult(id);
            }
        };

        task.setOnSucceeded(event -> {
            loadingDialog.close();
            showResultDialog(
                    owner,
                    task.getValue()
            );
        });

        task.setOnFailed(event -> {
            loadingDialog.close();

            Throwable error =
                    task.getException();

            if (error != null) {
                error.printStackTrace();
            }

            showError(
                    owner,
                    "Unable to Open",
                    error == null
                            ? "Unable to load the verification result."
                            : error.getMessage()
            );
        });

        Thread thread =
                new Thread(
                        task,
                        "employee-progress-result"
                );

        thread.setDaemon(true);
        thread.start();

        loadingDialog.show();
    }

    private static ResultData loadResult(
            UUID submissionId
    ) throws Exception {

        String accessToken =
                SessionManager.getAccessToken();

        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException(
                    "Your session has expired. Please login again."
            );
        }

        SupabaseClient client =
                SupabaseClient.getInstance();

        String submissionUrl =
                AppConfig.REST_URL
                        + "/task_progress_submission_tbl"
                        + "?select=*"
                        + "&submission_id=eq."
                        + submissionId
                        + "&limit=1";

        HttpRequest submissionRequest =
                client.createAuthenticatedRequest(
                                submissionUrl,
                                accessToken
                        )
                        .GET()
                        .build();

        HttpResponse<String> submissionResponse =
                client.getHttpClient()
                        .send(
                                submissionRequest,
                                HttpResponse.BodyHandlers.ofString()
                        );

        validate(
                submissionResponse,
                "Unable to load progress verification result"
        );

        JSONArray submissions =
                new JSONArray(
                        submissionResponse.body()
                );

        if (submissions.isEmpty()) {
            throw new IllegalArgumentException(
                    "Progress submission was not found."
            );
        }

        JSONObject submission =
                submissions.getJSONObject(0);

        String taskId =
                submission.optString(
                        "task_id",
                        ""
                );

        String taskTitle =
                "Task";

        int actualProgress =
                0;

        if (!taskId.isBlank()) {

            String taskUrl =
                    AppConfig.REST_URL
                            + "/task_tbl"
                            + "?select=task_title,progress"
                            + "&task_id=eq."
                            + taskId
                            + "&limit=1";

            HttpRequest taskRequest =
                    client.createAuthenticatedRequest(
                                    taskUrl,
                                    accessToken
                            )
                            .GET()
                            .build();

            HttpResponse<String> taskResponse =
                    client.getHttpClient()
                            .send(
                                    taskRequest,
                                    HttpResponse.BodyHandlers.ofString()
                            );

            validate(
                    taskResponse,
                    "Unable to load task information"
            );

            JSONArray tasks =
                    new JSONArray(
                            taskResponse.body()
                    );

            if (!tasks.isEmpty()) {

                JSONObject task =
                        tasks.getJSONObject(0);

                taskTitle =
                        task.optString(
                                "task_title",
                                "Task"
                        );

                actualProgress =
                        task.optInt(
                                "progress",
                                0
                        );
            }
        }

        return new ResultData(
                submissionId.toString(),
                taskTitle,
                submission.optInt(
                        "proposed_progress",
                        0
                ),
                actualProgress,
                submission.optString(
                        "status",
                        ""
                ),
                submission.optString(
                        "verification_method",
                        ""
                ),
                submission.optString(
                        "progress_note",
                        ""
                ),
                submission.optString(
                        "review_comment",
                        ""
                ),
                submission.optString(
                        "system_reason",
                        ""
                ),
                submission.optString(
                        "submitted_at",
                        ""
                ),
                submission.optString(
                        "reviewed_at",
                        ""
                )
        );
    }

    private static void showResultDialog(
            Window owner,
            ResultData data
    ) {

        Dialog<Void> dialog =
                new Dialog<>();

        if (owner != null) {
            dialog.initOwner(owner);
        }

        dialog.setTitle(
                "Progress Verification Result"
        );

        DialogPane pane =
                dialog.getDialogPane();

        pane.setPrefWidth(780);
        pane.setPrefHeight(700);

        pane.setStyle(
                "-fx-font-family: 'Plus Jakarta Sans';"
                        + "-fx-background-color: #FCFBF7;"
        );

        ButtonType closeType =
                new ButtonType(
                        "Close",
                        ButtonBar.ButtonData.CANCEL_CLOSE
                );

        pane.getButtonTypes()
                .add(closeType);

        Button defaultClose =
                (Button) pane.lookupButton(
                        closeType
                );

        defaultClose.setVisible(false);
        defaultClose.setManaged(false);

        Label icon =
                new Label("✓");

        icon.setAlignment(Pos.CENTER);
        icon.setMinSize(54, 54);
        icon.setMaxSize(54, 54);

        icon.setStyle(
                "-fx-background-color: #FFF3C4;"
                        + "-fx-background-radius: 15;"
                        + "-fx-text-fill: #B7791F;"
                        + "-fx-font-size: 23px;"
                        + "-fx-font-weight: bold;"
        );

        Label heading =
                new Label(
                        "Progress Verification Result"
                );

        heading.setStyle(
                "-fx-font-size: 25px;"
                        + "-fx-font-weight: 800;"
                        + "-fx-text-fill: #111827;"
        );

        Label subtitle =
                new Label(
                        "Review the verification outcome for your submitted task progress."
                );

        subtitle.setWrapText(true);

        subtitle.setStyle(
                "-fx-font-size: 12px;"
                        + "-fx-text-fill: #7C7A73;"
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

        Label taskTitle =
                new Label(
                        data.taskTitle
                );

        taskTitle.setWrapText(true);

        taskTitle.setStyle(
                "-fx-font-size: 21px;"
                        + "-fx-font-weight: 800;"
                        + "-fx-text-fill: #111827;"
        );

        Label statusBadge =
                badge(
                        statusText(
                                data.status
                        ),
                        statusBackground(
                                data.status
                        ),
                        statusForeground(
                                data.status
                        )
                );

        Label requestedBadge =
                badge(
                        data.requestedProgress
                                + "% SUBMITTED",
                        "#FFF7DB",
                        "#A16207"
                );

        HBox badges =
                new HBox(
                        10,
                        statusBadge,
                        requestedBadge
                );

        VBox titleSection =
                new VBox(
                        11,
                        taskTitle,
                        badges
                );

        Label summaryTitle =
                sectionTitle(
                        "VERIFICATION SUMMARY"
                );

        VBox submittedBox =
                infoCard(
                        "Submitted Progress",
                        data.requestedProgress
                                + "%"
                );

        VBox actualBox =
                infoCard(
                        "Current Verified Progress",
                        data.actualProgress
                                + "%"
                );

        HBox progressRow =
                infoRow(
                        submittedBox,
                        actualBox
                );

        VBox methodBox =
                infoCard(
                        "Verification Method",
                        displayMethod(
                                data.verificationMethod
                        )
                );

        VBox decisionBox =
                infoCard(
                        "Decision",
                        displayStatus(
                                data.status
                        )
                );

        HBox decisionRow =
                infoRow(
                        methodBox,
                        decisionBox
                );

        VBox summarySection =
                new VBox(
                        10,
                        summaryTitle,
                        progressRow,
                        decisionRow
                );

        summarySection.setPadding(
                new Insets(18)
        );

        summarySection.setStyle(
                "-fx-background-color: #FFF9E8;"
                        + "-fx-background-radius: 12;"
                        + "-fx-border-color: #F0D98A;"
                        + "-fx-border-radius: 12;"
        );

        Label noteTitle =
                sectionTitle(
                        "YOUR PROGRESS NOTE"
                );

        VBox noteCard =
                textCard(
                        empty(
                                data.progressNote
                        )
                );

        VBox noteSection =
                new VBox(
                        10,
                        noteTitle,
                        noteCard
                );

        String feedbackTitle;

        if ("Revision Required".equalsIgnoreCase(data.status)) {
            feedbackTitle =
                    "MANAGER REVISION INSTRUCTION";
        } else if ("Rejected".equalsIgnoreCase(data.status)) {
            feedbackTitle =
                    "MANAGER REJECTION REASON";
        } else if ("System Approved".equalsIgnoreCase(data.status)) {
            feedbackTitle =
                    "SYSTEM VERIFICATION RESULT";
        } else {
            feedbackTitle =
                    "REVIEW COMMENT";
        }

        Label feedbackLabel =
                sectionTitle(
                        feedbackTitle
                );

        String feedbackText =
                "System Approved".equalsIgnoreCase(
                        data.status
                )
                        ? data.systemReason
                        : data.reviewComment;

        VBox feedbackCard =
                textCard(
                        empty(
                                feedbackText
                        )
                );

        VBox feedbackSection =
                new VBox(
                        10,
                        feedbackLabel,
                        feedbackCard
                );

        Label timelineTitle =
                sectionTitle(
                        "TIMELINE"
                );

        VBox submittedAt =
                infoCard(
                        "Submitted At",
                        date(
                                data.submittedAt
                        )
                );

        VBox reviewedAt =
                infoCard(
                        "Reviewed At",
                        date(
                                data.reviewedAt
                        )
                );

        HBox timelineRow =
                infoRow(
                        submittedAt,
                        reviewedAt
                );

        VBox timelineSection =
                new VBox(
                        10,
                        timelineTitle,
                        timelineRow
                );

        VBox mainCard =
                new VBox(
                        18,
                        titleSection,
                        summarySection,
                        noteSection,
                        feedbackSection,
                        timelineSection
                );

        mainCard.setPadding(
                new Insets(20)
        );

        mainCard.setStyle(
                "-fx-background-color: #FFFFFF;"
                        + "-fx-background-radius: 14;"
                        + "-fx-border-color: #E7E1D2;"
                        + "-fx-border-radius: 14;"
        );

        VBox body =
                new VBox(
                        16,
                        header,
                        mainCard
                );

        body.setPadding(
                new Insets(
                        18,
                        18,
                        18,
                        18
                )
        );

        ScrollPane scroll =
                new ScrollPane(
                        body
                );

        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(
                ScrollPane.ScrollBarPolicy.NEVER
        );
        scroll.setVbarPolicy(
                ScrollPane.ScrollBarPolicy.AS_NEEDED
        );

        scroll.setStyle(
                "-fx-background: #FCFBF7;"
                        + "-fx-background-color: #FCFBF7;"
                        + "-fx-border-color: transparent;"
        );

        Button closeButton =
                new Button(
                        "Close"
                );

        closeButton.setStyle(
                "-fx-background-color: #111827;"
                        + "-fx-text-fill: #FBBF24;"
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
                        + "-fx-border-color: #E7E1D2 transparent transparent transparent;"
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
                        + "-fx-text-fill: #8A8069;"
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
                        + "-fx-text-fill: #8A8069;"
        );

        Label valueLabel =
                new Label(
                        empty(
                                value
                        )
                );

        valueLabel.setWrapText(true);

        valueLabel.setStyle(
                "-fx-font-size: 13px;"
                        + "-fx-font-weight: 700;"
                        + "-fx-text-fill: #111827;"
        );

        VBox box =
                new VBox(
                        7,
                        titleLabel,
                        valueLabel
                );

        box.setPadding(
                new Insets(13)
        );

        box.setMinWidth(0);
        box.setMaxWidth(
                Double.MAX_VALUE
        );

        box.setStyle(
                "-fx-background-color: #FFFCF5;"
                        + "-fx-background-radius: 10;"
                        + "-fx-border-color: #E9DFBF;"
                        + "-fx-border-radius: 10;"
        );

        return box;
    }

    private static VBox textCard(
            String value
    ) {

        Label label =
                new Label(
                        value
                );

        label.setWrapText(true);
        label.setMaxWidth(
                Double.MAX_VALUE
        );

        label.setStyle(
                "-fx-font-size: 13px;"
                        + "-fx-text-fill: #374151;"
        );

        VBox box =
                new VBox(
                        label
                );

        box.setPadding(
                new Insets(14)
        );

        box.setStyle(
                "-fx-background-color: #FFFCF5;"
                        + "-fx-background-radius: 10;"
                        + "-fx-border-color: #E9DFBF;"
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

        left.setMinWidth(0);
        right.setMinWidth(0);

        HBox.setHgrow(
                left,
                Priority.ALWAYS
        );

        HBox.setHgrow(
                right,
                Priority.ALWAYS
        );

        left.setMaxWidth(
                Double.MAX_VALUE
        );

        right.setMaxWidth(
                Double.MAX_VALUE
        );

        return row;
    }

    private static String statusText(
            String status
    ) {

        if ("Approved".equalsIgnoreCase(status)) {
            return "APPROVED";
        }

        if ("Rejected".equalsIgnoreCase(status)) {
            return "REJECTED";
        }

        if ("Revision Required".equalsIgnoreCase(status)) {
            return "REVISION REQUIRED";
        }

        if ("System Approved".equalsIgnoreCase(status)) {
            return "SYSTEM VERIFIED";
        }

        return status == null
                || status.isBlank()
                ? "RESULT"
                : status.toUpperCase();
    }

    private static String statusBackground(
            String status
    ) {

        if ("Approved".equalsIgnoreCase(status)
                || "System Approved".equalsIgnoreCase(status)) {
            return "#E8F7EC";
        }

        if ("Rejected".equalsIgnoreCase(status)) {
            return "#FDECEC";
        }

        return "#FFF3D6";
    }

    private static String statusForeground(
            String status
    ) {

        if ("Approved".equalsIgnoreCase(status)
                || "System Approved".equalsIgnoreCase(status)) {
            return "#16834A";
        }

        if ("Rejected".equalsIgnoreCase(status)) {
            return "#C2413A";
        }

        return "#A16207";
    }

    private static String displayStatus(
            String status
    ) {

        if (status == null || status.isBlank()) {
            return "-";
        }

        if ("System Approved".equalsIgnoreCase(status)) {
            return "Automatically Verified";
        }

        return status;
    }

    private static String displayMethod(
            String method
    ) {

        if (method == null || method.isBlank()) {
            return "-";
        }

        if ("MANAGER".equalsIgnoreCase(method)) {
            return "Manager Review";
        }

        if ("SYSTEM".equalsIgnoreCase(method)) {
            return "System Verification";
        }

        return method;
    }

    private static String date(
            String value
    ) {

        if (value == null || value.isBlank()) {
            return "-";
        }

        try {
            return OffsetDateTime
                    .parse(value)
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

    private static String empty(
            String value
    ) {

        return value == null
                || value.isBlank()
                ? "-"
                : value;
    }

    private static void validate(
            HttpResponse<String> response,
            String message
    ) {

        if (
                response.statusCode() < 200
                        || response.statusCode() >= 300
        ) {

            throw new RuntimeException(
                    message
                            + " [HTTP "
                            + response.statusCode()
                            + "]: "
                            + response.body()
            );
        }
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
            alert.initOwner(owner);
        }

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static class ResultData {

        private final String submissionId;
        private final String taskTitle;
        private final int requestedProgress;
        private final int actualProgress;
        private final String status;
        private final String verificationMethod;
        private final String progressNote;
        private final String reviewComment;
        private final String systemReason;
        private final String submittedAt;
        private final String reviewedAt;

        private ResultData(
                String submissionId,
                String taskTitle,
                int requestedProgress,
                int actualProgress,
                String status,
                String verificationMethod,
                String progressNote,
                String reviewComment,
                String systemReason,
                String submittedAt,
                String reviewedAt
        ) {
            this.submissionId = submissionId;
            this.taskTitle = taskTitle;
            this.requestedProgress = requestedProgress;
            this.actualProgress = actualProgress;
            this.status = status;
            this.verificationMethod = verificationMethod;
            this.progressNote = progressNote;
            this.reviewComment = reviewComment;
            this.systemReason = systemReason;
            this.submittedAt = submittedAt;
            this.reviewedAt = reviewedAt;
        }
    }
}
