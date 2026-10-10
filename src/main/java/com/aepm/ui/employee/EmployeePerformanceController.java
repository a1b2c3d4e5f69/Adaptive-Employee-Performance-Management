package com.aepm.ui.employee;

import com.aepm.logic.kpi.KpiCalculationService;
import com.aepm.logic.kpi.PerformanceFeedbackService;

import com.aepm.model.KpiResult;
import com.aepm.model.PerformanceFeedback;
import com.aepm.model.User;

import com.aepm.ui.navigation.NavigationManager;
import com.aepm.util.SessionManager;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javafx.event.ActionEvent;

import javafx.fxml.FXML;

import javafx.scene.Node;

import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import java.util.List;

public class EmployeePerformanceController {

    // =========================================================
    // TOP BAR
    // =========================================================

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label topNameLabel;

    @FXML
    private Label topRoleLabel;


    // =========================================================
    // SUMMARY
    // =========================================================

    @FXML
    private Label overallKpiLabel;

    @FXML
    private Label performanceStatusLabel;

    @FXML
    private Label evaluationPeriodLabel;

    @FXML
    private Label calculatedDateLabel;


    // =========================================================
    // KPI BREAKDOWN
    // =========================================================

    @FXML
    private Label productivityLabel;

    @FXML
    private Label timelinessLabel;

    @FXML
    private Label completionRateLabel;

    @FXML
    private Label attendanceLabel;


    // =========================================================
    // KPI PROGRESS BARS
    // =========================================================

    @FXML
    private ProgressBar productivityProgressBar;

    @FXML
    private ProgressBar timelinessProgressBar;

    @FXML
    private ProgressBar completionProgressBar;

    @FXML
    private ProgressBar attendanceProgressBar;


    // =========================================================
    // HISTORY
    // =========================================================

    @FXML
    private TableView<KpiResult> historyTable;

    @FXML
    private TableColumn<KpiResult, String> periodColumn;

    @FXML
    private TableColumn<KpiResult, Number> productivityColumn;

    @FXML
    private TableColumn<KpiResult, Number> timelinessColumn;

    @FXML
    private TableColumn<KpiResult, Number> completionColumn;

    @FXML
    private TableColumn<KpiResult, Number> attendanceColumn;

    @FXML
    private TableColumn<KpiResult, Number> overallColumn;

    @FXML
    private TableColumn<KpiResult, String> statusColumn;

    @FXML
    private TableColumn<KpiResult, String> calculatedColumn;


    // =========================================================
    // FEEDBACK
    // =========================================================

    @FXML
    private Label feedbackLabel;

    @FXML
    private Label feedbackDateLabel;

    @FXML
    private Label feedbackPeriodLabel;


    // =========================================================
    // MESSAGE
    // =========================================================

    @FXML
    private Label messageLabel;


    // =========================================================
    // SERVICES
    // =========================================================

    private final KpiCalculationService calculationService =
            new KpiCalculationService();

    private final PerformanceFeedbackService feedbackService =
            new PerformanceFeedbackService();


    // =========================================================
    // DATA
    // =========================================================

    private final ObservableList<KpiResult> results =
            FXCollections.observableArrayList();


    private KpiResult selectedResult;


    private static final ZoneId MALAYSIA_ZONE =
            ZoneId.of(
                    "Asia/Kuala_Lumpur"
            );


    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern(
                    "dd MMM yyyy"
            );


    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern(
                    "dd MMM yyyy HH:mm"
            );


    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {

        loadCurrentUser();

        setupHistoryTable();

        clearPerformance();

        clearFeedback();

        hideMessage();

        loadPerformanceResults();
    }


    // =========================================================
    // CURRENT USER
    // =========================================================

