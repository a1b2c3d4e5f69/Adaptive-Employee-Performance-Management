package com.aepm.ui.common.chat;

import com.aepm.logic.chat.ChatService;
import com.aepm.model.User;
import com.aepm.model.chat.ChatAttachment;
import com.aepm.model.chat.ChatConversation;
import com.aepm.model.chat.ChatMessage;
import com.aepm.model.chat.ChatUser;
import com.aepm.util.SessionManager;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.File;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

public class ChatController {

    private static final ZoneId MALAYSIA_ZONE =
            ZoneId.of("Asia/Kuala_Lumpur");

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern(
                    "hh:mm a",
                    Locale.ENGLISH
            );

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern(
                    "dd MMM yyyy, hh:mm a",
                    Locale.ENGLISH
            );

    private enum ConversationFilter {
        ALL,
        PRIVATE,
        TEAM
    }


    @FXML
    private Label roleBadgeLabel;

    @FXML
    private Button allFilterButton;

    @FXML
    private Button privateFilterButton;

    @FXML
    private Button teamFilterButton;

    @FXML
    private Button newTeamButton;

    @FXML
    private TextField searchField;

    @FXML
    private ListView<ChatConversation> conversationListView;

    @FXML
    private Label conversationStatusLabel;


    @FXML
    private HBox chatHeader;

    @FXML
    private Label activeChatInitialLabel;

    @FXML
    private Label activeChatNameLabel;

    @FXML
    private Label activeChatMetaLabel;

    @FXML
    private Button viewMembersButton;

    @FXML
    private Button sharedFilesButton;


    @FXML
    private ScrollPane messageScrollPane;

    @FXML
    private VBox messageContainer;

    @FXML
    private VBox emptyConversationBox;

    @FXML
    private VBox composerBox;

    @FXML
    private TextArea messageTextArea;

    @FXML
    private Button attachButton;

    @FXML
    private Button sendButton;

    @FXML
    private Label selectedFileLabel;

    @FXML
    private VBox detailsPanel;

    @FXML
    private Button detailsTabButton;

    @FXML
    private Button filesTabButton;

    @FXML
    private VBox chatDetailsContent;

    @FXML
    private VBox sharedFilesContent;

    @FXML
    private Label detailsInitialLabel;

    @FXML
    private Label detailsNameLabel;

    @FXML
    private Label detailsTypeLabel;

    @FXML
    private Label detailsMemberCountLabel;

    @FXML
    private Label detailsDescriptionLabel;

    @FXML
    private VBox memberPreviewBox;

    @FXML
    private Label sharedFilesEmptyLabel;


    private final ChatService chatService =
            new ChatService();

    private final ObservableList<ChatConversation> allConversations =
            FXCollections.observableArrayList();

    private final ObservableList<ChatConversation> filteredConversations =
            FXCollections.observableArrayList();

    private ConversationFilter currentFilter =
            ConversationFilter.ALL;

    private ChatConversation activeConversation;

    private File selectedFile;

    private final List<ChatAttachment> currentSharedAttachments =
            new ArrayList<>();

    private Timeline autoRefresh;

    private boolean loadingConversations;

    private boolean loadingMessages;


    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {

        configureRole();

        configureConversationList();

        configureSearch();

        configureMessageComposer();

        loadConversations(
                true
        );

        startAutoRefresh();
    }


    private void configureRole() {

        User currentUser =
                SessionManager.getCurrentUser();

        int roleId =
                currentUser == null
                        ? 0
                        : (int) currentUser.getRoleId();

        if (roleBadgeLabel != null) {
            roleBadgeLabel.setText(
                    switch (roleId) {
                        case 1 -> "ADMIN VIEW";
                        case 2 -> "MANAGER VIEW";
                        case 3 -> "EMPLOYEE VIEW";
                        default -> "SHARED MODULE";
                    }
            );
        }

        // Employees can participate in team chats but cannot create them.
        newTeamButton.setVisible(
                roleId == 1
                        || roleId == 2
        );

        newTeamButton.setManaged(
                roleId == 1
                        || roleId == 2
        );
    }


