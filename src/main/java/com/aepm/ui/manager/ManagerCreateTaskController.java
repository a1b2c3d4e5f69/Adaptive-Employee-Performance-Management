package com.aepm.ui.manager;

import com.aepm.logic.task.TaskCategoryService;
import com.aepm.logic.task.TaskService;
import com.aepm.logic.task.TeamTaskService;
import com.aepm.logic.user.UserService;

import com.aepm.model.Role;
import com.aepm.model.TaskCategory;
import com.aepm.model.User;

import javafx.util.StringConverter;
import java.time.format.DateTimeFormatter;

import com.aepm.ui.components.UIHelper;
import com.aepm.ui.navigation.NavigationManager;

import com.aepm.util.SessionManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javafx.concurrent.Task;

import javafx.event.ActionEvent;

import javafx.fxml.FXML;

import javafx.scene.Node;

import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import javafx.stage.Stage;


import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javafx.scene.layout.VBox;
import com.aepm.data.task.TaskAttachmentRepository;

import javafx.stage.FileChooser;

import javafx.scene.layout.HBox;

import java.io.File;


public class ManagerCreateTaskController {


    // =========================================================
    // TOP BAR
    // =========================================================

    @FXML
    private Label topRoleLabel;

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label topNameLabel;


    // =========================================================
    // FORM
    // =========================================================

    @FXML
    private TextField titleField;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private ComboBox<User> employeeComboBox;

    @FXML
    private ComboBox<TaskCategory> categoryComboBox;

    @FXML
    private ComboBox<String> priorityComboBox;

    @FXML
    private ComboBox<String> difficultyComboBox;

    @FXML
    private DatePicker dueDatePicker;

    @FXML
    private TextArea completionCriteriaArea;


    // =========================================================
    // MESSAGE / BUTTON
    // =========================================================

    @FXML
    private Label messageLabel;

    @FXML
    private Button createButton;

    @FXML
    private VBox selectedEmployeesBox;

    @FXML
     private HBox attachmentPreviewBox;

     @FXML
     private Label attachmentNameLabel;


     private File selectedAttachmentFile;


     private final TaskAttachmentRepository
            attachmentRepository =
            new TaskAttachmentRepository();


    // =========================================================
    // SERVICES
    // =========================================================

    private final TaskService taskService =
            new TaskService();

    private final TeamTaskService teamTaskService =
            new TeamTaskService();

    private final TaskCategoryService categoryService =
            new TaskCategoryService();

    private final UserService userService =
            new UserService();


    // =========================================================
    // MULTIPLE EMPLOYEE SELECTION
    // =========================================================

    private final ObservableList<User> selectedEmployees =
            FXCollections.observableArrayList();


    private static final ZoneId MALAYSIA_ZONE =
            ZoneId.of(
                    "Asia/Kuala_Lumpur"
        );

    private static final DateTimeFormatter DATE_PICKER_FORMATTER =
        DateTimeFormatter.ofPattern(
         "dd/MM/yyyy"
    );

    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {

        UIHelper.hideMessage(
                messageLabel
        );

        updateManagerRoleLabel();

        loadCurrentUser();

        configurePriority();

        configureDifficulty();

        configureEmployeeComboBox();

        configureCategoryComboBox();

        configureDueDatePicker();

        loadEmployees();

        loadCategories();

        dueDatePicker.setValue(
                LocalDate.now()
                        .plusDays(1)
        );

       hideAttachmentPreview();

        renderSelectedEmployees();
    }

    private void configureDueDatePicker() {

        dueDatePicker.setConverter(new StringConverter<LocalDate>() {

         @Override
         public String toString(LocalDate date) {

                if (date == null) {
                        return "";
                }

                return DATE_PICKER_FORMATTER.format(
                        date
                 );
                }


                        @Override
                        public LocalDate fromString(
                                String text
                        ) {

                        if (
                                text == null
                                        || text.isBlank()
                        ) {

                                return null;
                        }

                        return LocalDate.parse(
                                text.trim(),
                                DATE_PICKER_FORMATTER
                        );
                        }
                }
        );


        dueDatePicker.setPromptText(
                "DD/MM/YYYY"
        );
        }


