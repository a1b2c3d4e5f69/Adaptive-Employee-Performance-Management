package com.aepm.ui.admin;

import com.aepm.logic.attendance.AttendanceService;

import com.aepm.model.AttendanceQrCode;
import com.aepm.model.AttendanceRecord;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.aepm.ui.navigation.NavigationManager;
import com.aepm.util.SessionManager;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import javafx.scene.control.TextField;
import javafx.scene.control.DatePicker;
import java.time.LocalDate;

public class AdminAttendanceController {

    // =====================================================
    // FXML COMPONENTS
    // =====================================================

    @FXML
    private ImageView qrImage;

    @FXML
    private Label expiryLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private Button generateButton;

    @FXML
    private Button refreshButton;

    @FXML
    private Label totalEmployeesLabel;

    @FXML
    private Label presentTodayLabel;

    @FXML
    private Label lateTodayLabel;

    @FXML
    private Label absentTodayLabel;

    @FXML
    private TableView<AttendanceRecord> recordsTable;

    @FXML
    private TableColumn<AttendanceRecord, String>
            employeeColumn;

    @FXML
    private TableColumn<AttendanceRecord, String>
            dateColumn;

    @FXML
    private TableColumn<AttendanceRecord, String>
            checkInColumn;

    @FXML
    private TableColumn<AttendanceRecord, String>
            checkOutColumn;

    @FXML
    private TableColumn<AttendanceRecord, String>
        statusColumn;
    @FXML
    private TableColumn<AttendanceRecord, String>
            overtimeColumn;

    @FXML
    private TextField employeeSearchField;

    @FXML
    private DatePicker attendanceDatePicker;

    // =====================================================
    // SERVICE
    // =====================================================

    private final AttendanceService attendanceService =
            new AttendanceService();

    private String accessToken;

    private Map<String, String> employeeIdMap = new HashMap<>();

    private String getValidAccessToken() {

        if (accessToken != null && !accessToken.isBlank()) {
            return accessToken;
        }

        String sessionToken = SessionManager.getAccessToken();

        if (sessionToken != null && !sessionToken.isBlank()) {
            accessToken = sessionToken;
            return accessToken;
        }

        return null;
    }


    // =====================================================
    // SIDEBAR NAVIGATION
    // =====================================================

    @FXML
    private void handleHome(ActionEvent event) {
        navigate(event, "/fxml/admin/adminHome.fxml");
    }

    @FXML
    private void handleProfile(ActionEvent event) {
        navigate(event, "/fxml/admin/adminProfile.fxml");
    }

    @FXML
    private void handleUserManagement(ActionEvent event) {
        navigate(event, "/fxml/admin/adminUserManagement.fxml");
    }

    @FXML
    private void handleTaskManagement(ActionEvent event) {
        navigate(event, "/fxml/admin/adminTask.fxml");
    }

    @FXML
    private void handleKpiManagement(ActionEvent event) {
        navigate(event, "/fxml/admin/adminKpi.fxml");
    }

    @FXML
    private void handleSystemConfiguration(ActionEvent event) {
        navigate(event, "/fxml/admin/systemConfiguration.fxml");
    }

    @FXML
    private void handleAttendanceManagement() {
        // Already on Attendance Management page.
    }

    @FXML
    private void handleReports() {
        System.out.println("Reports not implemented yet.");
    }

    @FXML
    private void handleAnonymousReports() {
        System.out.println("Anonymous Reports not implemented yet.");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        SessionManager.clearSession();
        navigate(event, "/fxml/auth/login.fxml");
    }

    
    @FXML
    private void handleEscalationRules(ActionEvent event) {

        Stage stage = (Stage) ((Node) event.getSource())
                .getScene().getWindow();

        NavigationManager.navigate(
                stage,
                "/fxml/admin/adminEscalationRules.fxml"
        );
    }


    private void navigate(ActionEvent event, String path) {

        Stage stage = (Stage) ((Node) event.getSource())
                .getScene()
                .getWindow();

        NavigationManager.navigate(stage, path);
    }

    
    public void loadAttendanceData(String accessToken) {

        if (accessToken == null || accessToken.isBlank()) {
            statusLabel.setText(
                    "Session missing. Please login again."
            );
            return;
        }

        setAccessToken(accessToken);
    }


