package com.aepm.model;

import java.time.OffsetDateTime;

public class KpiConfig {

    private Long configId;
    private String configName;
    private double riskThreshold;
    private String effectiveDate;
    private String configStatus;
    private String createdBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;


    public KpiConfig() {
    }


    public Long getConfigId() {
        return configId;
    }

    public void setConfigId(Long configId) {
        this.configId = configId;
    }


    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }


    public double getRiskThreshold() {
        return riskThreshold;
    }

    public void setRiskThreshold(double riskThreshold) {
        this.riskThreshold = riskThreshold;
    }


    public String getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(String effectiveDate) {
        this.effectiveDate = effectiveDate;
    }


    public String getConfigStatus() {
        return configStatus;
    }

    public void setConfigStatus(String configStatus) {
        this.configStatus = configStatus;
    }


    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
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


    @Override
    public String toString() {
        return configName;
    }
}