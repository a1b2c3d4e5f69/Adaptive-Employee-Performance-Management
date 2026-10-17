package com.aepm.ui.components;

import com.aepm.data.repository.NotificationRepository;
import com.aepm.util.SessionManager;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Popup;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.aepm.util.SessionManager;
import com.aepm.model.User;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;
import java.net.URLEncoder;

public class NotificationBell extends StackPane {
    private static final ZoneId MALAYSIA = ZoneId.of("Asia/Kuala_Lumpur");

    private final NotificationRepository repository = new NotificationRepository();
    private final Button bellButton = new Button();
    private final Label badge = new Label("0");
    private final Popup popup = new Popup();
    private final VBox notificationList = new VBox(10);
    private boolean loading;

    public NotificationBell() {
        setAlignment(Pos.CENTER);
        setPrefSize(48, 48);
        setMaxSize(48, 48);
        SVGPath bellIcon = new SVGPath();
        bellIcon.setContent("M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4");
        bellIcon.setFill(Color.TRANSPARENT);
        bellIcon.setStroke(Color.web("#334155"));
        bellIcon.setStrokeWidth(1.8);
        bellIcon.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        bellIcon.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        bellButton.setGraphic(bellIcon);
        bellButton.setPrefSize(44, 44);
        bellButton.setMinSize(44, 44);
        bellButton.setStyle("-fx-background-color: transparent; -fx-background-radius: 10; -fx-padding: 10; -fx-cursor: hand;");
        badge.setAlignment(Pos.CENTER);
        badge.setMinSize(19, 19);
        badge.setStyle("-fx-background-color: #EF4444; -fx-background-radius: 100; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold;");
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);
        badge.setMouseTransparent(true);
        badge.setVisible(false);
        badge.setManaged(false);
        getChildren().addAll(bellButton, badge);
        createPopup();

