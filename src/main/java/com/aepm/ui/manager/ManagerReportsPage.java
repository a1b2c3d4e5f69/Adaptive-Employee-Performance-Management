package com.aepm.ui.manager;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;
import com.aepm.ui.common.AEPMDialog;
import com.aepm.ui.navigation.NavigationManager;
import com.aepm.util.SessionManager;
import com.google.gson.*;

import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ManagerReportsPage {

    private static final int PAGE_SIZE = 50;

    private final Stage stage;
    private final BorderPane shell;
    private final BorderPane page = new BorderPane();

    private final Object session = SessionManager.getAuthSession();
    private final String token = SessionManager.getAccessToken();

    private final ComboBox<Choice> employee = new ComboBox<>();
    private final ComboBox<Choice> department = new ComboBox<>();
    private final ComboBox<Choice> period = new ComboBox<>();

    private final Button apply = new Button("Apply Filters");
    private final Button clear = new Button("Clear Filters");
    private final Button refresh = new Button("Refresh");
    private final Button previous = new Button("Previous");
    private final Button next = new Button("Next");
    private final Button exportPdf = new Button("Export PDF");
    private final Button exportExcel = new Button("Export Excel");

    private final Label status = new Label();
    private final Label pageInfo = new Label("No reports loaded.");
    private final TableView<JsonObject> table = new TableView<>();

    private boolean busy;
    private boolean optionsLoaded;
    private int offset;
    private long total;

    private final ComboBox<Choice> trendEmployee = new ComboBox<>();
    private final Button loadTrendButton = new Button("Load Trend");
    private final Label trendMessage = new Label("Select an employee.");

    private final CategoryAxis trendXAxis = new CategoryAxis();
    private final NumberAxis trendYAxis = new NumberAxis(0, 100, 20);
    private final LineChart<String, Number> trendChart =
            new LineChart<>(trendXAxis, trendYAxis);

    private record Choice(String label, JsonObject value) {
        @Override
        public String toString() {
            return label;
        }
    }

    public static void show(Window owner) {
        var user = SessionManager.getCurrentUser();

        if (user == null
                || user.getRoleId() != 2L
                || SessionManager.getAccessToken() == null) {
            AEPMDialog.showError(
                    owner,
                    "Access Denied",
                    "Please sign in with a manager account."
            );
            return;
        }

        if (!(owner instanceof Stage mainStage)
                || !(mainStage.getScene().getRoot()
                    instanceof BorderPane layout)) {
            AEPMDialog.showError(
                    owner,
                    "Unable to Open Reports",
                    "Open Reports from a manager page."
            );
            return;
        }

        new ManagerReportsPage(mainStage, layout).open();
    }

    private ManagerReportsPage(Stage stage, BorderPane shell) {
        this.stage = stage;
        this.shell = shell;

        Label heading = label("Performance & KPI Reports");
        heading.setStyle(
                "-fx-font-size: 28px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: #0F172A;"
        );

        Label subtitle = label(
                "Review performance results for your direct reports."
        );
        subtitle.setStyle("-fx-text-fill: #64748B;");

        refresh.getStyleClass().add("secondary-button");
        refresh.setOnAction(event -> loadOptions());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox titleRow = new HBox(12, heading, spacer, refresh);
        titleRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        configureFilter(employee, "All Employees");
        configureFilter(department, "All Departments");
        configureFilter(period, "All Evaluation Periods");

        apply.getStyleClass().add("login-primary-button");
        clear.getStyleClass().add("secondary-button");
        for (Button button : new Button[]{apply, clear}) {
            button.setStyle(
                    "-fx-min-width: 150px;"
                    + "-fx-pref-width: 150px;"
                    + "-fx-max-width: 150px;"
                    + "-fx-min-height: 44px;"
                    + "-fx-pref-height: 44px;"
                    + "-fx-max-height: 44px;"
                    + "-fx-font-size: 14px;"
                    + "-fx-font-weight: bold;"
                    + "-fx-padding: 0 16px;"
                    + "-fx-alignment: CENTER;"
                    + "-fx-background-insets: 0;"
                    + "-fx-border-insets: 0;"
                    + "-fx-background-radius: 10px;"
                    + "-fx-border-radius: 10px;"
            );
        }

        apply.setOnAction(event -> loadReports(0));

        clear.setOnAction(event -> {
            employee.getSelectionModel().selectFirst();
            department.getSelectionModel().selectFirst();
            period.getSelectionModel().selectFirst();
            loadReports(0);
        });

        FlowPane filters = new FlowPane(
                16, 12,
                filterBox("Employee", employee),
                filterBox("Department", department),
                filterBox("Evaluation Period", period)
        );

        Label note = label(
                "Each employee and evaluation period shows the latest "
                + "calculated result. Missing scores appear as —."
                + "\nTeam membership and department reflect current records."
        );
        note.setStyle("-fx-text-fill: #64748B;");

        VBox filterCard = new VBox(
                14, filters, new HBox(10, apply, clear), note
        );
        filterCard.setPadding(new Insets(20));
        filterCard.setStyle(
                "-fx-background-color: white;"
                + "-fx-background-radius: 16;"
                + "-fx-border-color: #E1E6EF;"
                + "-fx-border-radius: 16;"
        );

        VBox header = new VBox(10, titleRow, subtitle);
        header.setPadding(new Insets(0, 0, 18, 0));
        page.setTop(header);

        addColumn("Employee No.", 120,
                row -> value(row, "employee_number"));
        addColumn("Employee", 170, row -> value(row, "full_name"));
        addColumn("Department", 130, row -> value(row, "department"));
        addColumn("Evaluation Period", 230, row ->
                date(value(row, "evaluation_start_date"))
                        + " – "
                        + date(value(row, "evaluation_end_date"))
        );
        addColumn("Productivity", 115,
                row -> score(row, "productivity_score"));
        addColumn("Timeliness", 110,
                row -> score(row, "timeliness_score"));
        addColumn("Completion", 110,
                row -> score(row, "completion_rate"));
        addColumn("Attendance", 110,
                row -> score(row, "attendance_score"));
        addColumn("Overall KPI", 115,
                row -> score(row, "overall_kpi_score"));
        addColumn("Performance", 140,
                row -> value(row, "performance_status"));
        addColumn("KPI Configuration", 180,
                row -> value(row, "config_name"));

        // Allow horizontal scrolling rather than squeezing score columns.
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Loading report options..."));

        previous.getStyleClass().add("secondary-button");
        next.getStyleClass().add("secondary-button");

        previous.setOnAction(event ->
                loadReports(Math.max(0, offset - PAGE_SIZE))
        );
        next.setOnAction(event -> loadReports(offset + PAGE_SIZE));

        pageInfo.setStyle("-fx-text-fill: #475569;");
        status.setStyle("-fx-text-fill: #475569;");
        status.setWrapText(true);

        HBox pagination = new HBox(12, previous, pageInfo, next);
        pagination.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        table.setMinHeight(320);
        table.setPrefHeight(420);
        table.setMaxHeight(Double.MAX_VALUE);

        BorderPane reportContent = new BorderPane();
        reportContent.setTop(filterCard);
        reportContent.setCenter(table);
        reportContent.setBottom(pagination);
        for (Button button : new Button[]{exportPdf, exportExcel}) {
            button.setStyle(
                    "-fx-min-width: 136px;"
                    + "-fx-pref-width: 136px;"
                    + "-fx-max-width: 136px;"
                    + "-fx-min-height: 40px;"
                    + "-fx-pref-height: 40px;"
                    + "-fx-max-height: 40px;"
                    + "-fx-background-color: white;"
                    + "-fx-text-fill: #2563EB;"
                    + "-fx-border-color: #D8E1EE;"
                    + "-fx-border-radius: 8px;"
                    + "-fx-background-radius: 8px;"
                    + "-fx-font-size: 14px;"
                    + "-fx-font-weight: bold;"
                    + "-fx-padding: 0 12px;"
                    + "-fx-cursor: hand;"
            );
        }

        exportPdf.setOnAction(event -> exportReport(true));
        exportExcel.setOnAction(event -> exportReport(false));

        Button clearSelection = new Button("Clear Selection");

        clearSelection.setStyle(exportExcel.getStyle());

        clearSelection.setOnAction(event -> {
            table.getSelectionModel().clearSelection();
            status.setText(
                    "Row selection cleared. Export will use the filters above."
            );
        });

        HBox exportActions = new HBox(
                12,
                exportPdf,
                exportExcel,
                clearSelection
        );
        exportActions.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        VBox reportFooter = new VBox(
                12,
                pagination,
                exportActions
        );
        reportFooter.setPadding(new Insets(0, 0, 8, 0));

        reportContent.setBottom(reportFooter);

        BorderPane.setMargin(
                table,
                new Insets(16, 0, 16, 0)
        );

        Tab reportTab = new Tab(
                "Report Table",
                scrollReportContent(reportContent)
        );

        Tab trendTab = new Tab(
                "KPI Trend",
                scrollReportContent(buildTrendContent())
        );

        TabPane reportTabs = new TabPane(reportTab, trendTab);
        reportTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        reportTabs.getStyleClass().add("report-tabs");

        page.setCenter(reportTabs);

        status.setPadding(new Insets(14, 0, 0, 0));
        page.setBottom(status);

        page.setPadding(new Insets(28));
        page.setStyle("-fx-background-color: #F6F8FC;");

        employee.valueProperty().addListener(
                (obs, old, selected) -> invalidateResults()
        );
        department.valueProperty().addListener(
                (obs, old, selected) -> invalidateResults()
        );
        period.valueProperty().addListener(
                (obs, old, selected) -> invalidateResults()
        );

        updateControls();
    }

    private void open() {
        shell.setCenter(page);
        configureSidebar();
        loadOptions();
    }

    private void configureSidebar() {
        if (shell.getLeft() == null) {
            return;
        }

        for (javafx.scene.Node node :
                shell.getLeft().lookupAll(".button")) {
            if (!(node instanceof Button button)) {
                continue;
            }

            String text = button.getText();
            String destination = null;

            if (text.contains("Reports")) {
                button.getStyleClass().remove("sidebar-button-active");
                button.getStyleClass().add("sidebar-button-active");
                button.setOnAction(event -> {
                    // Already on Reports.
                });
                continue;
            }

            if (text.contains("Home")) {
                destination = "/fxml/manager/managerHome.fxml";
            } else if (text.contains("My Profile")) {
                destination = "/fxml/manager/managerProfile.fxml";
            } else if (text.contains("Task Management")) {
                destination = "/fxml/manager/managerTask.fxml";
            } else if (text.contains("Deadline Escalation")) {
                destination = "/fxml/manager/managerDeadlineEscalation.fxml";
            } else if (text.contains("Performance")) {
                destination = "/fxml/manager/managerPerformance.fxml";
            }

            if (destination != null) {
                button.getStyleClass().remove("sidebar-button-active");

                if (!button.getStyleClass().contains("sidebar-button")) {
                    button.getStyleClass().add("sidebar-button");
                }

                String path = destination;
                button.setOnAction(event ->
                        NavigationManager.navigate(stage, path)
                );
            }
        }
    }

    private void configureFilter(
            ComboBox<Choice> combo, String allLabel
    ) {
        combo.getItems().add(new Choice(allLabel, null));
        combo.getSelectionModel().selectFirst();
        combo.setPrefWidth(260);
        combo.setMinHeight(40);
    }

    private VBox filterBox(String title, ComboBox<Choice> combo) {
        return new VBox(8, label(title), combo);
    }

    private void invalidateResults() {
        table.getItems().clear();
        total = 0;
        offset = 0;
        pageInfo.setText("Filters changed.");
        table.setPlaceholder(
                new Label("Click Apply Filters to load matching reports.")
        );
        updateControls();
    }

    private void loadOptions() {
        if (busy || !active()) {
            return;
        }

        String oldEmployee = selectedText(employee, "user_id");
        String oldTrendEmployee = selectedText(trendEmployee, "user_id");
        resetTrend("Select an employee and click Load Trend.");
        String oldDepartment = selectedText(department, "department");
        String oldStart = selectedText(period, "start_date");
        String oldEnd = selectedText(period, "end_date");

        optionsLoaded = false;
        status.setText("Loading team and evaluation periods...");

        run(
                () -> request(
                        "/rpc/get_manager_report_options", new JsonObject()
                ),
                data -> {
                    employee.getItems().setAll(
                            new Choice("All Employees", null)
                    );
                    department.getItems().setAll(
                            new Choice("All Departments", null)
                    );
                    period.getItems().setAll(
                            new Choice("All Evaluation Periods", null)
                    );

                    for (JsonElement item : data.getAsJsonArray("employees")) {
                        JsonObject row = item.getAsJsonObject();
                        employee.getItems().add(new Choice(
                                value(row, "full_name")
                                        + " | "
                                        + value(row, "employee_number"),
                                row
                        ));
                    }

                    for (JsonElement item : data.getAsJsonArray("departments")) {
                        JsonObject row = new JsonObject();
                        row.addProperty("department", item.getAsString());
                        department.getItems().add(
                                new Choice(item.getAsString(), row)
                        );
                    }

                    for (JsonElement item : data.getAsJsonArray("periods")) {
                        JsonObject row = item.getAsJsonObject();
                        period.getItems().add(new Choice(
                                date(value(row, "start_date"))
                                        + " – "
                                        + date(value(row, "end_date")),
                                row
                        ));
                    }

                    employee.getSelectionModel().selectFirst();
                    department.getSelectionModel().selectFirst();
                    period.getSelectionModel().selectFirst();

                    restoreChoice(employee, "user_id", oldEmployee);
                    restoreChoice(department, "department", oldDepartment);

                    for (Choice choice : period.getItems()) {
                        if (choice.value() != null
                                && value(choice.value(), "start_date")
                                        .equals(oldStart)
                                && value(choice.value(), "end_date")
                                        .equals(oldEnd)) {
                            period.setValue(choice);
                            break;
                        }
                    }

                    trendEmployee.getItems().clear();

                    for (Choice choice : employee.getItems()) {
                        if (choice.value() != null) {
                            trendEmployee.getItems().add(choice);
                        }
                    }

                    restoreChoice(trendEmployee, "user_id", oldTrendEmployee);

                    if (trendEmployee.getValue() == null
                            && !trendEmployee.getItems().isEmpty()) {
                        trendEmployee.getSelectionModel().selectFirst();
                    }

                    if (trendEmployee.getItems().isEmpty()) {
                        resetTrend("No direct-report employees are available.");
                    }

                    optionsLoaded = true;
                    loadReports(0);
                },
                error -> {
                    table.getItems().clear();
                    total = 0;
                    offset = 0;
                    pageInfo.setText("Reports unavailable.");
                    table.setPlaceholder(
                            new Label("Unable to load options. Click Refresh.")
                    );
                    fail("Unable to Load Report Options", error);
                }
        );
    }

    private void restoreChoice(
            ComboBox<Choice> combo, String key, String selected
    ) {
        if (selected == null) {
            return;
        }

        for (Choice choice : combo.getItems()) {
            if (choice.value() != null
                    && selected.equals(value(choice.value(), key))) {
                combo.setValue(choice);
                return;
            }
        }
    }

    private String selectedText(ComboBox<Choice> combo, String key) {
        Choice choice = combo.getValue();
        return choice == null || choice.value() == null
                ? null : value(choice.value(), key);
    }

    private void loadReports(int requestedOffset) {
        if (busy || !active() || !optionsLoaded) {
            return;
        }

        JsonObject body = new JsonObject();
        body.addProperty("p_employee_id", selectedText(employee, "user_id"));
        body.addProperty(
                "p_department", selectedText(department, "department")
        );
        body.addProperty("p_period_start", selectedText(period, "start_date"));
        body.addProperty("p_period_end", selectedText(period, "end_date"));
        body.addProperty("p_limit", PAGE_SIZE);
        body.addProperty("p_offset", requestedOffset);

        status.setText("Loading performance reports...");

        run(
                () -> request("/rpc/get_manager_performance_reports", body),
                data -> {
                    JsonArray rows = data.getAsJsonArray("rows");
                    total = data.get("total_count").getAsLong();
                    offset = requestedOffset;

                    table.getItems().clear();
                    for (JsonElement item : rows) {
                        table.getItems().add(item.getAsJsonObject());
                    }

                    table.setPlaceholder(
                            new Label("No KPI results match these filters.")
                    );

                    pageInfo.setText(rows.isEmpty()
                            ? "0 rows on this page • " + total + " results"
                            : (offset + 1) + "–" + (offset + rows.size())
                                    + " of " + total + " results");

                    status.setText(
                            "Report loaded. Employees without saved KPI "
                                    + "results are not listed as zero-score results."
                    );
                },
                error -> {
                    table.getItems().clear();
                    total = 0;
                    offset = 0;
                    pageInfo.setText("Report loading failed.");
                    table.setPlaceholder(
                            new Label("Loading failed. Click Apply Filters to retry.")
                    );
                    fail("Unable to Load Reports", error);
                }
        );
    }

    private JsonObject request(String path, JsonObject body) throws Exception {
        var client = SupabaseClient.getInstance();

        HttpRequest request = client.createAuthenticatedRequest(
                AppConfig.REST_URL + path, token
        )
                .timeout(Duration.ofSeconds(45))
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = client.getHttpClient().send(
                request, HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException(
                    "Report request failed (HTTP " + response.statusCode()
                            + "). Check your session and report access."
            );
        }

        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    private <T> void run(
            Callable<T> work,
            Consumer<T> success,
            Consumer<Throwable> failure
    ) {
        if (busy || !active()) {
            return;
        }

        busy = true;
        updateControls();

        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };

        task.setOnSucceeded(event -> {
            busy = false;
            if (active()) {
                success.accept(task.getValue());
                updateControls();
            }
        });

        task.setOnFailed(event -> {
            busy = false;
            if (active()) {
                failure.accept(task.getException());
                updateControls();
            }
        });

        Thread worker = new Thread(task, "manager-reports");
        worker.setDaemon(true);
        worker.start();
    }

    private boolean active() {
        var user = SessionManager.getCurrentUser();

        return stage.isShowing()
                && stage.getScene().getRoot() == shell
                && shell.getCenter() == page
                && SessionManager.getAuthSession() == session
                && user != null
                && user.getRoleId() == 2L;
    }

    private void updateControls() {
        boolean disabled = busy || !optionsLoaded;

        employee.setDisable(disabled);
        department.setDisable(disabled);
        period.setDisable(disabled);
        apply.setDisable(disabled);
        clear.setDisable(disabled);
        refresh.setDisable(busy);
        exportPdf.setDisable(disabled);
        exportExcel.setDisable(disabled);
        previous.setDisable(disabled || offset == 0);
        next.setDisable(disabled || offset + PAGE_SIZE >= total);
        trendEmployee.setDisable(disabled);
        loadTrendButton.setDisable(
                disabled || trendEmployee.getValue() == null
        );
    }

    private void fail(String title, Throwable error) {
        String detail = error.getMessage() == null
                ? "Please try again." : error.getMessage();

        status.setText(detail);
        AEPMDialog.showError(stage, title, detail);
    }

    private void addColumn(
            String title,
            double width,
            java.util.function.Function<JsonObject, String> reader
    ) {
        TableColumn<JsonObject, String> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(cell ->
                new SimpleStringProperty(reader.apply(cell.getValue()))
        );
        table.getColumns().add(column);
    }

    private static Label label(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle("-fx-text-fill: #334155;");
        return label;
    }

    private static String value(JsonObject row, String key) {
        JsonElement value = row.get(key);
        return value == null || value.isJsonNull()
                ? "—" : value.getAsString();
    }

    private static String score(JsonObject row, String key) {
        JsonElement value = row.get(key);
        return value == null || value.isJsonNull()
                ? "—"
                : String.format(Locale.ENGLISH, "%.2f", value.getAsDouble());
    }

    private static String date(String value) {
        try {
            return LocalDate.parse(value).format(
                    DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
            );
        } catch (RuntimeException error) {
            return value;
        }
    }

    private VBox buildTrendContent() {
        trendEmployee.setPromptText("Select employee");
        trendEmployee.setPrefWidth(300);
        trendEmployee.setMinHeight(44);

        loadTrendButton.getStyleClass().add("login-primary-button");
        loadTrendButton.setStyle(
                "-fx-min-width: 136px;"
                + "-fx-pref-width: 136px;"
                + "-fx-max-width: 136px;"
                + "-fx-min-height: 40px;"
                + "-fx-pref-height: 40px;"
                + "-fx-max-height: 40px;"
                + "-fx-font-size: 14px;"
                + "-fx-padding: 0 16px;"
        );
        loadTrendButton.setOnAction(event -> loadEmployeeTrend());

        trendEmployee.valueProperty().addListener((obs, old, selected) -> {
            resetTrend("Click Load Trend to view this employee's results.");
            updateControls();
        });

        Label explanation = label(
                "Overall KPI across all saved evaluation periods."
                        + "\nEach period uses its latest calculated result. "
                        + "Periods are ordered by evaluation end date."
        );
        explanation.setStyle("-fx-text-fill: #64748B;");

        trendXAxis.setLabel("Evaluation Period");
        trendXAxis.setTickLabelRotation(-25);

        trendYAxis.setLabel("Overall KPI Score");
        trendYAxis.setAutoRanging(false);

        trendChart.setTitle("Overall KPI Trend");
        trendChart.setAnimated(false);
        trendChart.setCreateSymbols(true);
        trendChart.setLegendVisible(true);
        trendChart.setAxisSortingPolicy(LineChart.SortingPolicy.NONE);
        trendChart.setMinHeight(320);
        trendChart.setStyle("-fx-background-color: white;");

        ScrollPane chartScroll = new ScrollPane(trendChart);
        chartScroll.setFitToWidth(true);
        chartScroll.setFitToHeight(true);
        chartScroll.setStyle(
                "-fx-background: white;"
                + "-fx-background-color: white;"
                + "-fx-border-color: transparent;"
        );

        trendMessage.setWrapText(true);
        trendMessage.setStyle("-fx-text-fill: #475569;");
        HBox trendActions = new HBox(
        12,
        filterBox("Employee", trendEmployee),
                loadTrendButton
        );
        trendActions.setAlignment(javafx.geometry.Pos.BOTTOM_LEFT);

        trendEmployee.setStyle(
                "-fx-min-height: 40px;"
                + "-fx-pref-height: 40px;"
                + "-fx-max-height: 40px;"
        );
        VBox content = new VBox(
                16,
                explanation,
                trendActions,
                chartScroll,
                trendMessage
        );

        content.setPadding(new Insets(20));
        content.setStyle(
                "-fx-background-color: white;"
                + "-fx-background-radius: 16;"
        );
        VBox.setVgrow(chartScroll, Priority.ALWAYS);

        return content;
    }

    private void resetTrend(String message) {
        trendChart.getData().clear();
        trendXAxis.getCategories().clear();
        trendChart.setTitle("Overall KPI Trend");
        trendMessage.setText(message);
    }

    private void loadEmployeeTrend() {
        if (busy || !active() || !optionsLoaded) {
            return;
        }

        Choice selected = trendEmployee.getValue();

        if (selected == null || selected.value() == null) {
            return;
        }

        String employeeId = value(selected.value(), "user_id");
        String employeeName = value(selected.value(), "full_name");

        resetTrend("Loading all evaluation periods...");
        status.setText("Loading KPI trend...");

        run(
                () -> fetchTrendResults(employeeId),
                rows -> {
                    displayTrend(employeeName, rows);
                    status.setText("KPI trend loaded.");
                },
                error -> {
                    resetTrend(
                            "Unable to load the trend. Click Load Trend to retry."
                    );
                    fail("Unable to Load KPI Trend", error);
                }
        );
    }

    private List<JsonObject> fetchTrendResults(String employeeId)
            throws Exception {
        List<JsonObject> results = new ArrayList<>();
        Set<String> resultIds = new HashSet<>();

        int readOffset = 0;
        long expectedCount = -1;

        while (true) {
            // Stop requesting further pages after logout or a session change.
            if (SessionManager.getAuthSession() != session) {
                throw new IllegalStateException(
                        "Your session changed. Please sign in again."
                );
            }

            JsonObject body = new JsonObject();
            body.addProperty("p_employee_id", employeeId);
            body.addProperty("p_limit", 500);
            body.addProperty("p_offset", readOffset);

            JsonObject response = request(
                    "/rpc/get_manager_performance_reports", body
            );

            long count = response.get("total_count").getAsLong();

            if (expectedCount < 0) {
                expectedCount = count;
            } else if (count != expectedCount) {
                throw new IllegalStateException(
                        "Report data changed while loading. Click Load Trend again."
                );
            }

            JsonArray rows = response.getAsJsonArray("rows");

            for (JsonElement element : rows) {
                JsonObject row = element.getAsJsonObject();

                if (!resultIds.add(value(row, "result_id"))) {
                    throw new IllegalStateException(
                            "Report data changed while loading. Click Load Trend again."
                    );
                }

                results.add(row);
            }

            readOffset += rows.size();

            if (readOffset >= expectedCount) {
                break;
            }

            if (rows.isEmpty()) {
                throw new IllegalStateException(
                        "The report response was incomplete. Please try again."
                );
            }
        }

        results.sort(
                Comparator
                        .comparing((JsonObject row) ->
                                LocalDate.parse(
                                        value(row, "evaluation_end_date")
                                )
                        )
                        .thenComparing(row ->
                                LocalDate.parse(
                                        value(row, "evaluation_start_date")
                                )
                        )
        );

        return results;
    }

    private void displayTrend(String employeeName, List<JsonObject> rows) {
        resetTrend("");
        trendChart.setTitle(employeeName + " — Overall KPI");

        if (rows.isEmpty()) {
            trendMessage.setText(
                    "No evaluation data is available for this employee."
            );
            return;
        }

        List<String> periods = new ArrayList<>();

        for (JsonObject row : rows) {
            periods.add(
                    date(value(row, "evaluation_start_date"))
                            + " – "
                            + date(value(row, "evaluation_end_date"))
            );
        }

        trendXAxis.setAutoRanging(false);
        trendXAxis.setCategories(
                javafx.collections.FXCollections.observableArrayList(periods)
        );

        // Horizontal scrolling keeps period labels readable.
        trendChart.setMinWidth(Math.max(650, rows.size() * 150.0));

        XYChart.Series<String, Number> segment = null;
        String previousConfig = null;

        Set<String> configs = new HashSet<>();
        int plotted = 0;
        int missing = 0;

        for (int i = 0; i < rows.size(); i++) {
            JsonObject row = rows.get(i);
            JsonElement scoreValue = row.get("overall_kpi_score");

            if (scoreValue == null || scoreValue.isJsonNull()) {
                missing++;
                segment = null;
                previousConfig = null;
                continue;
            }

            double overall = scoreValue.getAsDouble();

            String configId = value(row, "config_id");
            boolean unknownConfig = "—".equals(configId);

            configs.add(configId);

            // Break the line at a missing score or configuration change.
            if (segment == null
                    || unknownConfig
                    || !configId.equals(previousConfig)) {
                segment = new XYChart.Series<>();

                segment.setName(
                        unknownConfig
                                ? "Configuration unavailable"
                                : value(row, "config_name")
                                        + " [#" + configId + "]"
                );

                trendChart.getData().add(segment);
            }

            XYChart.Data<String, Number> point =
                    new XYChart.Data<>(periods.get(i), overall);

            String tooltipText =
                    periods.get(i)
                            + "\nOverall KPI: "
                            + String.format(Locale.ENGLISH, "%.2f", overall)
                            + "\nConfiguration: "
                            + value(row, "config_name")
                            + "\nPerformance: "
                            + value(row, "performance_status");

            point.nodeProperty().addListener((obs, old, node) -> {
                if (node != null) {
                    Tooltip.install(node, new Tooltip(tooltipText));
                }
            });

            segment.getData().add(point);

            if (point.getNode() != null) {
                Tooltip.install(
                        point.getNode(), new Tooltip(tooltipText)
                );
            }

            plotted++;
            previousConfig = configId;

            if (unknownConfig) {
                segment = null;
            }
        }

        String summary = rows.size() + " evaluation period(s); "
                + plotted + " score(s) plotted.";

        if (missing > 0) {
            summary += "\n" + missing
                    + " period(s) have no Overall KPI score; "
                    + "these are not plotted as zero.";
        }

        if (configs.size() > 1 || configs.contains("—")) {
            summary += "\nKPI configurations differ or are unavailable. "
                    + "Separate lines do not imply directly comparable scoring.";
        }

        if (plotted == 1) {
            summary += "\nOnly one score is available; "
                    + "at least two comparable periods are needed to show a trend.";
        }

        trendMessage.setText(summary);
    }

    private ScrollPane scrollReportContent(Region content) {
        // Keep enough space for the table/chart.
        // Smaller windows can scroll instead of compressing the content.
        content.setMinHeight(Region.USE_PREF_SIZE);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        scroll.setStyle(
                "-fx-background: white;"
                + "-fx-background-color: white;"
                + "-fx-border-color: transparent;"
        );

        return scroll;
    }

    private boolean sameReportSession() {
        return SessionManager.getAuthSession() == session
                && SessionManager.getAccessToken() != null;
    }

    private List<JsonObject> fetchAllReportRows(
            JsonObject filters
    ) throws Exception {

        List<JsonObject> results = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        long expected = -1;
        int readOffset = 0;

        while (true) {
            if (!sameReportSession()) {
                throw new IllegalStateException(
                        "Your session changed. Sign in again."
                );
            }

            JsonObject body = filters.deepCopy();
            body.addProperty("p_limit", 500);
            body.addProperty("p_offset", readOffset);

            JsonObject response = request(
                    "/rpc/get_manager_performance_reports",
                    body
            );

            long count = response.get("total_count").getAsLong();

            if (expected < 0) {
                expected = count;
            }

            if (expected != count) {
                throw new IllegalStateException(
                        "Report data changed while loading. Please retry."
                );
            }

            if (expected > 20000) {
                throw new IllegalStateException(
                        "More than 20,000 results match. "
                        + "Narrow the filters before exporting."
                );
            }

            JsonArray rows = response.getAsJsonArray("rows");

            for (JsonElement element : rows) {
                JsonObject row = element.getAsJsonObject();

                if (!seen.add(value(row, "result_id"))) {
                    throw new IllegalStateException(
                            "Report data changed while loading. Please retry."
                    );
                }

                results.add(row);
            }

            readOffset += rows.size();

            if (readOffset == expected) {
                return results;
            }

            if (rows.isEmpty() || readOffset > expected) {
                throw new IllegalStateException(
                        "The report response was incomplete. Please retry."
                );
            }
        }
    }

    private String filterLabel(ComboBox<Choice> combo) {
        return combo.getValue() == null
                ? "All"
                : combo.getValue().label();
    }

    private void exportReport(boolean pdf) {
        if (busy || !active() || !optionsLoaded) {
            return;
        }

        JsonObject selectedRow =
        table.getSelectionModel().getSelectedItem();

        String exportEmployeeId;
        String exportEmployeeLabel;

        if (selectedRow != null) {
            // The report row contains the employee UUID in employee_id.
            exportEmployeeId = value(selectedRow, "employee_id");

            exportEmployeeLabel =
                    value(selectedRow, "full_name")
                    + " | "
                    + value(selectedRow, "employee_number");

            // Never fall back to exporting everyone if the selected row is invalid.
            try {
                java.util.UUID.fromString(exportEmployeeId);
            } catch (IllegalArgumentException error) {
                AEPMDialog.showError(
                        stage,
                        "Unable to Export",
                        "The selected row has no valid employee ID. "
                                + "Refresh the report and select the employee again."
                );
                return;
            }
        } else {
            exportEmployeeId = selectedText(employee, "user_id");
            exportEmployeeLabel = filterLabel(employee);
        }

        JsonObject filters = new JsonObject();
        filters.addProperty("p_employee_id", exportEmployeeId);
        filters.addProperty(
                "p_department",
                selectedText(department, "department")
        );
        filters.addProperty(
                "p_period_start",
                selectedText(period, "start_date")
        );
        filters.addProperty(
                "p_period_end",
                selectedText(period, "end_date")
        );

        List<String> metadata = List.of(
                "Employee: " + exportEmployeeLabel,
                "Department: " + filterLabel(department),
                "Evaluation period: " + filterLabel(period),

                "Generated (MYT): "
                        + java.time.ZonedDateTime.now(
                                java.time.ZoneId.of("Asia/Kuala_Lumpur")
                        ).format(
                                DateTimeFormatter.ofPattern(
                                        "dd MMM yyyy HH:mm:ss",
                                        Locale.ENGLISH
                                )
                        ),

                "Scope: current direct reports; latest saved result "
                        + "per employee and period.",

                "Missing scores are blank / —, not zero. "
                        + "Different configurations may not be "
                        + "directly comparable."
        );

        boolean confirmed = AEPMDialog.showConfirmation(
                stage,
                "Export " + (pdf ? "PDF" : "Excel"),
                "Export all matching report rows using these filters?"
                        + "\nEvery matching page will be included."
                        + "\n\n"
                        + String.join("\n", metadata.subList(0, 3))
        );

        if (!confirmed) {
            return;
        }

        javafx.stage.FileChooser chooser =
                new javafx.stage.FileChooser();

        String extension = pdf ? ".pdf" : ".xlsx";

        chooser.setTitle("Save Performance Report");

        chooser.setInitialFileName(
                "Manager_Performance_Report_"
                        + LocalDate.now(
                                java.time.ZoneId.of("Asia/Kuala_Lumpur")
                        )
                        + extension
        );

        chooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter(
                        pdf ? "PDF report" : "Excel workbook",
                        "*" + extension
                )
        );

        java.io.File chosen = chooser.showSaveDialog(stage);

        if (chosen == null || !active() || !sameReportSession()) {
            return;
        }

        java.nio.file.Path destination =
                chosen.toPath().toAbsolutePath();

        if (!destination.getFileName()
                .toString()
                .toLowerCase(Locale.ROOT)
                .endsWith(extension)) {

            destination = destination.resolveSibling(
                    destination.getFileName() + extension
            );
        }

        boolean replace = java.nio.file.Files.exists(destination);

        if (replace && !AEPMDialog.showConfirmation(
                stage,
                "Replace File",
                "Replace this existing file?\n\n" + destination
        )) {
            return;
        }

        final java.nio.file.Path target = destination;

        status.setText(
                "Loading all matching records and creating "
                        + (pdf ? "PDF" : "Excel")
                        + "..."
        );

        run(
                () -> {
                    List<JsonObject> rows = fetchAllReportRows(filters);

                    if (rows.isEmpty()) {
                        throw new IllegalStateException(
                                "No KPI results match these filters. "
                                + "No file was written."
                        );
                    }

                    java.nio.file.Path temporary =
                            java.nio.file.Files.createTempFile(
                                    target.getParent(),
                                    ".aepm-report-",
                                    extension
                            );

                    try {
                        if (pdf) {
                            ManagerReportExporter.writePdf(
                                    temporary,
                                    rows,
                                    metadata
                            );
                        } else {
                            ManagerReportExporter.writeExcel(
                                    temporary,
                                    rows,
                                    metadata
                            );
                        }

                        if (!sameReportSession()) {
                            throw new IllegalStateException(
                                    "Your session changed. "
                                    + "The export was cancelled."
                            );
                        }

                        if (replace) {
                            java.nio.file.Files.move(
                                    temporary,
                                    target,
                                    java.nio.file.StandardCopyOption
                                            .REPLACE_EXISTING
                            );
                        } else {
                            java.nio.file.Files.move(
                                    temporary,
                                    target
                            );
                        }

                        return rows.size();
                    } finally {
                        java.nio.file.Files.deleteIfExists(temporary);
                    }
                },
                count -> {
                    status.setText(
                            "Export complete: " + count + " records."
                    );

                    AEPMDialog.showSuccess(
                            stage,
                            "Export Successful",
                            count + " records exported."
                                    + "\n\nSaved to:\n" + target
                    );
                },
                error -> fail("Export Failed", error)
        );
    }
}