package com.aepm.ui.common;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;
import com.aepm.util.SessionManager;
import com.google.gson.*;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.awt.Desktop;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.Callable;

public final class AnnouncementDetailsDialog {

    private static final int MAX_BYTES = 10 * 1024 * 1024;

    private final Stage dialog = new Stage();
    private final VBox content = new VBox(14);
    private final Label status = new Label();
    private final Button retry = new Button("Reload");
    private final Button close = new Button("Close");

    private final String token = SessionManager.getAccessToken();
    private final Object session = SessionManager.getAuthSession();
    private final JsonObject query;

    private boolean busy;

    public static void showNotification(Window owner, String notificationId) {
        JsonObject query = new JsonObject();
        query.addProperty("p_notification_id", notificationId);
        open(owner, query);
    }

    public static void showSent(Window owner, String announcementId) {
        JsonObject query = new JsonObject();
        query.addProperty("p_announcement_id", announcementId);
        open(owner, query);
    }

    private static void open(Window owner, JsonObject query) {
        if (SessionManager.getAccessToken() == null) {
            AEPMDialog.showError(
                    owner, "Sign In Required", "Please sign in again."
            );
            return;
        }

        new AnnouncementDetailsDialog(owner, query).show();
    }

    private AnnouncementDetailsDialog(Window owner, JsonObject query) {
        this.query = query;

        String windowTitle = query.has("p_schedule_id")
            ? "Scheduled Announcement"
            : "Announcement Details";

        dialog.setTitle(windowTitle);

        if (owner != null) {
            dialog.initOwner(owner);
            dialog.initModality(Modality.WINDOW_MODAL);
        } else {
            dialog.initModality(Modality.APPLICATION_MODAL);
        }

        Label heading = new Label(windowTitle);
        heading.setStyle(
                "-fx-font-size: 24px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: #0F172A;"
        );

        Label subtitle = new Label("Announcement details, attachments and links");
        subtitle.setStyle("-fx-text-fill: #64748B;");
        subtitle.setWrapText(true);

        VBox header = new VBox(6, heading, subtitle);
        header.setPadding(new Insets(24, 24, 16, 24));

        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: white;");

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle(
                "-fx-background: white;"
                + "-fx-background-color: white;"
                + "-fx-border-color: transparent;"
        );

        status.setWrapText(true);
        status.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(status, Priority.ALWAYS);

        retry.getStyleClass().add("secondary-button");
        close.getStyleClass().add("login-primary-button");

        retry.setOnAction(event -> load());
        close.setOnAction(event -> dialog.close());

        HBox footer = new HBox(12, status, retry, close);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(16, 24, 20, 24));

        BorderPane root = new BorderPane(scroll);
        root.setTop(header);
        root.setBottom(footer);
        root.setStyle(
                "-fx-background-color: white;"
                + "-fx-font-family: 'Segoe UI';"
                + "-fx-font-size: 14px;"
        );

        Scene scene = new Scene(root, 760, 620);

        var css = AnnouncementDetailsDialog.class.getResource("/css/app.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        dialog.setScene(scene);
        dialog.setMinWidth(550);
        dialog.setMinHeight(400);

        dialog.setOnCloseRequest(event -> {
            if (busy) {
                event.consume();
            }
        });
    }

    private void show() {
        dialog.show();
        load();
    }

    private boolean sameSession() {
        return SessionManager.getAuthSession() == session
                && SessionManager.getAccessToken() != null;
    }

