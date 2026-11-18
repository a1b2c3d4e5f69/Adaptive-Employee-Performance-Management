package com.aepm.logic.task;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;
import com.aepm.data.task.ProgressVerificationRepository;
import com.aepm.model.User;
import com.aepm.util.SessionManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class ManagerProgressVerificationService {

    private final SupabaseClient supabaseClient =
            SupabaseClient.getInstance();

    private final ProgressVerificationRepository verificationRepository =
            new ProgressVerificationRepository();


    // =========================================================
    // GET PENDING SUBMISSIONS FOR CURRENT MANAGER
    // =========================================================

    public List<VerificationItem> getPendingSubmissions()
            throws Exception {

        User manager =
                SessionManager.getCurrentUser();


        if (
                manager == null
                        || manager.getUserId() == null
        ) {

            throw new IllegalStateException(
                    "No logged-in manager found."
            );
        }


        UUID managerId =
                UUID.fromString(
                        manager.getUserId()
                );


        String accessToken =
                getAccessToken();


        /*
         * First get employees belonging to this manager.
         */
        List<UUID> employeeIds =
                getManagedEmployeeIds(
                        managerId,
                        accessToken
                );


        if (employeeIds.isEmpty()) {

            return new ArrayList<>();
        }


        List<VerificationItem> result =
                new ArrayList<>();


        for (UUID employeeId : employeeIds) {

            List<JSONObject> submissions =
                    getEmployeePendingSubmissions(
                            employeeId,
                            accessToken
                    );


            for (JSONObject submission : submissions) {

                result.add(
                        buildVerificationItem(
                                submission,
                                accessToken
                        )
                );
            }
        }


        return result;
    }


    // =========================================================
    // MANAGED EMPLOYEES
    // =========================================================

    private List<UUID> getManagedEmployeeIds(
            UUID managerId,
            String accessToken
    ) throws Exception {

        String url =
                AppConfig.REST_URL
                        + "/user_tbl"
                        + "?select=user_id"
                        + "&manager_id=eq."
                        + managerId
                        + "&role_id=eq.3"
                        + "&account_status=eq.Active";


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
                "Unable to load managed employees"
        );


        JSONArray array =
                new JSONArray(
                        response.body()
                );


        List<UUID> employeeIds =
                new ArrayList<>();


        for (
                int i = 0;
                i < array.length();
                i++
        ) {

            String id =
                    array
                            .getJSONObject(i)
                            .optString(
                                    "user_id",
                                    null
                            );


            if (
                    id != null
                            && !id.isBlank()
            ) {

                employeeIds.add(
                        UUID.fromString(id)
                );
            }
        }


        return employeeIds;
    }


    // =========================================================
    // EMPLOYEE PENDING SUBMISSIONS
    // =========================================================

    private List<JSONObject> getEmployeePendingSubmissions(
            UUID employeeId,
            String accessToken
    ) throws Exception {

        String url =
                AppConfig.REST_URL
                        + "/task_progress_submission_tbl"
                        + "?select=*"
                        + "&employee_id=eq."
                        + employeeId
                        + "&status=in.(Pending%20Manager%20Review,Needs%20Manager%20Review)"
                        + "&order=submitted_at.asc";


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
                "Unable to load pending progress submissions"
        );


        JSONArray array =
                new JSONArray(
                        response.body()
                );


        List<JSONObject> submissions =
                new ArrayList<>();


        for (
                int i = 0;
                i < array.length();
                i++
        ) {

            submissions.add(
                    array.getJSONObject(i)
            );
        }


        return submissions;
    }


    // =========================================================
    // BUILD VERIFICATION ITEM
    // =========================================================

    private VerificationItem buildVerificationItem(
            JSONObject submission,
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


        UUID employeeId =
                UUID.fromString(
                        submission.getString(
                                "employee_id"
                        )
                );


        JSONObject task =
                getTask(
                        taskId,
                        accessToken
                );


        JSONObject employee =
                getEmployee(
                        employeeId,
                        accessToken
                );


        List<EvidenceItem> evidence =
                getEvidence(
                        submissionId,
                        accessToken
                );


        return new VerificationItem(

                submissionId,

                taskId,

                employeeId,

                task.optString(
                        "task_title",
                        "Unknown Task"
                ),

                getEmployeeDisplayName(
                        employee
                ),

                task.optInt(
                        "progress",
                        0
                ),

                submission.optInt(
                        "proposed_progress",
                        0
                ),

                submission.optString(
                        "progress_note",
                        ""
                ),

                submission.optString(
                        "status",
                        ""
                ),

                submission.optString(
                        "submitted_at",
                        ""
                ),

                submission.optString(
                        "verification_deadline",
                        ""
                ),

                submission.optDouble(
                        "system_confidence",
                        0
                ),

                submission.optString(
                        "system_reason",
                        ""
                ),

                evidence
        );
    }


    // =========================================================
    // TASK
    // =========================================================

    private JSONObject getTask(
            UUID taskId,
            String accessToken
    ) throws Exception {

        String url =
                AppConfig.REST_URL
                        + "/task_tbl"
                        + "?select=task_id,task_title,progress,task_status"
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
                "Unable to load task information"
        );


        JSONArray array =
                new JSONArray(
                        response.body()
                );


        if (array.isEmpty()) {

            return new JSONObject();
        }


        return array.getJSONObject(0);
    }


    // =========================================================
    // EMPLOYEE
    // =========================================================

    private JSONObject getEmployee(
            UUID employeeId,
            String accessToken
    ) throws Exception {

        String url =
                AppConfig.REST_URL
                        + "/user_tbl"
                        + "?select=user_id,username,email,employee_id"
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
                send(request);


        validate(
                response,
                "Unable to load employee information"
        );


        JSONArray array =
                new JSONArray(
                        response.body()
                );


        if (array.isEmpty()) {

            return new JSONObject();
        }


        return array.getJSONObject(0);
    }


    private String getEmployeeDisplayName(
            JSONObject employee
    ) {

        String username =
                employee.optString(
                        "username",
                        ""
                );


        if (!username.isBlank()) {

            return username;
        }


        String employeeNo =
                employee.optString(
                        "employee_id",
                        ""
                );


        if (!employeeNo.isBlank()) {

            return employeeNo;
        }


        return employee.optString(
                "email",
                "Unknown Employee"
        );
    }


    // =========================================================
    // EVIDENCE
    // =========================================================

    private List<EvidenceItem> getEvidence(
            UUID submissionId,
            String accessToken
    ) throws Exception {

        String url =
                AppConfig.REST_URL
                        + "/task_progress_evidence_tbl"
                        + "?select=evidence_id,file_name,file_path,file_type,file_size"
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


        List<EvidenceItem> result =
                new ArrayList<>();


        for (
                int i = 0;
                i < array.length();
                i++
        ) {

            JSONObject row =
                    array.getJSONObject(i);


            result.add(
                    new EvidenceItem(

                            row.optString(
                                    "evidence_id",
                                    ""
                            ),

                            row.optString(
                                    "file_name",
                                    "Evidence"
                            ),

                            row.optString(
                                    "file_path",
                                    ""
                            ),

                            row.optString(
                                    "file_type",
                                    ""
                            ),

                            row.optLong(
                                    "file_size",
                                    0
                            )
                    )
            );
        }


        return result;
    }


    // =========================================================
    // APPROVE
    // =========================================================

    public void approve(
            UUID submissionId,
            String comment
    ) throws Exception {

        verificationRepository
                .approveByManager(

                        submissionId,

                        getCurrentManagerId(),

                        comment,

                        getAccessToken()
                );
    }


    // =========================================================
    // REJECT
    // =========================================================

    public void reject(
            UUID submissionId,
            String reason
    ) throws Exception {

        if (
                reason == null
                        || reason.trim().isEmpty()
        ) {

            throw new IllegalArgumentException(
                    "Please provide a rejection reason."
            );
        }


        verificationRepository
                .rejectByManager(

                        submissionId,

                        getCurrentManagerId(),

                        reason.trim(),

                        getAccessToken()
                );
    }


    // =========================================================
    // REQUEST REVISION
    // =========================================================

    public void requestRevision(
            UUID submissionId,
            String instruction
    ) throws Exception {

        if (
                instruction == null
                        || instruction.trim().isEmpty()
        ) {

            throw new IllegalArgumentException(
                    "Please provide revision instructions."
            );
        }


        verificationRepository
                .requestRevisionByManager(

                        submissionId,

                        getCurrentManagerId(),

                        instruction.trim(),

                        getAccessToken()
                );
    }


    // =========================================================
    // CURRENT MANAGER ID
    // =========================================================

    private UUID getCurrentManagerId() {

        User manager =
                SessionManager.getCurrentUser();


        if (
                manager == null
                        || manager.getUserId() == null
        ) {

            throw new IllegalStateException(
                    "No logged-in manager found."
            );
        }


        return UUID.fromString(
                manager.getUserId()
        );
    }


    // =========================================================
    // TOKEN
    // =========================================================

    private String getAccessToken() {

        String token =
                SessionManager.getAccessToken();


        if (
                token == null
                        || token.isBlank()
        ) {

            throw new IllegalStateException(
                    "User session has expired. Please login again."
            );
        }


        return token;
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
    // VERIFICATION ITEM
    // =========================================================

    public static class VerificationItem {

        private final UUID submissionId;

        private final UUID taskId;

        private final UUID employeeId;

        private final String taskTitle;

        private final String employeeName;

        private final int currentProgress;

        private final int requestedProgress;

        private final String progressNote;

        private final String status;

        private final String submittedAt;

        private final String reviewDeadline;

        private final double systemConfidence;

        private final String systemReason;

        private final List<EvidenceItem> evidence;


        public VerificationItem(
                UUID submissionId,
                UUID taskId,
                UUID employeeId,
                String taskTitle,
                String employeeName,
                int currentProgress,
                int requestedProgress,
                String progressNote,
                String status,
                String submittedAt,
                String reviewDeadline,
                double systemConfidence,
                String systemReason,
                List<EvidenceItem> evidence
        ) {

            this.submissionId = submissionId;
            this.taskId = taskId;
            this.employeeId = employeeId;
            this.taskTitle = taskTitle;
            this.employeeName = employeeName;
            this.currentProgress = currentProgress;
            this.requestedProgress = requestedProgress;
            this.progressNote = progressNote;
            this.status = status;
            this.submittedAt = submittedAt;
            this.reviewDeadline = reviewDeadline;
            this.systemConfidence = systemConfidence;
            this.systemReason = systemReason;
            this.evidence = evidence;
        }


        public UUID getSubmissionId() {
            return submissionId;
        }

        public UUID getTaskId() {
            return taskId;
        }

        public UUID getEmployeeId() {
            return employeeId;
        }

        public String getTaskTitle() {
            return taskTitle;
        }

        public String getEmployeeName() {
            return employeeName;
        }

        public int getCurrentProgress() {
            return currentProgress;
        }

        public int getRequestedProgress() {
            return requestedProgress;
        }

        public String getProgressNote() {
            return progressNote;
        }

        public String getStatus() {
            return status;
        }

        public String getSubmittedAt() {
            return submittedAt;
        }

        public String getReviewDeadline() {
            return reviewDeadline;
        }

        public double getSystemConfidence() {
            return systemConfidence;
        }

        public String getSystemReason() {
            return systemReason;
        }

        public List<EvidenceItem> getEvidence() {
            return evidence;
        }
    }


    // =========================================================
    // EVIDENCE ITEM
    // =========================================================

    public static class EvidenceItem {

        private final String evidenceId;

        private final String fileName;

        private final String filePath;

        private final String fileType;

        private final long fileSize;


        public EvidenceItem(
                String evidenceId,
                String fileName,
                String filePath,
                String fileType,
                long fileSize
        ) {

            this.evidenceId = evidenceId;
            this.fileName = fileName;
            this.filePath = filePath;
            this.fileType = fileType;
            this.fileSize = fileSize;
        }


        public String getEvidenceId() {
            return evidenceId;
        }

        public String getFileName() {
            return fileName;
        }

        public String getFilePath() {
            return filePath;
        }

        public String getFileType() {
            return fileType;
        }

        public long getFileSize() {
            return fileSize;
        }
    }

    // =========================================================
    // GET ONE SUBMISSION FOR NOTIFICATION REVIEW
    // =========================================================

    public VerificationItem getSubmissionForReview(
            UUID submissionId
    ) throws Exception {

        if (submissionId == null) {

            throw new IllegalArgumentException(
                    "Submission ID is required."
            );
        }


        String accessToken =
                getAccessToken();


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


        String status =
                submission.optString(
                        "status",
                        ""
                );


        /*
        * Only unresolved submissions can still
        * be reviewed from notification.
        */
        if (
                !"Pending Manager Review"
                        .equalsIgnoreCase(status)
                        &&
                !"Needs Manager Review"
                        .equalsIgnoreCase(status)
        ) {

            throw new IllegalStateException(
                    "This progress submission has already been reviewed."
            );
        }


        return buildVerificationItem(
                submission,
                accessToken
        );
    }
}