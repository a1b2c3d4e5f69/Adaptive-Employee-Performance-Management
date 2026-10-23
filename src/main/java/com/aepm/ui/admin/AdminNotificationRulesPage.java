package com.aepm.ui.admin;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;
import com.aepm.ui.common.AEPMDialog;
import com.aepm.util.SessionManager;
import com.google.gson.*;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public final class AdminNotificationRulesPage {

    private final Stage stage;
    private final BorderPane shell;
    private final BorderPane page = new BorderPane();

    private final Object session = SessionManager.getAuthSession();
    private final String token = SessionManager.getAccessToken();

    private final ComboBox<JsonObject> ruleChoice = new ComboBox<>();
    private final ComboBox<JsonObject> templateChoice = new ComboBox<>();
    private final ComboBox<String> recipient = new ComboBox<>();
    private final ComboBox<String> state = new ComboBox<>();

    private final TextField name = new TextField();
    private final TextField hours = new TextField();
    private final VBox form = new VBox(12);
    private final Label status = new Label();

    private final Button refresh = new Button("Refresh");
    private final Button save = new Button("Save Changes");

    private JsonObject editing;
    private boolean busy;
    private boolean filling;
    private boolean dirty;
    private boolean needsReload;

    public static void show(Window owner) {
        var user = SessionManager.getCurrentUser();

        if (user == null || user.getRoleId() != 1L
                || SessionManager.getAccessToken() == null) {
            AEPMDialog.showError(
                    owner, "Access Denied",
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

        new AdminNotificationRulesPage(stage, shell).open();
    }

    private AdminNotificationRulesPage(
            Stage stage, BorderPane shell
    ) {
        this.stage = stage;
        this.shell = shell;

        page.setPadding(new Insets(28));
        page.setStyle("-fx-background-color: #F6F8FC;");

        Label heading = label("Notification Center");
        heading.setStyle(
                "-fx-font-size: 28px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: #0F172A;"
        );

        Label subtitle = label(
                "Configure notification templates, reminder timing and recipients."
        );
        subtitle.getStyleClass().add("secondary-text");

        VBox header = new VBox(8, heading, subtitle);
        header.setPadding(new Insets(0, 0, 22, 0));
        page.setTop(header);

        NotificationCenterNavigation.install(
                page, stage, "Automated Rules", this::canLeave
        );

        ruleChoice.setMaxWidth(Double.MAX_VALUE);
        ruleChoice.setPromptText("Select a reminder rule");
        ruleChoice.setConverter(converter("rule_name", false));

        templateChoice.setMaxWidth(Double.MAX_VALUE);
        templateChoice.setPromptText("Select a TASK_REMINDER template");
        templateChoice.setConverter(converter("template_name", true));

        recipient.getItems().setAll("Employee", "Manager", "Both");
        state.getItems().setAll("Active", "Inactive");

        recipient.setMaxWidth(Double.MAX_VALUE);
        state.setMaxWidth(Double.MAX_VALUE);

        name.setPromptText("Rule name — maximum 100 characters");
        hours.setPromptText("For example: 24");

        Label event = label("Task deadline approaching (TASK_DUE_SOON)");
        event.setStyle(
                "-fx-background-color: #F1F5F9;"
                + "-fx-background-radius: 10;"
                + "-fx-padding: 12;"
                + "-fx-text-fill: #334155;"
        );

        Label timingHint = label(
                "Enter 1–720 hours before the task deadline. "
                + "The existing scheduler checks approximately every 5 minutes."
        );
        timingHint.getStyleClass().add("secondary-text");

        Label recipientHint = label(
                "Employee: assigned employee. "
                + "Manager: the manager who assigned the task. "
                + "Both: notify both recipients."
        );
        recipientHint.getStyleClass().add("secondary-text");

        Label implementationNote = label(
            "Active rules are checked approximately every 5 minutes "
            + "while an administrator is signed in and the application is running. "
            + "The existing due-today reminders continue independently."
        );
        implementationNote.setStyle(
                "-fx-background-color: #FFF7DB;"
                + "-fx-background-radius: 10;"
                + "-fx-padding: 12;"
                + "-fx-text-fill: #8A6116;"
        );

        save.getStyleClass().add("login-primary-button");
        refresh.getStyleClass().add("secondary-button");

        form.getChildren().addAll(
                label("Rule Name"), name,
                label("Trigger Event"), event,
                label("Notification Template"), templateChoice,
                label("Remind Before Deadline (Hours)"), hours,
                timingHint,
                label("Recipients"), recipient,
                recipientHint,
                label("Status"), state,
                save
        );

        VBox card = new VBox(
                16,
                implementationNote,
                label("Reminder Rules — latest 200"),
                ruleChoice,
                refresh,
                new Separator(),
                form
        );
        card.setPadding(new Insets(24));
        card.setStyle(
                "-fx-background-color: white;"
                + "-fx-background-radius: 16;"
                + "-fx-border-color: #E1E6EF;"
                + "-fx-border-radius: 16;"
        );

        ScrollPane scroll = new ScrollPane(card);
        scroll.setFitToWidth(true);
        scroll.setStyle(
                "-fx-background-color: transparent;"
                + "-fx-background: transparent;"
                + "-fx-border-color: transparent;"
        );
        page.setCenter(scroll);

        status.setWrapText(true);
        status.setPadding(new Insets(14, 0, 0, 0));
        page.setBottom(status);

        name.textProperty().addListener((o, a, b) -> changed());
        hours.textProperty().addListener((o, a, b) -> changed());
        templateChoice.valueProperty().addListener((o, a, b) -> changed());
        recipient.valueProperty().addListener((o, a, b) -> changed());
        state.valueProperty().addListener((o, a, b) -> changed());

        ruleChoice.valueProperty().addListener((o, old, selected) -> {
            if (filling) {
                return;
            }

            if (!canLeave()) {
                filling = true;
                ruleChoice.setValue(old);
                filling = false;
                return;
            }

            fill(selected);
        });

        refresh.setOnAction(eventAction -> {
            if (canLeave()) {
                load();
            }
        });

        save.setOnAction(eventAction -> saveRule());
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

        load();
    }

    private void changed() {
        if (!filling) {
            dirty = true;
            updateControls();
        }
    }

    private boolean canLeave() {
        if (!active()) {
            return false;
        }

        if (busy) {
            AEPMDialog.showError(
                    stage, "Please Wait",
                    "Wait for the current request to finish."
            );
            return false;
        }

        return !dirty || AEPMDialog.showConfirmation(
                stage, "Discard Changes",
                "Discard the unsaved changes to this rule?"
        );
    }

    private void fill(JsonObject row) {
        filling = true;
        editing = row == null ? null : row.deepCopy();

        name.setText(row == null ? "" : text(row, "rule_name"));
        hours.setText(row == null ? "" : text(row, "send_before_hours"));
        recipient.setValue(row == null ? null : text(row, "recipient_role"));
        state.setValue(row == null ? "Inactive" : text(row, "status"));

        templateChoice.setValue(null);

        if (row != null) {
            for (JsonObject template : templateChoice.getItems()) {
                if (text(template, "template_id").equals(
                        text(row, "template_id"))) {
                    templateChoice.setValue(template);
                    break;
                }
            }
        }

        dirty = false;
        filling = false;
        updateControls();
    }

    private void load() {
        status.setText("Loading rules and templates...");

        run(() -> {
            JsonObject result = new JsonObject();

            result.add("rules", request(
                    "GET",
                    "/notification_rule_tbl"
                    + "?select=*&event_type=eq.TASK_DUE_SOON"
                    + "&order=notification_rule_id.desc&limit=200",
                    null
            ));

            result.add("templates", request(
                    "GET",
                    "/notification_template_tbl"
                    + "?select=*&notification_type=eq.TASK_REMINDER"
                    + "&order=template_id.desc&limit=200",
                    null
            ));

            return result;
        }, result -> {
            filling = true;

            ruleChoice.getItems().clear();
            templateChoice.getItems().clear();

            for (JsonElement item : result.getAsJsonArray("templates")) {
                templateChoice.getItems().add(item.getAsJsonObject());
            }

            for (JsonElement item : result.getAsJsonArray("rules")) {
                ruleChoice.getItems().add(item.getAsJsonObject());
            }

            JsonObject first = ruleChoice.getItems().isEmpty()
                    ? null : ruleChoice.getItems().getFirst();

            ruleChoice.setValue(first);
            filling = false;

            needsReload = false;
            fill(first);

            status.setText(
                    ruleChoice.getItems().size() + " rule(s), "
                    + templateChoice.getItems().size() + " template(s) loaded."
            );
        });
    }

    private void saveRule() {
        if (busy || needsReload || editing == null || !active()) {
            return;
        }

        String ruleName = name.getText().trim();
        JsonObject template = templateChoice.getValue();
        int beforeHours;

        try {
            beforeHours = Integer.parseInt(hours.getText().trim());

            if (beforeHours < 1 || beforeHours > 720) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException error) {
            AEPMDialog.showError(
                    stage, "Invalid Reminder Time",
                    "Enter a whole number between 1 and 720 hours."
            );
            return;
        }

        if (ruleName.isBlank()
                || ruleName.codePointCount(0, ruleName.length()) > 100
                || template == null
                || !recipient.getItems().contains(recipient.getValue())
                || !state.getItems().contains(state.getValue())) {
            AEPMDialog.showError(
                    stage, "Check Rule",
                    "Enter a name of 1–100 characters, select a template, "
                    + "recipient option and status."
            );
            return;
        }

        if ("Active".equals(state.getValue())
                && !"Active".equals(text(template, "status"))) {
            AEPMDialog.showError(
                    stage, "Inactive Template",
                    "An active rule must use an active template."
            );
            return;
        }

        if (!AEPMDialog.showConfirmation(
                stage,
                "Save Notification Rule",
                "Save \"" + ruleName + "\"?"
                + "\n\nRemind before: " + beforeHours + " hours"
                + "\nRecipients: " + recipient.getValue()
                + "\nTemplate: " + text(template, "template_name")
                + "\nStatus: " + state.getValue()
        )) {
            return;
        }

        if (!active()) {
            return;
        }

        JsonObject body = new JsonObject();
        body.addProperty("rule_name", ruleName);
        body.addProperty("event_type", "TASK_DUE_SOON");
        body.addProperty("template_id", template.get("template_id").getAsLong());
        body.addProperty("send_before_hours", beforeHours);
        body.addProperty("recipient_role", recipient.getValue());
        body.addProperty("status", state.getValue());
        body.addProperty("updated_at", OffsetDateTime.now().toString());

        String path = "/notification_rule_tbl"
                + "?notification_rule_id=eq."
                + text(editing, "notification_rule_id")
                + "&updated_at=eq."
                + URLEncoder.encode(
                        text(editing, "updated_at"),
                        StandardCharsets.UTF_8
                );

        status.setText("Saving rule...");

        run(() -> {
            JsonArray result = request("PATCH", path, body);

            if (result.size() != 1) {
                throw new IllegalStateException(
                        "The rule changed or is unavailable. Refresh and try again."
                );
            }

            return result.get(0).getAsJsonObject();
        }, saved -> {
            filling = true;

            for (int i = 0; i < ruleChoice.getItems().size(); i++) {
                if (text(ruleChoice.getItems().get(i), "notification_rule_id")
                        .equals(text(saved, "notification_rule_id"))) {
                    ruleChoice.getItems().set(i, saved);
                    break;
                }
            }

            ruleChoice.setValue(saved);
            filling = false;
            fill(saved);

            status.setText("Rule settings saved.");

            AEPMDialog.showSuccess(
                    stage, "Rule Saved",
                    "The notification rule settings have been saved."
            );
        });
    }

    private JsonArray request(
            String method, String path, JsonObject body
    ) throws Exception {
        var client = SupabaseClient.getInstance();

        HttpRequest.Builder builder = client.createAuthenticatedRequest(
                AppConfig.REST_URL + path, token
        ).timeout(Duration.ofSeconds(30));

        if ("PATCH".equals(method)) {
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
            throw new IllegalStateException(
                    "The request failed (HTTP " + response.statusCode() + ")."
            );
        }

        return JsonParser.parseString(response.body()).getAsJsonArray();
    }

    private <T> void run(Callable<T> work, Consumer<T> success) {
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
            needsReload = true;
            updateControls();

            if (active()) {
                status.setText("Refresh before making further changes.");

                AEPMDialog.showError(
                        stage,
                        "Unable to Complete",
                        String.valueOf(task.getException().getMessage())
                        + "\n\nRefresh to check the saved state before retrying."
                );
            }
        });

        Thread worker = new Thread(task, "notification-rules");
        worker.setDaemon(true);
        worker.start();
    }

    private void updateControls() {
        refresh.setDisable(busy);
        ruleChoice.setDisable(busy || needsReload);
        form.setDisable(busy || needsReload || editing == null);
        save.setDisable(!dirty);
    }

    private boolean active() {
        var user = SessionManager.getCurrentUser();

        return stage.isShowing()
                && stage.getScene().getRoot() == shell
                && shell.getCenter() == page
                && SessionManager.getAuthSession() == session
                && user != null
                && user.getRoleId() == 1L;
    }

    private static StringConverter<JsonObject> converter(
            String key, boolean includeStatus
    ) {
        return new StringConverter<>() {
            @Override
            public String toString(JsonObject row) {
                if (row == null) {
                    return "";
                }

                return text(row, key)
                        + (includeStatus
                            ? " (" + text(row, "status") + ")"
                            : "");
            }

            @Override
            public JsonObject fromString(String value) {
                return null;
            }
        };
    }

    private static Label label(String value) {
        Label label = new Label(value);
        label.setWrapText(true);
        label.setMinWidth(0);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setMinHeight(Region.USE_PREF_SIZE);
        label.setStyle("-fx-text-fill: #334155;");
        return label;
    }

    private static String text(JsonObject row, String key) {
        JsonElement value = row.get(key);
        return value == null || value.isJsonNull()
                ? "" : value.getAsString();
    }
}