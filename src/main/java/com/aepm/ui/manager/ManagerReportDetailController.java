package com.aepm.ui.manager;

import com.aepm.logic.kpi.PerformanceFeedbackService;

import com.aepm.model.KpiResult;
import com.aepm.model.PerformanceFeedback;
import com.aepm.model.User;

import javafx.fxml.FXML;

import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class ManagerReportDetailController {

    // =========================================================
    // HEADER
    // =========================================================

    @FXML
    private Label employeeHeaderLabel;


    // =========================================================
    // INFORMATION
    // =========================================================

    @FXML
    private Label employeeLabel;

    @FXML
    private Label departmentLabel;

    @FXML
    private Label periodLabel;

    @FXML
    private Label overallLabel;

    @FXML
    private Label statusLabel;


    // =========================================================
    // KPI
    // =========================================================

    @FXML
    private Label productivityLabel;

    @FXML
    private Label timelinessLabel;

    @FXML
    private Label completionLabel;

    @FXML
    private Label attendanceLabel;


    @FXML
    private ProgressBar productivityProgressBar;

    @FXML
    private ProgressBar timelinessProgressBar;

    @FXML
    private ProgressBar completionProgressBar;

    @FXML
    private ProgressBar attendanceProgressBar;


    // =========================================================
    // FEEDBACK
    // =========================================================

    @FXML
    private Label feedbackLabel;

    @FXML
    private Label feedbackDateLabel;


    // =========================================================
    // SERVICE
    // =========================================================

    private final PerformanceFeedbackService feedbackService =
            new PerformanceFeedbackService();


    // =========================================================
    // DATA
    // =========================================================

    private Stage stage;


    private static final ZoneId MALAYSIA_ZONE =
            ZoneId.of(
                    "Asia/Kuala_Lumpur"
            );


    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern(
                    "dd MMM yyyy"
            );


    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern(
                    "dd MMM yyyy HH:mm"
            );


    // =========================================================
    // SET REPORT DATA
    // =========================================================

    public void setReportData(
            User employee,
            KpiResult result,
            Stage stage
    ) {

        this.stage =
                stage;


        if (
                employee == null
                        || result == null
        ) {

            return;
        }


        String employeeDisplay =
                formatEmployee(
                        employee
                );


        employeeHeaderLabel.setText(
                employeeDisplay
        );


        employeeLabel.setText(
                employeeDisplay
        );


        departmentLabel.setText(
                safe(
                        employee.getDepartment()
                )
        );


        periodLabel.setText(
                formatPeriod(
                        result.getEvaluationStartDate(),
                        result.getEvaluationEndDate()
                )
        );


        overallLabel.setText(
                String.format(
                        "%.2f%%",
                        result.getOverallKpiScore()
                )
        );


        statusLabel.setText(
                safe(
                        result.getPerformanceStatus()
                )
        );


        updateStatusStyle(
                result.getPerformanceStatus()
        );


        productivityLabel.setText(
                String.format(
                        "%.2f%%",
                        result.getProductivityScore()
                )
        );


        timelinessLabel.setText(
                String.format(
                        "%.2f%%",
                        result.getTimelinessScore()
                )
        );


        completionLabel.setText(
                String.format(
                        "%.2f%%",
                        result.getCompletionRate()
                )
        );


        attendanceLabel.setText(
                String.format(
                        "%.2f%%",
                        result.getAttendanceScore()
                )
        );


        productivityProgressBar.setProgress(
                toProgress(
                        result.getProductivityScore()
                )
        );


        timelinessProgressBar.setProgress(
                toProgress(
                        result.getTimelinessScore()
                )
        );


        completionProgressBar.setProgress(
                toProgress(
                        result.getCompletionRate()
                )
        );


        attendanceProgressBar.setProgress(
                toProgress(
                        result.getAttendanceScore()
                )
        );


        loadFeedback(
                result
        );
    }


    // =========================================================
    // LOAD FEEDBACK
    // =========================================================

    private void loadFeedback(
            KpiResult result
    ) {

        feedbackLabel.setText(
                "Loading feedback..."
        );


        feedbackDateLabel.setText(
                ""
        );


        if (
                result == null
                        || result.getResultId() == null
        ) {

            feedbackLabel.setText(
                    "No manager feedback is available."
            );

            return;
        }


        javafx.concurrent.Task<PerformanceFeedback> task =
                new javafx.concurrent.Task<>() {

                    @Override
                    protected PerformanceFeedback call()
                            throws Exception {

                        return feedbackService
                                .getLatestFeedbackByResult(
                                        result.getResultId()
                                );
                    }
                };


        task.setOnSucceeded(
                event -> {

                    PerformanceFeedback feedback =
                            task.getValue();


                    if (feedback == null) {

                        feedbackLabel.setText(
                                "No manager feedback has been submitted for this evaluation."
                        );


                        feedbackDateLabel.setText(
                                ""
                        );


                        return;
                    }


                    String text =
                            feedback.getFeedbackText();


                    if (
                            text == null
                                    || text.isBlank()
                    ) {

                        text =
                                "No feedback available.";
                    }


                    feedbackLabel.setText(
                            text
                    );


                    OffsetDateTime createdAt =
                            feedback.getCreatedAt();


                    if (createdAt != null) {

                        feedbackDateLabel.setText(
                                "Submitted: "
                                        + createdAt
                                        .atZoneSameInstant(
                                                MALAYSIA_ZONE
                                        )
                                        .format(
                                                DATE_TIME_FORMATTER
                                        )
                        );

                    } else {

                        feedbackDateLabel.setText(
                                ""
                        );
                    }
                }
        );


        task.setOnFailed(
                event -> {

                    Throwable error =
                            task.getException();


                    if (error != null) {

                        error.printStackTrace();
                    }


                    feedbackLabel.setText(
                            "Unable to load manager feedback."
                    );


                    feedbackDateLabel.setText(
                            ""
                    );
                }
        );


        Thread thread =
                new Thread(
                        task
                );


        thread.setDaemon(
                true
        );


        thread.start();
    }


    // =========================================================
    // STATUS STYLE
    // =========================================================

    private void updateStatusStyle(
            String status
    ) {

        statusLabel
                .getStyleClass()
                .removeAll(
                        "status-at-risk",
                        "status-normal",
                        "status-good"
                );


        if (
                "At Risk".equalsIgnoreCase(
                        status
                )
        ) {

            statusLabel
                    .getStyleClass()
                    .add(
                            "status-at-risk"
                    );

        } else if (
                "Normal".equalsIgnoreCase(
                        status
                )
        ) {

            statusLabel
                    .getStyleClass()
                    .add(
                            "status-normal"
                    );

        } else {

            statusLabel
                    .getStyleClass()
                    .add(
                            "status-good"
                    );
        }
    }


    // =========================================================
    // CLOSE
    // =========================================================

    @FXML
    private void handleClose() {

        if (stage != null) {

            stage.close();
        }
    }


    // =========================================================
    // HELPERS
    // =========================================================

    private String formatEmployee(
            User employee
    ) {

        String employeeId =
                employee.getEmployeeId();


        if (
                employeeId == null
                        || employeeId.isBlank()
        ) {

            employeeId =
                    "-";
        }


        String name =
                employee.getFullName();


        if (
                name == null
                        || name.isBlank()
        ) {

            name =
                    employee.getUsername();
        }


        if (
                name == null
                        || name.isBlank()
        ) {

            name =
                    "Employee";
        }


        return employeeId
                + " - "
                + name;
    }


    private String formatPeriod(
            LocalDate start,
            LocalDate end
    ) {

        if (
                start == null
                        || end == null
        ) {

            return "-";
        }


        return start.format(
                DATE_FORMATTER
        )
                + " - "
                + end.format(
                DATE_FORMATTER
        );
    }


    private double toProgress(
            double percentage
    ) {

        return Math.max(
                0,
                Math.min(
                        1,
                        percentage / 100.0
                )
        );
    }


    private String safe(
            String value
    ) {

        return value == null
                || value.isBlank()
                ? "-"
                : value;
    }
}