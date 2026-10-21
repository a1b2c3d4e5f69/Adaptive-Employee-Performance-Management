package com.aepm.ui.admin;

import com.aepm.logic.user.UserService;
import com.aepm.model.Role;
import com.aepm.model.User;
import com.aepm.ui.components.UIHelper;
import com.aepm.ui.navigation.NavigationManager;
import com.aepm.util.SessionManager;

import javafx.beans.property.SimpleStringProperty;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

import javafx.concurrent.Task;

import javafx.event.ActionEvent;

import javafx.fxml.FXML;

import javafx.scene.Node;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import javafx.scene.control.cell.PropertyValueFactory;

import javafx.stage.Stage;

import java.util.List;
import java.util.Optional;

public class AdminUserManagementController {

    // =========================================================
    // TOP BAR
    // =========================================================

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label topNameLabel;


    // =========================================================
    // TABLE
    // =========================================================

    @FXML
    private TableView<User> userTable;

    @FXML
    private TableColumn<User, String> employeeIdColumn;

    @FXML
    private TableColumn<User, String> nameColumn;

    @FXML
    private TableColumn<User, String> usernameColumn;

    @FXML
    private TableColumn<User, String> emailColumn;

    @FXML
    private TableColumn<User, String> departmentColumn;

    @FXML
    private TableColumn<User, String> roleColumn;

    @FXML
    private TableColumn<User, String> statusColumn;


    // =========================================================
    // SEARCH / FILTER
    // =========================================================

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> roleFilterComboBox;


    // =========================================================
    // FORM
    // =========================================================

    @FXML
    private TextField employeeIdField;

    @FXML
    private TextField fullNameField;

    @FXML
    private TextField usernameField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField contactNumberField;

    @FXML
    private TextField departmentField;

    @FXML
    private ComboBox<Role> roleComboBox;

    @FXML
    private Label messageLabel;

    @FXML
    private Button saveButton;

    @FXML
    private Button statusButton;

    @FXML
    private Button deleteButton;

