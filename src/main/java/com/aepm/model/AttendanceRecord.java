
package com.aepm.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public class AttendanceRecord {

    private long attendanceId;
    private UUID employeeId;
    private UUID qrId;
    private LocalDate attendanceDate;
    private OffsetDateTime checkInTime;
    private OffsetDateTime checkOutTime;
    private String attendanceStatus;
    private Integer overtimeMinutes;

    public AttendanceRecord(
            long attendanceId,
            UUID employeeId,
            UUID qrId,
            LocalDate attendanceDate,
            OffsetDateTime checkInTime,
            OffsetDateTime checkOutTime,
            String attendanceStatus,
            Integer overtimeMinutes
    ) {
        this.attendanceId = attendanceId;
        this.employeeId = employeeId;
        this.qrId = qrId;
        this.attendanceDate = attendanceDate;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
        this.attendanceStatus = attendanceStatus;
        this.overtimeMinutes = overtimeMinutes;
    }

    // Keep old constructor compatible
    public AttendanceRecord(
            long attendanceId,
            UUID employeeId,
            UUID qrId,
            LocalDate attendanceDate,
            OffsetDateTime checkInTime,
            OffsetDateTime checkOutTime,
            String attendanceStatus
    ) {
        this(
                attendanceId,
                employeeId,
                qrId,
                attendanceDate,
                checkInTime,
                checkOutTime,
                attendanceStatus,
                null
        );
    }

    public long getAttendanceId() {
        return attendanceId;
    }

    public UUID getEmployeeId() {
        return employeeId;
    }

    public UUID getQrId() {
        return qrId;
    }

    public LocalDate getAttendanceDate() {
        return attendanceDate;
    }

    public OffsetDateTime getCheckInTime() {
        return checkInTime;
    }

    public OffsetDateTime getCheckOutTime() {
        return checkOutTime;
    }

    public String getAttendanceStatus() {
        return attendanceStatus;
    }

    public Integer getOvertimeMinutes() {
        return overtimeMinutes;
    }

    public void setAttendanceId(long attendanceId) {
        this.attendanceId = attendanceId;
    }

    public void setEmployeeId(UUID employeeId) {
        this.employeeId = employeeId;
    }

    public void setQrId(UUID qrId) {
        this.qrId = qrId;
    }

    public void setAttendanceDate(LocalDate attendanceDate) {
        this.attendanceDate = attendanceDate;
    }

    public void setCheckInTime(OffsetDateTime checkInTime) {
        this.checkInTime = checkInTime;
    }

    public void setCheckOutTime(OffsetDateTime checkOutTime) {
        this.checkOutTime = checkOutTime;
    }

    public void setAttendanceStatus(String attendanceStatus) {
        this.attendanceStatus = attendanceStatus;
    }

    public void setOvertimeMinutes(Integer overtimeMinutes) {
        this.overtimeMinutes = overtimeMinutes;
    }

    // =====================================================
    // FORMAT OVERTIME
    // =====================================================
    public String getFormattedOvertime() {

        if (overtimeMinutes == null || overtimeMinutes <= 0) {
            return "—";
        }

        long hours = overtimeMinutes / 60;
        long minutes = overtimeMinutes % 60;

        if (hours == 0) {
            return minutes + "m";
        }

        if (minutes == 0) {
            return hours + "h";
        }

        return hours + "h " + minutes + "m";
    }
}
