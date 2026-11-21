package com.aepm.logic.chat;

import com.aepm.config.AppConfig;
import com.aepm.data.chat.ChatRepository;
import com.aepm.data.supabase.SupabaseClient;
import com.aepm.model.User;
import com.aepm.model.chat.ChatAttachment;
import com.aepm.model.chat.ChatConversation;
import com.aepm.model.chat.ChatMessage;
import com.aepm.model.chat.ChatUser;
import com.aepm.util.SessionManager;

import org.json.JSONObject;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ChatService {

    private static final String CHAT_BUCKET =
            "chat-attachments";

    private static final long MAX_FILE_SIZE =
            10L * 1024L * 1024L;

    private final ChatRepository repository =
            new ChatRepository();

    private final SupabaseClient supabaseClient =
            SupabaseClient.getInstance();


    // =========================================================
    // USERS / CONVERSATIONS
    // =========================================================

    public List<ChatUser> getAvailableChatUsers()
            throws Exception {

        return repository.getAvailableChatUsers();
    }


    public List<ChatConversation> getConversations()
            throws Exception {

        return repository.getConversations();
    }


    public List<ChatUser> getConversationMembers(
            UUID conversationId
    ) throws Exception {

        return repository.getConversationMembers(
                conversationId
        );
    }


    public UUID openPrivateChat(
            UUID otherUserId
    ) throws Exception {

        return repository.getOrCreatePrivateChat(
                otherUserId
        );
    }


    public UUID createTeamChat(
            String teamName,
            List<UUID> memberIds
    ) throws Exception {

        return repository.createTeamChat(
                teamName,
                memberIds
        );
    }


    public void addTeamMember(
            UUID conversationId,
            UUID userId
    ) throws Exception {

        repository.addTeamMember(
                conversationId,
                userId
        );
    }


    public void removeTeamMember(
            UUID conversationId,
            UUID userId
    ) throws Exception {

        repository.removeTeamMember(
                conversationId,
                userId
        );
    }


    public void renameTeamChat(
            UUID conversationId,
            String teamName
    ) throws Exception {

        repository.renameTeamChat(
                conversationId,
                teamName
        );
    }


    public void leaveTeamChat(
            UUID conversationId
    ) throws Exception {

        repository.leaveTeamChat(
                conversationId
        );
    }


    // =========================================================
    // MESSAGES
    // =========================================================

    public List<ChatMessage> getMessages(
            UUID conversationId
    ) throws Exception {

        return repository.getMessages(
                conversationId
        );
    }


    public UUID sendMessage(
            UUID conversationId,
            String message
    ) throws Exception {

        return repository.sendTextMessage(
                conversationId,
                message
        );
    }


    public void markRead(
            UUID conversationId
    ) throws Exception {

        repository.markConversationRead(
                conversationId
        );
    }


    // =========================================================
    // SEND ATTACHMENT
    // =========================================================

    public AttachmentSendResult sendAttachment(
            UUID conversationId,
            File file,
            String caption
    ) throws Exception {

        validateAttachment(
                file
        );

        UUID messageId =
                null;

        String storagePath =
                null;

        try {

            messageId =
                    repository.createFileMessage(
                            conversationId,
                            caption
                    );

            storagePath =
                    buildStoragePath(
                            conversationId,
                            messageId,
                            file.getName()
                    );

            String contentType =
                    detectContentType(
                            file
                    );

            uploadAttachment(
                    file.toPath(),
                    storagePath,
                    contentType
            );

            UUID attachmentId =
                    repository.saveAttachmentMetadata(
                            messageId,
                            file.getName(),
                            storagePath,
                            contentType,
                            file.length()
                    );

            return new AttachmentSendResult(
                    messageId,
                    attachmentId,
                    storagePath,
                    file.getName()
            );

        } catch (Exception exception) {

            /*
             * If storage upload succeeded but metadata failed,
             * attempt best-effort cleanup so orphan files do not
             * remain inside Storage.
             */
            if (
                    storagePath != null
                            && !storagePath.isBlank()
            ) {

                try {
                    deleteStorageObject(
                            storagePath
                    );
                } catch (Exception cleanupError) {

                    System.err.println(
                            "[Chat Attachment] Cleanup failed: "
                                    + cleanupError.getMessage()
                    );
                }
            }

            throw exception;
        }
    }


    // =========================================================
    // OPEN / DOWNLOAD ATTACHMENT
    // =========================================================

    public Path downloadAttachmentToTemp(
            ChatAttachment attachment
    ) throws Exception {

        if (attachment == null) {
            throw new IllegalArgumentException(
                    "Attachment is required."
            );
        }

        if (
                attachment.getFilePath() == null
                        || attachment.getFilePath().isBlank()
        ) {
            throw new IllegalStateException(
                    "Attachment file path is missing."
            );
        }

        String url =
                AppConfig.STORAGE_URL
                        + "/object/authenticated/"
                        + CHAT_BUCKET
                        + "/"
                        + encodePath(
                                attachment.getFilePath()
                        );

        HttpRequest request =
                supabaseClient
                        .createAuthenticatedRequest(
                                url,
                                accessToken()
                        )
                        .GET()
                        .build();

        HttpResponse<byte[]> response =
                sendBytes(
                        request
                );

        if (
                response.statusCode() < 200
                        || response.statusCode() >= 300
        ) {

            throw new IllegalStateException(
                    "Unable to download attachment [HTTP "
                            + response.statusCode()
                            + "]."
            );
        }

        String safeName =
                sanitizeFileName(
                        attachment.getFileName()
                );

        Path tempDir =
                Files.createTempDirectory(
                        "aepm-chat-"
                );

        Path output =
                tempDir.resolve(
                        safeName
                );

        Files.write(
                output,
                response.body(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );

        output.toFile()
                .deleteOnExit();

        tempDir.toFile()
                .deleteOnExit();

        return output;
    }


    public void openAttachment(
            ChatAttachment attachment
    ) throws Exception {

        Path file =
                downloadAttachmentToTemp(
                        attachment
                );

        if (!Desktop.isDesktopSupported()) {

            throw new IllegalStateException(
                    "Opening files is not supported on this computer."
            );
        }

        Desktop desktop =
                Desktop.getDesktop();

        if (!desktop.isSupported(
                Desktop.Action.OPEN
        )) {

            throw new IllegalStateException(
                    "Opening files is not supported on this computer."
            );
        }

        desktop.open(
                file.toFile()
        );
    }


    // =========================================================
    // STORAGE UPLOAD
    // =========================================================

    private void uploadAttachment(
            Path file,
            String storagePath,
            String contentType
    ) throws Exception {

        byte[] bytes =
                Files.readAllBytes(
                        file
                );

        String url =
                AppConfig.STORAGE_URL
                        + "/object/"
                        + CHAT_BUCKET
                        + "/"
                        + encodePath(
                                storagePath
                        );

        HttpRequest request =
                supabaseClient
                        .createAuthenticatedRequest(
                                url,
                                accessToken()
                        )
                        .header(
                                "Content-Type",
                                contentType
                        )
                        .header(
                                "x-upsert",
                                "false"
                        )
                        .POST(
                                HttpRequest.BodyPublishers.ofByteArray(
                                        bytes
                                )
                        )
                        .build();

        HttpResponse<String> response =
                sendText(
                        request
                );

        System.out.println(
                "[Chat Attachment] Storage response: "
                        + response.statusCode()
                        + " "
                        + response.body()
        );

        validateStorageResponse(
                response,
                "Unable to upload chat attachment"
        );
    }


    private void deleteStorageObject(
            String storagePath
    ) throws Exception {

        String url =
                AppConfig.STORAGE_URL
                        + "/object/"
                        + CHAT_BUCKET
                        + "/"
                        + encodePath(
                                storagePath
                        );

        HttpRequest request =
                supabaseClient
                        .createAuthenticatedRequest(
                                url,
                                accessToken()
                        )
                        .DELETE()
                        .build();

        HttpResponse<String> response =
                sendText(
                        request
                );

        if (
                response.statusCode() < 200
                        || response.statusCode() >= 300
        ) {

            throw new IllegalStateException(
                    "Unable to cleanup uploaded file [HTTP "
                            + response.statusCode()
                            + "]: "
                            + response.body()
            );
        }
    }


    // =========================================================
    // VALIDATION
    // =========================================================

    public void validateAttachment(
            File file
    ) throws IOException {

        if (
                file == null
                        || !file.exists()
                        || !file.isFile()
        ) {

            throw new IllegalArgumentException(
                    "Please select a valid file."
            );
        }

        if (file.length() <= 0) {

            throw new IllegalArgumentException(
                    "The selected file is empty."
            );
        }

        if (file.length() > MAX_FILE_SIZE) {

            throw new IllegalArgumentException(
                    "Attachment must not exceed 10 MB."
            );
        }

        String extension =
                getExtension(
                        file.getName()
                );

        if (
                !List.of(
                        "pdf",
                        "doc",
                        "docx",
                        "xls",
                        "xlsx",
                        "png",
                        "jpg",
                        "jpeg",
                        "txt"
                ).contains(
                        extension
                )
        ) {

            throw new IllegalArgumentException(
                    "Unsupported file type. "
                            + "Allowed: PDF, DOC, DOCX, XLS, XLSX, PNG, JPG, JPEG, TXT."
            );
        }

        detectContentType(
                file
        );
    }


    private String detectContentType(
            File file
    ) throws IOException {

        String extension =
                getExtension(
                        file.getName()
                );

        return switch (extension) {

            case "pdf" ->
                    "application/pdf";

            case "doc" ->
                    "application/msword";

            case "docx" ->
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

            case "xls" ->
                    "application/vnd.ms-excel";

            case "xlsx" ->
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

            case "png" ->
                    "image/png";

            case "jpg", "jpeg" ->
                    "image/jpeg";

            case "txt" ->
                    "text/plain";

            default ->
                    throw new IllegalArgumentException(
                            "Unsupported attachment type."
                    );
        };
    }


    // =========================================================
    // STORAGE PATH
    // =========================================================

    private String buildStoragePath(
            UUID conversationId,
            UUID messageId,
            String originalFileName
    ) {

        UUID currentUserId =
                currentUserId();

        String safeFileName =
                sanitizeFileName(
                        originalFileName
                );

        return currentUserId
                + "/"
                + conversationId
                + "/"
                + messageId
                + "/"
                + safeFileName;
    }


    private String sanitizeFileName(
            String fileName
    ) {

        if (
                fileName == null
                        || fileName.isBlank()
        ) {
            return "attachment";
        }

        String cleaned =
                fileName
                        .replaceAll(
                                "[\\\\/:*?\"<>|]",
                                "_"
                        )
                        .replaceAll(
                                "\\s+",
                                " "
                        )
                        .trim();

        if (cleaned.isBlank()) {
            return "attachment";
        }

        return cleaned;
    }


    private String encodePath(
            String path
    ) {

        if (
                path == null
                        || path.isBlank()
        ) {
            return "";
        }

        String[] parts =
                path.split(
                        "/"
                );

        StringBuilder builder =
                new StringBuilder();

        for (String part : parts) {

            if (builder.length() > 0) {
                builder.append('/');
            }

            builder.append(
                    java.net.URLEncoder.encode(
                            part,
                            java.nio.charset.StandardCharsets.UTF_8
                    )
                            .replace(
                                    "+",
                                    "%20"
                            )
            );
        }

        return builder.toString();
    }


    private String getExtension(
            String fileName
    ) {

        if (fileName == null) {
            return "";
        }

        int index =
                fileName.lastIndexOf(
                        '.'
                );

        if (
                index < 0
                        || index == fileName.length() - 1
        ) {
            return "";
        }

        return fileName
                .substring(
                        index + 1
                )
                .toLowerCase(
                        Locale.ROOT
                );
    }


    // =========================================================
    // SESSION
    // =========================================================

    private String accessToken() {

        String token =
                SessionManager.getAccessToken();

        if (
                token == null
                        || token.isBlank()
        ) {

            throw new IllegalStateException(
                    "Please log in again."
            );
        }

        return token;
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
    // HTTP HELPERS
    // =========================================================

    private HttpResponse<String> sendText(
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


    private HttpResponse<byte[]> sendBytes(
            HttpRequest request
    ) throws Exception {

        try {

            return supabaseClient
                    .getHttpClient()
                    .send(
                            request,
                            HttpResponse.BodyHandlers.ofByteArray()
                    );

        } catch (InterruptedException exception) {

            Thread.currentThread()
                    .interrupt();

            throw exception;
        }
    }


    private void validateStorageResponse(
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
    // RESULT MODEL
    // =========================================================

    public static class AttachmentSendResult {

        private final UUID messageId;
        private final UUID attachmentId;
        private final String storagePath;
        private final String fileName;

        public AttachmentSendResult(
                UUID messageId,
                UUID attachmentId,
                String storagePath,
                String fileName
        ) {
            this.messageId = messageId;
            this.attachmentId = attachmentId;
            this.storagePath = storagePath;
            this.fileName = fileName;
        }

        public UUID getMessageId() {
            return messageId;
        }

        public UUID getAttachmentId() {
            return attachmentId;
        }

        public String getStoragePath() {
            return storagePath;
        }

        public String getFileName() {
            return fileName;
        }
    }
}
