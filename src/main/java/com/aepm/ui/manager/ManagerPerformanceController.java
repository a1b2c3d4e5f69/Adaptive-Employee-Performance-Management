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

import com.aepm.logic.ai.AIPerformanceRecommendationService;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import com.aepm.ui.common.AEPMDialog;

import java.util.UUID;
public class ManagerPerformanceController {

    // =========================================================
    // TOP BAR
    // =========================================================

    @FXML
    private Label topNameLabel;

    @FXML
    private Label topRoleLabel;

    @FXML
    private Label welcomeLabel;


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

    @FXML
    private Button generateAiButton;

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

    @FXML
    private javafx.scene.control.Button aiHistoryButton;

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

    private PerformanceFeedback loadedFeedback;
    private boolean editingFeedback;
    private long feedbackViewVersion;
    private long performanceViewVersion;


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

    @FXML
    private void handleDeadlineEscalation(ActionEvent event) {    
        Stage stage = (Stage) ((Node) event.getSource())
                .getScene()
                .getWindow();
    
        NavigationManager.navigate(
                stage,
                "/fxml/manager/managerDeadlineEscalation.fxml"
        );
    }

    @FXML
    private void handleTaskManagement(ActionEvent event) {

        Stage stage = (Stage) ((Node) event.getSource())
                .getScene()
                .getWindow();

        NavigationManager.navigate(
                stage,
                "/fxml/manager/managerTask.fxml"
        );
    }

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

    @FXML
    private void handleGenerateAi() {
        if (generateAiButton.isDisabled()) {
                return;
        }

        if (selectedEmployee == null
                || selectedResult == null
                || selectedResult.getResultId() == null) {
                showMessage("Select an employee and a KPI evaluation first.");
                return;
        }

        var user = SessionManager.getCurrentUser();
        String token = SessionManager.getAccessToken();

        if (user == null || token == null || token.isBlank()) {
                showMessage("Please sign in again.");
                return;
        }

        final UUID employeeId;

        try {
                employeeId = UUID.fromString(selectedEmployee.getUserId());

                if (!employeeId.equals(
                        UUID.fromString(selectedResult.getEmployeeId()))) {
                showMessage(
                        "The selected evaluation belongs to another employee."
                );
                return;
                }
        } catch (RuntimeException ex) {
                showMessage("The selected employee or evaluation has an invalid ID.");
                return;
        }

        final Long resultId = selectedResult.getResultId();

        if (resultId <= 0) {
                showMessage("The selected KPI result ID is invalid.");
                return;
        }

        final String requestingUserId = user.getUserId();
        final var originalScene = generateAiButton.getScene();
        final String accessToken = token;

        generateAiButton.setDisable(true);
        showMessage("Checking existing AI recommendations...");

        javafx.concurrent.Task<Boolean> check =
                new javafx.concurrent.Task<>() {
                        @Override
                        protected Boolean call() throws Exception {
                        return new com.aepm.data.repository
                                .AIRecommendationRepository()
                                .hasRecommendation(
                                        employeeId,
                                        resultId,
                                        accessToken
                                );
                        }
                };

        check.setOnSucceeded(event -> {
                generateAiButton.setDisable(false);

                if (!isAiViewActive(originalScene, requestingUserId)) {
                return;
                }

                if (!isSameAiSelection(employeeId, resultId)) {
                showMessage(
                        "Selection changed. Click Generate again "
                        + "for the selected evaluation."
                );
                return;
                }

                if (!Boolean.TRUE.equals(check.getValue())) {
                generateAiNow();
                return;
                }

                var choice = com.aepm.ui.common.AEPMDialog
                        .showRecommendationChoice(originalScene.getWindow());

                // Recheck after the modal closes.
                if (!isAiViewActive(originalScene, requestingUserId)
                        || !isSameAiSelection(employeeId, resultId)) {
                return;
                }

                switch (choice) {
                case VIEW_HISTORY -> {
                        showMessage("Opening saved recommendation history.");
                        handleAiHistory();
                }
                case GENERATE_NEW -> generateAiNow();
                case CANCEL -> showMessage(
                        "Generation cancelled. No AI request was sent."
                );
                }
        });

        check.setOnFailed(event -> {
                generateAiButton.setDisable(false);

                if (!isAiViewActive(originalScene, requestingUserId)
                        || !isSameAiSelection(employeeId, resultId)) {
                return;
                }

                String message =
                        "Unable to check existing recommendations. "
                        + "No AI request was sent. Please try again.";

                showMessage(message);

                com.aepm.ui.common.AEPMDialog.showError(
                        originalScene.getWindow(),
                        "Unable to Check Recommendations",
                        message
                );
        });

        Thread worker = new Thread(check, "aepm-ai-duplicate-check");
        worker.setDaemon(true);
        worker.start();
        }

