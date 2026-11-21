package com.aepm.ui.manager;

import com.aepm.logic.task.TeamTaskService;
import com.aepm.model.Task;
import com.aepm.model.TaskAssignment;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class ManagerTaskDetailsDialog {

    private final TeamTaskService teamTaskService = new TeamTaskService();

    private static final ZoneId MALAYSIA_ZONE = ZoneId.of("Asia/Kuala_Lumpur");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public void show(
            Window owner,
            Task task,
            String categoryName,
            Map<String, String> employeeDisplayMap,
            Runnable editAction,
            Runnable cancelAction
    ) {
        if (task == null) return;

        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        if (owner != null) dialog.initOwner(owner);
        dialog.setTitle("Task Details");

        Label pageTitle = new Label(
                task.getTaskTitle() == null || task.getTaskTitle().isBlank()
                        ? "Task Details"
                        : task.getTaskTitle()
        );
        pageTitle.getStyleClass().add("manager-task-popup-title");

        Label subtitle = new Label(
                task.getAssignedEmployeeId() == null ? "Team Task" : "Individual Task"
        );
        subtitle.getStyleClass().add("manager-task-popup-subtitle");

        VBox heading = new VBox(4, pageTitle, subtitle);

        GridPane details = new GridPane();
        details.setHgap(28);
        details.setVgap(16);
        details.getStyleClass().add("manager-task-popup-card");

        addDetailRow(details, 0, "Category", safe(categoryName));
        addDetailRow(details, 1, "Priority", safe(task.getPriority()));
        addDetailRow(details, 2, "Difficulty", safe(task.getDifficultyLevel()));
        addDetailRow(details, 3, "Status", safe(task.getTaskStatus()));
        addDetailRow(details, 4, "Progress", String.format("%.0f%%", task.getProgress()));
        addDetailRow(
                details,
                5,
                task.getAssignedEmployeeId() == null ? "Shared Task Due Date" : "Due Date",
                task.getDueDate() == null
                        ? "-"
                        : task.getDueDate().atZoneSameInstant(MALAYSIA_ZONE).format(DATE_FORMATTER)
        );

        Label employeeSectionTitle = new Label("Assigned Employee(s)");
        employeeSectionTitle.getStyleClass().add("manager-task-popup-section-title");

        VBox employeeBox = new VBox(7);
        employeeBox.getStyleClass().add("manager-task-popup-employee-box");

        if (task.getAssignedEmployeeId() != null) {
            String userId = task.getAssignedEmployeeId().toString();
            addEmployeeLabel(employeeBox, employeeDisplayMap.getOrDefault(userId, userId));
        } else {
            Label loadingLabel = new Label("Loading team members...");
            loadingLabel.getStyleClass().add("manager-task-popup-employee");
            employeeBox.getChildren().add(loadingLabel);
            loadTeamMembers(task, employeeBox, employeeDisplayMap);
        }

        Label descriptionTitle = new Label("Description");
        descriptionTitle.getStyleClass().add("manager-task-popup-section-title");

        Label description = new Label(safe(task.getTaskDescription()));
        description.setWrapText(true);
        description.getStyleClass().add("manager-task-popup-text");

        Label criteriaTitle = new Label("Completion Criteria");
        criteriaTitle.getStyleClass().add("manager-task-popup-section-title");

        Label criteria = new Label(safe(task.getCompletionCriteria()));
        criteria.setWrapText(true);
        criteria.getStyleClass().add("manager-task-popup-text");

        VBox content = new VBox(
                20,
                heading,
                details,
                employeeSectionTitle,
                employeeBox,
                descriptionTitle,
                description,
                criteriaTitle,
                criteria
        );
        content.setPadding(new Insets(28));

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("manager-task-popup-scroll");

        Button editButton = new Button("Edit Task");
        editButton.getStyleClass().add("manager-button");
        editButton.setOnAction(event -> {
            dialog.close();
            if (editAction != null) editAction.run();
        });

        Button cancelButton = new Button("Cancel Task");
        cancelButton.getStyleClass().add("manager-danger-button");
        cancelButton.setOnAction(event -> {
            dialog.close();
            if (cancelAction != null) cancelAction.run();
        });

        Button closeButton = new Button("Close");
        closeButton.getStyleClass().add("manager-button-primary");
        closeButton.setOnAction(event -> dialog.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox footer = new HBox(10, editButton, cancelButton, spacer, closeButton);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(16, 28, 20, 28));
        footer.getStyleClass().add("manager-task-popup-footer");

        VBox root = new VBox(scrollPane, footer);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        root.getStyleClass().add("manager-root");

        Scene scene = new Scene(root, 760, 680);

        var appCss = getClass().getResource("/css/app.css");
        if (appCss != null) scene.getStylesheets().add(appCss.toExternalForm());

        var managerCss = getClass().getResource("/css/manager.css");
        if (managerCss != null) scene.getStylesheets().add(managerCss.toExternalForm());

        dialog.setScene(scene);
        dialog.setMinWidth(680);
        dialog.setMinHeight(560);
        dialog.showAndWait();
    }

    private void loadTeamMembers(
            Task task,
            VBox employeeBox,
            Map<String, String> employeeDisplayMap
    ) {
        javafx.concurrent.Task<List<TaskAssignment>> worker =
                new javafx.concurrent.Task<>() {
            @Override
            protected List<TaskAssignment> call() throws Exception {
                return teamTaskService.getAssignments(task.getTaskId());
            }
        };

        worker.setOnSucceeded(event -> {
            employeeBox.getChildren().clear();
            List<TaskAssignment> assignments = worker.getValue();

            if (assignments == null || assignments.isEmpty()) {
                addEmployeeLabel(employeeBox, "No assigned employees found.");
                return;
            }

            for (TaskAssignment assignment : assignments) {
                if (assignment == null || assignment.getEmployeeId() == null) continue;
                String userId = assignment.getEmployeeId().toString();
                String deadline = assignment.getDueDate() == null ? "-"
                        : assignment.getDueDate().atZoneSameInstant(MALAYSIA_ZONE).format(DATE_FORMATTER);
                String member = employeeDisplayMap.getOrDefault(userId, userId);
                addEmployeeLabel(employeeBox, member + " | " + safe(assignment.getAssignmentStatus())
                        + " | " + assignment.getIndividualProgress() + "%"
                        + " | Due: " + deadline);
            }

            if (employeeBox.getChildren().isEmpty()) {
                addEmployeeLabel(employeeBox, "No assigned employees found.");
            }
        });

        worker.setOnFailed(event -> {
            employeeBox.getChildren().clear();
            addEmployeeLabel(employeeBox, "Unable to load assigned employees.");
            Throwable error = worker.getException();
            if (error != null) error.printStackTrace();
        });

        Thread thread = new Thread(worker);
        thread.setDaemon(true);
        thread.start();
    }

    private void addEmployeeLabel(VBox employeeBox, String employee) {
        Label label = new Label("•  " + employee);
        label.getStyleClass().add("manager-task-popup-employee");
        label.setWrapText(true);
        employeeBox.getChildren().add(label);
    }

    private void addDetailRow(GridPane grid, int row, String field, String value) {
        Label fieldLabel = new Label(field);
        fieldLabel.getStyleClass().add("manager-task-popup-label");

        Label valueLabel = new Label(value);
        valueLabel.setWrapText(true);
        valueLabel.getStyleClass().add("manager-task-popup-value");

        grid.add(fieldLabel, 0, row);
        grid.add(valueLabel, 1, row);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
