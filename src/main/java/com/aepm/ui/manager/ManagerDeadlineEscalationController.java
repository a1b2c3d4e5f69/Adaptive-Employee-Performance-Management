package com.aepm.ui.manager;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;
import com.aepm.ui.navigation.NavigationManager;
import com.aepm.util.SessionManager;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.scene.control.TableView;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

import com.aepm.model.User;

public class ManagerDeadlineEscalationController {

    @FXML private Label topRoleLabel;
    @FXML private Label totalLabel;
    @FXML private Label activeLabel;
    @FXML private Label resolvedLabel;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> levelFilter;
    @FXML private ComboBox<String> statusFilter;
    @FXML private TableView<Record> escalationTable;
    @FXML private TableColumn<Record, String> taskColumn;
    @FXML private TableColumn<Record, String> employeeColumn;
    @FXML private TableColumn<Record, String> levelColumn;
    @FXML private TableColumn<Record, String> decisionColumn;
    @FXML private TableColumn<Record, String> statusColumn;
    @FXML private TableColumn<Record, String> dateColumn;

    @FXML private Button escalationTabButton;
    @FXML private Button riskTabButton;
    @FXML private Label recordsTitleLabel;

    private boolean riskHistoryMode = false;

    private final ObservableList<Record> allRecords =
        FXCollections.observableArrayList();

    private final SupabaseClient client =
        SupabaseClient.getInstance();

    public record Record(
        long id,
        String task,
        String employee,
        int level,
        String decision,
        String status,
        String date,
        String previousDeadline,
        String newDeadline,
        String note
    ) {}

    @FXML
    private void initialize() {
        updateManagerRoleLabel();
        escalationTable.setColumnResizePolicy(
            TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS
        );

        taskColumn.setCellValueFactory(c ->
            new ReadOnlyStringWrapper(c.getValue().task()));

        employeeColumn.setCellValueFactory(c ->
            new ReadOnlyStringWrapper(c.getValue().employee()));

        levelColumn.setCellValueFactory(c ->
            new ReadOnlyStringWrapper(
                "Level " + c.getValue().level()));

        decisionColumn.setCellValueFactory(c ->
            new ReadOnlyStringWrapper(c.getValue().decision()));

        statusColumn.setCellValueFactory(c ->
            new ReadOnlyStringWrapper(c.getValue().status()));

        dateColumn.setCellValueFactory(c ->
            new ReadOnlyStringWrapper(c.getValue().date()));

        levelFilter.getItems().setAll(
            "All Levels", "Level 1", "Level 2", "Level 3"
        );
        levelFilter.setValue("All Levels");

        statusFilter.getItems().setAll(
            "All Status", "Active", "Resolved"
        );
        statusFilter.setValue("All Status");

        searchField.textProperty().addListener(
            (obs, oldValue, newValue) -> applyFilters());

        levelFilter.valueProperty().addListener(
            (obs, oldValue, newValue) -> applyFilters());

        statusFilter.valueProperty().addListener(
            (obs, oldValue, newValue) -> applyFilters());

        updateTabAppearance();
        handleRefresh();
    }

    @FXML
    private void handleEscalationTab() {
        riskHistoryMode = false;

        levelFilter.setDisable(false);
        levelFilter.setValue("All Levels");
        statusFilter.setValue("All Status");

        recordsTitleLabel.setText("Escalation Records");

        updateTabAppearance();
        applyFilters();
    }

    @FXML
    private void handleRiskTab() {
        riskHistoryMode = true;

        levelFilter.setValue("Level 3");
        levelFilter.setDisable(true);
        statusFilter.setValue("All Status");

        recordsTitleLabel.setText("Performance Risk History");

        updateTabAppearance();
        applyFilters();
    }

