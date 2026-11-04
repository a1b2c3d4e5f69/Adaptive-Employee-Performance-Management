package com.aepm.ui.employee;

import com.aepm.logic.dashboard.EmployeeDashboardService;
import com.aepm.model.User;
import com.aepm.ui.components.NotificationBell;
import com.aepm.ui.navigation.NavigationManager;
import com.aepm.util.SessionManager;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.util.Locale;

public class EmployeeHomeController {

    @FXML private Label topNameLabel;
    @FXML private Label topRoleLabel;
    @FXML private Label welcomeNameLabel;
    @FXML private Label employeeIdLabel;
    @FXML private Label departmentLabel;
    @FXML private NotificationBell notificationBell;

    // Dashboard
    @FXML private Label dashboardStatusLabel;
    @FXML private Label taskCountLabel;
    @FXML private Label pendingCountLabel;
    @FXML private Label completedCountLabel;
    @FXML private Label overdueCountLabel;
    @FXML private Label inProgressCountLabel;
    @FXML private Label completionRateLabel;
    @FXML private Label kpiScoreLabel;
    @FXML private Label performanceStatusLabel;
    @FXML private Label dueSoonLabel;
    @FXML private Label escalationCountLabel;
    @FXML private Label remindersLabel;
    @FXML private Label donutTotalLabel;

    @FXML private BarChart<String, Number> taskStatusChart;
    @FXML private PieChart escalationChart;

    private final EmployeeDashboardService dashboardService =
            new EmployeeDashboardService();

    @FXML
    public void initialize() {
        loadCurrentUser();
        loadDashboard();
    }

    private void loadCurrentUser() {

        User user = SessionManager.getCurrentUser();

        String name = user == null
                ? "Employee"
                : user.getFullName();

        if (name == null || name.isBlank()) {
            name = user == null
                    ? "Employee"
                    : user.getUsername();
        }

        if (name == null || name.isBlank()) {
            name = "Employee";
        }

        topNameLabel.setText(name);
        welcomeNameLabel.setText(name);

        if (user == null) {
            topRoleLabel.setText("EMPLOYEE");
            employeeIdLabel.setText("-");
            departmentLabel.setText("-");
            return;
        }

        String employeeId = user.getEmployeeId();

        employeeIdLabel.setText(
                employeeId == null || employeeId.isBlank()
                        ? "-"
                        : employeeId
        );

        topRoleLabel.setText(
                employeeId == null || employeeId.isBlank()
                        ? "EMPLOYEE"
                        : "EMPLOYEE  •  " + employeeId
        );

        String department = user.getDepartment();

        departmentLabel.setText(
                department == null || department.isBlank()
                        ? "-"
                        : department
        );
    }

    private void loadDashboard() {

        String token = SessionManager.getAccessToken();
        User user = SessionManager.getCurrentUser();

        if (token == null || token.isBlank()
                || user == null
                || user.getUserId() == null) {

            dashboardStatusLabel.setText(
                    "Login session is missing."
            );
            return;
        }

        String employeeId =
                String.valueOf(user.getUserId());

        dashboardStatusLabel.setText(
                "Loading personal dashboard..."
        );

        Thread worker = new Thread(() -> {

            try {

                EmployeeDashboardService.Summary summary =
                        dashboardService.load(
                                token, employeeId
                        );

                Platform.runLater(() -> {

                    updateSummary(summary);
                    updateCharts(summary);

                    dashboardStatusLabel.setText(
                            "Dashboard updated."
                    );
                });

            } catch (Exception e) {

                e.printStackTrace();

                Platform.runLater(() ->
                        dashboardStatusLabel.setText(
                                "Unable to load dashboard: "
                                        + e.getMessage()
                        )
                );
            }

        }, "employee-dashboard-loader");

        worker.setDaemon(true);
        worker.start();
    }

