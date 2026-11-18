package com.aepm.logic.task;

import com.aepm.config.AppConfig;
import com.aepm.data.repository.NotificationRepository;
import com.aepm.data.repository.ProgressVerificationConfigRepository;
import com.aepm.data.repository.ProgressVerificationConfigRepository.ProgressVerificationConfig;
import com.aepm.data.supabase.SupabaseClient;
import com.aepm.model.User;
import com.aepm.util.SessionManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.nio.file.Files;

import java.time.Duration;
import java.time.OffsetDateTime;

import java.util.UUID;


public class EmployeeProgressSubmissionService {


    // =========================================================
    // STORAGE
    // =========================================================

    private static final String EVIDENCE_BUCKET =
            "task-progress-evidence";


    private static final long MAX_FILE_SIZE =
            10L * 1024L * 1024L;


    // =========================================================
    // REPOSITORIES / CLIENT
    // =========================================================

    private final SupabaseClient supabaseClient =
            SupabaseClient.getInstance();


    private final ProgressVerificationConfigRepository configRepository =
            new ProgressVerificationConfigRepository();


    private final NotificationRepository notificationRepository =
            new NotificationRepository();


    // =========================================================
    // SUBMIT PROGRESS
    // =========================================================

    public SubmissionResult submitProgress(
            UUID taskId,
            int proposedProgress,
            String progressNote,
            File evidenceFile
    ) throws Exception {


        if (taskId == null) {

            throw new IllegalArgumentException(
                    "Task ID is required."
            );
        }


        User currentUser =
                SessionManager.getCurrentUser();


        if (
                currentUser == null
                        || currentUser.getUserId() == null
                        || currentUser.getUserId().isBlank()
        ) {

            throw new IllegalStateException(
                    "No logged-in employee found."
            );
        }


        UUID employeeId =
                UUID.fromString(
                        currentUser.getUserId()
                );


        String accessToken =
                getAccessToken();


        // =====================================================
        // 1. LOAD CURRENT VERIFIED PROGRESS
        // =====================================================

        int currentProgress =
                getCurrentTaskProgress(
                        taskId,
                        accessToken
                );


        // =====================================================
        // 2. VALIDATE PROGRESS
        // =====================================================

        if (
                proposedProgress < 0
                        || proposedProgress > 100
        ) {

            throw new IllegalArgumentException(
                    "Progress must be between 0% and 100%."
            );
        }


        if (
                proposedProgress
                        <= currentProgress
        ) {

            throw new IllegalArgumentException(
                    "Requested progress must be higher than "
                            + "the current verified progress of "
                            + currentProgress
                            + "%."
            );
        }


        // =====================================================
        // 3. VALIDATE NOTE
        // =====================================================

        if (
                progressNote == null
                        || progressNote.trim().isEmpty()
        ) {

            throw new IllegalArgumentException(
                    "Please enter a progress note."
            );
        }


        if (
                progressNote.trim().length() < 20
        ) {

            throw new IllegalArgumentException(
                    "Please provide a more detailed progress note."
            );
        }


        // =====================================================
        // 4. VALIDATE EVIDENCE
        // =====================================================

        if (evidenceFile == null) {

            throw new IllegalArgumentException(
                    "Please upload supporting evidence."
            );
        }


        validateEvidenceFile(
                evidenceFile
        );


        // =====================================================
        // 5. CHECK EXISTING PENDING SUBMISSION
        // =====================================================

        if (
                hasPendingSubmission(
                        taskId,
                        employeeId,
                        accessToken
                )
        ) {

            throw new IllegalStateException(
                    "You already have a progress submission "
                            + "waiting for verification."
            );
        }


        // =====================================================
        // 6. LOAD VERIFICATION CONFIG
        // =====================================================

        ProgressVerificationConfig config =
                configRepository.getConfig(
                        accessToken
                );


        OffsetDateTime submittedAt =
                OffsetDateTime.now();


        OffsetDateTime verificationDeadline =
                submittedAt.plusHours(
                        config.getReviewTimeoutHours()
                );


        // =====================================================
        // 7. CREATE SUBMISSION
        // =====================================================

        UUID submissionId =
                createSubmission(
                        taskId,
                        employeeId,
                        proposedProgress,
                        progressNote.trim(),
                        submittedAt,
                        verificationDeadline,
                        accessToken
                );


        // =====================================================
        // 8. UPLOAD EVIDENCE
        // =====================================================

        try {

            uploadEvidence(
                    submissionId,
                    employeeId,
                    evidenceFile,
                    accessToken
            );

        } catch (Exception error) {

            /*
             * Submission is incomplete if evidence upload fails.
             * Remove the submission so employee can try again.
             */
            try {

                deleteSubmission(
                        submissionId,
                        accessToken
                );

            } catch (Exception cleanupError) {

                System.err.println(
                        "[Progress Submission Cleanup] "
                                + cleanupError.getMessage()
                );
            }


            throw error;
        }


        // =====================================================
        // 9. NOTIFY MANAGER
        // =====================================================

        try {

            notifyManager(
                    submissionId,
                    taskId,
                    employeeId,
                    proposedProgress,
                    accessToken
            );

        } catch (Exception notificationError) {

            /*
             * Submission already succeeded.
             * Notification failure must NOT delete submission.
             */
            System.err.println(
                    "[Progress Verification Notification] "
                            + notificationError.getMessage()
            );
        }


        // =====================================================
        // RESULT
        // =====================================================

        return new SubmissionResult(
                submissionId,
                currentProgress,
                proposedProgress,
                "Pending Manager Review",
                verificationDeadline
        );
    }


