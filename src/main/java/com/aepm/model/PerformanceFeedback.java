package com.aepm.model;

import java.time.OffsetDateTime;

public class PerformanceFeedback {

    private Long feedbackId;

    private Long resultId;

    private String employeeId;

    private String managerId;

    private String feedbackText;

    private OffsetDateTime createdAt;


    public PerformanceFeedback() {
    }


    public Long getFeedbackId() {
        return feedbackId;
    }

    public void setFeedbackId(
            Long feedbackId
    ) {
        this.feedbackId = feedbackId;
    }


    public Long getResultId() {
        return resultId;
    }

    public void setResultId(
            Long resultId
    ) {
        this.resultId = resultId;
    }


    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(
            String employeeId
    ) {
        this.employeeId = employeeId;
    }


    public String getManagerId() {
        return managerId;
    }

    public void setManagerId(
            String managerId
    ) {
        this.managerId = managerId;
    }


    public String getFeedbackText() {
        return feedbackText;
    }

    public void setFeedbackText(
            String feedbackText
    ) {
        this.feedbackText = feedbackText;
    }


    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(
            OffsetDateTime createdAt
    ) {
        this.createdAt = createdAt;
    }
}