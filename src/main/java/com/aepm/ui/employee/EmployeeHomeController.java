package com.aepm.ui.employee;

import com.aepm.model.User;
import com.aepm.ui.navigation.NavigationManager;
import com.aepm.util.SessionManager;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;

import javafx.stage.Stage;

public class EmployeeHomeController {

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label nameLabel;

    @FXML
    private Label roleLabel;

    @FXML
    private Label departmentLabel;

    @FXML
    private Label contentTitleLabel;

    @FXML
    private Label contentMessageLabel;


    @FXML
    public void initialize() {

        loadUserInformation();
    }


    // =========================================================
    // LOAD CURRENT USER
    // =========================================================

    private void loadUserInformation() {

        User user =
                SessionManager.getCurrentUser();


        if (user == null) {

            welcomeLabel.setText(
                    "Welcome"
            );

            nameLabel.setText(
                    "-"
            );

            roleLabel.setText(
                    "-"
            );

            departmentLabel.setText(
                    "-"
            );

            return;
        }


        String fullName =
                user.getFullName() == null
                        ? "Employee"
                        : user.getFullName();


        welcomeLabel.setText(
                "Welcome, " + fullName
        );


        nameLabel.setText(
                fullName
        );


        if (user.getRole() != null) {

            roleLabel.setText(
                    user.getRole()
                            .name()
                            .replace(
                                    "_",
                                    " "
                            )
            );

        } else {

            roleLabel.setText(
                    "-"
            );
        }


        if (
                user.getDepartment() != null
                        && !user.getDepartment().isBlank()
        ) {

            departmentLabel.setText(
                    user.getDepartment()
            );

        } else {

            departmentLabel.setText(
                    "-"
            );
        }
    }


    // =========================================================
    // HOME
    // =========================================================

    @FXML
    private void handleHome() {

        contentTitleLabel.setText(
                "Home"
        );

        contentMessageLabel.setText(
                "Welcome to the Adaptive Employee Performance Management System."
        );
    }

    // PROFILE
    @FXML
    private void handleProfile() {
        Stage stage =
            (Stage) welcomeLabel
                    .getScene()
                    .getWindow();
            
        NavigationManager.navigate(
            stage,
            "/fxml/employee/employeeProfile.fxml"
    );
}

    // TASK
    @FXML
    private void handleTasks(ActionEvent event) {

    Stage stage =
            (Stage) ((Node) event.getSource())
                    .getScene()
                    .getWindow();

    NavigationManager.navigate(
            stage,
            "/fxml/employee/employeeTask.fxml"
    );
}

    // KPI
    @FXML
    private void handleKpi() {

        contentTitleLabel.setText(
                "Performance KPI"
        );

        contentMessageLabel.setText(
                "KPI information will be available here."
        );
    }


    // =========================================================
    // REPORT
    // =========================================================

    @FXML
    private void handleReports() {

        contentTitleLabel.setText(
                "Performance Reports"
        );

        contentMessageLabel.setText(
                "Performance reports will be available here."
        );
    }


    // =========================================================
    // ANONYMOUS REPORT
    // =========================================================

    @FXML
    private void handleAnonymousReport() {

        contentTitleLabel.setText(
                "Anonymous Reporting"
        );

        contentMessageLabel.setText(
                "Anonymous reporting will be available here."
        );
    }


    // =========================================================
    // LOGOUT
    // =========================================================

    @FXML
    private void handleLogout() {

        SessionManager.clearSession();


        Stage stage =
                (Stage) welcomeLabel
                        .getScene()
                        .getWindow();


        NavigationManager.navigate(
                stage,
                "/fxml/auth/login.fxml"
        );
    }
}