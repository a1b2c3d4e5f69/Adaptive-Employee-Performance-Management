package com.aepm.ui.admin;

import com.aepm.logic.user.UserService;
import com.aepm.model.User;
import com.aepm.ui.components.UIHelper;
import com.aepm.ui.navigation.NavigationManager;
import com.aepm.util.SessionManager;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javafx.scene.shape.Circle;

import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;

public class AdminProfileController {

    // =========================================================
    // TOP BAR
    // =========================================================

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label topNameLabel;


    // =========================================================
    // PROFILE IMAGE
    // =========================================================

    @FXML
    private ImageView profileImageView;

    @FXML
    private Label profileInitialLabel;

    @FXML
    private Button editImageButton;


    // =========================================================
    // PROFILE INFORMATION
    // =========================================================

    @FXML
    private Label fullNameLabel;

    @FXML
    private Label employeeIdLabel;

    @FXML
    private Label usernameLabel;

    @FXML
    private Label emailLabel;

    @FXML
    private Label departmentLabel;

    @FXML
    private Label roleLabel;

    @FXML
    private Label accountStatusLabel;

    @FXML
    private TextField contactNumberField;


    // =========================================================
    // OTHER
    // =========================================================

    @FXML
    private Label messageLabel;

    @FXML
    private Button saveButton;


    private final UserService userService =
            new UserService();


    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {

        UIHelper.hideMessage(
                messageLabel
        );


        Circle clip =
                new Circle(
                        60,
                        60,
                        60
                );


        profileImageView.setClip(
                clip
        );


        loadProfile();
    }


    // =========================================================
    // LOAD PROFILE
    // =========================================================

    private void loadProfile() {

        User user =
                SessionManager.getCurrentUser();


        if (user == null) {

            UIHelper.showError(
                    messageLabel,
                    "No logged-in user found."
            );

            return;
        }


        String name =
                user.getFullName() == null
                        ? "Administrator"
                        : user.getFullName();


        welcomeLabel.setText(
                "Welcome, " + name
        );


        topNameLabel.setText(
                name
        );


        fullNameLabel.setText(
                name
        );


        employeeIdLabel.setText(
                safe(
                        user.getEmployeeId()
                )
        );


        usernameLabel.setText(
                safe(
                        user.getUsername()
                )
        );


        emailLabel.setText(
                safe(
                        user.getEmail()
                )
        );


        departmentLabel.setText(
                safe(
                        user.getDepartment()
                )
        );


        roleLabel.setText(
                "Administrator"
        );


        accountStatusLabel.setText(
                safe(
                        user.getAccountStatus()
                )
        );


        contactNumberField.setText(
                user.getContactNumber() == null
                        ? ""
                        : user.getContactNumber()
        );


        loadProfileImage(
                user
        );
    }


    // =========================================================
    // LOAD PROFILE IMAGE
    // =========================================================

    private void loadProfileImage(
            User user
    ) {

        String imageUrl =
                user.getProfileImage();


        if (
                imageUrl != null
                        && !imageUrl.isBlank()
        ) {

            try {

                Image image =
                        new Image(
                                imageUrl
                                        + "?t="
                                        + System.currentTimeMillis(),
                                true
                        );


                profileImageView.setImage(
                        image
                );


                profileImageView.setVisible(
                        true
                );


                profileInitialLabel.setVisible(
                        false
                );


                return;

            } catch (Exception ignored) {
            }
        }


        profileImageView.setImage(
                null
        );


        profileImageView.setVisible(
                false
        );


        profileInitialLabel.setVisible(
                true
        );


        String name =
                user.getFullName();


        if (
                name != null
                        && !name.isBlank()
        ) {

            profileInitialLabel.setText(
                    name.substring(
                            0,
                            1
                    ).toUpperCase()
            );

        } else {

            profileInitialLabel.setText(
                    "A"
            );
        }
    }


    // =========================================================
    // CHANGE PROFILE IMAGE
    // =========================================================