    private static final ZoneId MALAYSIA_ZONE =
            ZoneId.of("Asia/Kuala_Lumpur");

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern(
                    "dd MMM yyyy, hh:mm a"
            );

    // =====================================================
    // INITIALIZE
    // =====================================================

    @FXML
    public void initialize() {
        recordsTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS
        );

        // Center all Attendance Records table columns
        employeeColumn.setStyle("-fx-alignment: CENTER;");
        dateColumn.setStyle("-fx-alignment: CENTER;");
        checkInColumn.setStyle("-fx-alignment: CENTER;");
        checkOutColumn.setStyle("-fx-alignment: CENTER;");
        statusColumn.setStyle("-fx-alignment: CENTER;");    
        overtimeColumn.setStyle("-fx-alignment: CENTER;");

        employeeColumn.setCellValueFactory(cell -> {
            String uuid = cell.getValue()
                    .getEmployeeId()
                    .toString();

            String employeeId = employeeIdMap.getOrDefault(
                    uuid,
                    uuid
            );

            return new SimpleStringProperty(employeeId);
        });

        dateColumn.setCellValueFactory(
                cell -> new SimpleStringProperty(
                        cell.getValue()
                                .getAttendanceDate()
                                .toString()
                )
        );

        checkInColumn.setCellValueFactory(
                cell -> new SimpleStringProperty(
                        formatTime(
                                cell.getValue()
                                        .getCheckInTime()
                        )
                )
        );

        checkOutColumn.setCellValueFactory(
                cell -> new SimpleStringProperty(
                        formatTime(
                                cell.getValue()
                                        .getCheckOutTime()
                        )
                )
        );

        statusColumn.setCellValueFactory(
                cell -> new SimpleStringProperty(
                        cell.getValue()
                                .getAttendanceStatus()
                )
        );

        overtimeColumn.setCellValueFactory(
                cell -> new SimpleStringProperty(
                        cell.getValue().getFormattedOvertime()
                )
        );
        
        statusLabel.setText(
                "Checking today's QR Code..."
        );

        expiryLabel.setText(
                "Loading today's QR Code..."
        );

        // Automatic QR generation - button disabled
        generateButton.setDisable(true);
        generateButton.setText("Generated Automatically");

        generateButton.setStyle(
                "-fx-background-color: #D1D5DB;" +
                "-fx-text-fill: #6B7280;" +
                "-fx-opacity: 1;" +
                "-fx-cursor: default;"
        );

