package com.aepm.model.chat;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ChatAttachment {

    private final UUID attachmentId;
    private final UUID messageId;
    private final String fileName;
    private final String filePath;
    private final String fileType;
    private final long fileSize;
    private final UUID uploadedBy;
    private final OffsetDateTime uploadedAt;

    public ChatAttachment(
            UUID attachmentId,
            UUID messageId,
            String fileName,
            String filePath,
            String fileType,
            long fileSize,
            UUID uploadedBy,
            OffsetDateTime uploadedAt
    ) {
        this.attachmentId = attachmentId;
        this.messageId = messageId;
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.uploadedBy = uploadedBy;
        this.uploadedAt = uploadedAt;
    }

    public UUID getAttachmentId() {
        return attachmentId;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFileType() {
        return fileType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public UUID getUploadedBy() {
        return uploadedBy;
    }

    public OffsetDateTime getUploadedAt() {
        return uploadedAt;
    }
}
