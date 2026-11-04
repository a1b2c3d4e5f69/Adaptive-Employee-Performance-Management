package com.aepm.ui.admin;

import com.aepm.logic.anonymous.*;
import com.aepm.model.*;
import com.aepm.ui.common.AnonymousUiController;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import java.util.*;

public class AdminAnonymousReportDetailController extends AnonymousUiController {
    @FXML private Label subjectLabel,categoryLabel,statusLabel,submittedLabel,updatedLabel,publishedResolutionLabel;
    @FXML private TextArea descriptionArea,replyArea,investigationNoteArea,resolutionArea;
    @FXML private TextField evidenceField;
    @FXML private TableView<AnonymousReportMessage> conversationTable;
    @FXML private TableView<AnonymousInvestigation> historyTable;
    @FXML private ComboBox<AnonymousReportStatus> newStatusCombo;
    @FXML private Button saveInvestigationButton,replyButton;
    private final AnonymousReportService reports;
    private final AnonymousReportChatService chat;
    private final AnonymousInvestigationService investigations;
    private UUID reportId;
    private AnonymousReport current;
    private record Detail(AnonymousReport report,List<AnonymousReportMessage> messages,List<AnonymousInvestigation> history) {}
    public AdminAnonymousReportDetailController() {this(new AnonymousReportService(),new AnonymousReportChatService(),new AnonymousInvestigationService());}
    public AdminAnonymousReportDetailController(AnonymousReportService reports,AnonymousReportChatService chat,AnonymousInvestigationService investigations) {
        this.reports=reports;this.chat=chat;this.investigations=investigations;
    }
    @FXML private void initialize() {
        conversation(conversationTable);
        column(historyTable,"Date (UTC+08)",170,row->date(row.createdAt()));
        column(historyTable,"Previous Status",165,row->status(row.previousStatus()));
        column(historyTable,"New Status",165,row->status(row.status()));
        column(historyTable,"Investigation Note (private)",320,row->text(row.notes()));
        column(historyTable,"Resolution",300,row->text(row.resolution()));
        Label placeholder=new Label("No investigation history yet.");placeholder.getStyleClass().add("card-description");historyTable.setPlaceholder(placeholder);
        newStatusCombo.setConverter(new StringConverter<>() {
            @Override public String toString(AnonymousReportStatus status) {return status==null?"":status.value();}
            @Override public AnonymousReportStatus fromString(String value) {return AnonymousReportStatus.fromValue(value);}
        });
        saveInvestigationButton.setDisable(true);replyButton.setDisable(true);initializeView(1);
    }
    public void setReportId(UUID id) {reportId=Objects.requireNonNull(id);load();}
    @FXML private void handleRefreshReport() {load();}
    private void load() {
        if(!authorized() || reportId==null) return;
        UUID id=reportId;
        run(()->new Detail(reports.detail(id).orElseThrow(),chat.administratorHistory(id),investigations.history(id)),detail->{
            current=detail.report();subjectLabel.setText(current.subject());categoryLabel.setText(current.category());
            descriptionArea.setText(current.description());evidenceField.setText(text(current.evidencePath()));
            statusLabel.setText(current.status().value());submittedLabel.setText(date(current.submittedAt()));updatedLabel.setText(date(current.updatedAt()));
            publishedResolutionLabel.setText(text(current.resolution()));conversationTable.getItems().setAll(detail.messages());historyTable.getItems().setAll(detail.history());
            newStatusCombo.getItems().clear();newStatusCombo.getItems().add(current.status());
            var values=AnonymousReportStatus.values();if(current.status().ordinal()+1<values.length) newStatusCombo.getItems().add(values[current.status().ordinal()+1]);
            newStatusCombo.setValue(current.status());
            String draft=detail.history().isEmpty()?current.resolution():detail.history().get(detail.history().size()-1).resolution();
            resolutionArea.setText(draft==null?"":draft);
            boolean closed=current.status()==AnonymousReportStatus.CLOSED;
            saveInvestigationButton.setDisable(closed);newStatusCombo.setDisable(closed);investigationNoteArea.setEditable(!closed);resolutionArea.setEditable(!closed);
            replyButton.setDisable(false);message("Report and private investigation history loaded.",true);
        },"Unable to load this report. Return to the list and refresh.");
    }
    @FXML private void handleRefreshMessages() {
        if(!authorized() || current==null) return;
        UUID id=reportId;
        run(()->chat.administratorHistory(id),messages->{conversationTable.getItems().setAll(messages);message("Conversation refreshed.",true);},
                "Unable to refresh the conversation. Please try again.");
    }
    @FXML private void handleSendReply() {
        if(!authorized() || current==null) return;
        String content=replyArea.getText();if(!required(content,"Reply",5000)) return;
        UUID id=reportId;
        run(()->chat.sendAdministrator(id,content),sent->{replyArea.clear();conversationTable.getItems().add(sent);handleRefreshMessages();},
                "Unable to send the reply. Refresh and try again. This report may no longer accept messages.");
    }
    @FXML private void handleSaveInvestigation() {
        if(!authorized() || current==null || current.status()==AnonymousReportStatus.CLOSED) return;
        var next=newStatusCombo.getValue();
        if(next==null || !current.status().canTransitionTo(next)) {message("Choose the current status or the next allowed status.",false);return;}
        String notes=investigationNoteArea.getText(),resolution=resolutionArea.getText();
        if(!optional(notes,"Investigation note",20000) || !optional(resolution,"Resolution",10000)) return;
        if((next==AnonymousReportStatus.RESOLVED || next==AnonymousReportStatus.CLOSED) && !required(resolution,"Resolution",10000)) return;
        if(next==current.status() && notes.isBlank() && resolution.isBlank()) {message("Enter an investigation note or choose a new status.",false);return;}
        UUID id=reportId;var expected=current.status();
        run(()->investigations.save(id,expected,next,notes,null,resolution),saved->{investigationNoteArea.clear();load();},
                "Unable to save the investigation. Refresh the report and check its current status before trying again.");
    }
    @FXML private void handleBack() {navigate("/fxml/admin/adminAnonymousReports.fxml");}
    @Override protected void onLeave() {
        reportId=null;current=null;descriptionArea.clear();replyArea.clear();investigationNoteArea.clear();resolutionArea.clear();
        evidenceField.clear();conversationTable.getItems().clear();historyTable.getItems().clear();
        for(Label label:new Label[]{subjectLabel,categoryLabel,statusLabel,submittedLabel,updatedLabel,publishedResolutionLabel}) label.setText("-");
    }
}
