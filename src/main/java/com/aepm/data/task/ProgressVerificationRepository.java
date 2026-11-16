package com.aepm.data.task;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import java.time.OffsetDateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/**
 * Handles the persistence operations for task progress verification.
 *
 * Responsibilities:
 *
 * 1. Load progress submissions.
 * 2. Load expired manager-review submissions.
 * 3. Manager approve progress.
 * 4. Manager reject progress.
 * 5. Manager request revision.
 * 6. System approve verified progress.
 * 7. Mark submission as Needs Manager Review.
 * 8. Update task_tbl.progress.
 * 9. Store accepted progress into task_progress_tbl.
 */
public class ProgressVerificationRepository {

    private final SupabaseClient supabaseClient =
            SupabaseClient.getInstance();


    // =========================================================
    // LOAD SUBMISSION
    // =========================================================

    public JSONObject getSubmission(
            UUID submissionId,
            String accessToken
    ) throws Exception {

        if (submissionId == null) {

            throw new IllegalArgumentException(
                    "Submission ID is required."
            );
        }


        String url =
                AppConfig.REST_URL
                        + "/task_progress_submission_tbl"
                        + "?select=*"
                        + "&submission_id=eq."
                        + submissionId
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
                "Unable to load progress submission"
        );


        JSONArray array =
                new JSONArray(
                        response.body()
                );


        if (array.isEmpty()) {

            return null;
        }


