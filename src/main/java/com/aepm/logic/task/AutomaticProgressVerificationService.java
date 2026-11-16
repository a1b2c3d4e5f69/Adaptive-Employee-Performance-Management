package com.aepm.logic.task;

import com.aepm.config.AppConfig;
import com.aepm.data.repository.ProgressVerificationConfigRepository;
import com.aepm.data.repository.ProgressVerificationConfigRepository.ProgressVerificationConfig;
import com.aepm.data.supabase.SupabaseClient;
import com.aepm.data.task.ProgressVerificationRepository;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.util.List;
import java.util.UUID;


/**
 * Automatic progress verification engine.
 *
 * Trigger:
 * Manager review deadline has expired.
 *
 * The engine performs conservative automatic verification.
 *
 * It does NOT blindly trust the percentage entered by
 * the employee.
 */
public class AutomaticProgressVerificationService {

    private final ProgressVerificationRepository verificationRepository;

    private final ProgressVerificationConfigRepository configRepository;

    private final SupabaseClient supabaseClient;


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public AutomaticProgressVerificationService() {

        this.verificationRepository =
                new ProgressVerificationRepository();

        this.configRepository =
                new ProgressVerificationConfigRepository();

        this.supabaseClient =
                SupabaseClient.getInstance();
    }


    // =========================================================
    // PROCESS ALL EXPIRED SUBMISSIONS
    // =========================================================

    /**
     * Scans all expired Pending Manager Review submissions.
     *
     * Normally this method will later be called:
     *
     * - when Manager dashboard opens
     * - when Employee dashboard opens
     * - periodically by scheduler
     */
    public VerificationBatchResult processExpiredSubmissions(
            String accessToken
    ) throws Exception {

        ProgressVerificationConfig config =
                configRepository.getConfig(
                        accessToken
                );


        // Auto approval globally disabled
        if (!config.isAutoApprovalEnabled()) {

            return new VerificationBatchResult(
                    0,
                    0,
                    0,
                    0
            );
        }


        List<JSONObject> submissions =
                verificationRepository
                        .getExpiredPendingSubmissions(
                                accessToken
                        );


        int processed = 0;

        int approved = 0;

        int needsManagerReview = 0;

        int failed = 0;


        for (JSONObject submission : submissions) {

            try {

                VerificationDecision decision =
                        verifySubmission(
                                submission,
                                config,
                                accessToken
                        );


                processed++;


                if (
                        decision.getDecision()
                                == Decision.SYSTEM_APPROVED
                ) {

                    approved++;

                } else {

                    needsManagerReview++;
                }


            } catch (Exception e) {

                failed++;

                System.err.println(
                        "Automatic verification failed for submission: "
                                + submission.optString(
                                        "submission_id",
                                        "unknown"
                                )
                                + " - "
                                + e.getMessage()
                );
            }
        }


        return new VerificationBatchResult(
                processed,
                approved,
                needsManagerReview,
                failed
        );
    }


    // =========================================================
    // PROCESS ONE SUBMISSION
    // =========================================================

    public VerificationDecision processSubmission(
            UUID submissionId,
            String accessToken
    ) throws Exception {

        JSONObject submission =
                verificationRepository
                        .getSubmission(
                                submissionId,
                                accessToken
                        );


        if (submission == null) {

            throw new IllegalArgumentException(
                    "Progress submission was not found."
            );
        }


        ProgressVerificationConfig config =
                configRepository.getConfig(
                        accessToken
                );


        if (!config.isAutoApprovalEnabled()) {

            return new VerificationDecision(
                    Decision.NEEDS_MANAGER_REVIEW,
                    0,
                    0,
                    "Automatic progress verification is disabled."
            );
        }


        return verifySubmission(
                submission,
                config,
                accessToken
        );
    }


    // =========================================================
    // CORE VERIFICATION ENGINE
    // =========================================================

