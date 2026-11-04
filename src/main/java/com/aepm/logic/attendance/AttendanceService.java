package com.aepm.logic.attendance;

import com.aepm.data.repository.AttendanceRepository;

import com.aepm.model.AttendanceQrCode;
import com.aepm.model.AttendanceRecord;
import com.aepm.model.AttendanceScanResult;

import java.util.List;
import java.util.Map;

public class AttendanceService {

    private final AttendanceRepository attendanceRepository;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public AttendanceService() {

        attendanceRepository =
                new AttendanceRepository();
    }

    // =====================================================
    // ADMIN - GENERATE DAILY QR CODE
    // =====================================================
    public AttendanceQrCode generateDailyQr(
            String accessToken
    ) throws Exception {

        validateAccessToken(accessToken);

        return attendanceRepository.generateDailyQr(
                accessToken
        );
    }

    public AttendanceQrCode getTodayQrCode(
            String accessToken
    ) throws Exception {

        validateAccessToken(accessToken);

        return attendanceRepository.getTodayQrCode(
                accessToken
        );
    }

    // =====================================================
    // EMPLOYEE - CHECK IN / CHECK OUT
    // =====================================================
    public AttendanceScanResult scanAttendance(String qrToken,String accessToken ) throws Exception {

        validateAccessToken(accessToken);

        if (qrToken == null || qrToken.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Please scan a valid QR Code."
            );
        }

        return attendanceRepository.scanAttendance(
                qrToken.trim(),
                accessToken
        );
    }

    /**
     * Allow employees to check out after 10:00 PM
     * without scanning the daily QR Code.
     *
     * Supabase validates the time, employee identity,
     * attendance record and calculates overtime.
     */
    public AttendanceScanResult checkoutAttendance(String accessToken) throws Exception {
        validateAccessToken(accessToken);

        return attendanceRepository.checkoutAttendance(
                accessToken
        );
    }

    // =====================================================
    // GET ATTENDANCE RECORDS
    // =====================================================

    public List<AttendanceRecord> getAttendanceRecords(
            String accessToken
    ) throws Exception {

        validateAccessToken(accessToken);

        return attendanceRepository.getAttendanceRecords(
                accessToken
        );
    }

    // =====================================================
    // VALIDATE ACCESS TOKEN
    // =====================================================
    public int getTotalEmployees(String accessToken) throws Exception {

        validateAccessToken(accessToken);

        return attendanceRepository.getTotalEmployees(accessToken);
    }

    public Map<String, String> getEmployeeIdMap(String accessToken) throws Exception {
        validateAccessToken(accessToken);

        return attendanceRepository.getEmployeeIdMap(accessToken);
    }

    public List<AttendanceRecord> searchAttendanceRecords(
            String accessToken,
            String employeeUuid,
            java.time.LocalDate date
    ) throws Exception {

        validateAccessToken(accessToken);

        return attendanceRepository.searchAttendanceRecords(
                accessToken,
                employeeUuid,
                date
        );
    }
    private void validateAccessToken(
            String accessToken
    ) {

        if (accessToken == null
                || accessToken.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Your session has expired. Please login again."
            );
        }
    }
}
