package com.aepm.ui.auth;

import com.aepm.logic.auth.RegisterService;

import com.aepm.ui.components.UIHelper;
import com.aepm.ui.navigation.NavigationManager;

import javafx.concurrent.Task;

import javafx.fxml.FXML;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;

import javafx.stage.Stage;

public class RegisterController {

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
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label messageLabel;

    @FXML
    private Button registerButton;

    @FXML
    private ProgressIndicator loadingIndicator;


    private final RegisterService registerService =
            new RegisterService();


    @FXML
    public void initialize() {

        UIHelper.hideMessage(
                messageLabel
        );

        loadingIndicator.setVisible(
                false
        );

        loadingIndicator.setManaged(
                false
        );
    }


    @FXML
    private void handleRegister() {

        String fullName =
                fullNameField
                        .getText()
                        .trim();

        String employeeId =
                employeeIdField
                        .getText()
                        .trim();

        String department =
                departmentField
                        .getText()
                        .trim();

        String username =
                usernameField
                        .getText()
                        .trim();

        String email =
                emailField
                        .getText()
                        .trim();

        String password =
                passwordField
                        .getText();

        String confirmPassword =
                confirmPasswordField
                        .getText();


        String validation =
                registerService
                        .validateRegistration(
                                fullName,
                                employeeId,
                                department,
                                username,
                                email,
                                password,
                                confirmPassword
                        );


        if (validation != null) {

            UIHelper.showError(
                    messageLabel,
                    validation
            );

            return;
        }


        UIHelper.hideMessage(
                messageLabel
        );


        setLoading(true);


        Task<Void> registrationTask =
                new Task<>() {

                    @Override
                    protected Void call()
                            throws Exception {

                        registerService
                                .registerEmployee(
                                        fullName,
                                        employeeId,
                                        department,
                                        username,
                                        email,
                                        password,
                                        confirmPassword
                                );

                        return null;
                    }
                };


        registrationTask.setOnSucceeded(
                event -> {

                    setLoading(false);


                    UIHelper.showSuccess(
                            messageLabel,
                            "Account created successfully. You can now sign in."
                    );


                    registerButton.setText(
                            "Registration Successful"
                    );
                }
        );


        registrationTask.setOnFailed(
                event -> {

                    setLoading(false);


                    Throwable exception =
                            registrationTask
                                    .getException();


                    String message =
                            exception == null

                                    ? "Unable to create account."

                                    : exception.getMessage();


                    UIHelper.showError(
                            messageLabel,
                            message
                    );
                }
        );


        Thread thread =
                new Thread(
                        registrationTask
                );


        thread.setDaemon(
                true
        );


        thread.start();
    }


    @FXML
    private void goToLogin() {

        Stage stage =
                (Stage) registerButton
                        .getScene()
                        .getWindow();


        NavigationManager.navigate(
                stage,
                "/fxml/auth/login.fxml"
        );
    }


    private void setLoading(
            boolean loading
    ) {

        fullNameField.setDisable(
                loading
        );

        employeeIdField.setDisable(
                loading
        );

        departmentField.setDisable(
                loading
        );

        usernameField.setDisable(
                loading
        );

        emailField.setDisable(
                loading
        );

        passwordField.setDisable(
                loading
        );

        confirmPasswordField.setDisable(
                loading
        );


        registerButton.setDisable(
                loading
        );


        loadingIndicator.setVisible(
                loading
        );


        loadingIndicator.setManaged(
                loading
        );


        registerButton.setText(
                loading
                        ? "Creating Account..."
                        : "Create Account"
        );
    }
}