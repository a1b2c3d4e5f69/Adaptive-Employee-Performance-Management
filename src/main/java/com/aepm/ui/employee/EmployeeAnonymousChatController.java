package com.aepm.ui.employee;

import com.aepm.logic.anonymous.*;
import com.aepm.model.*;
import com.aepm.ui.common.AnonymousUiController;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.List;

public class EmployeeAnonymousChatController extends AnonymousUiController {
    @FXML private TextField referenceField;
    @FXML private TextArea messageArea;
    @FXML private Label caseLabel;
    @FXML private TableView<AnonymousReportMessage> conversationTable;
    @FXML private Button sendButton;
    private final AnonymousReportChatService chat;
    private final AnonymousReportTrackingService tracking;
    private String loadedReference;
    private record Conversation(AnonymousReport report,List<AnonymousReportMessage> messages) {}
    public EmployeeAnonymousChatController() {this(new AnonymousReportChatService(),new AnonymousReportTrackingService());}
    public EmployeeAnonymousChatController(AnonymousReportChatService chat,AnonymousReportTrackingService tracking) {this.chat=chat;this.tracking=tracking;}
    @FXML private void initialize() {
        conversation(conversationTable);sendButton.setDisable(true);
        referenceField.textProperty().addListener((observable,oldValue,newValue)->{
            loadedReference=null;sendButton.setDisable(true);conversationTable.getItems().clear();caseLabel.setText("Enter your report reference.");
        });
        initializeView(3);
    }
    public void setReference(String reference) {referenceField.setText(reference);handleRefresh();}
    @FXML private void handleRefresh() {
        if(!authorized()) return;
        String reference=referenceField.getText();
        loadedReference=null;sendButton.setDisable(true);conversationTable.getItems().clear();
        if(!validReference(reference)) return;
        run(()->{
            var report=tracking.track(reference).orElseThrow();
            return new Conversation(report,chat.employeeHistory(reference));
        },result->{
            loadedReference=reference;conversationTable.getItems().setAll(result.messages());sendButton.setDisable(false);
            caseLabel.setText(result.report().subject()+" | "+result.report().status().value());message("Conversation refreshed.",true);
        },"Report not found or invalid reference.");
    }
    @FXML private void handleSend() {
        if(!authorized() || loadedReference==null || !loadedReference.equals(referenceField.getText())) return;
        String reference=loadedReference,content=messageArea.getText();
        if(!required(content,"Message",5000)) return;
        run(()->chat.sendEmployee(reference,content),sent->{
            messageArea.clear();conversationTable.getItems().add(sent);handleRefresh();
        },"Unable to send the message. Refresh and try again. This report may no longer accept messages.");
    }
    @FXML private void handleBack() {
        String reference=loadedReference;
        if(reference==null || !authorized()) {navigate("/fxml/employee/employeeAnonymousReport.fxml");return;}
        navigate("/fxml/employee/employeeAnonymousReport.fxml",(EmployeeAnonymousReportController controller)->controller.setTrackingReference(reference));
    }
    @Override protected void onLeave() {
        loadedReference=null;referenceField.clear();messageArea.clear();conversationTable.getItems().clear();caseLabel.setText("");
    }
}
