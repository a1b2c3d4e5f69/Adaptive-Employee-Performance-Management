package com.aepm.ui.admin;

import com.aepm.model.User;
import com.aepm.ui.navigation.NavigationManager;
import com.aepm.util.SessionManager;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import javafx.scene.Node;
import javafx.scene.control.Label;

import javafx.stage.Stage;

public class AdminHomeController {

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label topNameLabel;


    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {

        User user =
                SessionManager.getCurrentUser();


        if (user == null) {
            return;
        }


        String name =
                user.getFullName() == null
                        ? "Administrator"
                        : user.getFullName();


        if (welcomeLabel != null) {

            welcomeLabel.setText(
                    "Welcome, " + name
            );
        }


        if (topNameLabel != null) {

            topNameLabel.setText(
                    name
            );
        }
    }


    // =========================================================
    // HOME
    // =========================================================

    @FXML
    private void handleHome() {

        // Already on Admin Home page.
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
                "/fxml/admin/adminProfile.fxml"
        );
    }


    // =========================================================
    // USER MANAGEMENT
    // =========================================================

    @FXML
    private void handleUserManagement(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/admin/adminUserManagement.fxml"
        );
    }


    // =========================================================
    // TASK MANAGEMENT
    // =========================================================

    @FXML
    private void handleTaskManagement() {

        System.out.println(
                "Admin Task Management not implemented yet."
        );
    }


    // =========================================================
    // KPI MANAGEMENT
    // =========================================================

    @FXML
    private void handleKpiManagement() {

        System.out.println(
                "Admin KPI Management not implemented yet."
        );
    }


    // =========================================================
    // REPORTS
    // =========================================================

    @FXML
    private void handleReports() {

        System.out.println(
                "Admin Reports not implemented yet."
        );
    }


    // =========================================================
    // ANONYMOUS REPORTS
    // =========================================================

    @FXML
    private void handleAnonymousReports() {

        System.out.println(
                "Admin Anonymous Reports not implemented yet."
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
    // NAVIGATION
    // =========================================================

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