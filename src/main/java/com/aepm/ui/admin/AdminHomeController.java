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


    @FXML
    public void initialize() {

        User user =
                SessionManager.getCurrentUser();

        if (user != null) {

            String name =
                    user.getFullName() == null
                            ? "Administrator"
                            : user.getFullName();

            welcomeLabel.setText(
                    "Welcome, " + name
            );

            if (topNameLabel != null) {
                topNameLabel.setText(name);
            }
        }
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
    // TASK
    // =========================================================

    @FXML
    private void handleTaskManagement() {

        System.out.println(
                "Task Management not implemented yet."
        );
    }


    // =========================================================
    // KPI
    // =========================================================

    @FXML
    private void handleKpiManagement() {

        System.out.println(
                "KPI Management not implemented yet."
        );
    }


    // =========================================================
    // REPORT
    // =========================================================

    @FXML
    private void handleReports() {

        System.out.println(
                "Reports not implemented yet."
        );
    }


    // =========================================================
    // ANONYMOUS REPORT
    // =========================================================

    @FXML
    private void handleAnonymousReports() {

        System.out.println(
                "Anonymous Reports not implemented yet."
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