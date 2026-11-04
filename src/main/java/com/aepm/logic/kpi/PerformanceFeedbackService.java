package com.aepm.logic.kpi;

import com.aepm.data.kpi.PerformanceFeedbackRepository;

import com.aepm.model.PerformanceFeedback;
import com.aepm.model.User;

import com.aepm.util.SessionManager;

import java.util.List;

public class PerformanceFeedbackService {

    private final PerformanceFeedbackRepository repository =
            new PerformanceFeedbackRepository();


    // =========================================================
    // SUBMIT FEEDBACK
    // =========================================================

    public PerformanceFeedback submitFeedback(
            Long resultId,
            String employeeId,
            String feedbackText
    ) throws Exception {

        // =====================================================
        // VALIDATE RESULT
        // =====================================================

        if (resultId == null) {

            throw new IllegalArgumentException(
                    "Performance result is required."
            );
        }


        // =====================================================
        // VALIDATE EMPLOYEE
        // =====================================================

        if (
                employeeId == null
                        || employeeId.isBlank()
        ) {

            throw new IllegalArgumentException(
                    "Employee is required."
            );
        }


        // =====================================================
        // VALIDATE FEEDBACK
        // =====================================================

        if (
                feedbackText == null
                        || feedbackText.trim().isBlank()
        ) {

            throw new IllegalArgumentException(
                    "Please enter performance feedback before submitting."
            );
        }


        // =====================================================
        // CURRENT MANAGER
        // =====================================================

        User manager =
                SessionManager.getCurrentUser();


        if (
                manager == null
                        || manager.getUserId() == null
                        || manager.getUserId().isBlank()
        ) {

            throw new IllegalStateException(
                    "Manager session is unavailable."
            );
        }


        String managerId =
                manager.getUserId();


        String accessToken =
                requireAccessToken();


        // =====================================================
        // PREVENT DUPLICATE FEEDBACK FOR SAME RESULT/MANAGER
        // =====================================================

        boolean alreadyExists =
                repository
                        .hasFeedbackForResult(
                                resultId,
                                managerId,
                                accessToken
                        );


        if (alreadyExists) {

            throw new IllegalStateException(
                    "Feedback has already been submitted for this performance result."
            );
        }


        // =====================================================
        // CREATE
        // =====================================================

        PerformanceFeedback feedback =
                new PerformanceFeedback();


        feedback.setResultId(
                resultId
        );


        feedback.setEmployeeId(
                employeeId
        );


        feedback.setManagerId(
                managerId
        );


        feedback.setFeedbackText(
                feedbackText.trim()
        );


        return repository
                .createFeedback(
                        feedback,
                        accessToken
                );
    }


    // =========================================================
    // RESULT FEEDBACK
    // =========================================================

    public List<PerformanceFeedback> getFeedbackByResult(
            Long resultId
    ) throws Exception {

        return repository
                .getFeedbackByResult(
                        resultId,
                        requireAccessToken()
                );
    }


    // =========================================================
    // EMPLOYEE FEEDBACK HISTORY
    // =========================================================

    public List<PerformanceFeedback> getFeedbackByEmployee(
            String employeeId
    ) throws Exception {

        return repository
                .getFeedbackByEmployee(
                        employeeId,
                        requireAccessToken()
                );
    }


    // =========================================================
    // LATEST FEEDBACK
    // =========================================================

    public PerformanceFeedback getLatestFeedbackByResult(
            Long resultId
    ) throws Exception {

        return repository
                .getLatestFeedbackByResult(
                        resultId,
                        requireAccessToken()
                );
    }


    // =========================================================
    // CHECK FEEDBACK
    // =========================================================

    public boolean currentManagerHasFeedbackForResult(
            Long resultId
    ) throws Exception {

        User manager =
                SessionManager.getCurrentUser();


        if (
                manager == null
                        || manager.getUserId() == null
                        || manager.getUserId().isBlank()
        ) {

            throw new IllegalStateException(
                    "Manager session is unavailable."
            );
        }


        return repository
                .hasFeedbackForResult(
                        resultId,
                        manager.getUserId(),
                        requireAccessToken()
                );
    }


    // =========================================================
    // TOKEN
    // =========================================================

    private String requireAccessToken() {

        String token =
                SessionManager.getAccessToken();


        if (
                token == null
                        || token.isBlank()
        ) {

            throw new IllegalStateException(
                    "User session has expired."
            );
        }


        return token;
    }

    public PerformanceFeedback updateFeedback(
                PerformanceFeedback existing,
                String newText
        ) throws Exception {

        User manager = SessionManager.getCurrentUser();

        if (manager == null
                || manager.getRoleId() != 2
                || manager.getUserId() == null) {
                throw new SecurityException(
                        "A manager account is required."
                );
        }

        if (existing == null
                || existing.getFeedbackId() == null
                || existing.getResultId() == null) {
                throw new IllegalArgumentException(
                        "Select an existing feedback record."
                );
        }

        if (!manager.getUserId().equals(existing.getManagerId())) {
                throw new SecurityException(
                        "You can only edit feedback you submitted."
                );
        }

        if (newText == null || newText.isBlank()) {
                throw new IllegalArgumentException(
                        "Feedback cannot be empty."
                );
        }

        return repository.updateFeedback(
                existing,
                newText.trim(),
                requireAccessToken()
        );
     }
}