        private boolean isSameAiSelection(
                UUID employeeId,
                Long resultId
        ) {
        return selectedEmployee != null
                && selectedResult != null
                && employeeId.toString().equals(
                        selectedEmployee.getUserId())
                && resultId.equals(selectedResult.getResultId());
    }

    private void generateAiNow() {

        if (generateAiButton.isDisabled()) {
                return;
        }

        if (selectedEmployee == null || selectedResult == null) {
                showMessage(
                        "Select an employee and a KPI evaluation first."
                );
                return;
        }

        final UUID employeeId;

        try {
                employeeId = UUID.fromString(
                        selectedEmployee.getUserId()
                );

                UUID resultEmployeeId = UUID.fromString(
                        selectedResult.getEmployeeId()
                );

                if (!employeeId.equals(resultEmployeeId)) {
                showMessage(
                        "The selected evaluation belongs to another employee."
                );
                return;
                }
        } catch (IllegalArgumentException | NullPointerException ex) {
                showMessage(
                        "The selected employee or evaluation has an invalid ID."
                );
                return;
        }

        final Long selectedKpiResultId = selectedResult.getResultId();

        if (selectedKpiResultId == null || selectedKpiResultId <= 0) {
        showMessage("The selected KPI evaluation has an invalid result ID.");
        return;
        }

        final LocalDate start =
                selectedResult.getEvaluationStartDate();

        final LocalDate end =
                selectedResult.getEvaluationEndDate();

        if (start == null || end == null || end.isBefore(start)) {
                showMessage(
                        "The selected evaluation has invalid reporting dates."
                );
                return;
        }

        User currentUser = SessionManager.getCurrentUser();

        if (currentUser == null) {
                showMessage("Please sign in again.");
                return;
        }

        final String requestingUserId = currentUser.getUserId();
        final var originalScene = generateAiButton.getScene();
        final String aiEmployeeName = selectedEmployee.getFullName();
        final String aiEmployeeCode = selectedEmployee.getEmployeeId();
        final String aiDepartment = selectedEmployee.getDepartment();

        generateAiButton.setDisable(true);
        showMessage(
                "Checking performance data and generating recommendations..."
        );

        Task<AIPerformanceRecommendationService.RecommendationResult> task =
                new Task<>() {
                        @Override
                        protected AIPerformanceRecommendationService
                                .RecommendationResult call() throws Exception {

                        return new AIPerformanceRecommendationService()
                                .generate(
                                        employeeId,
                                        start,
                                        end,
                                        selectedKpiResultId,
                                        null,
                                        null
                                );
                        }
                };

        task.setOnSucceeded(event -> {
                generateAiButton.setDisable(false);

                if (!isAiViewActive(originalScene, requestingUserId)) {
                        return;
                }

                showMessage("AI recommendations generated and saved.");

                var generated = task.getValue();

                com.aepm.ui.common.AIRecommendationDialog.show(
                        originalScene.getWindow(),
                        aiEmployeeName,
                        aiEmployeeCode,
                        aiDepartment,
                        start,
                        end,
                        generated.recommendation(),
                        generated.snapshot(),
                        generated.savedRecord().get("generated_at").getAsString(),
                        generated.savedRecord().get("model_name").getAsString()
                );
        });

        task.setOnFailed(event -> {
        generateAiButton.setDisable(false);

        if (!isAiViewActive(originalScene, requestingUserId)) {
                return;
        }

        Throwable error = task.getException();
        String message;

        if (error instanceof java.net.http.HttpTimeoutException) {
                message = "The AI request timed out. Please try again later.";

        } else if (error instanceof java.io.IOException) {
                message = "Unable to connect to the AI service. "
                        + "Please check your connection and try again.";

        } else if (error instanceof InterruptedException) {
                message = "AI generation was interrupted. Please try again.";

        } else {
                message = error == null
                        || error.getMessage() == null
                        || error.getMessage().isBlank()
                        ? "Unable to generate AI recommendations."
                        : error.getMessage();
        }

        showMessage(message);

        com.aepm.ui.common.AEPMDialog.showError(
                originalScene.getWindow(),
                "Unable to Generate AI Recommendations",
                message
        );
        });

        Thread worker = new Thread(task, "aepm-ai-recommendation");
        worker.setDaemon(true);
        worker.start();
        }

