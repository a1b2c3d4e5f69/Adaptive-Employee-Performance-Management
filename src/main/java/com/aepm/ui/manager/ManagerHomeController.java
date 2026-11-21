package com.aepm.ui.manager;

import com.aepm.logic.dashboard.ManagerDashboardService;
import com.aepm.model.User;
import com.aepm.ui.components.NotificationBell;
import com.aepm.ui.navigation.NavigationManager;
import com.aepm.util.SessionManager;
import com.aepm.ui.common.chat.ChatController;

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
import javafx.stage.Stage;
import javafx.stage.Window;

import com.aepm.ui.components.DashboardAttentionPanel;
import com.aepm.ui.components.DashboardTaskChart;
import java.util.Locale;

public class ManagerHomeController {

    
    @FXML private BorderPane pageRoot;
    @FXML private Button homeNavButton;
    @FXML private Button chatNavButton;
    private ChatController chatController;
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
    @FXML private Label escalationLevel1Label;
    @FXML private Label escalationLevel2Label;
    @FXML private Label escalationLevel3Label;
    @FXML private DashboardAttentionPanel attentionPanel;
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
                String.valueOf(s.level1() + s.level2() + s.level3())
        );

        escalationLevel1Label.setText("Level 1: " + s.level1());
        escalationLevel2Label.setText("Level 2: " + s.level2());
        escalationLevel3Label.setText("Level 3: " + s.level3());

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

                DashboardTaskChart.update(
                taskStatusChart,
                s.completed(),
                s.inProgress(),
                s.overdue(),
                s.pending(),
                s.otherTasks()
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
        attentionPanel.reload();
    }

    // =========================================================
        // NAVIGATION
        // =========================================================

        @FXML
        private void handleHome(ActionEvent event) {

        navigate(
                event,
                "/fxml/manager/managerHome.fxml"
        );
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
        private void handlePerformance(ActionEvent event) {

        navigate(
                event,
                "/fxml/manager/managerPerformance.fxml"
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
        private void handleReports(ActionEvent event) {

        Window owner =
                ((Node) event.getSource())
                        .getScene()
                        .getWindow();

        ManagerReportsPage.show(owner);
        }


        // =========================================================
        // CHAT
        // =========================================================

        private void setChatNavigationActive() {

        if (homeNavButton == null
                || chatNavButton == null) {

                return;
        }


        homeNavButton
                .getStyleClass()
                .removeAll(
                        "manager-nav-button",
                        "manager-nav-button-active"
                );

        homeNavButton
                .getStyleClass()
                .add(
                        "manager-nav-button"
                );


        chatNavButton
                .getStyleClass()
                .removeAll(
                        "manager-nav-button",
                        "manager-nav-button-active"
                );

        chatNavButton
                .getStyleClass()
                .add(
                        "manager-nav-button-active"
                );
        }


        @FXML
        private void handleChat(ActionEvent event) {

        setChatNavigationActive();

        try {

                FXMLLoader loader =
                        new FXMLLoader(
                                getClass().getResource(
                                        "/fxml/common/chat.fxml"
                                )
                        );


                Parent chatRoot =
                        loader.load();


                if (chatController != null) {

                chatController.stop();

                chatController = null;
                }


                chatController =
                        loader.getController();


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


        // =========================================================
        // LOGOUT
        // =========================================================

        @FXML
        private void handleLogout(ActionEvent event) {

        if (chatController != null) {

                chatController.stop();

                chatController = null;
        }


                SessionManager.clearSession();


                navigate(
                        event,
                        "/fxml/auth/login.fxml"
                );
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
                        "[Manager Navigation] Opening: " + path
                );

                // Stop Chat Center auto refresh before navigating away.
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
                        "[Manager Navigation] Failed: " + path
                );

                e.printStackTrace();
        }
        }
}
