package com.aepm.data.task;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;
import com.aepm.model.TaskProgressEvidence;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TaskProgressEvidenceRepository {

    private static final String BUCKET_NAME = "task-progress-evidence";
    private final SupabaseClient supabaseClient = SupabaseClient.getInstance();

    public TaskProgressEvidence upload(UUID taskId, UUID submissionId, UUID uploadedBy, File file, String accessToken) throws Exception {
        if (taskId == null) throw new IllegalArgumentException("Task ID is required.");
        if (submissionId == null) throw new IllegalArgumentException("Submission ID is required.");
        if (uploadedBy == null) throw new IllegalArgumentException("Uploader ID is required.");
        if (file == null || !file.exists() || !file.isFile()) throw new IllegalArgumentException("Evidence file is required.");

        String originalFileName = file.getName();
        String safeFileName = sanitizeFileName(originalFileName);
        String filePath = taskId + "/" + submissionId + "/" + UUID.randomUUID() + "_" + safeFileName;
        byte[] bytes = Files.readAllBytes(file.toPath());
        String contentType = Files.probeContentType(file.toPath());
        if (contentType == null || contentType.isBlank()) contentType = "application/octet-stream";

        uploadToStorage(filePath, bytes, contentType, accessToken);

        TaskProgressEvidence evidence = new TaskProgressEvidence();
        evidence.setSubmissionId(submissionId);
        evidence.setFileName(originalFileName);
        evidence.setFilePath(filePath);
        evidence.setFileType(contentType);
        evidence.setFileSize(file.length());
        evidence.setUploadedBy(uploadedBy);

        try {
            UUID evidenceId = insertMetadata(evidence, accessToken);
            evidence.setEvidenceId(evidenceId);
            evidence.setUploadedAt(OffsetDateTime.now());
            return evidence;
        } catch (Exception ex) {
            try { deleteStorageObject(filePath, accessToken); } catch (Exception cleanupError) { cleanupError.printStackTrace(); }
            throw ex;
        }
    }

    public List<TaskProgressEvidence> getBySubmission(UUID submissionId, String accessToken) throws Exception {
        if (submissionId == null) return List.of();
        String url = AppConfig.REST_URL + "/task_progress_evidence_tbl?select=*&submission_id=eq." + submissionId + "&order=uploaded_at.asc";
        HttpRequest request = supabaseClient.createAuthenticatedRequest(url, accessToken).GET().build();
        HttpResponse<String> response = supabaseClient.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        validate(response, "Unable to load progress evidence");
        JSONArray array = new JSONArray(response.body());
        List<TaskProgressEvidence> result = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) result.add(map(array.getJSONObject(i)));
        return result;
    }

    public byte[] download(TaskProgressEvidence evidence, String accessToken) throws Exception {
        if (evidence == null || evidence.getFilePath() == null || evidence.getFilePath().isBlank())
            throw new IllegalArgumentException("Evidence file path is required.");

        String url = getSupabaseBaseUrl() + "/storage/v1/object/" + BUCKET_NAME + "/" + encodeStoragePath(evidence.getFilePath());
        HttpRequest request = supabaseClient.createAuthenticatedRequest(url, accessToken).GET().build();
        HttpResponse<byte[]> response = supabaseClient.getHttpClient().send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            throw new RuntimeException("Unable to download progress evidence. HTTP " + response.statusCode());
        return response.body();
    }

    public void delete(TaskProgressEvidence evidence, String accessToken) throws Exception {
        if (evidence == null || evidence.getEvidenceId() == null) throw new IllegalArgumentException("Evidence record is required.");
        if (evidence.getFilePath() != null && !evidence.getFilePath().isBlank()) deleteStorageObject(evidence.getFilePath(), accessToken);

        String url = AppConfig.REST_URL + "/task_progress_evidence_tbl?evidence_id=eq." + evidence.getEvidenceId();
        HttpRequest request = supabaseClient.createAuthenticatedRequest(url, accessToken).DELETE().build();
        HttpResponse<String> response = supabaseClient.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        validate(response, "Unable to delete progress evidence metadata");
    }

    private void uploadToStorage(String filePath, byte[] bytes, String contentType, String accessToken) throws Exception {
        String url = getSupabaseBaseUrl() + "/storage/v1/object/" + BUCKET_NAME + "/" + encodeStoragePath(filePath);
        HttpRequest request = supabaseClient.createAuthenticatedRequest(url, accessToken)
                .header("Content-Type", contentType)
                .header("x-upsert", "false")
                .POST(HttpRequest.BodyPublishers.ofByteArray(bytes))
                .build();
        HttpResponse<String> response = supabaseClient.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        validate(response, "Unable to upload progress evidence");
    }

    private UUID insertMetadata(TaskProgressEvidence evidence, String accessToken) throws Exception {
        JSONObject body = new JSONObject();
        body.put("submission_id", evidence.getSubmissionId().toString());
        body.put("file_name", evidence.getFileName());
        body.put("file_path", evidence.getFilePath());
        body.put("file_type", evidence.getFileType() == null ? JSONObject.NULL : evidence.getFileType());
        body.put("file_size", evidence.getFileSize());
        body.put("uploaded_by", evidence.getUploadedBy().toString());

        String url = AppConfig.REST_URL + "/task_progress_evidence_tbl";
        HttpRequest request = supabaseClient.createAuthenticatedRequest(url, accessToken)
                .header("Prefer", "return=representation")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        HttpResponse<String> response = supabaseClient.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        validate(response, "Unable to save progress evidence metadata");
        JSONArray array = new JSONArray(response.body());
        if (array.isEmpty()) throw new RuntimeException("Evidence metadata was saved but no record was returned.");
        return UUID.fromString(array.getJSONObject(0).getString("evidence_id"));
    }

    private void deleteStorageObject(String filePath, String accessToken) throws Exception {
        String url = getSupabaseBaseUrl() + "/storage/v1/object/" + BUCKET_NAME + "/" + encodeStoragePath(filePath);
        HttpRequest request = supabaseClient.createAuthenticatedRequest(url, accessToken).DELETE().build();
        HttpResponse<String> response = supabaseClient.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        validate(response, "Unable to delete progress evidence file");
    }

    private TaskProgressEvidence map(JSONObject row) {
        TaskProgressEvidence evidence = new TaskProgressEvidence();
        evidence.setEvidenceId(getUuid(row, "evidence_id"));
        evidence.setSubmissionId(getUuid(row, "submission_id"));
        evidence.setFileName(getNullableString(row, "file_name"));
        evidence.setFilePath(getNullableString(row, "file_path"));
        evidence.setFileType(getNullableString(row, "file_type"));
        evidence.setFileSize(row.optLong("file_size", 0));
        evidence.setUploadedBy(getUuid(row, "uploaded_by"));
        evidence.setUploadedAt(getDateTime(row, "uploaded_at"));
        return evidence;
    }

    private String getSupabaseBaseUrl() {
        String restUrl = AppConfig.REST_URL;
        int restIndex = restUrl.indexOf("/rest/v1");
        return restIndex >= 0 ? restUrl.substring(0, restIndex) : restUrl;
    }

    private String encodeStoragePath(String path) {
        String[] parts = path.split("/");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) result.append("/");
            result.append(URLEncoder.encode(parts[i], StandardCharsets.UTF_8).replace("+", "%20"));
        }
        return result.toString();
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) return "evidence_file";
        return fileName.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
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
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            throw new RuntimeException(message + ": " + response.body());
    }
}
