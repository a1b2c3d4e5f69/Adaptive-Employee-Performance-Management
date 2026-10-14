package com.aepm.ui.admin;

import com.aepm.model.User;
import com.aepm.ui.navigation.NavigationManager;
import com.aepm.util.SessionManager;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.stage.Stage;


import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;


public class AdminHomeController {

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label topNameLabel;


    @FXML
    public void initialize() {

        loadCurrentUser();
    }


        @FXML
        private void handleEscalationRules(ActionEvent event) {

        Stage stage = (Stage) ((Node) event.getSource())
                .getScene().getWindow();

        NavigationManager.navigate(
                stage,
                "/fxml/admin/adminEscalationRules.fxml"
        );
        }


    private void loadCurrentUser() {

        User user =
                SessionManager.getCurrentUser();


        if (user == null) {

            welcomeLabel.setText(
                    "Welcome, Administrator"
            );

            topNameLabel.setText(
                    "Administrator"
            );

            return;
        }


        String name =
                user.getFullName();


        if (
                name == null
                        || name.isBlank()
        ) {

            name =
                    user.getUsername();
        }


        if (
                name == null
                        || name.isBlank()
        ) {

            name =
                    "Administrator";
        }


        welcomeLabel.setText(
                "Welcome, " + name
        );


        topNameLabel.setText(
                name
        );
    }


    @FXML
    private void handleHome() {

        // Already on Home page.
    }


    @FXML
    private void handleProfile(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/admin/adminProfile.fxml"
        );
    }


    @FXML
    private void handleUserManagement(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/admin/adminUserManagement.fxml"
        );
    }


    @FXML
    private void handleTaskManagement(ActionEvent event) {

        navigate(
                event,
                "/fxml/admin/adminTask.fxml"
        );
    }


    @FXML
    private void handleKpiManagement(ActionEvent event) {

        navigate(
                event,
                "/fxml/admin/adminKpi.fxml"
        );
    }

   @FXML
   private void handleSystemConfiguration(ActionEvent event) {
        navigate(event, "/fxml/admin/systemConfiguration.fxml");
   }
        
        

@FXML
private void handleAttendanceManagement(ActionEvent event) {

    try {

        String accessToken = SessionManager.getAccessToken();

        if (accessToken == null || accessToken.isBlank()) {

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

        controller.setAccessToken(accessToken);

        Stage stage = (Stage) ((Node) event.getSource())
                .getScene()
                .getWindow();

        stage.getScene().setRoot(root);

        System.out.println(
            "Attendance Management opened successfully."
        );

    } catch (Exception e) {

        System.err.println(
            "Failed to open Attendance Management:"
        );

        e.printStackTrace();
    }
}


    @FXML
    private void handleReports() {

        System.out.println(
                "Reports not implemented yet."
        );
    }


    @FXML
    private void handleAnonymousReports() {

        System.out.println(
                "Anonymous Reports not implemented yet."
        );
    }


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