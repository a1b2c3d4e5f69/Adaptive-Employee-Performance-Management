package com.aepm.data.repository;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class NotificationRepository {

    private static final String URL =
        AppConfig.REST_URL + "/notification_tbl";

    private final SupabaseClient client =
        SupabaseClient.getInstance();

    private String encode(String value) {
        return URLEncoder.encode(
            value, StandardCharsets.UTF_8
        );
    }

    private String send(
            String token,
            String method,
            String url,
            JSONObject body
    ) throws Exception {

        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                "Please log in again."
            );
        }

        HttpRequest.Builder builder =
            client.createAuthenticatedRequest(url, token);

        // INSERT/PATCH do not need returned rows.
        // GET must still return JSON data.
        if ("POST".equals(method)
                || "PATCH".equals(method)) {
            builder.header("Prefer", "return=minimal");
        }

        HttpRequest request;

        switch (method) {
            case "GET" ->
                request = builder.GET().build();

            case "DELETE" ->
                request = builder.DELETE().build();

            default ->
                request = builder.method(
                    method,
                    HttpRequest.BodyPublishers.ofString(
                        body.toString()
                    )
                ).build();
        }

        HttpResponse<String> response =
            client.getHttpClient().send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {

            throw new Exception(
                "Notification error "
                    + response.statusCode()
                    + ": " + response.body()
            );
        }

        return response.body();
    }

    // Get notifications belonging to a user.
    public JSONArray findByRecipient(
            String token,
            String recipientId
    ) throws Exception {

        return new JSONArray(send(
            token,
            "GET",
            URL + "?select=*"
                + "&recipient_id=eq."
                + encode(recipientId)
                + "&order=created_at.desc"
                + "&limit=30",
            null
        ));
    }

    // Count unread notifications.
    public int countUnread(
            String token,
            String recipientId
    ) throws Exception {

        JSONArray rows = new JSONArray(send(
            token,
            "GET",
            URL + "?select=notification_id"
                + "&recipient_id=eq."
                + encode(recipientId)
                + "&is_read=eq.false",
            null
        ));

        return rows.length();
    }

    // Create a new notification.
    public void create(
            String token,
            String recipientId,
            String title,
            String message,
            String type,
            String taskId,
            Long escalationId
    ) throws Exception {

        JSONObject body = new JSONObject()
            .put("recipient_id", recipientId)
            .put("title", title)
            .put("message", message)
            .put("notification_type", type)
            .put("is_read", false);

        if (taskId != null && !taskId.isBlank()) {
            body.put("related_task_id", taskId);
        }

        if (escalationId != null) {
            body.put(
                "related_escalation_id",
                escalationId
            );
        }

        send(token, "POST", URL, body);
    }

    public boolean createOnce(
        String token,
        String recipientId,
        String title,
        String message,
        String type,
        String taskId,
        Long escalationId,
        String eventKey
    ) throws Exception {

        JSONObject body = new JSONObject()
            .put("p_recipient_id", recipientId)
            .put("p_title", title)
            .put("p_message", message)
            .put("p_type", type)
            .put("p_event_key", eventKey)
            .put("p_task_id",
                taskId == null || taskId.isBlank()
                    ? JSONObject.NULL : taskId)
            .put("p_escalation_id",
                escalationId == null
                    ? JSONObject.NULL : escalationId);

        String url = AppConfig.REST_URL
            + "/rpc/create_notification_once";

        HttpRequest request = client
            .createAuthenticatedRequest(url, token)
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    body.toString()
                )
            )
            .build();

        HttpResponse<String> response =
            client.getHttpClient().send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {

            throw new Exception(
                "Notification RPC failed: "
                    + response.statusCode()
                    + " " + response.body()
            );
        }

        return Boolean.parseBoolean(
            response.body().trim()
        );
    }

    // Mark one notification as read.
    public void markAsRead(
            String token,
            String notificationId,
            String recipientId
    ) throws Exception {

        JSONObject body = new JSONObject()
            .put("is_read", true)
            .put(
                "read_at",
                OffsetDateTime.now(ZoneOffset.UTC).toString()
            );

        send(
            token,
            "PATCH",
            URL + "?notification_id=eq."
                + encode(notificationId)
                + "&recipient_id=eq."
                + encode(recipientId),
            body
        );
    }

    // Mark all notifications as read.
    public void markAllAsRead(
            String token,
            String recipientId
    ) throws Exception {

        JSONObject body = new JSONObject()
            .put("is_read", true)
            .put(
                "read_at",
                OffsetDateTime.now(ZoneOffset.UTC).toString()
            );

        send(
            token,
            "PATCH",
            URL + "?recipient_id=eq."
                + encode(recipientId)
                + "&is_read=eq.false",
            body
        );
    }
}
