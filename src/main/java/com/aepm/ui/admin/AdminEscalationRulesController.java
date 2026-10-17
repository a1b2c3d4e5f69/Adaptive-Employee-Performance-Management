package com.aepm.ui.admin;

import com.aepm.logic.deadline.EscalationRuleService;
import com.aepm.model.EscalationRule;
import com.aepm.util.SessionManager;
import com.aepm.ui.navigation.NavigationManager;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import com.aepm.ui.common.AEPMDialog;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class AdminEscalationRulesController {
    @FXML private TextField searchField;
    @FXML private Label totalRulesLabel;
    @FXML private Label activeRulesLabel;
    @FXML private Label inactiveRulesLabel;
    @FXML private Label statusLabel;
    @FXML private TableView<EscalationRule> rulesTable;
    @FXML private TableColumn<EscalationRule, String> nameColumn;
    @FXML private TableColumn<EscalationRule, Number> levelColumn;
    @FXML private TableColumn<EscalationRule, Number> missedColumn;
    @FXML private TableColumn<EscalationRule, String> actionColumn;
    @FXML private TableColumn<EscalationRule, Number> extensionColumn;
    @FXML private TableColumn<EscalationRule, String> statusColumn;
    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button refreshButton;
    @FXML private Button previewEscalationButton;
    @FXML private Button runEscalationButton;
    @FXML private Label escalationResultLabel;

    @FXML private HBox rulesSummary;
    @FXML private VBox rulesCard;
    @FXML private VBox rulesHelpCard;
    @FXML private VBox riskHistoryCard;

    @FXML private Button rulesTabButton;
    @FXML private Button riskTabButton;

    @FXML private TextField riskSearchField;
    @FXML private ComboBox<String> riskStatusFilter;
    @FXML private Button riskRefreshButton;
    @FXML private Label riskCountLabel;

    @FXML private TableView<RiskRecord> riskTable;

    @FXML private TableColumn<RiskRecord, Number> riskIdColumn;
    @FXML private TableColumn<RiskRecord, String> riskTaskColumn;
    @FXML private TableColumn<RiskRecord, String> riskEmployeeColumn;
    @FXML private TableColumn<RiskRecord, String> riskDepartmentColumn;
    @FXML private TableColumn<RiskRecord, String> riskDecisionColumn;
    @FXML private TableColumn<RiskRecord, String> riskStatusColumn;

    private final ObservableList<RiskRecord> allRisks =
        FXCollections.observableArrayList();

    private record RiskRecord(
        long id,
        String task,
        String employee,
        String department,
        String status,
        String decision,
        String note,
        String decidedAt
    ) {}


    private final EscalationRuleService service =
        new EscalationRuleService();
    private final ObservableList<EscalationRule> allRules =
        FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        rulesTable.setColumnResizePolicy(
            TableView.CONSTRAINED_RESIZE_POLICY
        );
        nameColumn.setCellValueFactory(c ->
            new ReadOnlyStringWrapper(c.getValue().ruleName()));
        levelColumn.setCellValueFactory(c ->
            new ReadOnlyObjectWrapper<Number>(
                c.getValue().escalationLevel()));
        missedColumn.setCellValueFactory(c ->
            new ReadOnlyObjectWrapper<Number>(
                c.getValue().missedDeadlineCount()));
        actionColumn.setCellValueFactory(c ->
            new ReadOnlyStringWrapper(c.getValue().actionType()));
        extensionColumn.setCellValueFactory(c ->
            new ReadOnlyObjectWrapper<Number>(
                c.getValue().extensionDays()));
        statusColumn.setCellValueFactory(c ->
            new ReadOnlyStringWrapper(c.getValue().status()));
        FilteredList<EscalationRule> filtered =
            new FilteredList<>(allRules, r -> true);
        rulesTable.setItems(filtered);
        searchField.textProperty().addListener(
            (obs, oldValue, keyword) -> {
                String query = keyword == null
                    ? "" : keyword.trim().toLowerCase();
                filtered.setPredicate(rule ->
                    query.isEmpty()
                    || rule.ruleName().toLowerCase().contains(query)
                    || rule.actionType().toLowerCase().contains(query)
                    || rule.status().toLowerCase().contains(query)
                );
            }
        );
        riskIdColumn.setCellValueFactory(c ->
            new ReadOnlyObjectWrapper<Number>(c.getValue().id()));

        riskTaskColumn.setCellValueFactory(c ->
            new ReadOnlyStringWrapper(c.getValue().task()));

        riskEmployeeColumn.setCellValueFactory(c ->
            new ReadOnlyStringWrapper(c.getValue().employee()));

        riskDepartmentColumn.setCellValueFactory(c ->
            new ReadOnlyStringWrapper(c.getValue().department()));

        riskDecisionColumn.setCellValueFactory(c ->
            new ReadOnlyStringWrapper(c.getValue().decision()));

        riskStatusColumn.setCellValueFactory(c ->
            new ReadOnlyStringWrapper(c.getValue().status()));

        riskTable.setColumnResizePolicy(
            TableView.CONSTRAINED_RESIZE_POLICY
        );

        riskStatusFilter.getItems().setAll(
            "All Status", "Active", "Resolved"
        );
        riskStatusFilter.setValue("All Status");

        riskSearchField.textProperty().addListener(
            (obs, oldValue, newValue) -> applyRiskFilters()
        );

        riskStatusFilter.valueProperty().addListener(
            (obs, oldValue, newValue) -> applyRiskFilters()
        );

        handleRulesTab();
        handleRefresh();
    }
    private void updateRuleCounts() {
        long active = allRules.stream()
                .filter(rule -> "Active".equalsIgnoreCase(rule.status()))
                .count();
        totalRulesLabel.setText(String.valueOf(allRules.size()));
        activeRulesLabel.setText(String.valueOf(active));
        inactiveRulesLabel.setText(String.valueOf(allRules.size() - active));
    }
    private void setBusy(boolean busy) {
        addButton.setDisable(busy);
        editButton.setDisable(busy);
        deleteButton.setDisable(busy);
        refreshButton.setDisable(busy);
    }

    @FXML
    private void handleRefresh() {
        setBusy(true);
        statusLabel.setText("Loading escalation rules...");
        new Thread(() -> {
            try {
                var result = service.getAll(
                    SessionManager.getAccessToken());
                Platform.runLater(() -> {
                    allRules.setAll(result);
                    updateRuleCounts();
                    statusLabel.setText(
                        result.isEmpty()
                            ? "No escalation rules found."
                            : result.size() + " rule(s) loaded."
                    );
                    setBusy(false);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    setBusy(false);
                    showError(
                        "Unable to retrieve escalation rules."
                    );
                });
            }
        }, "escalation-load").start();
    }

    @FXML
    private void handleAdd() {
        showRuleDialog(null);
    }

    @FXML
    private void handleEdit() {
        EscalationRule selected =
            rulesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select an escalation rule.");
            return;
        }
        showRuleDialog(selected);
    }
    private void showRuleDialog(EscalationRule existing) {
        Dialog<EscalationRule> dialog = new Dialog<>();
        dialog.setTitle(
            existing == null
                ? "Create Escalation Rule"
                : "Update Escalation Rule"
        );
        dialog.getDialogPane().getButtonTypes().addAll(
            ButtonType.OK,
            ButtonType.CANCEL
        );
        TextField nameField = new TextField(
            existing == null ? "" : existing.ruleName());
        TextField levelField = new TextField(
            existing == null
                ? "1"
                : String.valueOf(existing.escalationLevel()));
        TextField missedField = new TextField(
            existing == null
                ? "1"
                : String.valueOf(existing.missedDeadlineCount()));
        TextField actionField = new TextField(
            existing == null
                ? "Reminder"
                : existing.actionType());
        TextField extensionField = new TextField(
            existing == null
                ? "0"
                : String.valueOf(existing.extensionDays()));
        TextArea descriptionField = new TextArea(
            existing == null ? "" : existing.description());
        descriptionField.setPrefRowCount(3);
        descriptionField.setWrapText(true);
        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll("Active", "Inactive");
        statusBox.setValue(
            existing == null ? "Active" : existing.status());
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.addRow(0, new Label("Rule Name *"), nameField);
        grid.addRow(1, new Label("Escalation Level *"), levelField);
        grid.addRow(2, new Label("Missed Deadline Count *"), missedField);
        grid.addRow(3, new Label("Action Type *"), actionField);
        grid.addRow(4, new Label("Extension Days *"), extensionField);
        grid.addRow(5, new Label("Description"), descriptionField);
        grid.addRow(6, new Label("Status *"), statusBox);
        dialog.getDialogPane().setContent(grid);
        DialogPane pane = dialog.getDialogPane();
        pane.setPrefWidth(620);
        pane.setMinWidth(620);

        pane.setStyle(
            "-fx-background-color: #FFFFFF;" +
            "-fx-padding: 22;" +
            "-fx-font-family: 'Segoe UI';"
        );

        grid.setHgap(20);
        grid.setVgap(16);
        grid.setPadding(new Insets(12, 8, 12, 8));

        nameField.setPrefWidth(350);
        levelField.setPrefWidth(350);
        missedField.setPrefWidth(350);
        actionField.setPrefWidth(350);
        extensionField.setPrefWidth(350);
        descriptionField.setPrefWidth(350);
        statusBox.setPrefWidth(350);

        String inputStyle =
            "-fx-background-color: #F8FAFC;" +
            "-fx-border-color: #D9E2EF;" +
            "-fx-border-radius: 7;" +
            "-fx-background-radius: 7;" +
            "-fx-padding: 9 12;" +
            "-fx-font-size: 13px;";

        nameField.setStyle(inputStyle);
        levelField.setStyle(inputStyle);
        missedField.setStyle(inputStyle);
        actionField.setStyle(inputStyle);
        extensionField.setStyle(inputStyle);
        descriptionField.setStyle(inputStyle);
        statusBox.setStyle(inputStyle);

        Node okButton = pane.lookupButton(ButtonType.OK);
        Node cancelButton = pane.lookupButton(ButtonType.CANCEL);

        okButton.setStyle(
            "-fx-background-color: #2563EB;" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 7;" +
            "-fx-padding: 10 26;" +
            "-fx-font-weight: bold;"
        );

        cancelButton.setStyle(
            "-fx-background-color: #F1F5F9;" +
            "-fx-text-fill: #334155;" +
            "-fx-background-radius: 7;" +
            "-fx-padding: 10 26;"
        );
        Node saveButton = dialog.getDialogPane()
            .lookupButton(ButtonType.OK);
        saveButton.addEventFilter(
            ActionEvent.ACTION,
            event -> {
                try {
                    if (nameField.getText().isBlank()
                        || levelField.getText().isBlank()
                        || missedField.getText().isBlank()
                        || actionField.getText().isBlank()
                        || extensionField.getText().isBlank()
                        || statusBox.getValue() == null) {
                        throw new IllegalArgumentException(
                            "Please complete all required escalation rule fields."
                        );
                    }
                    int level = Integer.parseInt(
                        levelField.getText().trim());
                    int missed = Integer.parseInt(
                        missedField.getText().trim());
                    int extension = Integer.parseInt(
                        extensionField.getText().trim());
                    if (level < 1 || missed < 1 || extension < 0) {
                        throw new IllegalArgumentException(
                            "Level and missed count must be at least 1. "
                            + "Extension days cannot be negative."
                        );
                    }
                } catch (NumberFormatException ex) {
                    event.consume();
                    showError("Please enter valid whole numbers.");
                } catch (IllegalArgumentException ex) {
                    event.consume();
                    showError(ex.getMessage());
                }
            }
        );
        dialog.setResultConverter(button -> {
            if (button != ButtonType.OK) {
                return null;
            }
            return new EscalationRule(
                existing == null ? 0 : existing.ruleId(),
                nameField.getText().trim(),
                Integer.parseInt(levelField.getText().trim()),
                Integer.parseInt(missedField.getText().trim()),
                actionField.getText().trim(),
                Integer.parseInt(extensionField.getText().trim()),
                descriptionField.getText().trim(),
                statusBox.getValue(),
                existing == null ? null : existing.createdBy()
            );
        });
        dialog.showAndWait().ifPresent(rule ->
            saveRule(rule, existing == null)
        );
    }
    private void saveRule(
            EscalationRule rule,
            boolean isCreate) {
        setBusy(true);
        statusLabel.setText("Saving escalation rule...");
        new Thread(() -> {
            try {
                String token = SessionManager.getAccessToken();
                System.out.println(
                    "Escalation token exists: "
                    + (token != null && !token.isBlank())
                );

                System.out.println(
                    "Escalation token looks like JWT: "
                    + (token != null && token.split("\\.").length == 3)
                );
                if (isCreate) {
                    service.create(token, rule);
                } else {
                    service.update(token, rule);
                }
                Platform.runLater(() -> {
                    showInfo(
                        "Escalation rules updated successfully."
                    );
                    handleRefresh();
                });
            } catch (Exception ex) {
                ex.printStackTrace();

                Platform.runLater(() -> {
                    setBusy(false);

                    String message = ex.getMessage();

                    showError(
                        "Failed to save escalation rule.\n\n"
                        + (message == null ? ex.toString() : message)
                    );
                });
            }
        }, "escalation-save").start();
    }
    
    @FXML
    private void handleDelete() {
        EscalationRule selected =
            rulesTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showError("Please select an escalation rule.");
            return;
        }

        boolean confirmed = AEPMDialog.showConfirmation(
            rulesTable.getScene().getWindow(),
            "Delete Escalation Rule",
            "Are you sure you want to delete \""
                + selected.ruleName()
                + "\"?\nThis action cannot be undone."
        );

        if (!confirmed) {
            return;
        }

        setBusy(true);
        statusLabel.setText("Deleting escalation rule...");

        new Thread(() -> {
            try {
                service.delete(
                    SessionManager.getAccessToken(),
                    selected.ruleId()
                );

                Platform.runLater(() -> {
                    handleRefresh();
                    showInfo("Escalation rule deleted successfully.");
                });

            } catch (Exception ex) {
                ex.printStackTrace();

                Platform.runLater(() -> {
                    setBusy(false);
                    showError(
                        "Unable to delete escalation rule.\n\n"
                        + (ex.getMessage() == null
                            ? ex.toString()
                            : ex.getMessage())
                    );
                });
            }
        }, "escalation-delete").start();
    }

    private void showError(String message) {
        statusLabel.setText(message);

        AEPMDialog.showError(
            rulesTable.getScene().getWindow(),
            "Escalation Rule Error",
            message
        );
    }

    
    @FXML
    private void handlePreviewEscalations() {
        previewEscalationButton.setDisable(true);
        escalationResultLabel.setText("Checking overdue tasks...");

        Thread worker = new Thread(() -> {
            try {
                String token = SessionManager.getAccessToken();

                var previewService =
                    new com.aepm.logic.deadline.DeadlineEscalationPreviewService();

                var candidates = previewService.preview(token);

                StringBuilder result = new StringBuilder();
                result.append("Eligible escalations: ")
                    .append(candidates.size());

                for (var candidate : candidates) {
                    result.append("\n")
                        .append(candidate.taskTitle())
                        .append(" — ")
                        .append(candidate.ruleName())
                        .append(" (Level ")
                        .append(candidate.level())
                        .append(")");
                }

                String message = result.toString();

                Platform.runLater(() -> {
                    previewEscalationButton.setDisable(false);
                    escalationResultLabel.setText(message);
                });

            } catch (Exception ex) {
                ex.printStackTrace();

                Platform.runLater(() -> {
                    previewEscalationButton.setDisable(false);
                    escalationResultLabel.setText("Preview failed.");
                    showError(
                        "Unable to preview escalations.\n"
                        + ex.getMessage()
                    );
                });
            }
        }, "escalation-preview");

        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    private void handleRunEscalation() {
        boolean confirmed = AEPMDialog.showConfirmation(
            rulesTable.getScene().getWindow(),
            "Run Deadline Escalation",
            "Execute eligible escalation rules now?\n"
            + "This will create escalation history records."
        );

        if (!confirmed) {
            return;
        }

        runEscalationButton.setDisable(true);
        previewEscalationButton.setDisable(true);
        escalationResultLabel.setText("Running deadline escalation...");

        Thread worker = new Thread(() -> {
            try {
                String token = SessionManager.getAccessToken();

                var executionService =
                    new com.aepm.logic.deadline.DeadlineEscalationService();

                var result = executionService.execute(token);

                String message =
                    "Created: " + result.created()
                    + "   |   Skipped: " + result.skipped()
                    + "   |   Failed: " + result.failed();

                Platform.runLater(() -> {
                    runEscalationButton.setDisable(false);
                    previewEscalationButton.setDisable(false);
                    escalationResultLabel.setText(message);

                    if (result.failed() > 0) {
                        AEPMDialog.showError(
                            rulesTable.getScene().getWindow(),
                            "Escalation Completed with Errors",
                            message + "\nCheck the terminal for details."
                        );
                    } else {
                        AEPMDialog.showSuccess(
                            rulesTable.getScene().getWindow(),
                            "Escalation Completed",
                            message
                        );
                    }
                });

            } catch (Exception ex) {
                ex.printStackTrace();

                Platform.runLater(() -> {
                    runEscalationButton.setDisable(false);
                    previewEscalationButton.setDisable(false);
                    escalationResultLabel.setText("Escalation failed.");

                    showError(
                        "Unable to execute deadline escalation.\n"
                        + ex.getMessage()
                    );
                });
            }
        }, "escalation-execute");

        worker.setDaemon(true);
        worker.start();
    }

    private void showInfo(String message) {
        statusLabel.setText(message);

        AEPMDialog.showSuccess(
            rulesTable.getScene().getWindow(),
            "Operation Successful",
            message
        );
    }

    // ===== READ-ONLY PERFORMANCE RISK HISTORY =====
    @FXML
    private void handleRulesTab() {
        setRiskMode(false);
    }

    @FXML
    private void handleRiskTab() {
        setRiskMode(true);
        handleRiskRefresh();
    }

    private void setRiskMode(boolean riskMode) {

        rulesSummary.setVisible(!riskMode);
        rulesSummary.setManaged(!riskMode);

        rulesCard.setVisible(!riskMode);
        rulesCard.setManaged(!riskMode);

        rulesHelpCard.setVisible(!riskMode);
        rulesHelpCard.setManaged(!riskMode);

        riskHistoryCard.setVisible(riskMode);
        riskHistoryCard.setManaged(riskMode);

        String active =
            "-fx-background-color:#2563EB;" +
            "-fx-text-fill:white;" +
            "-fx-font-weight:bold;" +
            "-fx-background-radius:8;" +
            "-fx-padding:11 20;";

        String inactive =
            "-fx-background-color:#F8FAFD;" +
            "-fx-text-fill:#2563EB;" +
            "-fx-font-weight:bold;" +
            "-fx-border-color:#DCE5F2;" +
            "-fx-border-radius:8;" +
            "-fx-background-radius:8;" +
            "-fx-padding:11 20;";

        rulesTabButton.setStyle(riskMode ? inactive : active);
        riskTabButton.setStyle(riskMode ? active : inactive);
    }

    private void applyRiskFilters() {

        String query = riskSearchField.getText() == null
            ? ""
            : riskSearchField.getText().trim().toLowerCase();

        String status = riskStatusFilter.getValue();

        List<RiskRecord> visible = allRisks.stream()

            .filter(r ->
                query.isEmpty()
                || r.task().toLowerCase().contains(query)
                || r.employee().toLowerCase().contains(query)
                || r.department().toLowerCase().contains(query)
            )

            .filter(r ->
                status == null
                || "All Status".equals(status)
                || status.equalsIgnoreCase(r.status())
            )

            .toList();

        riskTable.setItems(
            FXCollections.observableArrayList(visible)
        );

        riskCountLabel.setText(
            visible.size() + " record(s)"
        );
    }

    private JSONArray fetchRiskRows(
        String token,
        String path
    ) throws Exception {

        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                "Please log in again."
            );
        }

        HttpRequest request = SupabaseClient.getInstance()
            .createAuthenticatedRequest(
                AppConfig.REST_URL + path,
                token
            )
            .GET()
            .build();

        HttpResponse<String> response =
            SupabaseClient.getInstance()
                .getHttpClient()
                .send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
                );

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {

            throw new IllegalStateException(
                "Supabase HTTP "
                + response.statusCode()
                + ": "
                + response.body()
            );
        }

        return new JSONArray(response.body());
    }

    private String encoded(String value) {
        return URLEncoder.encode(
            value,
            StandardCharsets.UTF_8
        );
    }

    @FXML
    private void handleRiskRefresh() {

        riskRefreshButton.setDisable(true);
        riskCountLabel.setText("Loading...");

        Thread worker = new Thread(() -> {

            try {

                String token =
                    SessionManager.getAccessToken();

                JSONArray rows = fetchRiskRows(
                    token,
                    "/task_escalation_tbl"
                    + "?select=escalation_id,task_id,employee_id,"
                    + "current_level,status,manager_decision,"
                    + "follow_up_note,decided_at"
                    + "&current_level=eq.3"
                    + "&order=escalation_id.desc"
                    + "&limit=500"
                );

                List<RiskRecord> records =
                    new ArrayList<>();

                for (int i = 0; i < rows.length(); i++) {

                    JSONObject row = rows.getJSONObject(i);

                    String task = "-";
                    String employee = "-";
                    String department = "-";

                    String taskId =
                        row.optString("task_id", "");

                    if (!taskId.isBlank()) {

                        JSONArray tasks = fetchRiskRows(
                            token,
                            "/task_tbl?select=task_title"
                            + "&task_id=eq."
                            + encoded(taskId)
                            + "&limit=1"
                        );

                        if (tasks.length() > 0) {
                            task = tasks.getJSONObject(0)
                                .optString("task_title", "-");
                        }
                    }

                    String employeeId =
                        row.optString("employee_id", "");

                    if (!employeeId.isBlank()) {

                        JSONArray employees = fetchRiskRows(
                            token,
                            "/user_tbl"
                            + "?select=full_name,employee_id,department"
                            + "&user_id=eq."
                            + encoded(employeeId)
                            + "&limit=1"
                        );

                        if (employees.length() > 0) {

                            JSONObject user =
                                employees.getJSONObject(0);

                            employee =
                                user.optString("full_name", "-");

                            String staffId =
                                user.optString("employee_id", "");

                            if (!staffId.isBlank()) {
                                employee += " (" + staffId + ")";
                            }

                            department =
                                user.optString("department", "-");
                        }
                    }

                    records.add(new RiskRecord(
                        row.getLong("escalation_id"),
                        task,
                        employee,
                        department,
                        row.optString("status", "-"),
                        row.optString("manager_decision", "Pending"),
                        row.optString("follow_up_note", ""),
                        row.optString("decided_at", "-")
                    ));
                }

                Platform.runLater(() -> {

                    allRisks.setAll(records);
                    applyRiskFilters();

                    riskRefreshButton.setDisable(false);
                });

            } catch (Exception ex) {

                ex.printStackTrace();

                Platform.runLater(() -> {

                    riskRefreshButton.setDisable(false);

                    riskCountLabel.setText(
                        "Unable to load risk history."
                    );

                    AEPMDialog.showError(
                        riskTable.getScene().getWindow(),
                        "Performance Risk History",
                        ex.getMessage()
                    );
                });
            }

        }, "admin-risk-history-load");

        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    private void handleRiskDetails() {

        RiskRecord selected =
            riskTable.getSelectionModel().getSelectedItem();

        if (selected == null) {

            AEPMDialog.showError(
                riskTable.getScene().getWindow(),
                "Performance Risk History",
                "Please select a risk record."
            );

            return;
        }

        Dialog<Void> dialog = new Dialog<>();

        dialog.setTitle(
            "Performance Risk #" + selected.id()
        );

        dialog.getDialogPane()
            .getButtonTypes()
            .add(ButtonType.CLOSE);

        VBox content = new VBox(14);
        content.setPadding(new Insets(22));

        content.setStyle(
            "-fx-background-color:white;"
        );

        Label heading =
            new Label("Performance Risk Details");

        heading.setStyle(
            "-fx-font-size:21px;" +
            "-fx-font-weight:bold;" +
            "-fx-text-fill:#10264A;"
        );

        content.getChildren().add(heading);

        String[][] details = {
            {"Risk ID", "#" + selected.id()},
            {"Task", selected.task()},
            {"Employee", selected.employee()},
            {"Department", selected.department()},
            {"Escalation Level", "Level 3"},
            {"Status", selected.status()},
            {"Manager Decision", selected.decision()},
            {"Decision Time", selected.decidedAt()}
        };

        for (String[] detail : details) {

            VBox field = new VBox(5);

            Label label = new Label(detail[0]);

            label.setStyle(
                "-fx-text-fill:#647DA3;" +
                "-fx-font-size:12px;"
            );

            Label value = new Label(detail[1]);

            value.setWrapText(true);

            value.setStyle(
                "-fx-text-fill:#10264A;" +
                "-fx-font-weight:bold;"
            );

            field.getChildren().addAll(label, value);

            field.setStyle(
                "-fx-background-color:#F8FAFD;" +
                "-fx-border-color:#E0E8F5;" +
                "-fx-border-radius:8;" +
                "-fx-background-radius:8;" +
                "-fx-padding:12;"
            );

            content.getChildren().add(field);
        }

        Label noteHeading =
            new Label("MANAGER FOLLOW-UP NOTE");

        noteHeading.setStyle(
            "-fx-text-fill:#647DA3;" +
            "-fx-font-weight:bold;"
        );

        Label note = new Label(
            selected.note().isBlank()
                ? "No follow-up note provided."
                : selected.note()
        );

        note.setWrapText(true);

        content.getChildren().addAll(
            noteHeading,
            note
        );

        Label policy = new Label(
            "Risk history only — no automatic score deduction."
        );

        policy.setWrapText(true);

        policy.setStyle(
            "-fx-text-fill:#64748B;"
        );

        content.getChildren().add(policy);

        ScrollPane scroll = new ScrollPane(content);

        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(560);

        dialog.getDialogPane().setContent(scroll);
        dialog.getDialogPane().setPrefWidth(620);

        dialog.showAndWait();
    }

    // ===== SIDEBAR NAVIGATION =====
    private void navigate(ActionEvent event, String path) {
        Stage stage = (Stage) ((Node) event.getSource())
                .getScene().getWindow();
        NavigationManager.navigate(stage, path);
    }

    @FXML
    private void handleHome(ActionEvent event) {
        navigate(event, "/fxml/admin/adminHome.fxml");
    }

    @FXML
    private void handleProfile(ActionEvent event) {
        navigate(event, "/fxml/admin/adminProfile.fxml");
    }

    @FXML
    private void handleUserManagement(ActionEvent event) {
        navigate(event, "/fxml/admin/adminUserManagement.fxml");
    }

    @FXML
    private void handleTaskManagement(ActionEvent event) {
        navigate(event, "/fxml/admin/adminTask.fxml");
    }

    @FXML
    private void handleKpiManagement(ActionEvent event) {
        navigate(event, "/fxml/admin/adminKpi.fxml");
    }

    @FXML
    private void handleSystemConfiguration(ActionEvent event) {
        navigate(event, "/fxml/admin/systemConfiguration.fxml");
    }

    @FXML
    private void handleAttendanceManagement(ActionEvent event) {
        navigate(event, "/fxml/admin/adminAttendance.fxml");
    }

    @FXML
    private void handleEscalationRules(ActionEvent event) {
        // Already on this page.
    }

    @FXML
    private void handleReports() {
        statusLabel.setText("Reports page is not available yet.");
    }

    @FXML
    private void handleAnonymousReports() {
        statusLabel.setText(
                "Anonymous Reports page is not available yet.");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        // Use the existing logout/session-clear logic
        // from AdminHomeController before navigating.
        navigate(event, "/fxml/auth/login.fxml");
    }
}
