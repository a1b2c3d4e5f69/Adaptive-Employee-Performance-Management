package com.aepm.model;

import java.time.LocalDate;

public class Holiday {

    private long holidayId;
    private String holidayName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
    private boolean nonWorkingDay;

    public Holiday(
            long holidayId,
            String holidayName,
            LocalDate startDate,
            LocalDate endDate,
            String description,
            boolean nonWorkingDay) {

        this.holidayId = holidayId;
        this.holidayName = holidayName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.description = description;
        this.nonWorkingDay = nonWorkingDay;
    }

    public long getHolidayId() {
        return holidayId;
    }

    public String getHolidayName() {
        return holidayName;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getDescription() {
        return description;
    }

    public boolean isNonWorkingDay() {
        return nonWorkingDay;
    }
}