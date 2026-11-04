package com.aepm.ui.admin;

import com.aepm.logic.anonymous.AnonymousReportService;
import com.aepm.model.*;
import com.aepm.ui.common.AnonymousUiController;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.time.LocalDate;

public class AdminAnonymousReportsController extends AnonymousUiController {
    private static final int PAGE_SIZE=25;
    @FXML private ComboBox<String> statusFilter,categoryFilter;
    @FXML private DatePicker startDate,endDate;
    @FXML private TableView<AnonymousReport> reportsTable;
    @FXML private Button previousButton,nextButton,detailButton;
    @FXML private Label pageLabel;
    private final AnonymousReportService reports;
    private int offset;
    private long total;
    public AdminAnonymousReportsController() {this(new AnonymousReportService());}
    public AdminAnonymousReportsController(AnonymousReportService reports) {this.reports=reports;}
    @FXML private void initialize() {
        statusFilter.getItems().add("All");for(var status:AnonymousReportStatus.values()) statusFilter.getItems().add(status.value());
        statusFilter.setValue("All");categoryFilter.getItems().add("All");categoryFilter.getItems().addAll(CATEGORIES);categoryFilter.setValue("All");
        column(reportsTable,"Submitted (UTC+08)",175,row->date(row.submittedAt()));
        column(reportsTable,"Category",180,AnonymousReport::category);column(reportsTable,"Subject",300,AnonymousReport::subject);
        column(reportsTable,"Status",170,row->row.status().value());column(reportsTable,"Updated (UTC+08)",175,row->date(row.updatedAt()));
        Label placeholder=new Label("No matching anonymous reports.");placeholder.getStyleClass().add("card-description");reportsTable.setPlaceholder(placeholder);
        detailButton.disableProperty().bind(reportsTable.getSelectionModel().selectedItemProperty().isNull());
        reportsTable.setRowFactory(table->{TableRow<AnonymousReport> row=new TableRow<>();row.setOnMouseClicked(event->{
            if(event.getClickCount()==2 && !row.isEmpty()) handleViewDetail();});return row;});
        statusFilter.valueProperty().addListener((o,a,b)->invalidateFilters());
        categoryFilter.valueProperty().addListener((o,a,b)->invalidateFilters());
        categoryFilter.getEditor().textProperty().addListener((o,a,b)->invalidateFilters());
        startDate.valueProperty().addListener((o,a,b)->invalidateFilters());endDate.valueProperty().addListener((o,a,b)->invalidateFilters());
        if(initializeView(1)) load(0);
    }
    private void invalidateFilters() {
        reportsTable.getItems().clear();offset=0;total=0;previousButton.setDisable(true);nextButton.setDisable(true);
        pageLabel.setText("Apply filters to load reports.");
    }
    @FXML private void handleApplyFilters() {load(0);}
    @FXML private void handleClearFilters() {
        statusFilter.setValue("All");categoryFilter.setValue("All");categoryFilter.getEditor().setText("All");
        startDate.setValue(null);endDate.setValue(null);load(0);
    }
    @FXML private void handleRefresh() {load(0);}
    @FXML private void handlePrevious() {load(Math.max(0,offset-PAGE_SIZE));}
    @FXML private void handleNext() {if((long)offset+PAGE_SIZE<total) load(offset+PAGE_SIZE);}
    private void load(int requestedOffset) {
        if(!authorized()) return;
        try {startDate.commitValue();endDate.commitValue();}
        catch(RuntimeException invalidDate) {message("Enter valid submitted dates or select them from the calendar.",false);return;}
        LocalDate from=startDate.getValue(),through=endDate.getValue();
        if(from!=null && through!=null && from.isAfter(through)) {message("Start date must not be after end date.",false);return;}
        String selected=statusFilter.getValue();
        AnonymousReportStatus status=selected==null || selected.equals("All")?null:AnonymousReportStatus.fromValue(selected);
        String typed=categoryFilter.getEditor().getText();
        String category=typed==null || typed.isBlank()?categoryFilter.getValue():typed.strip();
        if("All".equals(category) || category==null || category.isBlank()) category=null;
        if(!optional(category,"Category",100)) return;
        final String filter=category;
        reportsTable.getItems().clear();previousButton.setDisable(true);nextButton.setDisable(true);pageLabel.setText("Loading reports...");
        run(()->reports.review(status,filter,from,through,PAGE_SIZE,requestedOffset),page->{
            offset=requestedOffset;total=page.totalCount();reportsTable.getItems().setAll(page.rows());
            previousButton.setDisable(offset==0);nextButton.setDisable((long)offset+page.rows().size()>=total);
            pageLabel.setText(page.rows().isEmpty()?"0 records on this page ("+total+" total)":
                    (offset+1)+" - "+(offset+page.rows().size())+" of "+total);
            message(page.rows().isEmpty()?"No reports match the selected filters.":"Reports loaded.",true);
        },"Unable to load reports. Refresh and try again.");
    }
    @FXML private void handleViewDetail() {
        if(!authorized()) return;
        var selected=reportsTable.getSelectionModel().getSelectedItem();if(selected==null) return;
        navigate("/fxml/admin/adminAnonymousReportDetail.fxml",(AdminAnonymousReportDetailController controller)->controller.setReportId(selected.reportId()));
    }
    @Override protected void onLeave() {reportsTable.getItems().clear();}
}
