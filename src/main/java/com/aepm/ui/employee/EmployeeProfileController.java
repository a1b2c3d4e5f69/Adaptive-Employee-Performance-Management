package com.aepm.ui.employee;

import com.aepm.data.repository.UserRepository;
import com.aepm.model.User;
import com.aepm.ui.components.UIHelper;
import com.aepm.ui.navigation.NavigationManager;
import com.aepm.util.SessionManager;

import javafx.concurrent.Task;
import javafx.fxml.FXML;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import javafx.stage.Stage;

public class EmployeeProfileController {

    @FXML
    private Label fullNameLabel;

    @FXML
    private Label employeeIdLabel;

    @FXML
    private Label usernameLabel;

    @FXML
    private Label emailLabel;

    @FXML
    private TextField contactNumberField;

    @FXML
    private Label departmentLabel;

    @FXML
    private Label roleLabel;

    @FXML
    private Label accountStatusLabel;

    @FXML
    private Label messageLabel;

    @FXML
    private Button saveButton;

    private final UserRepository userRepository =
            new UserRepository();

    private User currentUser;


    @FXML
    public void initialize() {

        UIHelper.hideMessage(messageLabel);

        currentUser =
                SessionManager.getCurrentUser();

        loadProfile();
    }


    private void loadProfile() {

        if (currentUser == null) {

            UIHelper.showError(
                    messageLabel,
                    "Unable to load user profile."
            );

            return;
        }

        fullNameLabel.setText(
                safe(currentUser.getFullName())
        );

        employeeIdLabel.setText(
                safe(currentUser.getEmployeeId())
        );

        usernameLabel.setText(
                safe(currentUser.getUsername())
        );

        emailLabel.setText(
                safe(currentUser.getEmail())
        );

        contactNumberField.setText(
                safe(currentUser.getContactNumber())
        );

        departmentLabel.setText(
                safe(currentUser.getDepartment())
        );

        roleLabel.setText(
                currentUser.getRole() == null
                        ? "-"
                        : currentUser.getRole()
                        .name()
                        .replace("_", " ")
        );

        accountStatusLabel.setText(
                safe(currentUser.getAccountStatus())
        );
    }


    @FXML
    private void handleSave() {

        String contactNumber =
                contactNumberField
                        .getText()
                        .trim();

        UIHelper.hideMessage(
                messageLabel
        );

        setLoading(true);

        Task<Void> updateTask =
                new Task<>() {

                    @Override
                    protected Void call()
                            throws Exception {

                        String accessToken =
                                SessionManager
                                        .getAccessToken();

                        userRepository
                                .updateContactNumber(
                                        currentUser.getUserId(),
                                        contactNumber,
                                        accessToken
                                );

                        return null;
                    }
                };


        updateTask.setOnSucceeded(
                event -> {

                    setLoading(false);

                    currentUser.setContactNumber(
                            contactNumber
                    );

                    UIHelper.showSuccess(
                            messageLabel,
                            "Profile updated successfully."
                    );
                }
        );


        updateTask.setOnFailed(
                event -> {

                    setLoading(false);

                    Throwable exception =
                            updateTask
                                    .getException();

                    UIHelper.showError(
                            messageLabel,
                            exception == null
                                    ? "Unable to update profile."
                                    : exception.getMessage()
                    );
                }
        );


        Thread thread =
                new Thread(
                        updateTask
                );

        thread.setDaemon(
                true
        );

        thread.start();
    }


    @FXML
    private void handleChangePassword() {

    Stage stage =
            (Stage) saveButton
                    .getScene()
                    .getWindow();


    NavigationManager.navigate(
            stage,
            "/fxml/auth/changepassword.fxml"
    );
    }


    @FXML
    private void handleBack() {

        Stage stage =
                (Stage) saveButton
                        .getScene()
                        .getWindow();

        NavigationManager.navigate(
                stage,
                "/fxml/employee/employeeHome.fxml"
        );
    }


    private void setLoading(
            boolean loading
    ) {

        saveButton.setDisable(
                loading
        );

        contactNumberField.setDisable(
                loading
        );

        saveButton.setText(
                loading
                        ? "Saving..."
                        : "Save Changes"
        );
    }


    private String safe(
            String value
    ) {

        return value == null
                || value.isBlank()
                ? "-"
                : value;
    }
}