    @FXML
    private void handleChangeProfileImage(
            ActionEvent event
    ) {

        FileChooser chooser =
                new FileChooser();


        chooser.setTitle(
                "Select Profile Image"
        );


        chooser.getExtensionFilters()
                .add(
                        new FileChooser.ExtensionFilter(
                                "Image Files",
                                "*.png",
                                "*.jpg",
                                "*.jpeg"
                        )
                );


        File file =
                chooser.showOpenDialog(
                        getStage(event)
                );


        if (file == null) {
            return;
        }


        long maxSize =
                5L
                        * 1024
                        * 1024;


        if (file.length() > maxSize) {

            UIHelper.showError(
                    messageLabel,
                    "Profile image must be smaller than 5 MB."
            );

            return;
        }


        Path path =
                file.toPath();


        editImageButton.setDisable(
                true
        );


        UIHelper.hideMessage(
                messageLabel
        );


        Task<String> task =
                new Task<>() {

                    @Override
                    protected String call()
                            throws Exception {

                        return userService
                                .uploadOwnProfileImage(
                                        path
                                );
                    }
                };


        task.setOnSucceeded(
                e -> {

                    editImageButton.setDisable(
                            false
                    );


                    User user =
                            SessionManager
                                    .getCurrentUser();


                    if (user != null) {

                        loadProfileImage(
                                user
                        );
                    }


                    UIHelper.showSuccess(
                            messageLabel,
                            "Profile image updated successfully."
                    );
                }
        );


        task.setOnFailed(
                e -> {

                    editImageButton.setDisable(
                            false
                    );


                    Throwable error =
                            task.getException();


                    UIHelper.showError(
                            messageLabel,
                            error == null
                                    ? "Unable to upload profile image."
                                    : error.getMessage()
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
    // SAVE CONTACT
    // =========================================================

    @FXML
    private void handleSave() {

        String contact =
                contactNumberField
                        .getText()
                        .trim();


        saveButton.setDisable(
                true
        );


        Task<Void> task =
                new Task<>() {

                    @Override
                    protected Void call()
                            throws Exception {

                        userService
                                .updateOwnContactNumber(
                                        contact
                                );

                        return null;
                    }
                };


        task.setOnSucceeded(
                e -> {

                    saveButton.setDisable(
                            false
                    );


                    UIHelper.showSuccess(
                            messageLabel,
                            "Profile updated successfully."
                    );
                }
        );


        task.setOnFailed(
                e -> {

                    saveButton.setDisable(
                            false
                    );


                    Throwable error =
                            task.getException();


                    UIHelper.showError(
                            messageLabel,
                            error == null
                                    ? "Unable to update profile."
                                    : error.getMessage()
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
    // HOME
    // =========================================================

    @FXML
    private void handleHome(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/admin/adminHome.fxml"
        );
    }


    // =========================================================
    // PROFILE
    // =========================================================

    @FXML
    private void handleProfile(
        ActionEvent event
    ) {
        Stage stage =
            (Stage) ((Node) event.getSource())
                    .getScene()
                    .getWindow();
                    
            NavigationManager.navigate(
                 stage,
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
    // PLACEHOLDERS
    // =========================================================

    @FXML
    private void handleTaskManagement() {

        System.out.println(
                "Admin Task Management not implemented yet."
        );
    }


    @FXML
    private void handleKpiManagement() {

        System.out.println(
                "Admin KPI Management not implemented yet."
        );
    }


    @FXML
    private void handleReports() {

        System.out.println(
                "Admin Reports not implemented yet."
        );
    }


    @FXML
    private void handleAnonymousReports() {

        System.out.println(
                "Admin Anonymous Reports not implemented yet."
        );
    }


    // =========================================================
    // CHANGE PASSWORD
    // =========================================================

    @FXML
    private void handleChangePassword(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/auth/changePassword.fxml"
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

        NavigationManager.navigate(
                getStage(event),
                path
        );
    }


    private Stage getStage(
            ActionEvent event
    ) {

        return (Stage) ((Node) event.getSource())
                .getScene()
                .getWindow();
    }


    // =========================================================
    // HELPERS
    // =========================================================

    private String safe(
            String value
    ) {

        return value == null
                || value.isBlank()
                ? "-"
                : value;
    }
}