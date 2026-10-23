package com.aepm.ui.admin;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;
import com.aepm.ui.common.AEPMDialog;
import com.aepm.util.SessionManager;
import com.google.gson.*;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public final class AdminNotificationTemplatesPage {

    private final Stage stage;
    private final BorderPane shell;
    private final BorderPane page = new BorderPane();

    private final Object session = SessionManager.getAuthSession();
    private final String token = SessionManager.getAccessToken();
    private final String userId =
            SessionManager.getCurrentUser().getUserId();

    private final ObservableList<JsonObject> rows =
            FXCollections.observableArrayList();

    private final TableView<JsonObject> table = new TableView<>();
    private final TextField search = new TextField();

    private final TextField name = new TextField();
    private final TextField type = new TextField();
    private final TextField title = new TextField();
    private final TextArea message = new TextArea();
    private final ComboBox<String> state = new ComboBox<>();

    private final Label editorTitle = new Label("New Template");
    private final Label status = new Label();

    private final Button refresh = new Button("Refresh");
    private final Button create = new Button("+ New Template");
    private final Button edit = new Button("Edit Selected");
    private final Button save = new Button("Save Template");
    private final Button delete = new Button("Delete Template");
    private final Button preview = new Button("Preview");

    private final VBox form = new VBox(10);

    private JsonObject editing;
    private boolean busy;
    private boolean dirty;
    private boolean filling;
    private boolean needsReload;

    public static void show(Window owner) {
        var user = SessionManager.getCurrentUser();

        if (user == null || user.getRoleId() != 1L
                || SessionManager.getAccessToken() == null) {
            AEPMDialog.showError(
                    owner,
                    "Access Denied",
                    "Please sign in with an administrator account."
            );
            return;
        }

        if (!(owner instanceof Stage stage)
                || !(stage.getScene().getRoot() instanceof BorderPane shell)) {
            AEPMDialog.showError(
                    owner, "Unable to Open",
                    "Open this page from the administrator menu."
            );
            return;
        }

        new AdminNotificationTemplatesPage(stage, shell).open();
    }

    private AdminNotificationTemplatesPage(
            Stage stage, BorderPane shell
    ) {
        this.stage = stage;
        this.shell = shell;

        page.setId("notificationTemplatesPage");
        page.setPadding(new Insets(28));
        page.setStyle("-fx-background-color: #F6F8FC;");

        Label heading = new Label("Notification Center");
        heading.setStyle(
                "-fx-font-size: 28px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: #0F172A;"
        );

        Label subtitle = label(
                "Create and maintain reusable notification titles and messages."
        );
        subtitle.getStyleClass().add("secondary-text");

        create.getStyleClass().add("login-primary-button");
        refresh.getStyleClass().add("secondary-button");
        edit.getStyleClass().add("secondary-button");
        save.getStyleClass().add("login-primary-button");
        delete.getStyleClass().add("secondary-button");
        preview.getStyleClass().add("secondary-button");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox headingRow = new HBox(12, heading, spacer, create);
        headingRow.setAlignment(Pos.CENTER_LEFT);

        VBox header = new VBox(8, headingRow, subtitle);
        header.setPadding(new Insets(0, 0, 22, 0));
        page.setTop(header);
        NotificationCenterNavigation.install(
                page,
                stage,
                "Templates",
                () -> {
                    if (!active()) {
                        return false;
                    }

                    if (busy) {
                        AEPMDialog.showError(
                                stage,
                                "Please Wait",
                                "Wait for the current request to finish."
                        );
                        return false;
                    }

                    return discardConfirmed();
                }
        );
        search.setPromptText("Search name, notification type or status");

        FilteredList<JsonObject> filtered = new FilteredList<>(rows);
        table.setItems(filtered);

        search.textProperty().addListener((obs, old, value) -> {
            String query = value == null
                    ? "" : value.trim().toLowerCase(Locale.ROOT);

            filtered.setPredicate(row ->
                    (
                        text(row, "template_name") + " "
                        + text(row, "notification_type") + " "
                        + text(row, "status")
                    ).toLowerCase(Locale.ROOT).contains(query)
            );
        });

        addColumn("Template", "template_name", 210);
        addColumn("Type", "notification_type", 180);
        addColumn("Status", "status", 90);

        table.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );
        table.setPlaceholder(new Label("No templates loaded."));
        table.setMinHeight(260);

        table.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, value) -> updateControls());

        table.setRowFactory(view -> {
            TableRow<JsonObject> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    table.getSelectionModel().select(row.getItem());
                    editSelected();
                }
            });
            return row;
        });

        VBox list = new VBox(
                12,
                label("Latest 200 templates"),
                search,
                new HBox(10, refresh, edit),
                table
        );
        card(list);
        VBox.setVgrow(table, Priority.ALWAYS);

        name.setPromptText("For example: Meeting announcement");
        type.setPromptText("For example: SYSTEM_ANNOUNCEMENT");
        title.setPromptText("Notification title");
        message.setPromptText("Notification message");
        message.setWrapText(true);
        message.setPrefRowCount(7);

        state.getItems().setAll("Active", "Inactive");
        state.setValue("Active");
        state.setMaxWidth(Double.MAX_VALUE);

        editorTitle.setStyle(
                "-fx-font-size: 20px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: #0F172A;"
        );

        Label typeHint = label(
                "Use an uppercase notification type, for example "
                + "SYSTEM_ANNOUNCEMENT. A template is used only after "
                + "a notification workflow is connected to it."
        );
        typeHint.getStyleClass().add("secondary-text");

        form.getChildren().addAll(
                editorTitle,
                label("Template Name"), name,
                label("Notification Type"), type,
                typeHint,
                label("Title"), title,
                label("Message"), message,
                label("Status"), state,
                new FlowPane(10, 10, preview, save, delete)
        );
        card(form);

        ScrollPane formScroll = new ScrollPane(form);
        formScroll.setFitToWidth(true);
        formScroll.setMinWidth(320);
        formScroll.setStyle(
                "-fx-background-color: transparent;"
                + "-fx-background: transparent;"
                + "-fx-border-color: transparent;"
        );

        SplitPane split = new SplitPane(list, formScroll);
        split.setDividerPositions(0.52);
        split.setStyle(
                "-fx-background-color: transparent;"
                + "-fx-padding: 0;"
        );
        page.setCenter(split);

        status.setWrapText(true);
        status.setPadding(new Insets(14, 0, 0, 0));
        status.setStyle("-fx-text-fill: #64748B;");
        page.setBottom(status);

        name.textProperty().addListener((o, a, b) -> changed());
        type.textProperty().addListener((o, a, b) -> changed());
        title.textProperty().addListener((o, a, b) -> changed());
        message.textProperty().addListener((o, a, b) -> changed());
        state.valueProperty().addListener((o, a, b) -> changed());

        create.setOnAction(event -> {
            if (discardConfirmed()) {
                fill(null);
            }
        });
        edit.setOnAction(event -> editSelected());
        refresh.setOnAction(event -> {
            if (discardConfirmed()) {
                load();
            }
        });
        save.setOnAction(event -> saveTemplate());
        delete.setOnAction(event -> deleteTemplate());
        preview.setOnAction(event -> previewTemplate());
    }

    private void open() {
        shell.setCenter(page);

        if (shell.getLeft() != null) {
            for (Node node : shell.getLeft().lookupAll(".button")) {
                if (node instanceof Button button
                        && (button.getStyleClass().contains("sidebar-button")
                            || button.getStyleClass().contains(
                                    "sidebar-button-active"))) {
                    button.getStyleClass().remove("sidebar-button-active");

                    if (!button.getStyleClass().contains("sidebar-button")) {
                        button.getStyleClass().add("sidebar-button");
                    }

                    if (button.getText().contains("Notification/Announcements")) {
                        button.getStyleClass().add("sidebar-button-active");
                    }
                }
            }
        }

        fill(null);
        load();
    }

    private void changed() {
        if (!filling) {
            dirty = true;
            updateControls();
        }
    }

    private boolean discardConfirmed() {
        return !busy && (!dirty || AEPMDialog.showConfirmation(
                stage,
                "Discard Changes",
                "Discard the unsaved changes in this template?"
        ));
    }

    private void editSelected() {
        JsonObject selected = table.getSelectionModel().getSelectedItem();

        if (selected != null && !needsReload && discardConfirmed()) {
            fill(selected);
        }
    }

    private void fill(JsonObject row) {
        filling = true;
        editing = row == null ? null : row.deepCopy();

        name.setText(row == null ? "" : text(row, "template_name"));
        type.setText(row == null ? "" : text(row, "notification_type"));
        title.setText(row == null ? "" : text(row, "title_template"));
        message.setText(row == null ? "" : text(row, "message_template"));
        state.setValue(row == null ? "Active" : text(row, "status"));

        editorTitle.setText(
                row == null ? "New Template" : "Edit Template"
        );
        save.setText(row == null ? "Save Template" : "Save Changes");

        dirty = false;
        filling = false;
        updateControls();
    }

    private JsonObject validatedForm() {
        String templateName = name.getText().trim();
        String notificationType =
                type.getText().trim().toUpperCase(Locale.ROOT);
        String templateTitle = title.getText().trim();
        String templateMessage = message.getText().trim();

        if (!lengthBetween(templateName, 1, 100)
                || !notificationType.matches("[A-Z][A-Z0-9_]{0,49}")
                || !lengthBetween(templateTitle, 1, 150)
                || templateMessage.isBlank()
                || !("Active".equals(state.getValue())
                     || "Inactive".equals(state.getValue()))) {
            AEPMDialog.showError(
                    stage,
                    "Check Template",
                    "Template name: 1–100 characters.\n"
                    + "Notification type: up to 50 uppercase letters, "
                    + "digits or underscores, starting with a letter.\n"
                    + "Title: 1–150 characters.\n"
                    + "Message: required.\n"
                    + "Status: Active or Inactive."
            );
            return null;
        }

        JsonObject body = new JsonObject();
        body.addProperty("template_name", templateName);
        body.addProperty("notification_type", notificationType);
        body.addProperty("title_template", templateTitle);
        body.addProperty("message_template", templateMessage);
        body.addProperty("status", state.getValue());
        return body;
    }

    private void saveTemplate() {
        if (busy || needsReload || !active()) {
            return;
        }

        JsonObject body = validatedForm();
        if (body == null) {
            return;
        }

        boolean adding = editing == null;

        if (!AEPMDialog.showConfirmation(
                stage,
                adding ? "Create Template" : "Save Changes",
                (adding ? "Create" : "Update") + " template \""
                        + body.get("template_name").getAsString() + "\"?"
        )) {
            return;
        }

        if (!active()) {
            return;
        }

        String path = "/notification_template_tbl";

        if (adding) {
            body.addProperty("created_by", userId);
        } else {
            path += versionFilter(editing);
        }

        body.addProperty("updated_at", OffsetDateTime.now().toString());

        final String endpoint = path;
        status.setText("Saving template...");

        run(
                () -> request(
                        adding ? "POST" : "PATCH", endpoint, body
                ),
                result -> {
                    if (result.size() != 1) {
                        requireReload(
                                "The template changed or is no longer available. "
                                + "Refresh before editing again."
                        );
                        return;
                    }

                    JsonObject saved = result.get(0).getAsJsonObject();
                    rows.removeIf(row ->
                            text(row, "template_id").equals(
                                    text(saved, "template_id"))
                    );
                    rows.add(0, saved);
                    if (rows.size() > 200) {
                        rows.remove(rows.size() - 1);
                    }
                    fill(saved);

                    status.setText("Template saved.");
                    AEPMDialog.showSuccess(
                            stage, "Template Saved",
                            "The notification template has been saved."
                    );
                },
                error -> requireReload(
                        error.getMessage()
                        + "\n\nRefresh to check the saved state before trying again."
                )
        );
    }

    private void deleteTemplate() {
        if (busy || needsReload || editing == null || !active()) {
            return;
        }

        JsonObject target = editing.deepCopy();

        if (!AEPMDialog.showConfirmation(
                stage,
                "Delete Template",
                "Delete \"" + text(target, "template_name") + "\"?"
                + "\nTemplates referenced by notification rules cannot be deleted."
                + (dirty ? "\nUnsaved edits will also be discarded." : "")
        )) {
            return;
        }

        if (!active()) {
            return;
        }

        status.setText("Deleting template...");

        run(
                () -> request(
                        "DELETE",
                        "/notification_template_tbl" + versionFilter(target),
                        null
                ),
                result -> {
                    if (result.size() != 1) {
                        requireReload(
                                "The template changed or is no longer available. "
                                + "Refresh before trying again."
                        );
                        return;
                    }

                    rows.removeIf(row ->
                            text(row, "template_id").equals(
                                    text(target, "template_id"))
                    );
                    fill(null);
                    status.setText("Template deleted.");

                    AEPMDialog.showSuccess(
                            stage, "Template Deleted",
                            "The template has been deleted."
                    );
                },
                error -> requireReload(error.getMessage())
        );
    }

    private void previewTemplate() {
        JsonObject body = validatedForm();
        if (body == null) {
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(stage);
        dialog.setTitle("Notification Template Preview");

        Label heading = label(text(body, "title_template"));
        heading.setStyle(
                "-fx-font-size: 20px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: #0F172A;"
        );

        Label content = label(text(body, "message_template"));
        Label note = label(
                "Text preview only. Placeholder values are not substituted."
        );
        note.getStyleClass().add("secondary-text");

        VBox box = new VBox(16, heading, content, note);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: white;");

        ScrollPane scroll = new ScrollPane(box);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportWidth(540);
        scroll.setPrefViewportHeight(320);

        dialog.getDialogPane().setContent(scroll);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        var css = getClass().getResource("/css/app.css");
        if (css != null) {
            dialog.getDialogPane().getStylesheets().add(css.toExternalForm());
        }

        dialog.showAndWait();
    }

    private void load() {
        status.setText("Loading templates...");

        run(
                () -> request(
                        "GET",
                        "/notification_template_tbl"
                        + "?select=*&order=template_id.desc&limit=200",
                        null
                ),
                result -> {
                    rows.clear();
                    for (JsonElement item : result) {
                        rows.add(item.getAsJsonObject());
                    }

                    needsReload = false;
                    fill(null);
                    table.setPlaceholder(new Label("No matching templates."));
                    status.setText(
                            rows.size() + " template(s) loaded. "
                            + "Showing the latest 200 at most."
                    );
                },
                error -> {
                    needsReload = true;
                    updateControls();
                    status.setText("Unable to refresh; displayed data may be outdated.");
                    AEPMDialog.showError(
                            stage, "Unable to Load Templates", error.getMessage()
                    );
                }
        );
    }

    private JsonArray request(
            String method, String path, JsonObject body
    ) throws Exception {
        var client = SupabaseClient.getInstance();

        HttpRequest.Builder builder = client.createAuthenticatedRequest(
                AppConfig.REST_URL + path, token
        ).timeout(Duration.ofSeconds(30));

        if (!"GET".equals(method)) {
            builder.header("Prefer", "return=representation");
        }

        builder.method(
                method,
                body == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body.toString())
        );

        HttpResponse<String> response = client.getHttpClient().send(
                builder.build(), HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String code = "";

            try {
                code = text(
                        JsonParser.parseString(response.body()).getAsJsonObject(),
                        "code"
                );
            } catch (RuntimeException ignored) {
            }

            String detail = switch (code) {
                case "23505" ->
                    "A template with this name already exists.";
                case "23503" ->
                    "This template is used by a notification rule. "
                    + "Update the rule before deleting the template.";
                case "42501" ->
                    "Access denied. An active administrator account is required.";
                default ->
                    "The request failed (HTTP " + response.statusCode() + ").";
            };

            throw new IllegalStateException(detail);
        }

        return JsonParser.parseString(response.body()).getAsJsonArray();
    }

    private <T> void run(
            Callable<T> work,
            Consumer<T> success,
            Consumer<Throwable> failure
    ) {
        if (busy || !active()) {
            return;
        }

        busy = true;
        updateControls();

        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };

        task.setOnSucceeded(event -> {
            busy = false;
            updateControls();
            if (active()) {
                success.accept(task.getValue());
            }
        });

        task.setOnFailed(event -> {
            busy = false;
            updateControls();
            if (active()) {
                failure.accept(task.getException());
            }
        });

        Thread worker = new Thread(task, "notification-templates");
        worker.setDaemon(true);
        worker.start();
    }

    private void requireReload(String detail) {
        needsReload = true;
        updateControls();
        status.setText("Refresh is required before making more changes.");

        AEPMDialog.showError(
                stage, "Unable to Confirm Change", detail
        );
    }

    private void updateControls() {
        form.setDisable(busy || needsReload);
        create.setDisable(busy || needsReload);
        refresh.setDisable(busy);
        table.setDisable(busy);
        search.setDisable(busy);

        edit.setDisable(
                busy || needsReload
                || table.getSelectionModel().getSelectedItem() == null
        );
        delete.setDisable(editing == null);
        save.setDisable(!dirty);
    }

    private boolean active() {
        var user = SessionManager.getCurrentUser();

        return stage.isShowing()
                && stage.getScene().getRoot() == shell
                && shell.getCenter() == page
                && SessionManager.getAuthSession() == session
                && user != null
                && user.getRoleId() == 1L
                && userId.equals(user.getUserId());
    }

    private static String versionFilter(JsonObject row) {
        return "?template_id=eq." + text(row, "template_id")
                + "&updated_at=eq."
                + URLEncoder.encode(
                        text(row, "updated_at"),
                        StandardCharsets.UTF_8
                );
    }

    private void addColumn(String caption, String key, double width) {
        TableColumn<JsonObject, String> column = new TableColumn<>(caption);
        column.setPrefWidth(width);
        column.setCellValueFactory(cell ->
                new SimpleStringProperty(text(cell.getValue(), key))
        );
        table.getColumns().add(column);
    }

    private static void card(VBox box) {
        box.setPadding(new Insets(20));
        box.setStyle(
                "-fx-background-color: white;"
                + "-fx-background-radius: 16;"
                + "-fx-border-color: #E1E6EF;"
                + "-fx-border-radius: 16;"
        );
    }

    private static Label label(String value) {
        Label label = new Label(value);
        label.setWrapText(true);
        label.setMinWidth(0);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setMinHeight(Region.USE_PREF_SIZE);
        return label;
    }

    private static boolean lengthBetween(String value, int min, int max) {
        int length = value.codePointCount(0, value.length());
        return length >= min && length <= max;
    }

    private static String text(JsonObject row, String key) {
        JsonElement value = row.get(key);
        return value == null || value.isJsonNull()
                ? "" : value.getAsString();
    }
}