    private void configureConversationList() {

        conversationListView.setItems(
                filteredConversations
        );

        conversationListView.setCellFactory(
                list ->
                        new ConversationCell()
        );

        conversationListView
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> {

                            if (newValue != null) {

                                openConversation(
                                        newValue
                                );
                            }
                        }
                );
    }


    private void configureSearch() {

        searchField.textProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) ->
                                applyConversationFilter()
                );
    }


    private void configureMessageComposer() {

        messageTextArea.setOnKeyPressed(
                event -> {

                    if (
                            event.isControlDown()
                                    && "ENTER".equals(
                                            event.getCode().getName()
                                    )
                    ) {

                        handleSend();

                        event.consume();
                    }
                }
        );
    }


    // =========================================================
    // FILTER
    // =========================================================

    @FXML
    private void handleFilterAll() {

        currentFilter =
                ConversationFilter.ALL;

        updateFilterButtonStyles();

        applyConversationFilter();
    }


    @FXML
    private void handleFilterPrivate() {

        currentFilter =
                ConversationFilter.PRIVATE;

        updateFilterButtonStyles();

        applyConversationFilter();
    }


    @FXML
    private void handleFilterTeam() {

        currentFilter =
                ConversationFilter.TEAM;

        updateFilterButtonStyles();

        applyConversationFilter();
    }


    private void updateFilterButtonStyles() {

        setFilterStyle(
                allFilterButton,
                currentFilter == ConversationFilter.ALL
        );

        setFilterStyle(
                privateFilterButton,
                currentFilter == ConversationFilter.PRIVATE
        );

        setFilterStyle(
                teamFilterButton,
                currentFilter == ConversationFilter.TEAM
        );
    }


    private void setFilterStyle(
            Button button,
            boolean active
    ) {

        button.getStyleClass()
                .removeAll(
                        "chat-filter-button",
                        "chat-filter-button-active"
                );

        button.getStyleClass()
                .add(
                        active
                                ? "chat-filter-button-active"
                                : "chat-filter-button"
                );
    }


    private void applyConversationFilter() {

        String search =
                searchField.getText() == null
                        ? ""
                        : searchField
                                .getText()
                                .trim()
                                .toLowerCase(
                                        Locale.ROOT
                                );

        List<ChatConversation> values =
                allConversations
                        .stream()
                        .filter(
                                conversation -> {

                                    if (
                                            currentFilter == ConversationFilter.PRIVATE
                                                    && !conversation.isPrivateChat()
                                    ) {
                                        return false;
                                    }

                                    if (
                                            currentFilter == ConversationFilter.TEAM
                                                    && !conversation.isTeamChat()
                                    ) {
                                        return false;
                                    }

                                    if (search.isBlank()) {
                                        return true;
                                    }

                                    String displayName =
                                            safe(
                                                    conversation.getDisplayName()
                                            )
                                                    .toLowerCase(
                                                            Locale.ROOT
                                                    );

                                    String preview =
                                            safe(
                                                    conversation.getLastMessage()
                                            )
                                                    .toLowerCase(
                                                            Locale.ROOT
                                                    );

                                    return displayName.contains(
                                            search
                                    )
                                            || preview.contains(
                                                    search
                                            );
                                }
                        )
                        .sorted(
                                Comparator.comparing(
                                                ChatConversation::getUpdatedAt,
                                                Comparator.nullsLast(
                                                        Comparator.naturalOrder()
                                                )
                                        )
                                        .reversed()
                        )
                        .collect(
                                Collectors.toList()
                        );

        filteredConversations.setAll(
                values
        );

        conversationStatusLabel.setVisible(
                values.isEmpty()
        );

        conversationStatusLabel.setManaged(
                values.isEmpty()
        );

        conversationStatusLabel.setText(
                values.isEmpty()
                        ? "No conversations found."
                        : ""
        );
    }


    // =========================================================
    // LOAD CONVERSATIONS
    // =========================================================

    private void loadConversations(
            boolean selectFirst
    ) {

        if (loadingConversations) {
            return;
        }

        loadingConversations =
                true;

        UUID currentConversationId =
                activeConversation == null
                        ? null
                        : activeConversation.getConversationId();

        Task<List<ChatConversation>> task =
                new Task<>() {

                    @Override
                    protected List<ChatConversation> call()
                            throws Exception {

                        return chatService
                                .getConversations();
                    }
                };

        task.setOnSucceeded(
                event -> {

                    loadingConversations =
                            false;

                    allConversations.setAll(
                            task.getValue()
                    );

                    applyConversationFilter();

                    if (currentConversationId != null) {

                        ChatConversation matching =
                                allConversations
                                        .stream()
                                        .filter(
                                                conversation ->
                                                        currentConversationId.equals(
                                                                conversation.getConversationId()
                                                        )
                                        )
                                        .findFirst()
                                        .orElse(
                                                null
                                        );

                        if (matching != null) {

                            activeConversation =
                                    matching;

                            conversationListView
                                    .getSelectionModel()
                                    .select(
                                            matching
                                    );

                            refreshActiveConversationHeader();

                            return;
                        }
                    }

                    if (
                            selectFirst
                                    && !filteredConversations.isEmpty()
                    ) {

                        conversationListView
                                .getSelectionModel()
                                .select(
                                        0
                                );
                    }
                }
        );

        task.setOnFailed(
                event -> {

                    loadingConversations =
                            false;

                    Throwable error =
                            task.getException();

                    showConversationStatus(
                            error == null
                                    ? "Unable to load conversations."
                                    : error.getMessage()
                    );
                }
        );

        startTask(
                task
        );
    }


    // =========================================================
    // OPEN CONVERSATION
    // =========================================================

    private void openConversation(
            ChatConversation conversation
    ) {

        activeConversation =
                conversation;

        selectedFile =
                null;

        updateSelectedFileLabel();

        refreshActiveConversationHeader();

        composerBox.setDisable(
                false
        );

        viewMembersButton.setDisable(
                false
        );

        sharedFilesButton.setDisable(
                false
        );

        loadMessages(
                true
        );

        markActiveConversationRead();
    }


    private void refreshActiveConversationHeader() {

        if (activeConversation == null) {
            return;
        }

        String name =
                safe(
                        activeConversation.getDisplayName()
                );

        String initial =
                initial(
                        name
                );

        activeChatNameLabel.setText(
                name
        );

        activeChatInitialLabel.setText(
                initial
        );

        if (detailsNameLabel != null) {
            detailsNameLabel.setText(name);
        }

        if (detailsInitialLabel != null) {
            detailsInitialLabel.setText(initial);
        }

        int memberCount =
                activeConversation
                        .getMembers()
                        .size();

        String typeLabel =
                activeConversation.isTeamChat()
                        ? "Team Chat"
                        : "Private Chat";

        activeChatMetaLabel.setText(
                typeLabel
                        + "  •  "
                        + memberCount
                        + (
                        memberCount == 1
                                ? " Member"
                                : " Members"
                )
        );

        if (detailsTypeLabel != null) {
            detailsTypeLabel.setText(typeLabel);
        }

        if (detailsMemberCountLabel != null) {
            detailsMemberCountLabel.setText(
                    memberCount
                            + (
                            memberCount == 1
                                    ? " Member"
                                    : " Members"
                    )
            );
        }

        if (detailsDescriptionLabel != null) {
            detailsDescriptionLabel.setText(
                    activeConversation.isTeamChat()
                            ? "Team conversation for workplace coordination, task updates and shared files."
                            : "Private one-to-one conversation."
            );
        }

        if (memberPreviewBox != null) {
            populateMemberPreview();
        }
    }


    private void populateMemberPreview() {

        if (memberPreviewBox == null) {
            return;
        }

        memberPreviewBox
                .getChildren()
                .clear();

        if (activeConversation == null) {
            return;
        }

        List<ChatUser> members =
                activeConversation.getMembers();

        if (members.isEmpty()) {

            Label empty =
                    new Label(
                            "No member information available."
                    );

            empty.getStyleClass()
                    .add(
                            "chat-member-item"
                    );

            memberPreviewBox
                    .getChildren()
                    .add(
                            empty
                    );

            return;
        }

        int limit =
                Math.min(
                        members.size(),
                        6
                );

        for (int i = 0; i < limit; i++) {

            ChatUser user =
                    members.get(i);

            Label member =
                    new Label(
                            user.getFullName()
                                    + "  •  "
                                    + user.getRoleName()
                    );

            member.getStyleClass()
                    .add(
                            "chat-member-item"
                    );

            memberPreviewBox
                    .getChildren()
                    .add(
                            member
                    );
        }

        if (members.size() > limit) {

            Label more =
                    new Label(
                            "+"
                                    + (
                                    members.size()
                                            - limit
                            )
                                    + " more"
                    );

            more.getStyleClass()
                    .add(
                            "chat-member-item"
                    );

            memberPreviewBox
                    .getChildren()
                    .add(
                            more
                    );
        }
    }


    // =========================================================
    // MESSAGES
    // =========================================================

    private void loadMessages(
            boolean scrollToBottom
    ) {

        if (
                activeConversation == null
                        || loadingMessages
        ) {
            return;
        }

        loadingMessages =
                true;

        UUID conversationId =
                activeConversation
                        .getConversationId();

        Task<List<ChatMessage>> task =
                new Task<>() {

                    @Override
                    protected List<ChatMessage> call()
                            throws Exception {

                        return chatService
                                .getMessages(
                                        conversationId
                                );
                    }
                };

        task.setOnSucceeded(
                event -> {

                    loadingMessages =
                            false;

                    if (
                            activeConversation == null
                                    || !conversationId.equals(
                                            activeConversation.getConversationId()
                                    )
                    ) {
                        return;
                    }

                    renderMessages(
                            task.getValue()
                    );

                    if (scrollToBottom) {
                        scrollMessagesToBottom();
                    }

                    markActiveConversationRead();
                }
        );

        task.setOnFailed(
                event -> {

                    loadingMessages =
                            false;

                    Throwable error =
                            task.getException();

                    System.err.println(
                            "[Chat] Unable to load messages: "
                                    + (
                                    error == null
                                            ? "Unknown error"
                                            : error.getMessage()
                            )
                    );
                }
        );

        startTask(
                task
        );
    }


    private void renderMessages(
            List<ChatMessage> messages
    ) {
         messageContainer
            .getChildren()
            .clear();

        List<ChatAttachment> allAttachments =
                new ArrayList<>();

        if (
                messages == null
                        || messages.isEmpty()
        ) {

            VBox empty =
                    new VBox(
                            8
                    );

            empty.setAlignment(
                    Pos.CENTER
            );

            empty.setPadding(
                    new Insets(
                            80,
                            20,
                            80,
                            20
                    )
            );

            Label title =
                    new Label(
                            "No messages yet"
                    );

            title.getStyleClass()
                    .add(
                            "chat-empty-title"
                    );

            Label subtitle =
                    new Label(
                            "Send the first message in this conversation."
                    );

            subtitle.getStyleClass()
                    .add(
                            "chat-empty-subtitle"
                    );

            empty.getChildren()
                    .addAll(
                            title,
                            subtitle
                    );

            messageContainer
                    .getChildren()
                    .add(
                            empty
                    );

            updateSharedFiles(
                    allAttachments
            );

            return;
        }

        UUID currentUserId =
                currentUserId();

        for (ChatMessage message : messages) {

            boolean own =
                    currentUserId.equals(
                            message.getSenderId()
                    );

            HBox row =
                    createMessageRow(
                            message,
                            own
                    );

            messageContainer
                    .getChildren()
                    .add(
                            row
                    );

            allAttachments.addAll(
                    message.getAttachments()
            );
        }

        updateSharedFiles(
                allAttachments
        );
    }


    private HBox createMessageRow(
            ChatMessage message,
            boolean own
    ) {

        HBox row =
                new HBox(
                        8
                );

        row.getStyleClass()
                .add(
                        "chat-message-row"
                );

        row.setAlignment(
                own
                        ? Pos.TOP_RIGHT
                        : Pos.TOP_LEFT
        );


        VBox bubble =
                new VBox(
                        6
                );

        bubble.setMaxWidth(
                520
        );

        bubble.getStyleClass()
                .add(
                        own
                                ? "chat-message-bubble-own"
                                : "chat-message-bubble-other"
                );


        HBox meta =
                new HBox(
                        8
                );

        Label sender =
                new Label(
                        own
                                ? "You"
                                : safe(
                                        message.getSenderName()
                                )
                );

        sender.getStyleClass()
                .add(
                        "chat-message-sender"
                );

        Label time =
                new Label(
                        formatTime(
                                message.getCreatedAt()
                        )
                );

        time.getStyleClass()
                .add(
                        "chat-message-time"
                );

        meta.getChildren()
                .addAll(
                        sender,
                        time
                );


        String text =
                safe(
                        message.getMessageText()
                );

        if (!text.isBlank()) {

            Label textLabel =
                    new Label(
                            text
                    );

            textLabel.setWrapText(
                    true
            );

            textLabel.getStyleClass()
                    .add(
                            "chat-message-text"
                    );

            bubble.getChildren()
                    .addAll(
                            meta,
                            textLabel
                    );

        } else {

            bubble.getChildren()
                    .add(
                            meta
                    );
        }


        for (ChatAttachment attachment :
                message.getAttachments()) {

            bubble.getChildren()
                    .add(
                            createAttachmentCard(
                                    attachment
                            )
                    );
        }


        if (own) {

            Region spacer =
                    new Region();

            HBox.setHgrow(
                    spacer,
                    Priority.ALWAYS
            );

            row.getChildren()
                    .addAll(
                            spacer,
                            bubble
                    );

        } else {

            StackPane avatar =
                    createAvatar(
                            initial(
                                    message.getSenderName()
                            ),
                            34
                    );

            Region spacer =
                    new Region();

            HBox.setHgrow(
                    spacer,
                    Priority.ALWAYS
            );

            row.getChildren()
                    .addAll(
                            avatar,
                            bubble,
                            spacer
                    );
        }

        return row;
    }


    private VBox createAttachmentCard(
            ChatAttachment attachment
    ) {

        VBox card =
                new VBox(
                        4
                );

        card.getStyleClass()
                .add(
                        "chat-file-card"
                );


        Label name =
                new Label(
                        attachment.getFileName()
                );

        name.setWrapText(
                true
        );

        name.getStyleClass()
                .add(
                        "chat-file-name"
                );


        Label meta =
                new Label(
                        fileMeta(
                                attachment
                        )
                );

        meta.getStyleClass()
                .add(
                        "chat-file-meta"
                );


        Button open =
                new Button(
                        "Open File"
                );

        open.getStyleClass()
                .add(
                        "chat-file-open-button"
                );

        open.setOnAction(
                event ->
                        openAttachment(
                                attachment
                        )
        );


        Region spacer =
                new Region();

        HBox.setHgrow(
                spacer,
                Priority.ALWAYS
        );

        HBox row =
                new HBox(
                        10,
                        new VBox(
                                3,
                                name,
                                meta
                        ),
                        spacer,
                        open
                );

        row.setAlignment(
                Pos.CENTER_LEFT
        );

        card.getChildren()
                .add(
                        row
                );

        return card;
    }


    // =========================================================
    // SEND
    // =========================================================

    @FXML
    private void handleSend() {

        if (activeConversation == null) {

            showError(
                    "No Conversation",
                    "Select a conversation first."
            );

            return;
        }

        String message =
                messageTextArea
                        .getText()
                        .trim();

        if (
                message.isBlank()
                        && selectedFile == null
        ) {

            return;
        }

        sendButton.setDisable(
                true
        );

        attachButton.setDisable(
                true
        );


        File fileSnapshot =
                selectedFile;

        String messageSnapshot =
                message;

        UUID conversationId =
                activeConversation
                        .getConversationId();


        Task<Void> task =
                new Task<>() {

                    @Override
                    protected Void call()
                            throws Exception {

                        if (fileSnapshot != null) {

                            chatService
                                    .sendAttachment(
                                            conversationId,
                                            fileSnapshot,
                                            messageSnapshot
                                    );

                        } else {

                            chatService
                                    .sendMessage(
                                            conversationId,
                                            messageSnapshot
                                    );
                        }

                        return null;
                    }
                };


        task.setOnSucceeded(
                event -> {

                    sendButton.setDisable(
                            false
                    );

                    attachButton.setDisable(
                            false
                    );

                    messageTextArea.clear();

                    selectedFile =
                            null;

                    updateSelectedFileLabel();

                    loadMessages(
                            true
                    );

                    loadConversations(
                            false
                    );
                }
        );


        task.setOnFailed(
                event -> {

                    sendButton.setDisable(
                            false
                    );

                    attachButton.setDisable(
                            false
                    );

                    Throwable error =
                            task.getException();

                    showError(
                            "Unable to Send",
                            error == null
                                    ? "Unable to send the message."
                                    : error.getMessage()
                    );
                }
        );

        startTask(
                task
        );
    }


    @FXML
    private void handleAttachFile() {

        if (activeConversation == null) {
            return;
        }

        FileChooser chooser =
                new FileChooser();

        chooser.setTitle(
                "Select Chat Attachment"
        );

        chooser.getExtensionFilters()
                .addAll(
                        new FileChooser.ExtensionFilter(
                                "Supported Files",
                                "*.pdf",
                                "*.doc",
                                "*.docx",
                                "*.xls",
                                "*.xlsx",
                                "*.png",
                                "*.jpg",
                                "*.jpeg",
                                "*.txt"
                        ),
                        new FileChooser.ExtensionFilter(
                                "PDF",
                                "*.pdf"
                        ),
                        new FileChooser.ExtensionFilter(
                                "Documents",
                                "*.doc",
                                "*.docx",
                                "*.txt"
                        ),
                        new FileChooser.ExtensionFilter(
                                "Spreadsheets",
                                "*.xls",
                                "*.xlsx"
                        ),
                        new FileChooser.ExtensionFilter(
                                "Images",
                                "*.png",
                                "*.jpg",
                                "*.jpeg"
                        )
                );

        File file =
                chooser.showOpenDialog(
                        owner()
                );

        if (file == null) {
            return;
        }

        try {

            chatService
                    .validateAttachment(
                            file
                    );

            selectedFile =
                    file;

            updateSelectedFileLabel();

        } catch (Exception exception) {

            showError(
                    "Invalid Attachment",
                    exception.getMessage()
            );
        }
    }


    private void updateSelectedFileLabel() {

        boolean hasFile =
                selectedFile != null;

        selectedFileLabel.setVisible(
                hasFile
        );

        selectedFileLabel.setManaged(
                hasFile
        );

        selectedFileLabel.setText(
                hasFile
                        ? "Selected: "
                        + selectedFile.getName()
                        : ""
        );
    }


    // =========================================================
    // PRIVATE CHAT
    // =========================================================

    @FXML
    private void handleNewPrivateChat() {

        runUserSelectionDialog(
                false
        );
    }


    private void runUserSelectionDialog(
            boolean teamMode
    ) {

        Task<List<ChatUser>> task =
                new Task<>() {

                    @Override
                    protected List<ChatUser> call()
                            throws Exception {

                        return chatService
                                .getAvailableChatUsers();
                    }
                };

        task.setOnSucceeded(
                event -> {

                    List<ChatUser> users =
                            task.getValue();

                    if (
                            users == null
                                    || users.isEmpty()
                    ) {

                        showError(
                                "No Users",
                                "No available users were found."
                        );

                        return;
                    }

                    if (teamMode) {

                        showCreateTeamDialog(
                                users
                        );

                    } else {

                        showPrivateChatDialog(
                                users
                        );
                    }
                }
        );

        task.setOnFailed(
                event -> {

                    Throwable error =
                            task.getException();

                    showError(
                            "Unable to Load Users",
                            error == null
                                    ? "Unable to load available chat users."
                                    : error.getMessage()
                    );
                }
        );

        startTask(
                task
        );
    }


    private void showPrivateChatDialog(
            List<ChatUser> users
    ) {

        Dialog<ChatUser> dialog =
                new Dialog<>();

        dialog.initOwner(
                owner()
        );

        dialog.setTitle(
                "New Private Chat"
        );

        ButtonType openType =
                new ButtonType(
                        "Open Chat",
                        ButtonBar.ButtonData.OK_DONE
                );

        dialog.getDialogPane()
                .getButtonTypes()
                .addAll(
                        openType,
                        ButtonType.CANCEL
                );


        ListView<ChatUser> list =
        new ListView<>(
                FXCollections.observableArrayList(
                        users
                )
        );

        list.setPrefSize(
                420,
                360
        );

        list.setStyle(
                "-fx-background-color: #F8FAFD;"
                        + "-fx-control-inner-background: #F8FAFD;"
                        + "-fx-selection-bar: transparent;"
                        + "-fx-selection-bar-non-focused: transparent;"
                        + "-fx-focus-color: transparent;"
                        + "-fx-faint-focus-color: transparent;"
        );

        list.setCellFactory(
                value ->
                        new UserSelectionCell()
        );

        dialog.getDialogPane()
                .setContent(
                        list
                );

        styleCommonDialog(dialog);


        dialog.setResultConverter(
                button -> {

                    if (button == openType) {
                        return list
                                .getSelectionModel()
                                .getSelectedItem();
                    }

                    return null;
                }
        );


        dialog.showAndWait()
                .ifPresent(
                        this::createOrOpenPrivateChat
                );
    }


    private void createOrOpenPrivateChat(
            ChatUser user
    ) {

        Task<UUID> task =
                new Task<>() {

                    @Override
                    protected UUID call()
                            throws Exception {

                        return chatService
                                .openPrivateChat(
                                        user.getUserId()
                                );
                    }
                };

        task.setOnSucceeded(
                event -> {

                    UUID conversationId =
                            task.getValue();

                    loadConversationsAndSelect(
                            conversationId
                    );
                }
        );

        task.setOnFailed(
                event -> {

                    Throwable error =
                            task.getException();

                    showError(
                            "Unable to Open Chat",
                            error == null
                                    ? "Unable to open private chat."
                                    : error.getMessage()
                    );
                }
        );

        startTask(
                task
        );
    }


    // =========================================================
    // TEAM CHAT
    // =========================================================

    @FXML
    private void handleNewTeamChat() {

        runUserSelectionDialog(
                true
        );
    }


    private void showCreateTeamDialog(
            List<ChatUser> users
    ) {

        Dialog<TeamCreationRequest> dialog =
                new Dialog<>();

        dialog.initOwner(
                owner()
        );

        dialog.setTitle(
                "Create Team Chat"
        );

        ButtonType createType =
                new ButtonType(
                        "Create Team",
                        ButtonBar.ButtonData.OK_DONE
                );

        dialog.getDialogPane()
                .getButtonTypes()
                .addAll(
                        createType,
                        ButtonType.CANCEL
                );


        TextField teamName =
                new TextField();

        teamName.setPromptText(
                "Team name"
        );


        VBox userBox =
                new VBox(
                        7
                );

        List<CheckBox> choices =
                new ArrayList<>();

        for (ChatUser user : users) {

            CheckBox option =
                    new CheckBox(
                            user.getFullName()
                                    + "  •  "
                                    + user.getRoleName()
                                    + (
                                    user.getEmployeeId() == null
                                            || user.getEmployeeId().isBlank()
                                            ? ""
                                            : "  •  "
                                            + user.getEmployeeId()
                            )
                    );

            option.setUserData(
                    user
            );

            choices.add(
                    option
            );

            userBox.getChildren()
                    .add(
                            option
                    );
        }


        ScrollPane usersScroll =
                new ScrollPane(
                        userBox
                );

        usersScroll.setFitToWidth(
                true
        );

        usersScroll.setPrefHeight(
                320
        );


        VBox content =
                new VBox(
                        12,
                        new Label(
                                "Team Name"
                        ),
                        teamName,
                        new Label(
                                "Members"
                        ),
                        usersScroll
                );

        content.setPadding(
                new Insets(
                        8
                )
        );

        content.setPrefWidth(
                460
        );


        dialog.getDialogPane()
                .setContent(
                        content
                );

        styleCommonDialog(dialog);


        dialog.setResultConverter(
                button -> {

                    if (button != createType) {
                        return null;
                    }

                    String name =
                            teamName
                                    .getText()
                                    .trim();

                    List<UUID> selected =
                            choices
                                    .stream()
                                    .filter(
                                            CheckBox::isSelected
                                    )
                                    .map(
                                            checkBox ->
                                                    (ChatUser) checkBox.getUserData()
                                    )
                                    .map(
                                            ChatUser::getUserId
                                    )
                                    .collect(
                                            Collectors.toList()
                                    );

                    return new TeamCreationRequest(
                            name,
                            selected
                    );
                }
        );


        dialog.showAndWait()
                .ifPresent(
                        this::createTeamChat
                );
    }


    private void createTeamChat(
            TeamCreationRequest request
    ) {

        if (
                request.teamName() == null
                        || request.teamName()
                                .trim()
                                .length() < 3
        ) {

            showError(
                    "Invalid Team Name",
                    "Team name must contain at least 3 characters."
            );

            return;
        }


        Task<UUID> task =
                new Task<>() {

                    @Override
                    protected UUID call()
                            throws Exception {

                        return chatService
                                .createTeamChat(
                                        request.teamName(),
                                        request.memberIds()
                                );
                    }
                };


        task.setOnSucceeded(
                event -> {

                    loadConversationsAndSelect(
                            task.getValue()
                    );
                }
        );


        task.setOnFailed(
                event -> {

                    Throwable error =
                            task.getException();

                    showError(
                            "Unable to Create Team",
                            error == null
                                    ? "Unable to create team chat."
                                    : error.getMessage()
                    );
                }
        );

        startTask(
                task
        );
    }


    private void loadConversationsAndSelect(
            UUID conversationId
    ) {

        Task<List<ChatConversation>> task =
                new Task<>() {

                    @Override
                    protected List<ChatConversation> call()
                            throws Exception {

                        return chatService
                                .getConversations();
                    }
                };

        task.setOnSucceeded(
                event -> {

                    allConversations.setAll(
                            task.getValue()
                    );

                    applyConversationFilter();

                    ChatConversation matching =
                            allConversations
                                    .stream()
                                    .filter(
                                            conversation ->
                                                    conversationId.equals(
                                                            conversation.getConversationId()
                                                    )
                                    )
                                    .findFirst()
                                    .orElse(
                                            null
                                    );

                    if (matching != null) {

                        conversationListView
                                .getSelectionModel()
                                .select(
                                        matching
                                );

                        conversationListView.scrollTo(
                                matching
                        );
                    }
                }
        );

        task.setOnFailed(
                event -> {

                    Throwable error =
                            task.getException();

                    showError(
                            "Unable to Refresh Chats",
                            error == null
                                    ? "Unable to refresh conversations."
                                    : error.getMessage()
                    );
                }
        );

        startTask(
                task
        );
    }


    // =========================================================
    // DETAILS / MEMBERS / FILES
    // =========================================================

    @FXML
    private void handleShowDetails() {
        // Right-side details panel removed intentionally.
    }


    @FXML
    private void handleShowSharedFiles() {

        if (activeConversation == null) {
            return;
        }

        Dialog<Void> dialog = new Dialog<>();

        if (owner() != null) {
            dialog.initOwner(owner());
        }

        dialog.setTitle("Shared Files");
        dialog.getDialogPane()
                .getButtonTypes()
                .add(ButtonType.CLOSE);

        VBox content = new VBox(10);
        content.setPadding(new Insets(16));

        Label heading = new Label("Shared Files");
        heading.setStyle(
                "-fx-font-family: 'Plus Jakarta Sans';"
                        + "-fx-font-size: 18px;"
                        + "-fx-font-weight: 800;"
                        + "-fx-text-fill: #10264A;"
        );

        Label subtitle = new Label(
                activeConversation.getDisplayName()
        );
        subtitle.setStyle(
                "-fx-font-family: 'Plus Jakarta Sans';"
                        + "-fx-font-size: 11px;"
                        + "-fx-text-fill: #7184A4;"
        );

        content.getChildren().addAll(
                heading,
                subtitle
        );

        if (currentSharedAttachments.isEmpty()) {

            Label empty = new Label(
                    "No files have been shared in this conversation."
            );

            empty.setWrapText(true);
            empty.setStyle(
                    "-fx-font-family: 'Plus Jakarta Sans';"
                            + "-fx-text-fill: #7184A4;"
                            + "-fx-padding: 18 0 10 0;"
            );

            content.getChildren().add(empty);

        } else {

            for (ChatAttachment attachment :
                    currentSharedAttachments) {

                content.getChildren().add(
                        createAttachmentCard(
                                attachment
                        )
                );
            }
        }

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(
                ScrollPane.ScrollBarPolicy.NEVER
        );
        scroll.setPrefSize(500, 420);

        dialog.getDialogPane().setContent(scroll);

        styleCommonDialog(dialog);

        dialog.showAndWait();
    }


    @FXML
    private void handleViewMembers() {

        if (activeConversation == null) {
            return;
        }

        Dialog<Void> dialog =
                new Dialog<>();

        dialog.initOwner(
                owner()
        );

        dialog.setTitle(
                "Conversation Members"
        );

        dialog.getDialogPane()
                .getButtonTypes()
                .add(
                        ButtonType.CLOSE
                );


        VBox content =
                new VBox(
                        8
                );

        content.setPadding(
                new Insets(
                        12
                )
        );

        for (ChatUser user :
                activeConversation.getMembers()) {

            Label item =
                    new Label(
                            user.getFullName()
                                    + "  •  "
                                    + user.getRoleName()
                                    + (
                                    user.getEmployeeId() == null
                                            || user.getEmployeeId().isBlank()
                                            ? ""
                                            : "  •  "
                                            + user.getEmployeeId()
                            )
                    );

            content.getChildren()
                    .add(
                            item
                    );
        }

        ScrollPane scroll =
                new ScrollPane(
                        content
                );

        scroll.setFitToWidth(
                true
        );

        scroll.setPrefSize(
                420,
                360
        );

        dialog.getDialogPane()
                .setContent(
                        scroll
                );

        styleCommonDialog(dialog);

        dialog.showAndWait();
    }


    private void updateSharedFiles(
            List<ChatAttachment> attachments
    ) {

        currentSharedAttachments.clear();

        if (attachments == null
                || attachments.isEmpty()) {
            return;
        }

        currentSharedAttachments.addAll(
                attachments
        );

        currentSharedAttachments.sort(
                Comparator.comparing(
                                ChatAttachment::getUploadedAt,
                                Comparator.nullsLast(
                                        Comparator.naturalOrder()
                                )
                        )
                        .reversed()
        );
    }


    private void openAttachment(
            ChatAttachment attachment
    ) {

        Task<Void> task =
                new Task<>() {

                    @Override
                    protected Void call()
                            throws Exception {

                        chatService
                                .openAttachment(
                                        attachment
                                );

                        return null;
                    }
                };


        task.setOnFailed(
                event -> {

                    Throwable error =
                            task.getException();

                    showError(
                            "Unable to Open File",
                            error == null
                                    ? "Unable to open attachment."
                                    : error.getMessage()
                    );
                }
        );

        startTask(
                task
        );
    }


    // =========================================================
    // MARK READ
    // =========================================================

    private void markActiveConversationRead() {

        if (activeConversation == null) {
            return;
        }

        UUID id =
                activeConversation
                        .getConversationId();

        Task<Void> task =
                new Task<>() {

                    @Override
                    protected Void call()
                            throws Exception {

                        chatService
                                .markRead(
                                        id
                                );

                        return null;
                    }
                };

        task.setOnSucceeded(
                event -> {

                    if (
                            activeConversation != null
                                    && id.equals(
                                            activeConversation.getConversationId()
                                    )
                    ) {

                        activeConversation.setUnreadCount(
                                0
                        );

                        conversationListView.refresh();
                    }
                }
        );

        startTask(
                task
        );
    }


    // =========================================================
    // AUTO REFRESH
    // =========================================================

    private void startAutoRefresh() {

        autoRefresh =
                new Timeline(
                        new KeyFrame(
                                Duration.seconds(
                                        5
                                ),
                                event -> {

                                    loadConversations(
                                            false
                                    );

                                    if (activeConversation != null) {

                                        loadMessages(
                                                false
                                        );
                                    }
                                }
                        )
                );

        autoRefresh.setCycleCount(
                Timeline.INDEFINITE
        );

        autoRefresh.play();
    }


    public void stop() {

        if (autoRefresh != null) {

            autoRefresh.stop();
        }
    }


    private int currentRoleId() {

        User user = SessionManager.getCurrentUser();

        return user == null
                ? 0
                : (int) user.getRoleId();
    }


    private String rolePrimaryColor() {

        return switch (currentRoleId()) {
            case 1 -> "#1E3A8A";
            case 2 -> "#0F766E";
            case 3 -> "#FFC107";
            default -> "#2563EB";
        };
    }


    private String roleHoverColor() {

        return switch (currentRoleId()) {
            case 1 -> "#172F73";
            case 2 -> "#115E59";
            case 3 -> "#E0A800";
            default -> "#1D4ED8";
        };
    }


    private String roleSoftColor() {

        return switch (currentRoleId()) {
            case 1 -> "#E8EEFC";
            case 2 -> "#DFF7F2";
            case 3 -> "#FFF8E1";
            default -> "#EFF6FF";
        };
    }


    private String rolePrimaryTextColor() {

        return currentRoleId() == 3
                ? "#0F172A"
                : "#FFFFFF";
    }


    private void styleCommonDialog(
            Dialog<?> dialog
    ) {

        String primary = rolePrimaryColor();
        String hover = roleHoverColor();
        String soft = roleSoftColor();
        String primaryText = rolePrimaryTextColor();

        dialog.getDialogPane().setStyle(
                "-fx-font-family: 'Plus Jakarta Sans';"
                        + "-fx-background-color: #FFFFFF;"
                        + "-fx-border-color: #DCE6F4;"
                        + "-fx-border-width: 1;"
        );

        for (ButtonType type :
                dialog.getDialogPane()
                        .getButtonTypes()) {

            Button button =
                    (Button) dialog.getDialogPane()
                            .lookupButton(type);

            if (button == null) {
                continue;
            }

            boolean primaryAction =
                    type.getButtonData()
                            == ButtonBar.ButtonData.OK_DONE
                    || type.getButtonData()
                            == ButtonBar.ButtonData.FINISH
                    || type.getButtonData()
                            == ButtonBar.ButtonData.YES;

            if (primaryAction) {

                String base =
                        "-fx-background-color: "
                                + primary
                                + ";"
                                + "-fx-border-color: "
                                + primary
                                + ";"
                                + "-fx-text-fill: "
                                + primaryText
                                + ";"
                                + "-fx-font-weight: 800;"
                                + "-fx-background-radius: 8;"
                                + "-fx-border-radius: 8;"
                                + "-fx-padding: 8 18;"
                                + "-fx-cursor: hand;";

                String over =
                        "-fx-background-color: "
                                + hover
                                + ";"
                                + "-fx-border-color: "
                                + hover
                                + ";"
                                + "-fx-text-fill: "
                                + primaryText
                                + ";"
                                + "-fx-font-weight: 800;"
                                + "-fx-background-radius: 8;"
                                + "-fx-border-radius: 8;"
                                + "-fx-padding: 8 18;"
                                + "-fx-cursor: hand;";

                button.setStyle(base);
                button.setOnMouseEntered(
                        e -> button.setStyle(over)
                );
                button.setOnMouseExited(
                        e -> button.setStyle(base)
                );

            } else {

                String base =
                        "-fx-background-color: #FFFFFF;"
                                + "-fx-border-color: "
                                + primary
                                + ";"
                                + "-fx-text-fill: "
                                + primary
                                + ";"
                                + "-fx-font-weight: 700;"
                                + "-fx-background-radius: 8;"
                                + "-fx-border-radius: 8;"
                                + "-fx-padding: 8 18;"
                                + "-fx-cursor: hand;";

                String over =
                        "-fx-background-color: "
                                + soft
                                + ";"
                                + "-fx-border-color: "
                                + hover
                                + ";"
                                + "-fx-text-fill: "
                                + hover
                                + ";"
                                + "-fx-font-weight: 700;"
                                + "-fx-background-radius: 8;"
                                + "-fx-border-radius: 8;"
                                + "-fx-padding: 8 18;"
                                + "-fx-cursor: hand;";

                button.setStyle(base);
                button.setOnMouseEntered(
                        e -> button.setStyle(over)
                );
                button.setOnMouseExited(
                        e -> button.setStyle(base)
                );
            }
        }
    }


    // =========================================================
    // UI HELPERS
    // =========================================================

    private StackPane createAvatar(
            String text,
            double size
    ) {

        StackPane avatar =
                new StackPane();

        avatar.setMinSize(
                size,
                size
        );

        avatar.setPrefSize(
                size,
                size
        );

        avatar.setMaxSize(
                size,
                size
        );

        avatar.getStyleClass()
                .add(
                        "chat-avatar"
                );


        Label label =
                new Label(
                        text
                );

        label.getStyleClass()
                .add(
                        "chat-avatar-text"
                );


        avatar.getChildren()
                .add(
                        label
                );

        return avatar;
    }


    private void scrollMessagesToBottom() {

        Platform.runLater(
                () ->
                        messageScrollPane.setVvalue(
                                1.0
                        )
        );
    }


    private void showConversationStatus(
            String message
    ) {

        conversationStatusLabel.setText(
                safe(
                        message
                )
        );

        conversationStatusLabel.setVisible(
                true
        );

        conversationStatusLabel.setManaged(
                true
        );
    }


    private void showError(
            String title,
            String message
    ) {

        Alert alert =
                new Alert(
                        Alert.AlertType.ERROR
                );

        if (owner() != null) {
            alert.initOwner(
                    owner()
            );
        }

        alert.setTitle(
                title
        );

        alert.setHeaderText(
                null
        );

        alert.setContentText(
                safe(
                        message
                )
        );

        alert.showAndWait();
    }


    private Window owner() {

        if (
                conversationListView == null
                        || conversationListView.getScene() == null
        ) {
            return null;
        }

        return conversationListView
                .getScene()
                .getWindow();
    }


    private UUID currentUserId() {

        User user =
                SessionManager.getCurrentUser();

        if (
                user == null
                        || user.getUserId() == null
                        || user.getUserId().isBlank()
        ) {
            throw new IllegalStateException(
                    "No logged-in user found."
            );
        }

        return UUID.fromString(
                user.getUserId()
        );
    }


    private String fileMeta(
            ChatAttachment attachment
    ) {

        String size =
                formatFileSize(
                        attachment.getFileSize()
                );

        String time =
                attachment.getUploadedAt() == null
                        ? ""
                        : " • "
                        + formatDateTime(
                                attachment.getUploadedAt()
                        );

        String type =
                safe(
                        attachment.getFileType()
                );

        if (type.isBlank()) {
            type = "File";
        }

        return type
                + " • "
                + size
                + time;
    }


    private String formatFileSize(
            long bytes
    ) {

        if (bytes < 1024) {
            return bytes + " B";
        }

        double kb =
                bytes / 1024.0;

        if (kb < 1024) {
            return String.format(
                    Locale.ENGLISH,
                    "%.1f KB",
                    kb
            );
        }

        return String.format(
                Locale.ENGLISH,
                "%.1f MB",
                kb / 1024.0
        );
    }


    private String formatTime(
            OffsetDateTime value
    ) {

        if (value == null) {
            return "";
        }

        return TIME_FORMAT.format(
                value
                        .atZoneSameInstant(
                                MALAYSIA_ZONE
                        )
        );
    }


    private String formatDateTime(
            OffsetDateTime value
    ) {

        if (value == null) {
            return "";
        }

        return DATE_TIME_FORMAT.format(
                value
                        .atZoneSameInstant(
                                MALAYSIA_ZONE
                        )
        );
    }


    private String initial(
            String value
    ) {

        if (
                value == null
                        || value.isBlank()
        ) {
            return "C";
        }

        String[] words =
                value
                        .trim()
                        .split(
                                "\\s+"
                        );

        if (words.length == 1) {

            return words[0]
                    .substring(
                            0,
                            Math.min(
                                    2,
                                    words[0].length()
                            )
                    )
                    .toUpperCase(
                            Locale.ROOT
                    );
        }

        return (
                words[0].substring(
                        0,
                        1
                )
                        + words[
                        words.length - 1
                        ].substring(
                        0,
                        1
                )
        ).toUpperCase(
                Locale.ROOT
        );
    }


    private String safe(
            String value
    ) {

        return value == null
                ? ""
                : value;
    }


    private void startTask(
            Task<?> task
    ) {

        Thread thread =
                new Thread(
                        task
                );

        thread.setDaemon(
                true
        );

        thread.start();
    }


    // =========================================================
    // CELL: CONVERSATION
    // =========================================================

    private class ConversationCell
            extends ListCell<ChatConversation> {

        @Override
        protected void updateItem(
                ChatConversation conversation,
                boolean empty
        ) {

            super.updateItem(
                    conversation,
                    empty
            );

            if (
                    empty
                            || conversation == null
            ) {

                setText(
                        null
                );

                setGraphic(
                        null
                );

                return;
            }


            StackPane avatar =
                    createAvatar(
                            initial(
                                    conversation.getDisplayName()
                            ),
                            42
                    );

            Label name =
                    new Label(
                            safe(
                                    conversation.getDisplayName()
                            )
                    );

            name.getStyleClass()
                    .add(
                            "chat-conversation-name"
                    );


            String previewText =
                    safe(
                            conversation.getLastMessage()
                    );

            if (previewText.isBlank()) {
                previewText = "No messages yet";
            }

            if (previewText.length() > 42) {

                previewText =
                        previewText.substring(
                                0,
                                39
                        )
                                + "...";
            }

            Label preview =
                    new Label(
                            previewText
                    );

            preview.getStyleClass()
                    .add(
                            "chat-conversation-preview"
                    );


            VBox text =
                    new VBox(
                            4,
                            name,
                            preview
                    );

            HBox.setHgrow(
                    text,
                    Priority.ALWAYS
            );


            Label time =
                    new Label(
                            formatTime(
                                    conversation.getLastMessageAt()
                            )
                    );

            time.getStyleClass()
                    .add(
                            "chat-conversation-time"
                    );


            VBox right =
                    new VBox(
                            6,
                            time
                    );

            right.setAlignment(
                    Pos.CENTER_RIGHT
            );


            if (conversation.getUnreadCount() > 0) {

                Label unread =
                        new Label(
                                String.valueOf(
                                        conversation.getUnreadCount()
                                )
                        );

                unread.getStyleClass()
                        .add(
                                "chat-unread-badge"
                        );

                right.getChildren()
                        .add(
                                unread
                        );
            }


            HBox card =
                    new HBox(
                            10,
                            avatar,
                            text,
                            right
                    );

            card.setAlignment(
                    Pos.CENTER_LEFT
            );

            card.getStyleClass()
                    .add(
                            isSelected()
                                    ? "chat-conversation-card-selected"
                                    : "chat-conversation-card"
                    );


            setGraphic(
                    card
            );

            setText(
                    null
            );
        }
    }


    // =========================================================
    // CELL: USER SELECTION
    // =========================================================

    private class UserSelectionCell
        extends ListCell<ChatUser> {

    private VBox card;


    @Override
    protected void updateItem(
            ChatUser user,
            boolean empty
    ) {

        super.updateItem(
                user,
                empty
        );


        if (empty || user == null) {

            card = null;

            setText(null);
            setGraphic(null);

            setStyle(
                    "-fx-background-color: transparent;"
            );

            return;
        }


        // =====================================================
        // NAME
        // =====================================================

        Label name =
                new Label(
                        user.getFullName()
                );

        name.setStyle(
                "-fx-font-family: 'Plus Jakarta Sans';"
                        + "-fx-font-size: 13px;"
                        + "-fx-font-weight: 800;"
                        + "-fx-text-fill: #10264A;"
        );


        // =====================================================
        // ROLE + EMPLOYEE ID
        // =====================================================

        Label detail =
                new Label(
                        user.getRoleName()
                                + (
                                user.getEmployeeId() == null
                                        || user.getEmployeeId().isBlank()

                                        ? ""

                                        : "  •  "
                                        + user.getEmployeeId()
                        )
                );

        detail.setStyle(
                "-fx-font-family: 'Plus Jakarta Sans';"
                        + "-fx-font-size: 10px;"
                        + "-fx-text-fill: #7184A4;"
        );


        // =====================================================
        // CARD
        // =====================================================

        card =
                new VBox(
                        4,
                        name,
                        detail
                );

        card.setPadding(
                new Insets(
                        11,
                        12,
                        11,
                        12
                )
        );

        card.setMaxWidth(
                Double.MAX_VALUE
        );


        setGraphic(
                card
        );

        setText(
                null
        );

        setStyle(
                "-fx-background-color: transparent;"
                        + "-fx-padding: 4 5;"
        );


        applySelectionStyle();
    }


    @Override
    public void updateSelected(
            boolean selected
    ) {

        super.updateSelected(
                selected
        );

        applySelectionStyle();
    }


    private void applySelectionStyle() {

        if (card == null) {
            return;
        }


        if (!isSelected()) {

            card.setStyle(
                    "-fx-background-color: #FFFFFF;"
                            + "-fx-border-color: #E4EAF3;"
                            + "-fx-border-width: 1;"
                            + "-fx-background-radius: 9;"
                            + "-fx-border-radius: 9;"
            );

            return;
        }


        // =====================================================
        // EMPLOYEE = YELLOW
        // =====================================================

        if (currentRoleId() == 3) {

            card.setStyle(
                    "-fx-background-color: #FFF8E1;"
                            + "-fx-border-color: #FFC107;"
                            + "-fx-border-width: 1.5;"
                            + "-fx-background-radius: 9;"
                            + "-fx-border-radius: 9;"
            );

            return;
        }


        // =====================================================
        // MANAGER = TEAL
        // =====================================================

        if (currentRoleId() == 2) {

            card.setStyle(
                    "-fx-background-color: #DFF7F2;"
                            + "-fx-border-color: #0F766E;"
                            + "-fx-border-width: 1.5;"
                            + "-fx-background-radius: 9;"
                            + "-fx-border-radius: 9;"
            );

            return;
        }


        // =====================================================
        // ADMIN = NAVY
        // =====================================================

        if (currentRoleId() == 1) {

            card.setStyle(
                    "-fx-background-color: #E8EEFC;"
                            + "-fx-border-color: #1E3A8A;"
                            + "-fx-border-width: 1.5;"
                            + "-fx-background-radius: 9;"
                            + "-fx-border-radius: 9;"
            );

            return;
        }


        // FALLBACK
        card.setStyle(
                "-fx-background-color: #EFF6FF;"
                        + "-fx-border-color: #2563EB;"
                        + "-fx-border-width: 1.5;"
                        + "-fx-background-radius: 9;"
                        + "-fx-border-radius: 9;"
            );
        }
    }


    // =========================================================
    // REQUEST MODEL
    // =========================================================

    private record TeamCreationRequest(
            String teamName,
            List<UUID> memberIds
    ) {
    }
}
