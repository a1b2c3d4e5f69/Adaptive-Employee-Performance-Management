package com.aepm.model;

public class TaskCategory {

    private Long categoryId;
    private String categoryName;
    private String description;
    private String categoryStatus;
    private String createdBy;


    public TaskCategory() {
    }


    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }


    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public String getCategoryStatus() {
        return categoryStatus;
    }

    public void setCategoryStatus(String categoryStatus) {
        this.categoryStatus = categoryStatus;
    }


    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }


    @Override
    public String toString() {
        return categoryName;
    }
}