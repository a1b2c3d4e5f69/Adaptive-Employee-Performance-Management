package com.aepm.model.chat;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ChatMessage {

    private final UUID messageId;
    private final UUID conversationId;
    private final UUID senderId;
    private final String senderName;
    private final String senderProfileImage;
    private final String messageText;
    private final String messageType;
    private final UUID replyToMessageId;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime editedAt;
    private final OffsetDateTime deletedAt;

    private final List<ChatAttachment> attachments = new ArrayList<>();

    public ChatMessage(
            UUID messageId,
            UUID conversationId,
            UUID senderId,
            String senderName,
            String senderProfileImage,
            String messageText,
            String messageType,
            UUID replyToMessageId,
            OffsetDateTime createdAt,
            OffsetDateTime editedAt,
            OffsetDateTime deletedAt
    ) {
        this.messageId = messageId;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderProfileImage = senderProfileImage;
        this.messageText = messageText;
        this.messageType = messageType;
        this.replyToMessageId = replyToMessageId;
        this.createdAt = createdAt;
        this.editedAt = editedAt;
        this.deletedAt = deletedAt;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public UUID getSenderId() {
        return senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getSenderProfileImage() {
        return senderProfileImage;
    }

    public String getMessageText() {
        return messageText;
    }

    public String getMessageType() {
        return messageType;
    }

    public UUID getReplyToMessageId() {
        return replyToMessageId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getEditedAt() {
        return editedAt;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isFileMessage() {
        return "FILE".equalsIgnoreCase(messageType);
    }

    public List<ChatAttachment> getAttachments() {
        return Collections.unmodifiableList(attachments);
    }

    public void setAttachments(List<ChatAttachment> values) {
        attachments.clear();
        if (values != null) {
            attachments.addAll(values);
        }
    }
}