    private void loadCurrentUser() {

        User user =
                SessionManager.getCurrentUser();


        if (user == null) {

            topNameLabel.setText(
                    "Employee"
            );

            topRoleLabel.setText(
                    "EMPLOYEE"
            );

            return;
        }


        String name =
                user.getFullName();


        if (
                name == null
                        || name.isBlank()
        ) {

            name =
                    user.getUsername();
        }


        if (
                name == null
                        || name.isBlank()
        ) {

            name =
                    "Employee";
        }


        topNameLabel.setText(
                name
        );


        String employeeId =
                user.getEmployeeId();


        if (
                employeeId != null
                        && !employeeId.isBlank()
        ) {

            topRoleLabel.setText(
                    "EMPLOYEE  •  "
                            + employeeId
            );

        } else {

            topRoleLabel.setText(
                    "EMPLOYEE"
            );
        }


        if (welcomeLabel != null) {

            welcomeLabel.setText(
                    "Welcome, " + name
            );
        }
    }


    // =========================================================
    // HISTORY TABLE
    // =========================================================

    private void setupHistoryTable() {

        periodColumn.setCellValueFactory(
                cellData ->
                        new SimpleStringProperty(
                                formatPeriod(
                                        cellData.getValue()
                                                .getEvaluationStartDate(),

                                        cellData.getValue()
                                                .getEvaluationEndDate()
                                )
                        )
        );


        productivityColumn.setCellValueFactory(
                cellData ->
                        new SimpleDoubleProperty(
                                cellData.getValue()
                                        .getProductivityScore()
                        )
        );


        timelinessColumn.setCellValueFactory(
                cellData ->
                        new SimpleDoubleProperty(
                                cellData.getValue()
                                        .getTimelinessScore()
                        )
        );


        completionColumn.setCellValueFactory(
                cellData ->
                        new SimpleDoubleProperty(
                                cellData.getValue()
                                        .getCompletionRate()
                        )
        );


        attendanceColumn.setCellValueFactory(
                cellData ->
                        new SimpleDoubleProperty(
                                cellData.getValue()
                                        .getAttendanceScore()
                        )
        );


        overallColumn.setCellValueFactory(
                cellData ->
                        new SimpleDoubleProperty(
                                cellData.getValue()
                                        .getOverallKpiScore()
                        )
        );


        statusColumn.setCellValueFactory(
                cellData ->
                        new SimpleStringProperty(
                                safe(
                                        cellData.getValue()
                                                .getPerformanceStatus()
                                )
                        )
        );


        calculatedColumn.setCellValueFactory(
                cellData -> {

                    OffsetDateTime calculatedAt =
                            cellData
                                    .getValue()
                                    .getCalculatedAt();


                    return new SimpleStringProperty(
                            calculatedAt == null
                                    ? "-"
                                    : formatDateTime(
                                            calculatedAt
                                    )
                    );
                }
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


        historyTable.setItems(
                results
        );


        historyTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );


        // =====================================================
        // SELECT HISTORY RESULT
        // =====================================================

        historyTable
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> {

                            if (newValue == null) {
                                return;
                            }


                            selectedResult =
                                    newValue;


                            displayPerformance(
                                    newValue
                            );


                            loadManagerFeedback(
                                    newValue
                            );
                        }
                );
    }


    // =========================================================
    // PERCENTAGE COLUMN
    // =========================================================

    private void setupPercentageColumn(
            TableColumn<KpiResult, Number> column
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


                                if (
                                        empty
                                                || value == null
                                ) {

                                    setText(
                                            "-"
                                    );

                                } else {

                                    setText(
                                            String.format(
                                                    "%.2f%%",
                                                    value.doubleValue()
                                            )
                                    );
                                }
                            }
                        }
        );
    }


    // =========================================================
    // LOAD PERFORMANCE
    // =========================================================

    @FXML
    private void loadPerformanceResults() {

        hideMessage();


        User currentUser =
                SessionManager.getCurrentUser();


        if (
                currentUser == null
                        || currentUser.getUserId() == null
                        || currentUser.getUserId().isBlank()
        ) {

            clearPerformance();

            clearFeedback();


            showMessage(
                    "Unable to identify the current employee."
            );


            return;
        }


        String employeeUserId =
                currentUser.getUserId();


        javafx.concurrent.Task<List<KpiResult>> task =
                new javafx.concurrent.Task<>() {

                    @Override
                    protected List<KpiResult> call()
                            throws Exception {

                        return calculationService
                                .getEmployeeResults(
                                        employeeUserId
                                );
                    }
                };


        task.setOnSucceeded(
                event -> {

                    List<KpiResult> loadedResults =
                            task.getValue();


                    results.clear();


                    if (
                            loadedResults == null
                                    || loadedResults.isEmpty()
                    ) {

                        clearPerformance();

                        clearFeedback();


                        showMessage(
                                "No performance data is currently available."
                        );


                        return;
                    }


                    results.addAll(
                            loadedResults
                    );


                    historyTable.refresh();


                    // First result = latest result
                    KpiResult latest =
                            loadedResults.get(0);


                    selectedResult =
                            latest;


                    displayPerformance(
                            latest
                    );


                    /*
                     * Selecting it will trigger the listener,
                     * which also loads Manager Feedback.
                     */
                    historyTable
                            .getSelectionModel()
                            .select(
                                    latest
                            );


                    hideMessage();
                }
        );


        task.setOnFailed(
                event -> {

                    Throwable error =
                            task.getException();


                    if (error != null) {
                        error.printStackTrace();
                    }


                    clearPerformance();

                    clearFeedback();


                    showMessage(
                            error == null
                                    ? "Unable to load performance results."
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
    // LOAD MANAGER FEEDBACK
    // =========================================================

    private void loadManagerFeedback(
            KpiResult result
    ) {

        clearFeedback();


        if (
                result == null
                        || result.getResultId() == null
        ) {

            feedbackLabel.setText(
                    "No performance feedback is currently available."
            );

            return;
        }


        feedbackPeriodLabel.setText(
                "Evaluation: "
                        + formatPeriod(
                        result.getEvaluationStartDate(),
                        result.getEvaluationEndDate()
                )
        );


        javafx.concurrent.Task<PerformanceFeedback> task =
                new javafx.concurrent.Task<>() {

                    @Override
                    protected PerformanceFeedback call()
                            throws Exception {

                        return feedbackService
                                .getLatestFeedbackByResult(
                                        result.getResultId()
                                );
                    }
                };


        task.setOnSucceeded(
                event -> {

                    PerformanceFeedback feedback =
                            task.getValue();


                    if (feedback == null) {

                        feedbackLabel.setText(
                                "No performance feedback has been submitted for this evaluation."
                        );


                        feedbackDateLabel.setText(
                                ""
                        );


                        return;
                    }


                    String text =
                            feedback.getFeedbackText();


                    if (
                            text == null
                                    || text.isBlank()
                    ) {

                        text =
                                "No feedback provided.";
                    }


                    feedbackLabel.setText(
                            text
                    );


                    if (
                            feedback.getCreatedAt() != null
                    ) {

                        feedbackDateLabel.setText(
                                "Submitted: "
                                        + formatDateTime(
                                        feedback.getCreatedAt()
                                )
                        );

                    } else {

                        feedbackDateLabel.setText(
                                ""
                        );
                    }
                }
        );


        task.setOnFailed(
                event -> {

                    Throwable error =
                            task.getException();


                    if (error != null) {
                        error.printStackTrace();
                    }


                    feedbackLabel.setText(
                            "Unable to load manager feedback."
                    );


                    feedbackDateLabel.setText(
                            ""
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
    // DISPLAY PERFORMANCE
    // =========================================================

    private void displayPerformance(
            KpiResult result
    ) {

        if (result == null) {

            clearPerformance();

            return;
        }


        overallKpiLabel.setText(
                String.format(
                        "%.2f%%",
                        result.getOverallKpiScore()
                )
        );


        performanceStatusLabel.setText(
                safe(
                        result.getPerformanceStatus()
                )
        );


        evaluationPeriodLabel.setText(
                formatPeriod(
                        result.getEvaluationStartDate(),
                        result.getEvaluationEndDate()
                )
        );


        calculatedDateLabel.setText(
                result.getCalculatedAt() == null
                        ? "-"
                        : formatDateTime(
                                result.getCalculatedAt()
                        )
        );


        productivityLabel.setText(
                String.format(
                        "%.2f%%",
                        result.getProductivityScore()
                )
        );


        timelinessLabel.setText(
                String.format(
                        "%.2f%%",
                        result.getTimelinessScore()
                )
        );


        completionRateLabel.setText(
                String.format(
                        "%.2f%%",
                        result.getCompletionRate()
                )
        );


        attendanceLabel.setText(
                String.format(
                        "%.2f%%",
                        result.getAttendanceScore()
                )
        );


        productivityProgressBar.setProgress(
                toProgress(
                        result.getProductivityScore()
                )
        );


        timelinessProgressBar.setProgress(
                toProgress(
                        result.getTimelinessScore()
                )
        );


        completionProgressBar.setProgress(
                toProgress(
                        result.getCompletionRate()
                )
        );


        attendanceProgressBar.setProgress(
                toProgress(
                        result.getAttendanceScore()
                )
        );


        updatePerformanceStatusStyle(
                result.getPerformanceStatus()
        );
    }


    // =========================================================
    // STATUS STYLE
    // =========================================================

    private void updatePerformanceStatusStyle(
            String status
    ) {

        performanceStatusLabel
                .getStyleClass()
                .removeAll(
                        "status-at-risk",
                        "status-normal",
                        "status-good"
                );


        if (
                "At Risk".equalsIgnoreCase(
                        status
                )
        ) {

            performanceStatusLabel
                    .getStyleClass()
                    .add(
                            "status-at-risk"
                    );

        } else if (
                "Normal".equalsIgnoreCase(
                        status
                )
        ) {

            performanceStatusLabel
                    .getStyleClass()
                    .add(
                            "status-normal"
                    );

        } else {

            performanceStatusLabel
                    .getStyleClass()
                    .add(
                            "status-good"
                    );
        }
    }


    // =========================================================
    // CLEAR PERFORMANCE
    // =========================================================

    private void clearPerformance() {

        selectedResult =
                null;


        overallKpiLabel.setText(
                "-"
        );

        performanceStatusLabel.setText(
                "-"
        );

        evaluationPeriodLabel.setText(
                "-"
        );

        calculatedDateLabel.setText(
                "-"
        );


        productivityLabel.setText(
                "-"
        );

        timelinessLabel.setText(
                "-"
        );

        completionRateLabel.setText(
                "-"
        );

        attendanceLabel.setText(
                "-"
        );


        productivityProgressBar.setProgress(
                0
        );

        timelinessProgressBar.setProgress(
                0
        );

        completionProgressBar.setProgress(
                0
        );

        attendanceProgressBar.setProgress(
                0
        );
    }


    // =========================================================
    // CLEAR FEEDBACK
    // =========================================================

    private void clearFeedback() {

        feedbackLabel.setText(
                "No performance feedback is currently available."
        );


        feedbackDateLabel.setText(
                ""
        );


        feedbackPeriodLabel.setText(
                ""
        );
    }


    // =========================================================
    // REFRESH
    // =========================================================

    @FXML
    private void handleRefresh() {

        loadPerformanceResults();
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
                "/fxml/employee/employeeHome.fxml"
        );
    }


    @FXML
    private void handleProfile(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/employee/employeeProfile.fxml"
        );
    }


    @FXML
    private void handleTasks(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/employee/employeeTask.fxml"
        );
    }


    @FXML
    private void handleKpi() {

        // Current page
    }


    @FXML
    private void handleReports() {

        System.out.println(
                "Employee Reports not implemented yet."
        );
    }


    @FXML
    private void handleAnonymousReport() {

        System.out.println(
                "Anonymous Reporting not implemented yet."
        );
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

    private double toProgress(
            double percentage
    ) {

        return Math.max(
                0,
                Math.min(
                        1,
                        percentage / 100.0
                )
        );
    }


    private String formatPeriod(
            LocalDate startDate,
            LocalDate endDate
    ) {

        if (
                startDate == null
                        || endDate == null
        ) {

            return "-";
        }


        return startDate.format(
                DATE_FORMATTER
        )
                + " - "
                + endDate.format(
                DATE_FORMATTER
        );
    }


    private String formatDateTime(
            OffsetDateTime dateTime
    ) {

        if (dateTime == null) {
            return "-";
        }


        return dateTime
                .atZoneSameInstant(
                        MALAYSIA_ZONE
                )
                .format(
                        DATE_TIME_FORMATTER
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
            String message
    ) {

        messageLabel.setText(
                message
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
}