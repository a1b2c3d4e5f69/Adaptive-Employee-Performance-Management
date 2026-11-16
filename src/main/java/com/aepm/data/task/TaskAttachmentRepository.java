package com.aepm.data.task;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;
import com.aepm.model.TaskAttachment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.nio.file.Files;

import java.time.Duration;
import java.time.OffsetDateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class TaskAttachmentRepository {


    private static final long MAX_FILE_BYTES =
            10L * 1024L * 1024L;


    private final SupabaseClient client =
            SupabaseClient.getInstance();


    // =========================================================
    // UPLOAD
    // =========================================================

    public TaskAttachment upload(
            UUID taskId,
            UUID uploadedBy,
            File file,
            String accessToken
    ) throws Exception {


        if (file == null) {

            throw new IllegalArgumentException(
                    "No file selected."
            );
        }


        byte[] bytes =
                Files.readAllBytes(
                        file.toPath()
                );


        if (
                bytes.length < 1
                        || bytes.length > MAX_FILE_BYTES
        ) {

            throw new IllegalArgumentException(
                    "File must be between 1 byte and 10 MB."
            );
        }


        String originalName =
                file.getName();


        String safeName =
                sanitizeFilename(
                        originalName
                );


        String storagePath =
                taskId
                        + "/"
                        + UUID.randomUUID()
                        + "-"
                        + safeName;


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

        String storageUrl =
                AppConfig.STORAGE_URL
                        + "/object/task-attachments/"
                        + storagePath;


        HttpRequest uploadRequest =
                client
                        .createAuthenticatedRequest(
                                storageUrl,
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
                                HttpRequest
                                        .BodyPublishers
                                        .ofByteArray(
                                                bytes
                                        )
                        )
                        .build();


        HttpResponse<String> uploadResponse =
                client
                        .getHttpClient()
                        .send(
                                uploadRequest,
                                HttpResponse
                                        .BodyHandlers
                                        .ofString()
                        );


        validate(
                uploadResponse,
                "Unable to upload task attachment"
        );


        // =====================================================
        // SAVE METADATA
        // =====================================================

        JSONObject body =
                new JSONObject();

        body.put(
                "task_id",
                taskId.toString()
        );

        body.put(
                "file_name",
                originalName
        );

        body.put(
                "storage_path",
                storagePath
        );

        body.put(
                "mime_type",
                mimeType
        );

        body.put(
                "file_size",
                bytes.length
        );

        body.put(
                "uploaded_by",
                uploadedBy.toString()
        );


        String tableUrl =
                AppConfig.REST_URL
                        + "/task_attachment_tbl";


        HttpRequest metadataRequest =
                client
                        .createAuthenticatedRequest(
                                tableUrl,
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


        HttpResponse<String> metadataResponse =
                client
                        .getHttpClient()
                        .send(
                                metadataRequest,
                                HttpResponse
                                        .BodyHandlers
                                        .ofString()
                        );


        validate(
                metadataResponse,
                "Unable to save task attachment"
        );


        JSONArray array =
                new JSONArray(
                        metadataResponse.body()
                );


        return map(
                array.getJSONObject(
                        0
                )
        );
    }


    // =========================================================
    // GET ATTACHMENTS
    // =========================================================

    public List<TaskAttachment> getByTask(
            UUID taskId,
            String accessToken
    ) throws Exception {


        String url =
                AppConfig.REST_URL
                        + "/task_attachment_tbl"
                        + "?select=*"
                        + "&task_id=eq."
                        + taskId
                        + "&order=created_at.asc";


        HttpRequest request =
                client
                        .createAuthenticatedRequest(
                                url,
                                accessToken
                        )
                        .GET()
                        .build();


        HttpResponse<String> response =
                client
                        .getHttpClient()
                        .send(
                                request,
                                HttpResponse
                                        .BodyHandlers
                                        .ofString()
                        );


        validate(
                response,
                "Unable to load task attachments"
        );


        JSONArray array =
                new JSONArray(
                        response.body()
                );


        List<TaskAttachment> attachments =
                new ArrayList<>();


        for (
                int i = 0;
                i < array.length();
                i++
        ) {

            attachments.add(
                    map(
                            array.getJSONObject(
                                    i
                            )
                    )
            );
        }


        return attachments;
    }


    // =========================================================
    // DOWNLOAD
    // =========================================================

    public byte[] download(
            TaskAttachment attachment,
            String accessToken
    ) throws Exception {


        String url =
                AppConfig.STORAGE_URL
                        + "/object/authenticated/"
                        + "task-attachments/"
                        + attachment.getStoragePath();


        HttpRequest request =
                client
                        .createAuthenticatedRequest(
                                url,
                                accessToken
                        )
                        .GET()
                        .build();


        HttpResponse<byte[]> response =
                client
                        .getHttpClient()
                        .send(
                                request,
                                HttpResponse
                                        .BodyHandlers
                                        .ofByteArray()
                        );


        if (
                response.statusCode() < 200
                        || response.statusCode() >= 300
        ) {

            throw new RuntimeException(
                    "Unable to download file. HTTP "
                            + response.statusCode()
            );
        }


        return response.body();
    }


    // =========================================================
    // MAPPING
    // =========================================================

    private TaskAttachment map(
            JSONObject json
    ) {


        TaskAttachment attachment =
                new TaskAttachment();


        attachment.setAttachmentId(
                UUID.fromString(
                        json.getString(
                                "attachment_id"
                        )
                )
        );


        attachment.setTaskId(
                UUID.fromString(
                        json.getString(
                                "task_id"
                        )
                )
        );


        attachment.setFileName(
                json.optString(
                        "file_name",
                        ""
                )
        );


        attachment.setStoragePath(
                json.optString(
                        "storage_path",
                        ""
                )
        );


        attachment.setMimeType(
                json.optString(
                        "mime_type",
                        ""
                )
        );


        attachment.setFileSize(
                json.optLong(
                        "file_size",
                        0L
                )
        );


        String uploadedBy =
                json.optString(
                        "uploaded_by",
                        null
                );


        if (
                uploadedBy != null
                        && !uploadedBy.isBlank()
        ) {

            attachment.setUploadedBy(
                    UUID.fromString(
                            uploadedBy
                    )
            );
        }


        String createdAt =
                json.optString(
                        "created_at",
                        null
                );


        if (
                createdAt != null
                        && !createdAt.isBlank()
        ) {

            attachment.setCreatedAt(
                    OffsetDateTime.parse(
                            createdAt
                    )
            );
        }


        return attachment;
    }


    private String sanitizeFilename(
            String filename
    ) {

        return filename
                .replaceAll(
                        "[^a-zA-Z0-9._-]",
                        "_"
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
                            + " (HTTP "
                            + response.statusCode()
                            + "): "
                            + response.body()
            );
        }
    }
}