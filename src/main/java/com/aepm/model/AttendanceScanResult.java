
package com.aepm.model;

import org.json.JSONObject;
import java.time.OffsetDateTime;

public class AttendanceScanResult {

    private String actionType;
    private long attendanceId;
    private String attendanceStatus;
    private OffsetDateTime checkInTime;
    private OffsetDateTime checkOutTime;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public AttendanceScanResult(
            String actionType,
            long attendanceId,
            String attendanceStatus,
            OffsetDateTime checkInTime,
            OffsetDateTime checkOutTime
    ) {
        this.actionType = actionType;
        this.attendanceId = attendanceId;
        this.attendanceStatus = attendanceStatus;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
    }

    // =====================================================
    // CONVERT JSON TO ATTENDANCE SCAN RESULT
    // =====================================================

    public static AttendanceScanResult fromJson(
            JSONObject data
    ) {

        if (data == null) {
            throw new IllegalArgumentException(
                    "Attendance result cannot be null."
            );
        }

        String actionType = data.optString(
                "action_type", ""
        );

        long attendanceId = data.getLong(
                "attendance_id"
        );

        String attendanceStatus = data.optString(
                "attendance_status", ""
        );

        OffsetDateTime checkInTime = parseDateTime(
                data, "check_in_time"
        );

        OffsetDateTime checkOutTime = parseDateTime(
                data, "check_out_time"
        );

        return new AttendanceScanResult(
                actionType,
                attendanceId,
                attendanceStatus,
                checkInTime,
                checkOutTime
        );
    }

    // =====================================================
    // CONVERT TIMESTAMP
    // =====================================================

    private static OffsetDateTime parseDateTime(
            JSONObject data,
            String key
    ) {

        if (!data.has(key) || data.isNull(key)) {
            return null;
        }

        return OffsetDateTime.parse(
                data.getString(key)
        );
    }

    // =====================================================
    // GETTERS
    // =====================================================

    public String getActionType() {
        return actionType;
    }

    public long getAttendanceId() {
        return attendanceId;
    }

    public String getAttendanceStatus() {
        return attendanceStatus;
    }

    public OffsetDateTime getCheckInTime() {
        return checkInTime;
    }

    public OffsetDateTime getCheckOutTime() {
        return checkOutTime;
    }

    // =====================================================
    // SETTERS
    // =====================================================

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public void setAttendanceId(long attendanceId) {
        this.attendanceId = attendanceId;
    }

    public void setAttendanceStatus(String attendanceStatus) {
        this.attendanceStatus = attendanceStatus;
    }

    public void setCheckInTime(OffsetDateTime checkInTime) {
        this.checkInTime = checkInTime;
    }

    public void setCheckOutTime(OffsetDateTime checkOutTime) {
        this.checkOutTime = checkOutTime;
    }
}