        Platform.runLater(() -> {

            String token = SessionManager.getAccessToken();

            if (token == null || token.isBlank()) {
                statusLabel.setText(
                    "Session expired. Please login again."
                );
                return;
            }

            setAccessToken(token);
        });

    }

    // =====================================================
    // SET AUTHENTICATED SESSION
    // =====================================================
    public void setAccessToken(String accessToken) {

        this.accessToken = accessToken;

        loadTodayQrCode();
        refreshAttendanceRecords();
        loadEmployeeIdMap();
    }
    
    private void loadEmployeeIdMap() {

        String token = getValidAccessToken();

        if (token == null) {
            return;
        }

        CompletableFuture.supplyAsync(() -> {
            try {
                return attendanceService.getEmployeeIdMap(token);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).whenComplete((result, error) -> {

            Platform.runLater(() -> {

                if (error != null) {
                    System.err.println(
                            "Unable to load Employee IDs: "
                            + error.getMessage()
                    );
                    return;
                }

                employeeIdMap = result;

                recordsTable.refresh();
            });
        });
    }

    private void loadTodayQrCode() {

        String token = requireAccessToken();

        if (token == null) {
            return;
        }

        generateButton.setDisable(true);
        generateButton.setText("Loading...");
        statusLabel.setText("Checking today's QR Code...");

        long startTime = System.currentTimeMillis();

        CompletableFuture.supplyAsync(() -> {
            try {
                return attendanceService.getTodayQrCode(token);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).whenComplete((qrCode, error) -> {

            long duration = System.currentTimeMillis() - startTime;

            System.out.println(
                "[Attendance] QR Code query took "
                    + duration + " ms"
            );
            Platform.runLater(() -> {

                if (error != null) {
                    // Keep disabled if database check fails.
                    // We cannot safely assume no QR exists.
                    generateButton.setDisable(true);
                    generateButton.setText("Unable to verify QR Code");
                    showError(error);
                    return;
                }

                if (qrCode == null) {

                    qrImage.setImage(null);

                    expiryLabel.setText(
                            "No QR Code generated yet."
                    );
                    statusLabel.setText(
                        "Waiting for today's automatic QR Code."
                    );

                    updateGenerateButton(false);

                } else {

                    displayQrCode(qrCode);

                    statusLabel.setText(
                            "Today's QR Code loaded successfully."
                    );

                    updateGenerateButton(true);
                }
            });
        });
    }

    private String requireAccessToken() {

        String token = getValidAccessToken();

        if (token == null) {

            statusLabel.setText(
                "Error: Login session is missing. Please login again."
            );

            return null;
        }

        return token;
    }

    // =====================================================
    // GENERATE DAILY QR CODE
    // =====================================================
    @FXML
    private void handleGenerateQr() {

        // QR Code is generated automatically by Supabase Cron.
        // Manual generation is disabled.

        statusLabel.setText(
                "Daily QR Code is generated automatically."
        );
    }

    private void updateGenerateButton(boolean generated) {

        generateButton.setDisable(true);

        generateButton.setStyle(
            "-fx-background-color: #D1D5DB;" +
            "-fx-text-fill: #6B7280;" +
            "-fx-opacity: 1;" +
            "-fx-cursor: default;"
        );

        if (generated) {
            generateButton.setText("Generated Automatically");
        } else {
            generateButton.setText("Waiting for Automatic QR");
        }
    }


    // =====================================================
    // DISPLAY QR CODE
    // =====================================================

    private void displayQrCode(
            AttendanceQrCode qrCode
    ) {

        try {

            WritableImage image =
                    generateQrImage(
                            qrCode.getQrToken()
                    );

            qrImage.setImage(image);

            expiryLabel.setText(
                    "Expires: "
                            + formatTime(
                                    qrCode.getExpiresAt()
                            )
                            + " (MYT)"
            );

            statusLabel.setText(
                "Today's QR Code loaded successfully."
            );

        } catch (Exception e) {

            showError(e);
        }
    }

    // =====================================================
    // CREATE QR IMAGE USING ZXING
    // =====================================================

    private WritableImage generateQrImage(
            String qrToken
    ) throws Exception {

        int size = 280;

        BitMatrix bitMatrix =
                new MultiFormatWriter().encode(
                        qrToken,
                        BarcodeFormat.QR_CODE,
                        size,
                        size
                );

        WritableImage image =
                new WritableImage(
                        bitMatrix.getWidth(),
                        bitMatrix.getHeight()
                );

        PixelWriter pixelWriter =
                image.getPixelWriter();

        for (int y = 0;
             y < bitMatrix.getHeight();
             y++) {

            for (int x = 0;
                 x < bitMatrix.getWidth();
                 x++) {

                int color =
                        bitMatrix.get(x, y)
                                ? 0xFF16162A
                                : 0xFFFFFFFF;

                pixelWriter.setArgb(
                        x,
                        y,
                        color
                );
            }
        }

        return image;
    }

    // =====================================================
    // REFRESH ATTENDANCE RECORDS
    // =====================================================

    @FXML
    private void handleRefreshRecords() {

        refreshAttendanceRecords();
    }

    @FXML
    private void handleSearchAttendance() {

        String keyword = employeeSearchField.getText().trim();
        LocalDate selectedDate = attendanceDatePicker.getValue();

        String token = getValidAccessToken();

        if (token == null) {
            return;
        }

        CompletableFuture.supplyAsync(() -> {

            try {
                // Query attendance records using date filter
                return attendanceService.searchAttendanceRecords(
                        token,
                        null,
                        selectedDate
                );

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }).whenComplete((records, error) -> {

            Platform.runLater(() -> {

                if (error != null) {
                    showError(error);
                    return;
                }

                // Partial Employee ID Search
                List<AttendanceRecord> filteredRecords =
                        records.stream()
                                .filter(record -> {

                                    if (keyword.isBlank()) {
                                        return true;
                                    }

                                    String uuid = record
                                            .getEmployeeId()
                                            .toString();

                                    String employeeId =
                                            employeeIdMap.getOrDefault(
                                                    uuid,
                                                    uuid
                                            );

                                    return employeeId
                                            .toLowerCase()
                                            .contains(
                                                    keyword.toLowerCase()
                                            );
                                })
                                .toList();

                recordsTable.setItems(
                        FXCollections.observableArrayList(
                                filteredRecords
                        )
                );
            });
        });
    }

    @FXML
    private void handleClearAttendanceFilter() {

        employeeSearchField.clear();
        attendanceDatePicker.setValue(null);

        refreshAttendanceRecords();
    }

    private void refreshAttendanceRecords() {

        final String token;

        try {
            token = requireAccessToken();
        } catch (Exception e) {
            showError(e);
            return;
        }

        refreshButton.setDisable(true);

        CompletableFuture.supplyAsync(() -> {

            try {

                return attendanceService.getAttendanceRecords(
                        token
                );

            } catch (Exception e) {

                throw new RuntimeException(e);
            }

        }).whenComplete((records, error) -> {

            Platform.runLater(() -> {

                refreshButton.setDisable(false);

                if (error != null) {

                    showError(error);
                    return;
                }

                displayAttendanceRecords(records);
                loadTotalEmployees();
            });
        });
    }

    // =====================================================
    // DISPLAY ATTENDANCE RECORDS
    // =====================================================
    private void displayAttendanceRecords(List<AttendanceRecord> records) {
        recordsTable.setItems(
                FXCollections.observableArrayList(records)
        );
        updateAttendanceSummary(records);
    }

    // =====================================================
    // LOAD TOTAL EMPLOYEES
    // =====================================================
    private void loadTotalEmployees() {

        String token = getValidAccessToken();

        if (token == null) {
            totalEmployeesLabel.setText("--");
            return;
        }

        CompletableFuture.supplyAsync(() -> {
            try {
                return attendanceService.getTotalEmployees(token);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).whenComplete((total, error) -> {

            Platform.runLater(() -> {

                if (error != null) {
                    totalEmployeesLabel.setText("--");
                    return;
                }

                totalEmployeesLabel.setText(String.valueOf(total));
            });
        });
    }

    // =====================================================
    // FORMAT MALAYSIA TIME
    // =====================================================

    private String formatTime(
            OffsetDateTime dateTime
    ) {

        if (dateTime == null) {
            return "-";
        }

        return TIME_FORMAT.format(
                dateTime.atZoneSameInstant(
                        MALAYSIA_ZONE
                )
        );
    }

    // =====================================================
    // UPDATE ATTENDANCE SUMMARY
    // =====================================================
    private void updateAttendanceSummary(
            List<AttendanceRecord> records
    ) {

        LocalDate today = LocalDate.now(MALAYSIA_ZONE);

        Set<String> presentEmployees = new HashSet<>();
        Set<String> lateEmployees = new HashSet<>();

        for (AttendanceRecord record : records) {

            if (record.getAttendanceDate() == null
                    || !today.equals(record.getAttendanceDate())) {
                continue;
            }

            if (record.getCheckInTime() == null) {
                continue;
            }

            String employeeId = record.getEmployeeId().toString();

            presentEmployees.add(employeeId);

            if ("Late".equalsIgnoreCase(
                    record.getAttendanceStatus())) {

                lateEmployees.add(employeeId);
            }
        }

        presentTodayLabel.setText(
                String.valueOf(presentEmployees.size())
        );

        lateTodayLabel.setText(
                String.valueOf(lateEmployees.size())
        );

        // Requires employee count and working-day rules.
        absentTodayLabel.setText("--");
    }

    // =====================================================
    // ERROR HANDLING
    // =====================================================

    private void showError(Throwable error) {

        Throwable cause = error;

        while (cause.getCause() != null) {
            cause = cause.getCause();
        }

        statusLabel.setText(
                "Error: " + cause.getMessage()
        );
    }
}
