package com.aepm.data.repository;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;

import com.aepm.model.AttendanceQrCode;
import com.aepm.model.AttendanceRecord;
import com.aepm.model.AttendanceScanResult;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class AttendanceRepository {

    private final SupabaseClient supabaseClient =
            SupabaseClient.getInstance();

    // =====================================================
    // SEND REQUEST TO SUPABASE
    // =====================================================

    private String sendRequest(
            String url,
            String accessToken,
            JSONObject body
    ) throws Exception {

        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException(
                    "Access token is missing. Please login again."
            );
        }

        HttpRequest.Builder builder =
                supabaseClient.createAuthenticatedRequest(
                        url,
                        accessToken
                );

        HttpRequest request;

        if (body == null) {

            request = builder
                    .GET()
                    .build();

        } else {

            request = builder
                    .POST(
                            HttpRequest.BodyPublishers.ofString(
                                    body.toString()
                            )
                    )
                    .build();
        }

        HttpResponse<String> response =
                supabaseClient.getHttpClient().send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {

            String message = response.body();

            try {
                JSONObject error = new JSONObject(message);

                message = error.optString(
                        "message",
                        message
                );
            } catch (Exception ignored) {
                // Keep original error response.
            }

            throw new RuntimeException(
                    "Supabase Error ("
                            + response.statusCode()
                            + "): "
                            + message
            );
        }

        return response.body();
    }

    // =====================================================
    // CONVERT TIMESTAMP
    // =====================================================

    private OffsetDateTime parseDateTime(
            JSONObject object,
            String key
    ) {

        if (!object.has(key) || object.isNull(key)) {
            return null;
        }

        return OffsetDateTime.parse(
                object.getString(key)
        );
    }

    // =====================================================
    // ADMIN - GENERATE DAILY QR CODE
    // RPC: aepm_generate_daily_qr
    // =====================================================

    public AttendanceQrCode generateDailyQr(
            String accessToken
    ) throws Exception {

        String url =
                AppConfig.REST_URL
                        + "/rpc/aepm_generate_daily_qr";

        String response = sendRequest(
                url,
                accessToken,
                new JSONObject()
        );

        JSONArray results =
                new JSONArray(response);

        if (results.length() == 0) {
            throw new RuntimeException(
                    "Failed to generate Daily QR Code."
            );
        }

        JSONObject data =
                results.getJSONObject(0);

        return new AttendanceQrCode(
                UUID.fromString(
                        data.getString("qr_id")
                ),
                data.getString("qr_token"),
                parseDateTime(
                        data,
                        "expires_at"
                )
        );
    }

    public AttendanceQrCode getTodayQrCode(
            String accessToken
    ) throws Exception {

        java.time.ZoneId malaysiaZone =
                java.time.ZoneId.of("Asia/Kuala_Lumpur");

        java.time.LocalDate today =
                java.time.LocalDate.now(malaysiaZone);

        String start = today
                .atStartOfDay(malaysiaZone)
                .toOffsetDateTime()
                .toString();

        String end = today
                .plusDays(1)
                .atStartOfDay(malaysiaZone)
                .toOffsetDateTime()
                .toString();

        String startEncoded = java.net.URLEncoder.encode(
                "gte." + start,
                java.nio.charset.StandardCharsets.UTF_8
        );

        String endEncoded = java.net.URLEncoder.encode(
                "lt." + end,
                java.nio.charset.StandardCharsets.UTF_8
        );

        String url = AppConfig.REST_URL
                + "/attendance_qr_code_tbl"
                + "?select=qr_id,qr_token,expires_at"
                + "&created_at=" + startEncoded
                + "&created_at=" + endEncoded
                + "&order=created_at.desc"
                + "&limit=1";

        // PostgREST requires different filter parameters
        // for two comparisons on the same column.
        String filter = java.net.URLEncoder.encode(
                "(created_at.gte." + start
                        + ",created_at.lt." + end + ")",
                java.nio.charset.StandardCharsets.UTF_8
        );

        url = AppConfig.REST_URL
                + "/attendance_qr_code_tbl"
                + "?select=qr_id,qr_token,expires_at"
                + "&and=" + filter
                + "&order=created_at.desc"
                + "&limit=1";

        String response = sendRequest(
                url,
                accessToken,
                null
        );

        JSONArray results = new JSONArray(response);

        if (results.length() == 0) {
            return null;
        }

        JSONObject data = results.getJSONObject(0);

        return new AttendanceQrCode(
                UUID.fromString(data.getString("qr_id")),
                data.getString("qr_token"),
                parseDateTime(data, "expires_at")
        );
    }


    // =====================================================
    // EMPLOYEE - CHECK IN / CHECK OUT
    // RPC: aepm_scan_attendance
    // =====================================================

    public AttendanceScanResult scanAttendance(
            String qrToken,
            String accessToken
    ) throws Exception {

        if (qrToken == null || qrToken.isBlank()) {
            throw new IllegalArgumentException(
                    "QR Token cannot be empty."
            );
        }

        String url =
                AppConfig.REST_URL
                        + "/rpc/aepm_scan_attendance";

        JSONObject body =
                new JSONObject();

        body.put(
                "p_qr_token",
                qrToken.trim()
        );

        String response = sendRequest(
                url,
                accessToken,
                body
        );

        JSONArray results =
                new JSONArray(response);

        if (results.length() == 0) {
            throw new RuntimeException(
                    "No attendance result returned."
            );
        }

        JSONObject data =
                results.getJSONObject(0);

        String status = data.isNull("attendance_status")
                ? ""
                : data.getString("attendance_status");

        return new AttendanceScanResult(
                data.getString("action_type"),
                data.getLong("attendance_id"),
                status,
                parseDateTime(
                        data,
                        "check_in_time"
                ),
                parseDateTime(
                        data,
                        "check_out_time"
                )
        );
    }

    /**
     * Check out after 10:00 PM without scanning QR Code.
     * Supabase validates the logged-in employee,
     * check-in record, check-out time and overtime.
     */
    public AttendanceScanResult checkoutAttendance(
            String accessToken
    ) throws Exception {

        String url = AppConfig.REST_URL
                + "/rpc/aepm_checkout_attendance";

        // RPC has no parameters.
        JSONObject body = new JSONObject();

        String response = sendRequest(
                url,
                accessToken,
                body
        );

        JSONArray results = new JSONArray(response);

        if (results.length() == 0) {
            throw new RuntimeException(
                    "No check-out result returned."
            );
        }

        JSONObject result = results.getJSONObject(0);

        return AttendanceScanResult.fromJson(result);
    }

    // =====================================================
    // GET ATTENDANCE RECORDS
    // Supabase RLS controls which records are visible.
    // =====================================================
    public List<AttendanceRecord> getAttendanceRecords(
            String accessToken
    ) throws Exception {
        String url = buildAttendanceRecordsUrl(null, null);
        String response = sendRequest(
                url,
                accessToken,
                null
        );

        JSONArray results =
                new JSONArray(response);

        List<AttendanceRecord> records =
                new ArrayList<>();

        for (int i = 0; i < results.length(); i++) {

            JSONObject data =
                    results.getJSONObject(i);

            UUID qrId = null;

            if (data.has("qr_id")
                    && !data.isNull("qr_id")) {

                qrId = UUID.fromString(
                        data.getString("qr_id")
                );
            }

            String status =
                    data.isNull("attendance_status")
                            ? ""
                            : data.getString("attendance_status");

            
            AttendanceRecord record =
                    new AttendanceRecord(
                            data.getLong("attendance_id"),
                            UUID.fromString(
                                    data.getString("employee_id")
                            ),
                            qrId,
                            LocalDate.parse(
                                    data.getString("attendance_date")
                            ),
                            parseDateTime(data, "check_in_time"),
                            parseDateTime(data, "check_out_time"),
                            status,
                            data.isNull("overtime_minutes")
                                    ? null
                                    : data.getInt("overtime_minutes")
                    );
            records.add(record);
        }
        return records;
    }

    // =====================================================
    // ADMIN - GET TOTAL ACTIVE EMPLOYEES
    // =====================================================
    public int getTotalEmployees(String accessToken) throws Exception {

        String url = AppConfig.REST_URL
                + "/user_tbl"
                + "?select=user_id"
                + "&role_id=eq.3"
                + "&account_status=eq.Active";

        String response = sendRequest(
                url,
                accessToken,
                null
        );

        JSONArray employees = new JSONArray(response);

        return employees.length();
    }

    // =====================================================
    // GET EMPLOYEE ID MAPPING
    // UUID -> EMP001
    // =====================================================
    public Map<String, String> getEmployeeIdMap(
            String accessToken
    ) throws Exception {

        String url = AppConfig.REST_URL
                + "/user_tbl"
                + "?select=user_id,employee_id";

        String response = sendRequest(
                url,
                accessToken,
                null
        );

        JSONArray users = new JSONArray(response);

        Map<String, String> employeeMap = new HashMap<>();

        for (int i = 0; i < users.length(); i++) {

            JSONObject user = users.getJSONObject(i);

            String userId = user.getString("user_id");

            String employeeId = user.optString(
                    "employee_id",
                    ""
            );

            if (!employeeId.isBlank()) {
                employeeMap.put(userId, employeeId);
            }
        }

        return employeeMap;
    }

    // =====================================================
    // BUILD ATTENDANCE RECORDS URL
    // =====================================================

    private String buildAttendanceRecordsUrl(
            String employeeUuid,
            LocalDate date
    ) {

        StringBuilder url = new StringBuilder(
                AppConfig.REST_URL
                        + "/attendance_record_tbl"
                        + "?select=*"
        );

        if (employeeUuid != null && !employeeUuid.isBlank()) {
            url.append("&employee_id=eq.")
                    .append(URLEncoder.encode(
                            employeeUuid,
                            StandardCharsets.UTF_8
                    ));
        }

        if (date != null) {
            url.append("&attendance_date=eq.")
                    .append(date);
        }

        url.append("&order=attendance_date.desc")
                .append("&limit=100");

        return url.toString();
    }

    // =====================================================
    // FILTER ATTENDANCE RECORDS
    // =====================================================

    public List<AttendanceRecord> searchAttendanceRecords(
            String accessToken,
            String employeeUuid,
            LocalDate date
    ) throws Exception {

        String url = buildAttendanceRecordsUrl(employeeUuid, date);

        String response = sendRequest(url, accessToken, null);

        JSONArray results = new JSONArray(response);

        List<AttendanceRecord> records = new ArrayList<>();

        for (int i = 0; i < results.length(); i++) {

            JSONObject data = results.getJSONObject(i);

            UUID qrId = null;

            if (data.has("qr_id") && !data.isNull("qr_id")) {
                qrId = UUID.fromString(data.getString("qr_id"));
            }

            String status = data.isNull("attendance_status")
                    ? ""
                    : data.getString("attendance_status");

            
            records.add(new AttendanceRecord(
                    data.getLong("attendance_id"),
                    UUID.fromString(data.getString("employee_id")),
                    qrId,
                    LocalDate.parse(data.getString("attendance_date")),
                    parseDateTime(data, "check_in_time"),
                    parseDateTime(data, "check_out_time"),
                    status,
                    data.isNull("overtime_minutes")
                            ? null
                            : data.getInt("overtime_minutes")
            ));
        }

        return records;
    }
}
