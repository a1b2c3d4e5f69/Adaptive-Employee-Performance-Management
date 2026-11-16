package com.aepm.logic.task;

import com.aepm.data.repository.ProgressVerificationConfigRepository;
import com.aepm.data.repository.ProgressVerificationConfigRepository.ProgressVerificationConfig;

import java.time.Duration;
import java.time.OffsetDateTime;


/**
 * Business logic for task progress verification.
 *
 * Flow:
 *
 * Employee submits progress + evidence
 *
 *          ↓
 *
 * Pending Manager Review
 *
 *          ↓
 *
 * Manager approves / rejects
 *
 *          OR
 *
 * Manager review timeout
 *
 *          ↓
 *
 * System Verification
 *
 *          ↓
 *
 * Valid evidence + reasonable progress increase
 *
 *          ↓
 *
 * Automatically verified
 */
public class ProgressVerificationService {

    private final ProgressVerificationConfigRepository configRepository;


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public ProgressVerificationService() {

        this.configRepository =
                new ProgressVerificationConfigRepository();
    }


    // =========================================================
    // GET CONFIG
    // =========================================================

    public ProgressVerificationConfig getConfig(
            String accessToken
    ) throws Exception {

        return configRepository.getConfig(
                accessToken
        );
    }


    // =========================================================
    // VALIDATE EMPLOYEE PROGRESS SUBMISSION
    // =========================================================

    public void validateSubmission(
            int currentProgress,
            int requestedProgress,
            int evidenceCount,
            String progressNote,
            String accessToken
    ) throws Exception {

        ProgressVerificationConfig config =
                configRepository.getConfig(
                        accessToken
                );


        // -----------------------------------------------------
        // Progress must be 0 - 100
        // -----------------------------------------------------

        if (
                requestedProgress < 0
                        || requestedProgress > 100
        ) {

            throw new IllegalArgumentException(
                    "Progress must be between 0% and 100%."
            );
        }


        // -----------------------------------------------------
        // Cannot reduce progress
        // -----------------------------------------------------

        if (requestedProgress < currentProgress) {

            throw new IllegalArgumentException(
                    "Progress cannot be lower than the current progress."
            );
        }


        // -----------------------------------------------------
        // No change
        // -----------------------------------------------------

        if (requestedProgress == currentProgress) {

            throw new IllegalArgumentException(
                    "The new progress must be higher than the current progress."
            );
        }

        // -----------------------------------------------------
        // Progress note
        // -----------------------------------------------------

        if (
                progressNote == null
                        || progressNote.trim().isEmpty()
        ) {

            throw new IllegalArgumentException(
                    "Please explain the work completed."
            );
        }
    }


    // =========================================================
    // CHECK MANAGER REVIEW TIMEOUT
    // =========================================================

    public boolean isManagerReviewExpired(
            OffsetDateTime submittedAt,
            String accessToken
    ) throws Exception {

        if (submittedAt == null) {

            return false;
        }


        ProgressVerificationConfig config =
                configRepository.getConfig(
                        accessToken
                );


        OffsetDateTime deadline =
                submittedAt.plusHours(
                        config.getReviewTimeoutHours()
                );


        return OffsetDateTime.now()
                .isAfter(deadline);
    }


    // =========================================================
    // GET MANAGER REVIEW DEADLINE
    // =========================================================

    public OffsetDateTime getManagerReviewDeadline(
            OffsetDateTime submittedAt,
            String accessToken
    ) throws Exception {

        if (submittedAt == null) {

            return null;
        }


        ProgressVerificationConfig config =
                configRepository.getConfig(
                        accessToken
                );


        return submittedAt.plusHours(
                config.getReviewTimeoutHours()
        );
    }


    // =========================================================
    // REMAINING MANAGER REVIEW HOURS
    // =========================================================

    public long getRemainingReviewHours(
            OffsetDateTime submittedAt,
            String accessToken
    ) throws Exception {

        if (submittedAt == null) {

            return 0;
        }


        OffsetDateTime deadline =
                getManagerReviewDeadline(
                        submittedAt,
                        accessToken
                );


        if (
                deadline == null
                        || OffsetDateTime.now().isAfter(deadline)
        ) {

            return 0;
        }


        return Duration
                .between(
                        OffsetDateTime.now(),
                        deadline
                )
                .toHours();
    }


    // =========================================================
    // SHOULD SYSTEM TAKE OVER?
    // =========================================================

    public boolean shouldSystemVerify(
            String verificationStatus,
            OffsetDateTime submittedAt,
            String accessToken
    ) throws Exception {

        ProgressVerificationConfig config =
                configRepository.getConfig(
                        accessToken
                );


        // Auto verification disabled
        if (!config.isAutoApprovalEnabled()) {

            return false;
        }


        // Only pending submissions can be taken over
        if (
                verificationStatus == null
                        || !verificationStatus.equalsIgnoreCase(
                                "Pending Manager Review"
                        )
        ) {

            return false;
        }


        return isManagerReviewExpired(
                submittedAt,
                accessToken
        );
    }


