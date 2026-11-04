package com.aepm.ui.employee;
import javafx.stage.FileChooser;
import java.io.File;
import com.aepm.logic.attendance.AttendanceService;
import com.aepm.model.AttendanceRecord;
import com.aepm.model.AttendanceScanResult;
import com.aepm.model.User;
import com.aepm.ui.navigation.NavigationManager;
import com.aepm.util.SessionManager;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.time.LocalTime;
import java.time.ZoneId;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javax.imageio.ImageIO;

public class EmployeeAttendanceController {

    @FXML private Label topNameLabel;
    @FXML private Label topRoleLabel;
    @FXML private Label checkInLabel;
    @FXML private Label checkOutLabel;
    @FXML private Label scanStatusLabel;
    @FXML private Label cameraPlaceholder;

    @FXML private ImageView cameraPreview;
    @FXML private Button startCameraButton;
    @FXML private Button stopCameraButton;
    @FXML private Button lateCheckOutButton;
    @FXML private Button uploadQrButton;

    @FXML private TableView<AttendanceRecord> attendanceTable;
    @FXML private TableColumn<AttendanceRecord, String> dateColumn;
    @FXML private TableColumn<AttendanceRecord, String> checkInColumn;
    @FXML private TableColumn<AttendanceRecord, String> checkOutColumn;
    @FXML private TableColumn<AttendanceRecord, String> statusColumn;
    @FXML private TableColumn<AttendanceRecord, String> overtimeColumn;
    
    @FXML
    private void handleLateCheckOut(ActionEvent event) {
        recordAttendance(null, true);
    }

        
    @FXML private javafx.scene.shape.Rectangle qrScanLine;

    private final AttendanceService attendanceService =
            new AttendanceService();

    private static final ZoneId MALAYSIA_ZONE =
            ZoneId.of("Asia/Kuala_Lumpur");

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("hh:mm a");