    private void updateTabAppearance() {

        String activeStyle =
            "-fx-background-color: #2563EB;" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 11 20;";

        String inactiveStyle =
            "-fx-background-color: #F8FAFD;" +
            "-fx-text-fill: #2563EB;" +
            "-fx-font-weight: bold;" +
            "-fx-border-color: #DCE5F2;" +
            "-fx-border-radius: 8;" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 11 20;";

        escalationTabButton.setStyle(
            riskHistoryMode ? inactiveStyle : activeStyle
        );

        riskTabButton.setStyle(
            riskHistoryMode ? activeStyle : inactiveStyle
        );
    }       
    private String encode(String value) {
        return URLEncoder.encode(
            value, StandardCharsets.UTF_8);
    }

    private JSONArray getRows(
        String token,
        String url
    ) throws Exception {

        HttpRequest request = client
            .createAuthenticatedRequest(url, token)
            .GET()
            .build();

        HttpResponse<String> response =
            client.getHttpClient().send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );

        if (response.statusCode() < 200 ||
            response.statusCode() >= 300) {

            throw new IllegalStateException(
                "Supabase HTTP " + response.statusCode()
                + ": " + response.body()
            );
        }

        return new JSONArray(response.body());
    }

    private Map<String, JSONObject> getRowsByIds(
        String token,
        String table,
        String idColumn,
        String selectColumns,
        Set<String> ids
    ) throws Exception {

        Map<String, JSONObject> result = new HashMap<>();

        List<String> idList = new ArrayList<>(ids);

        // Batch requests to avoid very long URLs
        for (int start = 0; start < idList.size(); start += 50) {

            List<String> batch = idList.subList(
                    start,
                    Math.min(start + 50, idList.size())
            );

            String values = batch.stream()
                    .map(this::encode)
                    .collect(Collectors.joining(","));

            String url = AppConfig.REST_URL + "/" + table
                    + "?select=" + selectColumns
                    + "&" + idColumn + "=in.(" + values + ")";

            JSONArray rows = getRows(token, url);

            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                String id = optional(row, idColumn);

                if (!id.isBlank()) {
                    result.put(id, row);
                }
            }
        }

        return result;
    }

    private String optional(JSONObject object, String key) {
        return object.isNull(key)
            ? ""
            : object.optString(key, "");
    }

    private String formatDate(String value) {

        if (value == null || value.isBlank()) {
            return "-";
        }

        try {
            return OffsetDateTime.parse(value)
                .atZoneSameInstant(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern(
                    "dd MMM yyyy, hh:mm a"));
        } catch (Exception ex) {
            return value;
        }
    }

    @FXML
    private void handleRefresh() {

        String token = SessionManager.getAccessToken();

        if (token == null || token.isBlank()) {
            showError("Please log in again.");
            return;
        }

        escalationTable.setPlaceholder(
                new Label("Loading escalation records...")
        );

        Thread worker = new Thread(() -> {

            try {
                // 1. Load escalation records
                JSONArray escalations = getRows(
                        token,
                        AppConfig.REST_URL + "/task_escalation_tbl"
                                + "?select=escalation_id,task_id,employee_id,"
                                + "current_level,manager_decision,status,"
                                + "previous_deadline,new_deadline,follow_up_note"
                                + "&order=escalation_id.desc&limit=500"
                );

                // 2. Collect unique Task IDs and Employee IDs
                Set<String> taskIds = new LinkedHashSet<>();
                Set<String> employeeIds = new LinkedHashSet<>();

                for (int i = 0; i < escalations.length(); i++) {

                    JSONObject escalation = escalations.getJSONObject(i);

                    String taskId = optional(escalation, "task_id");
                    String employeeId = optional(escalation, "employee_id");

                    if (!taskId.isBlank()) {
                        taskIds.add(taskId);
                    }

                    if (!employeeId.isBlank()) {
                        employeeIds.add(employeeId);
                    }
                }

                // 3. Load tasks in batches
                Map<String, JSONObject> tasks = getRowsByIds(
                        token,
                        "task_tbl",
                        "task_id",
                        "task_id,task_title",
                        taskIds
                );

                // 4. Load employees in batches
                Map<String, JSONObject> employees = getRowsByIds(
                        token,
                        "user_tbl",
                        "user_id",
                        "user_id,full_name,employee_id",
                        employeeIds
                );

                // 5. Build records using cached data
                List<Record> loaded = new ArrayList<>();

                for (int i = 0; i < escalations.length(); i++) {

                    JSONObject escalation = escalations.getJSONObject(i);

                    String taskId = optional(escalation, "task_id");
                    String employeeId = optional(escalation, "employee_id");

                    String taskName = taskId;
                    String employeeName = employeeId;

                    JSONObject task = tasks.get(taskId);

                    if (task != null) {
                        taskName = task.optString("task_title", taskId);
                    }

                    JSONObject employee = employees.get(employeeId);

                    if (employee != null) {

                        employeeName = employee.optString(
                                "full_name",
                                "Employee"
                        );

                        String employeeCode = optional(
                                employee,
                                "employee_id"
                        );

                        if (!employeeCode.isBlank()) {
                            employeeName += " (" + employeeCode + ")";
                        }
                    }

                    String decision = optional(
                            escalation,
                            "manager_decision"
                    );

                    loaded.add(new Record(
                            escalation.getLong("escalation_id"),
                            taskName,
                            employeeName,
                            escalation.optInt("current_level", 0),
                            decision.isBlank() ? "Pending" : decision,
                            optional(escalation, "status"),
                            formatDate(optional(escalation, "previous_deadline")),
                            formatDate(optional(escalation, "previous_deadline")),
                            formatDate(optional(escalation, "new_deadline")),
                            optional(escalation, "follow_up_note")
                    ));
                }

                // 6. Update JavaFX UI
                Platform.runLater(() -> {

                    allRecords.setAll(loaded);

                    totalLabel.setText(
                            String.valueOf(loaded.size())
                    );

                    activeLabel.setText(
                            String.valueOf(
                                    loaded.stream()
                                            .filter(r ->
                                                    "Active".equalsIgnoreCase(
                                                            r.status()
                                                    )
                                            )
                                            .count()
                            )
                    );

                    resolvedLabel.setText(
                            String.valueOf(
                                    loaded.stream()
                                            .filter(r ->
                                                    "Resolved".equalsIgnoreCase(
                                                        r.status()
                                                    )
                                            )
                                            .count()
                            )
                    );

                    applyFilters();
                });

            } catch (Exception ex) {

                ex.printStackTrace();

                Platform.runLater(() -> {
                    escalationTable.setPlaceholder(
                            new Label("Unable to load records.")
                    );
                    showError(ex.getMessage());
                });
            }

        }, "manager-escalation-history");

        worker.setDaemon(true);
        worker.start();
    }

    private void applyFilters() {

        String query = searchField.getText() == null
            ? ""
            : searchField.getText().trim().toLowerCase();

        String level = levelFilter.getValue();
        String status = statusFilter.getValue();

        escalationTable.setItems(
            FXCollections.observableArrayList(
                allRecords.stream()

                    .filter(r ->
                        query.isEmpty()
                        || r.task().toLowerCase().contains(query)
                        || r.employee().toLowerCase().contains(query)
                    )

                    // Performance Risk History only displays Level 3.
                    .filter(r ->
                        !riskHistoryMode || r.level() == 3
                    )

                    // Keep existing Level filter behaviour.
                    .filter(r ->
                        riskHistoryMode
                        || level == null
                        || level.equals("All Levels")
                        || level.equals("Level " + r.level())
                    )

                    // Keep existing Status filter.
                    .filter(r ->
                        status == null
                        || status.equals("All Status")
                        || status.equalsIgnoreCase(r.status())
                    )

                    .toList()
            )
        );
    }

    @FXML
    private void handleViewDetails() {

        Record record = escalationTable
            .getSelectionModel()
            .getSelectedItem();

        if (record == null) {
            new Alert(
                Alert.AlertType.INFORMATION,
                "Please select an escalation record first.",
                ButtonType.OK
            ).showAndWait();
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Escalation Details");
        dialog.setHeaderText(null);
        dialog.initOwner(escalationTable.getScene().getWindow());

        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().add(ButtonType.CLOSE);
        pane.setPrefSize(620, 600);
        pane.setStyle(
            "-fx-background-color: #F6F8FC;"
        );

        Button defaultClose =
            (Button) pane.lookupButton(ButtonType.CLOSE);
        defaultClose.setVisible(false);
        defaultClose.setManaged(false);

        Label title = new Label("Escalation Details");
        title.setStyle(
            "-fx-font-size: 22px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #10264A;"
        );

        Label subtitle = new Label(
            "Review the escalation record and manager decision."
        );
        subtitle.setStyle(
            "-fx-font-size: 12px;" +
            "-fx-text-fill: #7184A4;"
        );

        VBox heading = new VBox(5, title, subtitle);

        Label levelBadge = new Label("LEVEL " + record.level());
        levelBadge.setStyle(
            "-fx-background-color: #E8F0FF;" +
            "-fx-text-fill: #2563EB;" +
            "-fx-background-radius: 20;" +
            "-fx-padding: 7 14;" +
            "-fx-font-size: 11px;" +
            "-fx-font-weight: bold;"
        );

        Label statusBadge = new Label(
            record.status().toUpperCase()
        );

        boolean resolved =
            "Resolved".equalsIgnoreCase(record.status());

        statusBadge.setStyle(
            "-fx-background-color: " +
                (resolved ? "#E7F7ED;" : "#FFF2DC;") +
            "-fx-text-fill: " +
                (resolved ? "#16834A;" : "#A75A08;") +
            "-fx-background-radius: 20;" +
            "-fx-padding: 7 14;" +
            "-fx-font-size: 11px;" +
            "-fx-font-weight: bold;"
        );

        HBox badges = new HBox(8, levelBadge, statusBadge);

        VBox header = new VBox(13, heading, badges);
        header.setPadding(new javafx.geometry.Insets(22));
        header.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 14;"
        );

        Label taskTitle = new Label(record.task());
        taskTitle.setWrapText(true);
        taskTitle.setStyle(
            "-fx-font-size: 19px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #10264A;"
        );

        VBox taskSection = new VBox(
            7,
            detailSectionTitle("TASK INFORMATION"),
            taskTitle,
            detailField("Assigned Employee", record.employee())
        );

        VBox decisionSection = new VBox(
            10,
            detailSectionTitle("ESCALATION INFORMATION"),
            detailField("Escalation ID", "#" + record.id()),
            detailField("Manager Decision", record.decision()),
            detailField("Escalated At", record.date())
        );

        javafx.scene.layout.GridPane dates =
            new javafx.scene.layout.GridPane();

        dates.setHgap(12);
        dates.setVgap(10);

        javafx.scene.layout.ColumnConstraints col1 =
            new javafx.scene.layout.ColumnConstraints();
        col1.setPercentWidth(50);

        javafx.scene.layout.ColumnConstraints col2 =
            new javafx.scene.layout.ColumnConstraints();
        col2.setPercentWidth(50);

        dates.getColumnConstraints().addAll(col1, col2);

        dates.add(
            detailField("Previous Deadline", record.previousDeadline()),
            0, 0
        );

        dates.add(
            detailField("New Deadline", record.newDeadline()),
            1, 0
        );

        VBox deadlineSection = new VBox(
            10,
            detailSectionTitle("DEADLINE INFORMATION"),
            dates
        );

        Label noteText = new Label(
            record.note() == null || record.note().isBlank()
                ? "No follow-up note provided."
                : record.note()
        );

        noteText.setWrapText(true);
        noteText.setMaxWidth(Double.MAX_VALUE);
        noteText.setStyle(
            "-fx-text-fill: #334B70;" +
            "-fx-font-size: 13px;"
        );

        VBox noteBox = new VBox(
            10,
            detailSectionTitle("MANAGER FOLLOW-UP NOTE"),
            noteText
        );

        VBox content = new VBox(
            22,
            taskSection,
            decisionSection,
            deadlineSection,
            noteBox
        );

        content.setPadding(new javafx.geometry.Insets(22));
        content.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 14;"
        );

        VBox body = new VBox(14, header, content);
        body.setPadding(new javafx.geometry.Insets(18));

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle(
            "-fx-background: #F6F8FC;" +
            "-fx-background-color: #F6F8FC;" +
            "-fx-border-color: transparent;"
        );

        Button closeButton = new Button("Close");
        closeButton.setStyle(
            "-fx-background-color: #2563EB;" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 9;" +
            "-fx-padding: 11 30;" +
            "-fx-cursor: hand;"
        );
        closeButton.setOnAction(e -> dialog.close());

        HBox footer = new HBox(closeButton);
        footer.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        footer.setPadding(new javafx.geometry.Insets(14, 20, 14, 20));
        footer.setStyle("-fx-background-color: white;");

        VBox layout = new VBox(scroll, footer);
        VBox.setVgrow(scroll, javafx.scene.layout.Priority.ALWAYS);

        pane.setContent(layout);

        dialog.showAndWait();
    }


    private Label detailSectionTitle(String text) {
        Label label = new Label(text);
        label.setStyle(
            "-fx-font-size: 11px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #7184A4;"
        );
        return label;
    }

    private VBox detailField(String heading, String value) {

        Label title = new Label(heading);
        title.setStyle(
            "-fx-font-size: 11px;" +
            "-fx-text-fill: #7184A4;"
        );

        Label content = new Label(
            value == null || value.isBlank() ? "-" : value
        );

        content.setWrapText(true);
        content.setStyle(
            "-fx-font-size: 13px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #10264A;"
        );

        VBox box = new VBox(6, title, content);
        box.setPadding(new javafx.geometry.Insets(12));
        box.setMaxWidth(Double.MAX_VALUE);

        box.setStyle(
            "-fx-background-color: #F8FAFD;" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: #E3EAF4;" +
            "-fx-border-radius: 10;"
        );

        return box;
    }

    private void showError(String message) {
        new Alert(
            Alert.AlertType.ERROR,
            message,
            ButtonType.OK
        ).showAndWait();
    }

    private void navigate(String path) {

        Stage stage = (Stage)
            escalationTable.getScene().getWindow();

        NavigationManager.navigate(stage, path);
    }

    @FXML
    private void handleHome() {
        navigate("/fxml/manager/managerHome.fxml");
    }

    @FXML
    private void handleProfile() {
        navigate("/fxml/manager/managerProfile.fxml");
    }

    @FXML
    private void handleTaskManagement() {
        navigate("/fxml/manager/managerTask.fxml");
    }

    @FXML
    private void handleDeadlineEscalation() {
        // Already on Deadline Escalation page.
    }

    @FXML
    private void handlePerformance() {
        navigate("/fxml/manager/managerPerformance.fxml");
    }

    @FXML
    private void handleReports() {
        System.out.println("Reports not implemented yet.");
    }

    @FXML
    private void handleDashboard() {
        System.out.println("Dashboard not implemented yet.");
    }

    @FXML
    private void handleLogout() {
        SessionManager.clearSession();
        navigate("/fxml/auth/login.fxml");
    }

    private void updateManagerRoleLabel() {
        User manager = SessionManager.getCurrentUser();

        if (manager == null) {
                topRoleLabel.setText("MANAGER");
                return;
        }

        String managerId = manager.getEmployeeId();

        if (managerId != null && !managerId.isBlank()) {
                topRoleLabel.setText("MANAGER  •  " + managerId);
        } else {
                topRoleLabel.setText("MANAGER");
        }
    }
}
