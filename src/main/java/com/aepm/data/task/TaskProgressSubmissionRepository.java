package com.aepm.data.task;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;
import com.aepm.model.TaskProgressSubmission;

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

public class TaskProgressSubmissionRepository {

    private final SupabaseClient supabaseClient = SupabaseClient.getInstance();

    public UUID createSubmission(TaskProgressSubmission submission, String accessToken) throws Exception {
        if (submission == null) throw new IllegalArgumentException("Progress submission is required.");
        if (submission.getTaskId() == null) throw new IllegalArgumentException("Task ID is required.");
        if (submission.getEmployeeId() == null) throw new IllegalArgumentException("Employee ID is required.");
        if (submission.getProposedProgress() < 0 || submission.getProposedProgress() > 100)
            throw new IllegalArgumentException("Proposed progress must be between 0 and 100.");
        if (submission.getProgressNote() == null || submission.getProgressNote().isBlank())
            throw new IllegalArgumentException("Progress note is required.");

        JSONObject body = new JSONObject();
        body.put("task_id", submission.getTaskId().toString());
        body.put("employee_id", submission.getEmployeeId().toString());
        body.put("proposed_progress", submission.getProposedProgress());
        body.put("progress_note", submission.getProgressNote().trim());
        body.put("status", submission.getStatus() == null || submission.getStatus().isBlank() ? "Pending" : submission.getStatus());

        String url = AppConfig.REST_URL + "/task_progress_submission_tbl";
        HttpRequest request = supabaseClient.createAuthenticatedRequest(url, accessToken)
                .header("Prefer", "return=representation")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = supabaseClient.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        validate(response, "Unable to create progress submission");

        JSONArray array = new JSONArray(response.body());
        if (array.isEmpty()) throw new RuntimeException("Progress submission was created but no record was returned.");
        return UUID.fromString(array.getJSONObject(0).getString("submission_id"));
    }

    public TaskProgressSubmission getById(UUID submissionId, String accessToken) throws Exception {
        if (submissionId == null) return null;
        String url = AppConfig.REST_URL + "/task_progress_submission_tbl?select=*&submission_id=eq." + submissionId + "&limit=1";
        HttpRequest request = supabaseClient.createAuthenticatedRequest(url, accessToken).GET().build();
        HttpResponse<String> response = supabaseClient.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        validate(response, "Unable to load progress submission");
        JSONArray array = new JSONArray(response.body());
        return array.isEmpty() ? null : map(array.getJSONObject(0));
    }

    public List<TaskProgressSubmission> getByTask(UUID taskId, String accessToken) throws Exception {
        if (taskId == null) return List.of();
        String url = AppConfig.REST_URL + "/task_progress_submission_tbl?select=*&task_id=eq." + taskId + "&order=submitted_at.desc";
        return getList(url, accessToken, "Unable to load task progress submissions");
    }

    public List<TaskProgressSubmission> getByEmployee(UUID employeeId, String accessToken) throws Exception {
        if (employeeId == null) return List.of();
        String url = AppConfig.REST_URL + "/task_progress_submission_tbl?select=*&employee_id=eq." + employeeId + "&order=submitted_at.desc";
        return getList(url, accessToken, "Unable to load employee progress submissions");
    }

    public List<TaskProgressSubmission> getPending(String accessToken) throws Exception {
        String url = AppConfig.REST_URL + "/task_progress_submission_tbl?select=*&status=eq.Pending&order=submitted_at.asc";
        return getList(url, accessToken, "Unable to load pending progress submissions");
    }

    public List<TaskProgressSubmission> getExpiredPending(OffsetDateTime now, String accessToken) throws Exception {
        OffsetDateTime currentTime = now == null ? OffsetDateTime.now() : now;
        String encodedTime = URLEncoder.encode(currentTime.toString(), StandardCharsets.UTF_8);
        String url = AppConfig.REST_URL + "/task_progress_submission_tbl?select=*&status=eq.Pending&verification_deadline=lte." + encodedTime + "&order=verification_deadline.asc";
        return getList(url, accessToken, "Unable to load expired progress submissions");
    }

    public void approveByManager(UUID submissionId, UUID managerId, String reviewComment, String accessToken) throws Exception {
        JSONObject body = new JSONObject();
        body.put("status", "Approved");
        body.put("verification_method", "MANAGER");
        body.put("reviewed_by", managerId == null ? JSONObject.NULL : managerId.toString());
        body.put("review_comment", reviewComment == null || reviewComment.isBlank() ? JSONObject.NULL : reviewComment.trim());
        body.put("reviewed_at", OffsetDateTime.now().toString());
        patchSubmission(submissionId, body, accessToken, "Unable to approve progress submission");
    }

