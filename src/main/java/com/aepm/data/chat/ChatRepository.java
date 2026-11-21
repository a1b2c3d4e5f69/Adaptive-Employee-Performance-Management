package com.aepm.data.chat;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;
import com.aepm.model.User;
import com.aepm.model.chat.ChatAttachment;
import com.aepm.model.chat.ChatConversation;
import com.aepm.model.chat.ChatMessage;
import com.aepm.model.chat.ChatUser;
import com.aepm.util.SessionManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ChatRepository {

    private final SupabaseClient supabaseClient =
            SupabaseClient.getInstance();


    // =========================================================
    // AVAILABLE CHAT USERS
    // =========================================================

    public List<ChatUser> getAvailableChatUsers()
            throws Exception {

        String accessToken =
                accessToken();

        String url =
                AppConfig.REST_URL
                        + "/rpc/aepm_get_available_chat_users";

        HttpRequest request =
                supabaseClient
                        .createAuthenticatedRequest(
                                url,
                                accessToken
                        )
                        .POST(
                                HttpRequest.BodyPublishers.noBody()
                        )
                        .build();

        HttpResponse<String> response =
                send(
                        request
                );

        validate(
                response,
                "Unable to load available chat users"
        );

        JSONArray rows =
                new JSONArray(
                        response.body()
                );

        List<ChatUser> users =
                new ArrayList<>();

        for (int i = 0; i < rows.length(); i++) {

            JSONObject row =
                    rows.getJSONObject(i);

            users.add(
                    new ChatUser(
                            uuid(
                                    row,
                                    "user_id"
                            ),
                            value(
                                    row,
                                    "full_name",
                                    "User"
                            ),
                            value(
                                    row,
                                    "employee_id",
                                    ""
                            ),
                            row.optInt(
                                    "role_id",
                                    0
                            ),
                            value(
                                    row,
                                    "department",
                                    ""
                            ),
                            value(
                                    row,
                                    "profile_image",
                                    ""
                            )
                    )
            );
        }

        return users;
    }


    // =========================================================
    // PRIVATE CHAT
    // =========================================================

    public UUID getOrCreatePrivateChat(
            UUID otherUserId
    ) throws Exception {

        if (otherUserId == null) {
            throw new IllegalArgumentException(
                    "Chat recipient is required."
            );
        }

        JSONObject body =
                new JSONObject();

        body.put(
                "p_other_user_id",
                otherUserId.toString()
        );

        return callUuidRpc(
                "aepm_get_or_create_private_chat",
                body,
                "Unable to open private chat"
        );
    }


    // =========================================================
    // TEAM CHAT
    // =========================================================

    public UUID createTeamChat(
            String teamName,
            List<UUID> memberIds
    ) throws Exception {

        if (
                teamName == null
                        || teamName.trim().length() < 3
        ) {
            throw new IllegalArgumentException(
                    "Team name must contain at least 3 characters."
            );
        }

        JSONArray members =
                new JSONArray();

        if (memberIds != null) {

            for (UUID memberId : memberIds) {

                if (memberId != null) {
                    members.put(
                            memberId.toString()
                    );
                }
            }
        }

        JSONObject body =
                new JSONObject();

        body.put(
                "p_team_name",
                teamName.trim()
        );

        body.put(
                "p_member_ids",
                members
        );

        return callUuidRpc(
                "aepm_create_team_chat",
                body,
                "Unable to create team chat"
        );
    }


    public void addTeamMember(
            UUID conversationId,
            UUID userId
    ) throws Exception {

        JSONObject body =
                new JSONObject();

        body.put(
                "p_conversation_id",
                required(
                        conversationId,
                        "Conversation ID"
                ).toString()
        );

        body.put(
                "p_user_id",
                required(
                        userId,
                        "User ID"
                ).toString()
        );

        callVoidRpc(
                "aepm_add_team_member",
                body,
                "Unable to add team member"
        );
    }


    public void removeTeamMember(
            UUID conversationId,
            UUID userId
    ) throws Exception {

        JSONObject body =
                new JSONObject();

        body.put(
                "p_conversation_id",
                required(
                        conversationId,
                        "Conversation ID"
                ).toString()
        );

        body.put(
                "p_user_id",
                required(
                        userId,
                        "User ID"
                ).toString()
        );

        callVoidRpc(
                "aepm_remove_team_member",
                body,
                "Unable to remove team member"
        );
    }


    public void renameTeamChat(
            UUID conversationId,
            String teamName
    ) throws Exception {

        JSONObject body =
                new JSONObject();

        body.put(
                "p_conversation_id",
                required(
                        conversationId,
                        "Conversation ID"
                ).toString()
        );

        body.put(
                "p_team_name",
                teamName == null
                        ? ""
                        : teamName.trim()
        );

        callVoidRpc(
                "aepm_rename_team_chat",
                body,
                "Unable to rename team chat"
        );
    }


    public void leaveTeamChat(
            UUID conversationId
    ) throws Exception {

        JSONObject body =
                new JSONObject();

        body.put(
                "p_conversation_id",
                required(
                        conversationId,
                        "Conversation ID"
                ).toString()
        );

        callVoidRpc(
                "aepm_leave_team_chat",
                body,
                "Unable to leave team chat"
        );
    }


    // =========================================================
    // CONVERSATIONS
    // =========================================================

    public List<ChatConversation> getConversations()
            throws Exception {

        String accessToken =
                accessToken();

        UUID currentUserId =
                currentUserId();

        String url =
                AppConfig.REST_URL
                        + "/chat_conversation_tbl"
                        + "?select=conversation_id,conversation_type,"
                        + "conversation_name,created_by,is_active,"
                        + "created_at,updated_at"
                        + "&is_active=eq.true"
                        + "&order=updated_at.desc";

        JSONArray rows =
                getArray(
                        url,
                        accessToken,
                        "Unable to load conversations"
                );

        List<ChatConversation> conversations =
                new ArrayList<>();

        for (int i = 0; i < rows.length(); i++) {

            JSONObject row =
                    rows.getJSONObject(i);

            ChatConversation conversation =
                    new ChatConversation(
                            uuid(
                                    row,
                                    "conversation_id"
                            ),
                            value(
                                    row,
                                    "conversation_type",
                                    ""
                            ),
                            value(
                                    row,
                                    "conversation_name",
                                    ""
                            ),
                            uuidNullable(
                                    row,
                                    "created_by"
                            ),
                            row.optBoolean(
                                    "is_active",
                                    true
                            ),
                            date(
                                    row,
                                    "created_at"
                            ),
                            date(
                                    row,
                                    "updated_at"
                            )
                    );

            List<ChatUser> members =
                    getConversationMembers(
                            conversation.getConversationId(),
                            accessToken
                    );

            conversation.setMembers(
                    members
            );

            if (conversation.isPrivateChat()) {

                ChatUser other =
                        members.stream()
                                .filter(
                                        member ->
                                                !currentUserId.equals(
                                                        member.getUserId()
                                                )
                                )
                                .findFirst()
                                .orElse(
                                        null
                                );

                if (other != null) {

                    conversation.setDisplayName(
                            other.getFullName()
                    );

                    conversation.setDisplayImage(
                            other.getProfileImage()
                    );

                } else {

                    conversation.setDisplayName(
                            "Private Chat"
                    );
                }

            } else {

                conversation.setDisplayName(
                        conversation.getConversationName()
                );
            }

            loadConversationPreview(
                    conversation,
                    currentUserId,
                    accessToken
            );

            conversations.add(
                    conversation
            );
        }

        return conversations;
    }


    public List<ChatUser> getConversationMembers(
            UUID conversationId
    ) throws Exception {

        return getConversationMembers(
                conversationId,
                accessToken()
        );
    }


    private List<ChatUser> getConversationMembers(
            UUID conversationId,
            String accessToken
    ) throws Exception {

        String memberUrl =
                AppConfig.REST_URL
                        + "/chat_member_tbl"
                        + "?select=user_id"
                        + "&conversation_id=eq."
                        + conversationId
                        + "&is_active=eq.true";

        JSONArray memberRows =
                getArray(
                        memberUrl,
                        accessToken,
                        "Unable to load conversation members"
                );

        if (memberRows.isEmpty()) {
            return List.of();
        }

        Set<String> userIds =
                new LinkedHashSet<>();

        for (int i = 0; i < memberRows.length(); i++) {

            userIds.add(
                    memberRows
                            .getJSONObject(i)
                            .getString(
                                    "user_id"
                            )
            );
        }

        String ids =
                String.join(
                        ",",
                        userIds
                );

        String userUrl =
                AppConfig.REST_URL
                        + "/user_tbl"
                        + "?select=user_id,full_name,employee_id,"
                        + "role_id,department,profile_image"
                        + "&user_id=in.("
                        + ids
                        + ")";

        JSONArray userRows =
                getArray(
                        userUrl,
                        accessToken,
                        "Unable to load conversation user details"
                );

        Map<String, ChatUser> users =
                new HashMap<>();

        for (int i = 0; i < userRows.length(); i++) {

            JSONObject row =
                    userRows.getJSONObject(i);

            ChatUser user =
                    new ChatUser(
                            uuid(
                                    row,
                                    "user_id"
                            ),
                            value(
                                    row,
                                    "full_name",
                                    "User"
                            ),
                            value(
                                    row,
                                    "employee_id",
                                    ""
                            ),
                            row.optInt(
                                    "role_id",
                                    0
                            ),
                            value(
                                    row,
                                    "department",
                                    ""
                            ),
                            value(
                                    row,
                                    "profile_image",
                                    ""
                            )
                    );

            users.put(
                    user.getUserId().toString(),
                    user
            );
        }

        List<ChatUser> ordered =
                new ArrayList<>();

        for (String userId : userIds) {

            ChatUser user =
                    users.get(
                            userId
                    );

            if (user != null) {
                ordered.add(
                        user
                );
            }
        }

        return ordered;
    }


    // =========================================================
    // MESSAGES
    // =========================================================

    public List<ChatMessage> getMessages(
            UUID conversationId
    ) throws Exception {

        String accessToken =
                accessToken();

        UUID id =
                required(
                        conversationId,
                        "Conversation ID"
                );

        String url =
                AppConfig.REST_URL
                        + "/chat_message_tbl"
                        + "?select=message_id,conversation_id,sender_id,"
                        + "message_text,message_type,reply_to_message_id,"
                        + "created_at,edited_at,deleted_at"
                        + "&conversation_id=eq."
                        + id
                        + "&order=created_at.asc"
                        + "&limit=500";

        JSONArray rows =
                getArray(
                        url,
                        accessToken,
                        "Unable to load chat messages"
                );

        Set<String> senderIds =
                new LinkedHashSet<>();

        for (int i = 0; i < rows.length(); i++) {

            String senderId =
                    rows.getJSONObject(i)
                            .optString(
                                    "sender_id",
                                    ""
                            );

            if (!senderId.isBlank()) {
                senderIds.add(
                        senderId
                );
            }
        }

        Map<String, ChatUser> senders =
                getUsersByIds(
                        senderIds,
                        accessToken
                );

        List<ChatMessage> messages =
                new ArrayList<>();

        for (int i = 0; i < rows.length(); i++) {

            JSONObject row =
                    rows.getJSONObject(i);

            UUID senderId =
                    uuid(
                            row,
                            "sender_id"
                    );

            ChatUser sender =
                    senders.get(
                            senderId.toString()
                    );

            ChatMessage message =
                    new ChatMessage(
                            uuid(
                                    row,
                                    "message_id"
                            ),
                            uuid(
                                    row,
                                    "conversation_id"
                            ),
                            senderId,
                            sender == null
                                    ? "User"
                                    : sender.getFullName(),
                            sender == null
                                    ? ""
                                    : sender.getProfileImage(),
                            value(
                                    row,
                                    "message_text",
                                    ""
                            ),
                            value(
                                    row,
                                    "message_type",
                                    "TEXT"
                            ),
                            uuidNullable(
                                    row,
                                    "reply_to_message_id"
                            ),
                            date(
                                    row,
                                    "created_at"
                            ),
                            date(
                                    row,
                                    "edited_at"
                            ),
                            date(
                                    row,
                                    "deleted_at"
                            )
                    );

            message.setAttachments(
                    getMessageAttachments(
                            message.getMessageId(),
                            accessToken
                    )
            );

            messages.add(
                    message
            );
        }

        return messages;
    }


    public UUID sendTextMessage(
            UUID conversationId,
            String message
    ) throws Exception {

        if (
                message == null
                        || message.trim().isEmpty()
        ) {
            throw new IllegalArgumentException(
                    "Message cannot be empty."
            );
        }

        JSONObject body =
                new JSONObject();

        body.put(
                "p_conversation_id",
                required(
                        conversationId,
                        "Conversation ID"
                ).toString()
        );

        body.put(
                "p_message_text",
                message.trim()
        );

        body.put(
                "p_message_type",
                "TEXT"
        );

        return callUuidRpc(
                "aepm_send_chat_message",
                body,
                "Unable to send message"
        );
    }


    public UUID createFileMessage(
            UUID conversationId,
            String caption
    ) throws Exception {

        JSONObject body =
                new JSONObject();

        body.put(
                "p_conversation_id",
                required(
                        conversationId,
                        "Conversation ID"
                ).toString()
        );

        if (
                caption == null
                        || caption.isBlank()
        ) {
            body.put(
                    "p_message_text",
                    JSONObject.NULL
            );
        } else {
            body.put(
                    "p_message_text",
                    caption.trim()
            );
        }

        body.put(
                "p_message_type",
                "FILE"
        );

        return callUuidRpc(
                "aepm_send_chat_message",
                body,
                "Unable to create file message"
        );
    }


    public void markConversationRead(
            UUID conversationId
    ) throws Exception {

        JSONObject body =
                new JSONObject();

        body.put(
                "p_conversation_id",
                required(
                        conversationId,
                        "Conversation ID"
                ).toString()
        );

        callVoidRpc(
                "aepm_mark_chat_read",
                body,
                "Unable to mark conversation as read"
        );
    }


    // =========================================================
    // ATTACHMENTS
    // =========================================================

    public UUID saveAttachmentMetadata(
            UUID messageId,
            String fileName,
            String filePath,
            String fileType,
            long fileSize
    ) throws Exception {

        JSONObject body =
                new JSONObject();

        body.put(
                "p_message_id",
                required(
                        messageId,
                        "Message ID"
                ).toString()
        );

        body.put(
                "p_file_name",
                fileName
        );

        body.put(
                "p_file_path",
                filePath
        );

        body.put(
                "p_file_type",
                fileType == null
                        ? JSONObject.NULL
                        : fileType
        );

        body.put(
                "p_file_size",
                fileSize
        );

        return callUuidRpc(
                "aepm_save_chat_attachment",
                body,
                "Unable to save chat attachment"
        );
    }


    public List<ChatAttachment> getMessageAttachments(
            UUID messageId
    ) throws Exception {

        return getMessageAttachments(
                messageId,
                accessToken()
        );
    }


    private List<ChatAttachment> getMessageAttachments(
            UUID messageId,
            String accessToken
    ) throws Exception {

        String url =
                AppConfig.REST_URL
                        + "/chat_attachment_tbl"
                        + "?select=attachment_id,message_id,file_name,"
                        + "file_path,file_type,file_size,uploaded_by,uploaded_at"
                        + "&message_id=eq."
                        + messageId
                        + "&order=uploaded_at.asc";

        JSONArray rows =
                getArray(
                        url,
                        accessToken,
                        "Unable to load message attachments"
                );

        List<ChatAttachment> attachments =
                new ArrayList<>();

        for (int i = 0; i < rows.length(); i++) {

            JSONObject row =
                    rows.getJSONObject(i);

            attachments.add(
                    new ChatAttachment(
                            uuid(
                                    row,
                                    "attachment_id"
                            ),
                            uuid(
                                    row,
                                    "message_id"
                            ),
                            value(
                                    row,
                                    "file_name",
                                    "Attachment"
                            ),
                            value(
                                    row,
                                    "file_path",
                                    ""
                            ),
                            value(
                                    row,
                                    "file_type",
                                    ""
                            ),
                            row.optLong(
                                    "file_size",
                                    0
                            ),
                            uuidNullable(
                                    row,
                                    "uploaded_by"
                            ),
                            date(
                                    row,
                                    "uploaded_at"
                            )
                    )
            );
        }

        return attachments;
    }


    // =========================================================
    // PREVIEW
    // =========================================================

    private void loadConversationPreview(
            ChatConversation conversation,
            UUID currentUserId,
            String accessToken
    ) throws Exception {

        UUID conversationId =
                conversation.getConversationId();

        String lastUrl =
                AppConfig.REST_URL
                        + "/chat_message_tbl"
                        + "?select=message_text,message_type,created_at"
                        + "&conversation_id=eq."
                        + conversationId
                        + "&order=created_at.desc"
                        + "&limit=1";

        JSONArray lastRows =
                getArray(
                        lastUrl,
                        accessToken,
                        "Unable to load conversation preview"
                );

        if (!lastRows.isEmpty()) {

            JSONObject row =
                    lastRows.getJSONObject(0);

            String type =
                    value(
                            row,
                            "message_type",
                            "TEXT"
                    );

            String text =
                    value(
                            row,
                            "message_text",
                            ""
                    );

            if (
                    "FILE".equalsIgnoreCase(
                            type
                    )
                            && text.isBlank()
            ) {
                text = "Attachment";
            }

            conversation.setLastMessage(
                    text
            );

            conversation.setLastMessageAt(
                    date(
                            row,
                            "created_at"
                    )
            );
        }

        String membershipUrl =
                AppConfig.REST_URL
                        + "/chat_member_tbl"
                        + "?select=last_read_at"
                        + "&conversation_id=eq."
                        + conversationId
                        + "&user_id=eq."
                        + currentUserId
                        + "&limit=1";

        JSONArray membershipRows =
                getArray(
                        membershipUrl,
                        accessToken,
                        "Unable to load conversation read status"
                );

        OffsetDateTime lastReadAt =
                null;

        if (!membershipRows.isEmpty()) {

            lastReadAt =
                    date(
                            membershipRows.getJSONObject(0),
                            "last_read_at"
                    );
        }

        String unreadUrl =
                AppConfig.REST_URL
                        + "/chat_message_tbl"
                        + "?select=message_id"
                        + "&conversation_id=eq."
                        + conversationId
                        + "&sender_id=neq."
                        + currentUserId;

        if (lastReadAt != null) {

            unreadUrl +=
                    "&created_at=gt."
                            + encode(
                                    lastReadAt.toString()
                            );
        }

        JSONArray unreadRows =
                getArray(
                        unreadUrl,
                        accessToken,
                        "Unable to calculate unread messages"
                );

        conversation.setUnreadCount(
                unreadRows.length()
        );
    }


    // =========================================================
    // USER LOOKUP
    // =========================================================

    private Map<String, ChatUser> getUsersByIds(
            Set<String> userIds,
            String accessToken
    ) throws Exception {

        Map<String, ChatUser> users =
                new HashMap<>();

        if (
                userIds == null
                        || userIds.isEmpty()
        ) {
            return users;
        }

        String ids =
                String.join(
                        ",",
                        userIds
                );

        String url =
                AppConfig.REST_URL
                        + "/user_tbl"
                        + "?select=user_id,full_name,employee_id,"
                        + "role_id,department,profile_image"
                        + "&user_id=in.("
                        + ids
                        + ")";

        JSONArray rows =
                getArray(
                        url,
                        accessToken,
                        "Unable to load chat users"
                );

        for (int i = 0; i < rows.length(); i++) {

            JSONObject row =
                    rows.getJSONObject(i);

            ChatUser user =
                    new ChatUser(
                            uuid(
                                    row,
                                    "user_id"
                            ),
                            value(
                                    row,
                                    "full_name",
                                    "User"
                            ),
                            value(
                                    row,
                                    "employee_id",
                                    ""
                            ),
                            row.optInt(
                                    "role_id",
                                    0
                            ),
                            value(
                                    row,
                                    "department",
                                    ""
                            ),
                            value(
                                    row,
                                    "profile_image",
                                    ""
                            )
                    );

            users.put(
                    user.getUserId().toString(),
                    user
            );
        }

        return users;
    }


    // =========================================================
    // HTTP / RPC HELPERS
    // =========================================================

    private UUID callUuidRpc(
            String rpcName,
            JSONObject body,
            String errorMessage
    ) throws Exception {

        String response =
                callRpc(
                        rpcName,
                        body,
                        errorMessage
                );

        String value =
                response == null
                        ? ""
                        : response.trim();

        if (
                value.startsWith("\"")
                        && value.endsWith("\"")
        ) {
            value =
                    new JSONArray(
                            "[" + value + "]"
                    )
                            .getString(
                                    0
                            );
        }

        if (value.isBlank()) {
            throw new IllegalStateException(
                    errorMessage
                            + ": RPC returned no ID."
            );
        }

        return UUID.fromString(
                value
        );
    }


    private void callVoidRpc(
            String rpcName,
            JSONObject body,
            String errorMessage
    ) throws Exception {

        callRpc(
                rpcName,
                body,
                errorMessage
        );
    }


    private String callRpc(
            String rpcName,
            JSONObject body,
            String errorMessage
    ) throws Exception {

        String url =
                AppConfig.REST_URL
                        + "/rpc/"
                        + rpcName;

        HttpRequest request =
                supabaseClient
                        .createAuthenticatedRequest(
                                url,
                                accessToken()
                        )
                        .header(
                                "Content-Type",
                                "application/json"
                        )
                        .POST(
                                HttpRequest.BodyPublishers.ofString(
                                        body == null
                                                ? "{}"
                                                : body.toString()
                                )
                        )
                        .build();

        HttpResponse<String> response =
                send(
                        request
                );

        validate(
                response,
                errorMessage
        );

        return response.body();
    }


    private JSONArray getArray(
            String url,
            String accessToken,
            String errorMessage
    ) throws Exception {

        HttpRequest request =
                supabaseClient
                        .createAuthenticatedRequest(
                                url,
                                accessToken
                        )
                        .GET()
                        .build();

        HttpResponse<String> response =
                send(
                        request
                );

        validate(
                response,
                errorMessage
        );

        return new JSONArray(
                response.body()
        );
    }


    private HttpResponse<String> send(
            HttpRequest request
    ) throws Exception {

        try {

            return supabaseClient
                    .getHttpClient()
                    .send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

        } catch (InterruptedException exception) {

            Thread.currentThread()
                    .interrupt();

            throw exception;
        }
    }


    private void validate(
            HttpResponse<String> response,
            String message
    ) {

        if (
                response.statusCode() < 200
                        || response.statusCode() >= 300
        ) {

            throw new IllegalStateException(
                    message
                            + " [HTTP "
                            + response.statusCode()
                            + "]: "
                            + response.body()
            );
        }
    }


    // =========================================================
    // SESSION HELPERS
    // =========================================================

    private String accessToken() {

        String accessToken =
                SessionManager.getAccessToken();

        if (
                accessToken == null
                        || accessToken.isBlank()
        ) {
            throw new IllegalStateException(
                    "Please log in again."
            );
        }

        return accessToken;
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


    // =========================================================
    // GENERAL HELPERS
    // =========================================================

    private static <T> T required(
            T value,
            String name
    ) {

        if (value == null) {
            throw new IllegalArgumentException(
                    name + " is required."
            );
        }

        return value;
    }


    private static UUID uuid(
            JSONObject row,
            String key
    ) {

        UUID value =
                uuidNullable(
                        row,
                        key
                );

        if (value == null) {
            throw new IllegalStateException(
                    "Missing UUID field: "
                            + key
            );
        }

        return value;
    }


    private static UUID uuidNullable(
            JSONObject row,
            String key
    ) {

        if (
                row == null
                        || !row.has(key)
                        || row.isNull(key)
        ) {
            return null;
        }

        String value =
                row.optString(
                        key,
                        ""
                );

        if (value.isBlank()) {
            return null;
        }

        return UUID.fromString(
                value
        );
    }


    private static String value(
            JSONObject row,
            String key,
            String fallback
    ) {

        if (
                row == null
                        || !row.has(key)
                        || row.isNull(key)
        ) {
            return fallback;
        }

        String value =
                row.optString(
                        key,
                        fallback
                );

        return value == null
                ? fallback
                : value;
    }


    private static OffsetDateTime date(
            JSONObject row,
            String key
    ) {

        if (
                row == null
                        || !row.has(key)
                        || row.isNull(key)
        ) {
            return null;
        }

        String value =
                row.optString(
                        key,
                        ""
                );

        if (value.isBlank()) {
            return null;
        }

        try {
            return OffsetDateTime.parse(
                    value
            );
        } catch (Exception ignored) {
            return null;
        }
    }


    private static String encode(
            String value
    ) {

        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8
        );
    }
}
