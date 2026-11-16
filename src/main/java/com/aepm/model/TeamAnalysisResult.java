package com.aepm.model;

import java.util.ArrayList;
import java.util.List;

public class TeamAnalysisResult {

    private int employeeCount;

    private double averageKpi;
    private double averageProductivity;
    private double averageTimeliness;
    private double averageCompletion;
    private double averageAttendance;

    private int totalTasks;
    private int completedTasks;
    private int overdueTasks;

    private List<EmployeeAnalysisResult> employees =
            new ArrayList<>();

    public int getEmployeeCount() {
        return employeeCount;
    }

    public void setEmployeeCount(int employeeCount) {
        this.employeeCount = employeeCount;
    }

    public double getAverageKpi() {
        return averageKpi;
    }

    public void setAverageKpi(double averageKpi) {
        this.averageKpi = averageKpi;
    }

    public double getAverageProductivity() {
        return averageProductivity;
    }

    public void setAverageProductivity(double averageProductivity) {
        this.averageProductivity = averageProductivity;
    }

    public double getAverageTimeliness() {
        return averageTimeliness;
    }

    public void setAverageTimeliness(double averageTimeliness) {
        this.averageTimeliness = averageTimeliness;
    }

    public double getAverageCompletion() {
        return averageCompletion;
    }

    public void setAverageCompletion(double averageCompletion) {
        this.averageCompletion = averageCompletion;
    }

    public double getAverageAttendance() {
        return averageAttendance;
    }

    public void setAverageAttendance(double averageAttendance) {
        this.averageAttendance = averageAttendance;
    }

    public int getTotalTasks() {
        return totalTasks;
    }

    public void setTotalTasks(int totalTasks) {
        this.totalTasks = totalTasks;
    }

    public int getCompletedTasks() {
        return completedTasks;
    }

    public void setCompletedTasks(int completedTasks) {
        this.completedTasks = completedTasks;
    }

    public int getOverdueTasks() {
        return overdueTasks;
    }

    public void setOverdueTasks(int overdueTasks) {
        this.overdueTasks = overdueTasks;
    }

    public List<EmployeeAnalysisResult> getEmployees() {
        return employees;
    }

    public void setEmployees(
            List<EmployeeAnalysisResult> employees
    ) {
        this.employees = employees;
    }
}