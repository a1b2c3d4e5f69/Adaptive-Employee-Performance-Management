package com.aepm.ui.admin;

import com.aepm.logic.dashboard.AdminDashboardService;
import com.aepm.model.User;
import com.aepm.ui.navigation.NavigationManager;
import com.aepm.util.SessionManager;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.util.Locale;

public class AdminHomeController {

    @FXML private Label welcomeLabel;
    @FXML private Label topNameLabel;
    @FXML private Label dashboardStatusLabel;

    @FXML private Label employeeCountLabel;
    @FXML private Label managerCountLabel;
    @FXML private Label taskCountLabel;
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

    @FXML private BarChart<String, Number> taskStatusChart;
    @FXML private PieChart escalationChart;

    private final AdminDashboardService dashboardService =
            new AdminDashboardService();

    @FXML
    public void initialize() {
        loadCurrentUser();
        loadDashboard();
    }

    @FXML
        private void handleAIConfiguration(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource())
                .getScene().getWindow();        
        NavigationManager.navigate(
                stage,
                "/fxml/admin/adminAIConfiguration.fxml"
        );
    }

    private void loadCurrentUser() {

        User user = SessionManager.getCurrentUser();

        String name = user == null
                ? "Administrator"
                : user.getFullName();

        if (name == null || name.isBlank()) {
            name = user == null
                    ? "Administrator"
                    : user.getUsername();
        }

        if (name == null || name.isBlank()) {
            name = "Administrator";
        }

        welcomeLabel.setText("Welcome, " + name);
        topNameLabel.setText(name);
    }

    private void loadDashboard() {

        String token = SessionManager.getAccessToken();

        if (token == null || token.isBlank()) {
            dashboardStatusLabel.setText(
                    "Login session is missing."
            );
            return;
        }

        dashboardStatusLabel.setText(
                "Loading dashboard..."
        );

        Thread worker = new Thread(() -> {

            try {

                AdminDashboardService.Summary summary =
                        dashboardService.load(token);

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

        }, "admin-dashboard-loader");

        worker.setDaemon(true);
        worker.start();
    }

    private void updateSummary(
            AdminDashboardService.Summary s
    ) {

        employeeCountLabel.setText(
                String.valueOf(s.employees())
        );

        managerCountLabel.setText(
                String.valueOf(s.managers())
        );

        taskCountLabel.setText(
                String.valueOf(s.totalTasks())
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
    }

    private void updateCharts(
            AdminDashboardService.Summary s
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
        loadDashboard();
    }

    @FXML
    private void handleHome() {
        // Already on Home.
    }

    @FXML
    private void handleProfile(ActionEvent event) {
        navigate(event, "/fxml/admin/adminProfile.fxml");
    }

    @FXML
    private void handleUserManagement(ActionEvent event) {
        navigate(
                event,
                "/fxml/admin/adminUserManagement.fxml"
        );
    }

    @FXML
    private void handleTaskManagement(ActionEvent event) {
        navigate(event, "/fxml/admin/adminTask.fxml");
    }

    @FXML
    private void handleEscalationRules(ActionEvent event) {
        navigate(
                event,
                "/fxml/admin/adminEscalationRules.fxml"
        );
    }

    @FXML
    private void handleKpiManagement(ActionEvent event) {
        navigate(event, "/fxml/admin/adminKpi.fxml");
    }

    @FXML
    private void handleSystemConfiguration(ActionEvent event) {
        navigate(
                event,
                "/fxml/admin/systemConfiguration.fxml"
        );
    }

    @FXML
    private void handleAttendanceManagement(ActionEvent event) {

        try {

            String token = SessionManager.getAccessToken();

            if (token == null || token.isBlank()) {
                System.err.println(
                        "Attendance: Login session is missing."
                );
                return;
            }

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/fxml/admin/adminAttendance.fxml"
                    )
            );

            Parent root = loader.load();

            AdminAttendanceController controller =
                    loader.getController();

            controller.setAccessToken(token);

            Stage stage = (Stage) ((Node) event.getSource())
                    .getScene().getWindow();

            stage.getScene().setRoot(root);

        } catch (Exception e) {
            System.err.println(
                    "Failed to open Attendance Management."
            );
            e.printStackTrace();
        }
    }

    @FXML
    private void handleReports() {
        System.out.println("Reports not implemented yet.");
    }

    @FXML
    private void handleAnonymousReports() {
        System.out.println(
                "Anonymous Reports not implemented yet."
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