        private boolean isAiViewActive(
                javafx.scene.Scene originalScene,
                String requestingUserId
        ) {
        User currentUser = SessionManager.getCurrentUser();

        return currentUser != null
                && requestingUserId != null
                && requestingUserId.equals(currentUser.getUserId())
                && originalScene != null
                && generateAiButton.getScene() == originalScene
                && originalScene.getWindow() != null
                && originalScene.getWindow().isShowing()
                && originalScene.getWindow().getScene() == originalScene;
        }

        private void showAiRecommendations(
                UUID employeeId,
                LocalDate start,
                LocalDate end,
                JsonObject recommendation
        ) {
        StringBuilder text = new StringBuilder();

        text.append("Scope: aggregate KPI only\n\n");

        text.append("Assessment\n")
                .append(recommendation
                        .get("overall_assessment").getAsString())
                .append("\n\n");

        appendAiList(text, "Strengths", recommendation, "strengths");

        appendAiList(
                text,
                "Areas for improvement",
                recommendation,
                "areas_for_improvement"
        );

        text.append("Recommendations\n");

        int number = 1;

        for (JsonElement element :
                recommendation.getAsJsonArray("recommendations")) {

                JsonObject item = element.getAsJsonObject();

                text.append(number++).append(". ")
                        .append(item.get("suggested_action").getAsString())
                        .append("\nReason: ")
                        .append(item.get("reason").getAsString())
                        .append("\nEvidence: ")
                        .append(item.get("evidence").getAsString())
                        .append("\n\n");
        }

        if (number == 1) {
                text.append("None provided.\n\n");
        }

        appendAiList(
                text,
                "Suggested training",
                recommendation,
                "suggested_training"
        );

        appendAiList(
                text,
                "Manager coaching",
                recommendation,
                "manager_interventions"
        );

        TextArea output = new TextArea(text.toString());
        output.setEditable(false);
        output.setWrapText(true);
        output.setPrefSize(680, 480);

        Alert dialog = new Alert(
                Alert.AlertType.INFORMATION,
                "",
                ButtonType.CLOSE
        );

        dialog.initOwner(generateAiButton.getScene().getWindow());
        dialog.setTitle("AI Performance Recommendations");
        dialog.setHeaderText(
                "Employee: " + employeeId
                + "\nPeriod: " + start + " to " + end
        );

        dialog.getDialogPane().setContent(output);
        dialog.setResizable(true);
        dialog.show();
        }

