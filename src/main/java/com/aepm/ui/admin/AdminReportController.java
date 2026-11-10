package com.aepm.ui.admin;

import com.aepm.logic.kpi.KpiCalculationService;
import com.aepm.logic.user.UserService;

import com.aepm.model.KpiResult;
import com.aepm.model.User;

import com.aepm.ui.navigation.NavigationManager;
import com.aepm.util.SessionManager;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

import javafx.event.ActionEvent;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;

import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AdminReportController {

    // =========================================================
    // TOP BAR
    // =========================================================

    @FXML
    private Label topNameLabel;

    @FXML
    private Label topRoleLabel;


    // =========================================================
    // FILTERS
    // =========================================================

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> departmentFilterComboBox;

    @FXML
    private ComboBox<String> statusFilterComboBox;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
        private void handleAttendanceManagement(ActionEvent event) {

        Stage stage = (Stage) ((Node) event.getSource())
                .getScene()
                .getWindow();

        NavigationManager.navigate(
                stage,
                "/fxml/admin/adminAttendance.fxml"
        );
    }

    @FXML
        private void handleAnnouncements(ActionEvent event) {
        AdminAnnouncementsDialog.show(
                ((Node) event.getSource())
                        .getScene()
                        .getWindow()
        );
    }

    @FXML
        private void handleSystemConfiguration(ActionEvent event) {
        navigate(
                event,
                "/fxml/admin/systemConfiguration.fxml"
        );
    }

           @FXML
        private void handleAIConfiguration(ActionEvent event) {
        navigate(
                event,
                "/fxml/admin/adminAIConfiguration.fxml"
        );
    }

    @FXML
        private void handleEscalationRules(ActionEvent event) {
        navigate(
                event,
                "/fxml/admin/adminEscalationRules.fxml"
        );
    }

    // =========================================================
    // TABLE
    // =========================================================

    @FXML
    private TableView<ReportRow> reportTable;

    @FXML
    private TableColumn<ReportRow, String> employeeColumn;

    @FXML
    private TableColumn<ReportRow, String> departmentColumn;

    @FXML
    private TableColumn<ReportRow, String> periodColumn;

    @FXML
    private TableColumn<ReportRow, Number> productivityColumn;

    @FXML
    private TableColumn<ReportRow, Number> timelinessColumn;

    @FXML
    private TableColumn<ReportRow, Number> completionColumn;

    @FXML
    private TableColumn<ReportRow, Number> attendanceColumn;

    @FXML
    private TableColumn<ReportRow, Number> overallColumn;

    @FXML
    private TableColumn<ReportRow, String> statusColumn;


    // =========================================================
    // MESSAGE
    // =========================================================

    @FXML
    private Label messageLabel;

    // =========================================================
    // REPORT STATISTICS
    // =========================================================

    @FXML
    private Label totalRecordsLabel;

    @FXML
    private Label averageKpiLabel;

    @FXML
    private Label atRiskCountLabel;

    @FXML
    private Label normalGoodCountLabel;


    // =========================================================
    // SERVICES
    // =========================================================

    private final UserService userService =
            new UserService();

    private final KpiCalculationService calculationService =
            new KpiCalculationService();


    // =========================================================
    // DATA
    // =========================================================

    private final ObservableList<ReportRow> allRows =
            FXCollections.observableArrayList();

    private FilteredList<ReportRow> filteredRows;


    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern(
                    "dd MMM yyyy"
            );


    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {

        loadCurrentAdmin();

        setupFilters();

        setupTable();

        setupRowOpening();

        hideMessage();

        loadReportData();
    }


    // =========================================================
    // CURRENT ADMIN
    // =========================================================

    private void loadCurrentAdmin() {

        User admin =
                SessionManager.getCurrentUser();


        if (admin == null) {

            topNameLabel.setText(
                    "Administrator"
            );

            topRoleLabel.setText(
                    "ADMINISTRATOR"
            );

            return;
        }


        String name =
                admin.getFullName();


        if (
                name == null
                        || name.isBlank()
        ) {

            name =
                    admin.getUsername();
        }


        if (
                name == null
                        || name.isBlank()
        ) {

            name =
                    "Administrator";
        }


        topNameLabel.setText(
                name
        );


        String staffId =
                admin.getEmployeeId();


        if (
                staffId != null
                        && !staffId.isBlank()
        ) {

            topRoleLabel.setText(
                    "ADMINISTRATOR  •  "
                            + staffId
            );

        } else {

            topRoleLabel.setText(
                    "ADMINISTRATOR"
            );
        }
    }


    // =========================================================
    // FILTER SETUP
    // =========================================================

    private void setupFilters() {

        filteredRows =
                new FilteredList<>(
                        allRows,
                        row -> true
                );


        reportTable.setItems(
                filteredRows
        );


        departmentFilterComboBox
                .getItems()
                .add(
                        "All Departments"
                );


        departmentFilterComboBox
                .getSelectionModel()
                .selectFirst();


        statusFilterComboBox
                .getItems()
                .setAll(
                        "All Status",
                        "At Risk",
                        "Normal",
                        "Good"
                );


        statusFilterComboBox
                .getSelectionModel()
                .selectFirst();


        searchField
                .textProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> applyFilters()
                );


        departmentFilterComboBox
                .valueProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> applyFilters()
                );


        statusFilterComboBox
                .valueProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> applyFilters()
                );


        startDatePicker
                .valueProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> applyFilters()
                );


        endDatePicker
                .valueProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> applyFilters()
                );
    }


    // =========================================================
    // TABLE SETUP
    // =========================================================

    private void setupTable() {

        employeeColumn.setCellValueFactory(
                cellData ->
                        new SimpleStringProperty(
                                cellData
                                        .getValue()
                                        .getEmployeeDisplay()
                        )
        );


        departmentColumn.setCellValueFactory(
                cellData ->
                        new SimpleStringProperty(
                                safe(
                                        cellData
                                                .getValue()
                                                .getDepartment()
                                )
                        )
        );


        periodColumn.setCellValueFactory(
                cellData ->
                        new SimpleStringProperty(
                                formatPeriod(
                                        cellData
                                                .getValue()
                                                .getResult()
                                                .getEvaluationStartDate(),

                                        cellData
                                                .getValue()
                                                .getResult()
                                                .getEvaluationEndDate()
                                )
                        )
        );


        productivityColumn.setCellValueFactory(
                cellData ->
                        new SimpleDoubleProperty(
                                cellData
                                        .getValue()
                                        .getResult()
                                        .getProductivityScore()
                        )
        );


        timelinessColumn.setCellValueFactory(
                cellData ->
                        new SimpleDoubleProperty(
                                cellData
                                        .getValue()
                                        .getResult()
                                        .getTimelinessScore()
                        )
        );


        completionColumn.setCellValueFactory(
                cellData ->
                        new SimpleDoubleProperty(
                                cellData
                                        .getValue()
                                        .getResult()
                                        .getCompletionRate()
                        )
        );


        attendanceColumn.setCellValueFactory(
                cellData ->
                        new SimpleDoubleProperty(
                                cellData
                                        .getValue()
                                        .getResult()
                                        .getAttendanceScore()
                        )
        );


        overallColumn.setCellValueFactory(
                cellData ->
                        new SimpleDoubleProperty(
                                cellData
                                        .getValue()
                                        .getResult()
                                        .getOverallKpiScore()
                        )
        );


        statusColumn.setCellValueFactory(
                cellData ->
                        new SimpleStringProperty(
                                safe(
                                        cellData
                                                .getValue()
                                                .getResult()
                                                .getPerformanceStatus()
                                )
                        )
        );


        setupPercentageColumn(
                productivityColumn
        );

        setupPercentageColumn(
                timelinessColumn
        );

        setupPercentageColumn(
                completionColumn
        );

        setupPercentageColumn(
                attendanceColumn
        );

        setupPercentageColumn(
                overallColumn
        );


        reportTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );
    }


    private void setupPercentageColumn(
            TableColumn<ReportRow, Number> column
    ) {

        column.setCellFactory(
                tableColumn ->
                        new TableCell<>() {

                            @Override
                            protected void updateItem(
                                    Number value,
                                    boolean empty
                            ) {

                                super.updateItem(
                                        value,
                                        empty
                                );


                                if (empty) {

                                    setText(
                                            null
                                    );

                                    return;
                                }


                                if (value == null) {

                                    setText(
                                            "-"
                                    );

                                    return;
                                }


                                setText(
                                        String.format(
                                                "%.2f%%",
                                                value.doubleValue()
                                        )
                                );
                            }
                        }
        );
    }


    // =========================================================
    // CLICK ROW -> POPUP
    // =========================================================

    private void setupRowOpening() {

        reportTable.setRowFactory(
                table -> {

                    TableRow<ReportRow> row =
                            new TableRow<>();


                    row.setOnMouseClicked(
                            event -> {

                                if (
                                        !row.isEmpty()
                                                && event.getClickCount() == 1
                                ) {

                                    openReportPopup(
                                            row.getItem()
                                    );
                                }
                            }
                    );


                    return row;
                }
        );
    }


    // =========================================================
    // OPEN REPORT DETAIL
    // =========================================================

    private void openReportPopup(
            ReportRow reportRow
    ) {

        if (reportRow == null) {
            return;
        }


        try {

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass()
                                    .getResource(
                                            "/fxml/admin/adminReportDetail.fxml"
                                    )
                    );


            Parent root =
                    loader.load();


            AdminReportDetailController controller =
                    loader.getController();


            Stage popupStage =
                    new Stage();


            controller.setReportData(
                    reportRow.getEmployee(),
                    reportRow.getResult(),
                    popupStage
            );


            Scene scene =
                    new Scene(
                            root
                    );


            URL css =
                    getClass()
                            .getResource(
                                    "/css/app.css"
                            );


            if (css != null) {

                scene
                        .getStylesheets()
                        .add(
                                css.toExternalForm()
                        );
            }


            popupStage.setScene(
                    scene
            );


            popupStage.setTitle(
                    "Performance Summary"
            );


            popupStage.initModality(
                    Modality.WINDOW_MODAL
            );


            popupStage.initOwner(
                    reportTable
                            .getScene()
                            .getWindow()
            );


            popupStage.setResizable(
                    false
            );


            popupStage.showAndWait();

        } catch (Exception e) {

            e.printStackTrace();


            showMessage(
                    "Unable to open performance summary."
            );
        }
    }


    // =========================================================
    // LOAD ALL EMPLOYEE REPORTS
    // =========================================================

    /** Call on the JavaFX thread to capture the current filter state. No UI changes. */
    public void exportPdf(java.nio.file.Path destination) throws java.io.IOException {
        exportFilteredReport(destination, true);
    }

    public void exportExcel(java.nio.file.Path destination) throws java.io.IOException {
        exportFilteredReport(destination, false);
    }

    private void exportFilteredReport(java.nio.file.Path destination, boolean pdf)
            throws java.io.IOException {
        if (!javafx.application.Platform.isFxApplicationThread()) {
            throw new IllegalStateException("Capture report filters on the JavaFX thread.");
        }
        var current = SessionManager.getCurrentUser();
        if (current == null || current.getRoleId() != 1) {
            throw new IllegalStateException("Sign in with a admin account to export reports.");
        }
        if (reportLoading || filteredRows == null) {
            throw new IllegalStateException("Wait for the report data to finish loading.");
        }
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();
        if (start != null && end != null && start.isAfter(end)) {
            throw new IllegalArgumentException("Start date must not be after end date.");
        }
        applyFilters();
        var rows = filteredRows.stream()
                .map(row -> com.aepm.logic.report.ReportExportService.record(
                        row.getEmployee(), row.getResult()))
                .toList();
        var metadata = List.of(
                "Admin report",
                "Employee search: " + java.util.Objects.toString(searchField.getText(), ""),
                "Department: " + departmentFilterComboBox.getValue(),
                "Performance: " + statusFilterComboBox.getValue(),
                "Period overlap: " + java.util.Objects.toString(start, "Any")
                        + " to " + java.util.Objects.toString(end, "Any"),
                "Generated: " + java.time.OffsetDateTime.now(),
                "Scope: current filtered records; active employees.");
        com.aepm.logic.report.ReportExportService.write(destination, rows, metadata, pdf);
    }

    private boolean reportLoading;

    private void loadReportData() {
        reportLoading = true;

        hideMessage();


        javafx.concurrent.Task<List<ReportRow>> task =
                new javafx.concurrent.Task<>() {

                    @Override
                    protected List<ReportRow> call()
                            throws Exception {

                        List<User> users =
                                userService
                                        .getAllUsers();


                        List<ReportRow> rows =
                                new ArrayList<>();


                        if (users == null) {

                            return rows;
                        }


                        for (User user : users) {

                            if (user == null) {
                                continue;
                            }


                            // EMPLOYEE ONLY
                            if (user.getRoleId() != 3) {
                                continue;
                            }


                            if (
                                    !"Active".equalsIgnoreCase(
                                            user.getAccountStatus()
                                    )
                            ) {

                                continue;
                            }


                            List<KpiResult> employeeResults =
                                    calculationService
                                            .getEmployeeResults(
                                                    user.getUserId()
                                            );


                            if (employeeResults == null) {
                                continue;
                            }


                            for (
                                    KpiResult result
                                    : employeeResults
                            ) {

                                rows.add(
                                        new ReportRow(
                                                user,
                                                result
                                        )
                                );
                            }
                        }


                        return rows;
                    }
                };


        task.setOnSucceeded(
                event -> {

                    reportLoading = false;
                    List<ReportRow> rows =
                            task.getValue();


                    allRows.clear();


                    if (rows != null) {

                        allRows.addAll(
                                rows
                        );
                    }


                    populateDepartmentFilter();

                    applyFilters();


                    if (allRows.isEmpty()) {

                        showMessage(
                                "No performance report data is available."
                        );

                    } else {

                        hideMessage();
                    }
                }
        );


        task.setOnFailed(
                event -> {

                    reportLoading = false;
                    Throwable error =
                            task.getException();
                    allRows.clear();


                    if (error != null) {

                        error.printStackTrace();
                    }


                    showMessage(
                            error == null
                                    ? "Unable to load performance reports."
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
    // DEPARTMENT FILTER
    // =========================================================

    private void populateDepartmentFilter() {

        Set<String> departments =
                new LinkedHashSet<>();


        for (ReportRow row : allRows) {

            String department =
                    row.getDepartment();


            if (
                    department != null
                            && !department.isBlank()
            ) {

                departments.add(
                        department
                );
            }
        }


        departmentFilterComboBox
                .getItems()
                .setAll(
                        "All Departments"
                );


        departmentFilterComboBox
                .getItems()
                .addAll(
                        departments
                );


        departmentFilterComboBox
                .getSelectionModel()
                .selectFirst();
    }

    private void applyFilters() {

        String keyword =
                searchField.getText() == null
                        ? ""
                        : searchField
                        .getText()
                        .trim()
                        .toLowerCase();


        String department =
                departmentFilterComboBox
                        .getValue();


        String status =
                statusFilterComboBox
                        .getValue();


        LocalDate startDate =
                startDatePicker
                        .getValue();


        LocalDate endDate =
                endDatePicker
                        .getValue();


        filteredRows.setPredicate(
                row -> {

                    if (row == null) {
                        return false;
                    }


                    User employee =
                            row.getEmployee();


                    KpiResult result =
                            row.getResult();


                    if (
                            employee == null
                                    || result == null
                    ) {

                        return false;
                    }


                    boolean matchesSearch =
                            keyword.isBlank()
                                    || contains(
                                    employee.getEmployeeId(),
                                    keyword
                            )
                                    || contains(
                                    employee.getFullName(),
                                    keyword
                            )
                                    || contains(
                                    employee.getUsername(),
                                    keyword
                            )
                                    || contains(
                                    employee.getDepartment(),
                                    keyword
                            );


                    boolean matchesDepartment =
                            department == null
                                    || department.equals(
                                    "All Departments"
                            )
                                    || department.equalsIgnoreCase(
                                    safe(
                                            employee.getDepartment()
                                    )
                            );


                    boolean matchesStatus =
                            status == null
                                    || status.equals(
                                    "All Status"
                            )
                                    || status.equalsIgnoreCase(
                                    result.getPerformanceStatus()
                            );


                    boolean matchesStartDate =
                            startDate == null
                                    || (
                                    result.getEvaluationEndDate() != null
                                            && !result
                                            .getEvaluationEndDate()
                                            .isBefore(
                                                    startDate
                                            )
                            );


                    boolean matchesEndDate =
                            endDate == null
                                    || (
                                    result.getEvaluationStartDate() != null
                                            && !result
                                            .getEvaluationStartDate()
                                            .isAfter(
                                                    endDate
                                            )
                            );


                    return matchesSearch
                            && matchesDepartment
                            && matchesStatus
                            && matchesStartDate
                            && matchesEndDate;
                }
        );


        updateStatistics();


        if (
                filteredRows.isEmpty()
                        && !allRows.isEmpty()
        ) {

            showMessage(
                    "No report records match the selected filters."
            );

        } else {

            hideMessage();
        }
    }

    // =========================================================
    // UPDATE STATISTICS
    // =========================================================

    private void updateStatistics() {

        if (
                filteredRows == null
                        || filteredRows.isEmpty()
        ) {

            totalRecordsLabel.setText(
                    "0"
            );

            averageKpiLabel.setText(
                    "0.00%"
            );

            atRiskCountLabel.setText(
                    "0"
            );

            normalGoodCountLabel.setText(
                    "0"
            );

            return;
        }


        int totalRecords =
                0;


        double totalKpi =
                0.0;


        int atRiskCount =
                0;


        int normalGoodCount =
                0;


        for (
                ReportRow row
                : filteredRows
        ) {

            if (
                    row == null
                            || row.getResult() == null
            ) {

                continue;
            }


            KpiResult result =
                    row.getResult();


            totalRecords++;


            totalKpi +=
                    result.getOverallKpiScore();


            String performanceStatus =
                    result.getPerformanceStatus();


            if (
                    "At Risk".equalsIgnoreCase(
                            performanceStatus
                    )
            ) {

                atRiskCount++;

            } else if (
                    "Normal".equalsIgnoreCase(
                            performanceStatus
                    )
                            || "Good".equalsIgnoreCase(
                            performanceStatus
                    )
            ) {

                normalGoodCount++;
            }
        }


        double averageKpi =
                totalRecords == 0
                        ? 0.0
                        : totalKpi / totalRecords;


        totalRecordsLabel.setText(
                String.valueOf(
                        totalRecords
                )
        );


        averageKpiLabel.setText(
                String.format(
                        "%.2f%%",
                        averageKpi
                )
        );


        atRiskCountLabel.setText(
                String.valueOf(
                        atRiskCount
                )
        );


        normalGoodCountLabel.setText(
                String.valueOf(
                        normalGoodCount
                )
        );
    }


    // =========================================================
    // CLEAR FILTERS
    // =========================================================

    @FXML
    private void handleClearFilters() {

        searchField.clear();


        departmentFilterComboBox
                .getSelectionModel()
                .select(
                        "All Departments"
                );


        statusFilterComboBox
                .getSelectionModel()
                .select(
                        "All Status"
                );


        startDatePicker.setValue(
                null
        );


        endDatePicker.setValue(
                null
        );


        applyFilters();
    }


    // =========================================================
    // REFRESH
    // =========================================================

    @FXML
    private void handleRefresh() {

        loadReportData();
    }


    // =========================================================
    // NAVIGATION
    // =========================================================

    @FXML
    private void handleHome(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/admin/adminHome.fxml"
        );
    }


    @FXML
    private void handleProfile(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/admin/adminProfile.fxml"
        );
    }


    @FXML
    private void handleUserManagement(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/admin/adminUserManagement.fxml"
        );
    }


    @FXML
    private void handleTaskManagement(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/admin/adminTask.fxml"
        );
    }


      @FXML
        private void handleKpiManagement(
                ActionEvent event
        ) {

        navigate(
                event,
                "/fxml/admin/adminKpi.fxml"
        );
        }

    @FXML
    private void handleReports() {

        // Current page
    }


    @FXML
    private void handleAnonymousReports(javafx.event.ActionEvent event) {
        navigate(event, "/fxml/admin/adminAnonymousReports.fxml");
    }


    @FXML
    private void handleLogout(
            ActionEvent event
    ) {

        SessionManager.clearSession();


        navigate(
                event,
                "/fxml/auth/login.fxml"
        );
    }


    // =========================================================
    // HELPERS
    // =========================================================

    private boolean contains(
            String value,
            String keyword
    ) {

        return value != null
                && value
                .toLowerCase()
                .contains(
                        keyword
                );
    }


    private String formatPeriod(
            LocalDate start,
            LocalDate end
    ) {

        if (
                start == null
                        || end == null
        ) {

            return "-";
        }


        return start.format(
                DATE_FORMATTER
        )
                + " - "
                + end.format(
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


    private void navigate(
            ActionEvent event,
            String path
    ) {

        Stage stage =
                (Stage) ((Node) event.getSource())
                        .getScene()
                        .getWindow();


        NavigationManager.navigate(
                stage,
                path
        );
    }


    // =========================================================
    // REPORT ROW
    // =========================================================

    public static class ReportRow {

        private final User employee;

        private final KpiResult result;


        public ReportRow(
                User employee,
                KpiResult result
        ) {

            this.employee =
                    employee;

            this.result =
                    result;
        }


        public User getEmployee() {

            return employee;
        }


        public KpiResult getResult() {

            return result;
        }


        public String getDepartment() {

            if (employee == null) {

                return "-";
            }


            return employee.getDepartment();
        }


        public String getEmployeeDisplay() {

            if (employee == null) {

                return "-";
            }


            String employeeId =
                    employee.getEmployeeId();


            if (
                    employeeId == null
                            || employeeId.isBlank()
            ) {

                employeeId =
                        "-";
            }


            String name =
                    employee.getFullName();


            if (
                    name == null
                            || name.isBlank()
            ) {

                name =
                        employee.getUsername();
            }


            if (
                    name == null
                            || name.isBlank()
            ) {

                name =
                        "Employee";
            }


            return employeeId
                    + " - "
                    + name;
        }
    }
}