    private void updateSummary(
            EmployeeDashboardService.Summary s
    ) {

        taskCountLabel.setText(
                String.valueOf(s.totalTasks())
        );

        pendingCountLabel.setText(
                String.valueOf(s.pending())
        );

        completedCountLabel.setText(
                String.valueOf(s.completed())
        );

        overdueCountLabel.setText(
                String.valueOf(s.overdue())
        );

        inProgressCountLabel.setText(
                String.valueOf(s.inProgress())
        );

        completionRateLabel.setText(
                String.format(
                        Locale.US,
                        "%.1f%%",
                        s.completionRate()
                )
        );

        kpiScoreLabel.setText(
                s.kpiScore() == null
                        ? "—"
                        : String.format(
                                Locale.US,
                                "%.1f",
                                s.kpiScore()
                        )
        );

        performanceStatusLabel.setText(
                s.performanceStatus() == null
                        ? "—"
                        : s.performanceStatus()
        );

        dueSoonLabel.setText(
                String.valueOf(s.dueSoon())
        );

        escalationCountLabel.setText(
                String.valueOf(s.activeEscalations())
        );

        donutTotalLabel.setText(
                String.valueOf(s.activeEscalations())
        );

        remindersLabel.setText(s.reminders());
    }

    private void updateCharts(
            EmployeeDashboardService.Summary s
    ) {

        XYChart.Series<String, Number> series =
                new XYChart.Series<>();

        series.getData().add(
                new XYChart.Data<>("Completed", s.completed())
        );

        series.getData().add(
                new XYChart.Data<>("In Progress", s.inProgress())
        );

        series.getData().add(
                new XYChart.Data<>("Overdue", s.overdue())
        );

        series.getData().add(
                new XYChart.Data<>("Pending", s.pending())
        );

        series.getData().add(
                new XYChart.Data<>("Other", s.other())
        );

        taskStatusChart.setAnimated(false);
        taskStatusChart.setLegendVisible(false);
        taskStatusChart.getData().setAll(series);

        String[] barColors = {
                "#2563EB",
                "#F59E0B",
                "#FB7185",
                "#8B5CF6",
                "#64748B"
        };

        for (int i = 0; i < series.getData().size(); i++) {

            final String color = barColors[i];

            XYChart.Data<String, Number> item =
                    series.getData().get(i);

            if (item.getNode() != null) {
                item.getNode().setStyle(
                        "-fx-bar-fill: " + color + ";"
                );
            }

            item.nodeProperty().addListener(
                    (obs, oldNode, node) -> {
                        if (node != null) {
                            node.setStyle(
                                    "-fx-bar-fill: "
                                            + color + ";"
                            );
                        }
                    }
            );
        }

        var pieData = FXCollections.observableArrayList(
                new PieChart.Data("Level 1", s.level1()),
                new PieChart.Data("Level 2", s.level2()),
                new PieChart.Data("Level 3", s.level3())
        );

        escalationChart.setAnimated(false);
        escalationChart.setLegendVisible(false);
        escalationChart.setLabelsVisible(false);
        escalationChart.setData(pieData);

        String[] pieColors = {
                "#2563EB",
                "#F59E0B",
                "#FB7185"
        };

        for (int i = 0; i < pieData.size(); i++) {

            final String color = pieColors[i];
            PieChart.Data item = pieData.get(i);

            if (item.getNode() != null) {
                item.getNode().setStyle(
                        "-fx-pie-color: " + color + ";"
                );
            }

            item.nodeProperty().addListener(
                    (obs, oldNode, node) -> {
                        if (node != null) {
                            node.setStyle(
                                    "-fx-pie-color: "
                                            + color + ";"
                            );
                        }
                    }
            );
        }
    }

    @FXML
    private void handleRefreshDashboard() {
        loadDashboard();
    }

    @FXML
    private void handleHome() {
        // Already on Home.
    }

    @FXML
    private void handleProfile(ActionEvent event) {
        navigate(
                event,
                "/fxml/employee/employeeProfile.fxml"
        );
    }

    @FXML
    private void handleTasks(ActionEvent event) {
        navigate(
                event,
                "/fxml/employee/employeeTask.fxml"
        );
    }

    @FXML
    private void handleKpi(ActionEvent event) {
        navigate(
                event,
                "/fxml/employee/employeePerformance.fxml"
        );
    }

    @FXML
    private void handleAttendance(ActionEvent event) {
        navigate(
                event,
                "/fxml/employee/employeeAttendance.fxml"
        );
    }

    @FXML
    private void handleEscalationRules(ActionEvent event) {
        // Preserve existing navigation for compatibility.
        navigate(
                event,
                "/fxml/employee/employeeProfile.fxml"
        );
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
    private void handleLogout(ActionEvent event) {

        SessionManager.clearSession();

        navigate(event, "/fxml/auth/login.fxml");
    }

    private void navigate(
            ActionEvent event,
            String path
    ) {

        Stage stage = (Stage) ((Node) event.getSource())
                .getScene().getWindow();

        NavigationManager.navigate(stage, path);
    }
}
