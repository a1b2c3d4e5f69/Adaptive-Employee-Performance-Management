package com.aepm.data.repository;

import com.aepm.config.AppConfig;
import com.aepm.data.supabase.SupabaseClient;
import com.aepm.model.*;
import com.google.gson.*;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

/** Repository for the anonymous report aggregate. All writes and reference access use guarded RPCs. */
public class AnonymousReportRepository {
    private final SupabaseClient client = SupabaseClient.getInstance();

    protected JsonElement rpc(String token, String function, JsonObject body) throws Exception {
        if (token == null || token.isBlank()) throw new IllegalStateException("Please sign in again.");
        HttpRequest request = client.createAuthenticatedRequest(AppConfig.REST_URL + "/rpc/" + function, token)
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
        final HttpResponse<String> response;
        try {
            response = client.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw interrupted;
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            // Never include the request, response body, reference token or report contents in errors/logs.
            String code = "";
            try {
                JsonElement error = JsonParser.parseString(response.body());
                if (error.isJsonObject() && error.getAsJsonObject().has("code"))
                    code = error.getAsJsonObject().get("code").getAsString();
            } catch (RuntimeException ignored) {
                // A proxy may return non-JSON. The HTTP status below still produces a clear error.
            }
            if ("42883".equals(code)) throw new IOException("Database function dependency missing (SQLSTATE 42883).");
            if ("42702".equals(code)) throw new IOException("Database function has an ambiguous column reference (SQLSTATE 42702).");
            if ("P0001".equals(code)) throw new IOException("Database validation/authorization rejected the anonymous reporting request.");
            if ("P0002".equals(code)) throw new IOException("Anonymous report unavailable.");
            if ("40001".equals(code)) throw new IOException("Report status changed. Reload before updating.");
            if ("22023".equals(code)) throw new IOException("Invalid report fields or workflow transition.");
            String message = switch (response.statusCode()) {
                case 401, 403 -> "Anonymous reporting access denied. Please check your session and role.";
                case 404 -> "Anonymous reporting endpoint is unavailable. Check the database migration.";
                case 409 -> "Report changed or a reference conflict occurred. Reload before retrying.";
                default -> "Anonymous reporting request failed (HTTP " + response.statusCode() + ").";
            };
            throw new IOException(message);
        }
        try {
            return JsonParser.parseString(response.body());
        } catch (RuntimeException malformed) {
            throw new IOException("Invalid anonymous reporting server response.");
        }
    }

