package com.aepm.ui.admin;

import com.aepm.logic.ai.AIConfigurationService;
import com.aepm.ui.navigation.NavigationManager;
import com.aepm.util.SessionManager;
import com.google.gson.JsonObject;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class AdminAIConfigurationController {

    @FXML private Label welcomeLabel;
    @FXML private Label topNameLabel;
    @FXML private Label statusLabel;

    @FXML private CheckBox enabledCheck;
    @FXML private CheckBox kpiCheck;
    @FXML private CheckBox taskCheck;
    @FXML private CheckBox attendanceCheck;
    @FXML private CheckBox deadlineCheck;
    @FXML private CheckBox historyCheck;

    @FXML private TextField modelField;
    @FXML private TextField periodField;
    @FXML private TextField minimumField;
    @FXML private TextField maximumField;
    @FXML private TextArea promptArea;

    @FXML private Button saveButton;
    @FXML private Button refreshButton;

    private final AIConfigurationService service =
            new AIConfigurationService();

    private long configId = -1;
    private boolean configurationBusy = false;
    private JsonObject savedFormState;

    @FXML
    private void handleRefresh(ActionEvent event) {
        loadConfiguration();
    }

    @FXML
    public void initialize() {

        var user = SessionManager.getCurrentUser();

        if (user == null || user.getRoleId() != 1) {
            statusLabel.setText(
                    "Access denied. Administrator only."
            );
            saveButton.setDisable(true);
            refreshButton.setDisable(true);
            return;
        }

        String name = user.getFullName();

        if (name == null || name.isBlank()) {
            name = user.getUsername();
        }

        welcomeLabel.setText("Welcome, " + name);
        topNameLabel.setText(name);

        installChangeListeners();
        updateSaveButton();
        loadConfiguration();
    }

    private void loadConfiguration() {

        setBusy(true);
        statusLabel.setText("Loading AI configuration...");

        Task<JsonObject> task = new Task<>() {
            @Override
            protected JsonObject call() throws Exception {
                return service.load();
            }
        };

        task.setOnSucceeded(event -> {
            try {
                applyConfiguration(task.getValue());

                statusLabel.setText(
                        "AI configuration loaded successfully."
                );
            } catch (RuntimeException ex) {
                configId = -1;
                savedFormState = null;

                showError(
                        "Unable to Read Configuration",
                        ex
                );
            } finally {
                setBusy(false);
            }
        });
        task.setOnFailed(event -> {
            setBusy(false);
            showError(
                    "Failed to load AI configuration",
                    task.getException()
            );
        });

        startTask(task);
    }

    @FXML
    private void handleSave() {
    
        if (configurationBusy || !hasUnsavedChanges()) {
            return;
        }

        if (configId <= 0) {
            showAlert(
                    Alert.AlertType.WARNING,
                    "Please load the configuration first."
            );
            return;
        }

        final int days;
        final int minimum;
        final int maximum;

        try {
            days = Integer.parseInt(periodField.getText().trim());
            minimum = Integer.parseInt(
                    minimumField.getText().trim()
            );
            maximum = Integer.parseInt(
                    maximumField.getText().trim()
            );
        } catch (NumberFormatException e) {
            showAlert(
                    Alert.AlertType.WARNING,
                    "Analysis Period, Minimum Data Points "
                    + "and Maximum Recommendations must be numbers."
            );
            return;
        }

        final long id = configId;
        final boolean enabled = enabledCheck.isSelected();
        final String model = modelField.getText();
        final boolean kpi = kpiCheck.isSelected();
        final boolean taskCompletion = taskCheck.isSelected();
        final boolean attendance = attendanceCheck.isSelected();
        final boolean deadlines = deadlineCheck.isSelected();
        final boolean history = historyCheck.isSelected();
        final String prompt = promptArea.getText();

        setBusy(true);
        statusLabel.setText("Saving AI configuration...");

        Task<JsonObject> task = new Task<>() {
            @Override
            protected JsonObject call() throws Exception {
                return service.save(
                        id,
                        enabled,
                        model,
                        kpi,
                        taskCompletion,
                        attendance,
                        deadlines,
                        history,
                        days,
                        minimum,
                        maximum,
                        prompt
                );
            }
        };

        task.setOnSucceeded(event -> {
            try {
                // Use the values actually returned by the database.
                applyConfiguration(task.getValue());

                statusLabel.setText(
                        "AI configuration saved successfully."
                );

                setBusy(false);

                showAlert(
                        Alert.AlertType.INFORMATION,
                        "AI configuration saved successfully."
                );
            } catch (RuntimeException ex) {
                configId = -1;
                savedFormState = null;
                setBusy(false);

                showError(
                        "Reload Configuration",
                        new IllegalStateException(
                                "The save response could not be read. "
                                + "Refresh the configuration before saving again.",
                                ex
                        )
                );
            }
        });

        task.setOnFailed(event -> {
            setBusy(false);
            showError(
                    "Failed to save AI configuration",
                    task.getException()
            );
        });

        startTask(task);
    }

    private boolean getBoolean(
            JsonObject data, String key
    ) {
        return data.has(key)
                && !data.get(key).isJsonNull()
                && data.get(key).getAsBoolean();
    }

    private String getString(
            JsonObject data,
            String key,
            String fallback
    ) {
        return data.has(key)
                && !data.get(key).isJsonNull()
                ? data.get(key).getAsString()
                : fallback;
    }

    private void setBusy(boolean busy) {
        configurationBusy = busy;
        refreshButton.setDisable(busy);

        for (CheckBox check : new CheckBox[] {
                enabledCheck, kpiCheck, taskCheck,
                attendanceCheck, deadlineCheck, historyCheck
        }) {
            check.setDisable(busy);
        }

        for (TextInputControl field : new TextInputControl[] {
                modelField, periodField, minimumField,
                maximumField, promptArea
        }) {
            field.setDisable(busy);
        }

        updateSaveButton();
    }

    private void startTask(Task<?> task) {
        Thread thread = new Thread(task, "aepm-ai-config");
        thread.setDaemon(true);
        thread.start();
    }

    private void showError(String title, Throwable error) {
        String message = error == null
                ? "Unknown error."
                : error.getMessage();

        statusLabel.setText(title + ": " + message);
        showAlert(
                Alert.AlertType.ERROR,
                title + "\n\n" + message
        );
    }

    private void showAlert(
        Alert.AlertType type,
        String message
    ) {
        var owner = saveButton.getScene() == null
                ? null
                : saveButton.getScene().getWindow();

        if (type == Alert.AlertType.INFORMATION) {
            com.aepm.ui.common.AEPMDialog.showSuccess(
                    owner,
                    "AI Configuration Saved",
                    message
            );
        } else {
            com.aepm.ui.common.AEPMDialog.showError(
                    owner,
                    type == Alert.AlertType.WARNING
                            ? "Check AI Configuration"
                            : "AI Configuration Error",
                    message
            );
        }
    }

    private void navigate(
            ActionEvent event, String path
    ) {
        Stage stage = (Stage) ((Node) event.getSource())
                .getScene().getWindow();
        NavigationManager.navigate(stage, path);
    }

    @FXML
    private void handleHome(ActionEvent e) {
        navigate(e, "/fxml/admin/adminHome.fxml");
    }

    @FXML
    private void handleProfile(ActionEvent e) {
        navigate(e, "/fxml/admin/adminProfile.fxml");
    }

    @FXML
    private void handleUserManagement(ActionEvent e) {
        navigate(e, "/fxml/admin/admin-manage-users.fxml");
    }

    @FXML
    private void handleTaskManagement(ActionEvent e) {
        navigate(e, "/fxml/admin/adminTask.fxml");
    }

    @FXML
    private void handleEscalationRules(ActionEvent e) {
        navigate(e, "/fxml/admin/adminEscalationRules.fxml");
    }

    @FXML
    private void handleKpiManagement(ActionEvent e) {
        navigate(e, "/fxml/admin/adminKpi.fxml");
    }

    @FXML
    private void handleSystemConfiguration(ActionEvent e) {
        navigate(e, "/fxml/admin/systemConfiguration.fxml");
    }

    @FXML
    private void handleAttendanceManagement(ActionEvent e) {
        navigate(e, "/fxml/admin/adminAttendance.fxml");
    }

    @FXML
    private void handleAIConfiguration(ActionEvent e) {
        // Already on AI Configuration page.
    }

    @FXML
    private void handleReports(ActionEvent e) {
        navigate(e, "/fxml/admin/adminReports.fxml");
    }

    @FXML
    private void handleAnonymousReports(ActionEvent e) {
        navigate(e, "/fxml/admin/adminAnonymousReports.fxml");
    }

    @FXML
    private void handleLogout(ActionEvent e) {
        SessionManager.clearSession();
        navigate(e, "/fxml/auth/login.fxml");
    }

    private void installChangeListeners() {
        javafx.beans.InvalidationListener listener =
                observable -> updateSaveButton();

        for (CheckBox check : new CheckBox[] {
                enabledCheck, kpiCheck, taskCheck,
                attendanceCheck, deadlineCheck, historyCheck
        }) {
            check.selectedProperty().addListener(listener);
        }

        for (TextInputControl field : new TextInputControl[] {
                modelField, periodField, minimumField,
                maximumField, promptArea
        }) {
            field.textProperty().addListener(listener);
        }
    }

    private String normalizedText(TextInputControl field) {
        String text = field.getText();
        return text == null ? "" : text.trim();
    }

    private String normalizedInteger(TextField field) {
        String text = normalizedText(field);

        try {
            return Integer.toString(Integer.parseInt(text));
        } catch (NumberFormatException ex) {
            return text;
        }
    }

    private JsonObject currentFormState() {
        JsonObject state = new JsonObject();

        state.addProperty("enabled", enabledCheck.isSelected());
        state.addProperty("kpi", kpiCheck.isSelected());
        state.addProperty("tasks", taskCheck.isSelected());
        state.addProperty("attendance", attendanceCheck.isSelected());
        state.addProperty("deadlines", deadlineCheck.isSelected());
        state.addProperty("history", historyCheck.isSelected());

        state.addProperty("model", normalizedText(modelField));
        state.addProperty("period", normalizedInteger(periodField));
        state.addProperty("minimum", normalizedInteger(minimumField));
        state.addProperty("maximum", normalizedInteger(maximumField));
        state.addProperty("prompt", normalizedText(promptArea));

        return state;
    }

    private boolean hasUnsavedChanges() {
        return savedFormState != null
                && !savedFormState.equals(currentFormState());
    }

    private void updateSaveButton() {
        var user = SessionManager.getCurrentUser();

        boolean allowed = user != null && user.getRoleId() == 1;

        saveButton.setDisable(
                !allowed
                || configurationBusy
                || configId <= 0
                || !hasUnsavedChanges()
        );
    }

    private void applyConfiguration(JsonObject data) {
        configId = data.get("ai_config_id").getAsLong();

        enabledCheck.setSelected(
                getBoolean(data, "recommendation_enabled"));
        kpiCheck.setSelected(getBoolean(data, "use_kpi"));
        taskCheck.setSelected(
                getBoolean(data, "use_task_completion"));
        attendanceCheck.setSelected(
                getBoolean(data, "use_attendance"));
        deadlineCheck.setSelected(
                getBoolean(data, "use_missed_deadlines"));
        historyCheck.setSelected(
                getBoolean(data, "use_performance_history"));

        modelField.setText(getString(data, "model_name", ""));
        periodField.setText(
                getString(data, "analysis_period_days", "90"));
        minimumField.setText(
                getString(data, "minimum_data_points", "1"));
        maximumField.setText(
                getString(data, "max_recommendations", "5"));
        promptArea.setText(
                getString(data, "additional_prompt", ""));

        savedFormState = currentFormState().deepCopy();
    }
}