        private void appendAiList(
                StringBuilder text,
                String heading,
                JsonObject recommendation,
                String key
        ) {
        text.append(heading).append("\n");

        var items = recommendation.getAsJsonArray(key);

        if (items.isEmpty()) {
                text.append("None provided.\n");
        } else {
                for (JsonElement item : items) {
                text.append("• ")
                        .append(item.getAsString())
                        .append("\n");
                }
        }

        text.append("\n");
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

        welcomeLabel.setText("Welcome, " + name);


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

    private void loadEmployeePerformance(User employee) {

        clearPerformance();
        clearFeedback();
        hideMessage();

        final long requestVersion = performanceViewVersion;

        if (employee == null
                || employee.getUserId() == null
                || employee.getUserId().isBlank()) {
                return;
        }

        final String employeeUserId = employee.getUserId();

        javafx.concurrent.Task<List<KpiResult>> task =
                new javafx.concurrent.Task<>() {
                        @Override
                        protected List<KpiResult> call() throws Exception {
                        return calculationService.getEmployeeResults(
                                employeeUserId
                        );
                        }
                };

        task.setOnSucceeded(event -> {
                if (requestVersion != performanceViewVersion) {
                return;
                }

                if (selectedEmployee == null
                        || !employeeUserId.equals(
                                selectedEmployee.getUserId())) {
                return;
                }

                List<KpiResult> loadedResults = task.getValue();

                allResults.clear();

                if (loadedResults == null || loadedResults.isEmpty()) {
                showMessage(
                        "No KPI results are available for this employee."
                );
                return;
                }

                allResults.addAll(loadedResults);
                applyPerformanceFilter();

                KpiResult latest = loadedResults.get(0);

                if (filteredResults.contains(latest)) {
                // The selection listener displays the KPI
                // and calls loadFeedback().
                historyTable.getSelectionModel().select(latest);
                } else {
                // Preserve the existing behaviour of displaying
                // the latest KPI even when the history filter hides it.
                selectedResult = latest;
                displayPerformance(latest);
                loadFeedback(latest);
                }
        });

        task.setOnFailed(event -> {
                if (requestVersion != performanceViewVersion) {
                return;
                }

                Throwable error = task.getException();

                showMessage(
                        error == null || error.getMessage() == null
                                ? "Unable to load employee performance."
                                : error.getMessage()
                );
        });

        Thread worker = new Thread(task, "load-employee-performance");
        worker.setDaemon(true);
        worker.start();
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

    private void loadFeedback(KpiResult result) {

        clearFeedback();

        if (result == null || result.getResultId() == null) {
                return;
        }

        if (selectedEmployee == null
                || selectedEmployee.getUserId() == null
                || !selectedEmployee.getUserId()
                        .equals(result.getEmployeeId())) {
                return;
        }

        // clearFeedback() resets selectedResult, so restore it here.
        selectedResult = result;

        final long viewVersion = feedbackViewVersion;
        final Long resultId = result.getResultId();

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
                        protected PerformanceFeedback call() throws Exception {
                        return feedbackService.getLatestFeedbackByResult(
                                resultId
                        );
                        }
                };

        task.setOnSucceeded(event -> {
                if (viewVersion != feedbackViewVersion) {
                return;
                }

                displayFeedbackState(task.getValue());
        });

        task.setOnFailed(event -> {
                if (viewVersion != feedbackViewVersion) {
                return;
                }

                existingFeedbackLabel.setText(
                        "Unable to load feedback."
                );

                feedbackTextArea.setDisable(true);
                submitFeedbackButton.setDisable(true);

                Throwable error = task.getException();

                showMessage(
                        error == null || error.getMessage() == null
                                ? "Unable to load feedback."
                                : error.getMessage()
                );
        });

        Thread worker = new Thread(task, "load-performance-feedback");
        worker.setDaemon(true);
        worker.start();
    }


    // =========================================================
    // SUBMIT FEEDBACK
    // =========================================================

    @FXML
    private void handleSubmitFeedback() {

        if (submitFeedbackButton.isDisabled()) {
                return;
        }

        if (selectedEmployee == null || selectedResult == null) {
                showMessage("Select an employee and a KPI result first.");
                return;
        }

        final String employeeId = selectedEmployee.getUserId();
        final Long resultId = selectedResult.getResultId();

        if (employeeId == null
                || resultId == null
                || !employeeId.equals(selectedResult.getEmployeeId())) {
                showMessage("The selected employee and KPI result do not match.");
                return;
        }

        final PerformanceFeedback existing = loadedFeedback;

        if (existing != null
                && (!resultId.equals(existing.getResultId())
                        || !employeeId.equals(existing.getEmployeeId()))) {
                showMessage("Refresh the feedback before editing.");
                return;
        }

        if (existing != null && !editingFeedback) {
                editingFeedback = true;
                feedbackTextArea.setEditable(true);
                feedbackTextArea.requestFocus();
                submitFeedbackButton.setText("Save Changes");

                showMessage(
                        "Edit the feedback, then click Save Changes. "
                        + "The employee can read the saved changes after refreshing."
                );
                return;
        }

        final String text = feedbackTextArea.getText().trim();

        if (text.isBlank()) {
                showMessage("Feedback cannot be empty.");
                return;
        }

        if (existing != null && text.equals(existing.getFeedbackText())) {
                displayFeedbackState(existing);
                showMessage("No changes to save.");
                return;
        }

        final long viewVersion = feedbackViewVersion;

        submitFeedbackButton.setDisable(true);
        feedbackTextArea.setEditable(false);

        javafx.concurrent.Task<PerformanceFeedback> task =
                new javafx.concurrent.Task<>() {
                        @Override
                        protected PerformanceFeedback call() throws Exception {
                        if (existing == null) {
                                return feedbackService.submitFeedback(
                                        resultId,
                                        employeeId,
                                        text
                                );
                        }

                        return feedbackService.updateFeedback(
                                existing,
                                text
                        );
                        }
                };

        task.setOnSucceeded(event -> {
                if (viewVersion != feedbackViewVersion) {
                return;
                }

                displayFeedbackState(task.getValue());

                showMessage(existing == null
                        ? "Feedback submitted successfully."
                        : "Feedback changes saved successfully.");
        });

        task.setOnFailed(event -> {
                if (viewVersion != feedbackViewVersion) {
                        return;
                }

                feedbackTextArea.setEditable(true);
                submitFeedbackButton.setDisable(false);

                Throwable error = task.getException();

                String message =
                        error == null || error.getMessage() == null
                                ? "Unable to save feedback."
                                : error.getMessage();

                showMessage(message);

                AEPMDialog.showError(
                        submitFeedbackButton.getScene().getWindow(),
                        "Unable to Save Feedback",
                        message
                );
        });

        Thread worker = new Thread(task, "performance-feedback-save");
        worker.setDaemon(true);
        worker.start();
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
        performanceViewVersion++;
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

        feedbackViewVersion++;
        loadedFeedback = null;
        editingFeedback = false;

        feedbackTextArea.setEditable(true);
        submitFeedbackButton.setText("Submit Feedback");

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

    @FXML
    private void handleAiHistory() {
        if (aiHistoryButton.isDisabled()) {
                return;
        }

        if (selectedEmployee == null) {
                showMessage("Select an employee first.");
                return;
        }

        var currentUser = SessionManager.getCurrentUser();

        if (currentUser == null) {
                showMessage("Please sign in again.");
                return;
        }

        final UUID employeeId;

        try {
                employeeId = UUID.fromString(selectedEmployee.getUserId());
        } catch (RuntimeException ex) {
                showMessage("The selected employee has an invalid ID.");
                return;
        }

        final String requestingUserId = currentUser.getUserId();
        final String accessToken = SessionManager.getAccessToken();
        final var originalScene = generateAiButton.getScene();

        final String employeeName = selectedEmployee.getFullName();
        final String employeeCode = selectedEmployee.getEmployeeId();
        final String department = selectedEmployee.getDepartment();

        aiHistoryButton.setDisable(true);

        javafx.concurrent.Task<com.google.gson.JsonArray> task =
                new javafx.concurrent.Task<>() {
                        @Override
                        protected com.google.gson.JsonArray call()
                                throws Exception {
                        return new com.aepm.data.repository
                                .AIRecommendationRepository()
                                .getHistory(employeeId, accessToken);
                        }
                };

        task.setOnSucceeded(event -> {
                aiHistoryButton.setDisable(false);

                if (!isAiViewActive(originalScene, requestingUserId)
                        || selectedEmployee == null
                        || !employeeId.toString().equals(
                                selectedEmployee.getUserId())) {
                return;
                }

                com.aepm.ui.common.AIRecommendationHistoryDialog.show(
                        originalScene.getWindow(),
                        employeeName,
                        employeeCode,
                        department,
                        task.getValue()
                );
        });

        task.setOnFailed(event -> {
                aiHistoryButton.setDisable(false);

                if (!isAiViewActive(originalScene, requestingUserId)
                        || selectedEmployee == null
                        || !employeeId.toString().equals(
                                selectedEmployee.getUserId())) {
                return;
                }

                Throwable error = task.getException();

                com.aepm.ui.common.AEPMDialog.showError(
                        originalScene.getWindow(),
                        "Unable to Load History",
                        error == null || error.getMessage() == null
                                ? "Please try loading the history again."
                                : error.getMessage()
                );
        });

        Thread worker = new Thread(task, "aepm-ai-history");
        worker.setDaemon(true);
        worker.start();
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
        private void handleReports(javafx.event.ActionEvent event) {
        javafx.stage.Window owner =
                ((javafx.scene.Node) event.getSource())
                        .getScene()
                        .getWindow();

        ManagerReportsPage.show(owner);
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

    private void displayFeedbackState(PerformanceFeedback feedback) {
        loadedFeedback = feedback;
        editingFeedback = false;

        if (feedback == null) {
                existingFeedbackLabel.setText(
                        "No feedback has been submitted for this evaluation."
                );

                feedbackTextArea.clear();
                feedbackTextArea.setDisable(false);
                feedbackTextArea.setEditable(true);

                submitFeedbackButton.setText("Submit Feedback");
                submitFeedbackButton.setDisable(false);
                return;
        }

        String created = feedback.getCreatedAt() == null
                ? ""
                : "\nOriginally submitted: "
                        + formatDateTime(feedback.getCreatedAt());

        existingFeedbackLabel.setText(
                feedback.getFeedbackText() + created
        );

        feedbackTextArea.setText(feedback.getFeedbackText());
        feedbackTextArea.setDisable(false);
        feedbackTextArea.setEditable(false);

        User manager = SessionManager.getCurrentUser();

        boolean canEdit = manager != null
                && manager.getRoleId() == 2
                && manager.getUserId() != null
                && manager.getUserId().equals(feedback.getManagerId());

        submitFeedbackButton.setText("Edit Feedback");
        submitFeedbackButton.setDisable(!canEdit);
    }
}