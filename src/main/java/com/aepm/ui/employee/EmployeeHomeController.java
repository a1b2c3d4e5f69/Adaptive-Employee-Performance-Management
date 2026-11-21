package com.aepm.ui.employee;

import com.aepm.logic.dashboard.EmployeeDashboardService;
import com.aepm.model.User;
import com.aepm.ui.components.NotificationBell;
import com.aepm.ui.navigation.NavigationManager;
import com.aepm.util.SessionManager;
import com.aepm.ui.common.chat.ChatController;
import com.aepm.ui.components.DashboardTaskChart;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;
import javafx.scene.Parent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.util.Locale;

public class EmployeeHomeController {

    
    @FXML private BorderPane pageRoot;
    @FXML private Button homeNavButton;
    @FXML private Button chatNavButton;
    private ChatController chatController;
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
    @FXML private Label escalationLevel1Label;
    @FXML private Label escalationLevel2Label;
    @FXML private Label escalationLevel3Label;

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
                String.valueOf(s.level1() + s.level2() + s.level3())
        );

        escalationLevel1Label.setText("Level 1: " + s.level1());
        escalationLevel2Label.setText("Level 2: " + s.level2());
        escalationLevel3Label.setText("Level 3: " + s.level3());
        remindersLabel.setText(s.reminders());
    }

    private void updateCharts(
                EmployeeDashboardService.Summary s
        ) {

        DashboardTaskChart.update(
                taskStatusChart,
                s.completed(),
                s.inProgress(),
                s.overdue(),
                s.pending(),
                s.other()
        );

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


        for (int i = 0;
                i < pieData.size();
                i++) {

                final String color =
                        pieColors[i];

                PieChart.Data item =
                        pieData.get(i);


                if (item.getNode() != null) {

                item.getNode().setStyle(
                        "-fx-pie-color: "
                                + color
                                + ";"
                );
                }


                item.nodeProperty().addListener(
                        (obs, oldNode, node) -> {

                        if (node != null) {

                                node.setStyle(
                                        "-fx-pie-color: "
                                                + color
                                                + ";"
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

        // =========================================================
        // HOME
        // =========================================================

        @FXML
        private void handleHome(ActionEvent event) {

        navigate(
                event,
                "/fxml/employee/employeeHome.fxml"
        );
        }


        // =========================================================
        // MY TASKS
        // =========================================================

        @FXML
        private void handleTasks(ActionEvent event) {

        navigate(
                event,
                "/fxml/employee/employeeTask.fxml"
        );
        }


        // =========================================================
        // ATTENDANCE
        // =========================================================

        @FXML
        private void handleAttendance(ActionEvent event) {

        navigate(
                event,
                "/fxml/employee/employeeAttendance.fxml"
        );
        }


        // =========================================================
        // MY PERFORMANCE
        // =========================================================

        @FXML
        private void handleKpi(ActionEvent event) {

        navigate(
                event,
                "/fxml/employee/employeePerformance.fxml"
        );
        }


        // =========================================================
        // ANONYMOUS REPORT
        // =========================================================

        @FXML
        private void handleAnonymousReport(ActionEvent event) {

        navigate(
                event,
                "/fxml/employee/employeeAnonymousReport.fxml"
        );
        }


        // =========================================================
        // MY PROFILE
        // =========================================================

        @FXML
        private void handleProfile(ActionEvent event) {

        navigate(
                event,
                "/fxml/employee/employeeProfile.fxml"
        );
        }


        // =========================================================
        // CHAT ACTIVE STYLE
        // =========================================================

        private void setChatNavigationActive() {

        if (
                homeNavButton == null
                        || chatNavButton == null
        ) {

                return;
        }


        // HOME NORMAL
        homeNavButton
                .getStyleClass()
                .removeAll(
                        "employee-nav-button",
                        "employee-nav-button-active"
                );

        homeNavButton
                .getStyleClass()
                .add(
                        "employee-nav-button"
                );


        // CHAT ACTIVE
        chatNavButton
                .getStyleClass()
                .removeAll(
                        "employee-nav-button",
                        "employee-nav-button-active"
                );

        chatNavButton
                .getStyleClass()
                .add(
                        "employee-nav-button-active"
                );
        }


        // =========================================================
        // CHAT CENTER
        // =========================================================

        @FXML
        private void handleChat(ActionEvent event) {

        try {

                FXMLLoader loader =
                        new FXMLLoader(
                                getClass().getResource(
                                        "/fxml/common/chat.fxml"
                                )
                        );


                Parent chatRoot =
                        loader.load();


                // Stop previous chat refresh if already opened.
                if (chatController != null) {

                chatController.stop();

                chatController = null;
                }


                chatController =
                        loader.getController();


                // Highlight Chat Center
                setChatNavigationActive();


                // IMPORTANT:
                // Only replace middle content.
                // Keep AEPM topbar + Employee sidebar.
                pageRoot.setCenter(
                        chatRoot
                );


        } catch (Exception e) {

                System.err.println(
                        "Failed to open Chat Center."
                );

                e.printStackTrace();
        }
        }

        @FXML
        private void handleLogout(ActionEvent event) {

        try {

                // Stop chat auto refresh if Chat Center was opened.
                if (chatController != null) {

                chatController.stop();

                chatController = null;
                }


                // Clear logged-in user session.
                SessionManager.clearSession();


                // Return to login page.
                Stage stage =
                        (Stage) ((Node) event.getSource())
                                .getScene()
                                .getWindow();


                NavigationManager.navigate(
                        stage,
                        "/fxml/auth/login.fxml"
                );


        } catch (Exception e) {

                System.err.println(
                        "Logout failed."
                );

                e.printStackTrace();
        }
        }


                // =========================================================
                // COMMON NAVIGATION
                // =========================================================

        private void navigate(
                ActionEvent event,
                String path
        ) {

        try {

                System.out.println(
                        "[Employee Navigation] Opening: " + path
                );

                // Stop Chat Center auto refresh before leaving.
                if (chatController != null) {

                chatController.stop();

                chatController = null;
                }

                Stage stage =
                        (Stage) ((Node) event.getSource())
                                .getScene()
                                .getWindow();

                NavigationManager.navigate(
                        stage,
                        path
                );

        } catch (Exception e) {

                System.err.println(
                        "[Employee Navigation] Failed: " + path
                );

                e.printStackTrace();
        }
        }

    private XYChart.Data<String, Number> createBarData(
                String category,
                int value
        ) {

        XYChart.Data<String, Number> data =
                new XYChart.Data<>(
                        category,
                        value
                );


        data.nodeProperty().addListener(
                (obs, oldNode, node) -> {

                        if (node == null) {
                        return;
                        }


                        Label valueLabel =
                                new Label(
                                        String.valueOf(value)
                                );

                        valueLabel.getStyleClass().add(
                                "employee-chart-value-label"
                        );


                        node.parentProperty().addListener(
                                (parentObs, oldParent, parent) -> {

                                if (!(parent instanceof StackPane stackPane)) {
                                        return;
                                }

                                if (!stackPane.getChildren().contains(
                                        valueLabel
                                )) {

                                        stackPane.getChildren().add(
                                                valueLabel
                                        );

                                        StackPane.setAlignment(
                                                valueLabel,
                                                javafx.geometry.Pos.TOP_CENTER
                                        );

                                        valueLabel.setTranslateY(
                                                -18
                                        );
                                }
                                }
                        );
                }
        );


        return data;
    }
}
