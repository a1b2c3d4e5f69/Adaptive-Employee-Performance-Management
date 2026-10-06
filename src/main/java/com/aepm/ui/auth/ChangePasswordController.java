package com.aepm.ui.auth;

import com.aepm.logic.auth.AuthService;
import com.aepm.ui.components.UIHelper;
import com.aepm.ui.navigation.NavigationManager;

import javafx.concurrent.Task;
import javafx.fxml.FXML;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import javafx.stage.Stage;

public class ChangePasswordController {

    // =========================================================
    // CURRENT PASSWORD
    // =========================================================

    @FXML
    private PasswordField currentPasswordField;

    @FXML
    private TextField currentPasswordVisibleField;

    @FXML
    private Button currentPasswordEyeButton;


    // =========================================================
    // NEW PASSWORD
    // =========================================================

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private TextField newPasswordVisibleField;

    @FXML
    private Button newPasswordEyeButton;


    // =========================================================
    // CONFIRM PASSWORD
    // =========================================================

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private TextField confirmPasswordVisibleField;

    @FXML
    private Button confirmPasswordEyeButton;


    // =========================================================
    // UI
    // =========================================================

    @FXML
    private Label messageLabel;

    @FXML
    private Button changePasswordButton;


    private final AuthService authService =
            new AuthService();


    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {

        UIHelper.hideMessage(
                messageLabel
        );
    }


    // =========================================================
    // CHANGE PASSWORD
    // =========================================================

    @FXML
    private void handleChangePassword() {

        String currentPassword =
                getPasswordValue(
                        currentPasswordField,
                        currentPasswordVisibleField
                );


        String newPassword =
                getPasswordValue(
                        newPasswordField,
                        newPasswordVisibleField
                );


        String confirmPassword =
                getPasswordValue(
                        confirmPasswordField,
                        confirmPasswordVisibleField
                );


        UIHelper.hideMessage(
                messageLabel
        );


        // Current password validation

        if (
                currentPassword == null
                        || currentPassword.isBlank()
        ) {

            UIHelper.showError(
                    messageLabel,
                    "Current password is required."
            );

            return;
        }


        // New password validation

        if (
                newPassword == null
                        || newPassword.isBlank()
        ) {

            UIHelper.showError(
                    messageLabel,
                    "New password is required."
            );

            return;
        }


        if (newPassword.length() < 6) {

            UIHelper.showError(
                    messageLabel,
                    "New password must contain at least 6 characters."
            );

            return;
        }


        // Prevent same password

        if (currentPassword.equals(newPassword)) {

            UIHelper.showError(
                    messageLabel,
                    "New password must be different from the current password."
            );

            return;
        }


        // Confirm password validation

        if (
                confirmPassword == null
                        || confirmPassword.isBlank()
        ) {

            UIHelper.showError(
                    messageLabel,
                    "Please confirm your new password."
            );

            return;
        }


        if (!newPassword.equals(confirmPassword)) {

            UIHelper.showError(
                    messageLabel,
                    "New passwords do not match."
            );

            return;
        }


        setLoading(
                true
        );


        Task<Void> task =
                new Task<>() {

                    @Override
                    protected Void call()
                            throws Exception {

                        authService.changePassword(
                                currentPassword,
                                newPassword
                        );

                        return null;
                    }
                };


        // =====================================================
        // SUCCESS
        // =====================================================

        task.setOnSucceeded(
                event -> {

                    setLoading(
                            false
                    );


                    clearPasswordFields();


                    UIHelper.showSuccess(
                            messageLabel,
                            "Password changed successfully."
                    );
                }
        );


        // =====================================================
        // FAILED
        // =====================================================

        task.setOnFailed(
                event -> {

                    setLoading(
                            false
                    );


                    Throwable exception =
                            task.getException();


                    String message =
                            exception == null

                                    ? "Unable to change password."

                                    : exception.getMessage();


                    /*
                     * AuthRepository may return
                     * "Invalid login credentials"
                     * when current password is wrong.
                     */

                    if (
                            message != null
                                    && (
                                    message
                                            .toLowerCase()
                                            .contains(
                                                    "invalid login"
                                            )

                                    || message
                                            .toLowerCase()
                                            .contains(
                                                    "invalid email or password"
                                            )
                            )
                    ) {

                        message =
                                "Current password is incorrect.";
                    }


                    UIHelper.showError(
                            messageLabel,
                            message
                    );
                }
        );


        Thread thread =
                new Thread(
                        task
                );


        thread.setDaemon(
                true
        );


        thread.start();
    }


    // =========================================================
    // CURRENT PASSWORD EYE
    // =========================================================

