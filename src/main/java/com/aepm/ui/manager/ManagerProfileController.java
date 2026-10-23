package com.aepm.ui.manager;

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

public class ManagerProfileController {

    @FXML
    private Label topRoleLabel;
    
    @FXML
    private Label welcomeLabel;

    @FXML
    private Label topNameLabel;


    @FXML
    private ImageView profileImageView;

    @FXML
    private Label profileInitialLabel;

    @FXML
    private Button editImageButton;


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


    @FXML
    private Label messageLabel;

    @FXML
    private Button saveButton;


    private final UserService userService =
            new UserService();


    @FXML
    private void handleDeadlineEscalation(ActionEvent event) {    
        Stage stage = (Stage) ((Node) event.getSource())
                .getScene()
                .getWindow();
    
        NavigationManager.navigate(
                stage,
                "/fxml/manager/managerDeadlineEscalation.fxml"
        );
    }

    @FXML
    public void initialize() {
        updateManagerRoleLabel();
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


    private void updateManagerRoleLabel() {
        User manager = SessionManager.getCurrentUser();

        if (manager == null) {
                topRoleLabel.setText("MANAGER");
                return;
        }

        String managerId = manager.getEmployeeId();

        if (managerId != null && !managerId.isBlank()) {
                topRoleLabel.setText("MANAGER  •  " + managerId);
        } else {
                topRoleLabel.setText("MANAGER");
        }
    }

    private void loadProfile() {

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
                "Manager"
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
                    "M"
            );
        }
    }


    // =========================================================
    // PROFILE IMAGE
    // =========================================================

    @FXML
    private void handleChangeProfileImage(
            ActionEvent event
    ) {

        FileChooser chooser =
                new FileChooser();


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


        Path path =
                file.toPath();


        editImageButton.setDisable(
                true
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
                new Thread(task);

        thread.setDaemon(true);

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
                "/fxml/manager/managerHome.fxml"
        );
    }


    // =========================================================
    // PROFILE
    // =========================================================

    @FXML
    private void handleProfile() {
        // already on profile page
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

    @FXML
    private void handleTaskManagement(ActionEvent event) {

        navigate(
            event,
            "/fxml/manager/managerTask.fxml"

        );
    }


        @FXML
        private void handlePerformance(
                ActionEvent event
        ) {

        Stage stage =
                (Stage) ((Node) event.getSource())
                        .getScene()
                        .getWindow();


        NavigationManager.navigate(
                stage,
                "/fxml/manager/managerPerformance.fxml"
        );
        }

    @FXML
        private void handleReports(javafx.event.ActionEvent event) {
        javafx.stage.Window owner =
                ((javafx.scene.Node) event.getSource())
                        .getScene()
                        .getWindow();

        ManagerReportsPage.show(owner);
    }
    
    @FXML
    private void handleDashboard() {

        System.out.println(
                "Dashboard not implemented yet."
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


    private String safe(
            String value
    ) {

        return value == null
                || value.isBlank()
                ? "-"
                : value;
    }
}