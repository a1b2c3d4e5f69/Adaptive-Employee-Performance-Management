package com.aepm.ui.admin;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;
import com.aepm.ui.common.AEPMDialog;
import com.aepm.ui.common.AnnouncementDetailsDialog;
import com.aepm.util.SessionManager;
import com.google.gson.*;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public final class AdminAnnouncementsDialog {

    private static final Map<String, JsonObject> PENDING =
            new HashMap<>();

    private static final int MAX_FILE_BYTES =
            10 * 1024 * 1024;


    // =========================================================
    // PAGE / SESSION
    // =========================================================

    private final Stage stage;

    private final BorderPane shell;

    private final BorderPane page =
            new BorderPane();

    private final Object session =
            SessionManager.getAuthSession();

    private final String userId;

    private final String token;


    // =========================================================
    // COMPOSE
    // =========================================================

    private final ComboBox<String> audience =
            new ComboBox<>();

    private final TextField search =
            new TextField();

    private final TextField subject =
            new TextField();

    private final TextArea message =
            new TextArea();

    private final Label selectedLabel =
            new Label("Selected: 0 users");

    private final Label status =
            new Label();

    private final Button send =
            new Button("Send Announcement");

    private final Button refresh =
            new Button("Refresh Sent");

    private final VBox composer =
            new VBox(12);


    // =========================================================
    // RECIPIENTS
    // =========================================================

    private final ObservableList<Recipient> users =
            FXCollections.observableArrayList();


    // =========================================================
    // SENT
    // =========================================================

    private final TableView<JsonObject> sentTable =
            new TableView<>();


    // =========================================================
    // SCHEDULED
    // =========================================================

    private final TableView<JsonObject> scheduledTable =
            new TableView<>();

    private final Button refreshScheduled =
            new Button("Refresh Scheduled");

    private final Button cancelScheduled =
            new Button("Cancel Schedule");

    private final Label scheduledSummary =
            new Label(
                    "Open this tab to load your latest 100 scheduled announcements."
            );

    private final TextArea scheduledError =
            new TextArea();

    private final DatePicker scheduleDate =
            new DatePicker();

    private final TextField scheduleTime =
            new TextField();

    private final Button scheduleButton =
            new Button("Schedule");

    private final Button viewScheduledDetails =
            new Button("View Details");

    private final Button rescheduleButton =
            new Button("Reschedule");


    // =========================================================
    // ATTACHMENTS / LINKS
    // =========================================================

    private String draftAnnouncementId =
            UUID.randomUUID().toString();

    private JsonArray attachments =
            new JsonArray();

    private JsonArray announcementLinks =
            new JsonArray();

    private final VBox attachmentRows =
            new VBox(8);

    private final VBox linkRows =
            new VBox(8);

    private final TextField linkLabelInput =
            new TextField();

    private final TextField linkUrlInput =
            new TextField();

    private final VBox linkEditor =
            new VBox(10);


    // =========================================================
    // STATE
    // =========================================================

    private boolean busy;

    private boolean recipientsLoaded;


    // =========================================================
    // PENDING CHECK
    // =========================================================

    private boolean hasPendingSchedule() {

        JsonObject pending =
                PENDING.get(userId);

        return pending != null
                && pending.has("p_scheduled_at");
    }


    // =========================================================
    // RECIPIENT MODEL
    // =========================================================

    private static final class Recipient {

        final String id;

        final String display;

        final BooleanProperty selected =
                new SimpleBooleanProperty(false);


        Recipient(JsonObject row) {

            id =
                    row.get("user_id")
                            .getAsString();


            String role =
                    row.get("role_id")
                                    .getAsLong() == 2L
                            ? "Manager"
                            : "Employee";


            display =
                    text(row, "full_name")
                            + "  |  "
                            + text(row, "employee_id")
                            + "  |  "
                            + role
                            + "  |  "
                            + text(row, "department");
        }


        @Override
        public String toString() {

            return display;
        }
    }


    // =========================================================
    // HTTP FAILURE
    // =========================================================

    private static final class HttpFailure
            extends Exception {

        final int statusCode;


        HttpFailure(int statusCode) {

            super(
                    "The server rejected the request (HTTP "
                            + statusCode
                            + ")."
            );

            this.statusCode =
                    statusCode;
        }
    }


    // =========================================================
    // SHOW
    // =========================================================

    public static void show(Window owner) {

        var user =
                SessionManager.getCurrentUser();


        if (
                user == null
                        || user.getRoleId() != 1L
                        || SessionManager.getAccessToken() == null
        ) {

            AEPMDialog.showError(
                    owner,
                    "Access Denied",
                    "Please sign in with an administrator account."
            );

            return;
        }


        new AdminAnnouncementsDialog(
                owner,
                user.getUserId(),
                SessionManager.getAccessToken()
        ).open();
    }


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    private AdminAnnouncementsDialog(
            Window owner,
            String userId,
            String token
    ) {

        this.userId =
                userId;

        this.token =
                token;


        if (
                !(owner instanceof Stage mainStage)
                        || !(
                        mainStage
                                .getScene()
                                .getRoot()
                                instanceof BorderPane layout
                )
        ) {

            throw new IllegalStateException(
                    "System Announcements must open from the administrator page."
            );
        }


        this.stage =
                mainStage;

        this.shell =
                layout;


        page.setId(
                "systemAnnouncementsPage"
        );


        // =====================================================
        // AUDIENCE
        // =====================================================

        audience
                .getItems()
                .setAll(
                        "Managers",
                        "Employees",
                        "Both",
                        "Selected"
                );

        audience.setValue(
                "Selected"
        );

        audience.setMaxWidth(
                Double.MAX_VALUE
        );


        // =====================================================
        // SEARCH
        // =====================================================

        search.setPromptText(
                "Optional: filter by name, employee number, role or department"
        );


        FilteredList<Recipient> filtered =
                new FilteredList<>(
                        users
                );


        search
                .textProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                value
                        ) -> {

                            String query =
                                    value == null
                                            ? ""
                                            : value
                                            .trim()
                                            .toLowerCase(
                                                    Locale.ROOT
                                            );


                            filtered.setPredicate(
                                    recipient ->
                                            recipient
                                                    .display
                                                    .toLowerCase(
                                                            Locale.ROOT
                                                    )
                                                    .contains(
                                                            query
                                                    )
                            );
                        }
                );


        // =====================================================
        // RECIPIENT LIST
        // =====================================================

        ListView<Recipient> recipientList =
                new ListView<>(
                        filtered
                );


        recipientList.setCellFactory(
                CheckBoxListCell.forListView(
                        item ->
                                item.selected
                )
        );


        recipientList.setPrefHeight(
                190
        );


        recipientList.setPlaceholder(
                new Label(
                        "No matching active users."
                )
        );


        VBox recipientContent =
                new VBox(
                        10,
                        search,
                        recipientList
                );


        recipientContent.setPadding(
                new Insets(12)
        );


        TitledPane picker =
                new TitledPane();


        picker.setContent(
                recipientContent
        );

        picker.setExpanded(
                false
        );

        picker.setAnimated(
                false
        );


        picker
                .textProperty()
                .bind(
                        selectedLabel
                                .textProperty()
                );


        picker
                .visibleProperty()
                .bind(
                        audience
                                .valueProperty()
                                .isEqualTo(
                                        "Selected"
                                )
                );


        picker
                .managedProperty()
                .bind(
                        picker
                                .visibleProperty()
                );


        audience
                .valueProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) ->
                                picker.setExpanded(
                                        false
                                )
                );


        // =====================================================
        // SUBJECT / MESSAGE
        // =====================================================

        subject.setPromptText(
                "Subject — maximum 150 characters"
        );


        message.setPromptText(
                "Write your system announcement..."
        );

        message.setWrapText(
                true
        );

        message.setPrefRowCount(
                7
        );


        composer
                .getChildren()
                .addAll(
                        new Label("To"),
                        audience,
                        picker,

                        new Label("Subject"),
                        subject,

                        new Label("Message"),
                        message
                );


        configureAttachmentsAndLinks();


        // =====================================================
        // SEND
        // =====================================================

        send
                .getStyleClass()
                .removeAll(
                        "secondary-button",
                        "login-primary-button",
                        "app-button"
                );


        if (
                !send
                        .getStyleClass()
                        .contains(
                                "admin-button-primary"
                        )
        ) {

            send
                    .getStyleClass()
                    .add(
                            "admin-button-primary"
                    );
        }


        send.setOnAction(
                event ->
                        sendAnnouncement()
        );


        // =====================================================
        // SCHEDULE
        // =====================================================

        scheduleDate.setEditable(
                false
        );


        scheduleDate.setPromptText(
                "Sending date"
        );


        scheduleDate.setValue(
                LocalDate
                        .now(
                                ZoneId.of(
                                        "Asia/Kuala_Lumpur"
                                )
                        )
                        .plusDays(1)
        );


        scheduleTime.setText(
                "09:00"
        );

        scheduleTime.setPromptText(
                "HH:mm"
        );

        scheduleTime.setPrefWidth(
                100
        );


        Label scheduleHeading =
                new Label(
                        "Schedule Announcement"
                );


        scheduleHeading
                .getStyleClass()
                .add(
                        "admin-section-title"
                );


        Label timeHint =
                new Label(
                        "Malaysia time (MYT). Use 24-hour time, for example 14:30."
                );


        timeHint.setWrapText(
                true
        );


        timeHint
                .getStyleClass()
                .add(
                        "admin-helper-text"
                );


        HBox scheduleFields =
                new HBox(
                        12,
                        scheduleDate,
                        scheduleTime
                );


        VBox scheduleBox =
                new VBox(
                        10,
                        scheduleHeading,
                        scheduleFields,
                        timeHint
                );


        scheduleBox.setPadding(
                new Insets(16)
        );


        scheduleBox.setStyle(
                "-fx-background-color: #F7F9FD;"
                        + "-fx-background-radius: 9;"
                        + "-fx-border-color: #DCE4F3;"
                        + "-fx-border-width: 1;"
                        + "-fx-border-radius: 9;"
        );


        composer
                .getChildren()
                .add(
                        scheduleBox
                );


        scheduleButton
                .getStyleClass()
                .remove(
                        "secondary-button"
                );


        if (
                !scheduleButton
                        .getStyleClass()
                        .contains(
                                "admin-button"
                        )
        ) {

            scheduleButton
                    .getStyleClass()
                    .add(
                            "admin-button"
                    );
        }


        scheduleButton.setOnAction(
                event ->
                        scheduleAnnouncement()
        );


        HBox composeActions =
                new HBox(
                        12,
                        send,
                        scheduleButton
                );


        VBox composeContent =
                new VBox(
                        16,
                        composer,
                        composeActions
                );


        composeContent.setPadding(
                new Insets(22)
        );


        ScrollPane composeScroll =
                new ScrollPane(
                        composeContent
                );


        composeScroll.setFitToWidth(
                true
        );


        // =====================================================
        // SENT TABLE
        // =====================================================

        configureSentTable();


        if (
                !sentTable
                        .getStyleClass()
                        .contains(
                                "admin-table"
                        )
        ) {

            sentTable
                    .getStyleClass()
                    .add(
                            "admin-table"
                    );
        }


        refresh.setOnAction(
                event ->
                        loadSent()
        );


        Button viewDetails =
                new Button(
                        "View Details"
                );


        viewDetails
                .disableProperty()
                .bind(
                        sentTable
                                .getSelectionModel()
                                .selectedItemProperty()
                                .isNull()
                );


        viewDetails.setOnAction(
                event ->
                        showSentDetails()
        );


        sentTable.setRowFactory(
                table -> {

                    TableRow<JsonObject> row =
                            new TableRow<>();


                    row.setOnMouseClicked(
                            event -> {

                                if (
                                        event.getClickCount() == 2
                                                && !row.isEmpty()
                                ) {

                                    table
                                            .getSelectionModel()
                                            .select(
                                                    row.getItem()
                                            );


                                    showSentDetails();
                                }
                            }
                    );


                    return row;
                }
        );


        VBox sentContent =
                new VBox(
                        12,

                        new Label(
                                "Your latest 50 sent announcements"
                        ),

                        new HBox(
                                10,
                                refresh,
                                viewDetails
                        ),

                        sentTable
                );


        sentContent.setPadding(
                new Insets(22)
        );


        VBox.setVgrow(
                sentTable,
                Priority.ALWAYS
        );


        // =====================================================
        // TABS
        // =====================================================

        Tab composeTab =
                new Tab(
                        "Compose",
                        composeScroll
                );

        Tab sentTab =
                new Tab(
                        "Sent",
                        sentContent
                );

        Tab scheduledTab =
                new Tab(
                        "Scheduled",
                        buildScheduledContent()
                );


        TabPane tabs =
                new TabPane(
                        sentTab,
                        scheduledTab,
                        composeTab
                );


        tabs.getStyleClass().remove(
                "announcement-tabs"
        );

        if (!tabs.getStyleClass()
                .contains("admin-notification-tabs")) {

        tabs.getStyleClass()
                .add("admin-notification-tabs");
        }


        tabs.setTabClosingPolicy(
                TabPane.TabClosingPolicy.UNAVAILABLE
        );


        tabs.getSelectionModel()
                .select(sentTab);


        // When user changes tab
        tabs.getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (
                                observable,
                                oldTab,
                                newTab
                        ) -> {

                        if (busy) {
                                return;
                        }

                        if (newTab == sentTab) {

                                loadSent();

                        } else if (newTab == scheduledTab) {

                                loadScheduled();
                        }
                        }
                );

        // =====================================================
        // STATUS
        // =====================================================

        status.setWrapText(
                true
        );


        status
                .getStyleClass()
                .add(
                        "admin-helper-text"
                );


        status.setPadding(
                new Insets(
                        12,
                        4,
                        0,
                        4
                )
        );


        // =====================================================
        // COMMON NOTIFICATION HEADER
        // =====================================================

        NotificationCenterNavigation.install(
                page,
                stage,
                "Announcements",
                "Compose announcements and review messages sent to managers and employees.",
                this::canLeaveNotificationCenter
        );


        // =====================================================
        // ADMIN UI
        // =====================================================
        composeContent.setStyle(
                "-fx-background-color: #FFFFFF;"
        );


        composeScroll.setStyle(
                "-fx-background: white;"
                        + "-fx-background-color: transparent;"
                        + "-fx-border-color: transparent;"
        );


        sentContent.setStyle(
                "-fx-background-color: #FFFFFF;"
        );


        refresh
                .getStyleClass()
                .remove(
                        "secondary-button"
                );


        viewDetails
                .getStyleClass()
                .remove(
                        "secondary-button"
                );


        if (
                !refresh
                        .getStyleClass()
                        .contains(
                                "admin-button"
                        )
        ) {

            refresh
                    .getStyleClass()
                    .add(
                            "admin-button"
                    );
        }


        if (
                !viewDetails
                        .getStyleClass()
                        .contains(
                                "admin-button"
                        )
        ) {

            viewDetails
                    .getStyleClass()
                    .add(
                            "admin-button"
                    );
        }


        subject.setMinHeight(
                38
        );


        audience.setMinHeight(
                38
        );


        message.setPrefRowCount(
                6
        );


        subject
                .getStyleClass()
                .add(
                        "admin-input"
                );


        audience
                .getStyleClass()
                .add(
                        "admin-combo-box"
                );


        message
                .getStyleClass()
                .add(
                        "admin-text-area"
                );


        search
                .getStyleClass()
                .add(
                        "admin-input"
                );


        scheduleDate
                .getStyleClass()
                .add(
                        "admin-date-picker"
                );


        scheduleTime
                .getStyleClass()
                .add(
                        "admin-input"
                );


        page.setCenter(
                tabs
        );


        page.setBottom(
                status
        );


        // =====================================================
        // RESTORE PENDING
        // =====================================================

        JsonObject pending =
                PENDING.get(
                        userId
                );


        if (pending != null) {

            audience.setValue(
                    text(
                            pending,
                            "p_audience"
                    )
            );


            subject.setText(
                    text(
                            pending,
                            "p_subject"
                    )
            );


            message.setText(
                    text(
                            pending,
                            "p_message"
                    )
            );


            draftAnnouncementId =
                    text(
                            pending,
                            "p_announcement_id"
                    );


            attachments =
                    pending.has(
                            "p_attachments"
                    )
                            ? pending
                            .getAsJsonArray(
                                    "p_attachments"
                            )
                            .deepCopy()

                            : new JsonArray();


            announcementLinks =
                    pending.has(
                            "p_links"
                    )
                            ? pending
                            .getAsJsonArray(
                                    "p_links"
                            )
                            .deepCopy()

                            : new JsonArray();


            renderAttachmentsAndLinks();


            status.setText(
                    hasPendingSchedule()
                            ? "A previous schedule is unconfirmed. "
                            + "Click Retry Same Schedule to confirm it."

                            : "A previous send is unconfirmed. Check Sent or retry "
                            + "the same request before composing another announcement."
            );
        }


        updateControls();
    }


    // =========================================================
    // OPEN
    // =========================================================

    private void open() {

        shell.setCenter(
                page
        );


        if (
                shell.getLeft() != null
        ) {

            for (
                    javafx.scene.Node node
                    : shell
                    .getLeft()
                    .lookupAll(
                            ".button"
                    )
            ) {

                if (
                        node instanceof Button button
                ) {

                    button
                            .getStyleClass()
                            .remove(
                                    "admin-nav-button-active"
                            );


                    button
                            .getStyleClass()
                            .remove(
                                    "sidebar-button-active"
                            );


                    if (
                            !button
                                    .getStyleClass()
                                    .contains(
                                            "logout-button"
                                    )
                    ) {

                        button
                                .getStyleClass()
                                .remove(
                                        "sidebar-button"
                                );


                        if (
                                !button
                                        .getStyleClass()
                                        .contains(
                                                "admin-nav-button"
                                        )
                        ) {

                            button
                                    .getStyleClass()
                                    .add(
                                            "admin-nav-button"
                                    );
                        }
                    }


                    String buttonText =
                            button.getText();


                    if (
                            buttonText != null
                                    && (
                                    buttonText.contains(
                                            "Notifications"
                                    )
                                            || buttonText.contains(
                                            "Notification/Announcements"
                                    )
                            )
                    ) {

                        button
                                .getStyleClass()
                                .remove(
                                        "admin-nav-button"
                                );


                        if (
                                !button
                                        .getStyleClass()
                                        .contains(
                                                "admin-nav-button-active"
                                        )
                        ) {

                            button
                                    .getStyleClass()
                                    .add(
                                            "admin-nav-button-active"
                                    );
                        }
                    }
                }
            }
        }


        loadRecipients();
    }


    // =========================================================
    // LOAD RECIPIENTS
    // =========================================================

    private void loadRecipients() {

        run(
                () ->
                        request(
                                "/rpc/list_announcement_recipients",
                                new JsonObject()
                        ).getAsJsonArray(),

                rows -> {

                    users.clear();


                    JsonObject pending =
                            PENDING.get(
                                    userId
                            );


                    for (
                            JsonElement element
                            : rows
                    ) {

                        Recipient user =
                                new Recipient(
                                        element
                                                .getAsJsonObject()
                                );


                        if (
                                pending != null
                                        && pending.has(
                                        "p_recipient_ids"
                                )
                        ) {

                            for (
                                    JsonElement id
                                    : pending.getAsJsonArray(
                                            "p_recipient_ids"
                                    )
                            ) {

                                if (
                                        user.id.equals(
                                                id.getAsString()
                                        )
                                ) {

                                    user.selected.set(
                                            true
                                    );
                                }
                            }
                        }


                        user
                                .selected
                                .addListener(
                                        (
                                                observable,
                                                oldValue,
                                                value
                                        ) ->
                                                updateSelectedCount()
                                );


                        users.add(
                                user
                        );
                    }


                    recipientsLoaded =
                            true;


                    updateSelectedCount();

                    updateControls();

                    loadSent();


                    if (
                            pending == null
                    ) {

                        status.setText(
                                "Choose recipients and compose an announcement."
                        );
                    }
                },

                error ->
                        showError(
                                "Unable to Load Recipients",
                                "Close and reopen this window to retry."
                        )
        );
    }


    // =========================================================
    // SELECTED COUNT
    // =========================================================

    private void updateSelectedCount() {

        long count =
                users
                        .stream()
                        .filter(
                                user ->
                                        user.selected.get()
                        )
                        .count();


        selectedLabel.setText(
                "Selected: "
                        + count
                        + " users"
        );
    }


    // =========================================================
    // SEND
    // =========================================================

    private void sendAnnouncement() {

        if (
                busy
                        || !active()
                        || hasPendingSchedule()
        ) {

            return;
        }


        if (
                !PENDING.containsKey(
                        userId
                )
                        && (
                        !linkLabelInput
                                .getText()
                                .isBlank()

                                || !linkUrlInput
                                .getText()
                                .isBlank()
                )
        ) {

            showError(
                    "Unfinished Link",
                    "Click Add to include the link, or Cancel to discard it, "
                            + "before sending."
            );

            return;
        }


        JsonObject existing =
                PENDING.get(
                        userId
                );


        boolean retry =
                existing != null;


        JsonObject body;


        if (retry) {

            body =
                    existing.deepCopy();

        } else {

            String title =
                    subject
                            .getText()
                            .trim();


            String content =
                    message
                            .getText()
                            .trim();


            if (
                    title.isEmpty()
                            || title.codePointCount(
                            0,
                            title.length()
                    ) > 150

                            || content.isEmpty()

                            || content.codePointCount(
                            0,
                            content.length()
                    ) > 10000
            ) {

                showError(
                        "Check Announcement",
                        "Enter a subject of 1–150 characters and "
                                + "a message of 1–10,000 characters."
                );

                return;
            }


            JsonArray ids =
                    new JsonArray();


            if (
                    "Selected".equals(
                            audience.getValue()
                    )
            ) {

                for (
                        Recipient user
                        : users
                ) {

                    if (
                            user.selected.get()
                    ) {

                        ids.add(
                                user.id
                        );
                    }
                }


                if (
                        ids.isEmpty()
                ) {

                    showError(
                            "Select Recipients",
                            "Please select at least one user."
                    );

                    return;
                }
            }


            String target =
                    "Selected".equals(
                            audience.getValue()
                    )
                            ? ids.size()
                            + " selected users"

                            : "active recipients in: "
                            + audience.getValue();


            if (
                    !AEPMDialog.showConfirmation(
                            stage,
                            "Send Announcement",
                            "Send \""
                                    + title
                                    + "\" to "
                                    + target
                                    + "?"
                                    + "\n\nAttachments: "
                                    + attachments.size()
                                    + "\nLinks: "
                                    + announcementLinks.size()
                    )
            ) {

                return;
            }


            if (
                    !active()
            ) {

                return;
            }


            body =
                    new JsonObject();


            body.addProperty(
                    "p_announcement_id",
                    draftAnnouncementId
            );


            body.addProperty(
                    "p_subject",
                    title
            );


            body.addProperty(
                    "p_message",
                    content
            );


            body.addProperty(
                    "p_audience",
                    audience.getValue()
            );


            body.add(
                    "p_recipient_ids",
                    ids
            );


            body.add(
                    "p_attachments",
                    attachments.deepCopy()
            );


            body.add(
                    "p_links",
                    announcementLinks.deepCopy()
            );
        }


        final JsonObject payload =
                body.deepCopy();


        PENDING.put(
                userId,
                payload.deepCopy()
        );


        status.setText(
                "Sending announcement..."
        );


        run(
                () ->
                        request(
                                "/rpc/send_system_announcement_v2",
                                payload
                        ).getAsJsonObject(),

                result -> {

                    PENDING.remove(
                            userId
                    );


                    subject.clear();

                    message.clear();


                    users.forEach(
                            user ->
                                    user.selected.set(
                                            false
                                    )
                    );


                    resetComposeExtras();

                    updateControls();


                    int count =
                            result
                                    .get(
                                            "recipient_count"
                                    )
                                    .getAsInt();


                    status.setText(
                            "Announcement recorded for "
                                    + count
                                    + " recipients."
                    );


                    AEPMDialog.showSuccess(
                            stage,
                            "Announcement Sent",
                            "The announcement was recorded for "
                                    + count
                                    + " recipients."
                    );
                },

                error -> {

                    boolean definiteRejection =
                            !retry
                                    && error instanceof HttpFailure failure
                                    && (
                                    failure.statusCode == 400
                                            || failure.statusCode == 401
                                            || failure.statusCode == 403
                                            || failure.statusCode == 404
                                            || failure.statusCode == 422
                            );


                    if (
                            definiteRejection
                    ) {

                        PENDING.remove(
                                userId
                        );


                        updateControls();


                        showError(
                                "Announcement Not Sent",
                                error.getMessage()
                                        + " Check the recipients, session and input."
                        );

                    } else {

                        updateControls();


                        showError(
                                "Send Not Confirmed",
                                "Check Sent or click Retry Same Send. "
                                        + "The same request ID will be reused to prevent "
                                        + "duplicate notifications. Keep the application "
                                        + "open until the result is confirmed."
                        );
                    }
                }
        );
    }


    // =========================================================
    // LOAD SENT
    // =========================================================

    private void loadSent() {

        if (busy) {
            return;
        }


        run(
                () ->
                        request(
                                "/system_announcement_tbl"
                                        + "?select=announcement_id,subject,message,audience,"
                                        + "recipient_count,sent_at"
                                        + "&sender_id=eq."
                                        + userId
                                        + "&order=sent_at.desc,announcement_id.desc"
                                        + "&limit=50",

                                null
                        ).getAsJsonArray(),

                rows -> {

                    sentTable
                            .getItems()
                            .clear();


                    JsonObject pending =
                            PENDING.get(
                                    userId
                            );


                    for (
                            JsonElement element
                            : rows
                    ) {

                        JsonObject row =
                                element
                                        .getAsJsonObject();


                        sentTable
                                .getItems()
                                .add(
                                        row
                                );


                        if (
                                pending != null
                                        && text(
                                        row,
                                        "announcement_id"
                                ).equals(
                                        text(
                                                pending,
                                                "p_announcement_id"
                                        )
                                )
                        ) {

                            PENDING.remove(
                                    userId
                            );


                            subject.clear();

                            message.clear();


                            users.forEach(
                                    user ->
                                            user.selected.set(
                                                    false
                                            )
                            );


                            resetComposeExtras();


                            status.setText(
                                    "The previous send was confirmed in Sent."
                            );
                        }
                    }


                    updateControls();
                },

                error ->
                        showError(
                                "Unable to Load Sent",
                                "Please refresh again. An empty list must not "
                                        + "be assumed when loading fails."
                        )
        );
    }


    // =========================================================
    // CONFIGURE SENT TABLE
    // =========================================================

    private void configureSentTable() {

        addColumn(
                "Subject",
                "subject",
                360
        );


        addColumn(
                "Audience",
                "audience",
                130
        );


        addColumn(
                "Recipients",
                "recipient_count",
                100
        );


        TableColumn<JsonObject, String> date =
                new TableColumn<>(
                        "Sent At (MYT)"
                );


        date.setCellValueFactory(
                cell -> {

                    String value =
                            text(
                                    cell.getValue(),
                                    "sent_at"
                            );


                    try {

                        value =
                                OffsetDateTime
                                        .parse(
                                                value
                                        )
                                        .atZoneSameInstant(
                                                ZoneId.of(
                                                        "Asia/Kuala_Lumpur"
                                                )
                                        )
                                        .format(
                                                DateTimeFormatter.ofPattern(
                                                        "dd MMM yyyy HH:mm",
                                                        Locale.ENGLISH
                                                )
                                        );

                    } catch (
                            RuntimeException ignored
                    ) {
                    }


                    return new SimpleStringProperty(
                            value
                    );
                }
        );


        date.setPrefWidth(
                190
        );


        sentTable
                .getColumns()
                .add(
                        date
                );


        sentTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );


        sentTable.setPlaceholder(
                new Label(
                        "No sent announcements loaded."
                )
        );
    }


    private void addColumn(
            String title,
            String key,
            double width
    ) {

        TableColumn<JsonObject, String> column =
                new TableColumn<>(
                        title
                );


        column.setPrefWidth(
                width
        );


        column.setCellValueFactory(
                cell ->
                        new SimpleStringProperty(
                                text(
                                        cell.getValue(),
                                        key
                                )
                        )
        );


        sentTable
                .getColumns()
                .add(
                        column
                );
    }


    // =========================================================
    // SENT DETAILS
    // =========================================================

    private void showSentDetails() {

        JsonObject row =
                sentTable
                        .getSelectionModel()
                        .getSelectedItem();


        if (
                row != null
        ) {

            AnnouncementDetailsDialog.showSent(
                    stage,
                    text(
                            row,
                            "announcement_id"
                    )
            );
        }
    }


    // =========================================================
    // REQUEST
    // =========================================================

    private JsonElement request(
            String path,
            JsonObject body
    ) throws Exception {

        var client =
                SupabaseClient.getInstance();


        HttpRequest.Builder builder =
                client.createAuthenticatedRequest(
                        AppConfig.REST_URL
                                + path,

                        token
                ).timeout(
                        Duration.ofSeconds(
                                30
                        )
                );


        HttpRequest request =
                body == null

                        ? builder
                        .GET()
                        .build()

                        : builder
                        .POST(
                                HttpRequest.BodyPublishers
                                        .ofString(
                                                body.toString()
                                        )
                        )
                        .build();


        HttpResponse<String> response =
                client
                        .getHttpClient()
                        .send(
                                request,
                                HttpResponse.BodyHandlers
                                        .ofString()
                        );


        if (
                response.statusCode() < 200
                        || response.statusCode() >= 300
        ) {

            throw new HttpFailure(
                    response.statusCode()
            );
        }


        return JsonParser.parseString(
                response.body()
        );
    }


    // =========================================================
    // ASYNC RUN
    // =========================================================

    private <T> void run(
            Callable<T> work,
            Consumer<T> success,
            Consumer<Throwable> failure
    ) {

        if (busy) {
            return;
        }


        busy =
                true;


        updateControls();


        Task<T> task =
                new Task<>() {

                    @Override
                    protected T call()
                            throws Exception {

                        return work.call();
                    }
                };


        task.setOnSucceeded(
                event -> {

                    busy =
                            false;


                    updateControls();


                    if (
                            active()
                    ) {

                        success.accept(
                                task.getValue()
                        );
                    }
                }
        );


        task.setOnFailed(
                event -> {

                    busy =
                            false;


                    updateControls();


                    if (
                            active()
                    ) {

                        failure.accept(
                                task.getException()
                        );
                    }
                }
        );


        Thread worker =
                new Thread(
                        task,
                        "admin-announcements"
                );


        worker.setDaemon(
                true
        );


        worker.start();
    }


    // =========================================================
    // ACTIVE
    // =========================================================

    private boolean active() {

        var user =
                SessionManager.getCurrentUser();


        return stage.isShowing()
                && stage
                .getScene()
                .getRoot() == shell

                && shell
                .getCenter() == page

                && SessionManager
                .getAuthSession() == session

                && user != null

                && user.getRoleId() == 1L

                && userId.equals(
                user.getUserId()
        );
    }


    // =========================================================
    // UPDATE CONTROLS
    // =========================================================

    private void updateControls() {

        boolean pending =
                PENDING.containsKey(
                        userId
                );


        boolean scheduledPending =
                hasPendingSchedule();


        composer.setDisable(
                busy
                        || pending
                        || !recipientsLoaded
        );


        send.setDisable(
                busy
                        || scheduledPending

                        || (
                        !pending
                                && !recipientsLoaded
                )
        );


        scheduleButton.setDisable(
                busy

                        || (
                        pending
                                && !scheduledPending
                )

                        || (
                        !pending
                                && !recipientsLoaded
                )
        );


        refresh.setDisable(
                busy
        );


        refreshScheduled.setDisable(
                busy
        );


        send.setText(
                pending
                        && !scheduledPending

                        ? "Retry Same Send"

                        : "Send Announcement"
        );


        scheduleButton.setText(
                scheduledPending

                        ? "Retry Same Schedule"

                        : "Schedule"
        );
    }


    // =========================================================
    // ERROR
    // =========================================================

    private void showError(
            String title,
            String message
    ) {

        status.setText(
                message
        );


        AEPMDialog.showError(
                stage,
                title,
                message
        );
    }


    // =========================================================
    // SAFE JSON TEXT
    // =========================================================

    private static String text(
            JsonObject row,
            String key
    ) {

        JsonElement value =
                row.get(
                        key
                );


        return value == null
                || value.isJsonNull()

                ? "—"

                : value.getAsString();
    }


    // =========================================================
    // ATTACHMENTS / LINKS
    // =========================================================

    private void configureAttachmentsAndLinks() {

        Button attachButton =
                new Button(
                        "Attach File"
                );


        Button linkButton =
                new Button(
                        "Add Link"
                );


        attachButton
                .getStyleClass()
                .remove(
                        "secondary-button"
                );


        linkButton
                .getStyleClass()
                .remove(
                        "secondary-button"
                );


        if (
                !attachButton
                        .getStyleClass()
                        .contains(
                                "admin-button"
                        )
        ) {

            attachButton
                    .getStyleClass()
                    .add(
                            "admin-button"
                    );
        }


        if (
                !linkButton
                        .getStyleClass()
                        .contains(
                                "admin-button"
                        )
        ) {

            linkButton
                    .getStyleClass()
                    .add(
                            "admin-button"
                    );
        }


        attachButton.setOnAction(
                event ->
                        chooseAttachment()
        );


        linkButton.setOnAction(
                event -> {

                    if (
                            announcementLinks.size() >= 5
                    ) {

                        showError(
                                "Link Limit",
                                "You can add up to 5 links."
                        );

                        return;
                    }


                    linkEditor.setVisible(
                            true
                    );


                    linkLabelInput.requestFocus();
                }
        );


        linkLabelInput.setPromptText(
                "Link name, for example: Meeting details"
        );


        linkUrlInput.setPromptText(
                "https://example.com"
        );


        linkLabelInput
                .getStyleClass()
                .add(
                        "admin-input"
                );


        linkUrlInput
                .getStyleClass()
                .add(
                        "admin-input"
                );


        Button saveLink =
                new Button(
                        "Add"
                );


        Button cancelLink =
                new Button(
                        "Cancel"
                );


        saveLink
                .getStyleClass()
                .remove(
                        "login-primary-button"
                );


        cancelLink
                .getStyleClass()
                .remove(
                        "secondary-button"
                );


        if (
                !saveLink
                        .getStyleClass()
                        .contains(
                                "admin-button-primary"
                        )
        ) {

            saveLink
                    .getStyleClass()
                    .add(
                            "admin-button-primary"
                    );
        }


        if (
                !cancelLink
                        .getStyleClass()
                        .contains(
                                "admin-button"
                        )
        ) {

            cancelLink
                    .getStyleClass()
                    .add(
                            "admin-button"
                    );
        }


        saveLink.setOnAction(
                event ->
                        addAnnouncementLink()
        );


        cancelLink.setOnAction(
                event -> {

                    linkLabelInput.clear();

                    linkUrlInput.clear();

                    linkEditor.setVisible(
                            false
                    );
                }
        );


        /*
         * IMPORTANT:
         * Clear first.
         *
         * This fixes:
         * Children: duplicate children added
         */
        linkEditor
                .getChildren()
                .clear();


        linkEditor
                .getChildren()
                .addAll(
                        new Label(
                                "Link name"
                        ),

                        linkLabelInput,

                        new Label(
                                "URL"
                        ),

                        linkUrlInput,

                        new HBox(
                                10,
                                saveLink,
                                cancelLink
                        )
                );


        linkEditor.setPadding(
                new Insets(14)
        );


        linkEditor.setStyle(
                "-fx-background-color: #F7F9FD;"
                        + "-fx-background-radius: 9;"
                        + "-fx-border-color: #DCE4F3;"
                        + "-fx-border-width: 1;"
                        + "-fx-border-radius: 9;"
        );


        linkEditor.setVisible(
                false
        );


        if (
                !linkEditor
                        .managedProperty()
                        .isBound()
        ) {

            linkEditor
                    .managedProperty()
                    .bind(
                            linkEditor
                                    .visibleProperty()
                    );
        }


        Label limits =
                new Label(
                        "Up to 5 files, 10 MB each, and 5 links. "
                                + "Supported files: PDF, DOCX, XLSX, PNG, JPG and TXT."
                );


        limits.setWrapText(
                true
        );


        limits
                .getStyleClass()
                .add(
                        "admin-helper-text"
                );


        /*
         * Prevent duplicate-child error if this UI is rebuilt.
         */
        composer
                .getChildren()
                .remove(
                        attachmentRows
                );


        composer
                .getChildren()
                .remove(
                        linkEditor
                );


        composer
                .getChildren()
                .remove(
                        linkRows
                );


        HBox extrasActions =
                new HBox(
                        10,
                        attachButton,
                        linkButton
                );


        composer
                .getChildren()
                .addAll(
                        extrasActions,
                        limits,
                        attachmentRows,
                        linkEditor,
                        linkRows
                );


        renderAttachmentsAndLinks();
    }


    // =========================================================
    // CAN EDIT EXTRAS
    // =========================================================

    private boolean canEditExtras() {

        return active()
                && !busy
                && recipientsLoaded
                && !PENDING.containsKey(
                userId
        );
    }


    // =========================================================
    // CHOOSE ATTACHMENT
    // =========================================================

    private void chooseAttachment() {

        if (
                !canEditExtras()
        ) {

            return;
        }


        if (
                attachments.size() >= 5
        ) {

            showError(
                    "Attachment Limit",
                    "You can add up to 5 attachments."
            );

            return;
        }


        FileChooser chooser =
                new FileChooser();


        chooser.setTitle(
                "Choose Announcement Attachment"
        );


        chooser
                .getExtensionFilters()
                .add(
                        new FileChooser.ExtensionFilter(
                                "Supported files",
                                "*.pdf",
                                "*.docx",
                                "*.xlsx",
                                "*.png",
                                "*.jpg",
                                "*.jpeg",
                                "*.txt"
                        )
                );


        File file =
                chooser.showOpenDialog(
                        stage
                );


        if (
                file == null
                        || !canEditExtras()
        ) {

            return;
        }


        String filename =
                file.getName();


        int dot =
                filename.lastIndexOf(
                        '.'
                );


        String extension =
                dot < 0

                        ? ""

                        : filename
                        .substring(
                                dot + 1
                        )
                        .toLowerCase(
                                Locale.ROOT
                        );


        String mimeType =
                switch (extension) {

                    case "pdf" ->
                            "application/pdf";

                    case "docx" ->
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

                    case "xlsx" ->
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

                    case "png" ->
                            "image/png";

                    case "jpg",
                         "jpeg" ->
                            "image/jpeg";

                    case "txt" ->
                            "text/plain";

                    default ->
                            null;
                };


        if (
                mimeType == null
        ) {

            showError(
                    "Unsupported File",
                    "Choose a PDF, DOCX, XLSX, PNG, JPG or TXT file."
            );

            return;
        }


        if (
                filename.codePointCount(
                        0,
                        filename.length()
                ) > 255

                        || filename
                        .chars()
                        .anyMatch(
                                Character::isISOControl
                        )
        ) {

            showError(
                    "Invalid File Name",
                    "Rename this file before uploading."
            );

            return;
        }


        if (
                !file.isFile()

                        || file.length() < 1

                        || file.length()
                        > MAX_FILE_BYTES
        ) {

            showError(
                    "Invalid File Size",
                    "Choose a non-empty file no larger than 10 MB."
            );

            return;
        }


        if (
                !AEPMDialog.showConfirmation(
                        stage,
                        "Upload Attachment",
                        "Upload \""
                                + filename
                                + "\" and attach it to this announcement?"
                )
        ) {

            return;
        }


        if (
                !canEditExtras()
        ) {

            return;
        }


        String storagePath =
                userId
                        + "/"
                        + draftAnnouncementId
                        + "/"
                        + UUID.randomUUID()
                        + "."
                        + extension;


        status.setText(
                "Uploading "
                        + filename
                        + "..."
        );


        run(
                () ->
                        uploadAttachment(
                                file,
                                filename,
                                storagePath,
                                mimeType
                        ),

                uploaded -> {

                    attachments.add(
                            uploaded
                    );


                    renderAttachmentsAndLinks();


                    status.setText(
                            "Uploaded: "
                                    + filename
                    );


                    AEPMDialog.showSuccess(
                            stage,
                            "Upload Successful",
                            filename
                                    + " is attached to this announcement."
                    );
                },

                error ->
                        showError(
                                "Upload Not Confirmed",
                                "The file was not added to this announcement. "
                                        + "Check your connection and try Attach File again."
                                        + "\n\nDetails: "
                                        + (
                                        error.getMessage() == null
                                                ? "Unable to complete the upload."
                                                : error.getMessage()
                                )
                        )
        );
    }


    // =========================================================
    // UPLOAD ATTACHMENT
    // =========================================================

    private JsonObject uploadAttachment(
            File file,
            String filename,
            String storagePath,
            String mimeType
    ) throws Exception {

        byte[] bytes;


        try (
                InputStream input =
                        Files.newInputStream(
                                file.toPath()
                        )
        ) {

            bytes =
                    input.readNBytes(
                            MAX_FILE_BYTES + 1
                    );
        }


        if (
                bytes.length < 1
                        || bytes.length > MAX_FILE_BYTES
        ) {

            throw new IllegalArgumentException(
                    "The file must be non-empty and no larger than 10 MB."
            );
        }


        var client =
                SupabaseClient.getInstance();


        HttpRequest upload =
                client
                        .createAuthenticatedRequest(
                                AppConfig.STORAGE_URL
                                        + "/object/announcement-attachments/"
                                        + storagePath,

                                token
                        )
                        .setHeader(
                                "Content-Type",
                                mimeType
                        )
                        .setHeader(
                                "x-upsert",
                                "false"
                        )
                        .timeout(
                                Duration.ofSeconds(
                                        120
                                )
                        )
                        .POST(
                                HttpRequest.BodyPublishers
                                        .ofByteArray(
                                                bytes
                                        )
                        )
                        .build();


        HttpResponse<String> response =
                client
                        .getHttpClient()
                        .send(
                                upload,
                                HttpResponse.BodyHandlers
                                        .ofString()
                        );


        if (
                response.statusCode() < 200
                        || response.statusCode() >= 300
        ) {

            throw new IllegalStateException(
                    "Storage rejected the upload (HTTP "
                            + response.statusCode()
                            + ")."
            );
        }


        JsonObject result =
                new JsonObject();


        result.addProperty(
                "file_name",
                filename
        );


        result.addProperty(
                "storage_path",
                storagePath
        );


        result.addProperty(
                "mime_type",
                mimeType
        );


        result.addProperty(
                "file_size",
                bytes.length
        );


        return result;
    }


    // =========================================================
    // ADD LINK
    // =========================================================

    private void addAnnouncementLink() {

        if (
                !canEditExtras()
        ) {

            return;
        }


        if (
                announcementLinks.size() >= 5
        ) {

            showError(
                    "Link Limit",
                    "You can add up to 5 links."
            );

            return;
        }


        String label =
                linkLabelInput
                        .getText()
                        .trim();


        String url =
                linkUrlInput
                        .getText()
                        .trim();


        if (
                label.isEmpty()

                        || label.codePointCount(
                        0,
                        label.length()
                ) > 150

                        || label
                        .chars()
                        .anyMatch(
                                Character::isISOControl
                        )
        ) {

            showError(
                    "Invalid Link Name",
                    "Enter a link name of 1–150 characters."
            );

            return;
        }


        try {

            URI uri =
                    new URI(
                            url
                    );


            if (
                    url.length() > 2048

                            || uri.getHost() == null

                            || uri.getUserInfo() != null

                            || (
                            !"https".equalsIgnoreCase(
                                    uri.getScheme()
                            )

                                    && !"http".equalsIgnoreCase(
                                    uri.getScheme()
                            )
                    )
            ) {

                throw new IllegalArgumentException();
            }

        } catch (
                Exception error
        ) {

            showError(
                    "Invalid Link",
                    "Enter a complete HTTP or HTTPS URL, "
                            + "for example https://example.com."
            );

            return;
        }


        if (
                !AEPMDialog.showConfirmation(
                        stage,
                        "Add Link",
                        "Add \""
                                + label
                                + "\"?"
                                + "\n\n"
                                + url
                )
        ) {

            return;
        }


        if (
                !canEditExtras()
        ) {

            return;
        }


        JsonObject item =
                new JsonObject();


        item.addProperty(
                "label",
                label
        );


        item.addProperty(
                "url",
                url
        );


        announcementLinks.add(
                item
        );


        linkLabelInput.clear();

        linkUrlInput.clear();

        linkEditor.setVisible(
                false
        );


        renderAttachmentsAndLinks();


        status.setText(
                "Link added: "
                        + label
        );
    }


    // =========================================================
    // RENDER EXTRAS
    // =========================================================

    private void renderAttachmentsAndLinks() {

        attachmentRows
                .getChildren()
                .clear();


        linkRows
                .getChildren()
                .clear();


        for (
                JsonElement element
                : attachments
        ) {

            JsonObject item =
                    element
                            .getAsJsonObject();


            String description =
                    text(
                            item,
                            "file_name"
                    )

                            + " — "

                            + String.format(
                            Locale.ENGLISH,
                            "%.1f KB",
                            item
                                    .get(
                                            "file_size"
                                    )
                                    .getAsLong()
                                    / 1024.0
                    )

                            + " — Uploaded";


            attachmentRows
                    .getChildren()
                    .add(
                            extraRow(
                                    description,

                                    () -> {

                                        attachments.remove(
                                                item
                                        );


                                        renderAttachmentsAndLinks();


                                        status.setText(
                                                "Attachment removed from this announcement."
                                        );
                                    },

                                    true
                            )
                    );
        }


        for (
                JsonElement element
                : announcementLinks
        ) {

            JsonObject item =
                    element
                            .getAsJsonObject();


            linkRows
                    .getChildren()
                    .add(
                            extraRow(
                                    text(
                                            item,
                                            "label"
                                    )
                                            + "\n"
                                            + text(
                                            item,
                                            "url"
                                    ),

                                    () -> {

                                        announcementLinks.remove(
                                                item
                                        );


                                        renderAttachmentsAndLinks();


                                        status.setText(
                                                "Link removed."
                                        );
                                    },

                                    false
                            )
                    );
        }
    }


    // =========================================================
    // EXTRA ROW
    // =========================================================

    private HBox extraRow(
            String description,
            Runnable removeAction,
            boolean attachment
    ) {

        Label label =
                new Label(
                        description
                );


        label.setWrapText(
                true
        );


        label.setMaxWidth(
                Double.MAX_VALUE
        );


        HBox.setHgrow(
                label,
                Priority.ALWAYS
        );


        Button remove =
                new Button(
                        "Remove"
                );


        remove
                .getStyleClass()
                .remove(
                        "secondary-button"
                );


        remove
                .getStyleClass()
                .add(
                        "admin-button"
                );


        remove.setOnAction(
                event -> {

                    if (
                            !canEditExtras()
                    ) {

                        return;
                    }


                    String prompt =
                            attachment

                                    ? "Remove this attachment from the announcement?"
                                    + "\n\nIt will not be included when you send."

                                    : "Remove this link from the announcement?";


                    if (
                            AEPMDialog.showConfirmation(
                                    stage,
                                    "Remove Item",
                                    prompt
                            )
                                    && canEditExtras()
                    ) {

                        removeAction.run();
                    }
                }
        );


        HBox row =
                new HBox(
                        12,
                        label,
                        remove
                );


        row.setAlignment(
                javafx.geometry.Pos.CENTER_LEFT
        );


        row.setPadding(
                new Insets(12)
        );


        row.setStyle(
                "-fx-background-color: #F7F9FD;"
                        + "-fx-border-color: #E3E7EF;"
                        + "-fx-border-width: 1;"
                        + "-fx-background-radius: 8;"
                        + "-fx-border-radius: 8;"
        );


        return row;
    }


    // =========================================================
    // RESET EXTRAS
    // =========================================================

    private void resetComposeExtras() {

        draftAnnouncementId =
                UUID.randomUUID()
                        .toString();


        attachments =
                new JsonArray();


        announcementLinks =
                new JsonArray();


        linkLabelInput.clear();

        linkUrlInput.clear();

        linkEditor.setVisible(
                false
        );


        renderAttachmentsAndLinks();
    }


    // =========================================================
    // CAN LEAVE
    // =========================================================

    private boolean canLeaveNotificationCenter() {

        if (
                !active()
        ) {

            return false;
        }


        if (busy) {

            AEPMDialog.showError(
                    stage,
                    "Please Wait",
                    "Wait for the current upload or request to finish."
            );

            return false;
        }


        if (
                PENDING.containsKey(
                        userId
                )
        ) {

            return true;
        }


        boolean hasDraft =
                !subject
                        .getText()
                        .isBlank()

                        || !message
                        .getText()
                        .isBlank()

                        || !attachments.isEmpty()

                        || !announcementLinks.isEmpty()

                        || !linkLabelInput
                        .getText()
                        .isBlank()

                        || !linkUrlInput
                        .getText()
                        .isBlank()

                        || users
                        .stream()
                        .anyMatch(
                                user ->
                                        user.selected.get()
                        );


        return !hasDraft

                || AEPMDialog.showConfirmation(
                stage,
                "Leave Announcement",
                "Leave Announcements and discard this unsent announcement?"
                        + "\n\nThe draft will not be saved."
        );
    }


    // =========================================================
    // SCHEDULE ANNOUNCEMENT
    // =========================================================

    private void scheduleAnnouncement() {

        if (
                busy
                        || !active()
        ) {

            return;
        }


        JsonObject existing =
                PENDING.get(
                        userId
                );


        if (
                existing != null
                        && !hasPendingSchedule()
        ) {

            showError(
                    "Unconfirmed Send",
                    "Confirm the previous send before scheduling another announcement."
            );

            return;
        }


        boolean retry =
                existing != null;


        JsonObject body;


        if (retry) {

            body =
                    existing.deepCopy();

        } else {

            if (
                    !recipientsLoaded
            ) {

                return;
            }


            if (
                    !linkLabelInput
                            .getText()
                            .isBlank()

                            || !linkUrlInput
                            .getText()
                            .isBlank()
            ) {

                showError(
                        "Unfinished Link",
                        "Click Add to include the link, or Cancel to discard it."
                );

                return;
            }


            String title =
                    subject
                            .getText()
                            .trim();


            String content =
                    message
                            .getText()
                            .trim();


            if (
                    title.isEmpty()

                            || title.codePointCount(
                            0,
                            title.length()
                    ) > 150

                            || content.isEmpty()

                            || content.codePointCount(
                            0,
                            content.length()
                    ) > 10000
            ) {

                showError(
                        "Check Announcement",
                        "Enter a subject of 1–150 characters and "
                                + "a message of 1–10,000 characters."
                );

                return;
            }


            JsonArray ids =
                    new JsonArray();


            if (
                    "Selected".equals(
                            audience.getValue()
                    )
            ) {

                for (
                        Recipient user
                        : users
                ) {

                    if (
                            user.selected.get()
                    ) {

                        ids.add(
                                user.id
                        );
                    }
                }


                if (
                        ids.isEmpty()
                ) {

                    showError(
                            "Select Recipients",
                            "Please select at least one user."
                    );

                    return;
                }
            }


            LocalDate selectedDate =
                    scheduleDate.getValue();


            if (
                    selectedDate == null
            ) {

                showError(
                        "Select Sending Date",
                        "No sending date was selected. "
                                + "Please choose a date using the calendar."
                );


                scheduleDate.requestFocus();

                return;
            }


            String rawTime =
                    scheduleTime.getText();


            String timeText =
                    java.text.Normalizer.normalize(
                            rawTime == null
                                    ? ""
                                    : rawTime,

                            java.text.Normalizer.Form.NFKC
                    ).strip();


            if (
                    !timeText.matches(
                            "(?:[01]?[0-9]|2[0-3]):[0-5][0-9]"
                    )
            ) {

                showError(
                        "Invalid Time Format",
                        "Time entered: ["
                                + timeText
                                + "]"
                                + "\nUse a time such as 09:00 or 17:15."
                                + "\nDo not include spaces or a full stop."
                );


                scheduleTime.requestFocus();

                return;
            }


            String[] parts =
                    timeText.split(
                            ":"
                    );


            LocalTime selectedTime =
                    LocalTime.of(
                            Integer.parseInt(
                                    parts[0]
                            ),

                            Integer.parseInt(
                                    parts[1]
                            )
                    );


            ZonedDateTime scheduledAt =
                    ZonedDateTime.of(
                            selectedDate,
                            selectedTime,
                            ZoneId.of(
                                    "Asia/Kuala_Lumpur"
                            )
                    );


            ZonedDateTime currentTime =
                    ZonedDateTime.now(
                            ZoneId.of(
                                    "Asia/Kuala_Lumpur"
                            )
                    );


            DateTimeFormatter timeDisplay =
                    DateTimeFormatter.ofPattern(
                            "dd MMM yyyy HH:mm:ss 'MYT'",
                            Locale.ENGLISH
                    );


            if (
                    !scheduledAt
                            .toInstant()
                            .isAfter(
                                    currentTime
                                            .toInstant()
                                            .plusSeconds(
                                                    60
                                            )
                            )
            ) {

                showError(
                        "Sending Time Is Too Early",
                        "Selected: "
                                + scheduledAt.format(
                                timeDisplay
                        )
                                + "\nCurrent time: "
                                + currentTime.format(
                                timeDisplay
                        )
                                + "\n\nChoose a time at least one minute from now."
                );

                return;
            }


            String target =
                    "Selected".equals(
                            audience.getValue()
                    )

                            ? ids.size()
                            + " selected users"

                            : "active recipients in: "
                            + audience.getValue();


            String displayTime =
                    scheduledAt.format(
                            DateTimeFormatter.ofPattern(
                                    "dd MMM yyyy HH:mm 'MYT'",
                                    Locale.ENGLISH
                            )
                    );


            body =
                    new JsonObject();


            body.addProperty(
                    "p_announcement_id",
                    draftAnnouncementId
            );


            body.addProperty(
                    "p_subject",
                    title
            );


            body.addProperty(
                    "p_message",
                    content
            );


            body.addProperty(
                    "p_audience",
                    audience.getValue()
            );


            body.add(
                    "p_recipient_ids",
                    ids
            );


            body.add(
                    "p_attachments",
                    attachments.deepCopy()
            );


            body.add(
                    "p_links",
                    announcementLinks.deepCopy()
            );


            body.addProperty(
                    "p_scheduled_at",
                    scheduledAt
                            .toOffsetDateTime()
                            .toString()
            );


            if (
                    !AEPMDialog.showConfirmation(
                            stage,
                            "Schedule Announcement",

                            "Schedule \""
                                    + title
                                    + "\" for "
                                    + target
                                    + "?"

                                    + "\n\nSending time: "
                                    + displayTime

                                    + "\nAttachments: "
                                    + attachments.size()

                                    + "\nLinks: "
                                    + announcementLinks.size()

                                    + "\n\nGroup recipients are checked at sending time."
                    )
            ) {

                return;
            }


            if (
                    !canEditExtras()
            ) {

                return;
            }
        }


        final JsonObject savedPayload =
                body.deepCopy();


        final JsonObject rpcPayload =
                savedPayload.deepCopy();


        rpcPayload.addProperty(
                "p_schedule_id",
                rpcPayload
                        .remove(
                                "p_announcement_id"
                        )
                        .getAsString()
        );


        PENDING.put(
                userId,
                savedPayload.deepCopy()
        );


        status.setText(
                "Saving scheduled announcement..."
        );


        run(
                () ->
                        request(
                                "/rpc/schedule_system_announcement",
                                rpcPayload
                        ).getAsJsonObject(),

                result -> {

                    String savedStatus =
                            text(
                                    result,
                                    "status"
                            );


                    String when =
                            text(
                                    result,
                                    "scheduled_at"
                            );


                    try {

                        when =
                                OffsetDateTime
                                        .parse(
                                                when
                                        )
                                        .atZoneSameInstant(
                                                ZoneId.of(
                                                        "Asia/Kuala_Lumpur"
                                                )
                                        )
                                        .format(
                                                DateTimeFormatter.ofPattern(
                                                        "dd MMM yyyy HH:mm 'MYT'",
                                                        Locale.ENGLISH
                                                )
                                        );

                    } catch (
                            RuntimeException ignored
                    ) {
                    }


                    PENDING.remove(
                            userId
                    );


                    subject.clear();

                    message.clear();


                    users.forEach(
                            user ->
                                    user.selected.set(
                                            false
                                    )
                    );


                    resetComposeExtras();

                    updateControls();


                    String details =
                            "Sending time: "
                                    + when
                                    + "\nStatus: "
                                    + savedStatus;


                    status.setText(
                            "Schedule confirmed. Status: "
                                    + savedStatus
                    );


                    if (
                            "Failed".equals(
                                    savedStatus
                            )
                    ) {

                        showError(
                                "Scheduled Delivery Failed",
                                "The schedule exists, but delivery failed. "
                                        + "It has not been scheduled again."
                                        + "\n\n"
                                        + details
                        );

                    } else {

                        AEPMDialog.showSuccess(
                                stage,

                                "Pending".equals(
                                        savedStatus
                                )
                                        ? "Announcement Scheduled"
                                        : "Schedule Confirmed",

                                details
                        );
                    }
                },

                error -> {

                    boolean definiteRejection =
                            !retry
                                    && error instanceof HttpFailure failure

                                    && (
                                    failure.statusCode == 400
                                            || failure.statusCode == 401
                                            || failure.statusCode == 403
                                            || failure.statusCode == 404
                                            || failure.statusCode == 422
                            );


                    if (
                            definiteRejection
                    ) {

                        PENDING.remove(
                                userId
                        );


                        updateControls();


                        showError(
                                "Schedule Not Saved",
                                error.getMessage()
                                        + "\nCheck the sending time, recipients "
                                        + "and attachments before trying again."
                        );

                    } else {

                        updateControls();


                        showError(
                                "Schedule Not Confirmed",
                                "Click Retry Same Schedule to confirm the result."
                                        + "\nThe same ID and content will be reused "
                                        + "to avoid creating a duplicate schedule."
                                        + "\nKeep the application open until confirmed."
                        );
                    }
                }
        );
    }


    // =========================================================
    // SCHEDULED CONTENT
    // =========================================================

    private VBox buildScheduledContent() {

        if (
                !scheduledTable
                        .getStyleClass()
                        .contains(
                                "admin-table"
                        )
        ) {

            scheduledTable
                    .getStyleClass()
                    .add(
                            "admin-table"
                    );
        }


        addScheduledColumn(
                "Subject",
                "subject",
                260
        );


        addScheduledColumn(
                "Audience",
                "audience",
                110
        );


        TableColumn<JsonObject, String> scheduledDateColumn =
                new TableColumn<>(
                        "Scheduled At (MYT)"
                );


        scheduledDateColumn.setCellValueFactory(
                cell ->
                        new SimpleStringProperty(
                                formatScheduledTime(
                                        text(
                                                cell.getValue(),
                                                "scheduled_at"
                                        )
                                )
                        )
        );


        scheduledDateColumn.setPrefWidth(
                190
        );


        scheduledTable
                .getColumns()
                .add(
                        scheduledDateColumn
                );


        TableColumn<JsonObject, String> stateColumn =
                new TableColumn<>(
                        "Status"
                );


        stateColumn.setCellValueFactory(
                cell ->
                        new SimpleStringProperty(
                                text(
                                        cell.getValue(),
                                        "status"
                                )
                        )
        );


        stateColumn.setPrefWidth(
                110
        );


        stateColumn.setCellFactory(
                column ->
                        new TableCell<>() {

                            @Override
                            protected void updateItem(
                                    String value,
                                    boolean empty
                            ) {

                                super.updateItem(
                                        value,
                                        empty
                                );


                                setText(
                                        null
                                );


                                setGraphic(
                                        null
                                );


                                if (
                                        empty
                                                || value == null
                                ) {

                                    return;
                                }


                                String background;

                                String foreground;


                                switch (value) {

                                    case "Pending" -> {

                                        background =
                                                "#DBEAFE";

                                        foreground =
                                                "#1D4ED8";
                                    }


                                    case "Sent" -> {

                                        background =
                                                "#DCFCE7";

                                        foreground =
                                                "#166534";
                                    }


                                    case "Failed" -> {

                                        background =
                                                "#FEE2E2";

                                        foreground =
                                                "#B91C1C";
                                    }


                                    case "Paused" -> {

                                        background =
                                                "#FEF3C7";

                                        foreground =
                                                "#92400E";
                                    }


                                    default -> {

                                        background =
                                                "#F1F5F9";

                                        foreground =
                                                "#475569";
                                    }
                                }


                                Label badge =
                                        new Label(
                                                value
                                        );


                                badge.setStyle(
                                        "-fx-background-color: "
                                                + background
                                                + ";"

                                                + "-fx-text-fill: "
                                                + foreground
                                                + ";"

                                                + "-fx-background-radius: 8;"
                                                + "-fx-padding: 5 10;"
                                                + "-fx-font-weight: bold;"
                                );


                                setGraphic(
                                        badge
                                );
                            }
                        }
        );


        scheduledTable
                .getColumns()
                .add(
                        stateColumn
                );


        TableColumn<JsonObject, String> sentDateColumn =
                new TableColumn<>(
                        "Sent At (MYT)"
                );


        sentDateColumn.setCellValueFactory(
                cell ->
                        new SimpleStringProperty(
                                formatScheduledTime(
                                        text(
                                                cell.getValue(),
                                                "sent_at"
                                        )
                                )
                        )
        );


        sentDateColumn.setPrefWidth(
                190
        );


        scheduledTable
                .getColumns()
                .add(
                        sentDateColumn
                );


        scheduledTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );


        scheduledTable.setPlaceholder(
                new Label(
                        "Scheduled announcements have not been loaded."
                )
        );


        scheduledSummary.setWrapText(
                true
        );


        scheduledSummary
                .getStyleClass()
                .add(
                        "admin-helper-text"
                );


        refreshScheduled.setOnAction(
                event ->
                        loadScheduled()
        );


        viewScheduledDetails
                .disableProperty()
                .bind(
                        scheduledTable
                                .getSelectionModel()
                                .selectedItemProperty()
                                .isNull()
                );


        viewScheduledDetails.setOnAction(
                event ->
                        showScheduledDetails()
        );


        scheduledTable.setRowFactory(
                table -> {

                    TableRow<JsonObject> row =
                            new TableRow<>();


                    row.setOnMouseClicked(
                            event -> {

                                if (
                                        event.getClickCount() == 2
                                                && !row.isEmpty()
                                ) {

                                    table
                                            .getSelectionModel()
                                            .select(
                                                    row.getItem()
                                            );


                                    showScheduledDetails();
                                }
                            }
                    );


                    return row;
                }
        );


        refreshScheduled
                .getStyleClass()
                .remove(
                        "secondary-button"
                );


        viewScheduledDetails
                .getStyleClass()
                .remove(
                        "secondary-button"
                );


        cancelScheduled
                .getStyleClass()
                .remove(
                        "secondary-button"
                );


        rescheduleButton
                .getStyleClass()
                .remove(
                        "secondary-button"
                );


        refreshScheduled
                .getStyleClass()
                .add(
                        "admin-button"
                );


        viewScheduledDetails
                .getStyleClass()
                .add(
                        "admin-button"
                );


        rescheduleButton
                .getStyleClass()
                .add(
                        "admin-button"
                );


        cancelScheduled
                .getStyleClass()
                .add(
                        "admin-button-danger"
                );


        cancelScheduled.setDisable(
                true
        );


        cancelScheduled.setOnAction(
                event ->
                        cancelSelectedSchedule()
        );


        rescheduleButton.setDisable(
                true
        );


        rescheduleButton.setOnAction(
                event ->
                        rescheduleSelectedAnnouncement()
        );


        scheduledTable
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (
                                observable,
                                oldRow,
                                row
                        ) ->
                                updateCancelScheduleControl()
                );


        Label errorHeading =
                new Label(
                        "Failure details"
                );


        errorHeading.setStyle(
                "-fx-text-fill: #B91C1C;"
                        + "-fx-font-weight: bold;"
        );


        scheduledError.setEditable(
                false
        );


        scheduledError.setWrapText(
                true
        );


        scheduledError.setPrefRowCount(
                3
        );


        VBox errorPanel =
                new VBox(
                        8,
                        errorHeading,
                        scheduledError
                );


        errorPanel.setPadding(
                new Insets(12)
        );


        errorPanel.setStyle(
                "-fx-background-color: #FEF2F2;"
                        + "-fx-border-color: #FECACA;"
                        + "-fx-border-width: 1;"
                        + "-fx-background-radius: 8;"
                        + "-fx-border-radius: 8;"
        );


        errorPanel.setVisible(
                false
        );


        errorPanel
                .managedProperty()
                .bind(
                        errorPanel
                                .visibleProperty()
                );


        scheduledTable
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (
                                observable,
                                oldRow,
                                row
                        ) -> {

                            String error =
                                    "";


                            if (
                                    row != null
                                            && row.has(
                                            "last_error"
                                    )
                                            && !row
                                            .get(
                                                    "last_error"
                                            )
                                            .isJsonNull()
                            ) {

                                error =
                                        row
                                                .get(
                                                        "last_error"
                                                )
                                                .getAsString();
                            }


                            scheduledError.setText(
                                    error
                            );


                            errorPanel.setVisible(
                                    !error.isBlank()
                            );
                        }
                );


        Label hint =
                new Label(
                        "Select a failed announcement to view its failure details. "
                                + "Click Refresh Scheduled to check the latest delivery status."
                );


        hint.setWrapText(
                true
        );


        hint
                .getStyleClass()
                .add(
                        "admin-helper-text"
                );


        VBox content =
                new VBox(
                        12,

                        scheduledSummary,

                        new FlowPane(
                                10,
                                10,

                                refreshScheduled,
                                viewScheduledDetails,
                                rescheduleButton,
                                cancelScheduled
                        ),

                        scheduledTable,
                        errorPanel,
                        hint
                );


        content.setPadding(
                new Insets(22)
        );


        content.setStyle(
                "-fx-background-color: #FFFFFF;"
        );


        VBox.setVgrow(
                scheduledTable,
                Priority.ALWAYS
        );


        return content;
    }


    // =========================================================
    // SCHEDULED COLUMN
    // =========================================================

    private void addScheduledColumn(
            String title,
            String key,
            double width
    ) {

        TableColumn<JsonObject, String> column =
                new TableColumn<>(
                        title
                );


        column.setPrefWidth(
                width
        );


        column.setCellValueFactory(
                cell ->
                        new SimpleStringProperty(
                                text(
                                        cell.getValue(),
                                        key
                                )
                        )
        );


        scheduledTable
                .getColumns()
                .add(
                        column
                );
    }


    // =========================================================
    // FORMAT SCHEDULED TIME
    // =========================================================

    private String formatScheduledTime(
            String value
    ) {

        if (
                value == null
                        || value.isBlank()
                        || "—".equals(
                        value
                )
        ) {

            return "—";
        }


        try {

            return OffsetDateTime
                    .parse(
                            value
                    )
                    .atZoneSameInstant(
                            ZoneId.of(
                                    "Asia/Kuala_Lumpur"
                            )
                    )
                    .format(
                            DateTimeFormatter.ofPattern(
                                    "dd MMM yyyy HH:mm",
                                    Locale.ENGLISH
                            )
                    );

        } catch (
                RuntimeException error
        ) {

            return value;
        }
    }


    // =========================================================
    // LOAD SCHEDULED
    // =========================================================

    private void loadScheduled() {

        if (
                busy
                        || !active()
        ) {

            return;
        }


        JsonObject selected =
                scheduledTable
                        .getSelectionModel()
                        .getSelectedItem();


        String selectedId =
                selected == null

                        ? null

                        : text(
                        selected,
                        "schedule_id"
                );


        scheduledSummary.setText(
                "Loading scheduled announcements..."
        );


        run(
                () ->
                        request(
                                "/scheduled_announcement_tbl"
                                        + "?select=schedule_id,subject,audience,"
                                        + "scheduled_at,status,sent_at,last_error,updated_at"
                                        + "&created_by=eq."
                                        + userId
                                        + "&order=created_at.desc,schedule_id.desc"
                                        + "&limit=100",

                                null
                        ).getAsJsonArray(),

                rows -> {

                    scheduledTable
                            .getSelectionModel()
                            .clearSelection();


                    scheduledTable
                            .getItems()
                            .clear();


                    JsonObject restoreSelection =
                            null;


                    int pendingCount =
                            0;

                    int sentCount =
                            0;

                    int failedCount =
                            0;


                    for (
                            JsonElement element
                            : rows
                    ) {

                        JsonObject row =
                                element
                                        .getAsJsonObject();


                        scheduledTable
                                .getItems()
                                .add(
                                        row
                                );


                        if (
                                selectedId != null
                                        && selectedId.equals(
                                        text(
                                                row,
                                                "schedule_id"
                                        )
                                )
                        ) {

                            restoreSelection =
                                    row;
                        }


                        switch (
                                text(
                                        row,
                                        "status"
                                )
                        ) {

                            case "Pending" ->
                                    pendingCount++;


                            case "Sent" ->
                                    sentCount++;


                            case "Failed" ->
                                    failedCount++;


                            default -> {
                            }
                        }
                    }


                    if (
                            restoreSelection != null
                    ) {

                        scheduledTable
                                .getSelectionModel()
                                .select(
                                        restoreSelection
                                );
                    }


                    scheduledTable.setPlaceholder(
                            new Label(
                                    "No scheduled announcements yet."
                            )
                    );


                    scheduledSummary.setText(
                            "Latest "
                                    + rows.size()
                                    + " records"

                                    + "  •  Pending: "
                                    + pendingCount

                                    + "  •  Sent: "
                                    + sentCount

                                    + "  •  Failed: "
                                    + failedCount
                    );
                },

                error -> {

                    scheduledSummary.setText(
                            "Refresh failed. Any displayed records may be outdated."
                    );


                    scheduledTable.setPlaceholder(
                            new Label(
                                    "Unable to load schedules. Click Refresh Scheduled."
                            )
                    );


                    showError(
                            "Unable to Load Scheduled Announcements",
                            "Please click Refresh Scheduled to try again."
                                    + "\n\n"
                                    + error.getMessage()
                    );
                }
        );
    }


    // =========================================================
    // CANCEL SCHEDULE
    // =========================================================

    private void cancelSelectedSchedule() {

        if (
                busy
                        || !active()
        ) {

            return;
        }


        JsonObject row =
                scheduledTable
                        .getSelectionModel()
                        .getSelectedItem();


        if (
                row == null
        ) {

            return;
        }


        String currentStatus =
                text(
                        row,
                        "status"
                );


        if (
                !"Pending".equals(
                        currentStatus
                )
                        && !"Paused".equals(
                        currentStatus
                )
        ) {

            showError(
                    "Cannot Cancel",
                    "Only pending or paused announcements can be cancelled."
            );

            return;
        }


        String scheduleId =
                text(
                        row,
                        "schedule_id"
                );


        String title =
                text(
                        row,
                        "subject"
                );


        String sendingTime =
                formatScheduledTime(
                        text(
                                row,
                                "scheduled_at"
                        )
                );


        if (
                !AEPMDialog.showConfirmation(
                        stage,

                        "Cancel Scheduled Announcement",

                        "Cancel \""
                                + title
                                + "\"?"

                                + "\n\nScheduled time: "
                                + sendingTime
                                + " MYT"

                                + "\n\nIf cancellation succeeds, this announcement "
                                + "will not be sent."
                )
        ) {

            return;
        }


        if (
                busy
                        || !active()
        ) {

            return;
        }


        JsonObject body =
                new JsonObject();


        body.addProperty(
                "p_schedule_id",
                scheduleId
        );


        status.setText(
                "Cancelling scheduled announcement..."
        );


        run(
                () ->
                        request(
                                "/rpc/cancel_scheduled_announcement",
                                body
                        ).getAsJsonObject(),

                result -> {

                    String resultStatus =
                            text(
                                    result,
                                    "status"
                            );


                    row.addProperty(
                            "status",
                            resultStatus
                    );


                    scheduledTable.refresh();


                    updateCancelScheduleControl();


                    if (
                            "Cancelled".equals(
                                    resultStatus
                            )
                    ) {

                        status.setText(
                                "Scheduled announcement cancelled."
                        );


                        AEPMDialog.showSuccess(
                                stage,
                                "Schedule Cancelled",
                                "\""
                                        + title
                                        + "\" has been cancelled."
                                        + "\nIt will not be sent."
                        );

                    } else if (
                            "Sent".equals(
                                    resultStatus
                            )
                    ) {

                        showError(
                                "Already Sent",
                                "This announcement was sent before cancellation "
                                        + "could complete. It cannot be recalled."
                        );

                    } else {

                        showError(
                                "Schedule Not Cancelled",
                                "The announcement's current status is: "
                                        + resultStatus
                                        + "\nRefresh the list to review its details."
                        );
                    }


                    if (
                            active()
                    ) {

                        loadScheduled();
                    }
                },

                error ->
                        showError(
                                "Cancellation Not Confirmed",
                                "The cancellation could not be confirmed."
                                        + "\nClick Refresh Scheduled to check its status."
                                        + "\nIf it is still Pending or Paused, "
                                        + "you can try cancelling again."
                                        + "\n\n"
                                        + error.getMessage()
                        )
        );
    }


    // =========================================================
    // SCHEDULE DETAILS
    // =========================================================

    private void showScheduledDetails() {

        if (
                busy
                        || !active()
        ) {

            return;
        }


        JsonObject row =
                scheduledTable
                        .getSelectionModel()
                        .getSelectedItem();


        if (
                row == null
        ) {

            return;
        }


        AnnouncementDetailsDialog.showScheduled(
                stage,
                text(
                        row,
                        "schedule_id"
                )
        );
    }


    // =========================================================
    // UPDATE SCHEDULE BUTTONS
    // =========================================================

    private void updateCancelScheduleControl() {

        JsonObject row =
                scheduledTable
                        .getSelectionModel()
                        .getSelectedItem();


        boolean pending =
                row != null
                        && "Pending".equals(
                        text(
                                row,
                                "status"
                        )
                );


        boolean paused =
                row != null
                        && "Paused".equals(
                        text(
                                row,
                                "status"
                        )
                );


        cancelScheduled.setDisable(
                busy
                        || (
                        !pending
                                && !paused
                )
        );


        boolean hasVersion =
                row != null
                        && row.has(
                        "updated_at"
                )
                        && !row
                        .get(
                                "updated_at"
                        )
                        .isJsonNull();


        rescheduleButton.setDisable(
                busy
                        || !pending
                        || !hasVersion
        );
    }


    // =========================================================
    // RESCHEDULE
    // =========================================================

    private void rescheduleSelectedAnnouncement() {

        if (
                busy
                        || !active()
        ) {

            return;
        }


        JsonObject selected =
                scheduledTable
                        .getSelectionModel()
                        .getSelectedItem();


        if (
                selected == null
                        || !"Pending".equals(
                        text(
                                selected,
                                "status"
                        )
                )
        ) {

            return;
        }


        JsonObject snapshot =
                selected.deepCopy();


        if (
                !snapshot.has(
                        "updated_at"
                )
                        || snapshot
                        .get(
                                "updated_at"
                        )
                        .isJsonNull()
        ) {

            showError(
                    "Refresh Required",
                    "Click Refresh Scheduled before changing the sending time."
            );

            return;
        }


        final ZonedDateTime oldTime;


        try {

            oldTime =
                    OffsetDateTime
                            .parse(
                                    text(
                                            snapshot,
                                            "scheduled_at"
                                    )
                            )
                            .atZoneSameInstant(
                                    ZoneId.of(
                                            "Asia/Kuala_Lumpur"
                                    )
                            );

        } catch (
                RuntimeException error
        ) {

            showError(
                    "Invalid Saved Time",
                    "The saved sending time could not be read. Refresh the list."
            );

            return;
        }


        Dialog<ButtonType> editor =
                new Dialog<>();


        editor.initOwner(
                stage
        );


        editor.initModality(
                Modality.WINDOW_MODAL
        );


        editor.setTitle(
                "Reschedule Announcement"
        );


        ButtonType saveType =
                new ButtonType(
                        "Save Changes",
                        ButtonBar.ButtonData.OK_DONE
                );


        editor
                .getDialogPane()
                .getButtonTypes()
                .addAll(
                        saveType,
                        ButtonType.CANCEL
                );


        DatePicker dateInput =
                new DatePicker(
                        oldTime.toLocalDate()
                );


        dateInput.setEditable(
                false
        );


        dateInput.setMaxWidth(
                Double.MAX_VALUE
        );


        TextField timeInput =
                new TextField(
                        oldTime.format(
                                DateTimeFormatter.ofPattern(
                                        "HH:mm"
                                )
                        )
                );


        timeInput.setPromptText(
                "HH:mm"
        );


        dateInput
                .getStyleClass()
                .add(
                        "admin-date-picker"
                );


        timeInput
                .getStyleClass()
                .add(
                        "admin-input"
                );


        Label heading =
                new Label(
                        "Reschedule Announcement"
                );


        heading
                .getStyleClass()
                .add(
                        "admin-page-title"
                );


        Label title =
                new Label(
                        text(
                                snapshot,
                                "subject"
                        )
                );


        title.setWrapText(
                true
        );


        Label current =
                new Label(
                        "Current sending time: "
                                + formatScheduledTime(
                                text(
                                        snapshot,
                                        "scheduled_at"
                                )
                        )
                                + " MYT"
                );


        current.setWrapText(
                true
        );


        current
                .getStyleClass()
                .add(
                        "admin-helper-text"
                );


        Label dateLabel =
                new Label(
                        "New sending date"
                );


        dateLabel
                .getStyleClass()
                .add(
                        "admin-field-label"
                );


        Label timeLabel =
                new Label(
                        "New sending time (MYT)"
                );


        timeLabel
                .getStyleClass()
                .add(
                        "admin-field-label"
                );


        Label hint =
                new Label(
                        "Use 24-hour time, for example 09:00 or 17:15."
                                + "\nThe current schedule remains active until you save."
                );


        hint.setWrapText(
                true
        );


        hint
                .getStyleClass()
                .add(
                        "admin-helper-text"
                );


        Label validation =
                new Label();


        validation.setWrapText(
                true
        );


        validation.setStyle(
                "-fx-text-fill: #B91C1C;"
        );


        VBox form =
                new VBox(
                        12,

                        heading,
                        title,
                        current,

                        dateLabel,
                        dateInput,

                        timeLabel,
                        timeInput,

                        hint,
                        validation
                );


        form.setPadding(
                new Insets(20)
        );


        form.setPrefWidth(
                440
        );


        form.setStyle(
                "-fx-background-color: #FFFFFF;"
        );


        editor
                .getDialogPane()
                .setContent(
                        form
                );


        editor
                .getDialogPane()
                .setStyle(
                        "-fx-background-color: #FFFFFF;"
                                + "-fx-font-family: 'Plus Jakarta Sans';"
                                + "-fx-font-size: 12px;"
                );


        var css =
                AdminAnnouncementsDialog
                        .class
                        .getResource(
                                "/css/app.css"
                        );


        if (
                css != null
        ) {

            editor
                    .getDialogPane()
                    .getStylesheets()
                    .add(
                            css.toExternalForm()
                    );
        }


        Button saveButton =
                (Button) editor
                        .getDialogPane()
                        .lookupButton(
                                saveType
                        );


        Button cancelButton =
                (Button) editor
                        .getDialogPane()
                        .lookupButton(
                                ButtonType.CANCEL
                        );


        saveButton
                .getStyleClass()
                .add(
                        "admin-button-primary"
                );


        cancelButton
                .getStyleClass()
                .add(
                        "admin-button"
                );


        final ZonedDateTime[] chosenTime =
                new ZonedDateTime[1];


        saveButton.addEventFilter(
                javafx.event.ActionEvent.ACTION,

                event -> {

                    validation.setText(
                            ""
                    );


                    if (
                            dateInput.getValue() == null
                    ) {

                        validation.setText(
                                "Please select a sending date."
                        );


                        event.consume();

                        return;
                    }


                    String timeText =
                            java.text.Normalizer
                                    .normalize(
                                            timeInput.getText(),
                                            java.text.Normalizer.Form.NFKC
                                    )
                                    .strip();


                    if (
                            !timeText.matches(
                                    "(?:[01]?[0-9]|2[0-3]):[0-5][0-9]"
                            )
                    ) {

                        validation.setText(
                                "Enter a valid time, such as 09:00 or 17:15."
                        );


                        event.consume();

                        return;
                    }


                    String[] parts =
                            timeText.split(
                                    ":"
                            );


                    ZonedDateTime newTime =
                            ZonedDateTime.of(
                                    dateInput.getValue(),

                                    LocalTime.of(
                                            Integer.parseInt(
                                                    parts[0]
                                            ),

                                            Integer.parseInt(
                                                    parts[1]
                                            )
                                    ),

                                    ZoneId.of(
                                            "Asia/Kuala_Lumpur"
                                    )
                            );


                    if (
                            !newTime
                                    .toInstant()
                                    .isAfter(
                                            java.time.Instant
                                                    .now()
                                                    .plusSeconds(
                                                            60
                                                    )
                                    )
                    ) {

                        validation.setText(
                                "Choose a sending time more than one minute from now."
                        );


                        event.consume();

                        return;
                    }


                    if (
                            newTime
                                    .toInstant()
                                    .equals(
                                            oldTime.toInstant()
                                    )
                    ) {

                        validation.setText(
                                "Choose a different sending date or time."
                        );


                        event.consume();

                        return;
                    }


                    chosenTime[0] =
                            newTime;
                }
        );


        var answer =
                editor.showAndWait();


        if (
                answer.isEmpty()

                        || answer.get() != saveType

                        || chosenTime[0] == null

                        || busy

                        || !active()
        ) {

            return;
        }


        String newTimeText =
                chosenTime[0]
                        .toOffsetDateTime()
                        .toString();


        if (
                !AEPMDialog.showConfirmation(
                        stage,
                        "Confirm Reschedule",

                        "Change the sending time for \""
                                + text(
                                snapshot,
                                "subject"
                        )
                                + "\"?"

                                + "\n\nFrom: "
                                + formatScheduledTime(
                                text(
                                        snapshot,
                                        "scheduled_at"
                                )
                        )
                                + " MYT"

                                + "\nTo: "
                                + formatScheduledTime(
                                newTimeText
                        )
                                + " MYT"
                )
        ) {

            return;
        }


        if (
                busy
                        || !active()
        ) {

            return;
        }


        JsonObject body =
                new JsonObject();


        body.addProperty(
                "p_schedule_id",
                text(
                        snapshot,
                        "schedule_id"
                )
        );


        body.addProperty(
                "p_scheduled_at",
                newTimeText
        );


        body.addProperty(
                "p_expected_updated_at",
                text(
                        snapshot,
                        "updated_at"
                )
        );


        status.setText(
                "Updating scheduled sending time..."
        );


        run(
                () ->
                        request(
                                "/rpc/reschedule_system_announcement",
                                body
                        ).getAsJsonObject(),

                result -> {

                    selected.addProperty(
                            "scheduled_at",
                            text(
                                    result,
                                    "scheduled_at"
                            )
                    );


                    selected.addProperty(
                            "updated_at",
                            text(
                                    result,
                                    "updated_at"
                            )
                    );


                    selected.addProperty(
                            "status",
                            text(
                                    result,
                                    "status"
                            )
                    );


                    scheduledTable.refresh();


                    updateCancelScheduleControl();


                    status.setText(
                            "Scheduled sending time updated."
                    );


                    AEPMDialog.showSuccess(
                            stage,
                            "Announcement Rescheduled",

                            "Saved sending time: "
                                    + formatScheduledTime(
                                    text(
                                            result,
                                            "scheduled_at"
                                    )
                            )
                                    + " MYT"
                    );


                    if (
                            active()
                    ) {

                        loadScheduled();
                    }
                },

                error -> {

                    showError(
                            "Reschedule Not Confirmed",

                            "The time change could not be confirmed."
                                    + "\nThe announcement may have changed or "
                                    + "already been sent, or the connection failed."
                                    + "\nRefresh Scheduled and check its saved time "
                                    + "before trying again."
                                    + "\n\n"
                                    + error.getMessage()
                    );


                    if (
                            active()
                    ) {

                        loadScheduled();
                    }
                }
        );
    }
}