package com.aepm.ui.admin;

import com.aepm.logic.task.TaskCategoryService;
import com.aepm.model.TaskCategory;
import com.aepm.model.User;
import com.aepm.ui.components.UIHelper;
import com.aepm.ui.navigation.NavigationManager;
import com.aepm.util.SessionManager;

import javafx.beans.property.SimpleLongProperty;
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
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import javafx.stage.Stage;

import java.util.List;
import java.util.Optional;

public class AdminTaskCategoryController {

    // =========================================================
    // TOP BAR
    // =========================================================

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label topNameLabel;


    // =========================================================
    // SEARCH
    // =========================================================

    @FXML
    private TextField searchField;


    // =========================================================
    // TABLE
    // =========================================================

    @FXML
    private TableView<TaskCategory> categoryTable;

    @FXML
    private TableColumn<TaskCategory, Number> idColumn;

    @FXML
    private TableColumn<TaskCategory, String> nameColumn;

    @FXML
    private TableColumn<TaskCategory, String> descriptionColumn;

    @FXML
    private TableColumn<TaskCategory, String> statusColumn;


    // =========================================================
    // FORM
    // =========================================================

    @FXML
    private TextField categoryNameField;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private ComboBox<String> statusComboBox;


    // =========================================================
    // BUTTONS
    // =========================================================

    @FXML
    private Button createButton;

    @FXML
    private Button saveButton;

    @FXML
    private Button statusButton;

    @FXML
    private Button deleteButton;


    // =========================================================
    // MESSAGE
    // =========================================================

    @FXML
    private Label messageLabel;


    // =========================================================
    // DATA
    // =========================================================

    private final TaskCategoryService categoryService =
            new TaskCategoryService();


    private final ObservableList<TaskCategory> categories =
            FXCollections.observableArrayList();


    private FilteredList<TaskCategory> filteredCategories;


    private TaskCategory selectedCategory;


    // =========================================================
    // INITIALIZE
    // =========================================================
   @FXML
   private void handleSystemConfiguration(ActionEvent event) {
        navigate(event, "/fxml/admin/systemConfiguration.fxml");
   }

   
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
    public void initialize() {

        UIHelper.hideMessage(
                messageLabel
        );


        loadCurrentUser();

        configureTable();

        configureStatusComboBox();

        configureSearch();

        configureSelection();

        clearForm();

        loadCategories();
    }


    // =========================================================
    // CURRENT USER
    // =========================================================

    private void loadCurrentUser() {

        User user =
                SessionManager.getCurrentUser();


        if (user == null) {
            return;
        }


        String name =
                user.getFullName();


        if (
                name == null
                        || name.isBlank()
        ) {

            name =
                    user.getUsername();
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

        idColumn.setCellValueFactory(
                cellData ->
                        new SimpleLongProperty(
                                cellData
                                        .getValue()
                                        .getCategoryId()
                        )
        );


        nameColumn.setCellValueFactory(
                cellData ->
                        new SimpleStringProperty(
                                safe(
                                        cellData
                                                .getValue()
                                                .getCategoryName()
                                )
                        )
        );


        descriptionColumn.setCellValueFactory(
                cellData ->
                        new SimpleStringProperty(
                                emptyToDash(
                                        cellData
                                                .getValue()
                                                .getDescription()
                                )
                        )
        );


        statusColumn.setCellValueFactory(
                cellData ->
                        new SimpleStringProperty(
                                safe(
                                        cellData
                                                .getValue()
                                                .getCategoryStatus()
                                )
                        )
        );


        categoryTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );
    }


    // =========================================================
    // STATUS COMBO
    // =========================================================

    private void configureStatusComboBox() {

        statusComboBox.setItems(
                FXCollections.observableArrayList(
                        "Active",
                        "Inactive"
                )
        );


        statusComboBox.setValue(
                "Active"
        );
    }


    // =========================================================
    // SEARCH
    // =========================================================

