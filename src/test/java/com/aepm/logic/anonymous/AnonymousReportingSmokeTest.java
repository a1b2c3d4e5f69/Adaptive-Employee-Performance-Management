package com.aepm.logic.anonymous;

import com.aepm.data.repository.AnonymousReportRepository;
import com.aepm.model.*;
import com.aepm.util.SessionManager;
import com.google.gson.*;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

/** Standalone backend checks with a fake RPC transport. No network, database or UI is used. */
public final class AnonymousReportingSmokeTest {
    private static final UUID REPORT_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final String REFERENCE = "A".repeat(43);
    private static int checks;
    private interface Checked { void run() throws Exception; }

    public static void main(String[] args) throws Exception {
        var auth = SessionManager.class.getDeclaredField("authSession"); auth.setAccessible(true);
        var user = SessionManager.class.getDeclaredField("currentUser"); user.setAccessible(true);
        Object oldAuth = auth.get(null), oldUser = user.get(null);
        try {
            FakeRepository repository = new FakeRepository();
            var reports = new AnonymousReportService(repository);
            var tracking = new AnonymousReportTrackingService(repository);
            var chat = new AnonymousReportChatService(repository);
            var investigations = new AnonymousInvestigationService(repository);
            session(3, true);
            Set<String> references = new HashSet<>();
            for (int i = 0; i < 2; i++) {
                var submitted = reports.submit(" Safety ", " A concern ", " Details ", null);
                check(submitted.reference().matches("[a-f0-9]{48}"), "Invalid reference format");
                check(references.add(submitted.reference()), "Reference collision");
                check(!submitted.toString().contains(submitted.reference()), "Receipt toString leaks reference");
                check(submitted.report().status() == AnonymousReportStatus.SUBMITTED, "Wrong default status");
                check(repository.body.get("p_report_category").getAsString().equals("Safety"), "Input not trimmed");
                check(repository.body.keySet().equals(Set.of("p_report_category","p_subject","p_report_description","p_evidence_file")), "Submission sends identity");
            }
            reject(IllegalArgumentException.class, () -> reports.submit(" ", "Title", "Details", null));
            reject(IllegalArgumentException.class, () -> reports.submit("Safety", "\t", "Details", null));
            reject(IllegalArgumentException.class, () -> reports.submit("Safety", "Title", "", null));
            reject(IllegalArgumentException.class, () -> reports.submit("Safety", "X".repeat(201), "Details", null));
            reject(IllegalArgumentException.class, () -> reports.submit("Safety", "Title", "Details", "X".repeat(2049)));
            check(tracking.track(REFERENCE).orElseThrow().reportId().equals(REPORT_ID), "Tracking wrong report");
            check(repository.body.keySet().equals(Set.of("p_reference_number")), "Tracking binds employee identity");
            repository.next = new JsonArray();
            check(tracking.track(REFERENCE).isEmpty(), "Unknown reference not handled safely");
            check(tracking.track(" Legacy #01 ").isPresent(), "Legacy references must remain usable");
            check(repository.body.get("p_reference_number").getAsString().equals(" Legacy #01 "), "Legacy reference was trimmed");
            reject(IllegalArgumentException.class, () -> tracking.track(" "));
            reject(IllegalArgumentException.class, () -> tracking.track("X".repeat(51)));
            reject(IllegalArgumentException.class, () -> chat.sendEmployee(REFERENCE, " "));
            var message = chat.sendEmployee(REFERENCE, " Follow up ");
            check(message.senderType() == AnonymousReportMessage.SenderType.ANONYMOUS_EMPLOYEE, "Wrong sender");
            check(message.messageId() > 9007199254740992L, "bigint message ID lost precision");
            check(!message.read(), "Read flag not parsed");
            check(repository.body.keySet().equals(Set.of("p_reference_number", "p_message_text")), "Employee chat sends identity/sender");
            check(repository.body.get("p_message_text").getAsString().equals("Follow up"), "Message not trimmed");
            var history = chat.employeeHistory(REFERENCE);
            check(history.size() == 2 && history.get(0).content().equals("Earlier"), "History not ordered");
            reject(IllegalStateException.class, () -> reports.review(null,null,null,null,50,0));
            reject(IllegalStateException.class, () -> investigations.get(REPORT_ID));
            reject(IllegalStateException.class, () -> chat.sendAdministrator(REPORT_ID,"reply"));
            session(2, true);
            reject(IllegalStateException.class, () -> reports.submit("Safety","Title","Details",null));
            reject(IllegalStateException.class, () -> tracking.track(REFERENCE));
            reject(IllegalStateException.class, () -> reports.detail(REPORT_ID));
            session(1, true);
            var page = reports.review(AnonymousReportStatus.SUBMITTED,"Safety",LocalDate.of(2026,1,1),LocalDate.of(2026,2,1),10,20);
            check(page.totalCount() == 21 && page.offset() == 20 && page.rows().size() == 1, "Invalid pagination");
            check(repository.body.get("p_start_date").getAsString().equals("2026-01-01"), "Wrong date filter");
            check(repository.body.get("p_end_date").getAsString().equals("2026-02-01"), "Wrong inclusive end filter");
            check(repository.body.get("p_status").getAsString().equals("Submitted"), "Wrong status filter");
            check(repository.body.get("p_category").getAsString().equals("Safety"), "Wrong category filter");
            repository.next=new JsonArray();
            check(reports.review(null,null,null,null,10,50).totalCount()==21,"Empty-page window count fallback failed");
            reject(IllegalArgumentException.class, () -> reports.review(null,null,null,null,101,0));
            reject(IllegalArgumentException.class, () -> reports.review(null,null,null,null,10,-1));
            reject(IllegalArgumentException.class, () -> reports.review(null,null,LocalDate.of(2026,2,1),LocalDate.of(2026,1,1),10,0));
            check(reports.detail(REPORT_ID).orElseThrow().description().equals("Details"), "Admin detail failed");
            String safe = new GsonBuilder().registerTypeAdapter(java.time.OffsetDateTime.class,
                    (JsonSerializer<java.time.OffsetDateTime>) (value, type, context) -> new JsonPrimitive(value.toString()))
                    .create().toJson(reports.detail(REPORT_ID).orElseThrow());
            check(!safe.contains("employee_id") && !safe.contains("email") && !safe.contains("SECRET"), "Identity or secret included in DTO");
            check(chat.sendAdministrator(REPORT_ID,"Reply").senderType() == AnonymousReportMessage.SenderType.ADMINISTRATOR, "Admin sender failed");
            check(chat.administratorHistory(REPORT_ID).size() == 2, "Admin history failed");
            check(investigations.get(REPORT_ID).isEmpty(), "Missing investigation not handled");
            check(investigations.history(REPORT_ID).isEmpty(), "Empty investigation history not handled");
            check(AnonymousReportStatus.fromValue("Under Investigation") == AnonymousReportStatus.INVESTIGATING, "Live status not mapped");
            var saved = investigations.save(REPORT_ID,AnonymousReportStatus.SUBMITTED,AnonymousReportStatus.UNDER_REVIEW,"Private notes",null,null);
            check(saved.status() == AnonymousReportStatus.UNDER_REVIEW, "Investigation save failed");
            reject(IllegalArgumentException.class, () -> investigations.save(REPORT_ID,AnonymousReportStatus.SUBMITTED,AnonymousReportStatus.RESOLVED,"",null,"Outcome"));
            reject(IllegalArgumentException.class, () -> investigations.save(REPORT_ID,AnonymousReportStatus.INVESTIGATING,AnonymousReportStatus.RESOLVED,"",null," "));
            reject(IllegalArgumentException.class, () -> investigations.save(REPORT_ID,AnonymousReportStatus.CLOSED,AnonymousReportStatus.CLOSED,"",null,"Outcome"));
            for (var status : AnonymousReportStatus.values()) {
                for (var next : AnonymousReportStatus.values()) {
                    check(status.canTransitionTo(next) == (status == next || next.ordinal() == status.ordinal()+1), "Workflow error");
                }
            }
            repository.next = new JsonObject();
            reject(IOException.class, () -> reports.detail(REPORT_ID));
            JsonObject malformed = report(); malformed.addProperty("submitted_at", "invalid"); repository.next = array(malformed);
            reject(IOException.class, () -> reports.detail(REPORT_ID));
            JsonObject badCount = report(); badCount.addProperty("total_count",-1); repository.next = array(badCount);
            reject(IOException.class, () -> reports.review(null,null,null,null,10,0));
            session(1, false);
            reject(IllegalStateException.class, () -> reports.detail(REPORT_ID));
            user.set(null,null); auth.set(null,null);
            reject(IllegalStateException.class, () -> reports.detail(REPORT_ID));
            System.out.println("PASS: " + checks + " anonymous reporting backend checks; no live Supabase requests.");
        } finally { auth.set(null,oldAuth); user.set(null,oldUser); }
    }
    private static void session(long role, boolean active) throws Exception {
        var auth = SessionManager.class.getDeclaredField("authSession"); auth.setAccessible(true);
        var current = SessionManager.class.getDeclaredField("currentUser"); current.setAccessible(true);
        User user = new User(); user.setRoleId(role); user.setAccountStatus(active ? "Active" : "Inactive");
        current.set(null,user);
        auth.set(null,new AuthSession("test-token-only",null,UUID.randomUUID().toString(),null));
    }
    private static JsonObject report() {
        JsonObject row = new JsonObject();
        row.addProperty("report_id",REPORT_ID.toString()); row.addProperty("report_category","Safety");
        row.addProperty("subject","A concern"); row.addProperty("report_description","Details");
        row.addProperty("case_status","Submitted"); row.addProperty("submitted_at","2026-11-04T00:00:00Z");
        row.addProperty("updated_at","2026-11-04T00:00:00Z");
        // Unexpected server fields are deliberately excluded by the repository's explicit projection.
        row.addProperty("employee_id","SECRET"); row.addProperty("email","SECRET"); row.addProperty("reference","SECRET");
        return row;
    }
    private static JsonObject message(String content, String sender, int hour) {
        JsonObject row = new JsonObject(); row.addProperty("message_id",9007199254740993L + hour);
        row.addProperty("sender_type",sender.equals("ADMINISTRATOR") ? "Administrator" : "Anonymous Reporter");
        row.addProperty("message_text",content); row.addProperty("is_read",false); row.addProperty("sent_at","2026-11-04T0"+hour+":00:00Z"); return row;
    }
    private static JsonArray array(JsonObject row) { JsonArray rows=new JsonArray(); rows.add(row); return rows; }
    private static class FakeRepository extends AnonymousReportRepository {
        JsonObject body; JsonElement next; JsonObject snapshot; int receipts;
        @Override protected JsonElement rpc(String token, String function, JsonObject payload) {
            body = payload.deepCopy();
            if (next != null) { var result = next; next = null; return result; }
            if (function.equals("aepm_submit_anonymous_report")) {
                JsonObject receipt=report(); receipt.addProperty("reference_number",String.format("%048x",++receipts));
                return array(receipt);
            }
            if (function.equals("aepm_list_anonymous_reports")) {
                JsonObject row=report(); row.addProperty("total_count",21); return array(row);
            }
            if (function.endsWith("_report_messages")) {
                JsonArray rows = new JsonArray(); rows.add(message("Later","ADMINISTRATOR",2));
                rows.add(message("Earlier","ANONYMOUS_EMPLOYEE",1)); return rows;
            }
            if (function.contains("send_")) return array(message("Reply", function.contains("employee") ? "ANONYMOUS_EMPLOYEE" : "ADMINISTRATOR",1));
            if (function.equals("aepm_get_anonymous_investigation")) return snapshot==null?new JsonArray():array(snapshot.deepCopy());
            if (function.equals("aepm_anonymous_investigation_history")) return snapshot==null?new JsonArray():array(snapshot.deepCopy());
            if (function.equals("aepm_save_anonymous_investigation")) {
                snapshot = new JsonObject(); snapshot.addProperty("report_id",REPORT_ID.toString());
                snapshot.addProperty("investigation_id",1); snapshot.addProperty("investigation_note","Private notes"); snapshot.addProperty("new_status","Under Review");
                snapshot.addProperty("previous_status","Submitted"); snapshot.addProperty("created_at","2026-11-04T00:00:00Z");
                snapshot.addProperty("updated_at","2026-11-04T00:00:00Z");
                JsonObject receipt=snapshot.deepCopy(); receipt.remove("investigation_note"); return array(receipt);
            }
            return array(report());
        }
    }
    private static void reject(Class<? extends Exception> type, Checked work) throws Exception {
        try { work.run(); } catch (Exception failure) {
            if (type.isInstance(failure)) { checks++; return; } throw failure;
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }
    private static void check(boolean condition, String message) { checks++; if (!condition) throw new AssertionError(message); }
}