    // =========================================================
    // CREATE SUBMISSION
    // =========================================================

    private UUID createSubmission(
            UUID taskId,
            UUID employeeId,
            int proposedProgress,
            String progressNote,
            OffsetDateTime submittedAt,
            OffsetDateTime verificationDeadline,
            String accessToken
    ) throws Exception {


        JSONObject body =
                new JSONObject();


        body.put(
                "task_id",
                taskId.toString()
        );


        body.put(
                "employee_id",
                employeeId.toString()
        );


        /*
         * IMPORTANT:
         *
         * DO NOT INSERT current_progress here.
         *
         * Your real Supabase table does not contain:
         *
         * current_progress
         *
         * Current actual progress is read from task_tbl.progress.
         */


        body.put(
                "proposed_progress",
                proposedProgress
        );


        body.put(
                "progress_note",
                progressNote
        );


        body.put(
                "status",
                "Pending Manager Review"
        );


        body.put(
                "submitted_at",
                submittedAt.toString()
        );


        body.put(
                "verification_deadline",
                verificationDeadline.toString()
        );


        String url =
                AppConfig.REST_URL
                        + "/task_progress_submission_tbl";


        HttpRequest request =
                supabaseClient
                        .createAuthenticatedRequest(
                                url,
                                accessToken
                        )
                        .header(
                                "Prefer",
                                "return=representation"
                        )
                        .POST(
                                HttpRequest.BodyPublishers.ofString(
                                        body.toString()
                                )
                        )
                        .build();


        HttpResponse<String> response =
                send(
                        request
                );


        validateResponse(
                response,
                "Unable to create progress submission"
        );


        JSONArray array =
                new JSONArray(
                        response.body()
                );


        if (array.isEmpty()) {

            throw new IllegalStateException(
                    "Progress submission was created "
                            + "but no submission ID was returned."
            );
        }


        JSONObject created =
                array.getJSONObject(
                        0
                );


        String id =
                created.optString(
                        "submission_id",
                        ""
                );


        if (id.isBlank()) {

            throw new IllegalStateException(
                    "Submission ID was not returned by Supabase."
            );
        }


        return UUID.fromString(
                id
        );
    }


    // =========================================================
    // UPLOAD EVIDENCE
    // =========================================================

    private void uploadEvidence(
            UUID submissionId,
            UUID employeeId,
            File file,
            String accessToken
    ) throws Exception {


        byte[] bytes;


        try (
                InputStream input =
                        Files.newInputStream(
                                file.toPath()
                        )
        ) {

            bytes =
                    input.readNBytes(
                            (int) MAX_FILE_SIZE + 1
                    );
        }


        if (bytes.length <= 0) {

            throw new IllegalArgumentException(
                    "Evidence file is empty."
            );
        }


        if (
                bytes.length
                        > MAX_FILE_SIZE
        ) {

            throw new IllegalArgumentException(
                    "Evidence file cannot exceed 10 MB."
            );
        }


        String originalName =
                file.getName();


        String extension =
                getExtension(
                        originalName
                );


        String storagePath =
                employeeId
                        + "/"
                        + submissionId
                        + "/"
                        + UUID.randomUUID()
                        + (
                        extension.isBlank()
                                ? ""
                                : "." + extension
                );


        String mimeType =
                Files.probeContentType(
                        file.toPath()
                );


        if (
                mimeType == null
                        || mimeType.isBlank()
        ) {

            mimeType =
                    "application/octet-stream";
        }


        // =====================================================
        // STORAGE UPLOAD
        // =====================================================

        String uploadUrl =
                AppConfig.STORAGE_URL
                        + "/object/"
                        + EVIDENCE_BUCKET
                        + "/"
                        + storagePath;


        HttpRequest uploadRequest =
                supabaseClient
                        .createAuthenticatedRequest(
                                uploadUrl,
                                accessToken
                        )
                        .setHeader(
                                "Content-Type",
                                mimeType
                        )
                        .setHeader(
                                "x-upsert",
                                "false"
                        )
                        .timeout(
                                Duration.ofSeconds(
                                        120
                                )
                        )
                        .POST(
                                HttpRequest.BodyPublishers.ofByteArray(
                                        bytes
                                )
                        )
                        .build();


        HttpResponse<String> uploadResponse =
                send(
                        uploadRequest
                );


        validateResponse(
                uploadResponse,
                "Unable to upload progress evidence"
        );


        // =====================================================
        // SAVE EVIDENCE DATABASE RECORD
        // =====================================================

        JSONObject evidence =
                new JSONObject();


        evidence.put(
                "submission_id",
                submissionId.toString()
        );


        evidence.put(
                "file_name",
                originalName
        );


        evidence.put(
                "file_path",
                storagePath
        );


        evidence.put(
                "file_type",
                mimeType
        );


        evidence.put(
                "file_size",
                bytes.length
        );


        String evidenceUrl =
                AppConfig.REST_URL
                        + "/task_progress_evidence_tbl";


        HttpRequest evidenceRequest =
                supabaseClient
                        .createAuthenticatedRequest(
                                evidenceUrl,
                                accessToken
                        )
                        .header(
                                "Prefer",
                                "return=representation"
                        )
                        .POST(
                                HttpRequest.BodyPublishers.ofString(
                                        evidence.toString()
                                )
                        )
                        .build();


        HttpResponse<String> evidenceResponse =
                send(
                        evidenceRequest
                );


        validateResponse(
                evidenceResponse,
                "Unable to save progress evidence information"
        );
    }


