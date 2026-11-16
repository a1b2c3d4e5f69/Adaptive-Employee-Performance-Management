package com.aepm.ui.manager;

import com.aepm.logic.task.TaskCategoryService;
import com.aepm.logic.task.TaskProgressService;
import com.aepm.logic.task.TaskService;
import com.aepm.logic.task.TeamTaskService;
import com.aepm.logic.user.UserService;
import com.aepm.model.Task;
import com.aepm.model.TaskAssignment;
import com.aepm.model.TaskCategory;
import com.aepm.model.TaskProgress;
import com.aepm.model.User;
import com.aepm.ui.navigation.NavigationManager;
import com.aepm.util.ManagerTaskEditContext;
import com.aepm.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
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
import java.util.Optional;
import java.util.UUID;

public class ManagerTaskController {

    @FXML private Label welcomeLabel;
    @FXML private Label topNameLabel;
    @FXML private Label topRoleLabel;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> employeeFilterComboBox;
    @FXML private ComboBox<String> categoryFilterComboBox;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private DatePicker dueDateFilterPicker;

    @FXML private TableView<Task> taskTable;
    @FXML private TableColumn<Task, String> titleColumn;
    @FXML private TableColumn<Task, String> categoryColumn;
    @FXML private TableColumn<Task, String> employeeColumn;
    @FXML private TableColumn<Task, String> priorityColumn;
    @FXML private TableColumn<Task, String> difficultyColumn;
    @FXML private TableColumn<Task, String> statusColumn;
    @FXML private TableColumn<Task, Double> progressColumn;
    @FXML private TableColumn<Task, OffsetDateTime> dueDateColumn;

    @FXML private TableView<TaskProgress> historyTable;
    @FXML private TableColumn<TaskProgress, Double> historyProgressColumn;
    @FXML private TableColumn<TaskProgress, String> historyNoteColumn;
    @FXML private TableColumn<TaskProgress, OffsetDateTime> historyDateColumn;

    @FXML private Label messageLabel;
    @FXML private Label historyMessageLabel;

    private final TaskService taskService = new TaskService();
    private final TaskProgressService taskProgressService = new TaskProgressService();
    private final TaskCategoryService categoryService = new TaskCategoryService();
    private final UserService userService = new UserService();
    private final TeamTaskService teamTaskService = new TeamTaskService();

    private final Map<String, String> employeeDisplayMap = new HashMap<>();
    private final Map<Long, String> categoryDisplayMap = new HashMap<>();
    private final Map<UUID, String> teamMemberDisplayMap = new HashMap<>();

    private final ObservableList<Task> allTasks = FXCollections.observableArrayList();
    private FilteredList<Task> filteredTasks;
    private Task selectedTask;