    // =========================================================
    // CURRENT MANAGER
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
                    "Manager";
        }


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
    }


    private void updateManagerRoleLabel() {

        if (topRoleLabel == null) {
            return;
        }


        User user =
                SessionManager.getCurrentUser();


        if (user == null) {

            topRoleLabel.setText(
                    "MANAGER"
            );

            return;
        }


        String employeeId =
                user.getEmployeeId();


        if (
                employeeId == null
                        || employeeId.isBlank()
        ) {

            topRoleLabel.setText(
                    "MANAGER"
            );

        } else {

            topRoleLabel.setText(
                    "MANAGER  •  "
                            + employeeId
            );
        }
    }


    // =========================================================
    // PRIORITY
    // =========================================================

    private void configurePriority() {

        priorityComboBox.setItems(
                FXCollections.observableArrayList(
                        "Low",
                        "Medium",
                        "High",
                        "Urgent"
                )
        );

        priorityComboBox.setValue(
                "Medium"
        );
    }


    // =========================================================
    // DIFFICULTY
    // =========================================================

    private void configureDifficulty() {

        difficultyComboBox.setItems(
                FXCollections.observableArrayList(
                        "Easy",
                        "Medium",
                        "Hard",
                        "Advanced"
                )
        );

        difficultyComboBox.setValue(
                "Medium"
        );
    }


    // =========================================================
    // EMPLOYEE COMBOBOX
    // MULTI-SELECT WITHOUT CHANGING FXML
    // =========================================================

    private void configureEmployeeComboBox() {

        employeeComboBox.setPromptText(
                "Select employee(s)"
        );


        employeeComboBox.setConverter(
                new StringConverter<>() {

                    @Override
                    public String toString(
                            User user
                    ) {

                        return formatEmployee(
                                user
                        );
                    }


                    @Override
                    public User fromString(
                            String value
                    ) {

                        return null;
                    }
                }
        );


        /*
         * Dropdown list.
         *
         * ✓ = already selected
         */
        employeeComboBox.setCellFactory(
                listView ->
                        new ListCell<>() {

                            @Override
                            protected void updateItem(
                                    User user,
                                    boolean empty
                            ) {

                                super.updateItem(
                                        user,
                                        empty
                                );


                                if (
                                        empty
                                                || user == null
                                ) {

                                    setText(
                                            null
                                    );

                                    return;
                                }


                                boolean selected =
                                        containsEmployee(
                                                user
                                        );


                                setText(
                                        (
                                                selected
                                                        ? "✓  "
                                                        : "    "
                                        )
                                                + formatEmployee(
                                                        user
                                                )
                                );
                            }
                        }
        );


        /*
         * This controls what is displayed inside the ComboBox
         * after employees have been selected.
         */
        employeeComboBox.setButtonCell(
                new ListCell<>() {

                    @Override
                    protected void updateItem(
                            User item,
                            boolean empty
                    ) {

                        super.updateItem(
                                item,
                                empty
                        );


                        if (
                                selectedEmployees.isEmpty()
                        ) {

                            setText(
                                    "Select employee(s)"
                            );

                            return;
                        }


                        if (
                                selectedEmployees.size() == 1
                        ) {

                            setText(
                                    formatEmployee(
                                            selectedEmployees.get(
                                                    0
                                            )
                                    )
                            );

                            return;
                        }


                        setText(
                                selectedEmployees.size()
                                        + " employees selected"
                        );
                    }
                }
        );


        /*
         * Selecting the same employee again removes them.
         *
         * This gives us multi-select behaviour while keeping
         * exactly the same ComboBox in the existing FXML.
         */
        employeeComboBox.setOnAction(
                event -> {

                    User selected =
                            employeeComboBox
                                    .getSelectionModel()
                                    .getSelectedItem();


                    if (selected == null) {
                        return;
                    }


                    toggleEmployee(
                            selected
                    );


                    employeeComboBox
                            .getSelectionModel()
                            .clearSelection();


                    refreshEmployeeDisplay();
                }
        );
    }


    private void toggleEmployee(
            User user
    ) {

        User existing =
                findSelectedEmployee(
                        user
                );


        if (existing != null) {

            selectedEmployees.remove(
                    existing
            );

        } else {

            selectedEmployees.add(
                    user
            );
        }
    }


    private User findSelectedEmployee(
            User user
    ) {

        if (
                user == null
                        || user.getUserId() == null
        ) {

            return null;
        }


        for (
                User selected
                : selectedEmployees
        ) {

            if (
                    selected != null
                            && selected.getUserId() != null
                            && selected
                            .getUserId()
                            .equals(
                                    user.getUserId()
                            )
            ) {

                return selected;
            }
        }


        return null;
    }


    private boolean containsEmployee(
            User user
    ) {

        return findSelectedEmployee(
                user
        ) != null;
    }


    private void refreshEmployeeDisplay() {

        employeeComboBox.requestLayout();

        renderSelectedEmployees();
    }

    private void hideAttachmentPreview() {

        selectedAttachmentFile = null;

        if (attachmentNameLabel != null) {
            attachmentNameLabel.setText("");
        }

        if (attachmentPreviewBox != null) {
            attachmentPreviewBox.setVisible(false);
            attachmentPreviewBox.setManaged(false);
        }
    }

    private void renderSelectedEmployees() {

        if (selectedEmployeesBox == null) {
                return;
        }


        selectedEmployeesBox
                .getChildren()
                .clear();


        if (selectedEmployees.isEmpty()) {

                selectedEmployeesBox.setVisible(
                        false
                );

                selectedEmployeesBox.setManaged(
                        false
                );

                return;
        }


        selectedEmployeesBox.setVisible(
                true
        );

        selectedEmployeesBox.setManaged(
                true
        );


        Label title =
                new Label(
                        "Selected Employees"
                );

        title.getStyleClass().add(
                "manager-selected-employees-title"
        );


        selectedEmployeesBox
                .getChildren()
                .add(
                        title
                );


        for (
                User user
                : selectedEmployees
        ) {

                Label employee =
                        new Label(
                                "•  "
                                        + formatEmployee(
                                                user
                                        )
                        );

                employee.getStyleClass().add(
                        "manager-selected-employee-item"
                );


                selectedEmployeesBox
                        .getChildren()
                        .add(
                                employee
                        );
        }
     }


    private String formatEmployee(
            User user
    ) {

        if (user == null) {
            return "";
        }


        String staffId =
                user.getEmployeeId();

        if (
                staffId == null
                        || staffId.isBlank()
        ) {

            staffId =
                    "-";
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
                    "-";
        }


        return staffId
                + " - "
                + name;
    }


    // =========================================================
    // CATEGORY COMBOBOX
    // =========================================================

    private void configureCategoryComboBox() {

        categoryComboBox.setConverter(
                new StringConverter<>() {

                    @Override
                    public String toString(
                            TaskCategory category
                    ) {

                        if (category == null) {
                            return "";
                        }

                        return category
                                .getCategoryName();
                    }


                    @Override
                    public TaskCategory fromString(
                            String value
                    ) {

                        return null;
                    }
                }
        );
    }


    // =========================================================
    // LOAD EMPLOYEES
    // =========================================================

    private void loadEmployees() {

        employeeComboBox.setDisable(
                true
        );


        Task<List<User>> worker =
                new Task<>() {

                    @Override
                    protected List<User> call()
                            throws Exception {

                        return userService
                                .getAllUsers();
                    }
                };


        worker.setOnSucceeded(
                event -> {

                    employeeComboBox
                            .getItems()
                            .clear();


                    List<User> users =
                            worker.getValue();


                    if (users != null) {

                        for (User user : users) {

                            if (user == null) {
                                continue;
                            }


                            boolean employee =
                                    user.getRole() == Role.EMPLOYEE
                                            || user.getRoleId() == 3;


                            boolean active =
                                    "Active".equalsIgnoreCase(
                                            user.getAccountStatus()
                                    );


                            if (
                                    employee
                                            && active
                            ) {

                                employeeComboBox
                                        .getItems()
                                        .add(
                                                user
                                        );
                            }
                        }
                    }


                    selectedEmployees.clear();

                    employeeComboBox.setDisable(
                            false
                    );


                    refreshEmployeeDisplay();
                }
        );


        worker.setOnFailed(
                event -> {

                    employeeComboBox.setDisable(
                            false
                    );


                    Throwable error =
                            worker.getException();


                    UIHelper.showError(
                            messageLabel,
                            error == null
                                    ? "Unable to load employees."
                                    : error.getMessage()
                    );
                }
        );


        Thread thread =
                new Thread(
                        worker
                );

        thread.setDaemon(
                true
        );

        thread.start();
    }


    // =========================================================
    // LOAD CATEGORIES
    // =========================================================

    private void loadCategories() {

        categoryComboBox.setDisable(
                true
        );


        Task<List<TaskCategory>> worker =
                new Task<>() {

                    @Override
                    protected List<TaskCategory> call()
                            throws Exception {

                        return categoryService
                                .getActiveCategories();
                    }
                };


        worker.setOnSucceeded(
                event -> {

                    List<TaskCategory> categories =
                            worker.getValue();


                    if (categories == null) {

                        categoryComboBox
                                .getItems()
                                .clear();

                    } else {

                        categoryComboBox
                                .getItems()
                                .setAll(
                                        categories
                                );
                    }


                    categoryComboBox.setDisable(
                            false
                    );


                    if (
                            !categoryComboBox
                                    .getItems()
                                    .isEmpty()
                    ) {

                        categoryComboBox
                                .getSelectionModel()
                                .selectFirst();
                    }
                }
        );


        worker.setOnFailed(
                event -> {

                    categoryComboBox.setDisable(
                            false
                    );


                    Throwable error =
                            worker.getException();


                    UIHelper.showError(
                            messageLabel,
                            error == null
                                    ? "Unable to load task categories."
                                    : error.getMessage()
                    );
                }
        );


        Thread thread =
                new Thread(
                        worker
                );

        thread.setDaemon(
                true
        );

        thread.start();
    }


    // =========================================================
    // CREATE TASK
    // =========================================================

    @FXML
    private void handleCreateTask(
            ActionEvent event
    ) {

        UIHelper.hideMessage(
                messageLabel
        );


        String title =
                titleField
                        .getText()
                        .trim();


        String description =
                descriptionArea
                        .getText();


        TaskCategory category =
                categoryComboBox
                        .getValue();


        String priority =
                priorityComboBox
                        .getValue();


        String difficulty =
                difficultyComboBox
                        .getValue();


        LocalDate dueDate =
                dueDatePicker
                        .getValue();


        String completionCriteria =
                completionCriteriaArea
                        .getText();


        // -----------------------------------------------------
        // VALIDATION
        // -----------------------------------------------------

        if (title.isBlank()) {

            UIHelper.showError(
                    messageLabel,
                    "Task title is required."
            );

            return;
        }


        if (
                selectedEmployees.isEmpty()
        ) {

            UIHelper.showError(
                    messageLabel,
                    "Please select at least one employee."
            );

            return;
        }


        if (category == null) {

            UIHelper.showError(
                    messageLabel,
                    "Please select a task category."
            );

            return;
        }


        if (dueDate == null) {

            UIHelper.showError(
                    messageLabel,
                    "Due date is required."
            );

            return;
        }


        /*
         * Same behaviour as your current tasks:
         * due date = 11:59 PM Malaysia time.
         */
        OffsetDateTime dueDateTime =
                dueDate
                        .atTime(
                                LocalTime.of(
                                        23,
                                        59
                                )
                        )
                        .atZone(
                                MALAYSIA_ZONE
                        )
                        .toOffsetDateTime();


        if (
                dueDateTime.isBefore(
                        OffsetDateTime.now()
                )
        ) {

            UIHelper.showError(
                    messageLabel,
                    "Due date cannot be in the past."
            );

            return;
        }


        createButton.setDisable(
                true
        );


        List<User> selectedSnapshot =
                new ArrayList<>(
                        selectedEmployees
                );


        Task<UUID> worker =
                new Task<>() {

                    @Override
                    protected UUID call()
                            throws Exception {

                        /*
                         * =================================================
                         * ONE EMPLOYEE
                         * Original individual task behaviour.
                         * =================================================
                         */
                        if (
                                selectedSnapshot.size() == 1
                        ) {

                            User employee =
                                    selectedSnapshot.get(
                                            0
                                    );


                            UUID taskId =
                        taskService.createTask(
                                title,
                                description,
                                category.getCategoryId(),
                                UUID.fromString(
                                        employee.getUserId()
                                ),
                                priority,
                                difficulty,
                                dueDateTime,
                                completionCriteria
                        );


                uploadAttachmentIfPresent(
                        taskId
                );


                return taskId;
                        }


                        /*
                         * =================================================
                         * MULTIPLE EMPLOYEES
                         * New Team Task.
                         * =================================================
                         */

                        List<UUID> employeeIds =
                                selectedSnapshot
                                        .stream()
                                        .map(
                                                user ->
                                                        UUID.fromString(
                                                                user.getUserId()
                                                        )
                                        )
                                        .toList();


                UUID taskId =
                        teamTaskService.createTeamTask(
                                title,
                                description,
                                category.getCategoryId(),
                                employeeIds,
                                priority,
                                difficulty,
                                dueDateTime,
                                completionCriteria
                        );


                uploadAttachmentIfPresent(
                        taskId
                );


                return taskId;
                    }
                };


        worker.setOnSucceeded(
                taskEvent -> {

                    createButton.setDisable(
                            false
                    );


                    String type =
                            selectedSnapshot.size() > 1
                                    ? "Team task"
                                    : "Task";


                    UIHelper.showSuccess(
                            messageLabel,
                            type
                                    + " created successfully for "
                                    + selectedSnapshot.size()
                                    + " employee"
                                    + (
                                    selectedSnapshot.size() > 1
                                            ? "s."
                                            : "."
                            )
                    );


                    clearForm();
                }
        );


        worker.setOnFailed(
                taskEvent -> {

                    createButton.setDisable(
                            false
                    );


                    Throwable error =
                            worker.getException();


                    if (error != null) {

                        error.printStackTrace();
                    }


                    UIHelper.showError(
                            messageLabel,
                            error == null
                                    ? "Unable to create task."
                                    : error.getMessage()
                    );
                }
        );


        Thread thread =
                new Thread(
                        worker
                );

        thread.setDaemon(
                true
        );

        thread.start();
    }


    // =========================================================
    // CLEAR FORM
    // =========================================================

    private void clearForm() {

        titleField.clear();

        descriptionArea.clear();

        completionCriteriaArea.clear();

        selectedEmployees.clear();


        employeeComboBox
                .getSelectionModel()
                .clearSelection();


        priorityComboBox.setValue(
                "Medium"
        );


        difficultyComboBox.setValue(
                "Medium"
        );


        dueDatePicker.setValue(
                LocalDate.now()
                        .plusDays(1)
        );


        if (
                !categoryComboBox
                        .getItems()
                        .isEmpty()
        ) {

            categoryComboBox
                    .getSelectionModel()
                    .selectFirst();
        }


        refreshEmployeeDisplay();

        selectedAttachmentFile =null;

        if (attachmentNameLabel != null) {

        attachmentNameLabel.setText(
                ""
        );
        }

        if (attachmentPreviewBox != null) {

        attachmentPreviewBox.setVisible(
                false
        );

        attachmentPreviewBox.setManaged(
                false
        );
        }
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
                "/fxml/manager/managerTask.fxml"
        );
    }


    @FXML
    private void handleHome(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/manager/managerHome.fxml"
        );
    }


    @FXML
    private void handleProfile(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/manager/managerProfile.fxml"
        );
    }


    @FXML
    private void handleTaskManagement(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/manager/managerTask.fxml"
        );
    }


    @FXML
    private void handleDeadlineEscalation(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/manager/managerDeadlineEscalation.fxml"
        );
    }


    @FXML
    private void handlePerformance(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/manager/managerPerformance.fxml"
        );
    }


    @FXML
    private void handleReports(
            ActionEvent event
    ) {

        Stage stage =
                getStage(
                        event
                );


        /*
         * Your current Report page is programmatic.
         * If you are still using ManagerReportsPage,
         * keep your existing opening code here.
         *
         * If managerReport.fxml is currently active,
         * this navigation works directly.
         */

        try {

            NavigationManager.navigate(
                    stage,
                    "/fxml/manager/managerReport.fxml"
            );

        } catch (Exception exception) {

            exception.printStackTrace();
        }
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
    // NAVIGATION HELPER
    // =========================================================

    private void navigate(
            ActionEvent event,
            String path
    ) {

        Stage stage =
                getStage(
                        event
                );

        NavigationManager.navigate(
                stage,
                path
        );
    }


    private Stage getStage(
            ActionEvent event
    ) {

        return (Stage) (
                (Node) event.getSource()
        )
                .getScene()
                .getWindow();
    }

    @FXML
        private void handleChooseAttachment() {

        FileChooser chooser =
                new FileChooser();


        chooser.setTitle(
                "Select Task Attachment"
        );


        chooser.getExtensionFilters().addAll(

                new FileChooser.ExtensionFilter(
                        "Supported Files",
                        "*.pdf",
                        "*.doc",
                        "*.docx",
                        "*.xls",
                        "*.xlsx",
                        "*.ppt",
                        "*.pptx",
                        "*.txt",
                        "*.csv",
                        "*.png",
                        "*.jpg",
                        "*.jpeg",
                        "*.zip"
                ),

                new FileChooser.ExtensionFilter(
                        "All Files",
                        "*.*"
                )
        );


        File file =
                chooser.showOpenDialog(
                        createButton
                                .getScene()
                                .getWindow()
                );


        if (file == null) {
                return;
        }


        long maxSize =
                10L * 1024L * 1024L;


        if (file.length() > maxSize) {

                UIHelper.showError(
                        messageLabel,
                        "The attachment cannot exceed 10 MB."
                );

                return;
        }


        selectedAttachmentFile =
                file;


        attachmentNameLabel.setText(
                file.getName()
                        + "  ("
                        + formatFileSize(
                                file.length()
                        )
                        + ")"
        );


        attachmentPreviewBox.setVisible(
                true
        );

        attachmentPreviewBox.setManaged(
                true
        );
        }

        @FXML
        private void handleRemoveAttachment() {

        selectedAttachmentFile =
                null;


        attachmentNameLabel.setText(
                ""
        );


        attachmentPreviewBox.setVisible(
                false
        );

        attachmentPreviewBox.setManaged(
                false
        );
     }

        private String formatFileSize(long bytes) {

        if (bytes < 1024) {

                return bytes
                        + " B";
        }


        if (
                bytes
                        < 1024L
                        * 1024L
        ) {

                return String.format(
                        "%.1f KB",
                        bytes / 1024.0
                );
        }


        return String.format(
                "%.1f MB",
                bytes
                        / (
                        1024.0
                                * 1024.0
                )
        );
        }

        private void uploadAttachmentIfPresent(
                UUID taskId
        ) throws Exception {

        if (
                selectedAttachmentFile == null
        ) {

                return;
        }


        User current =
                SessionManager
                        .getCurrentUser();


        if (current == null) {

                throw new IllegalStateException(
                        "No logged-in manager found."
                );
        }


        String token =
                SessionManager
                        .getAccessToken();


        attachmentRepository.upload(
                taskId,
                UUID.fromString(
                        current.getUserId()
                ),
                selectedAttachmentFile,
                token
        );
        }
}