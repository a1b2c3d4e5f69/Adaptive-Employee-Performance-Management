package com.aepm.ui.employee;

import com.aepm.model.User;
import com.aepm.ui.navigation.NavigationManager;
import com.aepm.util.SessionManager;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import com.aepm.ui.components.NotificationBell;

public class EmployeeHomeController {

    // =========================================================
    // TOP BAR
    // =========================================================

    @FXML
    private Label topNameLabel;

    @FXML
    private Label topRoleLabel;


    // =========================================================
    // HOME PAGE
    // =========================================================

    @FXML
    private Label welcomeNameLabel;

    @FXML
    private Label employeeIdLabel;

    @FXML
    private Label departmentLabel;

    @FXML
    private NotificationBell notificationBell;

    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {

        loadCurrentUser();
    }

    @FXML
        private void handleAttendance(ActionEvent event) {

        Stage stage = (Stage) ((Node) event.getSource())
                .getScene()
                .getWindow();

        NavigationManager.navigate(
                stage,
                "/fxml/employee/employeeAttendance.fxml"
        );
    }


    // =========================================================
    // LOAD CURRENT USER
    // =========================================================

    private void loadCurrentUser() {

        User user =
                SessionManager.getCurrentUser();


        if (user == null) {

            topNameLabel.setText(
                    "Employee"
            );

            topRoleLabel.setText(
                    "EMPLOYEE"
            );

            welcomeNameLabel.setText(
                    "Employee"
            );

            employeeIdLabel.setText(
                    "-"
            );

            departmentLabel.setText(
                    "-"
            );

            return;
        }


        // =====================================================
        // DISPLAY NAME
        // =====================================================

        String displayName =
                user.getFullName();


        if (
                displayName == null
                        || displayName.isBlank()
        ) {

            displayName =
                    user.getUsername();
        }


        if (
                displayName == null
                        || displayName.isBlank()
        ) {

            displayName =
                    "Employee";
        }


        // =====================================================
        // EMPLOYEE ID
        // =====================================================

        String employeeId =
                safe(
                        user.getEmployeeId()
                );


        // =====================================================
        // DEPARTMENT
        // =====================================================

        String department =
                safe(
                        user.getDepartment()
                );


        // =====================================================
        // UPDATE UI
        // =====================================================

        topNameLabel.setText(
                displayName
        );


        if (!"-".equals(employeeId)) {

            topRoleLabel.setText(
                    "EMPLOYEE  •  "
                            + employeeId
            );

        } else {

            topRoleLabel.setText(
                    "EMPLOYEE"
            );
        }


        welcomeNameLabel.setText(
                displayName
        );


        employeeIdLabel.setText(
                employeeId
        );


        departmentLabel.setText(
                department
        );
    }
        @FXML
        private void handleEscalationRules(ActionEvent event) {
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

                NavigationManager.navigate(
                stage,
                "/fxml/employee/employeeProfile.fxml"
                );
        }

    // =========================================================
    // HOME
    // =========================================================

    @FXML
    private void handleHome() {

        // Current page
    }


    // =========================================================
    // PROFILE
    // =========================================================

    @FXML
    private void handleProfile(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/employee/employeeProfile.fxml"
        );
    }


    // =========================================================
    // TASKS
    // =========================================================

    @FXML
    private void handleTasks(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/employee/employeeTask.fxml"
        );
    }


    // =========================================================
    // PERFORMANCE
    // =========================================================

    @FXML
    private void handleKpi(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/employee/employeePerformance.fxml"
        );
    }


    // =========================================================
    // REPORTS
    // =========================================================

    @FXML
    private void handleReports() {

        System.out.println(
                "Employee Reports not implemented yet."
        );
    }


    // =========================================================
    // ANONYMOUS REPORT
    // =========================================================

    @FXML
    private void handleAnonymousReport() {

        System.out.println(
                "Anonymous Reporting not implemented yet."
        );
    }


    // =========================================================
    // LOGOUT
    // =========================================================

    @FXML
    private void handleLogout(
            ActionEvent event
    ) {

        SessionManager.clearSession();


        navigate(
                event,
                "/fxml/auth/login.fxml"
        );
    }


    // =========================================================
    // HELPERS
    // =========================================================

    private String safe(
            String value
    ) {

        return value == null
                || value.isBlank()
                ? "-"
                : value;
    }


    private void navigate(
            ActionEvent event,
            String path
    ) {

        Stage stage =
                (Stage) ((Node) event.getSource())
                        .getScene()
                        .getWindow();


        NavigationManager.navigate(
                stage,
                path
        );
    }
}