    public void rejectByManager(UUID submissionId, UUID managerId, String reviewComment, String accessToken) throws Exception {
        JSONObject body = new JSONObject();
        body.put("status", "Rejected");
        body.put("verification_method", "MANAGER");
        body.put("reviewed_by", managerId == null ? JSONObject.NULL : managerId.toString());
        body.put("review_comment", reviewComment == null || reviewComment.isBlank() ? "Rejected by manager." : reviewComment.trim());
        body.put("reviewed_at", OffsetDateTime.now().toString());
        patchSubmission(submissionId, body, accessToken, "Unable to reject progress submission");
    }

    public void requestRevision(UUID submissionId, UUID managerId, String reviewComment, String accessToken) throws Exception {
        JSONObject body = new JSONObject();
        body.put("status", "Revision Required");
        body.put("verification_method", "MANAGER");
        body.put("reviewed_by", managerId == null ? JSONObject.NULL : managerId.toString());
        body.put("review_comment", reviewComment == null || reviewComment.isBlank() ? "Revision is required." : reviewComment.trim());
        body.put("reviewed_at", OffsetDateTime.now().toString());
        patchSubmission(submissionId, body, accessToken, "Unable to request revision");
    }

    public void approveBySystem(UUID submissionId, double confidence, String systemReason, String accessToken) throws Exception {
        JSONObject body = new JSONObject();
        body.put("status", "Approved");
        body.put("verification_method", "SYSTEM");
        body.put("system_confidence", confidence);
        body.put("system_reason", systemReason == null || systemReason.isBlank() ? "Approved by automatic system verification." : systemReason.trim());
        body.put("reviewed_at", OffsetDateTime.now().toString());
        patchSubmission(submissionId, body, accessToken, "Unable to system-approve progress submission");
    }

    public void markNeedsManagerReview(UUID submissionId, double confidence, String systemReason, String accessToken) throws Exception {
        JSONObject body = new JSONObject();
        body.put("status", "Needs Manager Review");
        body.put("verification_method", "SYSTEM");
        body.put("system_confidence", confidence);
        body.put("system_reason", systemReason == null || systemReason.isBlank() ? "Automatic verification could not safely approve this submission." : systemReason.trim());
        patchSubmission(submissionId, body, accessToken, "Unable to flag submission for manager review");
    }

    private void patchSubmission(UUID submissionId, JSONObject body, String accessToken, String errorMessage) throws Exception {
        if (submissionId == null) throw new IllegalArgumentException("Submission ID is required.");
        String url = AppConfig.REST_URL + "/task_progress_submission_tbl?submission_id=eq." + submissionId;
        HttpRequest request = supabaseClient.createAuthenticatedRequest(url, accessToken)
                .header("Prefer", "return=representation")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        HttpResponse<String> response = supabaseClient.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        validate(response, errorMessage);
    }

    private List<TaskProgressSubmission> getList(String url, String accessToken, String errorMessage) throws Exception {
        HttpRequest request = supabaseClient.createAuthenticatedRequest(url, accessToken).GET().build();
        HttpResponse<String> response = supabaseClient.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        validate(response, errorMessage);
        JSONArray array = new JSONArray(response.body());
        List<TaskProgressSubmission> result = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) result.add(map(array.getJSONObject(i)));
        return result;
    }

    private TaskProgressSubmission map(JSONObject row) {
        TaskProgressSubmission submission = new TaskProgressSubmission();
        submission.setSubmissionId(getUuid(row, "submission_id"));
        submission.setTaskId(getUuid(row, "task_id"));
        submission.setEmployeeId(getUuid(row, "employee_id"));
        submission.setProposedProgress(row.optDouble("proposed_progress", 0));
        submission.setProgressNote(getNullableString(row, "progress_note"));
        submission.setStatus(getNullableString(row, "status"));
        submission.setVerificationDeadline(getDateTime(row, "verification_deadline"));
        submission.setVerificationMethod(getNullableString(row, "verification_method"));
        if (row.has("system_confidence") && !row.isNull("system_confidence")) submission.setSystemConfidence(row.getDouble("system_confidence"));
        submission.setSystemReason(getNullableString(row, "system_reason"));
        submission.setReviewedBy(getUuid(row, "reviewed_by"));
        submission.setReviewComment(getNullableString(row, "review_comment"));
        submission.setSubmittedAt(getDateTime(row, "submitted_at"));
        submission.setReviewedAt(getDateTime(row, "reviewed_at"));
        return submission;
    }

    private UUID getUuid(JSONObject row, String key) {
        if (!row.has(key) || row.isNull(key)) return null;
        String value = row.optString(key, "");
        return value.isBlank() ? null : UUID.fromString(value);
    }

    private OffsetDateTime getDateTime(JSONObject row, String key) {
        String value = getNullableString(row, key);
        return value == null || value.isBlank() ? null : OffsetDateTime.parse(value);
    }

    private String getNullableString(JSONObject row, String key) {
        if (!row.has(key) || row.isNull(key)) return null;
        return row.optString(key, null);
    }

    private void validate(HttpResponse<String> response, String message) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException(message + ": " + response.body());
        }
    }
}