    /** The deployed RPC generates and returns the employee receipt exactly once. */
    public AnonymousReportSubmission submit(String token, String category, String subject,
                                             String description, String evidencePath) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("p_report_category", category);
        body.addProperty("p_subject", subject);
        body.addProperty("p_report_description", description);
        body.addProperty("p_evidence_file", evidencePath);
        JsonObject row = singleRow(rpc(token, "aepm_submit_anonymous_report", body));
        String reference = text(row, "reference_number", false);
        if (reference.isBlank() || reference.codePointCount(0, reference.length()) > 50) throw malformedResponse();
        return new AnonymousReportSubmission(reference, report(row));
    }

    public Optional<AnonymousReport> track(String token, String reference) throws Exception {
        return optionalRow(rpc(token, "aepm_track_anonymous_report", referenceBody(reference)));
    }

    public AnonymousReportPage findAll(String token, AnonymousReportStatus status, String category,
            LocalDate from, LocalDate through, int limit, int offset) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("p_status", status == null ? null : status.value());
        body.addProperty("p_category", category);
        body.addProperty("p_start_date", from == null ? null : from.toString());
        body.addProperty("p_end_date", through == null ? null : through.toString());
        body.addProperty("p_limit", limit);
        body.addProperty("p_offset", offset);
        JsonArray result = rows(rpc(token, "aepm_list_anonymous_reports", body));
        List<AnonymousReport> records = new ArrayList<>();
        long total = result.isEmpty() ? 0 : count(object(result.get(0)), "total_count");
        for (JsonElement element : result) {
            JsonObject row = object(element);
            if (count(row, "total_count") != total) throw malformedResponse();
            records.add(report(row));
        }
        // Window-count columns disappear on an empty page; obtain the count without fetching all reports.
        if (result.isEmpty() && offset > 0) {
            JsonObject countBody = body.deepCopy();
            countBody.addProperty("p_limit", 1); countBody.addProperty("p_offset", 0);
            JsonArray first = rows(rpc(token, "aepm_list_anonymous_reports", countBody));
            if (!first.isEmpty()) total = count(object(first.get(0)), "total_count");
        }
        if (total < records.size() || records.size() > limit) throw malformedResponse();
        return new AnonymousReportPage(records, total, limit, offset);
    }

    public Optional<AnonymousReport> findById(String token, UUID reportId) throws Exception {
        return optionalRow(rpc(token, "aepm_get_anonymous_report", idBody(reportId)));
    }

    private UUID employeeReportId(String token, String reference) throws Exception {
        return track(token, reference).orElseThrow(() -> new IOException("Anonymous report unavailable.")).reportId();
    }

    public List<AnonymousReportMessage> employeeMessages(String token, String reference) throws Exception {
        UUID reportId = employeeReportId(token, reference);
        return messages(rpc(token, "aepm_employee_report_messages", referenceBody(reference)), reportId);
    }

    public List<AnonymousReportMessage> adminMessages(String token, UUID reportId) throws Exception {
        return messages(rpc(token, "aepm_admin_report_messages", idBody(reportId)), reportId);
    }

    public AnonymousReportMessage sendEmployeeMessage(String token, String reference, String content) throws Exception {
        UUID reportId = employeeReportId(token, reference);
        JsonObject body = referenceBody(reference);
        body.addProperty("p_message_text", content);
        return message(singleRow(rpc(token, "aepm_send_employee_report_message", body)), reportId);
    }

    public AnonymousReportMessage sendAdminMessage(String token, UUID reportId, String content) throws Exception {
        JsonObject body = idBody(reportId);
        body.addProperty("p_message_text", content);
        return message(singleRow(rpc(token, "aepm_send_admin_report_message", body)), reportId);
    }

    public Optional<AnonymousInvestigation> investigation(String token, UUID reportId) throws Exception {
        JsonArray result = rows(rpc(token, "aepm_get_anonymous_investigation", idBody(reportId)));
        if (result.isEmpty()) return Optional.empty();
        if (result.size() != 1) throw malformedResponse();
        return Optional.of(investigation(object(result.get(0))));
    }

    public List<AnonymousInvestigation> investigationHistory(String token, UUID reportId) throws Exception {
        JsonArray result = rows(rpc(token, "aepm_anonymous_investigation_history", idBody(reportId)));
        List<AnonymousInvestigation> history = new ArrayList<>();
        for (JsonElement row : result) history.add(investigation(object(row)));
        return List.copyOf(history);
    }

    public AnonymousInvestigation saveInvestigation(String token, UUID reportId, AnonymousReportStatus expected,
            AnonymousReportStatus next, String notes, UUID assignedAdmin, String resolution) throws Exception {
        AnonymousReport current = findById(token, reportId).orElseThrow(() -> new IOException("Anonymous report unavailable."));
        if (current.status() != expected) throw new IOException("Report status changed. Reload before updating.");
        JsonObject body = idBody(reportId);
        body.addProperty("p_investigation_note", notes);
        body.addProperty("p_new_status", next.value());
        body.addProperty("p_resolution", resolution);
        JsonObject saved = singleRow(rpc(token, "aepm_save_anonymous_investigation", body));
        long savedId = positiveId(saved, "investigation_id");
        // The save RPC returns a compact receipt, not private note/administrator fields.
        // Match the exact saved snapshot rather than racing a "latest" lookup.
        for (AnonymousInvestigation snapshot : investigationHistory(token, reportId))
            if (snapshot.investigationId() == savedId) return snapshot;
        throw new IOException("Saved investigation snapshot could not be retrieved.");
    }

    private static JsonArray rows(JsonElement value) throws IOException {
        if (value == null || !value.isJsonArray()) throw malformedResponse();
        return value.getAsJsonArray();
    }
    private static JsonObject singleRow(JsonElement value) throws IOException {
        JsonArray result = rows(value);
        if (result.size() != 1) throw malformedResponse();
        return object(result.get(0));
    }
    private static Optional<AnonymousReport> optionalRow(JsonElement value) throws IOException {
        JsonArray result = rows(value);
        if (result.isEmpty()) return Optional.empty();
        if (result.size() != 1) throw malformedResponse();
        return Optional.of(report(object(result.get(0))));
    }
    private static long count(JsonObject row, String key) throws IOException {
        try {
            JsonElement value = row.get(key);
            if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw malformedResponse();
            long count = value.getAsBigDecimal().longValueExact();
            if (count < 0) throw malformedResponse();
            return count;
        } catch (RuntimeException malformed) { throw malformedResponse(); }
    }

    private static JsonObject referenceBody(String reference) {
        JsonObject body = new JsonObject(); body.addProperty("p_reference_number", reference); return body;
    }
    private static JsonObject idBody(UUID id) {
        JsonObject body = new JsonObject(); body.addProperty("p_report_id", id.toString()); return body;
    }
    private static IOException malformedResponse() { return new IOException("Malformed anonymous reporting data."); }
    private static JsonObject object(JsonElement value) throws IOException {
        if (value == null || !value.isJsonObject()) throw malformedResponse();
        return value.getAsJsonObject();
    }
    private static JsonArray array(JsonObject row, String key) throws IOException {
        JsonElement value = row.get(key);
        if (value == null || !value.isJsonArray()) throw malformedResponse();
        return value.getAsJsonArray();
    }
    private static String text(JsonObject row, String key, boolean optional) throws IOException {
        JsonElement value = row.get(key);
        if (value == null || value.isJsonNull()) {
            if (optional) return null;
            throw malformedResponse();
        }
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) throw malformedResponse();
        return value.getAsString();
    }
    private static long positiveId(JsonObject row, String key) throws IOException {
        try {
            JsonElement value = row.get(key);
            if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw malformedResponse();
            long id = value.getAsBigDecimal().longValueExact();
            if (id <= 0) throw malformedResponse();
            return id;
        } catch (RuntimeException malformed) { throw malformedResponse(); }
    }
    private static boolean readFlag(JsonObject row) throws IOException {
        JsonElement value = row.get("is_read");
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) throw malformedResponse();
        return value.getAsBoolean();
    }
    private static AnonymousReport report(JsonObject row) throws IOException {
        try {
            return new AnonymousReport(UUID.fromString(text(row, "report_id", false)),
                    text(row, "report_category", false), text(row, "subject", false), text(row, "report_description", false),
                    text(row, "evidence_file", true), AnonymousReportStatus.fromValue(text(row, "case_status", false)),
                    OffsetDateTime.parse(text(row, "submitted_at", false)), OffsetDateTime.parse(text(row, "updated_at", false)),
                    text(row, "resolution", true));
        } catch (RuntimeException malformed) { throw malformedResponse(); }
    }
    private static AnonymousReportMessage message(JsonObject row, UUID reportId) throws IOException {
        try {
            return new AnonymousReportMessage(positiveId(row, "message_id"), reportId,
                    AnonymousReportMessage.SenderType.fromValue(text(row, "sender_type", false)),
                    text(row, "message_text", false), OffsetDateTime.parse(text(row, "sent_at", false)), readFlag(row));
        } catch (RuntimeException malformed) { throw malformedResponse(); }
    }
    private static List<AnonymousReportMessage> messages(JsonElement value, UUID reportId) throws IOException {
        if (value == null || !value.isJsonArray()) throw malformedResponse();
        List<AnonymousReportMessage> rows = new ArrayList<>();
        for (JsonElement row : value.getAsJsonArray()) rows.add(message(object(row), reportId));
        rows.sort(Comparator.comparing(AnonymousReportMessage::sentAt).thenComparing(AnonymousReportMessage::messageId));
        return List.copyOf(rows);
    }
    private static AnonymousInvestigation investigation(JsonObject row) throws IOException {
        try {
            String assigned = text(row, "administrator_id", true);
            String previous = text(row, "previous_status", true);
            String status = text(row, "new_status", true);
            return new AnonymousInvestigation(positiveId(row, "investigation_id"),
                    UUID.fromString(text(row, "report_id", false)), text(row, "investigation_note", true),
                    assigned == null ? null : UUID.fromString(assigned),
                    previous == null ? null : AnonymousReportStatus.fromValue(previous),
                    status == null ? null : AnonymousReportStatus.fromValue(status), text(row, "resolution", true),
                    OffsetDateTime.parse(text(row, "created_at", false)), OffsetDateTime.parse(text(row, "updated_at", false)));
        } catch (RuntimeException malformed) { throw malformedResponse(); }
    }
}
