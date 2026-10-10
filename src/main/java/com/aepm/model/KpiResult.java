package com.aepm.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public class KpiResult {

    private Long resultId;
    private String employeeId;
    private Long configId;

    private LocalDate evaluationStartDate;
    private LocalDate evaluationEndDate;

    private double productivityScore;
    private double timelinessScore;
    private double completionRate;
    private double attendanceScore;

    private double overallKpiScore;

    private String performanceStatus;

    private OffsetDateTime calculatedAt;


    public KpiResult() {
    }


    public Long getResultId() {
        return resultId;
    }

    public void setResultId(Long resultId) {
        this.resultId = resultId;
    }


    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }


    public Long getConfigId() {
        return configId;
    }

    public void setConfigId(Long configId) {
        this.configId = configId;
    }


    public LocalDate getEvaluationStartDate() {
        return evaluationStartDate;
    }

    public void setEvaluationStartDate(
            LocalDate evaluationStartDate
    ) {
        this.evaluationStartDate =
                evaluationStartDate;
    }


    public LocalDate getEvaluationEndDate() {
        return evaluationEndDate;
    }

    public void setEvaluationEndDate(
            LocalDate evaluationEndDate
    ) {
        this.evaluationEndDate =
                evaluationEndDate;
    }


    public double getProductivityScore() {
        return productivityScore;
    }

    public void setProductivityScore(
            double productivityScore
    ) {
        this.productivityScore =
                productivityScore;
    }


    public double getTimelinessScore() {
        return timelinessScore;
    }

    public void setTimelinessScore(
            double timelinessScore
    ) {
        this.timelinessScore =
                timelinessScore;
    }


    public double getCompletionRate() {
        return completionRate;
    }

    public void setCompletionRate(
            double completionRate
    ) {
        this.completionRate =
                completionRate;
    }


    public double getAttendanceScore() {
        return attendanceScore;
    }

    public void setAttendanceScore(
            double attendanceScore
    ) {
        this.attendanceScore =
                attendanceScore;
    }


    public double getOverallKpiScore() {
        return overallKpiScore;
    }

    public void setOverallKpiScore(
            double overallKpiScore
    ) {
        this.overallKpiScore =
                overallKpiScore;
    }


    public String getPerformanceStatus() {
        return performanceStatus;
    }

    public void setPerformanceStatus(
            String performanceStatus
    ) {
        this.performanceStatus =
                performanceStatus;
    }


    public OffsetDateTime getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(
            OffsetDateTime calculatedAt
    ) {
        this.calculatedAt =
                calculatedAt;
    }
}