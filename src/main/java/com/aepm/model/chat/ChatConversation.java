package com.aepm.model.chat;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ChatConversation {

    private final UUID conversationId;
    private final String conversationType;
    private final String conversationName;
    private final UUID createdBy;
    private final boolean active;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    private final List<ChatUser> members = new ArrayList<>();

    private String displayName;
    private String displayImage;
    private String lastMessage;
    private OffsetDateTime lastMessageAt;
    private int unreadCount;

    public ChatConversation(
            UUID conversationId,
            String conversationType,
            String conversationName,
            UUID createdBy,
            boolean active,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        this.conversationId = conversationId;
        this.conversationType = conversationType;
        this.conversationName = conversationName;
        this.createdBy = createdBy;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public String getConversationType() {
        return conversationType;
    }

    public String getConversationName() {
        return conversationName;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public boolean isActive() {
        return active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isPrivateChat() {
        return "PRIVATE".equalsIgnoreCase(conversationType);
    }

    public boolean isTeamChat() {
        return "TEAM".equalsIgnoreCase(conversationType);
    }

    public List<ChatUser> getMembers() {
        return Collections.unmodifiableList(members);
    }

    public void setMembers(List<ChatUser> values) {
        members.clear();
        if (values != null) {
            members.addAll(values);
        }
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayImage() {
        return displayImage;
    }

    public void setDisplayImage(String displayImage) {
        this.displayImage = displayImage;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public OffsetDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(OffsetDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = Math.max(0, unreadCount);
    }
}
