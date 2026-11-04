package com.aepm.ui.components;

import com.aepm.logic.dashboard.DashboardDataService;
import com.aepm.util.SessionManager;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class DashboardAttentionPanel extends VBox {

    private final TableView<JSONObject> risks = new TableView<>();
    private final TableView<JSONObject> cases = new TableView<>();

    private final Label message = new Label();
    private final Button refresh = new Button("Refresh Details");
    private final Button open = new Button("View Selected Case");

    private long generation;
    private Object loadedSession;
    private boolean busy;

    public DashboardAttentionPanel() {
        setSpacing(12);
        setPadding(new Insets(20));
        getStyleClass().add("modern-panel");

        Label title = new Label(
                "Employees & Escalations Requiring Attention"
        );
        title.getStyleClass().add("modern-panel-title");
        title.setWrapText(true);
        message.setWrapText(true);

        for (Button button : List.of(refresh, open)) {
            button.getStyleClass().add("secondary-button");

            button.setStyle(
                "-fx-min-width:180px;"
                + "-fx-pref-width:180px;"
                + "-fx-max-width:180px;"
                + "-fx-min-height:40px;"
                + "-fx-pref-height:40px;"
                + "-fx-max-height:40px;"
                + "-fx-padding:0 16px;"
            );
        }

        column(risks, "Employee No.", "code", 120);
        column(risks, "Employee", "name", 180);
        column(risks, "Department", "department", 130);
        column(risks, "Latest KPI", "score", 100);
        column(risks, "Evaluation Period", "period", 240);

        column(cases, "Employee", "employee", 210);
        column(cases, "Task", "task", 240);
        column(cases, "Level", "level", 75);
        column(cases, "Current Due Date (MYT)", "deadline", 190);
        column(cases, "Manager Decision", "decision", 155);

        for (TableView<JSONObject> table : List.of(risks, cases)) {
            table.setMinHeight(240);
            table.setPrefHeight(280);
            table.setMaxHeight(280);
            table.setFixedCellSize(38);
            table.setColumnResizePolicy(
                    TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
            );
        }

        risks.setPlaceholder(
                new Label("Load details to view high-risk employees.")
        );
        cases.setPlaceholder(
                new Label("Load details to view active cases.")
        );

        open.setDisable(true);

        cases.getSelectionModel()
                .selectedItemProperty()
                .addListener((o, a, b) ->
                        open.setDisable(busy || b == null)
                );

        refresh.setOnAction(e -> reload());
        open.setOnAction(e -> openCase());

        cases.setRowFactory(table -> {
            TableRow<JSONObject> row = new TableRow<>();

            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    openCase();
                }
            });

            return row;
        });

        getChildren().addAll(
                title,
                refresh,
                message,
                new Label("High-Risk Employees — Latest Saved KPI"),
                risks,
                new Label("Active Escalation Cases"),
                cases,
                open
        );

        sceneProperty().addListener((o, oldScene, newScene) -> {
            generation++;
            busy = false;
            loadedSession = null;
            refresh.setDisable(false);

            risks.getItems().clear();
            cases.getItems().clear();

            if (newScene != null) {
                Platform.runLater(this::reload);
            }
        });
    }

    public void reload() {
        if (busy || getScene() == null) {
            return;
        }

        var user = SessionManager.getCurrentUser();
        String token = SessionManager.getAccessToken();

        if (user == null || token == null
                || (user.getRoleId() != 1 && user.getRoleId() != 2)) {
            risks.getItems().clear();
            cases.getItems().clear();
            message.setText(
                    "Administrator or Manager access is required."
            );
            return;
        }

        boolean admin = user.getRoleId() == 1;
        String actorId = UUID.fromString(user.getUserId()).toString();
        Object session = SessionManager.getAuthSession();
        long requestId = ++generation;

        busy = true;
        refresh.setDisable(true);
        open.setDisable(true);
        loadedSession = null;

        risks.getItems().clear();
        cases.getItems().clear();

        message.setText(
                "Loading employee and escalation details..."
        );

        Task<List<List<JSONObject>>> job = new Task<>() {

            private JSONArray read(
                    String table,
                    String fields,
                    String filter
            ) throws Exception {

                if (SessionManager.getAuthSession() != session) {
                    throw new IllegalStateException(
                            "Your session changed. Reload after signing in."
                    );
                }

                return new DashboardDataService().readAll(
                        token, table, fields, filter
                );
            }

            private JSONArray batches(
                    String table,
                    String fields,
                    String key,
                    Collection<String> ids,
                    String suffix
            ) throws Exception {

                JSONArray result = new JSONArray();
                List<String> values = new ArrayList<>(ids);

                for (int i = 0; i < values.size(); i += 40) {
                    List<String> batch = values.subList(
                            i, Math.min(i + 40, values.size())
                    );

                    for (String id : batch) {
                        UUID.fromString(id);
                    }

                    JSONArray rows = read(
                            table,
                            fields,
                            "&" + key + "=in.("
                                    + String.join(",", batch)
                                    + ")" + suffix
                    );

                    for (Object row : rows) {
                        result.put(row);
                    }
                }

                return result;
            }

            @Override
            protected List<List<JSONObject>> call() throws Exception {
                JSONArray users = read(
                        "user_tbl",
                        "user_id,employee_id,full_name,department",
                        "&role_id=eq.3"
                                + (admin
                                        ? ""
                                        : "&manager_id=eq." + actorId)
                                + "&order=user_id.asc"
                );

                Map<String, JSONObject> people = new LinkedHashMap<>();

                for (Object item : users) {
                    JSONObject person = (JSONObject) item;
                    people.put(person.getString("user_id"), person);
                }

                JSONArray kpis = batches(
                        "kpi_result_tbl",
                        "result_id,employee_id,overall_kpi_score,"
                                + "performance_status,"
                                + "evaluation_start_date,"
                                + "evaluation_end_date,calculated_at",
                        "employee_id",
                        people.keySet(),
                        "&order=calculated_at.desc,result_id.desc"
                );

                Set<String> seen = new HashSet<>();
                List<JSONObject> riskRows = new ArrayList<>();

                for (Object item : kpis) {
                    JSONObject kpi = (JSONObject) item;
                    String id = kpi.getString("employee_id");

                    if (!seen.add(id)
                            || !"At Risk".equalsIgnoreCase(
                                    text(kpi, "performance_status"))) {
                        continue;
                    }

                    JSONObject person = people.get(id);

                    if (person == null) {
                        continue;
                    }

                    riskRows.add(
                            new JSONObject()
                                    .put("code", text(person, "employee_id"))
                                    .put("name", text(person, "full_name"))
                                    .put("department",
                                            text(person, "department"))
                                    .put(
                                            "score",
                                            kpi.isNull("overall_kpi_score")
                                                    ? "—"
                                                    : String.format(
                                                            Locale.ENGLISH,
                                                            "%.2f",
                                                            kpi.getDouble(
                                                                    "overall_kpi_score"
                                                            )
                                                    )
                                    )
                                    .put(
                                            "period",
                                            text(kpi, "evaluation_start_date")
                                                    + " — "
                                                    + text(
                                                            kpi,
                                                            "evaluation_end_date"
                                                    )
                                    )
                    );
                }

                String taskFields =
                        "task_id,task_title,assigned_employee_id,"
                                + "due_date,task_status";

                JSONArray tasks = admin
                        ? read(
                                "task_tbl",
                                taskFields,
                                "&order=task_id.asc"
                        )
                        : batches(
                                "task_tbl",
                                taskFields,
                                "assigned_employee_id",
                                people.keySet(),
                                "&order=task_id.asc"
                        );

                Map<String, JSONObject> taskMap = new HashMap<>();

                for (Object item : tasks) {
                    JSONObject task = (JSONObject) item;
                    taskMap.put(task.getString("task_id"), task);
                }

                JSONArray escalations = batches(
                        "task_escalation_tbl",
                        "escalation_id,task_id,employee_id,"
                                + "current_level,status,manager_decision",
                        "task_id",
                        taskMap.keySet(),
                        "&status=eq.Active"
                                + "&current_level=in.(1,2,3)"
                                + "&order=escalation_id.desc"
                );

                List<JSONObject> caseRows = new ArrayList<>();

                for (Object item : escalations) {
                    JSONObject escalation = (JSONObject) item;
                    JSONObject task = taskMap.get(
                            escalation.getString("task_id")
                    );

                    String id = text(task, "assigned_employee_id");
                    JSONObject person = people.get(id);

                    String name = person == null
                            ? id
                            : text(person, "full_name")
                                    + " | "
                                    + text(person, "employee_id");

                    caseRows.add(
                            new JSONObject()
                                    .put("task_id",
                                            task.getString("task_id"))
                                    .put("escalation_id",
                                            escalation.getLong("escalation_id"))
                                    .put("employee", name)
                                    .put("task", text(task, "task_title"))
                                    .put("level",
                                            escalation.getInt("current_level"))
                                    .put("deadline",
                                            date(text(task, "due_date")))
                                    .put(
                                            "decision",
                                            escalation.isNull("manager_decision")
                                                    ? "Pending"
                                                    : text(
                                                            escalation,
                                                            "manager_decision"
                                                    )
                                    )
                    );
                }

                riskRows.sort(
                        Comparator.comparingDouble(r ->
                                "—".equals(text(r, "score"))
                                        ? Double.POSITIVE_INFINITY
                                        : Double.parseDouble(text(r, "score"))
                        )
                );

                caseRows.sort(
                        Comparator.comparingLong(
                                (JSONObject r) -> r.getLong("escalation_id")
                        ).reversed()
                );

                return List.of(riskRows, caseRows);
            }
        };

        job.setOnSucceeded(e -> {
            if (!current(requestId, session)) {
                return;
            }

            busy = false;
            refresh.setDisable(false);
            loadedSession = session;

            risks.getItems().setAll(job.getValue().get(0));
            cases.getItems().setAll(job.getValue().get(1));

            risks.setPlaceholder(
                    new Label("No high-risk employees identified.")
            );
            cases.setPlaceholder(
                    new Label("No active escalation cases.")
            );

            message.setText(
                    risks.getItems().size() + " high-risk employee(s), "
                            + cases.getItems().size() + " active case(s). "
                            + (admin
                                    ? "Organisation scope."
                                    : "Direct reports only.")
            );
        });

        job.setOnFailed(e -> {
            if (!current(requestId, session)) {
                return;
            }

            busy = false;
            refresh.setDisable(false);

            message.setText(
                    "Unable to load details. "
                            + job.getException().getMessage()
            );

            risks.setPlaceholder(
                    new Label(
                            "Details unavailable. Click Refresh Details to retry."
                    )
            );
            cases.setPlaceholder(
                    new Label(
                            "Details unavailable. Click Refresh Details to retry."
                    )
            );
        });

        Thread thread = new Thread(job, "dashboard-attention");
        thread.setDaemon(true);
        thread.start();
    }

    private boolean current(long requestId, Object session) {
        return generation == requestId
                && getScene() != null
                && SessionManager.getAuthSession() == session;
    }

    private void openCase() {
        JSONObject row = cases.getSelectionModel().getSelectedItem();

        if (busy || row == null || getScene() == null
                || loadedSession == null
                || loadedSession != SessionManager.getAuthSession()) {
            return;
        }

        TaskDetailsDialog.show(
                getScene().getWindow(),
                row.getString("task_id"),
                row.getLong("escalation_id")
        );
    }

    private static void column(
            TableView<JSONObject> table,
            String title,
            String key,
            double width
    ) {
        TableColumn<JSONObject, String> column =
                new TableColumn<>(title);

        column.setCellValueFactory(c ->
                new SimpleStringProperty(
                        text(c.getValue(), key)
                )
        );

        column.setPrefWidth(width);
        table.getColumns().add(column);
    }

    private static String text(JSONObject row, String key) {
        return row == null || row.isNull(key)
                ? "—"
                : row.optString(key, "—");
    }

    private static String date(String value) {
        try {
            return OffsetDateTime.parse(value)
                    .atZoneSameInstant(
                            ZoneId.of("Asia/Kuala_Lumpur")
                    )
                    .format(
                            DateTimeFormatter.ofPattern(
                                    "dd MMM yyyy HH:mm",
                                    Locale.ENGLISH
                            )
                    );
        } catch (RuntimeException error) {
            return value;
        }
    }
}