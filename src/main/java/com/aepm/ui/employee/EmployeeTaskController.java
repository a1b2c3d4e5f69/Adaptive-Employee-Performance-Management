package com.aepm.ui.employee;

import com.aepm.logic.task.TaskCategoryService;
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
import javafx.fxml.FXMLLoader;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;

import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import javafx.scene.control.cell.PropertyValueFactory;

import javafx.stage.Modality;
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
    private Label topNameLabel;

    @FXML
    private Label topRoleLabel;


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
    // MESSAGE
    // =========================================================

    @FXML
    private Label messageLabel;


    // =========================================================
    // SERVICES
    // =========================================================

    private final TaskService taskService =
            new TaskService();

    private final TaskCategoryService categoryService =
            new TaskCategoryService();


    // =========================================================
    // DATA
    // =========================================================

    private final ObservableList<Task> allTasks =
            FXCollections.observableArrayList();

    private FilteredList<Task> filteredTasks;


    private final Map<Long, String> categoryDisplayMap =
            new HashMap<>();


    private static final ZoneId MALAYSIA_ZONE =
            ZoneId.of(
                    "Asia/Kuala_Lumpur"
            );


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

        setupTaskOpening();

        loadCategoriesAndTasks();
    }


    // =========================================================
    // CURRENT USER
    // =========================================================

   private void loadCurrentUser() {

        User user =
                SessionManager.getCurrentUser();


        if (user == null) {

                topNameLabel.setText(
                        "Employee"
                );

                topRoleLabel.setText(
                        "EMPLOYEE"
                );

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
                        "Employee";
        }


        String employeeId =
                user.getEmployeeId();


        topNameLabel.setText(
                name
        );


        if (
                employeeId != null
                        && !employeeId.isBlank()
        ) {

                topRoleLabel.setText(
                        "EMPLOYEE  •  "
                                + employeeId
                );

        } else {

                topRoleLabel.setText(
                        "EMPLOYEE"
                );
        }
        }

    // =========================================================
    // TABLE SETUP
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
        // PROGRESS
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
                                        empty
                                                || value == null
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
        // DATE
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

                                    setText(
                                            "-"
                                    );

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
    // CLICK TASK -> POPUP
    // =========================================================

    private void setupTaskOpening() {

        taskTable.setRowFactory(
                table -> {

                    TableRow<Task> row =
                            new TableRow<>();


                    row.setOnMouseClicked(
                            event -> {

                                if (
                                        !row.isEmpty()
                                                && event.getClickCount() == 1
                                ) {

                                    Task task =
                                            row.getItem();


                                    openTaskPopup(
                                            task
                                    );
                                }
                            }
                    );


                    return row;
                }
        );
    }


    // =========================================================
    // OPEN POPUP
    // =========================================================

    private void openTaskPopup(
            Task task
    ) {

        if (task == null) {
            return;
        }


        try {

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/fxml/employee/employeeTaskDetail.fxml"
                            )
                    );


            Parent root =
                    loader.load();


            EmployeeTaskDetailController controller =
                    loader.getController();


            String categoryName =
                    task.getCategoryId() == null
                            ? "-"
                            : categoryDisplayMap.getOrDefault(
                                    task.getCategoryId(),
                                    "-"
                            );


            Stage popupStage =
                    new Stage();


            controller.setTask(
                    task,
                    categoryName,
                    popupStage,
                    this::loadCategoriesAndTasks
            );


            Scene scene =
                    new Scene(
                            root
                    );


            String css =
                    getClass()
                            .getResource(
                                    "/css/app.css"
                            )
                            .toExternalForm();


            scene.getStylesheets()
                    .add(
                            css
                    );


            popupStage.setScene(
                    scene
            );


            popupStage.setTitle(
                    "Task Details"
            );


            popupStage.initModality(
                    Modality.WINDOW_MODAL
            );


            if (
                    taskTable.getScene() != null
                            && taskTable.getScene().getWindow() != null
            ) {

                popupStage.initOwner(
                        taskTable
                                .getScene()
                                .getWindow()
                );
            }


            popupStage.setResizable(
                    false
            );


            popupStage.showAndWait();


        } catch (Exception e) {

            e.printStackTrace();


            showMessage(
                    "Unable to open task details: "
                            + e.getMessage()
            );
        }
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
                                    selectedDate.equals(
                                            taskDueDate
                                    );
                        }
                    }


                    return matchesName
                            && matchesDate;
                }
        );
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
                event ->
                        loadTasks()
        );


        Thread thread =
                new Thread(
                        categoryTask
                );


        thread.setDaemon(
                true
        );


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


                    if (error != null) {
                        error.printStackTrace();
                    }


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


        thread.setDaemon(
                true
        );


        thread.start();
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

        // Current page
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
    private void handleReports() {

        System.out.println(
                "Reports not implemented yet."
        );
    }


    @FXML
    private void handleAnonymousReport() {

        System.out.println(
                "Anonymous Reporting not implemented yet."
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