    // =========================================================
    // CHECK EXISTING PENDING SUBMISSION
    // =========================================================

    private boolean hasPendingSubmission(
            UUID taskId,
            UUID employeeId,
            String accessToken
    ) throws Exception {


        String url =
                AppConfig.REST_URL
                        + "/task_progress_submission_tbl"
                        + "?select=submission_id"
                        + "&task_id=eq."
                        + taskId
                        + "&employee_id=eq."
                        + employeeId
                        + "&status=in.(Pending%20Manager%20Review,Needs%20Manager%20Review)"
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
                send(
                        request
                );


        validateResponse(
                response,
                "Unable to check existing progress submissions"
        );


        JSONArray array =
                new JSONArray(
                        response.body()
                );


        return !array.isEmpty();
    }


    // =========================================================
    // GET CURRENT ACTUAL TASK PROGRESS
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
                send(
                        request
                );


        validateResponse(
                response,
                "Unable to load task progress"
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
                .getJSONObject(
                        0
                )
                .optInt(
                        "progress",
                        0
                );
    }


    // =========================================================
    // DELETE INCOMPLETE SUBMISSION
    // =========================================================

    private void deleteSubmission(
            UUID submissionId,
            String accessToken
    ) throws Exception {


        String url =
                AppConfig.REST_URL
                        + "/task_progress_submission_tbl"
                        + "?submission_id=eq."
                        + submissionId;


        HttpRequest request =
                supabaseClient
                        .createAuthenticatedRequest(
                                url,
                                accessToken
                        )
                        .DELETE()
                        .build();


        HttpResponse<String> response =
                send(
                        request
                );


        validateResponse(
                response,
                "Unable to remove incomplete progress submission"
        );
    }


    // =========================================================
    // NOTIFY MANAGER
    // =========================================================

    private void notifyManager(
            UUID submissionId,
            UUID taskId,
            UUID employeeId,
            int proposedProgress,
            String accessToken
    ) throws Exception {


        String managerId =
                getEmployeeManagerId(
                        employeeId,
                        accessToken
                );


        if (
                managerId == null
                        || managerId.isBlank()
        ) {

            System.err.println(
                    "[Progress Verification] "
                            + "Employee has no assigned manager."
            );

            return;
        }


        String taskTitle =
                getTaskTitle(
                        taskId,
                        accessToken
                );


        String employeeName =
                getEmployeeDisplayName(
                        employeeId,
                        accessToken
                );


        String title =
                "Progress Verification Required";


        String message =
                employeeName
                        + " submitted "
                        + proposedProgress
                        + "% progress for \""
                        + taskTitle
                        + "\". Review the progress note "
                        + "and supporting evidence.";


        /*
         * For now use the existing notification method.
         *
         * related_task_id = real task ID
         *
         * event_key contains submission ID for deduplication.
         */
        notificationRepository.createOnce(

                accessToken,

                managerId,

                title,

                message,

                "PROGRESS_VERIFICATION",

                taskId.toString(),

                null,

                "progress_verification:"
                        + submissionId
        );
    }


    // =========================================================
    // GET MANAGER
    // =========================================================

