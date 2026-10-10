package com.aepm.ui.manager;

import com.aepm.logic.kpi.KpiCalculationService;
import com.aepm.logic.kpi.PerformanceFeedbackService;
import com.aepm.logic.user.UserService;

import com.aepm.model.KpiResult;
import com.aepm.model.PerformanceFeedback;
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

import javafx.scene.Node;

import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import javafx.stage.Stage;

import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ManagerPerformanceController {

    // =========================================================
    // TOP BAR
    // =========================================================

    @FXML
    private Label topNameLabel;

    @FXML
    private Label topRoleLabel;


    // =========================================================
    // EMPLOYEE SEARCH / FILTER
    // =========================================================

    @FXML
    private TextField employeeSearchField;

    @FXML
    private ComboBox<String> departmentFilterComboBox;

    @FXML
    private ComboBox<User> employeeComboBox;

    @FXML
    private Label employeeInfoLabel;


    // =========================================================
    // PERFORMANCE FILTER
    // =========================================================

    @FXML
    private ComboBox<String> statusFilterComboBox;


    // =========================================================
    // CURRENT PERFORMANCE
    // =========================================================

    @FXML
    private Label overallKpiLabel;

    @FXML
    private Label performanceStatusLabel;

    @FXML
    private Label evaluationPeriodLabel;

    @FXML
    private Label calculatedAtLabel;


    // =========================================================
    // BREAKDOWN
    // =========================================================

    @FXML
    private Label productivityLabel;

    @FXML
    private Label timelinessLabel;

    @FXML
    private Label completionLabel;

    @FXML
    private Label attendanceLabel;


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
    private Label selectedResultLabel;

    @FXML
    private Label existingFeedbackLabel;

    @FXML
    private TextArea feedbackTextArea;

    @FXML
    private Button submitFeedbackButton;

    @FXML
    private Label messageLabel;


    // =========================================================
    // SERVICES
    // =========================================================

    private final UserService userService =
            new UserService();

    private final KpiCalculationService calculationService =
            new KpiCalculationService();

    private final PerformanceFeedbackService feedbackService =
            new PerformanceFeedbackService();


    // =========================================================
    // DATA
    // =========================================================

    private final ObservableList<User> allEmployees =
            FXCollections.observableArrayList();

    private FilteredList<User> filteredEmployees;


    private final ObservableList<KpiResult> allResults =
            FXCollections.observableArrayList();

    private FilteredList<KpiResult> filteredResults;


    private User selectedEmployee;

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

        loadCurrentManager();

        setupEmployeeFilters();

        setupEmployeeComboBox();

        setupPerformanceFilter();

        setupHistoryTable();

        clearPerformance();

        clearFeedback();

        hideMessage();

        loadEmployees();
    }


    // =========================================================
    // CURRENT MANAGER
    // =========================================================

    private void loadCurrentManager() {

        User manager =
                SessionManager.getCurrentUser();


        if (manager == null) {

            topNameLabel.setText(
                    "Manager"
            );

            topRoleLabel.setText(
                    "MANAGER"
            );

            return;
        }


        String name =
                manager.getFullName();


        if (
                name == null
                        || name.isBlank()
        ) {

            name =
                    manager.getUsername();
        }


        if (
                name == null
                        || name.isBlank()
        ) {

            name =
                    "Manager";
        }


        topNameLabel.setText(
                name
        );


        String staffId =
                manager.getEmployeeId();


        if (
                staffId != null
                        && !staffId.isBlank()
        ) {

            topRoleLabel.setText(
                    "MANAGER  •  "
                            + staffId
            );

        } else {

            topRoleLabel.setText(
                    "MANAGER"
            );
        }
    }


    // =========================================================
    // EMPLOYEE FILTER SETUP
    // =========================================================

    private void setupEmployeeFilters() {

        filteredEmployees =
                new FilteredList<>(
                        allEmployees,
                        user -> true
                );


        departmentFilterComboBox
                .getItems()
                .add(
                        "All Departments"
                );


        departmentFilterComboBox
                .getSelectionModel()
                .selectFirst();


        employeeSearchField
                .textProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> applyEmployeeFilters()
                );


        departmentFilterComboBox
                .valueProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> applyEmployeeFilters()
                );
    }


    // =========================================================
    // APPLY EMPLOYEE FILTER
    // =========================================================

    private void applyEmployeeFilters() {

        String keyword =
                employeeSearchField.getText() == null
                        ? ""
                        : employeeSearchField
                        .getText()
                        .trim()
                        .toLowerCase();


        String department =
                departmentFilterComboBox
                        .getValue();


        filteredEmployees.setPredicate(
                employee -> {

                    if (employee == null) {
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


                    return matchesSearch
                            && matchesDepartment;
                }
        );


        employeeComboBox.setItems(
                filteredEmployees
        );


        employeeInfoLabel.setText(
                filteredEmployees.size()
                        + " employee(s) found."
        );


        if (
                selectedEmployee != null
                        && !filteredEmployees.contains(
                        selectedEmployee
                )
        ) {

            selectedEmployee =
                    null;

            employeeComboBox
                    .getSelectionModel()
                    .clearSelection();

            clearPerformance();

            clearFeedback();
        }
    }


    // =========================================================
    // CLEAR EMPLOYEE FILTER
    // =========================================================

    @FXML
    private void handleClearEmployeeFilters() {

        employeeSearchField.clear();


        departmentFilterComboBox
                .getSelectionModel()
                .select(
                        "All Departments"
                );


        applyEmployeeFilters();
    }


    // =========================================================
    // EMPLOYEE COMBO BOX
    // =========================================================

    private void setupEmployeeComboBox() {

        employeeComboBox.setItems(
                filteredEmployees
        );


        employeeComboBox.setConverter(
                new StringConverter<>() {

                    @Override
                    public String toString(
                            User user
                    ) {

                        if (user == null) {
                            return "";
                        }


                        String employeeId =
                                safe(
                                        user.getEmployeeId()
                                );


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


                        return employeeId
                                + " - "
                                + name;
                    }


                    @Override
                    public User fromString(
                            String string
                    ) {

                        return null;
                    }
                }
        );


        employeeComboBox
                .valueProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> {

                            if (newValue != null) {

                                selectedEmployee =
                                        newValue;


                                loadEmployeePerformance(
                                        newValue
                                );
                            }
                        }
                );
    }


    // =========================================================
    // PERFORMANCE STATUS FILTER
    // =========================================================

    private void setupPerformanceFilter() {

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


        filteredResults =
                new FilteredList<>(
                        allResults,
                        result -> true
                );


        historyTable.setItems(
                filteredResults
        );


        statusFilterComboBox
                .valueProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> applyPerformanceFilter()
                );
    }


    // =========================================================
    // APPLY PERFORMANCE FILTER
    // =========================================================

    private void applyPerformanceFilter() {

        String selectedStatus =
                statusFilterComboBox
                        .getValue();


        filteredResults.setPredicate(
                result -> {

                    if (result == null) {
                        return false;
                    }


                    if (
                            selectedStatus == null
                                    || selectedStatus.equals(
                                    "All Status"
                            )
                    ) {

                        return true;
                    }


                    return selectedStatus
                            .equalsIgnoreCase(
                                    result
                                            .getPerformanceStatus()
                            );
                }
        );
    }


    // =========================================================
    // CLEAR PERFORMANCE FILTER
    // =========================================================

    @FXML
    private void handleClearPerformanceFilter() {

        statusFilterComboBox
                .getSelectionModel()
                .select(
                        "All Status"
                );


        applyPerformanceFilter();
    }


    // =========================================================
    // LOAD EMPLOYEES
    // =========================================================

    private void loadEmployees() {

        hideMessage();


        User manager =
                SessionManager.getCurrentUser();


        if (
                manager == null
                        || manager.getUserId() == null
                        || manager.getUserId().isBlank()
        ) {

            showMessage(
                    "Unable to identify the current manager."
            );

            return;
        }


        String managerUserId =
                manager.getUserId();


        javafx.concurrent.Task<List<User>> task =
                new javafx.concurrent.Task<>() {

                    @Override
                    protected List<User> call()
                            throws Exception {

                        return userService
                                .getAllUsers();
                    }
                };


        task.setOnSucceeded(
                event -> {

                    List<User> users =
                            task.getValue();


                    allEmployees.clear();


                    Set<String> departments =
                            new LinkedHashSet<>();


                    if (users != null) {

                        for (User user : users) {

                            if (
                                    user == null
                                            || user.getRoleId() != 3
                            ) {

                                continue;
                            }


                            if (
                                    !"Active".equalsIgnoreCase(
                                            user.getAccountStatus()
                                    )
                            ) {

                                continue;
                            }


                            String employeeManagerId =
                                    String.valueOf(
                                            user.getManagerId()
                                    );


                            if (
                                    !managerUserId.equals(
                                            employeeManagerId
                                    )
                            ) {

                                continue;
                            }


                            allEmployees.add(
                                    user
                            );


                            if (
                                    user.getDepartment() != null
                                            && !user.getDepartment()
                                            .isBlank()
                            ) {

                                departments.add(
                                        user.getDepartment()
                                );
                            }
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


                    applyEmployeeFilters();


                    if (allEmployees.isEmpty()) {

                        employeeInfoLabel.setText(
                                "No employees are currently assigned to you."
                        );


                        clearPerformance();

                        clearFeedback();

                    } else {

                        employeeComboBox
                                .getSelectionModel()
                                .selectFirst();
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


                    showMessage(
                            error == null
                                    ? "Unable to load employees."
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


        historyTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );


        historyTable
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> {

                            if (newValue != null) {

                                selectedResult =
                                        newValue;


                                displayPerformance(
                                        newValue
                                );


                                loadFeedback(
                                        newValue
                                );
                            }
                        }
                );
    }


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


                                setText(
                                        empty || value == null
                                                ? "-"
                                                : String.format(
                                                        "%.2f%%",
                                                        value.doubleValue()
                                                )
                                );
                            }
                        }
        );
    }


    // =========================================================
    // LOAD PERFORMANCE
    // =========================================================

    private void loadEmployeePerformance(
            User employee
    ) {

        clearPerformance();

        clearFeedback();

        hideMessage();


        if (
                employee == null
                        || employee.getUserId() == null
                        || employee.getUserId().isBlank()
        ) {

            return;
        }


        String employeeUserId =
                employee.getUserId();


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


                    allResults.clear();


                    if (
                            loadedResults == null
                                    || loadedResults.isEmpty()
                    ) {

                        showMessage(
                                "Insufficient data is available to evaluate this employee's performance."
                        );

                        return;
                    }


                    allResults.addAll(
                            loadedResults
                    );


                    applyPerformanceFilter();


                    KpiResult latest =
                            loadedResults.get(0);


                    selectedResult =
                            latest;


                    displayPerformance(
                            latest
                    );


                    if (
                            filteredResults.contains(
                                    latest
                            )
                    ) {

                        historyTable
                                .getSelectionModel()
                                .select(
                                        latest
                                );
                    }


                    loadFeedback(
                            latest
                    );
                }
        );


        task.setOnFailed(
                event -> {

                    Throwable error =
                            task.getException();


                    if (error != null) {
                        error.printStackTrace();
                    }


                    showMessage(
                            error == null
                                    ? "Unable to load employee performance."
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


        calculatedAtLabel.setText(
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


        completionLabel.setText(
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


        updateStatusStyle(
                result.getPerformanceStatus()
        );


        selectedResultLabel.setText(
                "Selected evaluation: "
                        + formatPeriod(
                        result.getEvaluationStartDate(),
                        result.getEvaluationEndDate()
                )
        );
    }


    // =========================================================
    // LOAD FEEDBACK
    // =========================================================

    private void loadFeedback(
            KpiResult result
    ) {

        clearFeedback();


        if (
                result == null
                        || result.getResultId() == null
        ) {

            return;
        }


        selectedResultLabel.setText(
                "Selected evaluation: "
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

                        existingFeedbackLabel.setText(
                                "No feedback has been submitted for this evaluation."
                        );


                        feedbackTextArea.setDisable(
                                false
                        );


                        submitFeedbackButton.setDisable(
                                false
                        );

                        return;
                    }


                    String date =
                            feedback.getCreatedAt() == null
                                    ? ""
                                    : " • "
                                    + formatDateTime(
                                    feedback.getCreatedAt()
                            );


                    existingFeedbackLabel.setText(
                            feedback.getFeedbackText()
                                    + date
                    );


                    feedbackTextArea.setText(
                            feedback.getFeedbackText()
                    );


                    feedbackTextArea.setDisable(
                            true
                    );


                    submitFeedbackButton.setDisable(
                            true
                    );
                }
        );


        task.setOnFailed(
                event -> {

                    Throwable error =
                            task.getException();


                    if (error != null) {
                        error.printStackTrace();
                    }


                    existingFeedbackLabel.setText(
                            "Unable to load feedback."
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
    // SUBMIT FEEDBACK
    // =========================================================

    @FXML
    private void handleSubmitFeedback() {

        if (selectedEmployee == null) {

            showMessage(
                    "Please select an employee."
            );

            return;
        }


        if (selectedResult == null) {

            showMessage(
                    "Please select a performance result."
            );

            return;
        }


        String feedbackText =
                feedbackTextArea
                        .getText()
                        .trim();


        if (feedbackText.isBlank()) {

            showMessage(
                    "Please enter performance feedback before submitting."
            );

            return;
        }


        submitFeedbackButton.setDisable(
                true
        );


        javafx.concurrent.Task<PerformanceFeedback> task =
                new javafx.concurrent.Task<>() {

                    @Override
                    protected PerformanceFeedback call()
                            throws Exception {

                        return feedbackService
                                .submitFeedback(
                                        selectedResult.getResultId(),
                                        selectedEmployee.getUserId(),
                                        feedbackText
                                );
                    }
                };


        task.setOnSucceeded(
                event -> {

                    PerformanceFeedback feedback =
                            task.getValue();


                    String date =
                            feedback.getCreatedAt() == null
                                    ? ""
                                    : " • "
                                    + formatDateTime(
                                    feedback.getCreatedAt()
                            );


                    existingFeedbackLabel.setText(
                            feedback.getFeedbackText()
                                    + date
                    );


                    feedbackTextArea.setText(
                            feedback.getFeedbackText()
                    );


                    feedbackTextArea.setDisable(
                            true
                    );


                    submitFeedbackButton.setDisable(
                            true
                    );


                    showMessage(
                            "Performance feedback submitted successfully."
                    );
                }
        );


        task.setOnFailed(
                event -> {

                    submitFeedbackButton.setDisable(
                            false
                    );


                    Throwable error =
                            task.getException();


                    if (error != null) {
                        error.printStackTrace();
                    }


                    showMessage(
                            error == null
                                    ? "Unable to submit performance feedback."
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
    // REFRESH
    // =========================================================

    @FXML
    private void handleRefresh() {

        loadEmployees();
    }


    // =========================================================
    // CLEAR
    // =========================================================

    private void clearPerformance() {

        allResults.clear();


        overallKpiLabel.setText("-");
        performanceStatusLabel.setText("-");
        evaluationPeriodLabel.setText("-");
        calculatedAtLabel.setText("-");

        productivityLabel.setText("-");
        timelinessLabel.setText("-");
        completionLabel.setText("-");
        attendanceLabel.setText("-");

        productivityProgressBar.setProgress(0);
        timelinessProgressBar.setProgress(0);
        completionProgressBar.setProgress(0);
        attendanceProgressBar.setProgress(0);
    }


    private void clearFeedback() {

        selectedResult =
                null;


        selectedResultLabel.setText(
                "Select a performance record to provide feedback."
        );


        existingFeedbackLabel.setText(
                "No feedback selected."
        );


        feedbackTextArea.clear();

        feedbackTextArea.setDisable(
                true
        );


        submitFeedbackButton.setDisable(
                true
        );
    }


    // =========================================================
    // STATUS STYLE
    // =========================================================

    private void updateStatusStyle(
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
    // NAVIGATION
    // =========================================================

    @FXML
    private void handleHome(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/manager/managerHome.fxml"
        );
    }


    @FXML
    private void handleProfile(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/manager/managerProfile.fxml"
        );
    }


    @FXML
    private void handleTasks(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/manager/managerTask.fxml"
        );
    }


    @FXML
    private void handlePerformance() {
        // Current page
    }


    @FXML
    private void handleReports() {

        System.out.println(
                "Manager Reports not implemented yet."
        );
    }


    @FXML
    private void handleDashboard() {

        System.out.println(
                "Manager Dashboard not implemented yet."
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
}