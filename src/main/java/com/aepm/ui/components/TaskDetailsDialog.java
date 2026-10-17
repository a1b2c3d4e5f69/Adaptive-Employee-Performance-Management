package com.aepm.ui.components;

import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.json.JSONArray;
import org.json.JSONObject;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;
import com.aepm.model.User;
import com.aepm.ui.navigation.NavigationManager;
import com.aepm.util.SessionManager;

public final class TaskDetailsDialog {
    private static final ZoneId MY = ZoneId.of("Asia/Kuala_Lumpur");
    private static final String NAVY = "#10264A";
    private static final String MUTED = "#7184A4";
    private static final String BLUE = "#2563EB";

    private TaskDetailsDialog() {
    }

    public static void show(Window owner, String taskId) {
        show(owner, taskId, null);
    }

    public static void show(Window owner, String taskId, Long escalationId) {
        if (taskId == null || taskId.isBlank()) {
            warning(owner, "This notification has no linked task.");
            return;
        }

        String token = SessionManager.getAccessToken();

        if (token == null || token.isBlank()) {
            warning(owner, "Please log in again.");
            return;
        }
        User current = SessionManager.getCurrentUser();
        boolean isManager = current != null && current.getRoleId() == 2;
        boolean isEmployee = current != null && current.getRoleId() == 3;

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(isManager ? "Task Escalation Details" : "Task Details");
        dialog.setHeaderText(null);

        if (owner != null) dialog.initOwner(owner);
        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().add(ButtonType.CLOSE);
        pane.setPrefSize(isManager ? 760 : 680, 700);
        pane.setMinWidth(isManager ? 720 : 650);
        pane.setStyle("-fx-background-color: #F6F8FC; -fx-padding: 0;");

        Button defaultClose = (Button) pane.lookupButton(ButtonType.CLOSE);
        defaultClose.setVisible(false);
        defaultClose.setManaged(false);

        Label heading = label(isManager ? "Task Escalation Details" : "Task Details", 22, NAVY, true);

        Label subtitle = label(isManager
            ? "Review the deadline escalation and record your decision."
            : "Review your task information and progress.", 12, MUTED, false);

        VBox headerText = new VBox(4, heading, subtitle);

        Label headerIcon = label("▤", 24, BLUE, true);
        headerIcon.setAlignment(Pos.CENTER);
        headerIcon.setMinSize(44, 44);
        headerIcon.setStyle("-fx-background-color: #E9F1FF; -fx-background-radius: 14; -fx-text-fill: " + BLUE + "; -fx-font-size: 24px;");

        HBox header = new HBox(12, headerIcon, headerText);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 18, 12, 18));

        VBox card = new VBox(isManager ? 12 : 17);
        card.setPadding(new Insets(isManager ? 16 : 22));
        card.setFillWidth(true);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 14; -fx-border-color: #E1E8F3; -fx-border-radius: 14;");

        ProgressIndicator indicator = new ProgressIndicator();
        indicator.setPrefSize(28, 28);

        HBox loading = new HBox(12, indicator, label("Loading task details...", 13, MUTED, false));
        loading.setAlignment(Pos.CENTER_LEFT);
        card.getChildren().add(loading);

        VBox body = new VBox(0, header, card);
        body.setPadding(new Insets(0, 18, 12, 18));

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setPannable(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: #F6F8FC; -fx-background-color: #F6F8FC; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        Button view = new Button("View My Tasks →");
        view.setStyle("-fx-background-color: #2563EB; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 9; -fx-padding: 11 19; -fx-cursor: hand;");
        view.setVisible(isEmployee);
        view.setManaged(isEmployee);
        view.setOnAction(e -> {
            if (!(owner instanceof Stage stage)) return;
            dialog.close();
            NavigationManager.navigate(stage, "/fxml/employee/employeeTask.fxml");
        });

        Button close = new Button("Close");
        close.setStyle("-fx-background-color: white; -fx-text-fill: #334B70; -fx-font-weight: bold; -fx-border-color: #CCD8E9; -fx-border-radius: 9; -fx-background-radius: 9; -fx-padding: 10 22; -fx-cursor: hand;");
        close.setOnAction(e -> dialog.close());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox footer = new HBox(10, spacer, close, view);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(10, 18, 10, 18));
        footer.setStyle("-fx-background-color: white; -fx-border-color: #E5EBF4; -fx-border-width: 1 0 0 0;");
        VBox dialogLayout = new VBox(scroll, footer);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        pane.setContent(dialogLayout);

        Task<JSONObject[]> fetch = new Task<>() {
            @Override protected JSONObject[] call() throws Exception {
                JSONArray rows = getRows(token, AppConfig.REST_URL + "/task_tbl?select=*&task_id=eq." + enc(taskId) + "&limit=1");

                if (rows.isEmpty()) return new JSONObject[]{null, null, null};
                JSONObject task = rows.getJSONObject(0);

                JSONObject employee = null;

                String employeeId = task.optString("assigned_employee_id", "");

                if (isManager && !employeeId.isBlank()) {
                    try {
                        JSONArray users = getRows(token, AppConfig.REST_URL + "/user_tbl?select=full_name,employee_id&user_id=eq." + enc(employeeId) + "&limit=1");

                        if (!users.isEmpty()) employee = users.getJSONObject(0);
                    } catch (Exception ex) {
                        System.err.println("[Task Details] Employee lookup unavailable: " + ex.getMessage());
                    }
                }

                JSONObject escalation = null;

                if (isManager && escalationId != null) {
                    JSONArray events = getRows(token, AppConfig.REST_URL
                        + "/task_escalation_tbl?select=*,escalation_rule_tbl(extension_days,action_type)"
                        + "&escalation_id=eq." + escalationId
                        + "&task_id=eq." + enc(taskId)
                        + "&limit=1");
                    System.out.println("[DEBUG] Is Manager: " + isManager);
                    System.out.println("[DEBUG] Escalation ID: " + escalationId);
                    System.out.println("[DEBUG] Task ID: " + taskId);
                    if (!events.isEmpty()) escalation = events.getJSONObject(0);
                    System.out.println("[DEBUG] Escalation Query Result: " + events);
                }

                if (isEmployee) {
                    try {
                        JSONArray decisions = getRows(
                            token,
                            AppConfig.REST_URL + "/task_escalation_tbl"
                                + "?select=escalation_id,current_level,status,"
                                + "manager_decision,follow_up_note,decided_at,"
                                + "previous_deadline,new_deadline"
                                + "&task_id=eq." + enc(taskId)
                                + "&order=escalation_id.desc"
                                + "&limit=1"
                        );

                        if (!decisions.isEmpty()) {
                            escalation = decisions.getJSONObject(0);
                        }

                        System.out.println(
                            "[Employee Escalation] " + decisions
                        );

                    } catch (Exception ex) {
                        System.err.println(
                            "[Employee Escalation] Lookup failed: "
                                + ex.getMessage()
                        );
                    }
                }


                return new JSONObject[]{task, employee, escalation};
            }
        };
        fetch.setOnSucceeded(e -> {
            JSONObject[] result = fetch.getValue();

            if (result[0] == null) {
                card.getChildren().setAll(label("Task not found or access is not permitted.", 13, MUTED, false));
                return;
            }

            populate(card, result[0], result[1], isManager);

            if (isEmployee && result[2] != null) {
                addEmployeeDecision(card, result[2]);
            }

            if (isManager) {
                addManagerActions(
                    card, result[0], result[1], result[2],
                    escalationId, taskId, token, dialog
                );
            }
        });
        fetch.setOnFailed(e -> card.getChildren().setAll(
            label("Unable to load task details: " + fetch.getException().getMessage(), 13, "#B91C1C", false)));

        Thread thread = new Thread(fetch, "notification-task-details");
        thread.setDaemon(true);
        thread.start();
        dialog.showAndWait();
    }

    private static void populate(VBox card, JSONObject task, JSONObject employee, boolean isManager) {
        card.getChildren().clear();

        String title = first(task, "task_title", "task_name", "title");

        String status = first(task, "task_status", "status");

        String priority = first(task, "priority", "priority_level");

        String difficulty = first(task, "difficulty", "difficulty_level");

        double progress = parsePercent(first(task, "progress", "progress_percentage"));

        Label taskTitle = label(title, 22, NAVY, true);
        taskTitle.setWrapText(true);

        HBox badges = new HBox(8);
        badges.setAlignment(Pos.CENTER_LEFT);
        badges.setFillHeight(false);
        badges.getChildren().addAll(
            chip(status, "Completed".equalsIgnoreCase(status) ? "#E8F8EF" : "#FFF3DC", "Completed".equalsIgnoreCase(status) ? "#16834A" : "#A75A08"),
            chip(priority + " Priority", "#FFF0F1", "#C12D49"),
            chip(formatPercent(progress) + " Progress", "#EEE9FF", "#6741C1")
        );

        VBox titleBlock = new VBox(8, taskTitle, badges);
        card.getChildren().add(titleBlock);

        if (isManager) {
            String name = employee == null ? "Not available" : first(employee, "full_name");

            String id = employee == null ? "-" : first(employee, "employee_id");

            VBox employeePanel = new VBox(7, sectionLabel("ASSIGNED EMPLOYEE"));

            GridPane employeeGrid = grid();
            employeeGrid.add(infoCard("Employee Name", name), 0, 0);
            employeeGrid.add(infoCard("Employee ID", id), 1, 0);
            employeePanel.getChildren().add(employeeGrid);
            card.getChildren().add(employeePanel);
        }

        VBox description = new VBox(6, sectionLabel("DESCRIPTION"));

        Label desc = label(first(task, "description", "task_description"), 13, "#273D60", false);
        desc.setWrapText(true);
        desc.setMaxWidth(Double.MAX_VALUE);
        desc.setPadding(new Insets(isManager ? 10 : 14));
        desc.setStyle("-fx-background-color: #F1F6FD; -fx-background-radius: 10; -fx-text-fill: #273D60; -fx-font-size: 13px;");
        description.getChildren().add(desc);
        card.getChildren().add(description);

        VBox details = new VBox(7, sectionLabel("TASK INFORMATION"));

        GridPane info = grid();
        info.add(infoCard("Due Date", formatDate(task.optString("due_date", ""))), 0, 0);
        info.add(infoCard("Completion Date", formatDate(task.optString("completion_date", ""))), 1, 0);
        info.add(infoCard("Difficulty", difficulty), 0, 1);
        info.add(infoCard("Task Status", status), 1, 1);
        details.getChildren().add(info);
        card.getChildren().add(details);

        VBox progressBox = new VBox(6);

        HBox progressHead = new HBox(sectionLabel("TASK PROGRESS"));
        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        progressHead.getChildren().addAll(grow, label(formatPercent(progress), 13, BLUE, true));

        ProgressBar bar = new ProgressBar(progress / 100.0);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setPrefHeight(9);
        progressBox.getChildren().addAll(progressHead, bar);
        card.getChildren().add(progressBox);

        if (!"Completed".equalsIgnoreCase(status) && !"Cancelled".equalsIgnoreCase(status)) {
            try {
                LocalDate due = OffsetDateTime.parse(task.getString("due_date")).atZoneSameInstant(MY).toLocalDate();
                LocalDate today = LocalDate.now(MY);

                if (!due.isAfter(today)) {
                    boolean overdue = due.isBefore(today);

                    String text = overdue
                        ? (isManager ? "This employee task is overdue. Please follow up with the assigned employee." : "This task is overdue. Please review your progress.")
                        : (isManager ? "This employee task is due today. Please follow up before the deadline." : "This task is due today. Please complete it before the deadline.");

                    Label alert = label(text, 12, overdue ? "#A45B13" : "#2058B6", true);
                    alert.setWrapText(true);
                    alert.setMaxWidth(Double.MAX_VALUE);
                    alert.setPadding(new Insets(13));
                    alert.setStyle("-fx-background-color: " + (overdue ? "#FFF5E7" : "#EAF2FF") + "; -fx-background-radius: 9; -fx-text-fill: " + (overdue ? "#A45B13" : "#2058B6") + "; -fx-font-size: 12px; -fx-font-weight: bold;");
                    card.getChildren().add(alert);
                }
            } catch (Exception ignored) { }
        }
    }

    private static void addEmployeeDecision(
            VBox card,
            JSONObject escalation) {

        int level = escalation.optInt("current_level", 0);

        String decision = escalation.optString(
            "manager_decision", ""
        );

        if (decision.isBlank()
                || "null".equalsIgnoreCase(decision)) {
            decision = "Pending";
        }

        String note = escalation.optString(
            "follow_up_note", ""
        );

        // Use the same style as the existing Task Details.
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(14));

        panel.setStyle(
            "-fx-background-color: #F0F6FF;"
            + "-fx-background-radius: 12;"
            + "-fx-border-color: #D5E4FA;"
            + "-fx-border-radius: 12;"
        );

        panel.getChildren().add(
            sectionLabel("MANAGER ESCALATION REVIEW")
        );

        GridPane info = grid();

        info.add(
            infoCard("Escalation Level", "Level " + level),
            0, 0
        );

        info.add(
            infoCard("Manager Decision", decision),
            1, 0
        );

        panel.getChildren().add(info);

        if (level == 2) {
            Label explanation = label(
                "Adjustment Planned".equalsIgnoreCase(decision)
                    ? "Your manager has proposed reducing task complexity. "
                        + "The task has not been automatically modified."
                    : "Your manager has reviewed the escalation.",
                12, MUTED, false
            );

            explanation.setWrapText(true);
            panel.getChildren().add(explanation);
        }

        panel.getChildren().add(
            sectionLabel("MANAGER FOLLOW-UP NOTE")
        );

        Label noteValue = label(
            note.isBlank() || "null".equalsIgnoreCase(note)
                ? "No follow-up note provided."
                : note,
            13, "#273D60", false
        );

        noteValue.setWrapText(true);
        noteValue.setMaxWidth(Double.MAX_VALUE);
        noteValue.setPadding(new Insets(12));

        noteValue.setStyle(
            "-fx-background-color: #FBFCFE;"
            + "-fx-background-radius: 10;"
            + "-fx-border-color: #E3EAF4;"
            + "-fx-border-radius: 10;"
            + "-fx-text-fill: #273D60;"
            + "-fx-font-size: 13px;"
        );

        panel.getChildren().add(noteValue);

        // populate() order:
        // 0 = Task title
        // 1 = Description
        // 2 = Task Information
        // Insert between Description and Task Information.
        card.getChildren().add(
            Math.min(2, card.getChildren().size()),
            panel
        );
    }

    private static void addManagerActions( VBox card, JSONObject task, JSONObject employee, JSONObject escalation, Long escalationId, String taskId, String token, Dialog<Void> dialog) {
        if (escalationId == null || escalation == null) {
            card.getChildren().add(notePanel("No linked escalation was found. Manager actions are unavailable."));
            return;
        }

        String decision = escalation.optString("manager_decision", "");

        if ("null".equalsIgnoreCase(decision)) decision = "";
        boolean acknowledged = escalation.has("acknowledged_at") && !escalation.isNull("acknowledged_at");

        int level = escalation.optInt("current_level", 0);

        JSONObject rule = escalation.optJSONObject("escalation_rule_tbl");
        int days = rule == null ? 0 : rule.optInt("extension_days", 0);

        if (days <= 0 && escalation.has("rule_id")
                && !escalation.isNull("rule_id")) {
            try {
                String ruleId = String.valueOf(escalation.get("rule_id"));

                JSONArray rules = getRows(token, AppConfig.REST_URL
                    + "/escalation_rule_tbl?select=extension_days"
                    + "&rule_id=eq." + enc(ruleId)
                    + "&limit=1");

                if (!rules.isEmpty()) {
                    days = rules.getJSONObject(0).optInt("extension_days", 0);
                }
                System.out.println("[DEBUG] Escalation Rule Query: " + rules);
                System.out.println("[DEBUG] Extension Days: " + days);
            } catch (Exception ex) {
                System.err.println("[Escalation] Unable to load extension days: "
                    + ex.getMessage());
            }
        }

        String status = escalation.optString("status", "");
        VBox management = new VBox(10);
        management.setPadding(new Insets(14));
        management.setStyle("-fx-background-color: #F0F6FF; -fx-background-radius: 12; "
            + "-fx-border-color: #D5E4FA; -fx-border-radius: 12;");
        management.getChildren().add(sectionLabel("ESCALATION MANAGEMENT"));

        GridPane info = grid();
        info.add(infoCard("Escalation Level", "Level " + level), 0, 0);
        info.add(infoCard("Manager Decision", decision.isBlank() ? "Pending" : decision), 1, 0);
        info.add(infoCard("Extension Allowed", level == 1 ? days + " days" : "Not applicable"), 0, 1);
        info.add(infoCard("Acknowledged", acknowledged ? "Yes" : "No"), 1, 1);
        management.getChildren().add(info);

        Label noteTitle = sectionLabel("MANAGER FOLLOW-UP NOTE");
        TextArea note = new TextArea(escalation.isNull("follow_up_note") ? "" : escalation.optString("follow_up_note", ""));
        note.setPromptText("Record your follow-up or reason for the decision...");
        note.setWrapText(true);
        note.setPrefRowCount(3);
        note.setPrefHeight(76);
        note.setMaxHeight(90);

        Label feedback = label("", 12, "#2563EB", false);
        feedback.setWrapText(true);

        Button save = actionButton("Save Note", "#E9EEF5");
        Button ack = actionButton("Acknowledge", "#E7EEFF");
        Button reject = actionButton("Reject Extension", "#F8EDEB");
        Button approve = actionButton("Approve Extension", "#E5F3EB");
        Button adjust = actionButton("Apply Adjustment", "#E5F3EB");
        Button keep = actionButton("Keep Current Task", "#F8EDEB");
        Button resolveRisk = actionButton(
            "Resolve Risk Alert", "#E5F3EB"
        );

        resolveRisk.setStyle(
            resolveRisk.getStyle()
                + "-fx-text-fill: #26734D;"
        );

        adjust.setStyle(adjust.getStyle() + "-fx-text-fill: #26734D;");
        keep.setStyle(keep.getStyle() + "-fx-text-fill: #A4574C;");

        save.setStyle(save.getStyle() + "-fx-text-fill: #334B70;");
        ack.setStyle(ack.getStyle() + "-fx-text-fill: #315DC5;");
        reject.setStyle(reject.getStyle() + "-fx-text-fill: #A4574C;");
        approve.setStyle(approve.getStyle() + "-fx-text-fill: #26734D;");

        boolean pendingLevelOne = level == 1 && decision.isBlank()
            && "active".equalsIgnoreCase(status);

        boolean pendingLevelTwo = level == 2
            && decision.isBlank()
            && "active".equalsIgnoreCase(status);

        boolean pendingLevelThree =
            level == 3
            && "active".equalsIgnoreCase(status);

        resolveRisk.setVisible(pendingLevelThree);
        resolveRisk.setManaged(pendingLevelThree);
        resolveRisk.setDisable(!pendingLevelThree);

        adjust.setVisible(pendingLevelTwo);
        adjust.setManaged(pendingLevelTwo);
        adjust.setDisable(!pendingLevelTwo);

        keep.setVisible(pendingLevelTwo);
        keep.setManaged(pendingLevelTwo);
        keep.setDisable(!pendingLevelTwo);

        boolean canApprove = pendingLevelOne && days > 0;
        approve.setVisible(pendingLevelOne);
        approve.setManaged(pendingLevelOne);
        approve.setDisable(!canApprove);
        reject.setVisible(pendingLevelOne);
        reject.setManaged(pendingLevelOne);
        ack.setDisable(acknowledged);

        FlowPane actions = new FlowPane( 8, 8, save, ack, reject, approve, adjust, keep, resolveRisk);
        actions.setAlignment(Pos.CENTER_LEFT);

        management.getChildren().addAll(noteTitle, note, feedback, actions);

        // Keep Manager actions near the top, before the longer task description.
        card.getChildren().add(Math.min(2, card.getChildren().size()), management);
        
        final int extensionDays = days;
        final String taskName = first(task, "task_title", "task_name", "title");
        final String employeeName = employee == null
            ? "Employee" : first(employee, "full_name");

        final String originalDeadline = formatDate(
            escalation.optString("previous_deadline",
                task.optString("due_date", ""))
        );

        save.setOnAction(e -> confirmAndExecute(
            "Save Follow-up Note", "note", escalationId,
            note.getText(), token, feedback, actions, dialog,
            taskName, employeeName, originalDeadline, extensionDays
        ));

        ack.setOnAction(e -> confirmAndExecute(
            "Acknowledge Escalation", "acknowledge", escalationId,
            note.getText(), token, feedback, actions, dialog,
            taskName, employeeName, originalDeadline, extensionDays
        ));

        reject.setOnAction(e -> confirmAndExecute(
            "Reject Extension", "reject", escalationId,
            note.getText(), token, feedback, actions, dialog,
            taskName, employeeName, originalDeadline, extensionDays
        ));

        approve.setOnAction(e -> confirmAndExecute(
            "Approve " + extensionDays + "-Day Extension",
            "approve", escalationId,
            note.getText(), token, feedback, actions, dialog,
            taskName, employeeName, originalDeadline, extensionDays
        ));

        adjust.setOnAction(e -> confirmAndExecute(
            "Apply Adjustment",
            "adjust", escalationId,
            note.getText(), token, feedback, actions, dialog,
            taskName, employeeName, originalDeadline, extensionDays
        ));

        keep.setOnAction(e -> confirmAndExecute(
            "Keep Current Task",
            "keep", escalationId,
            note.getText(), token, feedback, actions, dialog,
            taskName, employeeName, originalDeadline, extensionDays
        ));

        resolveRisk.setOnAction(e -> confirmAndExecute(
            "Resolve Performance Risk Alert",
            "resolve",
            escalationId,
            note.getText(),
            token,
            feedback,
            actions,
            dialog,
            taskName,
            employeeName,
            originalDeadline,
            extensionDays
        ));
    }

    private static Label notePanel(String text) {
        Label label = label(text, 12, "#A45B13", true);
        label.setWrapText(true);
        label.setPadding(new Insets(12));
        label.setStyle("-fx-background-color: #FFF5E7; -fx-text-fill: #A45B13; -fx-background-radius: 9;");
        return label;
    }

    private static Button actionButton(String text, String color) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: " + color
            + "; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;"
            + " -fx-background-radius: 8; -fx-padding: 10 12; -fx-cursor: hand;");
        return button;
    }

    
    private static void confirmAndExecute(
            String title, String action, Long escalationId,
            String note, String token, Label feedback,
            Pane buttons, Dialog<Void> dialog,
            String taskName, String employeeName,
            String originalDeadline, int extensionDays) {

        if ("note".equals(action)
                && (note == null || note.isBlank())) {
            feedback.setText("Enter a follow-up note before saving.");
            return;
        }

        if (note != null && note.length() > 2000) {
            feedback.setText("Note must be 2000 characters or fewer.");
            return;
        }

        if (("adjust".equals(action) || "keep".equals(action) || "resolve".equals(action)) && (note == null || note.isBlank())) {

            feedback.setText(
                "Please enter a follow-up note before confirming."
            );
            return;
        }

        Dialog<ButtonType> confirm = new Dialog<>();
        confirm.setTitle("Confirm Manager Action");
        confirm.setHeaderText(null);

        if (dialog.getDialogPane().getScene() != null) {
            confirm.initOwner(
                dialog.getDialogPane().getScene().getWindow()
            );
        }

        DialogPane pane = confirm.getDialogPane();
        pane.setPrefWidth(480);
        pane.setStyle(
            "-fx-background-color: white; -fx-padding: 20;"
        );

        String explanation = switch (action) {
            case "adjust" ->
                "Record a plan to reduce task complexity. "
                + "This will resolve Level 2, but will not automatically "
                + "modify the task.";

            case "keep" ->
                "Keep the current task unchanged and resolve "
                + "this Level 2 escalation.";
            case "approve" ->
                "Approve a " + extensionDays
                + "-day extension. The new deadline is calculated by Supabase.";
            case "reject" ->
                "Reject the extension request. The task deadline will remain unchanged.";
            case "acknowledge" ->
                "Mark this escalation as acknowledged. " + "You can still make a manager decision later.";
            case "resolve" -> 
                "Confirm that you have reviewed the Level 3 " + "performance risk and recorded your follow-up action. " + "This resolves the risk alert, but does not " + "complete the task or change its deadline.";
            default ->
                "Save this follow-up note. This will not finalize the extension decision.";
        };

        Label message = label(explanation, 13, "#334B70", false);
        message.setWrapText(true);

        VBox content = new VBox(11,
            label(title, 20, NAVY, true),
            message,
            infoCard("Task", taskName),
            infoCard("Assigned Employee", employeeName),
            infoCard("Original Deadline (MYT)", originalDeadline)
        );

        if ("approve".equals(action)) {
            content.getChildren().add(
                infoCard("Extension Granted", extensionDays + " days")
            );
        }

        if (note != null && !note.isBlank()) {
            Label notePreview = label(note.trim(), 12, "#334B70", false);
            notePreview.setWrapText(true);

            content.getChildren().add(
                new VBox(5, sectionLabel("FOLLOW-UP NOTE"), notePreview)
            );
        }

        if ("approve".equals(action)|| "reject".equals(action)|| "adjust".equals(action)|| "keep".equals(action)) {
            Label warning = label(
                "This decision cannot be changed after confirmation.",
                12, "#A45B13", true
            );
            warning.setWrapText(true);
            content.getChildren().add(warning);
        }

        pane.setContent(content);

        ButtonType cancelType = new ButtonType(
            "Cancel", ButtonBar.ButtonData.CANCEL_CLOSE
        );
        ButtonType confirmType = new ButtonType(
            "Confirm", ButtonBar.ButtonData.OK_DONE
        );

        pane.getButtonTypes().addAll(cancelType, confirmType);

        ((Button) pane.lookupButton(cancelType)).setStyle(
            "-fx-background-color: #F1F5F9;"
            + "-fx-text-fill: #334155;"
            + "-fx-background-radius: 8;"
            + "-fx-padding: 10 16;"
        );

        ((Button) pane.lookupButton(confirmType)).setStyle(
            "-fx-background-color: #2563EB;"
            + "-fx-text-fill: white;"
            + "-fx-font-weight: bold;"
            + "-fx-background-radius: 8;"
            + "-fx-padding: 10 16;"
        );

        if (confirm.showAndWait().orElse(cancelType) != confirmType) {
            return;
        }

        executeManagerAction(
            action, escalationId, note, token,
            feedback, buttons, dialog,
            taskName, employeeName, originalDeadline, extensionDays
        );
    }


    private static void executeManagerAction(
            String action, Long escalationId, String note,
            String token, Label feedback, Pane buttons,
            Dialog<Void> dialog, String taskName,
            String employeeName, String originalDeadline,
            int extensionDays) {

        buttons.setDisable(true);
        feedback.setText("Saving...");

        Task<JSONObject> work = new Task<>() {
            @Override
            protected JSONObject call() throws Exception {
                JSONObject payload = new JSONObject();
                payload.put("p_escalation_id", escalationId);
                payload.put("p_action", action);
                payload.put("p_note",
                    note == null || note.isBlank()
                        ? JSONObject.NULL : note.trim()
                );

                SupabaseClient client = SupabaseClient.getInstance();
                boolean isLevelTwoAction =
                    "adjust".equals(action) || "keep".equals(action);

                String rpcName;
                if ("resolve".equals(action)) {
                    rpcName = "manager_handle_level3_escalation";
                } else if (isLevelTwoAction) {
                    rpcName = "manager_handle_level2_escalation";
                } else {
                    rpcName = "manager_handle_escalation";
                }

                HttpRequest request = client.createAuthenticatedRequest(
                    AppConfig.REST_URL + "/rpc/" + rpcName,
                    token
                )
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

                HttpResponse<String> response =
                    client.getHttpClient().send(
                        request, HttpResponse.BodyHandlers.ofString()
                    );

                if (response.statusCode() < 200
                        || response.statusCode() >= 300) {
                    String message = response.body();

                    try {
                        message = new JSONObject(message)
                            .optString("message", message);
                    } catch (Exception ignored) { }

                    throw new Exception(message);
                }

                JSONArray rows = new JSONArray(response.body());

                if (rows.isEmpty()) {
                    throw new Exception(
                        "No result returned from " + rpcName
                    );
                }

                return rows.getJSONObject(0);
            }
        };

        work.setOnSucceeded(e -> {
            JSONObject result = work.getValue();

            String resultingDeadline = formatDate(
                result.optString("resulting_deadline", "")
            );

            feedback.setText("Action saved successfully.");

            showActionSuccess(
                dialog, action, taskName, employeeName,
                originalDeadline, resultingDeadline,
                extensionDays, note
            );

            dialog.close();
        });

        work.setOnFailed(e -> {
            buttons.setDisable(false);
            feedback.setText(
                "Unable to save: " + work.getException().getMessage()
            );
        });

        Thread thread = new Thread(
            work, "manager-escalation-action"
        );
        thread.setDaemon(true);
        thread.start();
    }

    private static void showActionSuccess(
            Dialog<Void> parent, String action,
            String taskName, String employeeName,
            String originalDeadline, String resultingDeadline,
            int extensionDays, String note) {

        Dialog<ButtonType> success = new Dialog<>();
        success.setTitle("Manager Action Completed");
        success.setHeaderText(null);

        if (parent.getDialogPane().getScene() != null) {
            success.initOwner(
                parent.getDialogPane().getScene().getWindow()
            );
        }

        DialogPane pane = success.getDialogPane();
        pane.setPrefWidth(480);
        pane.setStyle(
            "-fx-background-color: white; -fx-padding: 20;"
        );

        String heading = switch (action) {
            case "adjust" -> "Adjustment Plan Recorded";
            case "keep" -> "Task Kept Unchanged";
            case "approve" -> "Extension Approved Successfully";
            case "reject" -> "Extension Rejected Successfully";
            case "acknowledge" -> "Escalation Acknowledged";
            case "resolve" -> "Performance Risk Alert Resolved";
            default -> "Follow-up Note Saved";
        };

        VBox content = new VBox(11,
            label("✓  " + heading, 19, "#16834A", true),
            infoCard("Task", taskName),
            infoCard("Assigned Employee", employeeName)
        );

        if ("approve".equals(action)) {
            content.getChildren().addAll(
                infoCard("Extension Granted", extensionDays + " days"),
                infoCard("Previous Deadline (MYT)", originalDeadline),
                infoCard("New Deadline (MYT)", resultingDeadline)
            );

        } else if ("reject".equals(action)) {
            content.getChildren().add(
                infoCard("Deadline (Unchanged, MYT)", resultingDeadline)
            );
        
        } else if ("adjust".equals(action)) {

            Label message = label(
                "The Level 2 adjustment plan has been recorded. "
                + "The employee has been notified.",
                12, MUTED, false
            );

            message.setWrapText(true);
            content.getChildren().add(message);

        } else if ("resolve".equals(action)) {

            Label message = label(
                "The Level 3 performance risk has been reviewed "
                + "and resolved. The task remains unchanged.",
                12, MUTED, false
            );

            message.setWrapText(true);
            content.getChildren().add(message);
            message.setMaxWidth(Double.MAX_VALUE);
            message.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
            
        }else if ("keep".equals(action)) {

            Label message = label(
                "Level 2 has been resolved without changing the task. "
                + "The employee has been notified.",
                12, MUTED, false
            );

            message.setWrapText(true);
            content.getChildren().add(message);

        } else if ("acknowledge".equals(action)) {
            Label message = label(
                "Escalation acknowledged. You can still approve or reject later.",
                12, MUTED, false
            );
            message.setWrapText(true);
            content.getChildren().add(message);
        }

        if (note != null && !note.isBlank()) {
            Label noteValue = label(
                note.trim(), 12, "#334B70", false
            );
            noteValue.setWrapText(true);

            content.getChildren().add(
                new VBox(5,
                    sectionLabel("SAVED FOLLOW-UP NOTE"),
                    noteValue
                )
            );
        }

        pane.setContent(content);

        ButtonType done = new ButtonType(
            "Done", ButtonBar.ButtonData.OK_DONE
        );
        pane.getButtonTypes().add(done);

        ((Button) pane.lookupButton(done)).setStyle(
            "-fx-background-color: #2563EB;"
            + "-fx-text-fill: white;"
            + "-fx-font-weight: bold;"
            + "-fx-background-radius: 8;"
            + "-fx-padding: 10 22;"
        );

        success.showAndWait();
    }


    private static JSONArray getRows(String token, String url) throws Exception {
        SupabaseClient client = SupabaseClient.getInstance();
        HttpRequest request = client.createAuthenticatedRequest(url, token).GET().build();
        HttpResponse<String> response = client.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300)
        throw new Exception("Supabase HTTP " + response.statusCode() + ": " + response.body());
        return new JSONArray(response.body());
    }

    private static String enc(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }

    private static GridPane grid() {
        GridPane pane = new GridPane();
        pane.setHgap(10);
        pane.setVgap(8);
        ColumnConstraints a = new ColumnConstraints();
        ColumnConstraints b = new ColumnConstraints();
        a.setPercentWidth(50);
        b.setPercentWidth(50);
        pane.getColumnConstraints().addAll(a, b);
        return pane;
    }

    private static VBox infoCard(String heading, String value) {
        Label content = label(value, 13, NAVY, true);
        content.setWrapText(true);

        VBox box = new VBox(6, label(heading, 11, MUTED, false), content);
        box.setPadding(new Insets(10));
        box.setMaxWidth(Double.MAX_VALUE);
        box.setMinHeight(60);
        box.setStyle("-fx-background-color: #FBFCFE; -fx-background-radius: 10; -fx-border-color: #E3EAF4; -fx-border-radius: 10;");
        return box;
    }

    private static Label label(String text, int size, String color, boolean bold) {
        Label result = new Label(text);
        result.setStyle("-fx-font-size: " + size + "px; -fx-text-fill: " + color + ";" + (bold ? " -fx-font-weight: bold;" : ""));
        return result;
    }

    private static Label sectionLabel(String text) { return label(text, 11, MUTED, true); }

    private static Label chip(String text, String bg, String fg) {
        Label chip = label(text, 11, fg, true);
        chip.setPadding(new Insets(6, 11, 6, 11));
        chip.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg + "; -fx-background-radius: 20; -fx-padding: 6 11; -fx-font-size: 11px; -fx-font-weight: bold;");
        return chip;
    }

    private static String first(JSONObject obj, String... keys) {
        for (String key : keys) {
            if (obj.has(key) && !obj.isNull(key)) {
                String value = String.valueOf(obj.get(key)).trim();

                if (!value.isBlank()) return value;
            }
        }

        return "-";
    }

    private static double parsePercent(String raw) {
        try { return Math.max(0, Math.min(100, Double.parseDouble(raw.replace("%", "").trim()))); }
        catch (Exception ignored) { return 0; }
    }

    private static String formatPercent(double n) { return String.format(Locale.US, "%.0f%%", n); }

    private static String formatDate(String raw) {
        if (raw == null || raw.isBlank() || "null".equalsIgnoreCase(raw)) return "-";
        try {
            return DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH)
                .withZone(MY).format(OffsetDateTime.parse(raw).toInstant());
        } catch (Exception ignored) { return raw; }
    }

    private static void warning(Window owner, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);

        if (owner != null) alert.initOwner(owner);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

}
