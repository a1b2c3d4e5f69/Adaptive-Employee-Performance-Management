package com.aepm.ui.employee;

import com.aepm.logic.anonymous.*;
import com.aepm.ui.common.AnonymousUiController;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.FileChooser;

public class EmployeeAnonymousReportController extends AnonymousUiController {
    @FXML private ComboBox<String> categoryCombo;
    @FXML private TextField subjectField,evidenceField,referenceDisplay,trackingReferenceField;
    @FXML private TextArea descriptionArea;
    @FXML private Button copyReferenceButton,openChatButton;
    @FXML private Label trackedSubject,trackedCategory,trackedStatus,trackedSubmitted,trackedUpdated,trackedResolution,trackedResolutionCaption;
    private final AnonymousReportService reports;
    private final AnonymousReportTrackingService tracking;
    private String trackedReference;
    public EmployeeAnonymousReportController() {this(new AnonymousReportService(),new AnonymousReportTrackingService());}
    public EmployeeAnonymousReportController(AnonymousReportService reports,AnonymousReportTrackingService tracking) {
        this.reports=reports;this.tracking=tracking;
    }
    @FXML private void initialize() {
        categoryCombo.getItems().setAll(CATEGORIES);
        copyReferenceButton.disableProperty().bind(referenceDisplay.textProperty().isEmpty());
        trackingReferenceField.textProperty().addListener((observable,oldValue,newValue)->clearTracking());
        clearTracking();initializeView(3);
    }
    @FXML private void handleSubmit() {
        if(!authorized()) return;
        String category=categoryCombo.getValue(),subject=subjectField.getText(),description=descriptionArea.getText(),evidence=evidenceField.getText();
        if(!required(category,"Category",100) || !required(subject,"Subject",200)
                || !required(description,"Description",10000) || !optional(evidence,"Evidence path",2048)) return;
        run(()->reports.submit(category,subject,description,evidence),receipt->{
            referenceDisplay.setText(receipt.reference());
            trackingReferenceField.setText(receipt.reference());
            message("Report submitted. Keep this reference number safely. You will need it to track this report.",true);
            // A successful form is cleared so a second click cannot submit the same content accidentally.
            categoryCombo.setValue(null);subjectField.clear();descriptionArea.clear();evidenceField.clear();
        },"Unable to submit the report. Please try again. If the connection was interrupted, avoid submitting repeatedly.");
    }
    @FXML private void handleBrowseEvidence() {
        if(!authorized()) return;
        FileChooser chooser=new FileChooser();chooser.setTitle("Select evidence file name");
        var file=chooser.showOpenDialog(root.getScene()==null?null:root.getScene().getWindow());
        if(file!=null) evidenceField.setText(file.getName()); // Do not reveal the employee's local user-directory path.
    }
    @FXML private void handleCopyReference() {
        if(!authorized() || referenceDisplay.getText().isBlank()) return;
        ClipboardContent content=new ClipboardContent();content.putString(referenceDisplay.getText());
        Clipboard.getSystemClipboard().setContent(content);message("Reference copied. Keep it private and safe.",true);
    }
    @FXML private void handleClearForm() {
        categoryCombo.setValue(null);subjectField.clear();descriptionArea.clear();evidenceField.clear();referenceDisplay.clear();
        message("Submission form cleared.",true);
    }
    @FXML private void handleTrack() {
        if(!authorized()) return;
        String reference=trackingReferenceField.getText();clearTracking();
        if(!validReference(reference)) return;
        run(()->tracking.track(reference),result->{
            if(result.isEmpty()) {message("Report not found or invalid reference.",false);return;}
            var report=result.get();trackedReference=reference;
            trackedSubject.setText(report.subject());trackedCategory.setText(report.category());
            trackedStatus.setText(report.status().value());trackedSubmitted.setText(date(report.submittedAt()));
            trackedUpdated.setText(date(report.updatedAt()));trackedResolution.setText(text(report.resolution()));
            showResolution(report.resolution()!=null && !report.resolution().isBlank());
            openChatButton.setDisable(false);message("Report status loaded.",true);
        },"Report not found or invalid reference.");
    }
    @FXML private void handleOpenChat() {
        if(!authorized() || trackedReference==null || !trackedReference.equals(trackingReferenceField.getText())) return;
        String reference=trackedReference;
        navigate("/fxml/employee/employeeAnonymousChat.fxml",(EmployeeAnonymousChatController controller)->controller.setReference(reference));
    }
    public void setTrackingReference(String reference) {trackingReferenceField.setText(reference);handleTrack();}
    private void showResolution(boolean show) {
        trackedResolution.setVisible(show);trackedResolution.setManaged(show);
        trackedResolutionCaption.setVisible(show);trackedResolutionCaption.setManaged(show);
    }
    private void clearTracking() {
        showResolution(false);
        trackedReference=null;openChatButton.setDisable(true);
        for(Label label:new Label[]{trackedSubject,trackedCategory,trackedStatus,trackedSubmitted,trackedUpdated,trackedResolution}) label.setText("-");
    }
    @Override protected void onLeave() {
        trackedReference=null;referenceDisplay.clear();trackingReferenceField.clear();
        subjectField.clear();descriptionArea.clear();evidenceField.clear();clearTracking();
    }
}
