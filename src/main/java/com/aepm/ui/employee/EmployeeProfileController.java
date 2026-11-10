package com.aepm.ui.employee;

import com.aepm.logic.user.UserService;
import com.aepm.model.User;
import com.aepm.ui.components.UIHelper;
import com.aepm.ui.navigation.NavigationManager;
import com.aepm.util.SessionManager;

import javafx.application.Platform;
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
import java.util.List;

public class EmployeeProfileController {

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
    // HEADER PROFILE
    // =========================================================

    @FXML
    private Label fullNameLabel;

    @FXML
    private Label departmentHeaderLabel;

    @FXML
    private Label employeeHeaderIdLabel;

    @FXML
    private Label headerAccountStatusLabel;


    // =========================================================
    // PERSONAL INFORMATION
    // =========================================================

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

    private User loadedUser;


    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {

        UIHelper.hideMessage(messageLabel);

        configureAvatar();

        /*
         * Display session information first,
         * then replace it with the complete DB record.
         */
        User sessionUser =
                SessionManager.getCurrentUser();

        if (sessionUser != null) {

            populateProfile(
                    sessionUser
            );
        }

        loadFullProfile();
    }


    // =========================================================
    // AVATAR
    // =========================================================

    private void configureAvatar() {

        /*
         * ImageView = 92 x 92
         */
        Circle clip =
                new Circle(
                        46,
                        46,
                        46
                );

        profileImageView.setClip(
                clip
        );
    }


    // =========================================================
    // LOAD COMPLETE USER FROM DATABASE
    // =========================================================

    private void loadFullProfile() {

        User sessionUser =
                SessionManager.getCurrentUser();

        if (sessionUser == null) {

            UIHelper.showError(
                    messageLabel,
                    "No logged-in user found."
            );

            return;
        }


        Task<User> task =
                new Task<>() {

                    @Override
                    protected User call()
                            throws Exception {

                        List<User> users =
                                userService.getAllUsers();

                        if (users == null) {
                            return null;
                        }

                        for (User user : users) {

                            if (user == null) {
                                continue;
                            }

                            if (
                                    isSameUser(
                                            sessionUser,
                                            user
                                    )
                            ) {

                                return user;
                            }
                        }

                        return null;
                    }
                };


        task.setOnSucceeded(event -> {

            User result =
                    task.getValue();

            if (result == null) {

                UIHelper.showError(
                        messageLabel,
                        "Unable to find your complete profile record."
                );

                return;
            }

            /*
             * Task.setOnSucceeded already runs on FX thread,
             * but this keeps it safe.
             */
            runOnFxThread(() -> {

                loadedUser = result;

                populateProfile(
                        result
                );

                UIHelper.hideMessage(
                        messageLabel
                );
            });
        });


        task.setOnFailed(event -> {

            Throwable error =
                    task.getException();

            if (error != null) {
                error.printStackTrace();
            }

            runOnFxThread(() ->
                    UIHelper.showError(
                            messageLabel,
                            error == null
                                    ? "Unable to load profile information."
                                    : getRootMessage(error)
                    )
            );
        });


        Thread thread =
                new Thread(
                        task,
                        "employee-profile-loader"
                );

        thread.setDaemon(true);
        thread.start();
    }


    // =========================================================
    // MATCH CURRENT USER
    // =========================================================

    private boolean isSameUser(
            User sessionUser,
            User databaseUser
    ) {

        /*
         * 1. Best match = user_id
         */
        if (
                hasText(sessionUser.getUserId())
                        &&
                        hasText(databaseUser.getUserId())
                        &&
                        sessionUser.getUserId()
                                .equalsIgnoreCase(
                                        databaseUser.getUserId()
                                )
        ) {

            return true;
        }


        /*
         * 2. Fallback = email
         */
        if (
                hasText(sessionUser.getEmail())
                        &&
                        hasText(databaseUser.getEmail())
                        &&
                        sessionUser.getEmail()
                                .equalsIgnoreCase(
                                        databaseUser.getEmail()
                                )
        ) {

            return true;
        }


        /*
         * 3. Fallback = username
         */
        if (
                hasText(sessionUser.getUsername())
                        &&
                        hasText(databaseUser.getUsername())
                        &&
                        sessionUser.getUsername()
                                .equalsIgnoreCase(
                                        databaseUser.getUsername()
                                )
        ) {

            return true;
        }


        return false;
    }