    private final ExecutorService cameraExecutor =
            Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(r, "aepm-qr-camera");
                thread.setDaemon(true);
                return thread;
            });

    private final AtomicBoolean scanning =
            new AtomicBoolean(false);

    private final AtomicBoolean submitting =
            new AtomicBoolean(false);

    private Webcam webcam;

    private boolean readingQrImage = false;

    private javafx.animation.TranslateTransition qrScanAnimation;

    private record AttendanceUiResult(
            AttendanceScanResult result,
            Integer overtimeMinutes
    ) {}

    @FXML
    public void initialize() {

        attendanceTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS
        );

        // Center all Attendance History columns
        dateColumn.setStyle("-fx-alignment: CENTER;");
        checkInColumn.setStyle("-fx-alignment: CENTER;");
        checkOutColumn.setStyle("-fx-alignment: CENTER;");
        statusColumn.setStyle("-fx-alignment: CENTER;");
        overtimeColumn.setStyle("-fx-alignment: CENTER;");

        User user = SessionManager.getCurrentUser();

        if (user != null) {
            String name = user.getFullName();

            if (name == null || name.isBlank()) {
                name = user.getUsername();
            }

            topNameLabel.setText(
                    name == null ? "Employee" : name
            );

            if (topRoleLabel != null) {

                String employeeId = user.getEmployeeId();

                if (employeeId != null && !employeeId.isBlank()) {

                    topRoleLabel.setText(
                            "EMPLOYEE  •  " + employeeId
                    );

                } else {

                    topRoleLabel.setText("EMPLOYEE");
                }
            }
        }

        dateColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(
                        cell.getValue()
                                .getAttendanceDate()
                                .toString()
                )
        );

        checkInColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(
                        formatTime(
                                cell.getValue().getCheckInTime()
                        )
                )
        );

        checkOutColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(
                        formatTime(
                                cell.getValue().getCheckOutTime()
                        )
                )
        );

        statusColumn.setCellValueFactory(cell ->
            new SimpleStringProperty(
                cell.getValue().getAttendanceStatus()
            )
        );

        overtimeColumn.setCellValueFactory(cell ->
            new SimpleStringProperty(
                cell.getValue().getFormattedOvertime()
            )
        );

        cameraPlaceholder.setVisible(true);
        stopCameraButton.setDisable(true);

        refreshAttendanceHistory();
        startLateCheckOutTimer();
    }

    // =====================================================
    // START CAMERA
    // =====================================================

    @FXML
    private void handleStartCamera() {

        if (scanning.get() || submitting.get() || readingQrImage) {
            return;
        }

        scanning.set(true);

        startCameraButton.setDisable(true);
        stopCameraButton.setDisable(false);

        scanStatusLabel.setText("Starting camera...");

        cameraExecutor.submit(() -> {
            try {
                webcam = Webcam.getDefault();

                if (webcam == null) {
                    throw new IllegalStateException(
                            "No webcam detected."
                    );
                }

                Dimension resolution =
                        WebcamResolution.VGA.getSize();

                webcam.setViewSize(resolution);
                webcam.open();

                Platform.runLater(() -> {
                    cameraPlaceholder.setVisible(false);
                    scanStatusLabel.setText(
                            "Camera ready. Point it at Admin's QR Code."
                    );
                });

                scanLoop();

            } catch (Exception e) {
                scanning.set(false);

                Platform.runLater(() -> {
                    scanStatusLabel.setText(
                            "Camera error: " + e.getMessage()
                    );
                });

            } finally {
                closeCamera();

                Platform.runLater(() -> {
                    startCameraButton.setDisable(false);
                    stopCameraButton.setDisable(true);
                    cameraPlaceholder.setVisible(true);
                    cameraPreview.setImage(null);
                });
            }
        });
    }

    private Timeline lateCheckOutTimer;

    private void updateLateCheckOutButton() {

        LocalTime malaysiaTime = LocalTime.now(
                ZoneId.of("Asia/Kuala_Lumpur")
        );

        boolean after10PM = !malaysiaTime.isBefore(
                LocalTime.of(22, 0)
        );

        lateCheckOutButton.setDisable(
                !after10PM || submitting.get() || readingQrImage
        );
    }

    private void startLateCheckOutTimer() {

        updateLateCheckOutButton();

        lateCheckOutTimer = new Timeline(
                new KeyFrame(
                        Duration.seconds(30),
                        event -> updateLateCheckOutButton()
                )
        );

        lateCheckOutTimer.setCycleCount(
                Timeline.INDEFINITE
        );

        lateCheckOutTimer.play();
    }
    // =====================================================
    // QR SCANNING LOOP
    // =====================================================

    private void scanLoop() throws Exception {

        MultiFormatReader reader = new MultiFormatReader();

        reader.setHints(java.util.Map.of(
            com.google.zxing.DecodeHintType.POSSIBLE_FORMATS,
            java.util.List.of(com.google.zxing.BarcodeFormat.QR_CODE),

            com.google.zxing.DecodeHintType.TRY_HARDER,
            Boolean.TRUE
        ));

        while (scanning.get()) {

            BufferedImage frame = webcam.getImage();

            if (frame == null) {
                Thread.sleep(100);
                continue;
            }

            Image preview = toFxImage(frame);

            Platform.runLater(() -> {
                if (scanning.get()) {
                    cameraPreview.setImage(preview);
                }
            });

            try {
                BinaryBitmap bitmap = new BinaryBitmap(
                        new HybridBinarizer(
                                new BufferedImageLuminanceSource(frame)
                        )
                );

                Result result = reader.decode(bitmap);

                if (result != null
                        && result.getText() != null
                        && !result.getText().isBlank()) {

                    String qrToken = result.getText().trim();

                    scanning.set(false);

                    Platform.runLater(() -> {
                        scanStatusLabel.setText(
                                "QR Code detected. Submitting attendance..."
                        );
                    });

                    submitAttendance(qrToken);
                    break;
                }

                } catch (NotFoundException ignored) {
                    // No QR code detected in this frame.
                } catch (com.google.zxing.ReaderException e) {
                    System.out.println(
                        "[QR SCANNER] Decode error: " + e.getMessage()
                    );
                }finally {
                reader.reset();
            }

            Thread.sleep(120);
        }
    }

    @FXML
    private void handleUploadQrImage() {
        if (scanning.get() || submitting.get() || readingQrImage) {
            scanStatusLabel.setText(
                    "Stop the camera or wait for the current operation."
            );
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Attendance QR Code");

        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "QR Images (*.png, *.jpg, *.jpeg)",
                        "*.png", "*.jpg", "*.jpeg"
                )
        );

        File file = chooser.showOpenDialog(
                uploadQrButton.getScene().getWindow()
        );

        if (file == null) {
            return;
        }

        User user = SessionManager.getCurrentUser();

        if (user == null) {
            showAttendanceError("Please sign in again.");
            return;
        }

        final String userId = user.getUserId();
        final var scene = uploadQrButton.getScene();

        readingQrImage = true;
        updateAttendanceButtons();

        cameraPreview.setImage(null);
        cameraPlaceholder.setText("Reading QR image...");
        cameraPlaceholder.setVisible(true);
        scanStatusLabel.setText("Reading QR image...");
        startQrAnimation();

        CompletableFuture.supplyAsync(() -> {
            try {
                BufferedImage image = ImageIO.read(file);

                if (image == null) {
                    throw new IllegalArgumentException(
                            "The selected file is not a readable image."
                    );
                }

                Image preview = toFxImage(image);

                Platform.runLater(() -> {
                    if (attendanceViewActive(scene, userId)
                            && readingQrImage) {
                        cameraPreview.setImage(preview);
                        cameraPlaceholder.setVisible(false);
                    }
                });

                MultiFormatReader reader = new MultiFormatReader();

                reader.setHints(java.util.Map.of(
                        com.google.zxing.DecodeHintType.POSSIBLE_FORMATS,
                        java.util.List.of(
                                com.google.zxing.BarcodeFormat.QR_CODE
                        ),
                        com.google.zxing.DecodeHintType.TRY_HARDER,
                        Boolean.TRUE
                ));

                BinaryBitmap bitmap = new BinaryBitmap(
                        new HybridBinarizer(
                                new BufferedImageLuminanceSource(image)
                        )
                );

                Result decoded = reader.decode(bitmap);

                if (decoded.getText() == null
                        || decoded.getText().isBlank()) {
                    throw new IllegalArgumentException(
                            "The QR Code contains no usable text."
                    );
                }

                return decoded.getText().trim();

            } catch (NotFoundException ex) {
                throw new java.util.concurrent.CompletionException(
                        new IllegalArgumentException(
                                "No QR Code was found. Upload a clear image "
                                + "containing the complete attendance QR Code."
                        )
                );
            } catch (Exception ex) {
                throw new java.util.concurrent.CompletionException(ex);
            }
        }).whenComplete((qrToken, error) ->
                Platform.runLater(() -> {
                    readingQrImage = false;

                    if (!attendanceViewActive(scene, userId)) {
                        stopQrAnimation();
                        return;
                    }

                    if (error != null) {
                        stopQrAnimation();
                        updateAttendanceButtons();
                        cameraPlaceholder.setText("QR image could not be read");
                        cameraPlaceholder.setVisible(
                                cameraPreview.getImage() == null
                        );

                        showAttendanceError(rootMessage(error));
                        return;
                    }

                    // Keep the animation running while the server validates
                    // the QR and records attendance.
                    scanStatusLabel.setText(
                            "QR detected. Validating and saving attendance..."
                    );

                    submitAttendance(qrToken);
                })
        );
    }

    // =====================================================
    // STOP CAMERA
    // =====================================================
    @FXML
    private void handleStopCamera() {
        scanning.set(false);
        scanStatusLabel.setText("Camera stopped.");
    }

    private void closeCamera() {
        if (webcam != null) {
            try {
                if (webcam.isOpen()) {
                    webcam.close();
                }
            } catch (Exception ignored) {
            }
            webcam = null;
        }
    }
    
    private void submitAttendance(String qrToken) {
        // Camera decoding currently calls this from a worker thread.
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> submitAttendance(qrToken));
            return;
        }

        recordAttendance(qrToken, false);
    }

    private void recordAttendance(
            String qrToken,
            boolean checkoutWithoutQr
    ) {
        if (readingQrImage
                || !submitting.compareAndSet(false, true)) {
            return;
        }

        String token = SessionManager.getAccessToken();
        User user = SessionManager.getCurrentUser();

        if (user == null || token == null || token.isBlank()) {
            submitting.set(false);
            stopQrAnimation();
            updateAttendanceButtons();
            showAttendanceError("Please sign in again.");
            return;
        }

        final String userId = user.getUserId();
        final var scene = scanStatusLabel.getScene();

        scanning.set(false);
        updateAttendanceButtons();

        scanStatusLabel.setText(
                checkoutWithoutQr
                        ? "Saving check-out..."
                        : "QR detected. Saving attendance..."
        );

        CompletableFuture.supplyAsync(() -> {
            try {
                AttendanceScanResult result =
                        checkoutWithoutQr
                                ? attendanceService.checkoutAttendance(token)
                                : attendanceService.scanAttendance(qrToken, token);

                if (result == null
                        || result.getAttendanceId() <= 0
                        || result.getCheckInTime() == null) {
                    throw new IllegalStateException(
                            "The server did not return a confirmed attendance "
                            + "record. Refresh Attendance History before retrying."
                    );
                }

                Integer overtime = null;

                if (result.getCheckOutTime() != null) {
                    try {
                        List<AttendanceRecord> records =
                                attendanceService.getAttendanceRecords(token);

                        for (AttendanceRecord record : records) {
                            if (record.getAttendanceId()
                                    == result.getAttendanceId()
                                    && record.getEmployeeId() != null
                                    && userId.equals(
                                            record.getEmployeeId().toString())
                                    && record.getCheckOutTime() != null) {
                                overtime = record.getOvertimeMinutes();
                                break;
                            }
                        }
                    } catch (Exception ignored) {
                        // Attendance was saved. Failure to load OT must not
                        // turn a successful checkout into a submission failure.
                    }
                }

                return new AttendanceUiResult(result, overtime);

            } catch (Exception ex) {
                throw new java.util.concurrent.CompletionException(ex);
            }
        }).whenComplete((saved, error) ->
                Platform.runLater(() -> {
                    submitting.set(false);
                    stopQrAnimation();

                    if (!attendanceViewActive(scene, userId)) {
                        return;
                    }

                    updateAttendanceButtons();

                    if (error != null) {
                        String message = rootMessage(error);

                        if (message != null
                                && message.toLowerCase(java.util.Locale.ROOT)
                                        .contains("timed out")) {
                            message = "The request timed out. Attendance may "
                                    + "already have been saved. Refresh "
                                    + "Attendance History before trying again.";
                        }

                        showAttendanceError(message);
                        return;
                    }

                    refreshAttendanceHistory();
                    showAttendanceResult(saved);
                })
        );
    }
    // =====================================================
    // ATTENDANCE HISTORY
    // =====================================================

    @FXML
    private void handleRefreshHistory() {
        refreshAttendanceHistory();
    }

    private void refreshAttendanceHistory() {

        String token = SessionManager.getAccessToken();
        User user = SessionManager.getCurrentUser();

        if (token == null || user == null) {
            scanStatusLabel.setText(
                    "Session missing. Please login again."
            );
            return;
        }

        CompletableFuture.supplyAsync(() -> {
            try {
                return attendanceService.getAttendanceRecords(token);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).whenComplete((records, error) -> {

            Platform.runLater(() -> {

                if (error != null) {
                    scanStatusLabel.setText(
                            "Unable to load attendance: "
                                    + rootMessage(error)
                    );
                    return;
                }

                attendanceTable.setItems(
                        FXCollections.observableArrayList(records)
                );

                updateTodayStatus(records);
            });
        });
    }

    private void updateTodayStatus(
            List<AttendanceRecord> records
    ) {

        java.time.LocalDate today =
                java.time.LocalDate.now(MALAYSIA_ZONE);

        checkInLabel.setText("--:--");
        checkOutLabel.setText("--:--");

        for (AttendanceRecord record : records) {

            if (today.equals(record.getAttendanceDate())) {

                checkInLabel.setText(
                        formatTime(record.getCheckInTime())
                );

                checkOutLabel.setText(
                        formatTime(record.getCheckOutTime())
                );

                break;
            }
        }
    }

    // =====================================================
    // IMAGE CONVERSION
    // =====================================================

    private Image toFxImage(BufferedImage bufferedImage)
            throws Exception {

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        ImageIO.write(bufferedImage, "png", output);

        return new Image(
                new ByteArrayInputStream(output.toByteArray())
        );
    }

    // =====================================================
    // HELPERS
    // =====================================================

    private String formatTime(OffsetDateTime time) {

        if (time == null) {
            return "--:--";
        }

        return TIME_FORMAT.format(
                time.atZoneSameInstant(MALAYSIA_ZONE)
        );
    }

    private String rootMessage(Throwable error) {

        Throwable cause = error;

        while (cause.getCause() != null) {
            cause = cause.getCause();
        }

        return cause.getMessage();
    }

    // =====================================================
    // NAVIGATION
    // =====================================================

    @FXML
    private void handleHome(ActionEvent event) {
        navigate(event, "/fxml/employee/employeeHome.fxml");
    }

    @FXML
    private void handleProfile(ActionEvent event) {
        navigate(event, "/fxml/employee/employeeProfile.fxml");
    }

    @FXML
    private void handleTasks(ActionEvent event) {

        navigate(
                event,
                "/fxml/employee/employeeTask.fxml"
        );
    }

    @FXML
    private void handleAttendance() {
        // Already on this page.
    }

    @FXML
    private void handleKpi(ActionEvent event) {

        navigate(
                event,
                "/fxml/employee/employeePerformance.fxml"
        );
    }

    @FXML
    private void handleReports() {

        System.out.println(
                "Employee Reports not implemented yet."
        );
    }

    @FXML
    private void handleAnonymousReport() {

        System.out.println(
                "Anonymous Reporting not implemented yet."
        );
    }

    @FXML
    private void handleLogout(ActionEvent event) {

        stopScanner();

        SessionManager.clearSession();

        navigate(event, "/fxml/auth/login.fxml");
    }

    private void navigate(ActionEvent event, String path) {

        stopScanner();

        Stage stage = (Stage) ((Node) event.getSource())
                .getScene()
                .getWindow();

        NavigationManager.navigate(stage, path);
    }

    private void stopScanner() {
        scanning.set(false);
        stopQrAnimation();
    }

    private void startQrAnimation() {
        stopQrAnimation();

        qrScanLine.setVisible(true);

        qrScanAnimation = new javafx.animation.TranslateTransition(
                Duration.seconds(1.2),
                qrScanLine
        );

        qrScanAnimation.setFromY(-110);
        qrScanAnimation.setToY(110);
        qrScanAnimation.setAutoReverse(true);
        qrScanAnimation.setCycleCount(
                javafx.animation.Animation.INDEFINITE
        );
        qrScanAnimation.play();
    }

    private void stopQrAnimation() {
        if (qrScanAnimation != null) {
            qrScanAnimation.stop();
            qrScanAnimation = null;
        }

        if (qrScanLine != null) {
            qrScanLine.setVisible(false);
            qrScanLine.setTranslateY(0);
        }
    }

    private boolean attendanceViewActive(
            javafx.scene.Scene scene,
            String userId
    ) {
        User current = SessionManager.getCurrentUser();

        return current != null
                && userId.equals(current.getUserId())
                && scene != null
                && scanStatusLabel.getScene() == scene
                && scene.getWindow() != null
                && scene.getWindow().isShowing()
                && scene.getWindow().getScene() == scene;
    }

    private void updateAttendanceButtons() {
        boolean busy = submitting.get() || readingQrImage;

        startCameraButton.setDisable(busy || scanning.get());
        uploadQrButton.setDisable(busy || scanning.get());
        stopCameraButton.setDisable(!scanning.get());

        updateLateCheckOutButton();
    }

    private void showAttendanceError(String message) {
        String title = "Attendance Not Confirmed";

        if (message == null || message.isBlank()) {
            message = "Attendance could not be confirmed. "
                    + "Refresh Attendance History before trying again.";
        }

        String lower = message.toLowerCase(java.util.Locale.ROOT);

        if (lower.contains("check-in is unavailable on a holiday")) {
            title = "Check-in Unavailable";
            message = "Today is configured as a holiday. "
                    + "Check-in is not available. "
                    + "Contact your administrator if this is incorrect.";

        } else if (lower.contains("not a configured working day")) {
            title = "Non-working Day";
            message = "Today is not configured as a working day. "
                    + "Attendance cannot be recorded.";

        } else if (lower.contains("invalid or expired qr")) {
            title = "Invalid QR Code";
            message = "This QR Code is invalid or has expired. "
                    + "Use the current attendance QR Code from Admin.";

        } else if (lower.contains("already checked out")) {
            title = "Already Checked Out";
            message = "Your check-out has already been recorded today.";

        } else if (lower.contains("already checked in")) {
            title = "Already Checked In";
            message = "Your check-in has already been recorded today.";

        } else if (lower.contains("no check-in record")) {
            title = "Check-in Required";
            message = "Please check in before checking out.";
        }

        scanStatusLabel.setText(message);

        com.aepm.ui.common.AEPMDialog.showError(
                scanStatusLabel.getScene().getWindow(),
                title,
                message
        );
    }

    private void showAttendanceResult(AttendanceUiResult saved) {
        AttendanceScanResult result = saved.result();

        boolean checkedOut = result.getCheckOutTime() != null;
        boolean late = "Late".equalsIgnoreCase(
                result.getAttendanceStatus()
        );

        String title;
        String message;

        if (checkedOut) {
            title = "Check-out Recorded";

            message = "Check-out: "
                    + formatTime(result.getCheckOutTime());

            Integer overtime = saved.overtimeMinutes();

            if (overtime == null) {
                message += "\nOT details are unavailable. "
                        + "Refresh Attendance History to check.";
            } else if (overtime > 0) {
                message += "\nRecorded overtime: "
                        + (overtime / 60) + " hr "
                        + (overtime % 60) + " min.";
            } else {
                message += "\nRecorded overtime: 0 minutes.";
            }
        } else {
            title = late
                    ? "Check-in Recorded — Late"
                    : "Check-in Recorded";

            message = "Check-in: "
                    + formatTime(result.getCheckInTime());

            String status = result.getAttendanceStatus();

            if (status != null && !status.isBlank()) {
                message += "\nAttendance status: " + status;
            }
        }

        scanStatusLabel.setText(title);

        com.aepm.ui.common.AEPMDialog.showSuccess(
                scanStatusLabel.getScene().getWindow(),
                title,
                message
        );
    }
}
