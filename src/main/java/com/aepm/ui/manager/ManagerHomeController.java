package com.aepm.ui.manager;

import com.aepm.logic.dashboard.ManagerDashboardService;
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

public class ManagerHomeController {

    @FXML private Label welcomeLabel;
    @FXML private Label topNameLabel;
    @FXML private Label topRoleLabel;
    @FXML private Label departmentLabel;
    @FXML private Label roleLabel;
    @FXML private NotificationBell notificationBell;

    @FXML private Label dashboardStatusLabel;
    @FXML private Label teamMembersLabel;
    @FXML private Label taskCountLabel;
    @FXML private Label inProgressCountLabel;
    @FXML private Label overdueCountLabel;
    @FXML private Label completedCountLabel;
    @FXML private Label escalationCountLabel;
    @FXML private Label completionRateLabel;
    @FXML private Label donutTotalLabel;

    // New Dashboard fields
    @FXML private Label pendingCountLabel;
    @FXML private Label averageKpiLabel;
    @FXML private Label evaluatedCountLabel;
    @FXML private Label atRiskCountLabel;
    @FXML private Label escalationDetailsLabel;

    @FXML private BarChart<String, Number> taskStatusChart;
    @FXML private PieChart escalationChart;

    private final ManagerDashboardService dashboardService =
            new ManagerDashboardService();

    @FXML
    public void initialize() {
        loadCurrentManager();
        loadTeamDashboard();
    }

    private void loadCurrentManager() {

        User user = SessionManager.getCurrentUser();

        String name = user == null
                ? "Manager"
                : user.getFullName();

        if (name == null || name.isBlank()) {
            name = user == null
                    ? "Manager"
                    : user.getUsername();
        }

        if (name == null || name.isBlank()) {
            name = "Manager";
        }

        welcomeLabel.setText("Welcome, " + name);
        topNameLabel.setText(name);

        if (user == null) {
            topRoleLabel.setText("MANAGER");
            departmentLabel.setText("-");
            roleLabel.setText("Manager");
            return;
        }

        String employeeId = user.getEmployeeId();

        topRoleLabel.setText(
                employeeId == null || employeeId.isBlank()
                        ? "MANAGER"
                        : "MANAGER  •  " + employeeId
        );

        String department = user.getDepartment();

        departmentLabel.setText(
                department == null || department.isBlank()
                        ? "-"
                        : department
        );

        roleLabel.setText("Manager");
    }

    private void loadTeamDashboard() {

        String token = SessionManager.getAccessToken();
        User manager = SessionManager.getCurrentUser();

        if (token == null || token.isBlank()
                || manager == null
                || manager.getUserId() == null) {

            dashboardStatusLabel.setText(
                    "Login session is missing."
            );
            return;
        }

        String managerId =
                String.valueOf(manager.getUserId());

        dashboardStatusLabel.setText(
                "Loading team dashboard..."
        );

        Thread worker = new Thread(() -> {

            try {

                ManagerDashboardService.Summary summary =
                        dashboardService.load(token, managerId);

                Platform.runLater(() -> {

                    updateSummary(summary);
                    updateCharts(summary);

                    dashboardStatusLabel.setText(
                            "Team dashboard updated."
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

        }, "manager-dashboard-loader");

        worker.setDaemon(true);
        worker.start();
    }

    private void updateSummary(
            ManagerDashboardService.Summary s
    ) {

        teamMembersLabel.setText(
                String.valueOf(s.teamMembers())
        );

        taskCountLabel.setText(
                String.valueOf(s.totalTasks())
        );

        inProgressCountLabel.setText(
                String.valueOf(s.inProgress())
        );

        overdueCountLabel.setText(
                String.valueOf(s.overdue())
        );

        completedCountLabel.setText(
                String.valueOf(s.completed())
        );

        escalationCountLabel.setText(
                String.valueOf(s.activeEscalations())
        );

        completionRateLabel.setText(
                String.format(
                        Locale.US,
                        "%.1f%%",
                        s.completionRate()
                )
        );

        donutTotalLabel.setText(
                String.valueOf(s.activeEscalations())
        );

        pendingCountLabel.setText(
                String.valueOf(s.pending())
        );

        averageKpiLabel.setText(
                s.averageKpi() == null
                        ? "—"
                        : String.format(
                                Locale.US,
                                "%.1f",
                                s.averageKpi()
                        )
        );

        evaluatedCountLabel.setText(
                String.valueOf(s.evaluatedEmployees())
        );

        atRiskCountLabel.setText(
                String.valueOf(s.atRiskEmployees())
        );

        escalationDetailsLabel.setText(
                s.escalationDetails()
        );
    }

    private void updateCharts(
            ManagerDashboardService.Summary s
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
                new XYChart.Data<>("Other", s.otherTasks())
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
        loadTeamDashboard();
    }

    @FXML
    private void handleHome() {
        // Already on Home.
    }

    @FXML
    private void handleProfile(ActionEvent event) {
        navigate(
                event,
                "/fxml/manager/managerProfile.fxml"
        );
    }

    @FXML
    private void handleTaskManagement(ActionEvent event) {
        navigate(
                event,
                "/fxml/manager/managerTask.fxml"
        );
    }

    @FXML
    private void handleDeadlineEscalation(ActionEvent event) {
        navigate(
                event,
                "/fxml/manager/managerDeadlineEscalation.fxml"
        );
    }

    @FXML
    private void handlePerformance(ActionEvent event) {
        navigate(
                event,
                "/fxml/manager/managerPerformance.fxml"
        );
    }

    @FXML
    private void handleReports() {
        System.out.println(
                "Reports not implemented yet."
        );
    }

    @FXML
    private void handleDashboard(ActionEvent event) {
        navigate(
                event,
                "/fxml/manager/managerHome.fxml"
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