    private static final ZoneId MALAYSIA_ZONE = ZoneId.of("Asia/Kuala_Lumpur");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        loadCurrentUser();
        setupTaskTable();
        setupHistoryTable();
        setupFilters();
        configureTaskSelection();
        clearTaskSelection();
        loadReferenceDataAndTasks();
    }

    private void loadCurrentUser() {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        String name = user.getFullName();
        if (name == null || name.isBlank()) name = user.getUsername();
        if (name == null || name.isBlank()) name = "Manager";

        if (welcomeLabel != null) welcomeLabel.setText("Welcome, " + name);
        if (topNameLabel != null) topNameLabel.setText(name);
        if (topRoleLabel != null) {
            String employeeId = user.getEmployeeId();
            topRoleLabel.setText(employeeId == null || employeeId.isBlank()
                    ? "MANAGER"
                    : "MANAGER  •  " + employeeId);
        }
    }

    private void setupTaskTable() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("taskTitle"));

        categoryColumn.setCellValueFactory(cellData -> {
            Task task = cellData.getValue();
            if (task == null || task.getCategoryId() == null) return new SimpleStringProperty("-");
            return new SimpleStringProperty(categoryDisplayMap.getOrDefault(task.getCategoryId(), "-"));
        });

        employeeColumn.setCellValueFactory(cellData -> {
            Task task = cellData.getValue();
            if (task == null) return new SimpleStringProperty("-");

            if (task.getAssignedEmployeeId() != null) {
                String userId = task.getAssignedEmployeeId().toString();
                return new SimpleStringProperty(employeeDisplayMap.getOrDefault(userId, userId));
            }

            return new SimpleStringProperty(
                    teamMemberDisplayMap.getOrDefault(task.getTaskId(), "Team Task")
            );
        });

        priorityColumn.setCellValueFactory(new PropertyValueFactory<>("priority"));
        difficultyColumn.setCellValueFactory(new PropertyValueFactory<>("difficultyLevel"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("taskStatus"));
        progressColumn.setCellValueFactory(new PropertyValueFactory<>("progress"));
        dueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));

        progressColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? "-" : String.format("%.0f%%", value));
            }
        });

        dueDateColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(OffsetDateTime value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? "-" : formatDateTime(value));
            }
        });

        taskTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private void setupHistoryTable() {
        historyProgressColumn.setCellValueFactory(new PropertyValueFactory<>("progressPercentage"));
        historyNoteColumn.setCellValueFactory(new PropertyValueFactory<>("progressNote"));
        historyDateColumn.setCellValueFactory(new PropertyValueFactory<>("updatedAt"));

        historyProgressColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? "-" : String.format("%.0f%%", value));
            }
        });

        historyDateColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(OffsetDateTime value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? "-" : formatDateTime(value));
            }
        });

        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private void setupFilters() {
        filteredTasks = new FilteredList<>(allTasks, task -> true);
        taskTable.setItems(filteredTasks);

        statusFilterComboBox.setItems(FXCollections.observableArrayList(
                "All Status", "Pending", "Accepted", "In Progress",
                "Completed", "Overdue", "Cancelled", "Locked"
        ));
        statusFilterComboBox.setValue("All Status");

        searchField.textProperty().addListener((o, oldValue, newValue) -> applyFilters());
        employeeFilterComboBox.valueProperty().addListener((o, oldValue, newValue) -> applyFilters());
        categoryFilterComboBox.valueProperty().addListener((o, oldValue, newValue) -> applyFilters());
        statusFilterComboBox.valueProperty().addListener((o, oldValue, newValue) -> applyFilters());
        dueDateFilterPicker.valueProperty().addListener((o, oldValue, newValue) -> applyFilters());
    }

    private void populateFilterOptions() {
        List<String> employees = new ArrayList<>(employeeDisplayMap.values());
        employees.sort(String.CASE_INSENSITIVE_ORDER);

        employeeFilterComboBox.getItems().clear();
        employeeFilterComboBox.getItems().add("All Employees");
        employeeFilterComboBox.getItems().addAll(employees);
        employeeFilterComboBox.setValue("All Employees");

        List<String> categories = new ArrayList<>(categoryDisplayMap.values());
        categories.sort(String.CASE_INSENSITIVE_ORDER);

        categoryFilterComboBox.getItems().clear();
        categoryFilterComboBox.getItems().add("All Categories");
        categoryFilterComboBox.getItems().addAll(categories);
        categoryFilterComboBox.setValue("All Categories");
    }

    private void applyFilters() {
        if (filteredTasks == null) return;

        String searchText = searchField.getText() == null
                ? ""
                : searchField.getText().trim().toLowerCase();
        String selectedEmployee = employeeFilterComboBox.getValue();
        String selectedCategory = categoryFilterComboBox.getValue();
        String selectedStatus = statusFilterComboBox.getValue();
        LocalDate selectedDueDate = dueDateFilterPicker.getValue();

        filteredTasks.setPredicate(task -> {
            if (task == null) return false;

            boolean matchesTaskName = searchText.isBlank()
                    || (task.getTaskTitle() != null
                    && task.getTaskTitle().toLowerCase().contains(searchText));

            boolean matchesEmployee = true;
            if (selectedEmployee != null && !"All Employees".equals(selectedEmployee)) {
                if (task.getAssignedEmployeeId() != null) {
                    String employeeDisplay = employeeDisplayMap.get(task.getAssignedEmployeeId().toString());
                    matchesEmployee = selectedEmployee.equals(employeeDisplay);
                } else {
                    String members = teamMemberDisplayMap.get(task.getTaskId());
                    matchesEmployee = members != null && members.contains(selectedEmployee);
                }
            }

            boolean matchesCategory = true;
            if (selectedCategory != null && !"All Categories".equals(selectedCategory)) {
                if (task.getCategoryId() == null) {
                    matchesCategory = false;
                } else {
                    String categoryName = categoryDisplayMap.get(task.getCategoryId());
                    matchesCategory = selectedCategory.equals(categoryName);
                }
            }

            boolean matchesStatus = selectedStatus == null
                    || "All Status".equals(selectedStatus)
                    || selectedStatus.equals(task.getTaskStatus());

            boolean matchesDueDate = true;
            if (selectedDueDate != null) {
                if (task.getDueDate() == null) {
                    matchesDueDate = false;
                } else {
                    LocalDate taskDueDate = task.getDueDate()
                            .atZoneSameInstant(MALAYSIA_ZONE)
                            .toLocalDate();
                    matchesDueDate = taskDueDate.equals(selectedDueDate);
                }
            }

            return matchesTaskName
                    && matchesEmployee
                    && matchesCategory
                    && matchesStatus
                    && matchesDueDate;
        });

        clearTaskSelection();
    }

    @FXML
    private void handleClearFilters() {
        searchField.clear();
        employeeFilterComboBox.setValue("All Employees");
        categoryFilterComboBox.setValue("All Categories");
        statusFilterComboBox.setValue("All Status");
        dueDateFilterPicker.setValue(null);
        applyFilters();
    }

    private void configureTaskSelection() {
        taskTable.getSelectionModel().selectedItemProperty().addListener((o, oldValue, newValue) -> {
            if (newValue != null) {
                selectedTask = newValue;
                loadProgressHistory(newValue);
            }
        });

        taskTable.setOnMouseClicked(event -> {
            if (event.getClickCount() != 2) return;

            Task task = taskTable.getSelectionModel().getSelectedItem();
            if (task == null) return;

            selectedTask = task;
            showTaskPopup(task);
        });
    }

    private void showTaskPopup(Task task) {
        String category = task.getCategoryId() == null
                ? "-"
                : categoryDisplayMap.getOrDefault(task.getCategoryId(), "-");

        Stage owner = (Stage) taskTable.getScene().getWindow();

        ManagerTaskDetailsDialog dialog = new ManagerTaskDetailsDialog();
        dialog.show(
                owner,
                task,
                category,
                employeeDisplayMap,
                () -> openEditTask(task),
                () -> cancelTask(task)
        );
    }

    private void clearTaskSelection() {
        if (taskTable != null) taskTable.getSelectionModel().clearSelection();
        selectedTask = null;

        if (historyTable != null) historyTable.getItems().clear();
        showHistoryMessage("Select a task to view progress history.");
    }

    private void loadReferenceDataAndTasks() {
        hideMessage();

        javafx.concurrent.Task<Void> referenceTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() throws Exception {
                List<User> users = userService.getAllUsers();
                employeeDisplayMap.clear();

                if (users != null) {
                    for (User user : users) {
                        if (user == null || user.getRoleId() != 3) continue;
                        if (user.getUserId() == null || user.getUserId().isBlank()) continue;

                        String staffId = user.getEmployeeId() == null || user.getEmployeeId().isBlank()
                                ? "-"
                                : user.getEmployeeId();

                        String name = user.getFullName();
                        if (name == null || name.isBlank()) name = user.getUsername();
                        if (name == null || name.isBlank()) name = "-";

                        employeeDisplayMap.put(user.getUserId(), staffId + " - " + name);
                    }
                }

                List<TaskCategory> categories = categoryService.getActiveCategories();
                categoryDisplayMap.clear();

                if (categories != null) {
                    for (TaskCategory category : categories) {
                        if (category == null || category.getCategoryId() == null) continue;
                        categoryDisplayMap.put(category.getCategoryId(), category.getCategoryName());
                    }
                }

                return null;
            }
        };

        referenceTask.setOnSucceeded(event -> {
            populateFilterOptions();
            loadTasks();
        });

        referenceTask.setOnFailed(event -> {
            Throwable error = referenceTask.getException();
            if (error != null) error.printStackTrace();
            loadTasks();
        });

        Thread thread = new Thread(referenceTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void loadTasks() {
        javafx.concurrent.Task<List<Task>> loadTask = new javafx.concurrent.Task<>() {
            @Override
            protected List<Task> call() throws Exception {
                return taskService.getAllTasks();
            }
        };

        loadTask.setOnSucceeded(event -> {
            List<Task> tasks = loadTask.getValue();
            allTasks.clear();
            teamMemberDisplayMap.clear();

            if (tasks != null) allTasks.addAll(tasks);

            applyFilters();
            taskTable.refresh();
            loadTeamTaskMembers();

            if (tasks == null || tasks.isEmpty()) {
                showMessage("No tasks found.");
            } else {
                hideMessage();
            }
        });

        loadTask.setOnFailed(event -> {
            Throwable error = loadTask.getException();
            showMessage(error == null ? "Unable to load tasks." : error.getMessage());
        });

        Thread thread = new Thread(loadTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void loadTeamTaskMembers() {
        for (Task task : allTasks) {
            if (task == null || task.getTaskId() == null || task.getAssignedEmployeeId() != null) continue;

            javafx.concurrent.Task<List<TaskAssignment>> worker = new javafx.concurrent.Task<>() {
                @Override
                protected List<TaskAssignment> call() throws Exception {
                    return teamTaskService.getAssignments(task.getTaskId());
                }
            };

            worker.setOnSucceeded(event -> {
                teamMemberDisplayMap.put(task.getTaskId(), buildMemberDisplay(worker.getValue()));
                taskTable.refresh();
            });

            worker.setOnFailed(event -> {
                teamMemberDisplayMap.put(task.getTaskId(), "Team Task");
                taskTable.refresh();
            });

            Thread thread = new Thread(worker);
            thread.setDaemon(true);
            thread.start();
        }
    }

    private String buildMemberDisplay(List<TaskAssignment> assignments) {
        if (assignments == null || assignments.isEmpty()) return "Team Task";

        List<String> names = new ArrayList<>();
        for (TaskAssignment assignment : assignments) {
            if (assignment == null || assignment.getEmployeeId() == null) continue;

            String userId = assignment.getEmployeeId().toString();
            names.add(employeeDisplayMap.getOrDefault(userId, userId));
        }

        return names.isEmpty() ? "Team Task" : String.join(", ", names);
    }

    private void loadProgressHistory(Task task) {
        historyTable.getItems().clear();
        showHistoryMessage("Loading progress history...");

        javafx.concurrent.Task<List<TaskProgress>> historyTask = new javafx.concurrent.Task<>() {
            @Override
            protected List<TaskProgress> call() throws Exception {
                return taskProgressService.getProgressHistory(task.getTaskId());
            }
        };

        historyTask.setOnSucceeded(event -> {
            List<TaskProgress> history = historyTask.getValue();
            historyTable.getItems().clear();
            if (history != null) historyTable.getItems().addAll(history);
            historyTable.refresh();

            if (history == null || history.isEmpty()) {
                showHistoryMessage("No progress history yet.");
            } else {
                hideHistoryMessage();
            }
        });

        historyTask.setOnFailed(event -> {
            Throwable error = historyTask.getException();
            showHistoryMessage(error == null ? "Unable to load progress history." : error.getMessage());
        });

        Thread thread = new Thread(historyTask);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleCreateTask(ActionEvent event) {
        navigate(event, "/fxml/manager/managerCreateTask.fxml");
    }

    private void openEditTask(Task task) {
        if (task == null) return;

        if ("Completed".equals(task.getTaskStatus()) || "Cancelled".equals(task.getTaskStatus())) {
            showMessage("Completed or cancelled tasks cannot be edited.");
            return;
        }

        ManagerTaskEditContext.setTask(task);
        Stage stage = (Stage) taskTable.getScene().getWindow();
        NavigationManager.navigate(stage, "/fxml/manager/managerEditTask.fxml");
    }

    private void cancelTask(Task task) {
        if (task == null) return;

        if ("Completed".equals(task.getTaskStatus())) {
            showMessage("Completed tasks cannot be cancelled.");
            return;
        }

        if ("Cancelled".equals(task.getTaskStatus())) {
            showMessage("This task is already cancelled.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Cancel Task");
        confirmation.setHeaderText("Cancel " + task.getTaskTitle() + "?");
        confirmation.setContentText("This task will be marked as Cancelled.");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        javafx.concurrent.Task<Void> cancelWorker = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() throws Exception {
                taskService.cancelTask(task.getTaskId());
                return null;
            }
        };

        cancelWorker.setOnSucceeded(event -> {
            selectedTask = null;
            loadReferenceDataAndTasks();
        });

        cancelWorker.setOnFailed(event -> {
            Throwable error = cancelWorker.getException();
            showMessage(error == null ? "Unable to cancel task." : error.getMessage());
        });

        Thread thread = new Thread(cancelWorker);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleRefresh() {
        loadReferenceDataAndTasks();
    }

    @FXML
    private void handleHome(ActionEvent event) {
        navigate(event, "/fxml/manager/managerHome.fxml");
    }

    @FXML
    private void handleProfile(ActionEvent event) {
        navigate(event, "/fxml/manager/managerProfile.fxml");
    }

    @FXML
    private void handleTasks() {
        // Already on this page.
    }

    @FXML
    private void handleTaskManagement() {
        // Already on this page.
    }

    @FXML
    private void handleDeadlineEscalation(ActionEvent event) {
        navigate(event, "/fxml/manager/managerDeadlineEscalation.fxml");
    }

    @FXML
    private void handlePerformance(ActionEvent event) {
        navigate(event, "/fxml/manager/managerPerformance.fxml");
    }

    @FXML
    private void handleReports(ActionEvent event) {
        navigate(event, "/fxml/manager/managerReport.fxml");
    }

    @FXML
    private void handleDashboard(ActionEvent event) {
        navigate(event, "/fxml/manager/managerHome.fxml");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        SessionManager.clearSession();
        navigate(event, "/fxml/auth/login.fxml");
    }

    private String formatDateTime(OffsetDateTime value) {
        return value.atZoneSameInstant(MALAYSIA_ZONE).format(DATE_FORMATTER);
    }

    private void showMessage(String value) {
        if (messageLabel == null) return;
        messageLabel.setText(value);
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    private void hideMessage() {
        if (messageLabel == null) return;
        messageLabel.setText("");
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
    }

    private void showHistoryMessage(String value) {
        if (historyMessageLabel == null) return;
        historyMessageLabel.setText(value);
        historyMessageLabel.setVisible(true);
        historyMessageLabel.setManaged(true);
    }

    private void hideHistoryMessage() {
        if (historyMessageLabel == null) return;
        historyMessageLabel.setText("");
        historyMessageLabel.setVisible(false);
        historyMessageLabel.setManaged(false);
    }

    private void navigate(ActionEvent event, String path) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        NavigationManager.navigate(stage, path);
    }
}