    private VerificationDecision verifySubmission(
            JSONObject submission,
            ProgressVerificationConfig config,
            String accessToken
    ) throws Exception {

        UUID submissionId =
                UUID.fromString(
                        submission.getString(
                                "submission_id"
                        )
                );


        UUID taskId =
                UUID.fromString(
                        submission.getString(
                                "task_id"
                        )
                );


        int proposedProgress =
                submission.getInt(
                        "proposed_progress"
                );


        String progressNote =
                submission.optString(
                        "progress_note",
                        ""
                );


        // =====================================================
        // 1. CURRENT VERIFIED TASK PROGRESS
        // =====================================================

        int currentProgress =
                getCurrentTaskProgress(
                        taskId,
                        accessToken
                );


        // =====================================================
        // 2. BASIC PROGRESS VALIDATION
        // =====================================================

        if (
                proposedProgress <= currentProgress
                        || proposedProgress > 100
        ) {

            String reason =
                    "The submitted progress is invalid. "
                            + "Current verified progress is "
                            + currentProgress
                            + "% while the employee submitted "
                            + proposedProgress
                            + "%.";


            verificationRepository
                    .markNeedsManagerReview(
                            submissionId,
                            0,
                            reason,
                            accessToken
                    );


            return new VerificationDecision(
                    Decision.NEEDS_MANAGER_REVIEW,
                    currentProgress,
                    0,
                    reason
            );
        }


        // =====================================================
        // 3. LOAD EVIDENCE
        // =====================================================

        EvidenceSummary evidence =
                loadEvidenceSummary(
                        submissionId,
                        accessToken
                );


        // =====================================================
        // 4. NO EVIDENCE → NEVER AUTO APPROVE
        // =====================================================

        if (evidence.getEvidenceCount() <= 0) {

            String reason =
                    "Automatic verification cannot approve this "
                            + "submission because no supporting "
                            + "evidence was uploaded.";


            verificationRepository
                    .markNeedsManagerReview(
                            submissionId,
                            0,
                            reason,
                            accessToken
                    );


            return new VerificationDecision(
                    Decision.NEEDS_MANAGER_REVIEW,
                    currentProgress,
                    0,
                    reason
            );
        }


        // =====================================================
        // 5. 100% COMPLETION PROTECTION
        // =====================================================

        if (
                proposedProgress >= 100
                        && !config
                        .isAllowAutoApproveCompletion()
        ) {

            double confidence =
                    calculateConfidence(
                            currentProgress,
                            proposedProgress,
                            progressNote,
                            evidence,
                            config
                    );


            String reason =
                    "The employee requested 100% completion. "
                            + "Automatic completion approval is disabled, "
                            + "therefore a manager must review the evidence.";


            verificationRepository
                    .markNeedsManagerReview(
                            submissionId,
                            confidence,
                            reason,
                            accessToken
                    );


            return new VerificationDecision(
                    Decision.NEEDS_MANAGER_REVIEW,
                    currentProgress,
                    confidence,
                    reason
            );
        }


        // =====================================================
        // 6. CALCULATE CONFIDENCE
        // =====================================================

        double confidence =
                calculateConfidence(
                        currentProgress,
                        proposedProgress,
                        progressNote,
                        evidence,
                        config
                );


        // =====================================================
        // 7. BELOW MINIMUM CONFIDENCE
        // =====================================================

        if (
                confidence
                        < config.getMinimumConfidence()
        ) {

            String reason =
                    "Automatic verification confidence was "
                            + String.format(
                                    "%.1f",
                                    confidence
                            )
                            + "%, which is below the configured "
                            + "minimum confidence of "
                            + String.format(
                                    "%.1f",
                                    config.getMinimumConfidence()
                            )
                            + "%.";


            verificationRepository
                    .markNeedsManagerReview(
                            submissionId,
                            confidence,
                            reason,
                            accessToken
                    );


            return new VerificationDecision(
                    Decision.NEEDS_MANAGER_REVIEW,
                    currentProgress,
                    confidence,
                    reason
            );
        }


        // =====================================================
        // 8. LIMIT AUTOMATIC PROGRESS INCREASE
        // =====================================================

        int requestedIncrease =
                proposedProgress
                        - currentProgress;


        int allowedIncrease =
                Math.min(
                        requestedIncrease,
                        config.getMaxAutoProgressIncrease()
                );


        int verifiedProgress =
                Math.min(
                        currentProgress
                                + allowedIncrease,
                        100
                );


        // =====================================================
        // 9. SAFETY CHECK
        // =====================================================

        if (verifiedProgress <= currentProgress) {

            String reason =
                    "The system could not verify any additional "
                            + "task progress.";


            verificationRepository
                    .markNeedsManagerReview(
                            submissionId,
                            confidence,
                            reason,
                            accessToken
                    );


            return new VerificationDecision(
                    Decision.NEEDS_MANAGER_REVIEW,
                    currentProgress,
                    confidence,
                    reason
            );
        }


        // =====================================================
        // 10. CREATE SYSTEM REASON
        // =====================================================

        String systemReason;


        if (verifiedProgress == proposedProgress) {

            systemReason =
                    "Automatic verification approved the submitted "
                            + "progress because supporting evidence "
                            + "was provided and the confidence score was "
                            + String.format(
                                    "%.1f",
                                    confidence
                            )
                            + "%.";

        } else {

            systemReason =
                    "The employee requested "
                            + proposedProgress
                            + "% progress. "
                            + "The automatic verification limit allows "
                            + "a maximum increase of "
                            + config.getMaxAutoProgressIncrease()
                            + "% per automatic review. "
                            + "Therefore the verified progress was limited to "
                            + verifiedProgress
                            + "%. Confidence: "
                            + String.format(
                                    "%.1f",
                                    confidence
                            )
                            + "%.";
        }


        // =====================================================
        // 11. APPROVE VERIFIED PROGRESS
        // =====================================================

        verificationRepository
                .approveBySystem(
                        submissionId,
                        verifiedProgress,
                        confidence,
                        systemReason,
                        accessToken
                );


        return new VerificationDecision(
                Decision.SYSTEM_APPROVED,
                verifiedProgress,
                confidence,
                systemReason
        );
    }


