package com.aepm.ui.employee;

import com.aepm.logic.task.TaskCategoryService;
import com.aepm.logic.task.TaskProgressService;
import com.aepm.logic.task.TaskService;

import com.aepm.model.Task;
import com.aepm.model.TaskCategory;
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

import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import javafx.scene.control.cell.PropertyValueFactory;

import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmployeeTaskController {

    // =========================================================
    // TOP BAR
    // =========================================================

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label topNameLabel;


    // =========================================================
    // SEARCH / FILTER
    // =========================================================

    @FXML
    private TextField searchField;

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
    private TableColumn<Task, String> priorityColumn;

    @FXML
    private TableColumn<Task, String> difficultyColumn;

    @FXML
    private TableColumn<Task, String> statusColumn;

    @FXML
    private TableColumn<Task, Double> progressColumn;

    @FXML
    private TableColumn<Task, OffsetDateTime> dueDateColumn;


    // =========================================================
    // SELECTED TASK
    // =========================================================

    @FXML
    private Label selectedTaskLabel;

    @FXML
    private Label selectedCategoryLabel;

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
    // PROGRESS
    // =========================================================

    @FXML
    private TextField progressField;

    @FXML
    private TextArea progressNoteArea;


    // =========================================================
    // BUTTONS
    // =========================================================

    @FXML
    private Button acceptButton;

    @FXML
    private Button startButton;

    @FXML
    private Button updateProgressButton;

    @FXML
    private Button completeButton;


    // =========================================================
    // MESSAGE
    // =========================================================

    @FXML
    private Label messageLabel;


    // =========================================================
    // SERVICES
    // =========================================================

    private final TaskService taskService =
            new TaskService();

    private final TaskProgressService taskProgressService =
            new TaskProgressService();

    private final TaskCategoryService categoryService =
            new TaskCategoryService();


    // =========================================================
    // CATEGORY MAP
    // =========================================================

    private final Map<Long, String> categoryDisplayMap =
            new HashMap<>();


    // =========================================================
    // TASK DATA FOR SEARCH / FILTER
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

        setupTable();

        setupFilters();

        configureSelection();

        clearSelection();

        loadCategoriesAndTasks();
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
                        ? "Employee"
                        : user.getFullName();


        welcomeLabel.setText(
                "Welcome, " + name
        );


        topNameLabel.setText(
                name
        );
    }


    // =========================================================
    // SETUP TABLE
    // =========================================================

    private void setupTable() {

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


        priorityColumn.setCellValueFactory(
                new PropertyValueFactory<>(
                        "priority"
                )
        );


        difficultyColumn.setCellValueFactory(
                new PropertyValueFactory<>(
                        "difficultyLevel"
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


        // =====================================================
        // PROGRESS DISPLAY
        // =====================================================

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


        // =====================================================
        // DUE DATE DISPLAY
        // =====================================================

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


                                if (
                                        empty
                                                || value == null
                                ) {

                                    setText("-");

                                } else {

                                    setText(
                                            formatDateTime(
                                                    value
                                            )
                                    );
                                }
                            }
                        }
        );


        taskTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );
    }


    // =========================================================
    // SEARCH / FILTER SETUP
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


        searchField
                .textProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> applyFilters()
                );


        dueDateFilterPicker
                .valueProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> applyFilters()
                );
    }


    // =========================================================
    // APPLY FILTER
    // =========================================================

    private void applyFilters() {

        String searchText =
                searchField.getText() == null
                        ? ""
                        : searchField
                                .getText()
                                .trim()
                                .toLowerCase();


        LocalDate selectedDate =
                dueDateFilterPicker
                        .getValue();


        filteredTasks.setPredicate(
                task -> {

                    if (task == null) {
                        return false;
                    }


                    // =========================================
                    // TASK NAME SEARCH
                    // =========================================

                    boolean matchesName =
                            searchText.isBlank()
                                    || (
                                    task.getTaskTitle() != null
                                            && task.getTaskTitle()
                                            .toLowerCase()
                                            .contains(
                                                    searchText
                                            )
                            );


                    // =========================================
                    // DUE DATE FILTER
                    // =========================================

                    boolean matchesDate =
                            true;


                    if (selectedDate != null) {

                        if (task.getDueDate() == null) {

                            matchesDate =
                                    false;

                        } else {

                            LocalDate taskDueDate =
                                    task.getDueDate()
                                            .atZoneSameInstant(
                                                    MALAYSIA_ZONE
                                            )
                                            .toLocalDate();


                            matchesDate =
                                    taskDueDate.equals(
                                            selectedDate
                                    );
                        }
                    }


                    return matchesName
                            && matchesDate;
                }
        );


        clearSelection();
    }


    // =========================================================
    // CLEAR FILTER
    // =========================================================

    @FXML
    private void handleClearFilter() {

        searchField.clear();

        dueDateFilterPicker.setValue(
                null
        );


        applyFilters();
    }


    // =========================================================
    // TABLE SELECTION
    // =========================================================

    private void configureSelection() {

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
                            }
                        }
                );
    }


    // =========================================================
    // DISPLAY SELECTED TASK
    // =========================================================

    private void displaySelectedTask(
            Task task
    ) {

        selectedTaskLabel.setText(
                safe(
                        task.getTaskTitle()
                )
        );


        if (task.getCategoryId() == null) {

            selectedCategoryLabel.setText(
                    "-"
            );

        } else {

            selectedCategoryLabel.setText(
                    categoryDisplayMap.getOrDefault(
                            task.getCategoryId(),
                            "-"
                    )
            );
        }


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


        progressField.setText(
                String.format(
                        "%.0f",
                        task.getProgress()
                )
        );


        progressNoteArea.clear();


        updateButtonStates(
                task
        );
    }


    // =========================================================
    // BUTTON STATES
    // =========================================================

    private void updateButtonStates(
            Task task
    ) {

        String status =
                task.getTaskStatus();


        acceptButton.setDisable(
                !"Pending".equals(status)
        );


        startButton.setDisable(
                !"Accepted".equals(status)
        );


        updateProgressButton.setDisable(
                !"In Progress".equals(status)
        );


        completeButton.setDisable(
                !"In Progress".equals(status)
        );


        boolean canUpdate =
                "In Progress".equals(status);


        progressField.setDisable(
                !canUpdate
        );


        progressNoteArea.setDisable(
                !canUpdate
        );
    }


    // =========================================================
    // CLEAR SELECTION
    // =========================================================

    private void clearSelection() {

        taskTable
                .getSelectionModel()
                .clearSelection();


        selectedTask =
                null;


        selectedTaskLabel.setText(
                "No task selected"
        );


        selectedCategoryLabel.setText(
                "-"
        );


        selectedPriorityLabel.setText(
                "-"
        );


        selectedDifficultyLabel.setText(
                "-"
        );


        selectedStatusLabel.setText(
                "-"
        );


        selectedProgressLabel.setText(
                "-"
        );


        selectedAssignedDateLabel.setText(
                "-"
        );


        selectedDueDateLabel.setText(
                "-"
        );


        selectedDescriptionLabel.setText(
                "-"
        );


        selectedCriteriaLabel.setText(
                "-"
        );


        progressField.clear();

        progressNoteArea.clear();


        progressField.setDisable(
                true
        );


        progressNoteArea.setDisable(
                true
        );


        acceptButton.setDisable(
                true
        );


        startButton.setDisable(
                true
        );


        updateProgressButton.setDisable(
                true
        );


        completeButton.setDisable(
                true
        );
    }


    // =========================================================
    // LOAD CATEGORY + TASK
    // =========================================================

    private void loadCategoriesAndTasks() {

        hideMessage();


        javafx.concurrent.Task<List<TaskCategory>> categoryTask =
                new javafx.concurrent.Task<>() {

                    @Override
                    protected List<TaskCategory> call()
                            throws Exception {

                        return categoryService
                                .getActiveCategories();
                    }
                };


        categoryTask.setOnSucceeded(
                event -> {

                    categoryDisplayMap.clear();


                    List<TaskCategory> categories =
                            categoryTask.getValue();


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


                    loadTasks();
                }
        );


        categoryTask.setOnFailed(
                event -> loadTasks()
        );


        Thread thread =
                new Thread(
                        categoryTask
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
                                .getMyTasks();
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
                                "No tasks assigned to you."
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
    // ACCEPT TASK
    // =========================================================

    @FXML
    private void handleAcceptTask() {

        if (!checkSelectedTask()) {
            return;
        }


        runTaskAction(
                () ->
                        taskService.acceptTask(
                                selectedTask.getTaskId()
                        )
        );
    }


    // =========================================================
    // START TASK
    // =========================================================

    @FXML
    private void handleStartTask() {

        if (!checkSelectedTask()) {
            return;
        }


        runTaskAction(
                () ->
                        taskService.startTask(
                                selectedTask.getTaskId()
                        )
        );
    }


    // =========================================================
    // UPDATE PROGRESS
    // =========================================================

    @FXML
    private void handleUpdateProgress() {

        if (!checkSelectedTask()) {
            return;
        }


        String progressText =
                progressField
                        .getText()
                        .trim();


        String note =
                progressNoteArea
                        .getText()
                        .trim();


        if (progressText.isBlank()) {

            showMessage(
                    "Please enter progress percentage."
            );

            return;
        }


        double progress;


        try {

            progress =
                    Double.parseDouble(
                            progressText
                    );

        } catch (NumberFormatException e) {

            showMessage(
                    "Progress must be a number."
            );

            return;
        }


        if (
                progress < 0
                        || progress > 100
        ) {

            showMessage(
                    "Progress must be between 0 and 100."
            );

            return;
        }


        if (
                progress
                        < selectedTask.getProgress()
        ) {

            showMessage(
                    "New progress cannot be lower than current progress."
            );

            return;
        }


        if (note.isBlank()) {

            showMessage(
                    "Please enter a progress note."
            );

            return;
        }


        double finalProgress =
                progress;


        runTaskAction(
                () -> {

                    taskService.updateProgress(
                            selectedTask.getTaskId(),
                            finalProgress
                    );


                    taskProgressService.addProgress(
                            selectedTask.getTaskId(),
                            finalProgress,
                            note
                    );
                }
        );
    }


    // =========================================================
    // COMPLETE TASK
    // =========================================================

    @FXML
    private void handleCompleteTask() {

        if (!checkSelectedTask()) {
            return;
        }


        runTaskAction(
                () -> {

                    taskService.completeTask(
                            selectedTask.getTaskId()
                    );


                    taskProgressService.addProgress(
                            selectedTask.getTaskId(),
                            100,
                            "Task completed."
                    );
                }
        );
    }


    // =========================================================
    // ACTION HELPER
    // =========================================================

    private void runTaskAction(
            TaskAction action
    ) {

        disableActionButtons(
                true
        );


        hideMessage();


        javafx.concurrent.Task<Void> actionTask =
                new javafx.concurrent.Task<>() {

                    @Override
                    protected Void call()
                            throws Exception {

                        action.run();

                        return null;
                    }
                };


        actionTask.setOnSucceeded(
                event -> loadCategoriesAndTasks()
        );


        actionTask.setOnFailed(
                event -> {

                    disableActionButtons(
                            false
                    );


                    Throwable error =
                            actionTask.getException();


                    showMessage(
                            error == null
                                    ? "Unable to update task."
                                    : error.getMessage()
                    );
                }
        );


        Thread thread =
                new Thread(
                        actionTask
                );


        thread.setDaemon(true);

        thread.start();
    }


    private void disableActionButtons(
            boolean value
    ) {

        acceptButton.setDisable(value);

        startButton.setDisable(value);

        updateProgressButton.setDisable(value);

        completeButton.setDisable(value);

        progressField.setDisable(value);

        progressNoteArea.setDisable(value);
    }


    private boolean checkSelectedTask() {

        if (selectedTask == null) {

            showMessage(
                    "Please select a task first."
            );

            return false;
        }


        return true;
    }


    @FunctionalInterface
    private interface TaskAction {

        void run()
                throws Exception;
    }


    // =========================================================
    // REFRESH
    // =========================================================

    @FXML
    private void handleRefresh() {

        loadCategoriesAndTasks();
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
    private void handleTasks() {
    }


    @FXML
    private void handleKpi() {
    }


    @FXML
    private void handleReports() {
    }


    @FXML
    private void handleAnonymousReport() {
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

    private String formatDateTime(
            OffsetDateTime dateTime
    ) {

        return dateTime
                .atZoneSameInstant(
                        MALAYSIA_ZONE
                )
                .format(
                        DATE_FORMATTER
                );
    }


    private String safe(
            String value
    ) {

        return value == null
                ? "-"
                : value;
    }


    private String emptyToDash(
            String value
    ) {

        if (
                value == null
                        || value.isBlank()
        ) {

            return "-";
        }


        return value;
    }


    private void showMessage(
            String value
    ) {

        messageLabel.setText(
                value
        );


        messageLabel.setVisible(
                true
        );


        messageLabel.setManaged(
                true
        );
    }


    private void hideMessage() {

        messageLabel.setText(
                ""
        );


        messageLabel.setVisible(
                false
        );


        messageLabel.setManaged(
                false
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