package com.aepm.model;

public class WorkingHoursConfig {

    private long id;
    private String dayOfWeek;
    private String startTime;
    private String endTime;
    private String breakStartTime;
    private String breakEndTime;
    private int gracePeriodMinutes;
    private boolean workingDay;

    public WorkingHoursConfig(
            long id,
            String dayOfWeek,
            String startTime,
            String endTime,
            String breakStartTime,
            String breakEndTime,
            int gracePeriodMinutes,
            boolean workingDay) {

        this.id = id;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.breakStartTime = breakStartTime;
        this.breakEndTime = breakEndTime;
        this.gracePeriodMinutes = gracePeriodMinutes;
        this.workingDay = workingDay;
    }

    public long getId() {
        return id;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getBreakStartTime() {
        return breakStartTime;
    }

    public String getBreakEndTime() {
        return breakEndTime;
    }

    public int getGracePeriodMinutes() {
        return gracePeriodMinutes;
    }

    public boolean isWorkingDay() {
        return workingDay;
    }
}