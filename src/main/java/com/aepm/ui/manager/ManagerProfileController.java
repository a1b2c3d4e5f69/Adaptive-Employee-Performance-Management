package com.aepm.ui.manager;

import com.aepm.logic.user.UserService;
import com.aepm.model.User;
import com.aepm.ui.components.UIHelper;
import com.aepm.ui.navigation.NavigationManager;
import com.aepm.ui.common.chat.ChatController;
import com.aepm.util.SessionManager;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.geometry.Rectangle2D;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;

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
    private BorderPane pageRoot;

    @FXML
    private Button currentNavButton;

    @FXML
    private Button chatNavButton;

    private ChatController chatController;


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
                        46,
                        46,
                        46
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

                profileImageView.setFitWidth(
                        92
                );

                profileImageView.setFitHeight(
                        92
                );

                profileImageView.setPreserveRatio(
                        false
                );

                profileImageView.setSmooth(
                        true
                );

                profileImageView.setClip(
                        new Circle(
                                46,
                                46,
                                46
                        )
                );

                profileImageView.setImage(
                        image
                );

                /*
                 * JavaFX background-loading images can initially report
                 * width/height as 0. Apply the center crop only after the
                 * real image dimensions are available.
                 */
                Runnable applyCoverCrop =
                        () -> {

                            double sourceWidth =
                                    image.getWidth();

                            double sourceHeight =
                                    image.getHeight();

                            if (
                                    sourceWidth <= 0
                                            || sourceHeight <= 0
                            ) {
                                return;
                            }

                            double cropSize =
                                    Math.min(
                                            sourceWidth,
                                            sourceHeight
                                    );

                            double cropX =
                                    (sourceWidth - cropSize)
                                            / 2.0;

                            double cropY =
                                    (sourceHeight - cropSize)
                                            / 2.0;

                            profileImageView.setViewport(
                                    new Rectangle2D(
                                            cropX,
                                            cropY,
                                            cropSize,
                                            cropSize
                                    )
                            );

                            profileImageView.setVisible(
                                    true
                            );

                            profileImageView.setManaged(
                                    true
                            );

                            profileInitialLabel.setVisible(
                                    false
                            );

                            profileInitialLabel.setManaged(
                                    false
                            );
                        };


                if (
                        image.getWidth() > 0
                                && image.getHeight() > 0
                ) {

                    applyCoverCrop.run();

                } else {

                    image.progressProperty()
                            .addListener(
                                    (
                                            observable,
                                            oldValue,
                                            newValue
                                    ) -> {

                                        if (
                                                newValue.doubleValue()
                                                        >= 1.0
                                        ) {

                                            applyCoverCrop.run();
                                        }
                                    }
                            );
                }


                image.errorProperty()
                        .addListener(
                                (
                                        observable,
                                        oldValue,
                                        hasError
                                ) -> {

                                    if (
                                            Boolean.TRUE.equals(
                                                    hasError
                                            )
                                    ) {

                                        showProfileInitial(
                                                user
                                        );
                                    }
                                }
                        );

                return;

            } catch (Exception ignored) {

                showProfileInitial(
                        user
                );

                return;
            }
        }


        showProfileInitial(
                user
        );
    }


    private void showProfileInitial(
            User user
    ) {

        profileImageView.setImage(
                null
        );

        profileImageView.setViewport(
                null
        );

        profileImageView.setVisible(
                false
        );

        profileImageView.setManaged(
                false
        );

        profileInitialLabel.setVisible(
                true
        );

        profileInitialLabel.setManaged(
                true
        );


        String name =
                user == null
                        ? null
                        : user.getFullName();


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
    private void handleProfile(ActionEvent event) {
        navigate(event, "/fxml/manager/managerProfile.fxml");
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
    private void handleDashboard(ActionEvent event) {
        navigate(event, "/fxml/manager/managerHome.fxml");
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

        stopChatIfNeeded();

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
    // CHAT CENTER
    // =========================================================

    private void setChatNavigationActive() {

        if (currentNavButton != null) {
            currentNavButton.getStyleClass().removeAll(
                    "manager-nav-button",
                    "manager-nav-button-active"
            );
            currentNavButton.getStyleClass().add("manager-nav-button");
        }

        if (chatNavButton != null) {
            chatNavButton.getStyleClass().removeAll(
                    "manager-nav-button",
                    "manager-nav-button-active"
            );
            chatNavButton.getStyleClass().add("manager-nav-button-active");
        }
    }

    @FXML
    private void handleChat(ActionEvent event) {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/common/chat.fxml")
            );

            Parent chatRoot = loader.load();

            if (chatController != null) {
                chatController.stop();
                chatController = null;
            }

            chatController = loader.getController();

            setChatNavigationActive();

            pageRoot.setCenter(chatRoot);

        } catch (Exception e) {

            System.err.println("Failed to open Manager Chat Center.");
            e.printStackTrace();
        }
    }

    private void stopChatIfNeeded() {

        if (chatController != null) {
            chatController.stop();
            chatController = null;
        }
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