    // =========================================================
    // CALCULATE SYSTEM VERIFIED PROGRESS
    // =========================================================

    /**
     * System does NOT blindly accept whatever percentage
     * the employee enters.
     *
     * Example:
     *
     * Current progress = 20%
     * Employee requests = 70%
     * Max automatic increase = 25%
     *
     * System verified progress = 45%
     *
     * Manager can still manually approve 70%.
     */
    public int calculateSystemVerifiedProgress(
            int currentProgress,
            int requestedProgress,
            int evidenceCount,
            String accessToken
    ) throws Exception {

        ProgressVerificationConfig config =
                configRepository.getConfig(
                        accessToken
                );


        // -----------------------------------------------------
        // Invalid progress
        // -----------------------------------------------------

        if (
                requestedProgress < 0
                        || requestedProgress > 100
        ) {

            throw new IllegalArgumentException(
                    "Requested progress must be between 0 and 100."
            );
        }


        // -----------------------------------------------------
        // Cannot reduce progress
        // -----------------------------------------------------

        if (requestedProgress <= currentProgress) {

            return currentProgress;
        }


        // -----------------------------------------------------
        // Evidence is compulsory for automatic verification
        // -----------------------------------------------------

        if (evidenceCount <= 0) {

            return currentProgress;
        }


        int requestedIncrease =
                requestedProgress
                        - currentProgress;


        int allowedIncrease =
                Math.min(
                        requestedIncrease,
                        config.getMaxAutoProgressIncrease()
                );


        int verifiedProgress =
                currentProgress
                        + allowedIncrease;


        return Math.min(
                verifiedProgress,
                100
        );
    }


    // =========================================================
    // SYSTEM VERIFICATION RESULT
    // =========================================================

    public SystemVerificationResult verifyBySystem(
            int currentProgress,
            int requestedProgress,
            int evidenceCount,
            String accessToken
    ) throws Exception {

        ProgressVerificationConfig config =
                configRepository.getConfig(
                        accessToken
                );


        // -----------------------------------------------------
        // System verification disabled
        // -----------------------------------------------------

        if (!config.isAutoApprovalEnabled()) {

            return new SystemVerificationResult(
                    false,
                    currentProgress,
                    "System automatic verification is disabled."
            );
        }


        // -----------------------------------------------------
        // No evidence
        // -----------------------------------------------------

        if (evidenceCount <= 0) {

            return new SystemVerificationResult(
                    false,
                    currentProgress,
                    "Automatic verification failed because no supporting evidence was provided."
            );
        }


        // -----------------------------------------------------
        // Requested progress invalid
        // -----------------------------------------------------

        if (
                requestedProgress <= currentProgress
                        || requestedProgress > 100
        ) {

            return new SystemVerificationResult(
                    false,
                    currentProgress,
                    "The submitted progress value is invalid."
            );
        }


        int verifiedProgress =
                calculateSystemVerifiedProgress(
                        currentProgress,
                        requestedProgress,
                        evidenceCount,
                        accessToken
                );


        if (verifiedProgress <= currentProgress) {

            return new SystemVerificationResult(
                    false,
                    currentProgress,
                    "The system could not verify additional progress."
            );
        }


        // -----------------------------------------------------
        // Full requested progress verified
        // -----------------------------------------------------

        if (verifiedProgress == requestedProgress) {

            return new SystemVerificationResult(
                    true,
                    verifiedProgress,
                    "Evidence was detected and the submitted progress was within the automatic verification limit."
            );
        }


        // -----------------------------------------------------
        // Partial progress verified
        // -----------------------------------------------------

        return new SystemVerificationResult(
                true,
                verifiedProgress,
                "The submitted progress increase was higher than the automatic verification limit. "
                        + "The system verified progress up to "
                        + verifiedProgress
                        + "%."
        );
    }


    // =========================================================
    // RESULT CLASS
    // =========================================================

    public static class SystemVerificationResult {

        private final boolean approved;

        private final int verifiedProgress;

        private final String reason;


        public SystemVerificationResult(
                boolean approved,
                int verifiedProgress,
                String reason
        ) {

            this.approved =
                    approved;

            this.verifiedProgress =
                    verifiedProgress;

            this.reason =
                    reason;
        }


        public boolean isApproved() {
            return approved;
        }


        public int getVerifiedProgress() {
            return verifiedProgress;
        }


        public String getReason() {
            return reason;
        }


        @Override
        public String toString() {

            return "SystemVerificationResult{" +
                    "approved=" + approved +
                    ", verifiedProgress=" + verifiedProgress +
                    ", reason='" + reason + '\'' +
                    '}';
        }
    }
}