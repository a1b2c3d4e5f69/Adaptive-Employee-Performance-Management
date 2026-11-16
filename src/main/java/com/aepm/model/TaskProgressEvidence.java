package com.aepm.model;

import java.time.OffsetDateTime;
import java.util.UUID;


public class TaskProgressEvidence {


    private UUID evidenceId;

    private UUID submissionId;

    private String fileName;

    private String filePath;

    private String fileType;

    private long fileSize;

    private UUID uploadedBy;

    private OffsetDateTime uploadedAt;


    public UUID getEvidenceId() {
        return evidenceId;
    }


    public void setEvidenceId(
            UUID evidenceId
    ) {

        this.evidenceId =
                evidenceId;
    }


    public UUID getSubmissionId() {
        return submissionId;
    }


    public void setSubmissionId(
            UUID submissionId
    ) {

        this.submissionId =
                submissionId;
    }


    public String getFileName() {
        return fileName;
    }


    public void setFileName(
            String fileName
    ) {

        this.fileName =
                fileName;
    }


    public String getFilePath() {
        return filePath;
    }


    public void setFilePath(
            String filePath
    ) {

        this.filePath =
                filePath;
    }


    public String getFileType() {
        return fileType;
    }


    public void setFileType(
            String fileType
    ) {

        this.fileType =
                fileType;
    }


    public long getFileSize() {
        return fileSize;
    }


    public void setFileSize(
            long fileSize
    ) {

        this.fileSize =
                fileSize;
    }


    public UUID getUploadedBy() {
        return uploadedBy;
    }


    public void setUploadedBy(
            UUID uploadedBy
    ) {

        this.uploadedBy =
                uploadedBy;
    }


    public OffsetDateTime getUploadedAt() {
        return uploadedAt;
    }


    public void setUploadedAt(
            OffsetDateTime uploadedAt
    ) {

        this.uploadedAt =
                uploadedAt;
    }
}