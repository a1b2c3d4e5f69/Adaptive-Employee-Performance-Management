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

        if (scanning.get() || submitting.get()) {
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

        lateCheckOutButton.setDisable(!after10PM);
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

                System.out.println(
                    "[QR SCANNER] QR detected: " + result.getText()
                );

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

        if (scanning.get() || submitting.get()) {
            scanStatusLabel.setText(
                    "Please stop the camera or wait for attendance submission."
            );
            return;
        }

        FileChooser fileChooser = new FileChooser();

        fileChooser.setTitle("Select Attendance QR Code");

        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Image Files (*.png, *.jpg, *.jpeg)",
                        "*.png", "*.jpg", "*.jpeg"
                )
        );

        Stage stage = (Stage) uploadQrButton
                .getScene()
                .getWindow();

        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile == null) {
            return;
        }

        uploadQrButton.setDisable(true);

        scanStatusLabel.setText(
                "Reading QR Code from image..."
        );

        CompletableFuture.supplyAsync(() -> {

            try {

                BufferedImage image = ImageIO.read(selectedFile);

                if (image == null) {
                    throw new IllegalArgumentException(
                            "Unable to read the selected image."
                    );
                }

                BinaryBitmap bitmap = new BinaryBitmap(
                        new HybridBinarizer(
                                new BufferedImageLuminanceSource(image)
                        )
                );

                MultiFormatReader reader = new MultiFormatReader();

                reader.setHints(java.util.Map.of(
                        com.google.zxing.DecodeHintType.POSSIBLE_FORMATS,
                        java.util.List.of(
                                com.google.zxing.BarcodeFormat.QR_CODE
                        ),
                        com.google.zxing.DecodeHintType.TRY_HARDER,
                        Boolean.TRUE
                ));

                Result result = reader.decode(bitmap);

                if (result == null ||
                        result.getText() == null ||
                        result.getText().isBlank()) {

                    throw new IllegalArgumentException(
                            "No valid QR Code found in the image."
                    );
                }

                return result.getText().trim();

            } catch (Exception e) {

                throw new RuntimeException(e);

            }

        }).whenComplete((qrToken, error) -> {

            Platform.runLater(() -> {

                uploadQrButton.setDisable(false);

                if (error != null) {

                    scanStatusLabel.setText(
                            "Unable to read QR Code: "
                                    + rootMessage(error)
                    );

                    return;
                }

                scanStatusLabel.setText(
                        "QR Code detected. Submitting attendance..."
                );

                submitAttendance(qrToken);

            });

        });
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

    // =====================================================
    // SUBMIT CHECK IN / CHECK OUT
    // =====================================================

    private void submitAttendance(String qrToken) {

        if (!submitting.compareAndSet(false, true)) {
            return;
        }

        String token = SessionManager.getAccessToken();

        if (token == null || token.isBlank()) {
            submitting.set(false);

            Platform.runLater(() ->
                    scanStatusLabel.setText(
                            "Session missing. Please login again."
                    )
            );
            return;
        }

        CompletableFuture.supplyAsync(() -> {
            try {
                return attendanceService.scanAttendance(
                        qrToken,
                        token
                );
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).whenComplete((result, error) -> {

            submitting.set(false);

            Platform.runLater(() -> {

                if (error != null) {

                    String message = rootMessage(error);

                    if (message.contains("Already checked out today")) {
                        scanStatusLabel.setText("Already checked out today.");

                    } else if (message.contains("Already checked in")) {
                        scanStatusLabel.setText("Already checked in today.");

                    } else if (message.contains("Invalid or expired QR Code")) {
                        scanStatusLabel.setText("QR Code is invalid or expired.");

                    } else if (message.contains("Today is not a configured working day")) {
                        scanStatusLabel.setText("Today is not a working day.");

                    } else {
                        scanStatusLabel.setText("Unable to record attendance. Please try again.");
                    }

                    return;
                }

                scanStatusLabel.setText(
                        "Attendance successful: "
                                + result.getActionType()
                );

                refreshAttendanceHistory();
            });
        });
    }

    
    // =====================================================
    // LATE CHECK OUT - AFTER 10 PM
    // =====================================================

    @FXML
    private void handleLateCheckOut(ActionEvent event) {

        if (!submitting.compareAndSet(false, true)) {
            return;
        }

        String token = SessionManager.getAccessToken();

        if (token == null || token.isBlank()) {
            submitting.set(false);
            scanStatusLabel.setText(
                    "Session missing. Please login again."
            );
            return;
        }

        // Stop camera before submitting check out.
        stopScanner();

        lateCheckOutButton.setDisable(true);
        scanStatusLabel.setText(
                "Submitting late check out..."
        );

        CompletableFuture.supplyAsync(() -> {

            try {
                return attendanceService.checkoutAttendance(token);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }).whenComplete((result, error) -> {

            Platform.runLater(() -> {

                submitting.set(false);
                lateCheckOutButton.setDisable(false);

                if (error != null) {

                    String message = rootMessage(error);

                    if (message != null
                            && message.contains("Already checked out today")) {

                        scanStatusLabel.setText(
                                "Already checked out today."
                        );

                    } else if (message != null
                            && message.contains("No check-in record found")) {

                        scanStatusLabel.setText(
                                "Please check in before checking out."
                        );

                    } else if (message != null
                            && message.contains("before 10 PM")) {

                        scanStatusLabel.setText(
                                "Please scan the QR Code to check out before 10 PM."
                        );

                    } else {

                        scanStatusLabel.setText(
                                "Unable to check out. Please try again."
                        );
                    }

                    return;
                }

                scanStatusLabel.setText(
                        "Check Out successful!"
                );

                refreshAttendanceHistory();
            });
        });
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
    }
}