        bellButton.setOnAction(e -> {
            if (popup.isShowing()) popup.hide();
            else showPopup();
        });

        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) Platform.runLater(this::refresh);
            else popup.hide();
        });
    }

    private String token() {
        return SessionManager.getAccessToken();
    }

    private String userId() {
        if (SessionManager.getCurrentUser() == null || SessionManager.getCurrentUser().getUserId() == null) return null;

        return SessionManager.getCurrentUser().getUserId().toString();
    }

    private void createPopup() {

        Label heading = new Label("Notifications");
        heading.setStyle("-fx-font-size: 19px; -fx-font-weight: bold; -fx-text-fill: #0F2345;");

        Button markAll = new Button("Mark all as read");
        markAll.setStyle("-fx-background-color: transparent; -fx-text-fill: #2563EB; -fx-font-size: 12px; -fx-cursor: hand;");
        markAll.setOnAction(e -> markAllAsRead());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(10, heading, spacer, markAll);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 20, 16, 20));

        notificationList.setPadding(new Insets(14));
        notificationList.setFillWidth(true);

        ScrollPane scroll = new ScrollPane(notificationList);
        scroll.setFitToWidth(true);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setPrefHeight(330);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: white; -fx-background-color: white; -fx-border-color: transparent;");

        VBox container = new VBox(header, new Separator(), scroll);
        container.setPrefWidth(420);
        container.setStyle("-fx-background-color: white; -fx-background-radius: 14; -fx-border-color: #DEE7F3; -fx-border-radius: 14; -fx-effect: dropshadow(gaussian, rgba(15,35,69,0.15), 22, 0, 0, 7);");

        popup.getContent().add(container);
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);
    }

    private void showPopup() {
        if (getScene() == null || getScene().getWindow() == null || localToScreen(getBoundsInLocal()) == null) return;

        double x = localToScreen(getBoundsInLocal()).getMaxX() - 420;

        double y = localToScreen(getBoundsInLocal()).getMaxY() + 8;
        popup.show(getScene().getWindow(), x, y);
        refresh();
    }

    public void refresh() {

        String accessToken = token();

        String recipientId = userId();
        if (accessToken == null || accessToken.isBlank() || recipientId == null || loading) return;
        loading = true;

        Task<JSONArray> task = new Task<>() {
            @Override protected JSONArray call() throws Exception {
                return repository.findByRecipient(accessToken, recipientId);
            }
        };

        task.setOnSucceeded(e -> {
            loading = false;

            if (recipientId.equals(userId())) updateNotifications(task.getValue());
        });

        task.setOnFailed(e -> {
            loading = false;
            System.err.println("[Notification] Failed to load: " + task.getException().getMessage());

            if (popup.isShowing()) notificationList.getChildren().setAll(messageLabel("Unable to load notifications."));
        });

        Thread thread = new Thread(task, "notification-refresh");
        thread.setDaemon(true);
        thread.start();
    }

    private void updateNotifications(JSONArray array) {
        notificationList.getChildren().clear();

        int unread = 0;
        for (int i = 0; i < array.length(); i++) {
            JSONObject data = array.getJSONObject(i);

            if (!data.optBoolean("is_read", false)) unread++;
            notificationList.getChildren().add(createNotificationItem(data));
        }

        badge.setText(unread > 99 ? "99+" : String.valueOf(unread));
        badge.setVisible(unread > 0);
        badge.setManaged(unread > 0);
        if (array.isEmpty()) notificationList.getChildren().add(messageLabel("No notifications yet."));
    }

    private Label messageLabel(String text) {

        Label label = new Label(text);
        label.setPadding(new Insets(28));
        label.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 13px;");

        return label;
    }

    private VBox createNotificationItem(JSONObject data) {

        String notificationId = data.getString("notification_id");
        boolean read = data.optBoolean("is_read", false);

        Label icon = new Label("▤");
        icon.setStyle("-fx-font-size: 21px; -fx-text-fill: #2563EB; -fx-background-color: #E4EEFF; -fx-background-radius: 10; -fx-padding: 8 11;");

        Label title = new Label(data.optString("title", "Notification"));
        title.setWrapText(true);
        title.setMaxWidth(290);
        title.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #10264A;");

        Label message = new Label(data.optString("message", ""));
        message.setWrapText(true);
        message.setMaxWidth(290);
        message.setStyle("-fx-font-size: 12px; -fx-text-fill: #62799C;");

        Label date = new Label(formatDate(data.optString("created_at", "")));
        date.setStyle("-fx-font-size: 11px; -fx-text-fill: #7C90AF;");

        Label arrow = new Label("›");
        arrow.setStyle("-fx-font-size: 25px; -fx-text-fill: #2563EB;");

        VBox info = new VBox(6, title, message, date);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox row = new HBox(14, icon, info, arrow);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox item = new VBox(row);
        item.setPadding(new Insets(16));
        item.setStyle("-fx-background-color: " + (read ? "#FFFFFF" : "#EFF6FF")
            + "; -fx-background-radius: 11; -fx-border-color: " + (read ? "#E6EBF3" : "#D2E4FF")
            + "; -fx-border-radius: 11; -fx-cursor: hand;");

        item.setOnMouseClicked(e -> {
            String taskId = data.optString("related_task_id", "");

            if (!read) markAsRead(notificationId);

            popup.hide();

            Long escalationId = null;

            if (data.has("related_escalation_id") && !data.isNull("related_escalation_id")) {
                try { escalationId = data.getLong("related_escalation_id"); }
                catch (Exception ignored) { }
            }

            String notificationTitle = data.optString("title", "");
            String notificationType = data.optString("notification_type", "");

            boolean isPerformanceRisk =
                    notificationTitle.toLowerCase().contains("performance risk")
                    || notificationType.toLowerCase().contains("performance risk");

            boolean isAdmin =
                    SessionManager.getCurrentUser() != null
                    && SessionManager.getCurrentUser().getRole() != null
                    && "ADMINISTRATOR".equalsIgnoreCase(
                            SessionManager.getCurrentUser().getRole().name()
                    );

            if (isAdmin && isPerformanceRisk) {

                showPerformanceRiskDetails(data, escalationId);

            } else {

                TaskDetailsDialog.show(
                    getScene() == null ? null : getScene().getWindow(),
                    taskId,
                    escalationId
                );
            }
        });

        return item;
    }

    private void showPerformanceRiskDetails(
            JSONObject data,
            Long escalationId
    ) {

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Performance Risk Details");

        if (getScene() != null && getScene().getWindow() != null) {
            dialog.initOwner(getScene().getWindow());
        }

        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().add(ButtonType.CLOSE);
        pane.setPrefWidth(850);
        pane.setPrefHeight(680);
        pane.setStyle("-fx-background-color: #F5F7FC;");

        Label heading = new Label("Performance Risk Details");
        heading.setStyle(
            "-fx-font-size: 24px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #10264A;"
        );

        Label subtitle = new Label(
            "Level 3 employee performance risk review."
        );
        subtitle.setStyle(
            "-fx-font-size: 13px;" +
            "-fx-text-fill: #7187AC;"
        );

        VBox header = new VBox(6, heading, subtitle);

        VBox details = new VBox(12);
        details.getChildren().add(
            riskDetailField("Loading", "Loading risk information...")
        );

        VBox card = new VBox(14, details);
        card.setPadding(new Insets(20));
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 14;" +
            "-fx-border-color: #DDE6F4;" +
            "-fx-border-radius: 14;"
        );

        VBox content = new VBox(20, header, card);
        content.setPadding(new Insets(24));

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent;");

        pane.setContent(scroll);

        Button close = (Button) pane.lookupButton(ButtonType.CLOSE);
        close.setText("Close");
        close.setStyle(
            "-fx-background-color: #2563EB;" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 9;" +
            "-fx-padding: 10 28;"
        );

        if (escalationId == null) {
            details.getChildren().setAll(
                riskDetailField(
                    "Unable to Load",
                    "This notification has no escalation ID."
                ),
                riskDetailField(
                    "Notification Message",
                    data.optString("message", "-")
                )
            );

        } else {

            String accessToken = token();
            String expectedUserId = userId();

            Task<JSONObject> loadTask = new Task<>() {
                @Override
                protected JSONObject call() throws Exception {
                    return loadPerformanceRiskData(
                        accessToken,
                        escalationId
                    );
                }
            };

            loadTask.setOnSucceeded(event -> {

                if (!java.util.Objects.equals(
                        expectedUserId, userId())) {
                    dialog.close();
                    return;
                }

                JSONObject result = loadTask.getValue();

                JSONObject escalation =
                    result.getJSONObject("escalation");

                JSONObject task =
                    result.optJSONObject("task");

                JSONObject employee =
                    result.optJSONObject("employee");

                String taskName = task == null
                    ? "-"
                    : task.optString("task_title", "-");

                String employeeName = "-";

                if (employee != null) {
                    employeeName =
                        employee.optString("full_name", "-")
                        + " ("
                        + employee.optString("employee_id", "-")
                        + ")";
                }

                String status =
                    escalation.optString("status", "-");

                String decision =
                    escalation.optString("manager_decision", "-");

                if (decision.isBlank()) {
                    decision = "Pending";
                }

                String note =
                    escalation.optString("follow_up_note", "-");

                if (note.isBlank()) {
                    note = "-";
                }

                String previousDeadline =
                    escalation.optString("previous_deadline", "");

                String reviewedAt =
                    escalation.optString("decided_at", "");

                details.getChildren().setAll(

                // 1. Task & Employee Information
                riskSection(
                    "Task & Employee Information",

                    riskInfoRow(
                        riskInfoCard("Task Name", taskName),
                        riskInfoCard("Employee", employeeName)
                    )
                ),

                // 2. Escalation Information
                riskSection(
                    "Escalation Information",

                    riskInfoRow(
                        riskInfoCard(
                            "Escalation ID",
                            "#" + escalationId
                        ),

                        riskInfoCard(
                            "Escalation Level",
                            "Level " + escalation.optInt("current_level", 3)
                        ),

                        riskInfoCard(
                            "Risk Status",
                            status
                        )
                    )
                ),

                // 3. Manager Review
                riskSection(
                    "Manager Review",

                    riskInfoRow(
                        riskInfoCard(
                            "Manager Decision",
                            decision
                        ),

                        riskInfoCard(
                            "Manager Follow-up Note",
                            note
                        )
                    )
                ),

                // 4. Timeline
                riskSection(
                    "Timeline",

                    riskInfoRow(
                        riskInfoCard(
                            "Previous Deadline",
                            previousDeadline.isBlank()
                                ? "-"
                                : formatDate(previousDeadline)
                        ),

                        riskInfoCard(
                            "Reviewed At",
                            reviewedAt.isBlank()
                                ? "-"
                                : formatDate(reviewedAt)
                        )
                    )
                ),

                // 5. Additional Information
                riskSection(
                    "Additional Information",

                    riskInfoRow(
                        riskInfoCard(
                            "Performance Score Impact",
                            "No automatic score deduction"
                        ),

                        riskInfoCard(
                            "Notification Message",
                            data.optString("message", "-")
                        )
                    )
                )
            );
            });

            loadTask.setOnFailed(event -> {

                Throwable error = loadTask.getException();

                details.getChildren().setAll(
                    riskDetailField(
                        "Unable to Load Risk Details",
                        error == null
                            ? "Unknown error"
                            : error.getMessage()
                    ),
                    riskDetailField(
                        "Notification Message",
                        data.optString("message", "-")
                    )
                );
            });

            Thread thread = new Thread(
                loadTask,
                "performance-risk-details"
            );

            thread.setDaemon(true);
            thread.start();
        }

        dialog.showAndWait();
    }

    private Label riskDetailField(String title, String value) {

        Label label = new Label(title + "\n" + value);

        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);

        label.setStyle(
            "-fx-background-color: #F8FAFD;" +
            "-fx-background-radius: 9;" +
            "-fx-border-color: #E1E9F5;" +
            "-fx-border-radius: 9;" +
            "-fx-padding: 12;" +
            "-fx-font-size: 13px;" +
            "-fx-text-fill: #10264A;"
        );

        return label;
    }

    private VBox riskInfoCard(String title, String value) {

        Label titleLabel = new Label(title);
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(Double.MAX_VALUE);

        titleLabel.setStyle(
            "-fx-font-size: 12px;" +
            "-fx-text-fill: #64748B;"
        );

        Label valueLabel = new Label(
            value == null || value.isBlank() ? "-" : value
        );

        valueLabel.setWrapText(true);
        valueLabel.setMaxWidth(Double.MAX_VALUE);

        valueLabel.setStyle(
            "-fx-font-size: 13px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #10264A;"
        );

        VBox card = new VBox(7, titleLabel, valueLabel);

        card.setPadding(new Insets(15));
        card.setMinWidth(0);
        card.setMinHeight(Region.USE_PREF_SIZE);
        card.setMaxWidth(Double.MAX_VALUE);

        card.setStyle(
            "-fx-background-color: #F5F8FD;" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: #E4ECF7;" +
            "-fx-border-radius: 10;"
        );

        return card;
    }

    private VBox riskSection(String title, javafx.scene.Node... fields) {

        Label heading = new Label(title);

        heading.setStyle(
            "-fx-font-size: 15px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #10264A;"
        );

        VBox body = new VBox(12, fields);

        VBox section = new VBox(14, heading, body);

        section.setPadding(new Insets(18));

        section.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: #DDE7F5;" +
            "-fx-border-radius: 12;"
        );

        return section;
    }

    private HBox riskInfoRow(VBox... cards) {

        HBox row = new HBox(12);
        row.setAlignment(Pos.TOP_LEFT);
        row.setFillHeight(false);
        row.setMaxWidth(Double.MAX_VALUE);

        for (VBox card : cards) {

            card.setMinWidth(0);
            card.setPrefWidth(0);
            card.setMaxWidth(Double.MAX_VALUE);

            HBox.setHgrow(card, Priority.ALWAYS);
            row.getChildren().add(card);
        }

        return row;
    }

    private JSONArray getRiskRows(
            String accessToken,
            String url
    ) throws Exception {

        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException(
                "Session expired. Please log in again."
            );
        }

        SupabaseClient client =
            SupabaseClient.getInstance();

        HttpRequest request = client
            .createAuthenticatedRequest(url, accessToken)
            .GET()
            .build();

        HttpResponse<String> response =
            client.getHttpClient().send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {

            throw new IllegalStateException(
                "Supabase HTTP "
                    + response.statusCode()
                    + ": "
                    + response.body()
            );
        }

        return new JSONArray(response.body());
    }

    private JSONObject loadPerformanceRiskData(
            String accessToken,
            long escalationId
    ) throws Exception {

        String base = AppConfig.REST_URL;

        // 1. Load Level 3 escalation.
        JSONArray escalationRows = getRiskRows(
            accessToken,
            base + "/task_escalation_tbl"
                + "?select=escalation_id,task_id,employee_id,"
                + "current_level,status,manager_decision,"
                + "follow_up_note,previous_deadline,decided_at"
                + "&escalation_id=eq." + escalationId
                + "&current_level=eq.3"
                + "&limit=1"
        );

        if (escalationRows.isEmpty()) {
            throw new IllegalStateException(
                "Level 3 escalation not found or access denied."
            );
        }

        JSONObject escalation =
            escalationRows.getJSONObject(0);

        String taskId =
            escalation.optString("task_id", "");

        String employeeId =
            escalation.optString("employee_id", "");

        JSONObject result = new JSONObject();
        result.put("escalation", escalation);

        // 2. Load task information.
        if (!taskId.isBlank()) {

            String encodedTaskId = URLEncoder.encode(
                taskId,
                StandardCharsets.UTF_8
            );

            JSONArray taskRows = getRiskRows(
                accessToken,
                base + "/task_tbl"
                    + "?select=task_id,task_title"
                    + "&task_id=eq." + encodedTaskId
                    + "&limit=1"
            );

            if (!taskRows.isEmpty()) {
                result.put(
                    "task",
                    taskRows.getJSONObject(0)
                );
            }
        }

        // 3. Load employee information.
        if (!employeeId.isBlank()) {

            String encodedEmployeeId = URLEncoder.encode(
                employeeId,
                StandardCharsets.UTF_8
            );

            JSONArray employeeRows = getRiskRows(
                accessToken,
                base + "/user_tbl"
                    + "?select=user_id,full_name,employee_id"
                    + "&user_id=eq." + encodedEmployeeId
                    + "&limit=1"
            );

            if (!employeeRows.isEmpty()) {
                result.put(
                    "employee",
                    employeeRows.getJSONObject(0)
                );
            }
        }

        return result;
    }

    private String formatDate(String raw) {
        try {
            return DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH)
                .withZone(MALAYSIA).format(OffsetDateTime.parse(raw).toInstant());
        } catch (Exception ignored) { return raw; }
    }

    private void markAsRead(String notificationId) {

        String accessToken = token(), recipientId = userId();
        if (accessToken == null || recipientId == null) return;
        runUpdate(() -> repository.markAsRead(accessToken, notificationId, recipientId));
    }

    private void markAllAsRead() {

        String accessToken = token(), recipientId = userId();
        if (accessToken == null || recipientId == null) return;
        runUpdate(() -> repository.markAllAsRead(accessToken, recipientId));
    }

    private void runUpdate(UpdateAction action) {

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception { action.run(); return null; }
        };

        task.setOnSucceeded(e -> refresh());

        task.setOnFailed(e -> System.err.println("[Notification] Update failed: " + task.getException().getMessage()));

        Thread thread = new Thread(task, "notification-update");
        thread.setDaemon(true);
        thread.start();
    }

    @FunctionalInterface
    private interface UpdateAction {
        void run() throws Exception;
    }
}