    // =========================================================
    // CONFIDENCE CALCULATION
    // =========================================================

    /**
     * Conservative rule-based confidence score.
     *
     * Important:
     *
     * This does NOT claim that the system understands whether
     * the PDF/image is truthful.
     *
     * It determines whether enough structured supporting
     * information exists to safely allow LIMITED automatic
     * progress.
     *
     * Maximum = 100.
     */
    private double calculateConfidence(
            int currentProgress,
            int proposedProgress,
            String progressNote,
            EvidenceSummary evidence,
            ProgressVerificationConfig config
    ) {

        double score = 0;


        // =====================================================
        // A. Evidence exists
        // Maximum 35 points
        // =====================================================

        if (evidence.getEvidenceCount() >= 1) {

            score += 25;
        }


        if (evidence.getEvidenceCount() >= 2) {

            score += 5;
        }


        if (evidence.getEvidenceCount() >= 3) {

            score += 5;
        }


        // =====================================================
        // B. Evidence has actual file content
        // Maximum 15 points
        // =====================================================

        if (evidence.getValidFileCount() > 0) {

            score += 10;
        }


        if (
                evidence.getValidFileCount()
                        == evidence.getEvidenceCount()
        ) {

            score += 5;
        }


        // =====================================================
        // C. Progress explanation quality
        // Maximum 25 points
        // =====================================================

        if (
                progressNote != null
                        && !progressNote.isBlank()
        ) {

            String trimmed =
                    progressNote.trim();


            if (trimmed.length() >= 20) {

                score += 10;
            }


            if (trimmed.length() >= 50) {

                score += 10;
            }


            if (trimmed.length() >= 100) {

                score += 5;
            }
        }


        // =====================================================
        // D. Requested increase reasonableness
        // Maximum 25 points
        // =====================================================

        int increase =
                proposedProgress
                        - currentProgress;


        if (
                increase
                        <= config.getMaxAutoProgressIncrease()
        ) {

            score += 25;

        } else if (
                increase
                        <= config.getMaxAutoProgressIncrease() * 2
        ) {

            score += 15;

        } else {

            score += 5;
        }


        // =====================================================
        // Final bounds
        // =====================================================

        return Math.max(
                0,
                Math.min(
                        score,
                        100
                )
        );
    }


    // =========================================================
    // CURRENT TASK PROGRESS
    // =========================================================

    private int getCurrentTaskProgress(
            UUID taskId,
            String accessToken
    ) throws Exception {

        String url =
                AppConfig.REST_URL
                        + "/task_tbl"
                        + "?select=progress"
                        + "&task_id=eq."
                        + taskId
                        + "&limit=1";


        HttpRequest request =
                supabaseClient
                        .createAuthenticatedRequest(
                                url,
                                accessToken
                        )
                        .GET()
                        .build();


        HttpResponse<String> response =
                send(request);


        validate(
                response,
                "Unable to load current task progress"
        );


        JSONArray array =
                new JSONArray(
                        response.body()
                );


        if (array.isEmpty()) {

            throw new IllegalArgumentException(
                    "Task was not found."
            );
        }


        return array
                .getJSONObject(0)
                .optInt(
                        "progress",
                        0
                );
    }


    // =========================================================
    // LOAD EVIDENCE SUMMARY
    // =========================================================