    private String getEmployeeManagerId(
            UUID employeeId,
            String accessToken
    ) throws Exception {


        String url =
                AppConfig.REST_URL
                        + "/user_tbl"
                        + "?select=manager_id"
                        + "&user_id=eq."
                        + employeeId
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
                send(
                        request
                );


        validateResponse(
                response,
                "Unable to find employee manager"
        );


        JSONArray array =
                new JSONArray(
                        response.body()
                );


        if (array.isEmpty()) {

            return null;
        }


        JSONObject row =
                array.getJSONObject(
                        0
                );


        if (
                !row.has(
                        "manager_id"
                )
                        || row.isNull(
                        "manager_id"
                )
        ) {

            return null;
        }


        return row.optString(
                "manager_id",
                null
        );
    }


    // =========================================================
    // GET TASK TITLE
    // =========================================================

    private String getTaskTitle(
            UUID taskId,
            String accessToken
    ) throws Exception {


        String url =
                AppConfig.REST_URL
                        + "/task_tbl"
                        + "?select=task_title"
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
                send(
                        request
                );


        validateResponse(
                response,
                "Unable to load task title"
        );


        JSONArray array =
                new JSONArray(
                        response.body()
                );


        if (array.isEmpty()) {

            return "Task";
        }


        return array
                .getJSONObject(
                        0
                )
                .optString(
                        "task_title",
                        "Task"
                );
    }


    // =========================================================
    // GET EMPLOYEE NAME
    // =========================================================

    private String getEmployeeDisplayName(
            UUID employeeId,
            String accessToken
    ) throws Exception {


        String url =
                AppConfig.REST_URL
                        + "/user_tbl"
                        + "?select=username,employee_id,email"
                        + "&user_id=eq."
                        + employeeId
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
                send(
                        request
                );


        validateResponse(
                response,
                "Unable to load employee information"
        );


        JSONArray array =
                new JSONArray(
                        response.body()
                );


        if (array.isEmpty()) {

            return "Employee";
        }


        JSONObject row =
                array.getJSONObject(
                        0
                );


        String username =
                row.optString(
                        "username",
                        ""
                );


        if (!username.isBlank()) {

            return username;
        }


        String employeeNo =
                row.optString(
                        "employee_id",
                        ""
                );


        if (!employeeNo.isBlank()) {

            return employeeNo;
        }


        return row.optString(
                "email",
                "Employee"
        );
    }


    // =========================================================
    // VALIDATE EVIDENCE FILE
    // =========================================================

    private void validateEvidenceFile(
            File file
    ) {


        if (!file.exists()) {

            throw new IllegalArgumentException(
                    "The selected evidence file no longer exists."
            );
        }


        if (!file.isFile()) {

            throw new IllegalArgumentException(
                    "Please select a valid evidence file."
            );
        }


        if (file.length() <= 0) {

            throw new IllegalArgumentException(
                    "Evidence file cannot be empty."
            );
        }


        if (
                file.length()
                        > MAX_FILE_SIZE
        ) {

            throw new IllegalArgumentException(
                    "Evidence file cannot exceed 10 MB."
            );
        }
    }


    // =========================================================
    // EXTENSION
    // =========================================================

    private String getExtension(
            String fileName
    ) {


        if (
                fileName == null
                        || !fileName.contains(".")
        ) {

            return "";
        }


        return fileName
                .substring(
                        fileName.lastIndexOf(
                                '.'
                        ) + 1
                )
                .toLowerCase();
    }


    // =========================================================
    // ACCESS TOKEN
    // =========================================================

    private String getAccessToken() {


        String accessToken =
                SessionManager.getAccessToken();


        if (
                accessToken == null
                        || accessToken.isBlank()
        ) {

            throw new IllegalStateException(
                    "User session has expired. Please login again."
            );
        }


        return accessToken;
    }


    // =========================================================
    // HTTP
    // =========================================================

    private HttpResponse<String> send(
            HttpRequest request
    ) throws Exception {


        return supabaseClient
                .getHttpClient()
                .send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );
    }


    private void validateResponse(
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
    // RESULT
    // =========================================================

    public static class SubmissionResult {


        private final UUID submissionId;

        private final int currentProgress;

        private final int requestedProgress;

        private final String status;

        private final OffsetDateTime verificationDeadline;


        public SubmissionResult(
                UUID submissionId,
                int currentProgress,
                int requestedProgress,
                String status,
                OffsetDateTime verificationDeadline
        ) {

            this.submissionId =
                    submissionId;


            this.currentProgress =
                    currentProgress;


            this.requestedProgress =
                    requestedProgress;


            this.status =
                    status;


            this.verificationDeadline =
                    verificationDeadline;
        }


        public UUID getSubmissionId() {

            return submissionId;
        }


        public int getCurrentProgress() {

            return currentProgress;
        }


        public int getRequestedProgress() {

            return requestedProgress;
        }


        public String getStatus() {

            return status;
        }


        public OffsetDateTime getVerificationDeadline() {

            return verificationDeadline;
        }
    }
}