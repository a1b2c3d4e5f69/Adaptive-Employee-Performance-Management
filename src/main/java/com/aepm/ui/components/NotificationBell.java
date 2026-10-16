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

            TaskDetailsDialog.show(getScene() == null ? null : getScene().getWindow(), taskId, escalationId);
        });

        return item;
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