        return array.getJSONObject(0);
    }


    // =========================================================
    // GET EXPIRED PENDING SUBMISSIONS
    // =========================================================

    /**
     * Used by automatic verification.
     *
     * Returns submissions where:
     *
     * status = Pending Manager Review
     *
     * AND
     *
     * verification_deadline <= current time
     */
    public List<JSONObject> getExpiredPendingSubmissions(
            String accessToken
    ) throws Exception {

        String now =
                URLEncoder.encode(
                        OffsetDateTime.now().toString(),
                        StandardCharsets.UTF_8
                );


        String url =
                AppConfig.REST_URL
                        + "/task_progress_submission_tbl"
                        + "?select=*"
                        + "&status=eq."
                        + encode(
                                "Pending Manager Review"
                        )
                        + "&verification_deadline=lte."
                        + now
                        + "&order=verification_deadline.asc";


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
                "Unable to load expired progress submissions"
        );


        JSONArray array =
                new JSONArray(
                        response.body()
                );


        List<JSONObject> result =
                new ArrayList<>();


        for (
                int i = 0;
                i < array.length();
                i++
        ) {

            result.add(
                    array.getJSONObject(i)
            );
        }


        return result;
    }


    // =========================================================
    // MANAGER APPROVE
    // =========================================================

    /**
     * Manager approval accepts the employee's full proposed
     * progress.
     */
    public void approveByManager(
            UUID submissionId,
            UUID managerId,
            String reviewComment,
            String accessToken
    ) throws Exception {

        JSONObject submission =
                requireSubmission(
                        submissionId,
                        accessToken
                );


        validatePendingSubmission(
                submission
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


        int proposedProgress =
                submission.getInt(
                        "proposed_progress"
                );


        String progressNote =
                submission.optString(
                        "progress_note",
                        null
                );


        // -----------------------------------------------------
        // Update actual task progress
        // -----------------------------------------------------

        updateTaskProgress(
                taskId,
                proposedProgress,
                accessToken
        );


        // -----------------------------------------------------
        // Save approved progress history
        // -----------------------------------------------------

        saveProgressHistory(
                taskId,
                employeeId,
                proposedProgress,
                progressNote,
                accessToken
        );


        // -----------------------------------------------------
        // Mark submission as approved
        // -----------------------------------------------------

        JSONObject body =
                new JSONObject();


        body.put(
                "status",
                "Approved"
        );


        body.put(
                "verification_method",
                "Manager"
        );


        body.put(
                "reviewed_by",
                managerId == null
                        ? JSONObject.NULL
                        : managerId.toString()
        );


        body.put(
                "review_comment",
                emptyToNull(
                        reviewComment
                )
        );


        body.put(
                "reviewed_at",
                OffsetDateTime
                        .now()
                        .toString()
        );


        updateSubmission(
                submissionId,
                body,
                accessToken
        );
    }


    // =========================================================
    // MANAGER REJECT
    // =========================================================

    public void rejectByManager(
            UUID submissionId,
            UUID managerId,
            String reviewComment,
            String accessToken
    ) throws Exception {

        JSONObject submission =
                requireSubmission(
                        submissionId,
                        accessToken
                );


        validatePendingSubmission(
                submission
        );


        if (
                reviewComment == null
                        || reviewComment.trim().isEmpty()
        ) {

            throw new IllegalArgumentException(
                    "A rejection reason is required."
            );
        }


        JSONObject body =
                new JSONObject();


        body.put(
                "status",
                "Rejected"
        );


        body.put(
                "verification_method",
                "Manager"
        );


        body.put(
                "reviewed_by",
                managerId == null
                        ? JSONObject.NULL
                        : managerId.toString()
        );


        body.put(
                "review_comment",
                reviewComment.trim()
        );


        body.put(
                "reviewed_at",
                OffsetDateTime
                        .now()
                        .toString()
        );


        updateSubmission(
                submissionId,
                body,
                accessToken
        );
    }


    // =========================================================
    // MANAGER REQUEST REVISION
    // =========================================================

    /**
     * Used when the manager does not want to fully reject the
     * submission but wants the employee to provide better
     * evidence or explanation.
     */
    public void requestRevisionByManager(
            UUID submissionId,
            UUID managerId,
            String reviewComment,
            String accessToken
    ) throws Exception {

        JSONObject submission =
                requireSubmission(
                        submissionId,
                        accessToken
                );


        validatePendingSubmission(
                submission
        );


        if (
                reviewComment == null
                        || reviewComment.trim().isEmpty()
        ) {

            throw new IllegalArgumentException(
                    "Please provide the revision instructions."
            );
        }


        JSONObject body =
                new JSONObject();


        body.put(
                "status",
                "Revision Required"
        );


        body.put(
                "verification_method",
                "Manager"
        );


        body.put(
                "reviewed_by",
                managerId == null
                        ? JSONObject.NULL
                        : managerId.toString()
        );


        body.put(
                "review_comment",
                reviewComment.trim()
        );


        body.put(
                "reviewed_at",
                OffsetDateTime
                        .now()
                        .toString()
        );


        updateSubmission(
                submissionId,
                body,
                accessToken
        );
    }


    // =========================================================
    // SYSTEM APPROVE
    // =========================================================

    /**
     * Called after the Manager verification deadline expires.
     *
     * verifiedProgress may be lower than proposedProgress.
     *
     * Example:
     *
     * Current = 20
     * Proposed = 70
     * System verified = 40
     *
     * task_tbl.progress becomes 40.
     */
    public void approveBySystem(
            UUID submissionId,
            int verifiedProgress,
            double confidence,
            String systemReason,
            String accessToken
    ) throws Exception {

        JSONObject submission =
                requireSubmission(
                        submissionId,
                        accessToken
                );


        validateSystemEligibleSubmission(
                submission
        );


        if (
                verifiedProgress < 0
                        || verifiedProgress > 100
        ) {

            throw new IllegalArgumentException(
                    "Verified progress must be between 0 and 100."
            );
        }


        if (
                confidence < 0
                        || confidence > 100
        ) {

            throw new IllegalArgumentException(
                    "System confidence must be between 0 and 100."
            );
        }


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


        String progressNote =
                submission.optString(
                        "progress_note",
                        null
                );


        // -----------------------------------------------------
        // Update actual task progress
        // -----------------------------------------------------

        updateTaskProgress(
                taskId,
                verifiedProgress,
                accessToken
        );


        // -----------------------------------------------------
        // Save accepted system progress history
        // -----------------------------------------------------

        saveProgressHistory(
                taskId,
                employeeId,
                verifiedProgress,
                buildSystemProgressNote(
                        progressNote,
                        systemReason
                ),
                accessToken
        );


        // -----------------------------------------------------
        // Update submission
        // -----------------------------------------------------

        JSONObject body =
                new JSONObject();


        body.put(
                "status",
                "System Approved"
        );


        body.put(
                "verification_method",
                "System"
        );


        body.put(
                "system_confidence",
                confidence
        );


        body.put(
                "system_reason",
                emptyToNull(
                        systemReason
                )
        );


        body.put(
                "reviewed_by",
                JSONObject.NULL
        );


        body.put(
                "reviewed_at",
                OffsetDateTime
                        .now()
                        .toString()
        );


        updateSubmission(
                submissionId,
                body,
                accessToken
        );
    }


    // =========================================================
    // NEEDS MANAGER REVIEW
    // =========================================================

    /**
     * Used when automatic verification cannot safely approve
     * the employee submission.
     *
     * Example:
     *
     * - low system confidence
     * - suspicious evidence
     * - completion = 100% but automatic completion is disabled
     * - evidence cannot be evaluated
     */
    public void markNeedsManagerReview(
            UUID submissionId,
            double confidence,
            String systemReason,
            String accessToken
    ) throws Exception {

        JSONObject submission =
                requireSubmission(
                        submissionId,
                        accessToken
                );


        validateSystemEligibleSubmission(
                submission
        );


        JSONObject body =
                new JSONObject();


        body.put(
                "status",
                "Needs Manager Review"
        );


        body.put(
                "verification_method",
                "System"
        );


        body.put(
                "system_confidence",
                confidence
        );


        body.put(
                "system_reason",
                emptyToNull(
                        systemReason
                )
        );


        /*
         * Do NOT set reviewed_at.
         *
         * The submission still requires human review.
         */


        updateSubmission(
                submissionId,
                body,
                accessToken
        );
    }


    // =========================================================
    // RETURN TO PENDING MANAGER REVIEW
    // =========================================================

    /**
     * Useful when employee submits revised evidence after
     * Revision Required.
     */
    public void returnToManagerReview(
            UUID submissionId,
            OffsetDateTime newDeadline,
            String accessToken
    ) throws Exception {

        requireSubmission(
                submissionId,
                accessToken
        );


        JSONObject body =
                new JSONObject();


        body.put(
                "status",
                "Pending Manager Review"
        );


        body.put(
                "verification_method",
                JSONObject.NULL
        );


        body.put(
                "system_confidence",
                JSONObject.NULL
        );


        body.put(
                "system_reason",
                JSONObject.NULL
        );


        body.put(
                "reviewed_by",
                JSONObject.NULL
        );


        body.put(
                "review_comment",
                JSONObject.NULL
        );


        body.put(
                "reviewed_at",
                JSONObject.NULL
        );


        if (newDeadline != null) {

            body.put(
                    "verification_deadline",
                    newDeadline.toString()
            );
        }


        updateSubmission(
                submissionId,
                body,
                accessToken
        );
    }


    // =========================================================
    // UPDATE TASK ACTUAL PROGRESS
    // =========================================================

    private void updateTaskProgress(
            UUID taskId,
            int progress,
            String accessToken
    ) throws Exception {

        JSONObject body =
                new JSONObject();


        body.put(
                "progress",
                progress
        );


        /*
         * Automatically change task status when appropriate.
         */

        if (progress >= 100) {

            body.put(
                    "task_status",
                    "Completed"
            );

        } else if (progress > 0) {

            body.put(
                    "task_status",
                    "In Progress"
            );
        }


        String url =
                AppConfig.REST_URL
                        + "/task_tbl"
                        + "?task_id=eq."
                        + taskId;


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
                        .method(
                                "PATCH",
                                HttpRequest
                                        .BodyPublishers
                                        .ofString(
                                                body.toString()
                                        )
                        )
                        .build();


        HttpResponse<String> response =
                send(request);


        validate(
                response,
                "Unable to update task progress"
        );
    }


    // =========================================================
    // SAVE TASK PROGRESS HISTORY
    // =========================================================

    /**
     * Important:
     *
     * task_progress_submission_tbl stores what the employee
     * REQUESTED.
     *
     * task_progress_tbl stores what was actually ACCEPTED.
     */
    private void saveProgressHistory(
            UUID taskId,
            UUID employeeId,
            int progress,
            String progressNote,
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


        body.put(
                "progress_percentage",
                progress
        );


        body.put(
                "progress_note",
                progressNote == null
                        ? JSONObject.NULL
                        : progressNote
        );


        String url =
                AppConfig.REST_URL
                        + "/task_progress_tbl";


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
                                HttpRequest
                                        .BodyPublishers
                                        .ofString(
                                                body.toString()
                                        )
                        )
                        .build();


        HttpResponse<String> response =
                send(request);


        validate(
                response,
                "Unable to save verified progress history"
        );
    }


    // =========================================================
    // UPDATE SUBMISSION
    // =========================================================

    private void updateSubmission(
            UUID submissionId,
            JSONObject body,
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
                        .header(
                                "Prefer",
                                "return=representation"
                        )
                        .method(
                                "PATCH",
                                HttpRequest
                                        .BodyPublishers
                                        .ofString(
                                                body.toString()
                                        )
                        )
                        .build();


        HttpResponse<String> response =
                send(request);


        validate(
                response,
                "Unable to update progress verification submission"
        );
    }


    // =========================================================
    // REQUIRE SUBMISSION
    // =========================================================

    private JSONObject requireSubmission(
            UUID submissionId,
            String accessToken
    ) throws Exception {

        JSONObject submission =
                getSubmission(
                        submissionId,
                        accessToken
                );


        if (submission == null) {

            throw new IllegalArgumentException(
                    "Progress submission was not found."
            );
        }


        return submission;
    }


    // =========================================================
    // VALIDATE PENDING MANAGER SUBMISSION
    // =========================================================

    private void validatePendingSubmission(
            JSONObject submission
    ) {

        String status =
                submission.optString(
                        "status",
                        ""
                );


        boolean valid =
                status.equalsIgnoreCase(
                        "Pending Manager Review"
                )
                        || status.equalsIgnoreCase(
                                "Needs Manager Review"
                        );


        if (!valid) {

            throw new IllegalStateException(
                    "This progress submission can no longer be reviewed."
            );
        }
    }


    // =========================================================
    // VALIDATE SYSTEM ELIGIBILITY
    // =========================================================

    private void validateSystemEligibleSubmission(
            JSONObject submission
    ) {

        String status =
                submission.optString(
                        "status",
                        ""
                );


        if (
                !status.equalsIgnoreCase(
                        "Pending Manager Review"
                )
        ) {

            throw new IllegalStateException(
                    "Only pending manager-review submissions "
                            + "can be processed automatically."
            );
        }


        String deadlineText =
                submission.optString(
                        "verification_deadline",
                        null
                );


        if (
                deadlineText == null
                        || deadlineText.isBlank()
        ) {

            throw new IllegalStateException(
                    "The submission has no verification deadline."
            );
        }


        OffsetDateTime deadline =
                OffsetDateTime.parse(
                        deadlineText
                );


        if (
                OffsetDateTime.now()
                        .isBefore(deadline)
        ) {

            throw new IllegalStateException(
                    "The manager review period has not expired yet."
            );
        }
    }


    // =========================================================
    // SYSTEM PROGRESS NOTE
    // =========================================================

    private String buildSystemProgressNote(
            String employeeNote,
            String systemReason
    ) {

        StringBuilder result =
                new StringBuilder();


        if (
                employeeNote != null
                        && !employeeNote.isBlank()
        ) {

            result.append(
                    employeeNote.trim()
            );
        }


        if (
                systemReason != null
                        && !systemReason.isBlank()
        ) {

            if (!result.isEmpty()) {

                result.append(
                        "\n\n"
                );
            }


            result.append(
                    "[System Verification] "
            );


            result.append(
                    systemReason.trim()
            );
        }


        return result.isEmpty()
                ? null
                : result.toString();
    }


    // =========================================================
    // EMPTY STRING → JSON NULL
    // =========================================================

    private Object emptyToNull(
            String value
    ) {

        if (
                value == null
                        || value.trim().isEmpty()
        ) {

            return JSONObject.NULL;
        }


        return value.trim();
    }


    // =========================================================
    // URL ENCODE
    // =========================================================

    private String encode(
            String value
    ) {

        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8
        );
    }


    // =========================================================
    // SEND REQUEST
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
    // VALIDATE RESPONSE
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
}