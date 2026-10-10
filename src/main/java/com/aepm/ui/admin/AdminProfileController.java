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

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label topNameLabel;


    @FXML
    private ImageView profileImageView;

    @FXML
    private Label profileInitialLabel;

    @FXML
    private Label profileNameLabel;

    @FXML
    private Label profileRoleLabel;


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

    @FXML
    private Label messageLabel;

    @FXML
    private Button saveButton;


    private final UserService userService =
            new UserService();


    private User currentUser;


    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {

        UIHelper.hideMessage(
                messageLabel
        );


        createCircularImage();

        loadProfile();
    }


    // =========================================================
    // PROFILE
    // =========================================================

    private void loadProfile() {

        currentUser =
                SessionManager.getCurrentUser();


        if (currentUser == null) {

            UIHelper.showError(
                    messageLabel,
                    "Unable to load current user."
            );

            return;
        }


        String name =
                safeDisplay(
                        currentUser.getFullName(),
                        "Administrator"
                );


        welcomeLabel.setText(
                "Welcome, " + name
        );


        topNameLabel.setText(
                name
        );


        profileNameLabel.setText(
                name
        );


        profileRoleLabel.setText(
                "Administrator"
        );


        employeeIdLabel.setText(
                safeDisplay(
                        currentUser.getEmployeeId(),
                        "-"
                )
        );


        usernameLabel.setText(
                safeDisplay(
                        currentUser.getUsername(),
                        "-"
                )
        );


        emailLabel.setText(
                safeDisplay(
                        currentUser.getEmail(),
                        "-"
                )
        );


        departmentLabel.setText(
                safeDisplay(
                        currentUser.getDepartment(),
                        "-"
                )
        );


        roleLabel.setText(
                "Administrator"
        );


        accountStatusLabel.setText(
                safeDisplay(
                        currentUser.getAccountStatus(),
                        "-"
                )
        );


        contactNumberField.setText(
                currentUser.getContactNumber() == null
                        ? ""
                        : currentUser.getContactNumber()
        );


        String initial =
                name.isBlank()
                        ? "A"
                        : name
                        .substring(
                                0,
                                1
                        )
                        .toUpperCase();


        profileInitialLabel.setText(
                initial
        );


        loadProfileImage();
    }


    // =========================================================
    // IMAGE
    // =========================================================

    private void createCircularImage() {

        profileImageView.setFitWidth(
                120
        );


        profileImageView.setFitHeight(
                120
        );


        profileImageView.setPreserveRatio(
                false
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
    }


    private void loadProfileImage() {

        if (
                currentUser == null
                        || currentUser.getProfileImage() == null
                        || currentUser.getProfileImage().isBlank()
        ) {

            profileImageView.setVisible(
                    false
            );


            profileInitialLabel.setVisible(
                    true
            );


            return;
        }


        try {

            String imageUrl =
                    currentUser.getProfileImage()
                            + "?t="
                            + System.currentTimeMillis();


            Image image =
                    new Image(
                            imageUrl,
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

        } catch (Exception e) {

            profileImageView.setVisible(
                    false
            );


            profileInitialLabel.setVisible(
                    true
            );
        }
    }


    @FXML
    private void handleChangeProfileImage() {

        FileChooser fileChooser =
                new FileChooser();


        fileChooser.setTitle(
                "Select Profile Image"
        );


        fileChooser
                .getExtensionFilters()
                .add(
                        new FileChooser.ExtensionFilter(
                                "Image Files",
                                "*.png",
                                "*.jpg",
                                "*.jpeg"
                        )
                );


        Stage stage =
                (Stage) profileImageView
                        .getScene()
                        .getWindow();


        File selectedFile =
                fileChooser.showOpenDialog(
                        stage
                );


        if (selectedFile == null) {
            return;
        }


        if (
                selectedFile.length()
                        > 5L * 1024L * 1024L
        ) {

            UIHelper.showError(
                    messageLabel,
                    "Profile image must be 5 MB or smaller."
            );

            return;
        }


        Path imagePath =
                selectedFile.toPath();


        Task<String> uploadTask =
                new Task<>() {

                    @Override
                    protected String call()
                            throws Exception {

                        return userService
                                .uploadOwnProfileImage(
                                        imagePath
                                );
                    }
                };


        uploadTask.setOnSucceeded(
                event -> {

                    currentUser =
                            SessionManager.getCurrentUser();


                    loadProfileImage();


                    UIHelper.showSuccess(
                            messageLabel,
                            "Profile image updated successfully."
                    );
                }
        );


        uploadTask.setOnFailed(
                event -> {

                    Throwable error =
                            uploadTask.getException();


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
                        uploadTask
                );


        thread.setDaemon(true);

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
                event -> {

                    saveButton.setDisable(
                            false
                    );


                    currentUser =
                            SessionManager.getCurrentUser();


                    UIHelper.showSuccess(
                            messageLabel,
                            "Profile updated successfully."
                    );
                }
        );


        task.setOnFailed(
                event -> {

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
                new Thread(task);


        thread.setDaemon(true);

        thread.start();
    }


    // =========================================================
    // PASSWORD
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
    // SIDEBAR
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


    @FXML
    private void handleProfile() {

        // Current page.
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
    private void handleTaskManagement(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/admin/adminTask.fxml"
        );
    }


        @FXML
        private void handleKpiManagement(
                ActionEvent event
        ) {

        navigate(
                event,
                "/fxml/admin/adminKpi.fxml"
        );
        }


    @FXML
    private void handleReports() {
    }


    @FXML
    private void handleAnonymousReports() {
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


    // =========================================================
    // HELPERS
    // =========================================================

    private String safeDisplay(
            String value,
            String fallback
    ) {

        return value == null
                || value.isBlank()
                ? fallback
                : value;
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