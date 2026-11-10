package com.aepm.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

public class EmployeeDailyTask {

    private UUID dailyTaskId;
    private UUID employeeId;
    private String taskTitle;
    private String taskDescription;
    private String category;
    private String priority;
    private LocalDate taskDate;
    private LocalTime dueTime;
    private String taskStatus;
    private OffsetDateTime completedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public EmployeeDailyTask() {
    }

    public EmployeeDailyTask(
            UUID dailyTaskId,
            UUID employeeId,
            String taskTitle,
            String taskDescription,
            String category,
            String priority,
            LocalDate taskDate,
            LocalTime dueTime,
            String taskStatus,
            OffsetDateTime completedAt,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        this.dailyTaskId = dailyTaskId;
        this.employeeId = employeeId;
        this.taskTitle = taskTitle;
        this.taskDescription = taskDescription;
        this.category = category;
        this.priority = priority;
        this.taskDate = taskDate;
        this.dueTime = dueTime;
        this.taskStatus = taskStatus;
        this.completedAt = completedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getDailyTaskId() {
        return dailyTaskId;
    }

    public void setDailyTaskId(UUID dailyTaskId) {
        this.dailyTaskId = dailyTaskId;
    }

    public UUID getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(UUID employeeId) {
        this.employeeId = employeeId;
    }

    public String getTaskTitle() {
        return taskTitle;
    }

    public void setTaskTitle(String taskTitle) {
        this.taskTitle = taskTitle;
    }

    public String getTaskDescription() {
        return taskDescription;
    }

    public void setTaskDescription(String taskDescription) {
        this.taskDescription = taskDescription;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public LocalDate getTaskDate() {
        return taskDate;
    }

    public void setTaskDate(LocalDate taskDate) {
        this.taskDate = taskDate;
    }

    public LocalTime getDueTime() {
        return dueTime;
    }

    public void setDueTime(LocalTime dueTime) {
        this.dueTime = dueTime;
    }

    public String getTaskStatus() {
        return taskStatus;
    }

    public void setTaskStatus(String taskStatus) {
        this.taskStatus = taskStatus;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}