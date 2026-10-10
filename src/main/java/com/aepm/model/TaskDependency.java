package com.aepm.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public class TaskDependency {

    private Long dependencyId;

    private UUID prerequisiteTaskId;

    private UUID nextTaskId;

    private String ruleStatus;

    private UUID createdBy;

    private OffsetDateTime createdAt;


    public TaskDependency() {
    }


    public Long getDependencyId() {
        return dependencyId;
    }

    public void setDependencyId(Long dependencyId) {
        this.dependencyId = dependencyId;
    }


    public UUID getPrerequisiteTaskId() {
        return prerequisiteTaskId;
    }

    public void setPrerequisiteTaskId(UUID prerequisiteTaskId) {
        this.prerequisiteTaskId = prerequisiteTaskId;
    }


    public UUID getNextTaskId() {
        return nextTaskId;
    }

    public void setNextTaskId(UUID nextTaskId) {
        this.nextTaskId = nextTaskId;
    }


    public String getRuleStatus() {
        return ruleStatus;
    }

    public void setRuleStatus(String ruleStatus) {
        this.ruleStatus = ruleStatus;
    }


    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }


    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}