    private void configureSearch() {

        filteredCategories =
                new FilteredList<>(
                        categories,
                        category -> true
                );


        categoryTable.setItems(
                filteredCategories
        );


        searchField
                .textProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> applySearch()
                );
    }


    private void applySearch() {

        String keyword =
                searchField.getText() == null
                        ? ""
                        : searchField
                                .getText()
                                .trim()
                                .toLowerCase();


        filteredCategories.setPredicate(
                category -> {

                    if (category == null) {
                        return false;
                    }


                    if (keyword.isBlank()) {
                        return true;
                    }


                    return contains(
                            category.getCategoryName(),
                            keyword
                    )
                            || contains(
                            category.getDescription(),
                            keyword
                    )
                            || contains(
                            category.getCategoryStatus(),
                            keyword
                    );
                }
        );
    }


    // =========================================================
    // TABLE SELECTION
    // =========================================================

    private void configureSelection() {

        categoryTable
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> {

                            if (newValue != null) {

                                selectedCategory =
                                        newValue;


                                displayCategory(
                                        newValue
                                );
                            }
                        }
                );
    }


    private void displayCategory(
            TaskCategory category
    ) {

        categoryNameField.setText(
                safe(
                        category.getCategoryName()
                )
        );


        descriptionArea.setText(
                category.getDescription() == null
                        ? ""
                        : category.getDescription()
        );


        statusComboBox.setValue(
                category.getCategoryStatus()
        );


        createButton.setDisable(
                true
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


        updateStatusButtonText(
                category.getCategoryStatus()
        );
    }


    // =========================================================
    // LOAD CATEGORIES
    // =========================================================

    @FXML
    private void loadCategories() {

        UIHelper.hideMessage(
                messageLabel
        );


        Task<List<TaskCategory>> task =
                new Task<>() {

                    @Override
                    protected List<TaskCategory> call()
                            throws Exception {

                        return categoryService
                                .getAllCategories();
                    }
                };


        task.setOnSucceeded(
                event -> {

                    categories.setAll(
                            task.getValue()
                    );


                    categoryTable.refresh();


                    clearForm();
                }
        );


        task.setOnFailed(
                event -> {

                    Throwable error =
                            task.getException();


                    UIHelper.showError(
                            messageLabel,
                            error == null
                                    ? "Unable to load task categories."
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
    // NEW CATEGORY
    // =========================================================

    @FXML
    private void handleNewCategory() {

        categoryTable
                .getSelectionModel()
                .clearSelection();


        clearForm();


        categoryNameField
                .requestFocus();
    }


    // =========================================================
    // CREATE CATEGORY
    // =========================================================

    @FXML
    private void handleCreateCategory() {

        String name =
                categoryNameField
                        .getText()
                        .trim();


        String description =
                descriptionArea
                        .getText()
                        .trim();


        if (name.isBlank()) {

            UIHelper.showError(
                    messageLabel,
                    "Category name is required."
            );

            return;
        }


        createButton.setDisable(
                true
        );


        Task<Void> task =
                new Task<>() {

                    @Override
                    protected Void call()
                            throws Exception {

                        categoryService
                                .createCategory(
                                        name,
                                        description
                                );


                        return null;
                    }
                };


        task.setOnSucceeded(
                event -> {

                    UIHelper.showSuccess(
                            messageLabel,
                            "Task category created successfully."
                    );


                    loadCategories();
                }
        );


        task.setOnFailed(
                event -> {

                    createButton.setDisable(
                            false
                    );


                    Throwable error =
                            task.getException();


                    UIHelper.showError(
                            messageLabel,
                            error == null
                                    ? "Unable to create category."
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
    // UPDATE CATEGORY
    // =========================================================

    @FXML
    private void handleSaveChanges() {

        if (selectedCategory == null) {

            UIHelper.showError(
                    messageLabel,
                    "Please select a category first."
            );

            return;
        }


        String name =
                categoryNameField
                        .getText()
                        .trim();


        if (name.isBlank()) {

            UIHelper.showError(
                    messageLabel,
                    "Category name is required."
            );

            return;
        }


        selectedCategory.setCategoryName(
                name
        );


        selectedCategory.setDescription(
                descriptionArea
                        .getText()
                        .trim()
        );


        selectedCategory.setCategoryStatus(
                statusComboBox
                        .getValue()
        );


        saveButton.setDisable(
                true
        );


        Task<Void> task =
                new Task<>() {

                    @Override
                    protected Void call()
                            throws Exception {

                        categoryService
                                .updateCategory(
                                        selectedCategory
                                );


                        return null;
                    }
                };


        task.setOnSucceeded(
                event -> {

                    saveButton.setDisable(
                            false
                    );


                    categoryTable.refresh();


                    UIHelper.showSuccess(
                            messageLabel,
                            "Task category updated successfully."
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
                                    ? "Unable to update category."
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
    // ACTIVATE / DEACTIVATE
    // =========================================================

    @FXML
    private void handleToggleStatus() {

        if (selectedCategory == null) {

            UIHelper.showError(
                    messageLabel,
                    "Please select a category first."
            );

            return;
        }


        String currentStatus =
                selectedCategory
                        .getCategoryStatus();


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

                        categoryService
                                .updateCategoryStatus(
                                        selectedCategory
                                                .getCategoryId(),
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


                    selectedCategory
                            .setCategoryStatus(
                                    newStatus
                            );


                    statusComboBox.setValue(
                            newStatus
                    );


                    updateStatusButtonText(
                            newStatus
                    );


                    categoryTable.refresh();


                    UIHelper.showSuccess(
                            messageLabel,
                            "Category status updated successfully."
                    );
                }
        );


        task.setOnFailed(
                event -> {

                    statusButton.setDisable(
                            false
                    );


                    Throwable error =
                            task.getException();


                    UIHelper.showError(
                            messageLabel,
                            error == null
                                    ? "Unable to update category status."
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
    // DELETE CATEGORY
    // =========================================================

    @FXML
    private void handleDeleteCategory() {

        if (selectedCategory == null) {

            UIHelper.showError(
                    messageLabel,
                    "Please select a category first."
            );

            return;
        }


        Alert alert =
                new Alert(
                        Alert.AlertType.CONFIRMATION
                );


        alert.setTitle(
                "Delete Task Category"
        );


        alert.setHeaderText(
                "Delete "
                        + selectedCategory
                        .getCategoryName()
                        + "?"
        );


        alert.setContentText(
                "A category already used by a task cannot be deleted."
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


        Long categoryId =
                selectedCategory
                        .getCategoryId();


        Task<Void> task =
                new Task<>() {

                    @Override
                    protected Void call()
                            throws Exception {

                        categoryService
                                .deleteCategory(
                                        categoryId
                                );


                        return null;
                    }
                };


        task.setOnSucceeded(
                event -> {

                    UIHelper.showSuccess(
                            messageLabel,
                            "Task category deleted successfully."
                    );


                    loadCategories();
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
                                    ? "Unable to delete category."
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
    // CLEAR
    // =========================================================

    private void clearForm() {

        selectedCategory =
                null;


        categoryTable
                .getSelectionModel()
                .clearSelection();


        categoryNameField.clear();

        descriptionArea.clear();

        statusComboBox.setValue(
                "Active"
        );


        createButton.setDisable(
                false
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


        statusButton.setText(
                "Deactivate"
        );
    }


    // =========================================================
    // STATUS BUTTON
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
                    "Deactivate"
            );

        } else {

            statusButton.setText(
                    "Activate"
            );
        }
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


    @FXML
    private void handleBack(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/admin/adminTask.fxml"
        );
    }


    // =========================================================
    // HELPERS
    // =========================================================

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


    private String emptyToDash(
            String value
    ) {

        return value == null
                || value.isBlank()
                ? "-"
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