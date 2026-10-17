package com.aepm.ui.auth;

import com.aepm.logic.auth.AuthService;

import com.aepm.model.Role;
import com.aepm.model.User;

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
import com.aepm.logic.deadline.DeadlineEscalationScheduler;
import com.aepm.util.SessionManager;

public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    private Button loginButton;

    @FXML
    private ProgressIndicator loadingIndicator;


    private final AuthService authService =
            new AuthService();


    @FXML
    public void initialize() {

        UIHelper.hideMessage(
                errorLabel
        );

        loadingIndicator.setVisible(
                false
        );

        loadingIndicator.setManaged(
                false
        );
    }


    @FXML
    private void handleLogin() {

        String email =
                emailField
                        .getText()
                        .trim();

        String password =
                passwordField
                        .getText();


        String validationMessage =
                authService
                        .validateLoginInput(
                                email,
                                password
                        );


        if (validationMessage != null) {

            UIHelper.showError(
                    errorLabel,
                    validationMessage
            );

            return;
        }


        UIHelper.hideMessage(
                errorLabel
        );


        setLoading(true);


        Task<User> loginTask =
                new Task<>() {

                    @Override
                    protected User call()
                            throws Exception {

                        return authService.login(
                                email,
                                password
                        );
                    }
                };


        loginTask.setOnSucceeded(
                event -> {

                setLoading(false);

                User user = loginTask.getValue();

                navigateByRole(user);

                        // Only Admin starts the automatic deadline scheduler.
                        if (user.getRole() == Role.ADMINISTRATOR) {

                        DeadlineEscalationScheduler.getInstance()
                                .start(SessionManager::getAccessToken);

                        System.out.println(
                                "[Deadline Scheduler] Started for Admin."
                        );

                        } else {

                        System.out.println(
                                "[Deadline Scheduler] Not started for role: "
                                        + user.getRole()
                        );
                        }
                }
        );


        loginTask.setOnFailed(
                event -> {

                    setLoading(false);


                    Throwable exception =
                            loginTask
                                    .getException();


                    String message =
                            exception == null
                                    ? "Login failed."
                                    : exception.getMessage();


                    UIHelper.showError(
                            errorLabel,
                            message
                    );
                }
        );


        Thread thread =
                new Thread(
                        loginTask
                );


        thread.setDaemon(
                true
        );


        thread.start();
    }


    private void navigateByRole(User user) {

        Stage stage = (Stage) loginButton
                .getScene()
                .getWindow();

        Role role = user.getRole();

        switch (role) {

                case ADMINISTRATOR -> {

                        NavigationManager.navigate(
                                stage,
                                "/fxml/admin/adminHome.fxml"
                        );
                }

                case MANAGER -> {

                        NavigationManager.navigate(
                                stage,
                                "/fxml/manager/managerHome.fxml"
                        );
                }

                case EMPLOYEE -> {

                        NavigationManager.navigate(
                                stage,
                                "/fxml/employee/employeeHome.fxml"
                        );
                }
        }
    }


    private void setLoading(
            boolean loading
    ) {

        loginButton.setDisable(
                loading
        );


        emailField.setDisable(
                loading
        );


        passwordField.setDisable(
                loading
        );


        loadingIndicator.setVisible(
                loading
        );


        loadingIndicator.setManaged(
                loading
        );


        loginButton.setText(
                loading
                        ? "Signing In..."
                        : "Sign In"
        );
    }

    @FXML
    private void goToRegister() {

        Stage stage =
            (Stage) loginButton
                    .getScene()
                    .getWindow();

        NavigationManager.navigate(
            stage,
            "/fxml/auth/register.fxml"
        );
    }
}