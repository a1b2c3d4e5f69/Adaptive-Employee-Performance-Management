package com.aepm.ui.employee;

import com.aepm.logic.task.EmployeeDailyTaskService;
import com.aepm.model.EmployeeDailyTask;
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
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;


public class EmployeeDailyTaskController {

    @FXML
    private DatePicker taskDatePicker;

    @FXML
    private ComboBox<String> statusFilter;

    @FXML
    private ComboBox<String> priorityFilter;

    @FXML
    private Label totalLabel;

    @FXML
    private Label completedLabel;

    @FXML
    private Label remainingLabel;

    @FXML
    private Label completionLabel;

    @FXML
    private Label messageLabel;

    @FXML
    private TableView<EmployeeDailyTask> dailyTaskTable;

    @FXML
    private TableColumn<EmployeeDailyTask, String> titleColumn;

    @FXML
    private TableColumn<EmployeeDailyTask, String> categoryColumn;

    @FXML
    private TableColumn<EmployeeDailyTask, String> priorityColumn;

    @FXML
    private TableColumn<EmployeeDailyTask, String> dueTimeColumn;

    @FXML
    private TableColumn<EmployeeDailyTask, String> statusColumn;

    @FXML
    private TableColumn<EmployeeDailyTask, String> completedAtColumn;


    private final EmployeeDailyTaskService service =
            new EmployeeDailyTaskService();


    private final ObservableList<EmployeeDailyTask> allTasks =
            FXCollections.observableArrayList();


    private FilteredList<EmployeeDailyTask> filteredTasks;


    private static final ZoneId MALAYSIA_ZONE =
            ZoneId.of("Asia/Kuala_Lumpur");


    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm");


    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");


    @FXML
    private void initialize() {

        setupFilters();

        setupTable();

        taskDatePicker.setValue(
                LocalDate.now()
        );


        taskDatePicker
                .valueProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> {

                            if (newValue != null) {
                                loadTasks();
                            }
                        }
                );