    private void load() {
        if (busy) {
            return;
        }

        status.setText("Loading announcement...");

        run(() -> {
            var client = SupabaseClient.getInstance();

            HttpRequest request = client.createAuthenticatedRequest(
                    AppConfig.REST_URL
                        + (query.has("p_schedule_id")
                            ? "/rpc/get_scheduled_announcement_detail"
                            : "/rpc/get_system_announcement_detail"),
                    token
            )
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(query.toString()))
                    .build();

            HttpResponse<String> response = client.getHttpClient().send(
                    request, HttpResponse.BodyHandlers.ofString()
            );

            requireSuccess(response.statusCode());

            return JsonParser.parseString(response.body()).getAsJsonObject();
        }, this::display);
    }

    private void display(JsonObject data) {
        content.getChildren().clear();

        Label subject = paragraph(text(data, "subject"));
        subject.setStyle(
                "-fx-font-size: 21px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: #0F172A;"
        );

        VBox metadata = new VBox(8);

        if (query.has("p_schedule_id")) {
            Label state = paragraph("Status: " + text(data, "status"));
            state.setStyle(
                    "-fx-text-fill: #1D4ED8;"
                    + "-fx-font-weight: bold;"
            );

            Label scheduled = paragraph(
                    "Scheduled at (MYT): "
                            + formatDetailTime(text(data, "scheduled_at"))
            );
            scheduled.setStyle("-fx-text-fill: #64748B;");

            Label audienceLabel = paragraph(
                    "Audience: " + text(data, "audience")
            );
            audienceLabel.setStyle("-fx-text-fill: #334155;");

            metadata.getChildren().addAll(state, scheduled, audienceLabel);

            String sentAt = text(data, "sent_at");

            if (!sentAt.isBlank()) {
                Label sentDate = paragraph(
                        "Sent at (MYT): " + formatDetailTime(sentAt)
                );
                sentDate.setStyle("-fx-text-fill: #64748B;");
                metadata.getChildren().add(sentDate);
            }

            if ("Selected".equals(text(data, "audience"))) {
                metadata.getChildren().add(
                        sectionTitle("Selected recipients")
                );

                JsonArray recipients = data.getAsJsonArray(
                        "selected_recipients"
                );

                if (recipients != null) {
                    for (JsonElement element : recipients) {
                        JsonObject recipient = element.getAsJsonObject();

                        String description = text(recipient, "full_name");
                        String employeeId = text(recipient, "employee_id");

                        if (!employeeId.isBlank()) {
                            description += "  |  " + employeeId;
                        }

                        String accountStatus = text(
                                recipient, "account_status"
                        );

                        if (!"Active".equals(accountStatus)) {
                            description += "  |  "
                                    + (accountStatus.isBlank()
                                        ? "Unavailable"
                                        : accountStatus);
                        }

                        Label recipientLabel = paragraph(description);
                        recipientLabel.setStyle("-fx-text-fill: #334155;");
                        metadata.getChildren().add(recipientLabel);
                    }
                }
            } else {
                Label audienceHint = paragraph(
                        "Group recipients are determined at sending time."
                );
                audienceHint.setStyle("-fx-text-fill: #64748B;");
                metadata.getChildren().add(audienceHint);
            }

            String failure = text(data, "last_error");

            if (!failure.isBlank()) {
                Label errorLabel = paragraph(
                        "Failure details:\n" + failure
                );
                errorLabel.setPadding(new Insets(12));
                errorLabel.setStyle(
                        "-fx-background-color: #FEF2F2;"
                        + "-fx-background-radius: 10;"
                        + "-fx-text-fill: #B91C1C;"
                );

                metadata.getChildren().add(errorLabel);
            }
        } else {
            Label sentDate = paragraph(
                    "Sent at (MYT): "
                            + formatDetailTime(text(data, "sent_at"))
            );
            sentDate.setStyle("-fx-text-fill: #64748B;");
            metadata.getChildren().add(sentDate);
        }

        Label body = paragraph(text(data, "message"));
        body.setPadding(new Insets(16));
        body.setStyle(
                "-fx-background-color: #F6F8FC;"
                + "-fx-background-radius: 12;"
                + "-fx-text-fill: #334155;"
        );

        content.getChildren().addAll(subject, metadata, body);

        JsonArray files = data.getAsJsonArray("attachments");
        content.getChildren().add(sectionTitle("Attachments"));

        if (files.isEmpty()) {
            content.getChildren().add(paragraph("No attachments."));
        }

        for (JsonElement element : files) {
            JsonObject file = element.getAsJsonObject();

            Label name = paragraph(
                    text(file, "file_name")
                    + " — "
                    + String.format(
                            Locale.ENGLISH,
                            "%.1f KB",
                            file.get("file_size").getAsLong() / 1024.0
                    )
            );
            HBox.setHgrow(name, Priority.ALWAYS);

            Button download = new Button("Download");
            download.getStyleClass().add("secondary-button");
            download.setOnAction(event -> download(file));

            HBox row = new HBox(12, name, download);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(12));
            row.setStyle(
                    "-fx-background-color: #F1F5F9;"
                    + "-fx-background-radius: 10;"
            );

            content.getChildren().add(row);
        }

        JsonArray links = data.getAsJsonArray("links");
        content.getChildren().add(sectionTitle("Links"));

        if (links.isEmpty()) {
            content.getChildren().add(paragraph("No links."));
        }

        for (JsonElement element : links) {
            JsonObject link = element.getAsJsonObject();
            String url = text(link, "url");

            Hyperlink button = new Hyperlink(text(link, "label"));
            button.setWrapText(true);
            button.setOnAction(event -> openLink(url));

            Hyperlink address = new Hyperlink(url);
            address.setWrapText(true);
            address.setMaxWidth(Double.MAX_VALUE);
            address.setStyle(
                    "-fx-text-fill: #2563EB;"
                    + "-fx-underline: true;"
                    + "-fx-cursor: hand;"
                    + "-fx-padding: 0;"
            );
            address.setOnAction(event -> openLink(url));

            content.getChildren().add(new VBox(4, button, address));
        }

        status.setText(
                files.size() + " attachment(s), "
                + links.size() + " link(s)."
        );
    }

    private void download(JsonObject file) {
        if (busy || !sameSession()) {
            return;
        }

        String filename = text(file, "file_name")
                .replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_")
                .replaceAll("[. ]+$", "");

        if (filename.isBlank()) {
            filename = "attachment";
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Announcement Attachment");
        chooser.setInitialFileName(filename);

        File selected = chooser.showSaveDialog(dialog);
        if (selected == null) {
            return;
        }

        if (selected.exists()
                && !AEPMDialog.showConfirmation(
                    dialog,
                    "Replace File",
                    "Replace the existing file?\n\n"
                            + selected.getAbsolutePath()
                )) {
            return;
        }

        if (!sameSession()) {
            return;
        }

        Path destination = selected.toPath().toAbsolutePath();
        String storagePath = text(file, "storage_path");
        long expectedSize = file.get("file_size").getAsLong();

        // Only allow the paths produced by this announcement uploader.
        if (!storagePath.matches(
                "[0-9a-f-]{36}/[0-9a-f-]{36}/[0-9a-f-]{36}"
                + "\\.(pdf|docx|xlsx|png|jpg|jpeg|txt)"
        ) || expectedSize < 1 || expectedSize > MAX_BYTES) {
            AEPMDialog.showError(
                    dialog, "Invalid Attachment",
                    "The saved attachment information is invalid."
            );
            return;
        }

        status.setText("Downloading attachment...");

        run(() -> {
            var client = SupabaseClient.getInstance();

            HttpRequest request = client.createAuthenticatedRequest(
                    AppConfig.STORAGE_URL
                            + "/object/authenticated/announcement-attachments/"
                            + storagePath,
                    token
            )
                    .timeout(Duration.ofSeconds(120))
                    .GET()
                    .build();

            HttpResponse<InputStream> response = client.getHttpClient().send(
                    request, HttpResponse.BodyHandlers.ofInputStream()
            );

            byte[] bytes;
            try (InputStream input = response.body()) {
                requireSuccess(response.statusCode());
                bytes = input.readNBytes(MAX_BYTES + 1);
            }

            if (bytes.length != expectedSize) {
                throw new IllegalStateException(
                        "The downloaded file is incomplete or has an unexpected size."
                );
            }

            if (!sameSession()) {
                throw new IllegalStateException(
                        "Your session changed. Please sign in again."
                );
            }

            // Write a temporary file first so a failed download does not
            // overwrite an existing file with partial content.
            Path temporary = Files.createTempFile(
                    destination.getParent(), ".aepm-", ".download"
            );

            try {
                Files.write(temporary, bytes);
                Files.move(
                        temporary,
                        destination,
                        StandardCopyOption.REPLACE_EXISTING
                );
            } finally {
                Files.deleteIfExists(temporary);
            }

            return destination.toString();
        }, savedPath -> {
            status.setText("Download complete.");

            AEPMDialog.showSuccess(
                    dialog,
                    "Download Successful",
                    "Saved to:\n" + savedPath
            );
        });
    }

    private void openLink(String value) {
        if (busy || !sameSession()) {
            return;
        }

        final URI uri;

        try {
            uri = new URI(value);

            if (uri.getHost() == null
                    || uri.getUserInfo() != null
                    || (!"https".equalsIgnoreCase(uri.getScheme())
                        && !"http".equalsIgnoreCase(uri.getScheme()))) {
                throw new IllegalArgumentException();
            }
        } catch (Exception error) {
            AEPMDialog.showError(
                    dialog, "Invalid Link",
                    "This is not a valid HTTP or HTTPS link."
            );
            return;
        }

        if (!AEPMDialog.showConfirmation(
                dialog, "Open Link",
                "Open this link in your browser?\n\n" + value
        )) {
            return;
        }

        run(() -> {
            if (!Desktop.isDesktopSupported()
                    || !Desktop.getDesktop().isSupported(
                            Desktop.Action.BROWSE
                    )) {
                throw new IllegalStateException(
                        "Opening a browser is not supported on this computer."
                );
            }

            Desktop.getDesktop().browse(uri);
            return true;
        }, ignored -> status.setText("Link opened in your browser."));
    }

    private <T> void run(
            Callable<T> work,
            java.util.function.Consumer<T> success
    ) {
        if (busy || !sameSession()) {
            return;
        }

        busy = true;
        content.setDisable(true);
        retry.setDisable(true);
        close.setDisable(true);

        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };

        task.setOnSucceeded(event -> {
            finish();

            if (sameSession() && dialog.isShowing()) {
                success.accept(task.getValue());
            } else {
                dialog.close();
            }
        });

        task.setOnFailed(event -> {
            finish();

            if (!sameSession() || !dialog.isShowing()) {
                dialog.close();
                return;
            }

            String detail = task.getException().getMessage();
            if (detail == null || detail.isBlank()) {
                detail = "Please check your connection and try again.";
            }

            status.setText("Operation failed.");

            AEPMDialog.showError(
                    dialog, "Unable to Complete", detail
            );
        });

        Thread worker = new Thread(task, "announcement-details");
        worker.setDaemon(true);
        worker.start();
    }

    private void finish() {
        busy = false;
        content.setDisable(false);
        retry.setDisable(false);
        close.setDisable(false);
    }

    private static void requireSuccess(int code) {
        if (code < 200 || code >= 300) {
            throw new IllegalStateException(
                    "The request failed (HTTP " + code
                    + "). Your session may have expired, "
                    + "or you may not have access to this announcement."
            );
        }
    }

    private static Label paragraph(String value) {
        Label label = new Label(value);
        label.setWrapText(true);
        label.setMinWidth(0);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setMinHeight(Region.USE_PREF_SIZE);
        return label;
    }

    private static Label sectionTitle(String value) {
        Label label = paragraph(value);
        label.setStyle(
                "-fx-font-size: 16px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: #0F172A;"
        );
        return label;
    }

    private static String text(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull()
                ? "" : value.getAsString();
    }

    public static void showScheduled(Window owner, String scheduleId) {
        JsonObject query = new JsonObject();
        query.addProperty("p_schedule_id", scheduleId);
        open(owner, query);
    }

    private static String formatDetailTime(String value) {
        if (value == null || value.isBlank()) {
            return "—";
        }

        try {
            return OffsetDateTime.parse(value)
                    .atZoneSameInstant(ZoneId.of("Asia/Kuala_Lumpur"))
                    .format(DateTimeFormatter.ofPattern(
                            "dd MMM yyyy HH:mm", Locale.ENGLISH
                    ));
        } catch (RuntimeException error) {
            return value;
        }
    }
}