    private EvidenceSummary loadEvidenceSummary(
            UUID submissionId,
            String accessToken
    ) throws Exception {

        String url =
                AppConfig.REST_URL
                        + "/task_progress_evidence_tbl"
                        + "?select="
                        + "evidence_id,"
                        + "file_name,"
                        + "file_path,"
                        + "file_type,"
                        + "file_size"
                        + "&submission_id=eq."
                        + submissionId;


        HttpRequest request =
                supabaseClient
                        .createAuthenticatedRequest(
                                url,
                                accessToken
                        )
                        .GET()
                        .build();


        HttpResponse<String> response =
                send(request);


        validate(
                response,
                "Unable to load progress evidence"
        );


        JSONArray array =
                new JSONArray(
                        response.body()
                );


        int evidenceCount =
                array.length();


        int validFileCount = 0;

        long totalFileSize = 0;


        for (
                int i = 0;
                i < array.length();
                i++
        ) {

            JSONObject evidence =
                    array.getJSONObject(i);


            String fileName =
                    evidence.optString(
                            "file_name",
                            ""
                    );


            String filePath =
                    evidence.optString(
                            "file_path",
                            ""
                    );


            long fileSize =
                    evidence.optLong(
                            "file_size",
                            0
                    );


            boolean validFile =
                    !fileName.isBlank()
                            && !filePath.isBlank()
                            && fileSize > 0;


            if (validFile) {

                validFileCount++;
            }


            totalFileSize +=
                    Math.max(
                            fileSize,
                            0
                    );
        }


        return new EvidenceSummary(
                evidenceCount,
                validFileCount,
                totalFileSize
        );
    }


    // =========================================================
    // HTTP SEND
    // =========================================================

    private HttpResponse<String> send(
            HttpRequest request
    ) throws Exception {

        return supabaseClient
                .getHttpClient()
                .send(
                        request,
                        HttpResponse
                                .BodyHandlers
                                .ofString()
                );
    }


    // =========================================================
    // HTTP VALIDATION
    // =========================================================

    private void validate(
            HttpResponse<String> response,
            String message
    ) {

        if (
                response.statusCode() < 200
                        || response.statusCode() >= 300
        ) {

            throw new RuntimeException(
                    message
                            + " [HTTP "
                            + response.statusCode()
                            + "]: "
                            + response.body()
            );
        }
    }


    // =========================================================
    // DECISION ENUM
    // =========================================================

    public enum Decision {

        SYSTEM_APPROVED,

        NEEDS_MANAGER_REVIEW
    }


    // =========================================================
    // VERIFICATION RESULT
    // =========================================================

    public static class VerificationDecision {

        private final Decision decision;

        private final int verifiedProgress;

        private final double confidence;

        private final String reason;


        public VerificationDecision(
                Decision decision,
                int verifiedProgress,
                double confidence,
                String reason
        ) {

            this.decision =
                    decision;

            this.verifiedProgress =
                    verifiedProgress;

            this.confidence =
                    confidence;

            this.reason =
                    reason;
        }


        public Decision getDecision() {
            return decision;
        }


        public int getVerifiedProgress() {
            return verifiedProgress;
        }


        public double getConfidence() {
            return confidence;
        }


        public String getReason() {
            return reason;
        }


        @Override
        public String toString() {

            return "VerificationDecision{" +
                    "decision=" + decision +
                    ", verifiedProgress=" + verifiedProgress +
                    ", confidence=" + confidence +
                    ", reason='" + reason + '\'' +
                    '}';
        }
    }


    // =========================================================
    // EVIDENCE SUMMARY
    // =========================================================

    private static class EvidenceSummary {

        private final int evidenceCount;

        private final int validFileCount;

        private final long totalFileSize;


        public EvidenceSummary(
                int evidenceCount,
                int validFileCount,
                long totalFileSize
        ) {

            this.evidenceCount =
                    evidenceCount;

            this.validFileCount =
                    validFileCount;

            this.totalFileSize =
                    totalFileSize;
        }


        public int getEvidenceCount() {
            return evidenceCount;
        }


        public int getValidFileCount() {
            return validFileCount;
        }


        public long getTotalFileSize() {
            return totalFileSize;
        }
    }


    // =========================================================
    // BATCH RESULT
    // =========================================================

    public static class VerificationBatchResult {

        private final int processed;

        private final int systemApproved;

        private final int needsManagerReview;

        private final int failed;


        public VerificationBatchResult(
                int processed,
                int systemApproved,
                int needsManagerReview,
                int failed
        ) {

            this.processed =
                    processed;

            this.systemApproved =
                    systemApproved;

            this.needsManagerReview =
                    needsManagerReview;

            this.failed =
                    failed;
        }


        public int getProcessed() {
            return processed;
        }


        public int getSystemApproved() {
            return systemApproved;
        }


        public int getNeedsManagerReview() {
            return needsManagerReview;
        }


        public int getFailed() {
            return failed;
        }


        @Override
        public String toString() {

            return "VerificationBatchResult{" +
                    "processed=" + processed +
                    ", systemApproved=" + systemApproved +
                    ", needsManagerReview=" + needsManagerReview +
                    ", failed=" + failed +
                    '}';
        }
    }
}