package com.aepm.ui.manager;

import com.aepm.model.User;
import com.aepm.ui.navigation.NavigationManager;
import com.aepm.util.SessionManager;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class ManagerHomeController {

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label topNameLabel;

    @FXML
    private Label departmentLabel;

    @FXML
    private Label roleLabel;


    @FXML
    public void initialize() {

        User user =
                SessionManager.getCurrentUser();

        if (user == null) {
            return;
        }

        String name =
                user.getFullName() == null
                        ? "Manager"
                        : user.getFullName();

        welcomeLabel.setText(
                "Welcome, " + name
        );

        topNameLabel.setText(
                name
        );

        departmentLabel.setText(
                user.getDepartment() == null
                        ? "-"
                        : user.getDepartment()
        );

        roleLabel.setText(
                "Manager"
        );
    }


    // =========================================================
    // HOME
    // =========================================================

    @FXML
    private void handleHome() {
        // already on Home page
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
    private void handlePerformance() {

        System.out.println(
                "Performance not implemented yet."
        );
    }


    @FXML
    private void handleReports() {

        System.out.println(
                "Reports not implemented yet."
        );
    }


    @FXML
    private void handleDashboard() {

        System.out.println(
                "Dashboard not implemented yet."
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