        statusFilter
                .valueProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> applyFilter()
                );


        priorityFilter
                .valueProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> applyFilter()
                );


        loadTasks();
    }


    // =========================================================
    // FILTER
    // =========================================================

    private void setupFilters() {

        statusFilter.getItems().setAll(
                "All",
                "Pending",
                "In Progress",
                "Completed",
                "Cancelled"
        );


        priorityFilter.getItems().setAll(
                "All",
                "Low",
                "Medium",
                "High"
        );


        statusFilter.setValue("All");

        priorityFilter.setValue("All");


        filteredTasks =
                new FilteredList<>(
                        allTasks,
                        task -> true
                );


        dailyTaskTable.setItems(
                filteredTasks
        );
    }


    private void applyFilter() {

        String status =
                statusFilter.getValue();


        String priority =
                priorityFilter.getValue();


        filteredTasks.setPredicate(
                task -> {

                    boolean statusMatches =
                            status == null
                                    || "All".equals(status)
                                    || status.equals(
                                    task.getTaskStatus()
                            );


                    boolean priorityMatches =
                            priority == null
                                    || "All".equals(priority)
                                    || priority.equals(
                                    task.getPriority()
                            );


                    return statusMatches
                            && priorityMatches;
                }
        );
    }


    @FXML
    private void handleClearFilter() {

        statusFilter.setValue("All");

        priorityFilter.setValue("All");
    }


    // =========================================================
    // TABLE
    // =========================================================

    private void setupTable() {

        titleColumn.setCellValueFactory(
                data ->
                        new SimpleStringProperty(
                                safe(
                                        data.getValue()
                                                .getTaskTitle()
                                )
                        )
        );


        categoryColumn.setCellValueFactory(
                data ->
                        new SimpleStringProperty(
                                safe(
                                        data.getValue()
                                                .getCategory()
                                )
                        )
        );


        priorityColumn.setCellValueFactory(
                data ->
                        new SimpleStringProperty(
                                safe(
                                        data.getValue()
                                                .getPriority()
                                )
                        )
        );


        dueTimeColumn.setCellValueFactory(
                data -> {

                    LocalTime value =
                            data.getValue()
                                    .getDueTime();


                    return new SimpleStringProperty(
                            value == null
                                    ? "-"
                                    : value.format(
                                    TIME_FORMAT
                            )
                    );
                }
        );


        statusColumn.setCellValueFactory(
                data ->
                        new SimpleStringProperty(
                                safe(
                                        data.getValue()
                                                .getTaskStatus()
                                )
                        )
        );


        completedAtColumn.setCellValueFactory(
                data ->
                        new SimpleStringProperty(
                                formatDateTime(
                                        data.getValue()
                                                .getCompletedAt()
                                )
                        )
        );


        dailyTaskTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );
    }


    // =========================================================
    // LOAD TASKS
    // =========================================================

    private void loadTasks() {

        hideMessage();


        LocalDate date =
                taskDatePicker.getValue();


        if (date == null) {
            return;
        }


        Task<List<EmployeeDailyTask>> task =
                new Task<>() {

                    @Override
                    protected List<EmployeeDailyTask> call()
                            throws Exception {

                        return service.getTasks(
                                date
                        );
                    }
                };


        task.setOnSucceeded(
                event -> {

                    List<EmployeeDailyTask> loaded =
                            task.getValue();


                    allTasks.clear();


                    if (loaded != null) {

                        allTasks.addAll(
                                loaded
                        );
                    }


                    applyFilter();

                    updateSummary();
                }
        );


        task.setOnFailed(
                event -> {

                    Throwable error =
                            task.getException();


                    if (error != null) {
                        error.printStackTrace();
                    }


                    showMessage(
                            rootMessage(
                                    error
                            )
                    );
                }
        );


        Thread thread =
                new Thread(task);


        thread.setDaemon(true);

        thread.start();
    }


    // =========================================================
    // SUMMARY
    // =========================================================

    private void updateSummary() {

        int total =
                allTasks.size();


        long completed =
                allTasks.stream()
                        .filter(
                                task ->
                                        "Completed".equalsIgnoreCase(
                                                task.getTaskStatus()
                                        )
                        )
                        .count();


        long remaining =
                allTasks.stream()
                        .filter(
                                task ->
                                        !"Completed".equalsIgnoreCase(
                                                task.getTaskStatus()
                                        )
                                                &&
                                                !"Cancelled".equalsIgnoreCase(
                                                        task.getTaskStatus()
                                                )
                        )
                        .count();


        double percentage =
                total == 0
                        ? 0
                        : completed * 100.0 / total;


        totalLabel.setText(
                String.valueOf(total)
        );


        completedLabel.setText(
                String.valueOf(completed)
        );


        remainingLabel.setText(
                String.valueOf(remaining)
        );


        completionLabel.setText(
                String.format(
                        "%.0f%%",
                        percentage
                )
        );
    }


    // =========================================================
    // CREATE
    // =========================================================

    @FXML
    private void handleNewTask() {

        showTaskDialog(
                null
        );
    }


    // =========================================================
    // EDIT
    // =========================================================

    @FXML
    private void handleEdit() {

        EmployeeDailyTask selected =
                selectedTask();


        if (selected == null) {
            return;
        }


        showTaskDialog(
                selected
        );
    }


    // =========================================================
    // ADD / EDIT DIALOG
    // =========================================================

    private void showTaskDialog(
            EmployeeDailyTask existing
    ) {

        Dialog<ButtonType> dialog =
                new Dialog<>();


        dialog.setTitle(
                existing == null
                        ? "Add Daily Task"
                        : "Edit Daily Task"
        );


        dialog.getDialogPane()
                .getButtonTypes()
                .addAll(
                        ButtonType.OK,
                        ButtonType.CANCEL
                );


        TextField titleField =
                new TextField();


        titleField.setPromptText(
                "Example: Reply client email"
        );


        TextArea descriptionArea =
                new TextArea();


        descriptionArea.setPromptText(
                "Optional description"
        );


        descriptionArea.setPrefRowCount(3);


        ComboBox<String> categoryBox =
                new ComboBox<>();


        categoryBox.getItems().setAll(
                "Email",
                "Report",
                "Meeting",
                "Follow-up",
                "Documentation",
                "Development",
                "Other"
        );


        categoryBox.setValue(
                "Other"
        );


        ComboBox<String> priorityBox =
                new ComboBox<>();


        priorityBox.getItems().setAll(
                "Low",
                "Medium",
                "High"
        );


        priorityBox.setValue(
                "Medium"
        );


        DatePicker datePicker =
                new DatePicker(
                        taskDatePicker.getValue()
                );


        TextField dueTimeField =
                new TextField();


        dueTimeField.setPromptText(
                "HH:mm, example 17:30"
        );


        if (existing != null) {

            titleField.setText(
                    existing.getTaskTitle()
            );


            descriptionArea.setText(
                    existing.getTaskDescription()
            );


            categoryBox.setValue(
                    existing.getCategory()
            );


            priorityBox.setValue(
                    existing.getPriority()
            );


            datePicker.setValue(
                    existing.getTaskDate()
            );


            if (
                    existing.getDueTime() != null
            ) {

                dueTimeField.setText(
                        existing.getDueTime()
                                .format(
                                        TIME_FORMAT
                                )
                );
            }
        }


        GridPane grid =
                new GridPane();


        grid.setHgap(12);

        grid.setVgap(12);


        grid.add(
                new Label("Task Title *"),
                0,
                0
        );

        grid.add(
                titleField,
                1,
                0
        );


        grid.add(
                new Label("Description"),
                0,
                1
        );

        grid.add(
                descriptionArea,
                1,
                1
        );


        grid.add(
                new Label("Category *"),
                0,
                2
        );

        grid.add(
                categoryBox,
                1,
                2
        );


        grid.add(
                new Label("Priority *"),
                0,
                3
        );

        grid.add(
                priorityBox,
                1,
                3
        );


        grid.add(
                new Label("Task Date *"),
                0,
                4
        );

        grid.add(
                datePicker,
                1,
                4
        );


        grid.add(
                new Label("Due Time"),
                0,
                5
        );

        grid.add(
                dueTimeField,
                1,
                5
        );


        dialog.getDialogPane()
                .setContent(
                        grid
                );


        Optional<ButtonType> result =
                dialog.showAndWait();


        if (
                result.isEmpty()
                        ||
                        result.get()
                                != ButtonType.OK
        ) {

            return;
        }


        LocalTime dueTime = null;


        try {

            if (
                    !dueTimeField
                            .getText()
                            .isBlank()
            ) {

                dueTime =
                        LocalTime.parse(
                                dueTimeField
                                        .getText()
                                        .trim(),
                                TIME_FORMAT
                        );
            }

        } catch (Exception exception) {

            showAlert(
                    "Invalid Time",
                    "Please use HH:mm format. Example: 17:30."
            );

            return;
        }


        LocalTime finalDueTime =
                dueTime;


        Task<EmployeeDailyTask> saveTask =
                new Task<>() {

                    @Override
                    protected EmployeeDailyTask call()
                            throws Exception {

                        if (existing == null) {

                            return service.createTask(
                                    titleField.getText(),
                                    descriptionArea.getText(),
                                    categoryBox.getValue(),
                                    priorityBox.getValue(),
                                    datePicker.getValue(),
                                    finalDueTime
                            );
                        }


                        return service.updateTask(
                                existing,
                                titleField.getText(),
                                descriptionArea.getText(),
                                categoryBox.getValue(),
                                priorityBox.getValue(),
                                datePicker.getValue(),
                                finalDueTime
                        );
                    }
                };


        saveTask.setOnSucceeded(
                event -> {

                    taskDatePicker.setValue(
                            datePicker.getValue()
                    );


                    loadTasks();


                    showMessage(
                            existing == null
                                    ? "Daily task created successfully."
                                    : "Daily task updated successfully."
                    );
                }
        );


        saveTask.setOnFailed(
                event -> {

                    Throwable error =
                            saveTask.getException();


                    showAlert(
                            "Unable to Save",
                            rootMessage(
                                    error
                            )
                    );
                }
        );


        Thread thread =
                new Thread(
                        saveTask
                );


        thread.setDaemon(true);

        thread.start();
    }


    // =========================================================
    // STATUS ACTIONS
    // =========================================================

    @FXML
    private void handleStart() {

        EmployeeDailyTask task =
                selectedTask();


        if (task == null) {
            return;
        }


        changeStatus(
                task,
                "In Progress"
        );
    }


    @FXML
    private void handleComplete() {

        EmployeeDailyTask task =
                selectedTask();


        if (task == null) {
            return;
        }


        changeStatus(
                task,
                "Completed"
        );
    }


    @FXML
    private void handleReopen() {

        EmployeeDailyTask task =
                selectedTask();


        if (task == null) {
            return;
        }


        changeStatus(
                task,
                "Pending"
        );
    }


    @FXML
    private void handleCancelTask() {

        EmployeeDailyTask task =
                selectedTask();


        if (task == null) {
            return;
        }


        changeStatus(
                task,
                "Cancelled"
        );
    }


    private void changeStatus(
            EmployeeDailyTask task,
            String status
    ) {

        Task<EmployeeDailyTask> updateTask =
                new Task<>() {

                    @Override
                    protected EmployeeDailyTask call()
                            throws Exception {

                        return switch (status) {

                            case "Pending" ->
                                    service.markPending(
                                            task.getDailyTaskId()
                                    );

                            case "In Progress" ->
                                    service.markInProgress(
                                            task.getDailyTaskId()
                                    );

                            case "Completed" ->
                                    service.markCompleted(
                                            task.getDailyTaskId()
                                    );

                            case "Cancelled" ->
                                    service.cancelTask(
                                            task.getDailyTaskId()
                                    );

                            default ->
                                    throw new IllegalArgumentException(
                                            "Invalid status."
                                    );
                        };
                    }
                };


        updateTask.setOnSucceeded(
                event -> {

                    loadTasks();


                    showMessage(
                            "Task updated to "
                                    + status
                                    + "."
                    );
                }
        );


        updateTask.setOnFailed(
                event ->
                        showAlert(
                                "Update Failed",
                                rootMessage(
                                        updateTask.getException()
                                )
                        )
        );


        Thread thread =
                new Thread(
                        updateTask
                );


        thread.setDaemon(true);

        thread.start();
    }


    // =========================================================
    // DELETE
    // =========================================================

    @FXML
    private void handleDelete() {

        EmployeeDailyTask task =
                selectedTask();


        if (task == null) {
            return;
        }


        Alert confirmation =
                new Alert(
                        Alert.AlertType.CONFIRMATION
                );


        confirmation.setTitle(
                "Delete Daily Task"
        );


        confirmation.setHeaderText(
                "Delete \""
                        + task.getTaskTitle()
                        + "\"?"
        );


        confirmation.setContentText(
                "This action cannot be undone."
        );


        Optional<ButtonType> result =
                confirmation.showAndWait();


        if (
                result.isEmpty()
                        ||
                        result.get()
                                != ButtonType.OK
        ) {

            return;
        }


        Task<Void> deleteTask =
                new Task<>() {

                    @Override
                    protected Void call()
                            throws Exception {

                        service.deleteTask(
                                task.getDailyTaskId()
                        );


                        return null;
                    }
                };


        deleteTask.setOnSucceeded(
                event -> {

                    loadTasks();


                    showMessage(
                            "Daily task deleted successfully."
                    );
                }
        );


        deleteTask.setOnFailed(
                event ->
                        showAlert(
                                "Delete Failed",
                                rootMessage(
                                        deleteTask.getException()
                                )
                        )
        );


        Thread thread =
                new Thread(
                        deleteTask
                );


        thread.setDaemon(true);

        thread.start();
    }


    // =========================================================
    // DATE NAVIGATION
    // =========================================================

    @FXML
    private void handlePreviousDay() {

        LocalDate current =
                taskDatePicker.getValue();


        if (current == null) {

            current = LocalDate.now();
        }


        taskDatePicker.setValue(
                current.minusDays(1)
        );
    }


    @FXML
    private void handleNextDay() {

        LocalDate current =
                taskDatePicker.getValue();


        if (current == null) {

            current = LocalDate.now();
        }


        taskDatePicker.setValue(
                current.plusDays(1)
        );
    }


    @FXML
    private void handleToday() {

        taskDatePicker.setValue(
                LocalDate.now()
        );
    }


    @FXML
    private void handleRefresh() {

        loadTasks();
    }


    // =========================================================
    // HELPERS
    // =========================================================

    private EmployeeDailyTask selectedTask() {

        EmployeeDailyTask task =
                dailyTaskTable
                        .getSelectionModel()
                        .getSelectedItem();


        if (task == null) {

            showMessage(
                    "Please select a daily task first."
            );
        }


        return task;
    }


    private String safe(
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

        if (value == null) {
            return "-";
        }


        return value
                .atZoneSameInstant(
                        MALAYSIA_ZONE
                )
                .format(
                        DATE_TIME_FORMAT
                );
    }


    private String rootMessage(
            Throwable error
    ) {

        if (error == null) {

            return "Unexpected error.";
        }


        Throwable cause =
                error;


        while (
                cause.getCause() != null
        ) {

            cause =
                    cause.getCause();
        }


        String message =
                cause.getMessage();


        return message == null
                || message.isBlank()
                ? "Unexpected error."
                : message;
    }


    private void showAlert(
            String title,
            String message
    ) {

        Alert alert =
                new Alert(
                        Alert.AlertType.ERROR
                );


        alert.setTitle(
                title
        );


        alert.setHeaderText(
                null
        );


        alert.setContentText(
                message
        );


        alert.showAndWait();
    }


    private void showMessage(
            String message
    ) {

        messageLabel.setText(
                message
        );


        messageLabel.setVisible(
                true
        );


        messageLabel.setManaged(
                true
        );
    }


    private void hideMessage() {

        messageLabel.setText("");

        messageLabel.setVisible(false);

        messageLabel.setManaged(false);
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
                "/fxml/employee/employeeTask.fxml"
        );
    }


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
    private void handleAttendance(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/employee/employeeAttendance.fxml"
        );
    }


    @FXML
    private void handlePerformance(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/employee/employeePerformance.fxml"
        );
    }


    @FXML
    private void handleReports(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/employee/employeeReport.fxml"
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