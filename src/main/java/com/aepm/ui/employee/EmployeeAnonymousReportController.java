package com.aepm.ui.employee;

import com.aepm.logic.anonymous.AnonymousReportService;
import com.aepm.logic.anonymous.AnonymousReportTrackingService;
import com.aepm.ui.common.AnonymousUiController;
import com.aepm.ui.common.chat.ChatController;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.FileChooser;

public class EmployeeAnonymousReportController
        extends AnonymousUiController {

    @FXML
    private ComboBox<String> categoryCombo;

    @FXML
    private TextField subjectField;

    @FXML
    private TextField evidenceField;

    @FXML
    private TextField referenceDisplay;

    @FXML
    private TextField trackingReferenceField;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private Button copyReferenceButton;

    @FXML
    private Button openChatButton;

    @FXML
    private Button anonymousNavButton;

    @FXML
    private Button chatNavButton;

    @FXML
    private Label trackedSubject;

    @FXML
    private Label trackedCategory;

    @FXML
    private Label trackedStatus;

    @FXML
    private Label trackedSubmitted;

    @FXML
    private Label trackedUpdated;

    @FXML
    private Label trackedResolution;

    @FXML
    private Label trackedResolutionCaption;

    private final AnonymousReportService reports;

    private final AnonymousReportTrackingService tracking;

    private String trackedReference;

    private ChatController chatController;


    public EmployeeAnonymousReportController() {

        this(
                new AnonymousReportService(),
                new AnonymousReportTrackingService()
        );
    }


    public EmployeeAnonymousReportController(
            AnonymousReportService reports,
            AnonymousReportTrackingService tracking
    ) {

        this.reports = reports;
        this.tracking = tracking;
    }


    @FXML
    private void initialize() {

        categoryCombo
                .getItems()
                .setAll(
                        CATEGORIES
                );

        copyReferenceButton
                .disableProperty()
                .bind(
                        referenceDisplay
                                .textProperty()
                                .isEmpty()
                );

        trackingReferenceField
                .textProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> clearTracking()
                );

        clearTracking();

        initializeView(
                3
        );
    }


    @FXML
    private void handleSubmit() {

        if (!authorized()) {
            return;
        }

        String category =
                categoryCombo.getValue();

        String subject =
                subjectField.getText();

        String description =
                descriptionArea.getText();

        String evidence =
                evidenceField.getText();

        if (
                !required(
                        category,
                        "Category",
                        100
                )
                        || !required(
                        subject,
                        "Subject",
                        200
                )
                        || !required(
                        description,
                        "Description",
                        10000
                )
                        || !optional(
                        evidence,
                        "Evidence path",
                        2048
                )
        ) {

            return;
        }

        run(
                () ->
                        reports.submit(
                                category,
                                subject,
                                description,
                                evidence
                        ),
                receipt -> {

                    referenceDisplay.setText(
                            receipt.reference()
                    );

                    trackingReferenceField.setText(
                            receipt.reference()
                    );

                    message(
                            "Report submitted. Keep this reference number safely. "
                                    + "You will need it to track this report.",
                            true
                    );

                    categoryCombo.setValue(
                            null
                    );

                    subjectField.clear();
                    descriptionArea.clear();
                    evidenceField.clear();
                },
                "Unable to submit the report. Please try again. "
                        + "If the connection was interrupted, avoid submitting repeatedly."
        );
    }


    @FXML
    private void handleBrowseEvidence() {

        if (!authorized()) {
            return;
        }

        FileChooser chooser =
                new FileChooser();

        chooser.setTitle(
                "Select evidence file name"
        );

        var file =
                chooser.showOpenDialog(
                        root.getScene() == null
                                ? null
                                : root.getScene()
                                        .getWindow()
                );

        if (file != null) {

            // Store only the file name to avoid exposing local paths.
            evidenceField.setText(
                    file.getName()
            );
        }
    }


    @FXML
    private void handleCopyReference() {

        if (
                !authorized()
                        || referenceDisplay
                                .getText()
                                .isBlank()
        ) {

            return;
        }

        ClipboardContent content =
                new ClipboardContent();

        content.putString(
                referenceDisplay.getText()
        );

        Clipboard
                .getSystemClipboard()
                .setContent(
                        content
                );

        message(
                "Reference copied. Keep it private and safe.",
                true
        );
    }


    @FXML
    private void handleClearForm() {

        categoryCombo.setValue(
                null
        );

        subjectField.clear();
        descriptionArea.clear();
        evidenceField.clear();
        referenceDisplay.clear();

        message(
                "Submission form cleared.",
                true
        );
    }


    @FXML
    private void handleTrack() {

        if (!authorized()) {
            return;
        }

        String reference =
                trackingReferenceField
                        .getText();

        clearTracking();

        if (!validReference(reference)) {
            return;
        }

        run(
                () ->
                        tracking.track(
                                reference
                        ),
                result -> {

                    if (result.isEmpty()) {

                        message(
                                "Report not found or invalid reference.",
                                false
                        );

                        return;
                    }

                    var report =
                            result.get();

                    trackedReference =
                            reference;

                    trackedSubject.setText(
                            report.subject()
                    );

                    trackedCategory.setText(
                            report.category()
                    );

                    trackedStatus.setText(
                            report.status()
                                    .value()
                    );

                    trackedSubmitted.setText(
                            date(
                                    report.submittedAt()
                            )
                    );

                    trackedUpdated.setText(
                            date(
                                    report.updatedAt()
                            )
                    );

                    trackedResolution.setText(
                            text(
                                    report.resolution()
                            )
                    );

                    showResolution(
                            report.resolution() != null
                                    && !report.resolution()
                                            .isBlank()
                    );

                    openChatButton.setDisable(
                            false
                    );

                    message(
                            "Report status loaded.",
                            true
                    );
                },
                "Report not found or invalid reference."
        );
    }


    @FXML
    private void handleOpenChat() {

        if (
                !authorized()
                        || trackedReference == null
                        || !trackedReference.equals(
                                trackingReferenceField
                                        .getText()
                        )
        ) {

            return;
        }

        String reference =
                trackedReference;

        navigate(
                "/fxml/employee/employeeAnonymousChat.fxml",
                (
                        EmployeeAnonymousChatController controller
                ) ->
                        controller.setReference(
                                reference
                        )
        );
    }


    // =========================================================
    // SHARED CHAT CENTER
    // =========================================================

    private void setChatNavigationActive() {

        if (anonymousNavButton != null) {

            anonymousNavButton
                    .getStyleClass()
                    .removeAll(
                            "employee-nav-button",
                            "employee-nav-button-active"
                    );

            anonymousNavButton
                    .getStyleClass()
                    .add(
                            "employee-nav-button"
                    );
        }

        if (chatNavButton != null) {

            chatNavButton
                    .getStyleClass()
                    .removeAll(
                            "employee-nav-button",
                            "employee-nav-button-active",
                            "manager-nav-button",
                            "manager-nav-button-active"
                    );

            chatNavButton
                    .getStyleClass()
                    .add(
                            "employee-nav-button-active"
                    );
        }
    }


    @FXML
    private void handleChat(
            ActionEvent event
    ) {

        try {

            if (chatController != null) {

                chatController.stop();

                chatController = null;
            }

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/fxml/common/chat.fxml"
                            )
                    );

            Parent chatRoot =
                    loader.load();

            chatController =
                    loader.getController();

            setChatNavigationActive();

            // `root` is the BorderPane provided by AnonymousUiController.
            root.setCenter(
                    chatRoot
            );

        } catch (Exception e) {

            System.err.println(
                    "Failed to open Chat Center from Anonymous Report."
            );

            e.printStackTrace();
        }
    }


    public void setTrackingReference(
            String reference
    ) {

        trackingReferenceField.setText(
                reference
        );

        handleTrack();
    }


    private void showResolution(
            boolean show
    ) {

        trackedResolution.setVisible(
                show
        );

        trackedResolution.setManaged(
                show
        );

        trackedResolutionCaption.setVisible(
                show
        );

        trackedResolutionCaption.setManaged(
                show
        );
    }


    private void clearTracking() {

        showResolution(
                false
        );

        trackedReference =
                null;

        openChatButton.setDisable(
                true
        );

        for (
                Label label
                : new Label[]{
                        trackedSubject,
                        trackedCategory,
                        trackedStatus,
                        trackedSubmitted,
                        trackedUpdated,
                        trackedResolution
                }
        ) {

            label.setText(
                    "-"
            );
        }
    }


    @Override
    protected void onLeave() {

        if (chatController != null) {

            chatController.stop();

            chatController = null;
        }

        trackedReference =
                null;

        referenceDisplay.clear();
        trackingReferenceField.clear();
        subjectField.clear();
        descriptionArea.clear();
        evidenceField.clear();

        clearTracking();
    }
}