    // =========================================================
    // POPULATE PROFILE
    // =========================================================

    private void populateProfile(
            User user
    ) {

        if (user == null) {
            return;
        }


        String name =
                safeName(
                        user.getFullName()
                );

        String employeeId =
                safe(
                        user.getEmployeeId()
                );

        String username =
                safe(
                        user.getUsername()
                );

        String email =
                safe(
                        user.getEmail()
                );

        String department =
                safe(
                        user.getDepartment()
                );

        String accountStatus =
                safe(
                        user.getAccountStatus()
                );


        // TOP BAR
        if (welcomeLabel != null) {

            welcomeLabel.setText(
                    "Welcome, " + name
            );
        }

        if (topNameLabel != null) {

            topNameLabel.setText(
                    name
            );
        }


        // PROFILE HEADER
        fullNameLabel.setText(
                name
        );

        employeeHeaderIdLabel.setText(
                employeeId
        );

        departmentHeaderLabel.setText(
                department
        );

        headerAccountStatusLabel.setText(
                accountStatus
        );


        // PERSONAL INFO
        employeeIdLabel.setText(
                employeeId
        );

        usernameLabel.setText(
                username
        );

        emailLabel.setText(
                email
        );

        departmentLabel.setText(
                department
        );

        roleLabel.setText(
                "Employee"
        );

        accountStatusLabel.setText(
                accountStatus
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
                user == null
                        ? null
                        : user.getProfileImage();


        if (!hasText(imageUrl)) {

            showInitial(
                    firstLetter(
                            user == null
                                    ? null
                                    : user.getFullName()
                    )
            );

            return;
        }


        try {

            String separator =
                    imageUrl.contains("?")
                            ? "&"
                            : "?";

            String cacheBustedUrl =
                    imageUrl
                            + separator
                            + "t="
                            + System.currentTimeMillis();


            Image image =
                    new Image(
                            cacheBustedUrl,
                            true
                    );


            image.progressProperty()
                    .addListener(
                            (
                                    observable,
                                    oldValue,
                                    newValue
                            ) -> {

                                if (
                                        newValue.doubleValue()
                                                < 1.0
                                ) {

                                    return;
                                }

                                runOnFxThread(() -> {

                                    if (!image.isError()) {

                                        profileImageView.setImage(
                                                image
                                        );

                                        profileImageView.setVisible(
                                                true
                                        );

                                        profileInitialLabel.setVisible(
                                                false
                                        );

                                    } else {

                                        showInitial(
                                                firstLetter(
                                                        user.getFullName()
                                                )
                                        );
                                    }
                                });
                            }
                    );


            image.exceptionProperty()
                    .addListener(
                            (
                                    observable,
                                    oldException,
                                    newException
                            ) -> {

                                if (newException != null) {

                                    runOnFxThread(() ->
                                            showInitial(
                                                    firstLetter(
                                                            user.getFullName()
                                                    )
                                            )
                                    );
                                }
                            }
                    );


        } catch (Exception exception) {

            exception.printStackTrace();

            showInitial(
                    firstLetter(
                            user.getFullName()
                    )
            );
        }
    }


    // =========================================================
    // FALLBACK INITIAL
    // =========================================================

    private void showInitial(
            String initial
    ) {

        runOnFxThread(() -> {

            profileImageView.setImage(
                    null
            );

            profileImageView.setVisible(
                    false
            );

            profileInitialLabel.setVisible(
                    true
            );

            profileInitialLabel.setText(
                    hasText(initial)
                            ? initial
                            : "E"
            );
        });
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


        Task<String> uploadTask =
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


        uploadTask.setOnSucceeded(event2 -> {

            editImageButton.setDisable(
                    false
            );

            UIHelper.showSuccess(
                    messageLabel,
                    "Profile image updated successfully."
            );


            /*
             * Reload DB after successful upload.
             */
            loadFullProfile();
        });


        uploadTask.setOnFailed(event2 -> {

            editImageButton.setDisable(
                    false
            );

            Throwable error =
                    uploadTask.getException();

            if (error != null) {
                error.printStackTrace();
            }

            UIHelper.showError(
                    messageLabel,
                    error == null
                            ? "Unable to upload profile image."
                            : getRootMessage(error)
            );
        });


        Thread thread =
                new Thread(
                        uploadTask,
                        "profile-image-upload"
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


        if (
                !contact.isBlank()
                        &&
                        !contact.matches(
                                "[0-9+\\- ]{7,20}"
                        )
        ) {

            UIHelper.showError(
                    messageLabel,
                    "Please enter a valid contact number."
            );

            return;
        }


        saveButton.setDisable(
                true
        );

        UIHelper.hideMessage(
                messageLabel
        );


        Task<Void> saveTask =
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


        saveTask.setOnSucceeded(event -> {

            saveButton.setDisable(
                    false
            );

            UIHelper.showSuccess(
                    messageLabel,
                    "Profile updated successfully."
            );

            loadFullProfile();
        });


        saveTask.setOnFailed(event -> {

            saveButton.setDisable(
                    false
            );

            Throwable error =
                    saveTask.getException();

            if (error != null) {
                error.printStackTrace();
            }

            UIHelper.showError(
                    messageLabel,
                    error == null
                            ? "Unable to update profile."
                            : getRootMessage(error)
            );
        });


        Thread thread =
                new Thread(
                        saveTask,
                        "profile-contact-save"
                );

        thread.setDaemon(true);
        thread.start();
    }


    // =========================================================
    // NAVIGATION
    // =========================================================

    @FXML
    private void handleHome(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/employee/employeeHome.fxml"
        );
    }


    @FXML
    private void handleProfile(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/employee/employeeProfile.fxml"
        );
    }


