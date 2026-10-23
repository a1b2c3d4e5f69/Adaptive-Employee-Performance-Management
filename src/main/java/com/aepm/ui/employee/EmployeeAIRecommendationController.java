package com.aepm.ui.employee;

import com.aepm.data.repository.AIRecommendationRepository;
import com.aepm.logic.ai.AIPerformanceRecommendationService;
import com.aepm.logic.kpi.KpiCalculationService;
import com.aepm.model.KpiResult;
import com.aepm.model.User;
import com.aepm.ui.common.AEPMDialog;
import com.aepm.ui.common.AIRecommendationDialog;
import com.aepm.ui.common.AIRecommendationHistoryDialog;
import com.aepm.ui.navigation.NavigationManager;
import com.aepm.util.SessionManager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.net.URL;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class EmployeeAIRecommendationController {

    @FXML private BorderPane pageRoot;

    @FXML private Label welcomeLabel;
    @FXML private Label topNameLabel;
    @FXML private Label topRoleLabel;

    @FXML private Label overallKpiLabel;
    @FXML private Label evaluationPeriodLabel;

    @FXML private Label latestAssessmentLabel;
    @FXML private Label latestPeriodLabel;
    @FXML private Label latestGeneratedLabel;
    @FXML private Label latestModelLabel;
    @FXML private Label historyCountLabel;
    @FXML private Label messageLabel;

    @FXML private ComboBox<KpiResult> evaluationComboBox;

    @FXML private Button generateButton;
    @FXML private Button viewLatestButton;
    @FXML private Button viewHistoryButton;
    @FXML private Button refreshButton;

    private final KpiCalculationService kpiService =
            new KpiCalculationService();

    private final AIRecommendationRepository aiRepository =
            new AIRecommendationRepository();

    private final AIPerformanceRecommendationService aiService =
            new AIPerformanceRecommendationService();

    private static final ZoneId MYT =
            ZoneId.of("Asia/Kuala_Lumpur");

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern(
                    "dd MMM yyyy", Locale.ENGLISH
            );

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern(
                    "dd MMM yyyy HH:mm", Locale.ENGLISH
            );

    private User employee;
    private UUID employeeUuid;

    private JsonArray recommendationHistory = new JsonArray();
    private JsonObject latestRecord;

    private boolean busy;
    private long requestVersion;

    // =====================================================
    // INITIALIZE
    // =====================================================

    @FXML
    public void initialize() {

        URL css = getClass().getResource(
                "/css/employee-ai-recommendation.css"
        );

        if (css != null) {
            pageRoot.getStylesheets().add(
                    css.toExternalForm()
            );
        }

        setupEvaluationComboBox();
        resetDisplay();

        employee = SessionManager.getCurrentUser();

        if (employee == null
                || employee.getRoleId() != 3
                || employee.getUserId() == null) {

            setBusy(true);
            showMessage("Employee access is required.");
            return;
        }

        try {
            employeeUuid = UUID.fromString(
                    employee.getUserId()
            );
        } catch (IllegalArgumentException ex) {
            setBusy(true);
            showMessage("Invalid employee account ID.");
            return;
        }

        String name = employee.getFullName();

        if (name == null || name.isBlank()) {
            name = employee.getUsername();
        }

        if (name == null || name.isBlank()) {
            name = "Employee";
        }

        welcomeLabel.setText("Welcome, " + name);
        topNameLabel.setText(name);

        topRoleLabel.setText(
                "EMPLOYEE  •  " + safe(employee.getEmployeeId())
        );

        loadData();
    }

    // =====================================================
    // KPI SELECTION
    // =====================================================

    private void setupEvaluationComboBox() {

        evaluationComboBox.setConverter(
                new StringConverter<>() {

                    @Override
                    public String toString(KpiResult result) {
                        return result == null
                                ? ""
                                : period(result);
                    }

                    @Override
                    public KpiResult fromString(String text) {
                        return null;
                    }
                }
        );

        evaluationComboBox.valueProperty().addListener(
                (observable, oldValue, newValue) -> {
                    displaySelectedKpi(newValue);
                    updateButtons();
                }
        );
    }

    private void displaySelectedKpi(KpiResult result) {

        if (result == null) {
            overallKpiLabel.setText("—");
            evaluationPeriodLabel.setText(
                    "Select a KPI evaluation"
            );
            return;
        }

        overallKpiLabel.setText(
                String.format(
                        Locale.ENGLISH,
                        "%.2f%%",
                        result.getOverallKpiScore()
                )
        );

        evaluationPeriodLabel.setText(
                period(result)
        );
    }

    // =====================================================
    // LOAD KPI + RECOMMENDATION HISTORY
    // =====================================================

    @FXML
    private void handleRefresh() {
        loadData();
    }

    private void loadData() {

        if (employeeUuid == null || busy) {
            return;
        }

        final long version = ++requestVersion;

        final UUID requestedEmployeeId = employeeUuid;
        final String token = SessionManager.getAccessToken();

        setBusy(true);
        showMessage("Loading your performance and AI records...");

        Task<LoadedData> task = new Task<>() {

            @Override
            protected LoadedData call() throws Exception {

                List<KpiResult> fetchedKpis =
                        kpiService.getEmployeeResults(
                                requestedEmployeeId.toString()
                        );

                List<KpiResult> ownKpis = new ArrayList<>();

                if (fetchedKpis != null) {
                    for (KpiResult result : fetchedKpis) {
                        if (result != null
                                && requestedEmployeeId.toString()
                                .equals(result.getEmployeeId())) {
                            ownKpis.add(result);
                        }
                    }
                }

                ownKpis.sort(
                        Comparator.comparing(
                                KpiResult::getEvaluationEndDate,
                                Comparator.nullsFirst(
                                        Comparator.naturalOrder()
                                )
                        ).reversed()
                );

                JsonArray history = aiRepository.getHistory(
                        requestedEmployeeId,
                        token
                );

                return new LoadedData(
                        ownKpis,
                        history == null
                                ? new JsonArray()
                                : history
                );
            }
        };

        task.setOnSucceeded(event -> {

            if (!isCurrent(version)) {
                return;
            }

            LoadedData data = task.getValue();

            evaluationComboBox.getItems().setAll(
                    data.kpiResults()
            );

            if (!data.kpiResults().isEmpty()) {
                evaluationComboBox
                        .getSelectionModel()
                        .selectFirst();
            }

            recommendationHistory = data.history();

            updateLatestRecommendation();

            setBusy(false);

            if (data.kpiResults().isEmpty()) {
                showMessage(
                        "No KPI evaluation is available yet. "
                        + "You can view existing AI records, "
                        + "but cannot generate a new recommendation."
                );
            } else {
                hideMessage();
            }
        });

        task.setOnFailed(event -> {

            if (!isCurrent(version)) {
                return;
            }

            setBusy(false);

            Throwable error = task.getException();

            showMessage(
                    error == null || error.getMessage() == null
                            ? "Unable to load AI recommendations."
                            : error.getMessage()
            );
        });

        Thread worker = new Thread(
                task,
                "employee-ai-load"
        );

        worker.setDaemon(true);
        worker.start();
    }

    // =====================================================
    // UPDATE LATEST RECOMMENDATION
    // =====================================================

    private void updateLatestRecommendation() {

        latestRecord = null;

        for (JsonElement element : recommendationHistory) {

            if (element == null || !element.isJsonObject()) {
                continue;
            }

            JsonObject row = element.getAsJsonObject();

            if (latestRecord == null
                    || generatedTime(row).isAfter(
                            generatedTime(latestRecord)
                    )) {
                latestRecord = row;
            }
        }

        historyCountLabel.setText(
                recommendationHistory.size()
                        + " saved recommendation(s)"
        );

        if (latestRecord == null) {

            latestAssessmentLabel.setText(
                    "No AI recommendation has been generated yet."
            );

            latestPeriodLabel.setText("—");
            latestGeneratedLabel.setText("—");
            latestModelLabel.setText("—");

            updateButtons();
            return;
        }

        latestPeriodLabel.setText(
                formatRecordPeriod(latestRecord)
        );

        latestGeneratedLabel.setText(
                formatTime(
                        jsonValue(latestRecord, "generated_at")
                )
        );

        latestModelLabel.setText(
                jsonValue(latestRecord, "model_name")
        );

        String assessment = jsonValue(
                latestRecord,
                "overall_assessment"
        );

        JsonObject recommendation =
                extractRecommendation(latestRecord);

        if (recommendation != null
                && recommendation.has("overall_assessment")
                && !recommendation.get(
                        "overall_assessment"
                ).isJsonNull()) {

            assessment = recommendation
                    .get("overall_assessment")
                    .getAsString();
        }

        latestAssessmentLabel.setText(
                "-".equals(assessment)
                        ? "Open View Details to review this recommendation."
                        : assessment
        );

        updateButtons();
    }

    // =====================================================
    // GENERATE NEW AI RECOMMENDATION
    // =====================================================

    @FXML
    private void handleGenerate() {

        if (busy || employeeUuid == null) {
            return;
        }

        KpiResult selected = evaluationComboBox.getValue();

        if (selected == null
                || selected.getResultId() == null
                || selected.getResultId() <= 0) {

            showMessage(
                    "Select a valid KPI evaluation first."
            );
            return;
        }

        if (!employeeUuid.toString().equals(
                selected.getEmployeeId()
        )) {
            showMessage(
                    "You cannot generate a recommendation "
                    + "for another employee."
            );
            return;
        }

        LocalDate start = selected.getEvaluationStartDate();
        LocalDate end = selected.getEvaluationEndDate();

        if (start == null
                || end == null
                || end.isBefore(start)) {

            showMessage("Invalid evaluation period.");
            return;
        }

        final UUID ownId = employeeUuid;
        final Long kpiId = selected.getResultId();
        final long version = ++requestVersion;

        setBusy(true);

        showMessage(
                "Analysing your KPI and generating AI recommendations..."
        );

        Task<AIPerformanceRecommendationService.RecommendationResult>
                task = new Task<>() {

            @Override
            protected AIPerformanceRecommendationService
                    .RecommendationResult call()
                    throws Exception {

                return aiService.generate(
                        ownId,
                        start,
                        end,
                        kpiId,
                        null,
                        null
                );
            }
        };

        
    task.setOnSucceeded(event -> {

        if (!isCurrent(version)) {
            return;
        }

        setBusy(false);

        showMessage(
                "AI recommendation generated and saved successfully. "
                + "Loading your updated recommendation history..."
        );

        // Reload the saved recommendation from Supabase.
        // This ensures Performance Evidence, Generated At
        // and Gemini Model come from the actual saved record.
        loadData();
    });


        task.setOnFailed(event -> {

            if (!isCurrent(version)) {
                return;
            }

            setBusy(false);

            Throwable error = task.getException();

            String message =
                    error == null || error.getMessage() == null
                            ? "Unable to generate AI recommendations."
                            : error.getMessage();

            showMessage(message);

            AEPMDialog.showError(
                    pageRoot.getScene().getWindow(),
                    "AI Generation Failed",
                    message
            );
        });

        Thread worker = new Thread(
                task,
                "employee-ai-generate"
        );

        worker.setDaemon(true);
        worker.start();
    }

    // =====================================================
    // VIEW LATEST RECOMMENDATION
    // =====================================================

    @FXML
    private void handleViewLatest() {

        if (busy || latestRecord == null) {
            return;
        }

        JsonObject recommendation =
                extractRecommendation(latestRecord);

        if (recommendation == null) {

            AEPMDialog.showError(
                    pageRoot.getScene().getWindow(),
                    "Recommendation Unavailable",
                    "The saved recommendation has no "
                    + "supported structured details."
            );
            return;
        }

        try {

            LocalDate start = LocalDate.parse(
                    jsonValue(latestRecord, "period_start")
            );

            LocalDate end = LocalDate.parse(
                    jsonValue(latestRecord, "period_end")
            );
            
            JsonObject evidence = latestRecord.has("performance_evidence")
                    && latestRecord.get("performance_evidence").isJsonObject()
                    ? latestRecord.getAsJsonObject("performance_evidence").deepCopy()
                    : new JsonObject();

            AIRecommendationDialog.show(
                    pageRoot.getScene().getWindow(),
                    employee.getFullName(),
                    employee.getEmployeeId(),
                    employee.getDepartment(),
                    start,
                    end,
                    recommendation.deepCopy(),
                    evidence,
                    jsonValue(latestRecord, "generated_at"),
                    jsonValue(latestRecord, "model_name")
            );


        } catch (RuntimeException ex) {

            AEPMDialog.showError(
                    pageRoot.getScene().getWindow(),
                    "Unable to Open Recommendation",
                    "This record has an invalid evaluation period."
            );
        }
    }

    // =====================================================
    // VIEW AI HISTORY
    // =====================================================

    @FXML
    private void handleViewHistory() {

        if (busy || recommendationHistory.isEmpty()) {
            return;
        }

        AIRecommendationHistoryDialog.show(
                pageRoot.getScene().getWindow(),
                employee.getFullName(),
                employee.getEmployeeId(),
                employee.getDepartment(),
                recommendationHistory
        );
    }

    // =====================================================
    // HELPERS
    // =====================================================

    private JsonObject extractRecommendation(JsonObject row) {

        if (row == null
                || !row.has("performance_evidence")
                || !row.get("performance_evidence").isJsonObject()) {
            return null;
        }

        JsonObject evidence =
                row.getAsJsonObject("performance_evidence");

        if (!evidence.has("recommendation")
                || !evidence.get("recommendation").isJsonObject()) {
            return null;
        }

        return evidence.getAsJsonObject("recommendation");
    }

    private OffsetDateTime generatedTime(JsonObject row) {
        try {
            return OffsetDateTime.parse(
                    jsonValue(row, "generated_at")
            );
        } catch (RuntimeException ex) {
            return OffsetDateTime.MIN;
        }
    }

    private String formatRecordPeriod(JsonObject row) {

        try {
            LocalDate start = LocalDate.parse(
                    jsonValue(row, "period_start")
            );

            LocalDate end = LocalDate.parse(
                    jsonValue(row, "period_end")
            );

            return start.format(DATE_FORMAT)
                    + " – "
                    + end.format(DATE_FORMAT);

        } catch (RuntimeException ex) {
            return "—";
        }
    }

    private String formatTime(String text) {

        try {
            return OffsetDateTime.parse(text)
                    .atZoneSameInstant(MYT)
                    .format(TIME_FORMAT);

        } catch (RuntimeException ex) {
            return "—";
        }
    }

    private String period(KpiResult result) {

        if (result == null
                || result.getEvaluationStartDate() == null
                || result.getEvaluationEndDate() == null) {
            return "Unknown evaluation period";
        }

        return result.getEvaluationStartDate()
                .format(DATE_FORMAT)
                + " – "
                + result.getEvaluationEndDate()
                .format(DATE_FORMAT);
    }

    private String jsonValue(JsonObject object, String key) {

        if (object == null
                || !object.has(key)
                || object.get(key).isJsonNull()) {
            return "-";
        }

        return object.get(key).getAsString();
    }

    private String safe(String value) {
        return value == null || value.isBlank()
                ? "-"
                : value;
    }

    private boolean isCurrent(long version) {

        User current = SessionManager.getCurrentUser();

        return version == requestVersion
                && current != null
                && employee != null
                && employee.getUserId().equals(
                        current.getUserId()
                );
    }

    private void setBusy(boolean value) {
        busy = value;
        updateButtons();
    }

    private void updateButtons() {

        if (generateButton == null) {
            return;
        }

        refreshButton.setDisable(busy);
        evaluationComboBox.setDisable(busy);

        generateButton.setDisable(
                busy
                || evaluationComboBox.getValue() == null
                || employeeUuid == null
        );

        viewLatestButton.setDisable(
                busy || latestRecord == null
        );

        viewHistoryButton.setDisable(
                busy || recommendationHistory.isEmpty()
        );
    }

    private void resetDisplay() {

        overallKpiLabel.setText("—");
        evaluationPeriodLabel.setText(
                "Select a KPI evaluation"
        );

        latestAssessmentLabel.setText(
                "Loading your latest AI recommendation..."
        );

        latestPeriodLabel.setText("—");
        latestGeneratedLabel.setText("—");
        latestModelLabel.setText("—");
        historyCountLabel.setText("0 saved recommendations");

        updateButtons();
        hideMessage();
    }

    private void showMessage(String message) {

        messageLabel.setText(message);
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    private void hideMessage() {

        messageLabel.setText("");
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
    }

    // =====================================================
    // NAVIGATION
    // =====================================================

    @FXML
    private void handleHome() {
        navigate(
                "/fxml/employee/employeeHome.fxml"
        );
    }

    @FXML
    private void handleProfile() {
        navigate(
                "/fxml/employee/employeeProfile.fxml"
        );
    }

    @FXML
    private void handleTasks() {
        navigateToExisting(
                "/fxml/employee/employeeTask.fxml",
                "/fxml/employee/employeeTasks.fxml",
                "/fxml/employee/employeeTaskManagement.fxml"
        );
    }

    @FXML
    private void handleAttendance() {
        navigateToExisting(
                "/fxml/employee/employeeAttendance.fxml",
                "/fxml/employee/employeeAttendanceManagement.fxml"
        );
    }

    @FXML
    private void handlePerformance() {
        navigateToExisting(
                "/fxml/employee/employeePerformance.fxml",
                "/fxml/employee/employeeKpi.fxml"
        );
    }

    @FXML
    private void handleAiRecommendations() {
        // Current page.
    }

    @FXML
    private void handleLogout() {

        SessionManager.clearSession();

        navigate(
                "/fxml/auth/login.fxml"
        );
    }

    private void navigateToExisting(String... paths) {

        for (String path : paths) {
            if (getClass().getResource(path) != null) {
                navigate(path);
                return;
            }
        }

        AEPMDialog.showError(
                pageRoot.getScene().getWindow(),
                "Page Not Found",
                "Please update this menu to use "
                + "the existing Employee FXML path."
        );
    }

    private void navigate(String path) {

        Stage stage = (Stage)
                pageRoot.getScene().getWindow();

        NavigationManager.navigate(
                stage,
                path
        );
    }

    private record LoadedData(
            List<KpiResult> kpiResults,
            JsonArray history
    ) {}
}
