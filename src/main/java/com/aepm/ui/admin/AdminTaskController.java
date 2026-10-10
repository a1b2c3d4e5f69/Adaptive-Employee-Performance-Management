package com.aepm.ui.admin;

import com.aepm.logic.task.TaskCategoryService;
import com.aepm.logic.task.TaskProgressService;
import com.aepm.logic.task.TaskService;
import com.aepm.logic.user.UserService;

import com.aepm.model.Task;
import com.aepm.model.TaskCategory;
import com.aepm.model.TaskProgress;
import com.aepm.model.User;

import com.aepm.ui.navigation.NavigationManager;
import com.aepm.util.SessionManager;

import javafx.beans.property.SimpleStringProperty;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import javafx.scene.Node;

import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import javafx.scene.control.cell.PropertyValueFactory;

import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminTaskController {

    // =========================================================
    // TOP BAR
    // =========================================================

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label topNameLabel;


    // =========================================================
    // FILTERS
    // =========================================================

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> employeeFilterComboBox;

    @FXML
    private ComboBox<String> managerFilterComboBox;

    @FXML
    private ComboBox<String> categoryFilterComboBox;

    @FXML
    private ComboBox<String> statusFilterComboBox;

    @FXML
    private DatePicker dueDateFilterPicker;


    // =========================================================
    // TASK TABLE
    // =========================================================

    @FXML
    private TableView<Task> taskTable;

    @FXML
    private TableColumn<Task, String> titleColumn;

    @FXML
    private TableColumn<Task, String> categoryColumn;

    @FXML
    private TableColumn<Task, String> employeeColumn;

    @FXML
    private TableColumn<Task, String> managerColumn;

    @FXML
    private TableColumn<Task, String> priorityColumn;

    @FXML
    private TableColumn<Task, String> statusColumn;

    @FXML
    private TableColumn<Task, Double> progressColumn;

    @FXML
    private TableColumn<Task, OffsetDateTime> dueDateColumn;


    // =========================================================
    // SELECTED TASK DETAILS
    // =========================================================

    @FXML
    private Label selectedTaskLabel;

    @FXML
    private Label selectedCategoryLabel;

    @FXML
    private Label selectedEmployeeLabel;

    @FXML
    private Label selectedManagerLabel;

    @FXML
    private Label selectedPriorityLabel;

    @FXML
    private Label selectedDifficultyLabel;

    @FXML
    private Label selectedStatusLabel;

    @FXML
    private Label selectedProgressLabel;

    @FXML
    private Label selectedAssignedDateLabel;

    @FXML
    private Label selectedDueDateLabel;

    @FXML
    private Label selectedDescriptionLabel;

    @FXML
    private Label selectedCriteriaLabel;


    // =========================================================
    // HISTORY TABLE
    // =========================================================

    @FXML
    private TableView<TaskProgress> historyTable;

    @FXML
    private TableColumn<TaskProgress, Double> historyProgressColumn;

    @FXML
    private TableColumn<TaskProgress, String> historyNoteColumn;

    @FXML
    private TableColumn<TaskProgress, OffsetDateTime> historyDateColumn;


    // =========================================================
    // MESSAGES
    // =========================================================

    @FXML
    private Label messageLabel;

    @FXML
    private Label historyMessageLabel;


    // =========================================================
    // SERVICES
    // =========================================================

    private final TaskService taskService =
            new TaskService();

    private final TaskProgressService taskProgressService =
            new TaskProgressService();

    private final TaskCategoryService categoryService =
            new TaskCategoryService();

    private final UserService userService =
            new UserService();


    // =========================================================
    // LOOKUP MAPS
    // =========================================================

    private final Map<String, String> employeeDisplayMap =
            new HashMap<>();

    private final Map<String, String> managerDisplayMap =
            new HashMap<>();

    private final Map<Long, String> categoryDisplayMap =
            new HashMap<>();


    // =========================================================
    // FILTERED DATA
    // =========================================================

    private final ObservableList<Task> allTasks =
            FXCollections.observableArrayList();

    private FilteredList<Task> filteredTasks;


    private Task selectedTask;


    private static final ZoneId MALAYSIA_ZONE =
            ZoneId.of("Asia/Kuala_Lumpur");


    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern(
                    "dd MMM yyyy HH:mm"
            );


    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {

        loadCurrentUser();

        setupTaskTable();

        setupHistoryTable();

        setupFilters();

        configureTaskSelection();

        clearSelectedTask();

        loadReferenceDataAndTasks();
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
                user.getFullName() == null
                        || user.getFullName().isBlank()
                        ? "Administrator"
                        : user.getFullName();


        welcomeLabel.setText(
                "Welcome, " + name
        );


        topNameLabel.setText(
                name
        );
    }


    // =========================================================
    // TASK TABLE SETUP
    // =========================================================

    private void setupTaskTable() {

        titleColumn.setCellValueFactory(
                new PropertyValueFactory<>(
                        "taskTitle"
                )
        );


        categoryColumn.setCellValueFactory(
                cellData -> {

                    Task task =
                            cellData.getValue();


                    if (
                            task == null
                                    || task.getCategoryId() == null
                    ) {

                        return new SimpleStringProperty(
                                "-"
                        );
                    }


                    return new SimpleStringProperty(
                            categoryDisplayMap.getOrDefault(
                                    task.getCategoryId(),
                                    "-"
                            )
                    );
                }
        );


        employeeColumn.setCellValueFactory(
                cellData -> {

                    Task task =
                            cellData.getValue();


                    if (
                            task == null
                                    || task.getAssignedEmployeeId() == null
                    ) {

                        return new SimpleStringProperty(
                                "-"
                        );
                    }


                    String id =
                            task.getAssignedEmployeeId()
                                    .toString();


                    return new SimpleStringProperty(
                            employeeDisplayMap.getOrDefault(
                                    id,
                                    id
                            )
                    );
                }
        );


        managerColumn.setCellValueFactory(
                cellData -> {

                    Task task =
                            cellData.getValue();


                    if (
                            task == null
                                    || task.getAssignedBy() == null
                    ) {

                        return new SimpleStringProperty(
                                "-"
                        );
                    }


                    String id =
                            task.getAssignedBy()
                                    .toString();


                    return new SimpleStringProperty(
                            managerDisplayMap.getOrDefault(
                                    id,
                                    id
                            )
                    );
                }
        );


        priorityColumn.setCellValueFactory(
                new PropertyValueFactory<>(
                        "priority"
                )
        );


        statusColumn.setCellValueFactory(
                new PropertyValueFactory<>(
                        "taskStatus"
                )
        );


        progressColumn.setCellValueFactory(
                new PropertyValueFactory<>(
                        "progress"
                )
        );


        dueDateColumn.setCellValueFactory(
                new PropertyValueFactory<>(
                        "dueDate"
                )
        );


        progressColumn.setCellFactory(
                column ->
                        new TableCell<>() {

                            @Override
                            protected void updateItem(
                                    Double value,
                                    boolean empty
                            ) {

                                super.updateItem(
                                        value,
                                        empty
                                );


                                setText(
                                        empty || value == null
                                                ? "-"
                                                : String.format(
                                                        "%.0f%%",
                                                        value
                                                )
                                );
                            }
                        }
        );


        dueDateColumn.setCellFactory(
                column ->
                        new TableCell<>() {

                            @Override
                            protected void updateItem(
                                    OffsetDateTime value,
                                    boolean empty
                            ) {

                                super.updateItem(
                                        value,
                                        empty
                                );


                                setText(
                                        empty || value == null
                                                ? "-"
                                                : formatDateTime(
                                                        value
                                                )
                                );
                            }
                        }
        );


        taskTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );
    }


    // =========================================================
    // HISTORY TABLE SETUP
    // =========================================================

    private void setupHistoryTable() {

        historyProgressColumn.setCellValueFactory(
                new PropertyValueFactory<>(
                        "progressPercentage"
                )
        );


        historyNoteColumn.setCellValueFactory(
                new PropertyValueFactory<>(
                        "progressNote"
                )
        );


        historyDateColumn.setCellValueFactory(
                new PropertyValueFactory<>(
                        "updatedAt"
                )
        );


        historyProgressColumn.setCellFactory(
                column ->
                        new TableCell<>() {

                            @Override
                            protected void updateItem(
                                    Double value,
                                    boolean empty
                            ) {

                                super.updateItem(
                                        value,
                                        empty
                                );


                                setText(
                                        empty || value == null
                                                ? "-"
                                                : String.format(
                                                        "%.0f%%",
                                                        value
                                                )
                                );
                            }
                        }
        );


        historyDateColumn.setCellFactory(
                column ->
                        new TableCell<>() {

                            @Override
                            protected void updateItem(
                                    OffsetDateTime value,
                                    boolean empty
                            ) {

                                super.updateItem(
                                        value,
                                        empty
                                );


                                setText(
                                        empty || value == null
                                                ? "-"
                                                : formatDateTime(
                                                        value
                                                )
                                );
                            }
                        }
        );


        historyTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );
    }


    // =========================================================
    // FILTER SETUP
    // =========================================================

    private void setupFilters() {

        filteredTasks =
                new FilteredList<>(
                        allTasks,
                        task -> true
                );


        taskTable.setItems(
                filteredTasks
        );


        statusFilterComboBox.setItems(
                FXCollections.observableArrayList(
                        "All Status",
                        "Pending",
                        "Accepted",
                        "In Progress",
                        "Completed",
                        "Overdue",
                        "Cancelled",
                        "Locked"
                )
        );


        statusFilterComboBox.setValue(
                "All Status"
        );


        searchField.textProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> applyFilters()
                );


        employeeFilterComboBox.valueProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> applyFilters()
                );


        managerFilterComboBox.valueProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> applyFilters()
                );


        categoryFilterComboBox.valueProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> applyFilters()
                );


        statusFilterComboBox.valueProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> applyFilters()
                );


        dueDateFilterPicker.valueProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> applyFilters()
                );
    }


    // =========================================================
    // POPULATE FILTERS
    // =========================================================

    private void populateFilterOptions() {

        List<String> employees =
                new ArrayList<>(
                        employeeDisplayMap.values()
                );


        employees.sort(
                String.CASE_INSENSITIVE_ORDER
        );


        employeeFilterComboBox
                .getItems()
                .setAll(
                        "All Employees"
                );


        employeeFilterComboBox
                .getItems()
                .addAll(
                        employees
                );


        employeeFilterComboBox.setValue(
                "All Employees"
        );


        List<String> managers =
                new ArrayList<>(
                        managerDisplayMap.values()
                );


        managers.sort(
                String.CASE_INSENSITIVE_ORDER
        );


        managerFilterComboBox
                .getItems()
                .setAll(
                        "All Managers"
                );


        managerFilterComboBox
                .getItems()
                .addAll(
                        managers
                );


        managerFilterComboBox.setValue(
                "All Managers"
        );


        List<String> categories =
                new ArrayList<>(
                        categoryDisplayMap.values()
                );


        categories.sort(
                String.CASE_INSENSITIVE_ORDER
        );


        categoryFilterComboBox
                .getItems()
                .setAll(
                        "All Categories"
                );


        categoryFilterComboBox
                .getItems()
                .addAll(
                        categories
                );


        categoryFilterComboBox.setValue(
                "All Categories"
        );
    }


    // =========================================================
    // APPLY FILTERS
    // =========================================================

    private void applyFilters() {

        if (filteredTasks == null) {
            return;
        }


        String keyword =
                searchField.getText() == null
                        ? ""
                        : searchField
                                .getText()
                                .trim()
                                .toLowerCase();


        String selectedEmployee =
                employeeFilterComboBox.getValue();


        String selectedManager =
                managerFilterComboBox.getValue();


        String selectedCategory =
                categoryFilterComboBox.getValue();


        String selectedStatus =
                statusFilterComboBox.getValue();


        LocalDate selectedDate =
                dueDateFilterPicker.getValue();


        filteredTasks.setPredicate(
                task -> {

                    if (task == null) {
                        return false;
                    }


                    // TASK NAME
                    boolean matchesTask =
                            keyword.isBlank()
                                    || (
                                    task.getTaskTitle() != null
                                            && task.getTaskTitle()
                                            .toLowerCase()
                                            .contains(
                                                    keyword
                                            )
                            );


                    // EMPLOYEE
                    boolean matchesEmployee =
                            true;


                    if (
                            selectedEmployee != null
                                    && !"All Employees".equals(
                                    selectedEmployee
                            )
                    ) {

                        if (
                                task.getAssignedEmployeeId() == null
                        ) {

                            matchesEmployee =
                                    false;

                        } else {

                            String display =
                                    employeeDisplayMap.get(
                                            task.getAssignedEmployeeId()
                                                    .toString()
                                    );


                            matchesEmployee =
                                    selectedEmployee.equals(
                                            display
                                    );
                        }
                    }


                    // MANAGER
                    boolean matchesManager =
                            true;


                    if (
                            selectedManager != null
                                    && !"All Managers".equals(
                                    selectedManager
                            )
                    ) {

                        if (
                                task.getAssignedBy() == null
                        ) {

                            matchesManager =
                                    false;

                        } else {

                            String display =
                                    managerDisplayMap.get(
                                            task.getAssignedBy()
                                                    .toString()
                                    );


                            matchesManager =
                                    selectedManager.equals(
                                            display
                                    );
                        }
                    }


                    // CATEGORY
                    boolean matchesCategory =
                            true;


                    if (
                            selectedCategory != null
                                    && !"All Categories".equals(
                                    selectedCategory
                            )
                    ) {

                        if (
                                task.getCategoryId() == null
                        ) {

                            matchesCategory =
                                    false;

                        } else {

                            String display =
                                    categoryDisplayMap.get(
                                            task.getCategoryId()
                                    );


                            matchesCategory =
                                    selectedCategory.equals(
                                            display
                                    );
                        }
                    }


                    // STATUS
                    boolean matchesStatus =
                            selectedStatus == null
                                    || "All Status".equals(
                                    selectedStatus
                            )
                                    || selectedStatus.equals(
                                    task.getTaskStatus()
                            );


                    // DUE DATE
                    boolean matchesDate =
                            true;


                    if (selectedDate != null) {

                        if (task.getDueDate() == null) {

                            matchesDate =
                                    false;

                        } else {

                            LocalDate dueDate =
                                    task.getDueDate()
                                            .atZoneSameInstant(
                                                    MALAYSIA_ZONE
                                            )
                                            .toLocalDate();


                            matchesDate =
                                    selectedDate.equals(
                                            dueDate
                                    );
                        }
                    }


                    return matchesTask
                            && matchesEmployee
                            && matchesManager
                            && matchesCategory
                            && matchesStatus
                            && matchesDate;
                }
        );


        clearSelectedTask();
    }


    // =========================================================
    // CLEAR FILTERS
    // =========================================================

    @FXML
    private void handleClearFilters() {

        searchField.clear();


        employeeFilterComboBox.setValue(
                "All Employees"
        );


        managerFilterComboBox.setValue(
                "All Managers"
        );


        categoryFilterComboBox.setValue(
                "All Categories"
        );


        statusFilterComboBox.setValue(
                "All Status"
        );


        dueDateFilterPicker.setValue(
                null
        );


        applyFilters();
    }


    // =========================================================
    // SELECTION
    // =========================================================

    private void configureTaskSelection() {

        taskTable
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> {

                            if (newValue != null) {

                                selectedTask =
                                        newValue;


                                displaySelectedTask(
                                        newValue
                                );


                                loadProgressHistory(
                                        newValue
                                );
                            }
                        }
                );
    }


    // =========================================================
    // DISPLAY DETAILS
    // =========================================================

    private void displaySelectedTask(
            Task task
    ) {

        selectedTaskLabel.setText(
                safe(
                        task.getTaskTitle()
                )
        );


        selectedCategoryLabel.setText(
                task.getCategoryId() == null
                        ? "-"
                        : categoryDisplayMap.getOrDefault(
                                task.getCategoryId(),
                                "-"
                        )
        );


        selectedEmployeeLabel.setText(
                task.getAssignedEmployeeId() == null
                        ? "-"
                        : employeeDisplayMap.getOrDefault(
                                task.getAssignedEmployeeId()
                                        .toString(),
                                "-"
                        )
        );


        selectedManagerLabel.setText(
                task.getAssignedBy() == null
                        ? "-"
                        : managerDisplayMap.getOrDefault(
                                task.getAssignedBy()
                                        .toString(),
                                "-"
                        )
        );


        selectedPriorityLabel.setText(
                safe(
                        task.getPriority()
                )
        );


        selectedDifficultyLabel.setText(
                safe(
                        task.getDifficultyLevel()
                )
        );


        selectedStatusLabel.setText(
                safe(
                        task.getTaskStatus()
                )
        );


        selectedProgressLabel.setText(
                String.format(
                        "%.0f%%",
                        task.getProgress()
                )
        );


        selectedAssignedDateLabel.setText(
                task.getAssignedDate() == null
                        ? "-"
                        : formatDateTime(
                                task.getAssignedDate()
                        )
        );


        selectedDueDateLabel.setText(
                task.getDueDate() == null
                        ? "-"
                        : formatDateTime(
                                task.getDueDate()
                        )
        );


        selectedDescriptionLabel.setText(
                emptyToDash(
                        task.getTaskDescription()
                )
        );


        selectedCriteriaLabel.setText(
                emptyToDash(
                        task.getCompletionCriteria()
                )
        );
    }


    // =========================================================
    // CLEAR DETAILS
    // =========================================================

    private void clearSelectedTask() {

        taskTable
                .getSelectionModel()
                .clearSelection();


        selectedTask =
                null;


        selectedTaskLabel.setText(
                "No task selected"
        );


        selectedCategoryLabel.setText("-");

        selectedEmployeeLabel.setText("-");

        selectedManagerLabel.setText("-");

        selectedPriorityLabel.setText("-");

        selectedDifficultyLabel.setText("-");

        selectedStatusLabel.setText("-");

        selectedProgressLabel.setText("-");

        selectedAssignedDateLabel.setText("-");

        selectedDueDateLabel.setText("-");

        selectedDescriptionLabel.setText("-");

        selectedCriteriaLabel.setText("-");


        historyTable
                .getItems()
                .clear();


        showHistoryMessage(
                "Select a task to view progress history."
        );
    }


    // =========================================================
    // LOAD REFERENCE DATA
    // =========================================================

    private void loadReferenceDataAndTasks() {

        hideMessage();


        javafx.concurrent.Task<Void> loadReferenceTask =
                new javafx.concurrent.Task<>() {

                    @Override
                    protected Void call()
                            throws Exception {

                        List<User> users =
                                userService
                                        .getAllUsers();


                        employeeDisplayMap.clear();

                        managerDisplayMap.clear();


                        if (users != null) {

                            for (User user : users) {

                                if (
                                        user == null
                                                || user.getUserId() == null
                                                || user.getUserId().isBlank()
                                ) {

                                    continue;
                                }


                                String staffId =
                                        user.getEmployeeId() == null
                                                || user.getEmployeeId().isBlank()
                                                ? "-"
                                                : user.getEmployeeId();


                                String name =
                                        user.getFullName() == null
                                                || user.getFullName().isBlank()
                                                ? "-"
                                                : user.getFullName();


                                String display =
                                        staffId
                                                + " - "
                                                + name;


                                if (
                                        user.getRoleId() == 3
                                ) {

                                    employeeDisplayMap.put(
                                            user.getUserId(),
                                            display
                                    );
                                }


                                if (
                                        user.getRoleId() == 2
                                ) {

                                    managerDisplayMap.put(
                                            user.getUserId(),
                                            display
                                    );
                                }
                            }
                        }


                        List<TaskCategory> categories =
                                categoryService
                                        .getActiveCategories();


                        categoryDisplayMap.clear();


                        if (categories != null) {

                            for (
                                    TaskCategory category
                                    : categories
                            ) {

                                if (
                                        category != null
                                                && category.getCategoryId() != null
                                ) {

                                    categoryDisplayMap.put(
                                            category.getCategoryId(),
                                            category.getCategoryName()
                                    );
                                }
                            }
                        }


                        return null;
                    }
                };


        loadReferenceTask.setOnSucceeded(
                event -> {

                    populateFilterOptions();

                    loadTasks();
                }
        );


        loadReferenceTask.setOnFailed(
                event -> {

                    Throwable error =
                            loadReferenceTask
                                    .getException();


                    if (error != null) {
                        error.printStackTrace();
                    }


                    loadTasks();
                }
        );


        Thread thread =
                new Thread(
                        loadReferenceTask
                );


        thread.setDaemon(true);

        thread.start();
    }


    // =========================================================
    // LOAD TASKS
    // =========================================================

    private void loadTasks() {

        javafx.concurrent.Task<List<Task>> loadTask =
                new javafx.concurrent.Task<>() {

                    @Override
                    protected List<Task> call()
                            throws Exception {

                        return taskService
                                .getAllTasks();
                    }
                };


        loadTask.setOnSucceeded(
                event -> {

                    List<Task> tasks =
                            loadTask.getValue();


                    allTasks.clear();


                    if (tasks != null) {

                        allTasks.addAll(
                                tasks
                        );
                    }


                    applyFilters();

                    taskTable.refresh();


                    if (
                            tasks == null
                                    || tasks.isEmpty()
                    ) {

                        showMessage(
                                "No tasks found."
                        );

                    } else {

                        hideMessage();
                    }
                }
        );


        loadTask.setOnFailed(
                event -> {

                    Throwable error =
                            loadTask.getException();


                    showMessage(
                            error == null
                                    ? "Unable to load tasks."
                                    : error.getMessage()
                    );
                }
        );


        Thread thread =
                new Thread(
                        loadTask
                );


        thread.setDaemon(true);

        thread.start();
    }


    // =========================================================
    // LOAD HISTORY
    // =========================================================

    private void loadProgressHistory(
            Task task
    ) {

        historyTable
                .getItems()
                .clear();


        showHistoryMessage(
                "Loading progress history..."
        );


        javafx.concurrent.Task<List<TaskProgress>> historyTask =
                new javafx.concurrent.Task<>() {

                    @Override
                    protected List<TaskProgress> call()
                            throws Exception {

                        return taskProgressService
                                .getProgressHistory(
                                        task.getTaskId()
                                );
                    }
                };


        historyTask.setOnSucceeded(
                event -> {

                    List<TaskProgress> history =
                            historyTask.getValue();


                    historyTable
                            .getItems()
                            .clear();


                    if (history != null) {

                        historyTable
                                .getItems()
                                .addAll(
                                        history
                                );
                    }


                    historyTable.refresh();


                    if (
                            history == null
                                    || history.isEmpty()
                    ) {

                        showHistoryMessage(
                                "No progress history yet."
                        );

                    } else {

                        hideHistoryMessage();
                    }
                }
        );


        historyTask.setOnFailed(
                event -> {

                    Throwable error =
                            historyTask.getException();


                    showHistoryMessage(
                            error == null
                                    ? "Unable to load progress history."
                                    : error.getMessage()
                    );
                }
        );


        Thread thread =
                new Thread(
                        historyTask
                );


        thread.setDaemon(true);

        thread.start();
    }


    // =========================================================
    // REFRESH
    // =========================================================

    @FXML
    private void handleRefresh() {

        loadReferenceDataAndTasks();
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
    private void handleTaskManagement(ActionEvent event) {
        navigate(
            event,
            "/fxml/admin/adminTask.fxml"
        );   
    }

    @FXML
    private void handleManageCategories(ActionEvent event) {

        navigate(
            event,
            "/fxml/admin/adminTaskCategory.fxml"
        );
    }

    @FXML
    private void handleProgressiveRules(ActionEvent event) {

        navigate(
                event,
                "/fxml/admin/adminTaskDependency.fxml"
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

    private String safe(
            String value
    ) {

        return value == null
                || value.isBlank()
                ? "-"
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


    private String formatDateTime(
            OffsetDateTime value
    ) {

        return value
                .atZoneSameInstant(
                        MALAYSIA_ZONE
                )
                .format(
                        DATE_FORMATTER
                );
    }


    private void showMessage(
            String value
    ) {

        messageLabel.setText(value);

        messageLabel.setVisible(true);

        messageLabel.setManaged(true);
    }


    private void hideMessage() {

        messageLabel.setText("");

        messageLabel.setVisible(false);

        messageLabel.setManaged(false);
    }


    private void showHistoryMessage(
            String value
    ) {

        historyMessageLabel.setText(value);

        historyMessageLabel.setVisible(true);

        historyMessageLabel.setManaged(true);
    }


    private void hideHistoryMessage() {

        historyMessageLabel.setText("");

        historyMessageLabel.setVisible(false);

        historyMessageLabel.setManaged(false);
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