    @FXML
        private void handleAttendanceManagement(ActionEvent event) {

        Stage stage = (Stage) ((Node) event.getSource())
                .getScene()
                .getWindow();

        NavigationManager.navigate(
                stage,
                "/fxml/admin/adminAttendance.fxml"
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
    // DATA
    // =========================================================

    private final UserService userService =
            new UserService();


    private final ObservableList<User> users =
            FXCollections.observableArrayList();


    private FilteredList<User> filteredUsers;

    private User selectedUser;


    // =========================================================
    // INITIALIZE
    // =========================================================
   @FXML
   private void handleSystemConfiguration(ActionEvent event) {
        navigate(event, "/fxml/admin/systemConfiguration.fxml");
   }

    @FXML
    public void initialize() {

        UIHelper.hideMessage(
                messageLabel
        );


        loadCurrentUser();

        configureTable();

        configureRoleComboBox();

        configureFilters();

        configureSelection();

        clearSelectedUser();

        loadUsers();
    }


    // =========================================================
    // TOP USER
    // =========================================================

    private void loadCurrentUser() {

        User currentUser =
                SessionManager.getCurrentUser();


        if (currentUser == null) {
            return;
        }


        String name =
                currentUser.getFullName();


        if (
                name == null
                        || name.isBlank()
        ) {

            name =
                    currentUser.getUsername();
        }


        if (
                name == null
                        || name.isBlank()
        ) {

            name =
                    "Administrator";
        }


        welcomeLabel.setText(
                "Welcome, " + name
        );


        topNameLabel.setText(
                name
        );
    }


    // =========================================================
    // TABLE
    // =========================================================

    private void configureTable() {

        employeeIdColumn.setCellValueFactory(
                new PropertyValueFactory<>(
                        "employeeId"
                )
        );


        nameColumn.setCellValueFactory(
                new PropertyValueFactory<>(
                        "fullName"
                )
        );


        usernameColumn.setCellValueFactory(
                new PropertyValueFactory<>(
                        "username"
                )
        );


        emailColumn.setCellValueFactory(
                new PropertyValueFactory<>(
                        "email"
                )
        );


        departmentColumn.setCellValueFactory(
                new PropertyValueFactory<>(
                        "department"
                )
        );


        statusColumn.setCellValueFactory(
                new PropertyValueFactory<>(
                        "accountStatus"
                )
        );


        roleColumn.setCellValueFactory(
                cellData -> {

                    User user =
                            cellData.getValue();


                    String roleName =
                            user.getRole() == null
                                    ? "-"
                                    : formatRole(
                                            user.getRole()
                                    );


                    return new SimpleStringProperty(
                            roleName
                    );
                }
        );


        userTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );
    }


    // =========================================================
    // ROLE
    // =========================================================

    private void configureRoleComboBox() {

        roleComboBox.setItems(
                FXCollections.observableArrayList(
                        Role.values()
                )
        );
    }


    // =========================================================
    // SEARCH + FILTER
    // =========================================================

    private void configureFilters() {

        filteredUsers =
                new FilteredList<>(
                        users,
                        user -> true
                );


        userTable.setItems(
                filteredUsers
        );


        roleFilterComboBox.setItems(
                FXCollections.observableArrayList(
                        "All",
                        "Administrator",
                        "Manager",
                        "Employee"
                )
        );


        roleFilterComboBox.setValue(
                "All"
        );


        searchField
                .textProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> applyFilters()
                );


        roleFilterComboBox
                .valueProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> applyFilters()
                );
    }


    private void applyFilters() {

        String keyword =
                searchField.getText() == null
                        ? ""
                        : searchField
                                .getText()
                                .trim()
                                .toLowerCase();


        String roleFilter =
                roleFilterComboBox.getValue();


        filteredUsers.setPredicate(
                user -> {

                    if (user == null) {
                        return false;
                    }


                    boolean matchesSearch =
                            keyword.isBlank()
                                    || contains(
                                    user.getEmployeeId(),
                                    keyword
                            )
                                    || contains(
                                    user.getFullName(),
                                    keyword
                            )
                                    || contains(
                                    user.getUsername(),
                                    keyword
                            )
                                    || contains(
                                    user.getEmail(),
                                    keyword
                            )
                                    || contains(
                                    user.getDepartment(),
                                    keyword
                            )
                                    || contains(
                                    user.getAccountStatus(),
                                    keyword
                            );


                    boolean matchesRole =
                            roleFilter == null
                                    || "All".equals(
                                    roleFilter
                            )
                                    || (
                                    user.getRole() != null
                                            && roleFilter.equals(
                                            formatRole(
                                                    user.getRole()
                                            )
                                    )
                            );


                    return matchesSearch
                            && matchesRole;
                }
        );
    }


    // =========================================================
    // SELECT USER
    // =========================================================

    private void configureSelection() {

        userTable
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> {

                            if (newValue != null) {

                                selectedUser =
                                        newValue;


                                displaySelectedUser(
                                        newValue
                                );
                            }
                        }
                );
    }


    private void displaySelectedUser(
            User user
    ) {

        employeeIdField.setText(
                safe(
                        user.getEmployeeId()
                )
        );


        fullNameField.setText(
                safe(
                        user.getFullName()
                )
        );


        usernameField.setText(
                safe(
                        user.getUsername()
                )
        );


        emailField.setText(
                safe(
                        user.getEmail()
                )
        );


        contactNumberField.setText(
                safe(
                        user.getContactNumber()
                )
        );


        departmentField.setText(
                safe(
                        user.getDepartment()
                )
        );


        roleComboBox.setValue(
                user.getRole()
        );


        updateStatusButtonText(
                user.getAccountStatus()
        );


        saveButton.setDisable(
                false
        );

        statusButton.setDisable(
                false
        );

        deleteButton.setDisable(
                false
        );
    }


    private void clearSelectedUser() {

        selectedUser =
                null;


        employeeIdField.clear();

        fullNameField.clear();

        usernameField.clear();

        emailField.clear();

        contactNumberField.clear();

        departmentField.clear();

        roleComboBox.setValue(
                null
        );


        saveButton.setDisable(
                true
        );


        statusButton.setDisable(
                true
        );


        deleteButton.setDisable(
                true
        );
    }


    // =========================================================
    // LOAD USERS
    // =========================================================

    @FXML
    private void loadUsers() {

        Task<List<User>> task =
                new Task<>() {

                    @Override
                    protected List<User> call()
                            throws Exception {

                        return userService
                                .getAllUsers();
                    }
                };


        task.setOnSucceeded(
                event -> {

                    users.setAll(
                            task.getValue()
                    );


                    userTable.refresh();


                    UIHelper.hideMessage(
                            messageLabel
                    );
                }
        );


        task.setOnFailed(
                event -> {

                    Throwable exception =
                            task.getException();


                    UIHelper.showError(
                            messageLabel,
                            exception == null
                                    ? "Unable to load users."
                                    : exception.getMessage()
                    );
                }
        );


        Thread thread =
                new Thread(task);


        thread.setDaemon(true);

        thread.start();
    }


    // =========================================================
    // SAVE USER
    // =========================================================

    @FXML
    private void handleSave() {

        if (selectedUser == null) {

            UIHelper.showError(
                    messageLabel,
                    "Please select a user first."
            );

            return;
        }


        Role selectedRole =
                roleComboBox.getValue();


        if (selectedRole == null) {

            UIHelper.showError(
                    messageLabel,
                    "Please select a role."
            );

            return;
        }


        selectedUser.setFullName(
                fullNameField
                        .getText()
                        .trim()
        );


        selectedUser.setUsername(
                usernameField
                        .getText()
                        .trim()
        );


        selectedUser.setContactNumber(
                contactNumberField
                        .getText()
                        .trim()
        );


        selectedUser.setDepartment(
                departmentField
                        .getText()
                        .trim()
        );


        selectedUser.setRole(
                selectedRole
        );


        selectedUser.setRoleId(
                roleToId(
                        selectedRole
                )
        );


        saveButton.setDisable(
                true
        );


        Task<Void> task =
                new Task<>() {

                    @Override
                    protected Void call()
                            throws Exception {

                        userService.updateUser(
                                selectedUser
                        );


                        return null;
                    }
                };


        task.setOnSucceeded(
                event -> {

                    saveButton.setDisable(
                            false
                    );


                    userTable.refresh();


                    UIHelper.showSuccess(
                            messageLabel,
                            "User updated successfully."
                    );
                }
        );


        task.setOnFailed(
                event -> {

                    saveButton.setDisable(
                            false
                    );


                    Throwable exception =
                            task.getException();


                    UIHelper.showError(
                            messageLabel,
                            exception == null
                                    ? "Unable to update user."
                                    : exception.getMessage()
                    );
                }
        );


        Thread thread =
                new Thread(task);


        thread.setDaemon(true);

        thread.start();
    }


    // =========================================================
    // STATUS
    // =========================================================

    @FXML
    private void handleToggleStatus() {

        if (selectedUser == null) {

            UIHelper.showError(
                    messageLabel,
                    "Please select a user first."
            );

            return;
        }


        String currentStatus =
                selectedUser
                        .getAccountStatus();


        String newStatus =
                "Active".equalsIgnoreCase(
                        currentStatus
                )
                        ? "Inactive"
                        : "Active";


        statusButton.setDisable(
                true
        );


        Task<Void> task =
                new Task<>() {

                    @Override
                    protected Void call()
                            throws Exception {

                        userService
                                .updateAccountStatus(
                                        selectedUser
                                                .getUserId(),
                                        newStatus
                                );


                        return null;
                    }
                };


        task.setOnSucceeded(
                event -> {

                    statusButton.setDisable(
                            false
                    );


                    selectedUser
                            .setAccountStatus(
                                    newStatus
                            );


                    userTable.refresh();


                    updateStatusButtonText(
                            newStatus
                    );


                    UIHelper.showSuccess(
                            messageLabel,
                            "Account status updated successfully."
                    );
                }
        );


        task.setOnFailed(
                event -> {

                    statusButton.setDisable(
                            false
                    );


                    Throwable exception =
                            task.getException();


                    UIHelper.showError(
                            messageLabel,
                            exception == null
                                    ? "Unable to update account status."
                                    : exception.getMessage()
                    );
                }
        );


        Thread thread =
                new Thread(task);


        thread.setDaemon(true);

        thread.start();
    }


    // =========================================================
    // DELETE
    // =========================================================

    @FXML
    private void handleDeleteUser() {

        if (selectedUser == null) {

            UIHelper.showError(
                    messageLabel,
                    "Please select a user first."
            );

            return;
        }


        if (
                selectedUser.getRoleId() == 1
        ) {

            UIHelper.showError(
                    messageLabel,
                    "Administrator accounts cannot be deleted."
            );

            return;
        }


        Alert alert =
                new Alert(
                        Alert.AlertType.CONFIRMATION
                );


        alert.setTitle(
                "Delete User"
        );


        alert.setHeaderText(
                "Delete "
                        + safe(
                        selectedUser.getFullName()
                )
                        + "?"
        );


        alert.setContentText(
                "This action cannot be undone."
        );


        Optional<ButtonType> result =
                alert.showAndWait();


        if (
                result.isEmpty()
                        || result.get() != ButtonType.OK
        ) {

            return;
        }


        deleteButton.setDisable(
                true
        );


        Task<Void> task =
                new Task<>() {

                    @Override
                    protected Void call()
                            throws Exception {

                        userService.deleteUser(
                                selectedUser.getUserId()
                        );


                        return null;
                    }
                };


        task.setOnSucceeded(
                event -> {

                    User deletedUser =
                            selectedUser;


                    users.remove(
                            deletedUser
                    );


                    clearSelectedUser();


                    UIHelper.showSuccess(
                            messageLabel,
                            "User deleted successfully."
                    );
                }
        );


        task.setOnFailed(
                event -> {

                    deleteButton.setDisable(
                            false
                    );


                    Throwable exception =
                            task.getException();


                    UIHelper.showError(
                            messageLabel,
                            exception == null
                                    ? "Unable to delete user."
                                    : exception.getMessage()
                    );
                }
        );


        Thread thread =
                new Thread(task);


        thread.setDaemon(true);

        thread.start();
    }


    // =========================================================
    // CREATE USER
    // =========================================================

    @FXML
    private void handleCreateUser(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/admin/adminCreateUser.fxml"
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
    private void handleProfile(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/admin/adminProfile.fxml"
        );
    }


    @FXML
    private void handleUserManagement() {

        // Current page.
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

        System.out.println(
                "Reports not implemented yet."
        );
    }


    @FXML
    private void handleAnonymousReports() {

        System.out.println(
                "Anonymous Reports not implemented yet."
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


    // =========================================================
    // HELPERS
    // =========================================================

    private void updateStatusButtonText(
            String status
    ) {

        if (
                "Active".equalsIgnoreCase(
                        status
                )
        ) {

            statusButton.setText(
                    "Deactivate Account"
            );

        } else {

            statusButton.setText(
                    "Activate Account"
            );
        }
    }


    private boolean contains(
            String value,
            String keyword
    ) {

        return value != null
                && value
                .toLowerCase()
                .contains(
                        keyword
                );
    }


    private String safe(
            String value
    ) {

        return value == null
                ? ""
                : value;
    }


    private String formatRole(
            Role role
    ) {

        return switch (role) {

            case ADMINISTRATOR ->
                    "Administrator";

            case MANAGER ->
                    "Manager";

            case EMPLOYEE ->
                    "Employee";
        };
    }


    private long roleToId(
            Role role
    ) {

        return switch (role) {

            case ADMINISTRATOR ->
                    1L;

            case MANAGER ->
                    2L;

            case EMPLOYEE ->
                    3L;
        };
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