package com.aepm.ui.admin;

import com.aepm.logic.user.UserService;
import com.aepm.model.Role;
import com.aepm.model.User;
import com.aepm.ui.components.UIHelper;
import com.aepm.ui.navigation.NavigationManager;

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
    // SEARCH
    // =========================================================

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> filterRoleComboBox;


    // =========================================================
    // EDIT FORM
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


    // =========================================================
    // BUTTONS
    // =========================================================

    @FXML
    private Button saveButton;

    @FXML
    private Button statusButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Label messageLabel;


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
    public void initialize() {

        System.out.println(
                "AdminUserManagementController initialize()"
        );


        UIHelper.hideMessage(
                messageLabel
        );


        configureTable();

        configureRoleBoxes();

        configureFiltering();

        configureSelection();

        setEditDisabled(
                true
        );

        loadUsers();
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
                data -> {

                    User user =
                            data.getValue();

                    String role =
                            user.getRole() == null
                                    ? "-"
                                    : formatRole(
                                            user.getRole()
                                    );

                    return new SimpleStringProperty(
                            role
                    );
                }
        );


        // IMPORTANT:
        // Do this in Java, NOT in FXML.

        userTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );
    }


    // =========================================================
    // ROLE BOXES
    // =========================================================

    private void configureRoleBoxes() {

        roleComboBox.setItems(
                FXCollections.observableArrayList(
                        Role.ADMINISTRATOR,
                        Role.MANAGER,
                        Role.EMPLOYEE
                )
        );


        filterRoleComboBox.setItems(
                FXCollections.observableArrayList(
                        "All",
                        "Administrator",
                        "Manager",
                        "Employee"
                )
        );


        filterRoleComboBox.setValue(
                "All"
        );
    }


    // =========================================================
    // FILTER
    // =========================================================

    private void configureFiltering() {

        filteredUsers =
                new FilteredList<>(
                        users,
                        user -> true
                );


        userTable.setItems(
                filteredUsers
        );


        searchField
                .textProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> applyFilter()
                );


        filterRoleComboBox
                .valueProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> applyFilter()
                );
    }


    private void applyFilter() {

        if (filteredUsers == null) {
            return;
        }


        String keyword =
                searchField.getText() == null
                        ? ""
                        : searchField
                        .getText()
                        .trim()
                        .toLowerCase();


        String filterRole =
                filterRoleComboBox.getValue();


        filteredUsers.setPredicate(
                user -> {

                    boolean roleMatch =
                            true;


                    if (
                            filterRole != null
                                    && !"All".equals(
                                    filterRole
                            )
                    ) {

                        roleMatch =
                                user.getRole() != null
                                        && filterRole.equals(
                                        formatRole(
                                                user.getRole()
                                        )
                                );
                    }


                    if (!roleMatch) {
                        return false;
                    }


                    if (keyword.isEmpty()) {
                        return true;
                    }


                    return contains(
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
                }
        );
    }


    // =========================================================
    // LOAD USERS
    // =========================================================

    @FXML
    private void loadUsers() {

        System.out.println(
                "Loading users..."
        );


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

                    List<User> result =
                            task.getValue();


                    users.clear();


                    if (result != null) {

                        users.addAll(
                                result
                        );
                    }


                    System.out.println(
                            "Loaded users: "
                                    + users.size()
                    );


                    for (User user : users) {

                        System.out.println(
                                "USER: "
                                        + user.getEmployeeId()
                                        + " | "
                                        + user.getFullName()
                                        + " | "
                                        + user.getEmail()
                        );
                    }


                    applyFilter();


                    if (users.isEmpty()) {

                        UIHelper.showError(
                                messageLabel,
                                "No users were returned from Supabase."
                        );

                    } else {

                        UIHelper.hideMessage(
                                messageLabel
                        );
                    }
                }
        );


        task.setOnFailed(
                event -> {

                    Throwable error =
                            task.getException();


                    if (error != null) {
                        error.printStackTrace();
                    }


                    UIHelper.showError(
                            messageLabel,
                            error == null
                                    ? "Unable to load users."
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

                            selectedUser =
                                    newValue;


                            if (newValue == null) {

                                clearFields();

                                setEditDisabled(
                                        true
                                );

                                return;
                            }


                            displayUser(
                                    newValue
                            );


                            setEditDisabled(
                                    false
                            );
                        }
                );
    }


    private void displayUser(
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


        updateStatusButton(
                user.getAccountStatus()
        );
    }


    // =========================================================
    // SAVE
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


        Role role =
                roleComboBox.getValue();


        if (role == null) {

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
                role
        );


        selectedUser.setRoleId(
                roleToId(
                        role
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


                    UIHelper.showSuccess(
                            messageLabel,
                            "User updated successfully."
                    );


                    loadUsers();
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
                                    ? "Unable to update user."
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


        String newStatus =
                "Active".equalsIgnoreCase(
                        selectedUser
                                .getAccountStatus()
                )
                        ? "Inactive"
                        : "Active";


        Task<Void> task =
                new Task<>() {

                    @Override
                    protected Void call()
                            throws Exception {

                        userService.updateAccountStatus(
                                selectedUser
                                        .getUserId(),
                                newStatus
                        );

                        return null;
                    }
                };


        task.setOnSucceeded(
                event -> {

                    UIHelper.showSuccess(
                            messageLabel,
                            "Account status updated."
                    );

                    loadUsers();
                }
        );


        task.setOnFailed(
                event -> {

                    Throwable error =
                            task.getException();


                    UIHelper.showError(
                            messageLabel,
                            error == null
                                    ? "Unable to update account status."
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
    // DELETE
    // =========================================================

    @FXML
    private void handleDeleteUser() {

    // =====================================================
    // CHECK USER SELECTED
    // =====================================================

    if (selectedUser == null) {

        UIHelper.showError(
                messageLabel,
                "Please select a user first."
        );

        return;
    }


    // =====================================================
    // PREVENT DELETE ADMIN
    // =====================================================

    if (
            selectedUser.getRole()
                    == Role.ADMINISTRATOR
    ) {

        UIHelper.showError(
                messageLabel,
                "Administrator accounts cannot be deleted."
        );

        return;
    }


    // =====================================================
    // CONFIRM DELETE
    // =====================================================

    Alert alert =
            new Alert(
                    Alert.AlertType.CONFIRMATION
            );


    alert.setTitle(
            "Delete User"
    );


    alert.setHeaderText(
            "Delete "
                    + selectedUser
                    .getFullName()
                    + "?"
    );


    alert.setContentText(
            "This will permanently delete the user account. "
                    + "This action cannot be undone."
    );


    Optional<ButtonType> result =
            alert.showAndWait();


    if (
            result.isEmpty()
                    || result.get()
                    != ButtonType.OK
    ) {

        return;
    }


    String userId =
            selectedUser.getUserId();


    deleteButton.setDisable(
            true
    );


    // =====================================================
    // DELETE
    // =====================================================

    Task<Void> task =
            new Task<>() {

                @Override
                protected Void call()
                        throws Exception {

                    userService.deleteUser(
                            userId
                    );

                    return null;
                }
            };


    task.setOnSucceeded(
            event -> {

                deleteButton.setDisable(
                        false
                );


                UIHelper.showSuccess(
                        messageLabel,
                        "User deleted successfully."
                );


                selectedUser =
                        null;


                clearFields();


                setEditDisabled(
                        true
                );


                loadUsers();
            }
    );


    task.setOnFailed(
            event -> {

                deleteButton.setDisable(
                        false
                );


                Throwable error =
                        task.getException();


                UIHelper.showError(
                        messageLabel,
                        error == null
                                ? "Unable to delete user."
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
    // CREATE USER NAVIGATION
    // =========================================================

    @FXML
    private void handleCreateUser(
            ActionEvent event
    ) {

        System.out.println(
                "Opening adminCreateUser.fxml"
        );


        navigate(
                event,
                "/fxml/admin/adminCreateUser.fxml"
        );
    }


    // =========================================================
    // BACK
    // =========================================================

    @FXML
    private void handleBack(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/admin/adminHome.fxml"
        );
    }


    // =========================================================
    // NAVIGATION
    // =========================================================

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


    // =========================================================
    // HELPERS
    // =========================================================

    private void updateStatusButton(
            String status
    ) {

        statusButton.setText(
                "Active".equalsIgnoreCase(
                        status
                )
                        ? "Deactivate Account"
                        : "Activate Account"
        );
    }


    private void setEditDisabled(
            boolean disabled
    ) {

        employeeIdField.setDisable(true);
        emailField.setDisable(true);

        fullNameField.setDisable(disabled);
        usernameField.setDisable(disabled);
        contactNumberField.setDisable(disabled);
        departmentField.setDisable(disabled);
        roleComboBox.setDisable(disabled);

        saveButton.setDisable(disabled);
        statusButton.setDisable(disabled);
        deleteButton.setDisable(disabled);
    }


    private void clearFields() {

        employeeIdField.clear();
        fullNameField.clear();
        usernameField.clear();
        emailField.clear();
        contactNumberField.clear();
        departmentField.clear();

        roleComboBox.setValue(null);
    }


    private String safe(
            String value
    ) {

        return value == null
                ? ""
                : value;
    }


    private boolean contains(
            String value,
            String keyword
    ) {

        return value != null
                && value
                .toLowerCase()
                .contains(keyword);
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
}