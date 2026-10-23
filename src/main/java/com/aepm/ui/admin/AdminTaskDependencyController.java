package com.aepm.ui.admin;

import com.aepm.logic.task.TaskDependencyService;
import com.aepm.logic.task.TaskService;

import com.aepm.model.Task;
import com.aepm.model.TaskDependency;
import com.aepm.model.User;

import com.aepm.ui.components.UIHelper;
import com.aepm.ui.navigation.NavigationManager;

import com.aepm.util.SessionManager;

import javafx.beans.property.SimpleStringProperty;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

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

import javafx.stage.Stage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class AdminTaskDependencyController {

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
    private TableView<TaskDependency> dependencyTable;

    @FXML
    private TableColumn<TaskDependency, String> prerequisiteColumn;

    @FXML
    private TableColumn<TaskDependency, String> nextTaskColumn;

    @FXML
    private TableColumn<TaskDependency, String> statusColumn;


    // =========================================================
    // FORM
    // =========================================================

    @FXML
    private ComboBox<Task> prerequisiteComboBox;

    @FXML
    private ComboBox<Task> nextTaskComboBox;


    // =========================================================
    // BUTTONS
    // =========================================================

    @FXML
    private Button createButton;

    @FXML
    private Button statusButton;

    @FXML
    private Button deleteButton;


    // =========================================================
    // MESSAGE
    // =========================================================

    @FXML
    private Label messageLabel;
    
    @FXML
        private void handleAIConfiguration(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource())
                .getScene().getWindow();        
        NavigationManager.navigate(
                stage,
                "/fxml/admin/adminAIConfiguration.fxml"
        );
    }

    @FXML
    private void handleAnnouncements(ActionEvent event) {
        AdminAnnouncementsDialog.show(
            ((Node) event.getSource()).getScene().getWindow()
        );
    }

    // =========================================================
    // SERVICES
    // =========================================================

    private final TaskDependencyService dependencyService =
            new TaskDependencyService();

    private final TaskService taskService =
            new TaskService();


    // =========================================================
    // DATA
    // =========================================================

    private final ObservableList<TaskDependency> dependencies =
            FXCollections.observableArrayList();

    private final ObservableList<Task> tasks =
            FXCollections.observableArrayList();

    private FilteredList<TaskDependency> filteredDependencies;

    private TaskDependency selectedDependency;


    // =========================================================
    // LOOKUP
    // =========================================================

    private final Map<UUID, String> taskTitleMap =
            new HashMap<>();


    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {

        UIHelper.hideMessage(
                messageLabel
        );

        loadCurrentUser();

        configureTable();

        configureSearch();

        configureSelection();

        configureTaskComboBoxes();

        clearSelection();

        loadTasksAndDependencies();
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

    // =========================================================
    // CURRENT USER
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
            name = user.getUsername();
        }

        if (
                name == null
                        || name.isBlank()
        ) {
            name = "Administrator";
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

        prerequisiteColumn.setCellValueFactory(
                cellData -> {

                    TaskDependency dependency =
                            cellData.getValue();

                    String title =
                            taskTitleMap.getOrDefault(
                                    dependency.getPrerequisiteTaskId(),
                                    dependency
                                            .getPrerequisiteTaskId()
                                            .toString()
                            );

                    return new SimpleStringProperty(
                            title
                    );
                }
        );


        nextTaskColumn.setCellValueFactory(
                cellData -> {

                    TaskDependency dependency =
                            cellData.getValue();

                    String title =
                            taskTitleMap.getOrDefault(
                                    dependency.getNextTaskId(),
                                    dependency
                                            .getNextTaskId()
                                            .toString()
                            );

                    return new SimpleStringProperty(
                            title
                    );
                }
        );


        statusColumn.setCellValueFactory(
                cellData ->
                        new SimpleStringProperty(
                                cellData
                                        .getValue()
                                        .getRuleStatus()
                        )
        );


        dependencyTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );
    }


    // =========================================================
    // SEARCH
    // =========================================================

    private void configureSearch() {

        filteredDependencies =
                new FilteredList<>(
                        dependencies,
                        dependency -> true
                );

        dependencyTable.setItems(
                filteredDependencies
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


        filteredDependencies.setPredicate(
                dependency -> {

                    if (dependency == null) {
                        return false;
                    }

                    if (keyword.isBlank()) {
                        return true;
                    }


                    String prerequisiteTitle =
                            taskTitleMap.getOrDefault(
                                    dependency.getPrerequisiteTaskId(),
                                    ""
                            );


                    String nextTitle =
                            taskTitleMap.getOrDefault(
                                    dependency.getNextTaskId(),
                                    ""
                            );


                    String status =
                            dependency.getRuleStatus() == null
                                    ? ""
                                    : dependency.getRuleStatus();


                    return prerequisiteTitle
                            .toLowerCase()
                            .contains(keyword)

                            || nextTitle
                            .toLowerCase()
                            .contains(keyword)

                            || status
                            .toLowerCase()
                            .contains(keyword);
                }
        );
    }


    // =========================================================
    // TASK COMBO BOXES
    // =========================================================

    private void configureTaskComboBoxes() {

        prerequisiteComboBox.setItems(
                tasks
        );

        nextTaskComboBox.setItems(
                tasks
        );


        prerequisiteComboBox.setConverter(
                new javafx.util.StringConverter<>() {

                    @Override
                    public String toString(Task task) {

                        if (task == null) {
                            return "";
                        }

                        return task.getTaskTitle();
                    }

                    @Override
                    public Task fromString(String string) {
                        return null;
                    }
                }
        );


        nextTaskComboBox.setConverter(
                new javafx.util.StringConverter<>() {

                    @Override
                    public String toString(Task task) {

                        if (task == null) {
                            return "";
                        }

                        return task.getTaskTitle();
                    }

                    @Override
                    public Task fromString(String string) {
                        return null;
                    }
                }
        );
    }


    // =========================================================
    // SELECTION
    // =========================================================

    private void configureSelection() {

        dependencyTable
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> {

                            if (newValue != null) {

                                selectedDependency =
                                        newValue;

                                displaySelectedDependency(
                                        newValue
                                );
                            }
                        }
                );
    }


    private void displaySelectedDependency(
            TaskDependency dependency
    ) {

        prerequisiteComboBox.setValue(
                findTaskById(
                        dependency.getPrerequisiteTaskId()
                )
        );


        nextTaskComboBox.setValue(
                findTaskById(
                        dependency.getNextTaskId()
                )
        );


        prerequisiteComboBox.setDisable(
                true
        );

        nextTaskComboBox.setDisable(
                true
        );


        createButton.setDisable(
                true
        );

        statusButton.setDisable(
                false
        );

        deleteButton.setDisable(
                false
        );


        updateStatusButtonText(
                dependency.getRuleStatus()
        );
    }


    // =========================================================
    // LOAD DATA
    // =========================================================

    private void loadTasksAndDependencies() {

        UIHelper.hideMessage(
                messageLabel
        );


        javafx.concurrent.Task<List<Task>> loadTasksTask =
                new javafx.concurrent.Task<>() {

                    @Override
                    protected List<Task> call()
                            throws Exception {

                        return taskService
                                .getAllTasks();
                    }
                };


        loadTasksTask.setOnSucceeded(
                event -> {

                    List<Task> taskList =
                            loadTasksTask.getValue();


                    // =========================
                    // UPDATE UI DATA
                    // JavaFX Thread
                    // =========================

                    tasks.clear();

                    taskTitleMap.clear();


                    if (taskList != null) {

                        for (Task task : taskList) {

                            tasks.add(
                                    task
                            );


                            taskTitleMap.put(
                                    task.getTaskId(),
                                    task.getTaskTitle()
                            );
                        }
                    }


                    prerequisiteComboBox.setItems(
                            tasks
                    );

                    nextTaskComboBox.setItems(
                            tasks
                    );


                    // After tasks loaded,
                    // load dependencies
                    loadDependencies();
                }
        );


        loadTasksTask.setOnFailed(
                event -> {

                    Throwable error =
                            loadTasksTask.getException();


                    if (error != null) {
                        error.printStackTrace();
                    }


                    UIHelper.showError(
                            messageLabel,
                            error == null
                                    ? "Unable to load tasks."
                                    : error.getMessage()
                    );
                }
        );


        Thread thread =
                new Thread(
                        loadTasksTask
                );


        thread.setDaemon(true);

        thread.start();
    }

    private void loadDependencies() {

        javafx.concurrent.Task<List<TaskDependency>> loadDependencyTask =
                new javafx.concurrent.Task<>() {

                    @Override
                    protected List<TaskDependency> call()
                            throws Exception {

                        return dependencyService
                                .getAllDependencies();
                    }
                };


        loadDependencyTask.setOnSucceeded(
                event -> {

                    List<TaskDependency> dependencyList =
                            loadDependencyTask.getValue();


                    dependencies.clear();


                    if (dependencyList != null) {

                        dependencies.addAll(
                                dependencyList
                        );
                    }


                    applySearch();

                    dependencyTable.refresh();

                    clearSelection();
                }
        );


        loadDependencyTask.setOnFailed(
                event -> {

                    Throwable error =
                            loadDependencyTask.getException();


                    if (error != null) {
                        error.printStackTrace();
                    }


                    UIHelper.showError(
                            messageLabel,
                            error == null
                                    ? "Unable to load progressive task rules."
                                    : error.getMessage()
                    );
                }
        );


        Thread thread =
                new Thread(
                        loadDependencyTask
                );


        thread.setDaemon(true);

        thread.start();
    }
    // =========================================================
    // NEW RULE
    // =========================================================

    @FXML
    private void handleNewRule() {

        clearSelection();

        prerequisiteComboBox
                .requestFocus();
    }


    // =========================================================
    // CREATE RULE
    // =========================================================

    @FXML
    private void handleCreateRule() {

        Task prerequisite =
                prerequisiteComboBox
                        .getValue();


        Task nextTask =
                nextTaskComboBox
                        .getValue();


        if (
                prerequisite == null
                        || nextTask == null
        ) {

            UIHelper.showError(
                    messageLabel,
                    "Please select both prerequisite task and next task."
            );

            return;
        }


        if (
                prerequisite
                        .getTaskId()
                        .equals(
                                nextTask.getTaskId()
                        )
        ) {

            UIHelper.showError(
                    messageLabel,
                    "A task cannot depend on itself."
            );

            return;
        }


        createButton.setDisable(
                true
        );


        javafx.concurrent.Task<Void> createTask =
                new javafx.concurrent.Task<>() {

                    @Override
                    protected Void call()
                            throws Exception {

                        dependencyService
                                .createDependency(
                                        prerequisite
                                                .getTaskId(),
                                        nextTask
                                                .getTaskId()
                                );

                        return null;
                    }
                };


        createTask.setOnSucceeded(
                event -> {

                    UIHelper.showSuccess(
                            messageLabel,
                            "Progressive task rule created successfully."
                    );

                    loadTasksAndDependencies();
                }
        );


        createTask.setOnFailed(
                event -> {

                    createButton.setDisable(
                            false
                    );


                    Throwable error =
                            createTask.getException();


                    UIHelper.showError(
                            messageLabel,
                            error == null
                                    ? "Unable to create progressive task rule."
                                    : error.getMessage()
                    );
                }
        );


        Thread thread =
                new Thread(createTask);

        thread.setDaemon(true);

        thread.start();
    }


    // =========================================================
    // ACTIVATE / DEACTIVATE
    // =========================================================

    @FXML
    private void handleToggleStatus() {

        if (selectedDependency == null) {

            UIHelper.showError(
                    messageLabel,
                    "Please select a rule first."
            );

            return;
        }


        String newStatus =
                "Active".equalsIgnoreCase(
                        selectedDependency
                                .getRuleStatus()
                )
                        ? "Inactive"
                        : "Active";


        statusButton.setDisable(
                true
        );


        javafx.concurrent.Task<Void> task =
                new javafx.concurrent.Task<>() {

                    @Override
                    protected Void call()
                            throws Exception {

                        dependencyService
                                .updateRuleStatus(
                                        selectedDependency
                                                .getDependencyId(),
                                        newStatus
                                );

                        return null;
                    }
                };


        task.setOnSucceeded(
                event -> {

                    selectedDependency
                            .setRuleStatus(
                                    newStatus
                            );

                    statusButton.setDisable(
                            false
                    );

                    updateStatusButtonText(
                            newStatus
                    );

                    dependencyTable.refresh();


                    UIHelper.showSuccess(
                            messageLabel,
                            "Rule status updated successfully."
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
                                    ? "Unable to update rule status."
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
    private void handleDeleteRule() {

        if (selectedDependency == null) {

            UIHelper.showError(
                    messageLabel,
                    "Please select a rule first."
            );

            return;
        }


        Alert alert =
                new Alert(
                        Alert.AlertType.CONFIRMATION
                );


        alert.setTitle(
                "Delete Progressive Rule"
        );


        alert.setHeaderText(
                "Delete selected progressive task rule?"
        );


        alert.setContentText(
                "This dependency rule will be permanently removed."
        );


        Optional<ButtonType> result =
                alert.showAndWait();


        if (
                result.isEmpty()
                        || result.get() != ButtonType.OK
        ) {
            return;
        }


        Long dependencyId =
                selectedDependency
                        .getDependencyId();


        deleteButton.setDisable(
                true
        );


        javafx.concurrent.Task<Void> deleteTask =
                new javafx.concurrent.Task<>() {

                    @Override
                    protected Void call()
                            throws Exception {

                        dependencyService
                                .deleteDependency(
                                        dependencyId
                                );

                        return null;
                    }
                };


        deleteTask.setOnSucceeded(
                event -> {

                    UIHelper.showSuccess(
                            messageLabel,
                            "Progressive task rule deleted successfully."
                    );

                    loadTasksAndDependencies();
                }
        );


        deleteTask.setOnFailed(
                event -> {

                    deleteButton.setDisable(
                            false
                    );


                    Throwable error =
                            deleteTask.getException();


                    UIHelper.showError(
                            messageLabel,
                            error == null
                                    ? "Unable to delete progressive task rule."
                                    : error.getMessage()
                    );
                }
        );


        Thread thread =
                new Thread(deleteTask);

        thread.setDaemon(true);

        thread.start();
    }


    // =========================================================
    // CLEAR
    // =========================================================

    private void clearSelection() {

        selectedDependency =
                null;


        dependencyTable
                .getSelectionModel()
                .clearSelection();


        prerequisiteComboBox.setValue(
                null
        );

        nextTaskComboBox.setValue(
                null
        );


        prerequisiteComboBox.setDisable(
                false
        );

        nextTaskComboBox.setDisable(
                false
        );


        createButton.setDisable(
                false
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
    // FIND TASK
    // =========================================================

    private Task findTaskById(
            UUID taskId
    ) {

        if (taskId == null) {
            return null;
        }


        for (Task task : tasks) {

            if (
                    task.getTaskId()
                            .equals(taskId)
            ) {
                return task;
            }
        }


        return null;
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
    // REFRESH
    // =========================================================

    @FXML
    private void handleRefresh() {

        loadTasksAndDependencies();
    }


    // =========================================================
    // NAVIGATION
    // =========================================================

    @FXML
    private void handleBack(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/admin/adminTask.fxml"
        );
    }


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