    @FXML
    private void toggleCurrentPassword() {

        togglePasswordVisibility(
                currentPasswordField,
                currentPasswordVisibleField,
                currentPasswordEyeButton
        );
    }


    // =========================================================
    // NEW PASSWORD EYE
    // =========================================================

    @FXML
    private void toggleNewPassword() {

        togglePasswordVisibility(
                newPasswordField,
                newPasswordVisibleField,
                newPasswordEyeButton
        );
    }


    // =========================================================
    // CONFIRM PASSWORD EYE
    // =========================================================

    @FXML
    private void toggleConfirmPassword() {

        togglePasswordVisibility(
                confirmPasswordField,
                confirmPasswordVisibleField,
                confirmPasswordEyeButton
        );
    }


    // =========================================================
    // TOGGLE HELPER
    // =========================================================

    private void togglePasswordVisibility(
            PasswordField passwordField,
            TextField visibleField,
            Button eyeButton
    ) {

        /*
         * Password currently hidden
         * → show password
         */

        if (passwordField.isVisible()) {

            visibleField.setText(
                    passwordField.getText()
            );


            visibleField.setVisible(
                    true
            );


            visibleField.setManaged(
                    true
            );


            passwordField.setVisible(
                    false
            );


            passwordField.setManaged(
                    false
            );


            eyeButton.setText(
                    "🙈"
            );


            visibleField.requestFocus();


            visibleField.positionCaret(
                    visibleField
                            .getText()
                            .length()
            );

        } else {

            /*
             * Password currently visible
             * → hide password
             */

            passwordField.setText(
                    visibleField.getText()
            );


            passwordField.setVisible(
                    true
            );


            passwordField.setManaged(
                    true
            );


            visibleField.setVisible(
                    false
            );


            visibleField.setManaged(
                    false
            );


            eyeButton.setText(
                    "👁"
            );


            passwordField.requestFocus();


            passwordField.positionCaret(
                    passwordField
                            .getText()
                            .length()
            );
        }
    }


    // =========================================================
    // GET ACTUAL PASSWORD
    // =========================================================

    private String getPasswordValue(
            PasswordField passwordField,
            TextField visibleField
    ) {

        if (visibleField.isVisible()) {

            return visibleField
                    .getText();
        }


        return passwordField
                .getText();
    }


    // =========================================================
    // CLEAR PASSWORD FIELDS
    // =========================================================

    private void clearPasswordFields() {

        currentPasswordField.clear();
        currentPasswordVisibleField.clear();

        newPasswordField.clear();
        newPasswordVisibleField.clear();

        confirmPasswordField.clear();
        confirmPasswordVisibleField.clear();


        // Return all fields to hidden mode

        currentPasswordField.setVisible(
                true
        );

        currentPasswordField.setManaged(
                true
        );

        currentPasswordVisibleField.setVisible(
                false
        );

        currentPasswordVisibleField.setManaged(
                false
        );


        newPasswordField.setVisible(
                true
        );

        newPasswordField.setManaged(
                true
        );

        newPasswordVisibleField.setVisible(
                false
        );

        newPasswordVisibleField.setManaged(
                false
        );


        confirmPasswordField.setVisible(
                true
        );

        confirmPasswordField.setManaged(
                true
        );

        confirmPasswordVisibleField.setVisible(
                false
        );

        confirmPasswordVisibleField.setManaged(
                false
        );


        currentPasswordEyeButton.setText(
                "👁"
        );

        newPasswordEyeButton.setText(
                "👁"
        );

        confirmPasswordEyeButton.setText(
                "👁"
        );
    }


    // =========================================================
    // BACK
    // =========================================================

    @FXML
    private void handleBack() {

        Stage stage =
                (Stage) changePasswordButton
                        .getScene()
                        .getWindow();


        NavigationManager.navigate(
                stage,
                "/fxml/employee/employeeProfile.fxml"
        );
    }


    // =========================================================
    // LOADING
    // =========================================================

    private void setLoading(
            boolean loading
    ) {

        currentPasswordField.setDisable(
                loading
        );

        currentPasswordVisibleField.setDisable(
                loading
        );

        currentPasswordEyeButton.setDisable(
                loading
        );


        newPasswordField.setDisable(
                loading
        );

        newPasswordVisibleField.setDisable(
                loading
        );

        newPasswordEyeButton.setDisable(
                loading
        );


        confirmPasswordField.setDisable(
                loading
        );

        confirmPasswordVisibleField.setDisable(
                loading
        );

        confirmPasswordEyeButton.setDisable(
                loading
        );


        changePasswordButton.setDisable(
                loading
        );


        changePasswordButton.setText(
                loading
                        ? "Updating..."
                        : "Change Password"
        );
    }
}