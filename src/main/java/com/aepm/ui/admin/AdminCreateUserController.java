package com.aepm.ui.admin;

import com.aepm.logic.user.UserService;
import com.aepm.ui.components.UIHelper;
import com.aepm.ui.navigation.NavigationManager;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;

import javafx.event.ActionEvent;

import javafx.fxml.FXML;

import javafx.scene.Node;

import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import javafx.stage.Stage;

public class AdminCreateUserController {

    @FXML
    private TextField fullNameField;

    @FXML
    private TextField employeeIdField;

    @FXML
    private TextField departmentField;

    @FXML
    private TextField usernameField;

    @FXML
    private TextField emailField;

    @FXML
    private ComboBox<String> roleComboBox;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label messageLabel;

    @FXML
    private Button createButton;


    private final UserService userService =
            new UserService();


    @FXML
    public void initialize() {

        System.out.println(
                "AdminCreateUserController initialize()"
        );


        UIHelper.hideMessage(
                messageLabel
        );


        roleComboBox.setItems(
                FXCollections.observableArrayList(
                        "Employee",
                        "Manager"
                )
        );


        roleComboBox.setValue(
                "Employee"
        );
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

        @FXML
        private void handleAIConfiguration(ActionEvent event) {
                Stage stage = (Stage) ((Node) event.getSource())
                        .getScene().getWindow();

                NavigationManager.navigate(
                        stage,
                        "/fxml/admin/adminAIConfiguration.fxml"
                );
        }


    // =========================================================
    // CREATE
    // =========================================================

    @FXML
    private void handleCreateUser() {

        String fullName =
                fullNameField.getText().trim();

        String employeeId =
                employeeIdField.getText().trim();

        String department =
                departmentField.getText().trim();

        String username =
                usernameField.getText().trim();

        String email =
                emailField.getText().trim();

        String password =
                passwordField.getText();

        String confirmPassword =
                confirmPasswordField.getText();

        String role =
                roleComboBox.getValue();


        String validation =
                userService.validateCreateUser(
                        fullName,
                        employeeId,
                        department,
                        username,
                        email,
                        password,
                        confirmPassword,
                        role
                );


        if (validation != null) {

            UIHelper.showError(
                    messageLabel,
                    validation
            );

            return;
        }


        createButton.setDisable(true);


        Task<Void> task =
                new Task<>() {

                    @Override
                    protected Void call()
                            throws Exception {

                        userService.createUser(
                                fullName,
                                employeeId,
                                department,
                                username,
                                email,
                                password,
                                confirmPassword,
                                role
                        );

                        return null;
                    }
                };


        task.setOnSucceeded(
                event -> {

                    createButton.setDisable(false);

                    UIHelper.showSuccess(
                            messageLabel,
                            role
                                    + " account created successfully."
                    );

                    clearForm();
                }
        );


        task.setOnFailed(
                event -> {

                    createButton.setDisable(false);


                    Throwable error =
                            task.getException();


                    UIHelper.showError(
                            messageLabel,
                            error == null
                                    ? "Unable to create user."
                                    : error.getMessage()
                    );
                }
        );


        Thread thread =
                new Thread(task);

        thread.setDaemon(true);

        thread.start();
    }


    // =========================================================
    // BACK
    // =========================================================

    @FXML
    private void handleBack(
            ActionEvent event
    ) {

        Stage stage =
                (Stage) ((Node) event.getSource())
                        .getScene()
                        .getWindow();


        NavigationManager.navigate(
                stage,
                "/fxml/admin/adminUserManagement.fxml"
        );
    }


    private void clearForm() {

        fullNameField.clear();
        employeeIdField.clear();
        departmentField.clear();
        usernameField.clear();
        emailField.clear();

        passwordField.clear();
        confirmPasswordField.clear();

        roleComboBox.setValue("Employee");
    }
}