    @FXML
    private void handleTasks(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/employee/employeeTask.fxml"
        );
    }


    @FXML
    private void handleAttendance(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/employee/employeeAttendance.fxml"
        );
    }


    @FXML
    private void handleKpi(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/employee/employeePerformance.fxml"
        );
    }


    @FXML
    private void handleAnonymousReport(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/employee/employeeAnonymousReport.fxml"
        );
    }


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


    // =========================================================
    // FX THREAD
    // =========================================================

    private void runOnFxThread(
            Runnable action
    ) {

        if (Platform.isFxApplicationThread()) {

            action.run();

        } else {

            Platform.runLater(
                    action
            );
        }
    }


    // =========================================================
    // HELPERS
    // =========================================================

    private boolean hasText(
            String value
    ) {

        return value != null
                && !value.isBlank();
    }


    private String safe(
            String value
    ) {

        return hasText(value)
                ? value
                : "-";
    }


    private String safeName(
            String value
    ) {

        return hasText(value)
                ? value
                : "Employee";
    }


    private String firstLetter(
            String value
    ) {

        if (!hasText(value)) {
            return "E";
        }

        return value
                .substring(
                        0,
                        1
                )
                .toUpperCase();
    }


    private String getRootMessage(
            Throwable error
    ) {

        if (error == null) {
            return "Unexpected error.";
        }


        Throwable current =
                error;

        while (current.getCause() != null) {

            current =
                    current.getCause();
        }


        return hasText(
                current.getMessage()
        )
                ? current.getMessage()
                : "Unexpected error.";
    }
}