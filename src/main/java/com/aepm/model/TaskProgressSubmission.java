package com.aepm.model;

import java.time.OffsetDateTime;
import java.util.UUID;


public class TaskProgressSubmission {


    private UUID submissionId;

    private UUID taskId;

    private UUID employeeId;

    private double proposedProgress;

    private String progressNote;

    private String status;

    private OffsetDateTime verificationDeadline;

    private String verificationMethod;

    private Double systemConfidence;

    private String systemReason;

    private UUID reviewedBy;

    private String reviewComment;

    private OffsetDateTime submittedAt;

    private OffsetDateTime reviewedAt;


    public UUID getSubmissionId() {
        return submissionId;
    }


    public void setSubmissionId(
            UUID submissionId
    ) {

        this.submissionId =
                submissionId;
    }


    public UUID getTaskId() {
        return taskId;
    }


    public void setTaskId(
            UUID taskId
    ) {

        this.taskId =
                taskId;
    }


    public UUID getEmployeeId() {
        return employeeId;
    }


    public void setEmployeeId(
            UUID employeeId
    ) {

        this.employeeId =
                employeeId;
    }


    public double getProposedProgress() {
        return proposedProgress;
    }


    public void setProposedProgress(
            double proposedProgress
    ) {

        this.proposedProgress =
                proposedProgress;
    }


    public String getProgressNote() {
        return progressNote;
    }


    public void setProgressNote(
            String progressNote
    ) {

        this.progressNote =
                progressNote;
    }


    public String getStatus() {
        return status;
    }


    public void setStatus(
            String status
    ) {

        this.status =
                status;
    }


    public OffsetDateTime getVerificationDeadline() {
        return verificationDeadline;
    }


    public void setVerificationDeadline(
            OffsetDateTime verificationDeadline
    ) {

        this.verificationDeadline =
                verificationDeadline;
    }


    public String getVerificationMethod() {
        return verificationMethod;
    }


    public void setVerificationMethod(
            String verificationMethod
    ) {

        this.verificationMethod =
                verificationMethod;
    }


    public Double getSystemConfidence() {
        return systemConfidence;
    }


    public void setSystemConfidence(
            Double systemConfidence
    ) {

        this.systemConfidence =
                systemConfidence;
    }


    public String getSystemReason() {
        return systemReason;
    }


    public void setSystemReason(
            String systemReason
    ) {

        this.systemReason =
                systemReason;
    }


    public UUID getReviewedBy() {
        return reviewedBy;
    }


    public void setReviewedBy(
            UUID reviewedBy
    ) {

        this.reviewedBy =
                reviewedBy;
    }


    public String getReviewComment() {
        return reviewComment;
    }


    public void setReviewComment(
            String reviewComment
    ) {

        this.reviewComment =
                reviewComment;
    }


    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }


    public void setSubmittedAt(
            OffsetDateTime submittedAt
    ) {

        this.submittedAt =
                submittedAt;
    }


    public OffsetDateTime getReviewedAt() {
        return reviewedAt;
    }


    public void setReviewedAt(
            OffsetDateTime reviewedAt
    ) {

        this.reviewedAt =
                reviewedAt;
    }
}