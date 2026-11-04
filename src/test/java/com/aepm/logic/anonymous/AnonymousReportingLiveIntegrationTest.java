package com.aepm.logic.anonymous;

import com.aepm.config.AppConfig;
import com.aepm.data.repository.AnonymousReportRepository;
import com.aepm.data.supabase.SupabaseClient;
import com.aepm.logic.auth.AuthService;
import com.aepm.model.*;
import com.aepm.util.SessionManager;
import com.google.gson.*;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.regex.*;

/** Explicitly opt-in LIVE integration test. Creates and closes one labeled report; never deletes data.
 * Credentials come from environment variables or the existing README; no secrets are logged or saved.
 * Run manually with --live. It is not executed automatically by Maven.
 */
public final class AnonymousReportingLiveIntegrationTest {
    private static final String CATEGORY = "AEPM Integration Test";
    private static final Set<String> FORBIDDEN = Set.of("user_id","employee_id","username","email","full_name",
            "reference_number","reference_hash","reference","p_reference","sender_admin_id");
    private record Account(AuthSession session, User user) {}
    private record Observation(String audience, String endpoint, JsonElement response) {}
    private record Outcome(int number, String description, boolean passed, String detail) {}
    private record Raw(int status, String code, JsonElement json) {
        boolean denied() {
            if ((status == 401 || status == 403) && "42501".equals(code)) return true;
            if (status != 400 || !"P0001".equals(code) || !json.isJsonObject() || !json.getAsJsonObject().has("message")) return false;
            return Set.of("Administrator access required.","Employee access required.","Authentication required.")
                    .contains(json.getAsJsonObject().get("message").getAsString());
        }
    }
    private interface Checked { void run() throws Exception; }
    private final String runId = UUID.randomUUID().toString();
    private final String privateNote = "AEPM LIVE INTEGRATION TEST PRIVATE NOTE " + runId;
    private final String resolution = "AEPM LIVE INTEGRATION TEST completed; no real allegation. " + runId;
    private final List<Observation> observations = new ArrayList<>();
    private final List<Outcome> outcomes = new ArrayList<>();
    private final Map<String,String[]> credentials = new HashMap<>();
    private Account employee, administrator, manager;
    private UUID reportId;
    private String reference;
    private AnonymousReportSubmission submission;
    private AnonymousReport tracked;
    private AnonymousReportPage listed;
    private AnonymousReport adminDetail;
    private AnonymousInvestigation saved;
    private List<AnonymousInvestigation> history;
    private List<AnonymousReportMessage> employeeMessages;
    private final AnonymousReportRepository repository = new AnonymousReportRepository() {
        @Override protected JsonElement rpc(String token, String function, JsonObject body) throws Exception {
            JsonElement result = super.rpc(token, function, body);
            observations.add(new Observation(audience(),function,result.deepCopy()));
            return result;
        }
    };
    private final AnonymousReportService reports = new AnonymousReportService(repository);
    private final AnonymousReportTrackingService tracking = new AnonymousReportTrackingService(repository);
    private final AnonymousReportChatService chat = new AnonymousReportChatService(repository);
    private final AnonymousInvestigationService investigations = new AnonymousInvestigationService(repository);

    public static void main(String[] args) throws Exception {
        if (!Arrays.asList(args).contains("--live") && !Arrays.asList(args).contains("--diagnose"))
            throw new IllegalArgumentException("Refusing live writes without --live.");
        AnonymousReportingLiveIntegrationTest test = new AnonymousReportingLiveIntegrationTest();
        test.loadCredentials();
        if (Arrays.asList(args).contains("--diagnose")) {
            try { test.diagnose(); } finally { quietly(SessionManager::clearSession); }
            return;
        }
        try { test.execute(); }
        finally { quietly(SessionManager::clearSession); }
        test.writeReport();
        long failures = test.outcomes.stream().filter(row -> !row.passed()).count();
        System.out.println("LIVE SUMMARY: " + (36-failures) + "/36 PASS; failures=" + failures);
        System.out.println("Sanitized results: docs/anonymous-reporting-live-integration-results.md");
        if (failures > 0) System.exit(1);
    }

    private void diagnose() throws Exception {
        employee=login("employee",3); administrator=login("admin",1); manager=login("manager",2);
        System.out.println("Real employee/admin/manager authentication and role checks: PASS");
        Raw schema=send(administrator,"GET",AppConfig.REST_URL+"/",null);
        if(schema.json().isJsonObject() && schema.json().getAsJsonObject().has("paths")) {
            for(var path:schema.json().getAsJsonObject().getAsJsonObject("paths").entrySet()) {
                if(!path.getKey().contains("aepm_") || !path.getKey().contains("anonymous")) continue;
                Set<String> parameters=new TreeSet<>();
                Matcher names=Pattern.compile("p_[a-z_]+").matcher(path.getValue().toString());
                while(names.find()) parameters.add(names.group());
                System.out.println("OPENAPI "+path.getKey()+" parameters="+parameters);
            }
        } else System.out.println("OPENAPI status="+schema.status()+" code="+safeCode(schema.code()));
        diagnostic("employee submit route (blank description, no write)",rpc(employee,"aepm_submit_anonymous_report",
                body("p_report_category",CATEGORY,"p_subject","AEPM route diagnostic","p_report_description","","p_evidence_file","")));
        diagnostic("employee tracking route",rpc(employee,"aepm_track_anonymous_report",body("p_reference_number",newReference())));
        diagnostic("manager admin list",rpc(manager,"aepm_list_anonymous_reports",new JsonObject()));
        diagnostic("anon hash helper",rpc(null,"aepm_anonymous_reference_hash",body("p_reference",newReference())));
        diagnostic("admin hash helper",rpc(administrator,"aepm_anonymous_reference_hash",body("p_reference",newReference())));
        diagnostic("anon transition helper",rpc(null,"aepm_anonymous_valid_transition",body("p_old_status","Submitted","p_new_status","Under Review")));
    }
    private static void diagnostic(String label,Raw raw) {
        Set<String> parameters=new TreeSet<>();
        Matcher names=Pattern.compile("p_[a-z_]+").matcher(raw.json().toString());
        while(names.find()) parameters.add(names.group());
        String message="";
        if(raw.json().isJsonObject() && raw.json().getAsJsonObject().has("message"))
            message=raw.json().getAsJsonObject().get("message").getAsString();
        String reason=switch(message) {
            case "Administrator access required.","Employee access required.","Authentication required." -> "explicit-role-denial";
            case "Report description is required." -> "expected-validation";
            default -> "not-printed";
        };
        System.out.println("DIAGNOSTIC "+label+": HTTP="+raw.status()+" code="+safeCode(raw.code())
                +" reason="+reason+" parameter-names="+parameters+" response-type="
                +(raw.json().isJsonArray()?"array":raw.json().isJsonObject()?"object":raw.json().isJsonNull()?"null":"scalar"));
    }

    private void execute() {
        step(1,"Employee login",() -> employee=login("employee",3));
        step(2,"Submit category, subject, description and optional evidence",() -> {
            activate(employee);
            submission=reports.submit(CATEGORY,"AEPM LIVE INTEGRATION TEST " + runId,
                    "Authorized backend integration test only; no real allegation. Run " + runId,
                    "integration-test/no-upload/"+runId+".txt");
            reportId=submission.report().reportId(); reference=submission.reference();
            require(reportId!=null && CATEGORY.equals(submission.report().category()),"Submission mapping failed");
            require(submission.report().subject().endsWith(runId),"Subject was not saved");
            require(submission.report().description().contains(runId),"Description was not saved");
            require(submission.report().evidencePath().endsWith(runId+".txt"),"Evidence path was not saved");
        });
        step(3,"Submission succeeds",() -> require(submission!=null && reportId!=null,"Submission prerequisite failed"));
        step(4,"Employee receives plaintext secret reference",() -> {
            needReference(); require(reference.matches("[a-f0-9]{48}"),"Secure reference format failed");
            require(!submission.toString().contains(reference),"Receipt toString leaks secret");
        });
        step(5,"Submission RPC does not expose reference_hash",() -> {
            needReference(); auditObservation(findObservation("aepm_submit_anonymous_report"));
        });
        step(6,"Employee tracks using returned reference",() -> {
            activate(employee); needReference(); tracked=tracking.track(reference).orElseThrow();
            require(tracked.reportId().equals(reportId),"Tracking returned a different report");
        });
        step(7,"Tracked status is Submitted",() -> require(tracked!=null && tracked.status()==AnonymousReportStatus.SUBMITTED,"Initial tracked status mismatch"));
        step(8,"Invalid reference returns no private data",() -> {
            activate(employee);
            require(tracking.track(newReference()).isEmpty(),"Unknown reference returned data");
            Raw malformed=rpc(employee,"aepm_track_anonymous_report",body("p_reference_number","invalid-reference"));
            require(malformed.status()==200 && malformed.json().isJsonArray() && malformed.json().getAsJsonArray().isEmpty(),"Malformed reference did not return an empty result");
        });
        step(9,"Employee sends anonymous message",() -> {
            activate(employee); needReference();
            var message=chat.sendEmployee(reference,"AEPM LIVE INTEGRATION TEST employee follow-up " + runId);
            require(message.senderType()==AnonymousReportMessage.SenderType.ANONYMOUS_EMPLOYEE,"Incorrect employee sender");
            require(message.reportId().equals(reportId),"Message linked to another report");
        });
        step(10,"Employee retrieves ordered conversation",() -> {
            activate(employee); needReference(); employeeMessages=chat.employeeHistory(reference);
            require(employeeMessages.size()==1,"Employee conversation size mismatch");
            require(employeeMessages.get(0).content().contains(runId),"Follow-up message missing");
            ordered(employeeMessages);
        });
        step(11,"Report request/response contains no employee identity",() -> {
            needReference(); JsonElement raw=findObservation("aepm_submit_anonymous_report").response(); auditObservation(findObservation("aepm_submit_anonymous_report"));
            JsonObject row=raw.getAsJsonArray().get(0).getAsJsonObject();
            require(row.keySet().equals(Set.of("report_id","report_category","subject","report_description",
                    "evidence_file","case_status","submitted_at","updated_at","reference_number")),"Unexpected report fields");
            String rawText=raw.toString();
            for(String identity:List.of(employee.user().getUserId(),employee.user().getEmail()))
                if(identity!=null && !identity.isBlank()) require(!rawText.contains(identity),"Employee identity value in report");
        });
        step(12,"Administrator login",() -> administrator=login("admin",1));
        step(13,"Administrator lists reports with backend filters",() -> {
            activate(administrator); listed=reports.review(AnonymousReportStatus.SUBMITTED,CATEGORY,null,null,100,0);
            require(listed!=null,"List response missing");
        });
        step(14,"Submitted report appears in list",() -> {
            needReport(); require(listed!=null && listed.rows().stream().anyMatch(row->row.reportId().equals(reportId)),"New report missing from list");
        });
        step(15,"Administrator retrieves report detail",() -> {
            activate(administrator); needReport(); adminDetail=reports.detail(reportId).orElseThrow();
            require(adminDetail.reportId().equals(reportId),"Detail report mismatch");
        });
        step(16,"Administrator report responses exclude identity and secrets",() -> {
            require(adminDetail!=null && listed!=null,"Admin retrieval prerequisite failed");
            audit(findObservation("aepm_list_anonymous_reports").response());
            audit(findObservation("aepm_get_anonymous_report").response());
        });
        step(17,"Administrator retrieves report messages",() -> {
            activate(administrator); needReport(); var messages=chat.administratorHistory(reportId);
            require(messages.size()==1 && messages.get(0).senderType()==AnonymousReportMessage.SenderType.ANONYMOUS_EMPLOYEE,"Admin could not read follow-up");
            ordered(messages);
        });
        step(18,"Administrator replies",() -> {
            activate(administrator); needReport();
            require(chat.sendAdministrator(reportId,"AEPM LIVE INTEGRATION TEST admin reply " + runId).senderType()==AnonymousReportMessage.SenderType.ADMINISTRATOR,"Admin sender mismatch");
            activate(employee); var messages=chat.employeeHistory(reference);
            require(messages.size()==2 && messages.stream().anyMatch(m->m.senderType()==AnonymousReportMessage.SenderType.ADMINISTRATOR),"Employee cannot see admin reply");
            ordered(messages);
        });
        step(19,"Administrator saves private investigation note",() -> {
            activate(administrator); needReport(); saved=investigations.save(reportId,AnonymousReportStatus.SUBMITTED,
                    AnonymousReportStatus.SUBMITTED,privateNote,adminId(),null);
            require(privateNote.equals(saved.notes()),"Private note not saved");
        });
        step(20,"Submitted -> Under Review",() -> move(AnonymousReportStatus.SUBMITTED,AnonymousReportStatus.UNDER_REVIEW,"review",null));
        step(21,"Under Review -> Under Investigation",() -> move(AnonymousReportStatus.UNDER_REVIEW,AnonymousReportStatus.INVESTIGATING,"investigate",null));
        step(22,"Under Investigation -> Resolved with resolution",() -> move(AnonymousReportStatus.INVESTIGATING,AnonymousReportStatus.RESOLVED,"resolved",resolution));
        step(23,"Employee sees published resolution",() -> {
            activate(employee); needReference(); var result=tracking.track(reference).orElseThrow();
            require(result.status()==AnonymousReportStatus.RESOLVED && resolution.equals(result.resolution()),"Published resolution/status missing");
            audit(findObservation("aepm_track_anonymous_report").response());
        });
        step(24,"Resolved -> Closed",() -> move(AnonymousReportStatus.RESOLVED,AnonymousReportStatus.CLOSED,"closed",resolution));
        step(25,"Investigation history is preserved",() -> {
            activate(administrator); needReport(); history=investigations.history(reportId);
            require(history.size()==5,"Expected five preserved investigation snapshots");
            require(history.stream().map(AnonymousInvestigation::investigationId).distinct().count()==5,"Investigation IDs overwritten");
            require(privateNote.equals(history.get(0).notes()),"First private note was overwritten");
            require(history.get(4).status()==AnonymousReportStatus.CLOSED,"Final history status mismatch");
            require(investigations.get(reportId).orElseThrow().investigationId()==history.get(4).investigationId(),"Latest investigation mismatch");
        });
        step(26,"Manager login",() -> manager=login("manager",2));
        step(27,"Manager admin-list RPC denied",() -> mustDeny(rpc(manager,"aepm_list_anonymous_reports",new JsonObject())));
        step(28,"Manager detail RPC denied",() -> mustDeny(rpc(manager,"aepm_get_anonymous_report",idBody(targetId()))));
        step(29,"Manager admin messaging RPC denied",() -> {
            mustDeny(rpc(manager,"aepm_admin_report_messages",idBody(targetId())));
            mustDeny(rpc(manager,"aepm_send_admin_report_message",body("p_report_id",UUID.randomUUID().toString(),"p_message_text","AEPM denied permission probe")));
        });
        step(30,"Manager investigation RPCs denied",() -> {
            mustDeny(rpc(manager,"aepm_get_anonymous_investigation",idBody(targetId())));
            mustDeny(rpc(manager,"aepm_anonymous_investigation_history",idBody(targetId())));
            mustDeny(rpc(manager,"aepm_save_anonymous_investigation",investigationBody(UUID.randomUUID())));
        });
        step(31,"Employee direct report SELECT denied",() -> mustDeny(table(employee,"GET","anonymous_report_tbl",null,targetId())));
        step(32,"Employee direct INSERT/UPDATE/DELETE denied on all three tables",() -> directWritesDenied(employee));
        step(33,"Manager direct reads/writes denied on all three tables",() -> {
            for(String table:tables()) mustDeny(table(manager,"GET",table,null,targetId()));
            directWritesDenied(manager);
        });
        step(34,"Anon/public table and all module RPC access denied",() -> {
            for(String table:tables()) mustDeny(table(null,"GET",table,null,targetId()));
            Map<String,JsonObject> calls=new LinkedHashMap<>();
            calls.put("aepm_submit_anonymous_report",body("p_report_category",CATEGORY,"p_subject","AEPM denied permission probe","p_report_description",""));
            calls.put("aepm_track_anonymous_report",body("p_reference_number",reference==null?newReference():reference));
            calls.put("aepm_employee_report_messages",body("p_reference_number",reference==null?newReference():reference));
            calls.put("aepm_send_employee_report_message",body("p_reference_number",newReference(),"p_message_text","AEPM denied permission probe"));
            calls.put("aepm_list_anonymous_reports",new JsonObject());
            calls.put("aepm_get_anonymous_report",idBody(targetId()));
            calls.put("aepm_admin_report_messages",idBody(targetId()));
            calls.put("aepm_send_admin_report_message",body("p_report_id",UUID.randomUUID().toString(),"p_message_text","AEPM denied permission probe"));
            calls.put("aepm_get_anonymous_investigation",idBody(targetId()));
            calls.put("aepm_anonymous_investigation_history",idBody(targetId()));
            calls.put("aepm_save_anonymous_investigation",investigationBody(UUID.randomUUID()));
            for(var call:calls.entrySet()) mustDeny(rpc(null,call.getKey(),call.getValue()));
        });
        step(35,"No identity, plaintext reference or reference_hash in observed RPC responses",() -> {
            needReference(); require(observations.size()>=20,"Insufficient live RPC observations");
            for(var observation:observations) auditObservation(observation);
        });
        step(36,"Investigation notes are administrator-only",() -> {
            activate(employee); needReference(); tracking.track(reference).orElseThrow(); chat.employeeHistory(reference);
            mustDeny(rpc(employee,"aepm_get_anonymous_investigation",idBody(reportId)));
            mustDeny(rpc(employee,"aepm_anonymous_investigation_history",idBody(reportId)));
            mustDeny(table(employee,"GET","investigation_tbl",null,reportId));
            for(var observation:observations) if(!observation.audience().equals("ADMIN")) {
                require(!observation.response().toString().contains(privateNote),"Private note exposed outside administrator responses");
                require(!containsKey(observation.response(),"investigation_note"),"Private note field exposed outside administrator responses");
            }
            require(history!=null && history.stream().anyMatch(row->privateNote.equals(row.notes())),"Admin note visibility prerequisite failed");
        });
    }

    private void move(AnonymousReportStatus expected,AnonymousReportStatus next,String suffix,String outcome) throws Exception {
        activate(administrator); needReport();
        saved=investigations.save(reportId,expected,next,privateNote+" / "+suffix,adminId(),outcome);
        require(saved.status()==next && saved.previousStatus()==expected,"Workflow history mapping mismatch");
        require(reports.detail(reportId).orElseThrow().status()==next,"Report status did not advance");
    }
    private UUID adminId() { return UUID.fromString(administrator.user().getUserId()); }
    private void step(int number,String description,Checked check) {
        try { check.run(); outcomes.add(new Outcome(number,description,true,"Verified live")); }
        catch(Exception | AssertionError failure) {
            String detail=failure instanceof SafeFailure ? failure.getMessage() : classify(failure);
            outcomes.add(new Outcome(number,description,false,detail));
        }
        var result=outcomes.get(outcomes.size()-1);
        System.out.println(number+" "+(result.passed()?"PASS":"FAIL")+" "+description+(result.passed()?"":" ["+result.detail()+"]"));
    }
    private Account login(String role,long expectedRole) throws Exception {
        String[] credential=credentials.get(role);
        require(credential!=null,"Credentials unavailable for required role");
        final User[] result=new User[1];
        quietly(()->result[0]=new AuthService().login(credential[0],credential[1]));
        require(result[0]!=null && result[0].getRoleId()==expectedRole,"Account role does not match required test role");
        require("Active".equalsIgnoreCase(result[0].getAccountStatus()),"Test account is not active");
        return new Account(SessionManager.getAuthSession(),result[0]);
    }
    private static void activate(Account account) throws Exception {
        require(account!=null,"Required account login prerequisite failed");
        quietly(()->SessionManager.setSession(account.session(),account.user()));
    }
    private static void quietly(Checked work) throws Exception {
        PrintStream previous=System.out;
        try(var sink=new PrintStream(OutputStream.nullOutputStream())) {
            System.setOut(sink); work.run();
        } finally { System.setOut(previous); }
    }
    private String audience() {
        var user=SessionManager.getCurrentUser();
        return user==null?"ANON":user.getRoleId()==1?"ADMIN":user.getRoleId()==3?"EMPLOYEE":"MANAGER";
    }
    private void needReference() throws Exception { require(reference!=null && reportId!=null,"Successful submission prerequisite failed"); }
    private void needReport() throws Exception { require(reportId!=null,"Successful submission prerequisite failed"); }
    private UUID targetId() { return reportId==null?UUID.randomUUID():reportId; }
    private Observation findObservation(String endpoint) throws Exception {
        for(int i=observations.size()-1;i>=0;i--) if(observations.get(i).endpoint().equals(endpoint)) return observations.get(i);
        throw new SafeFailure("Required RPC observation missing");
    }
    private void auditObservation(Observation observation) throws Exception {
        if (observation.audience().equals("EMPLOYEE") && observation.endpoint().equals("aepm_submit_anonymous_report")) {
            require(observation.response().isJsonArray() && observation.response().getAsJsonArray().size()==1,"Unexpected submission receipt shape");
            JsonObject row=observation.response().getAsJsonArray().get(0).getAsJsonObject().deepCopy();
            require(row.has("reference_number") && row.get("reference_number").getAsString().equals(reference),"Employee receipt reference mismatch");
            row.remove("reference_number"); // The one explicitly permitted plaintext response is the employee submission receipt.
            audit(row); return;
        }
        audit(observation.response());
    }
    private void audit(JsonElement json) throws Exception {
        if(json.isJsonObject()) for(var entry:json.getAsJsonObject().entrySet()) {
            require(!FORBIDDEN.contains(entry.getKey()),"Forbidden field in RPC response"); audit(entry.getValue());
        }
        else if(json.isJsonArray()) for(var element:json.getAsJsonArray()) audit(element);
        if(reference!=null) {
            String raw=json.toString();
            require(!raw.contains(reference),"Plaintext reference present in RPC response");
            String digest=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(reference.getBytes(StandardCharsets.UTF_8)));
            require(!raw.contains(digest),"Reference hash value present in RPC response");
        }
    }
    private static boolean containsKey(JsonElement json,String key) {
        if(json.isJsonObject()) {
            if(json.getAsJsonObject().has(key)) return true;
            return json.getAsJsonObject().entrySet().stream().anyMatch(entry->containsKey(entry.getValue(),key));
        }
        if(json.isJsonArray()) for(var value:json.getAsJsonArray()) if(containsKey(value,key)) return true;
        return false;
    }
    private static void ordered(List<AnonymousReportMessage> messages) throws Exception {
        for(int i=1;i<messages.size();i++) {
            var previous=messages.get(i-1);var next=messages.get(i);
            require(!previous.sentAt().isAfter(next.sentAt()),"Conversation timestamps out of order");
            if(previous.sentAt().equals(next.sentAt())) require(previous.messageId()<next.messageId(),"Conversation ID ordering mismatch");
        }
    }
    private Raw rpc(Account account,String endpoint,JsonObject body) throws Exception {
        Raw raw=send(account,"POST",AppConfig.REST_URL+"/rpc/"+endpoint,body);
        observations.add(new Observation(account==null?"ANON":account.user().getRoleId()==1?"ADMIN":account.user().getRoleId()==3?"EMPLOYEE":"MANAGER",endpoint,raw.json()));
        return raw;
    }
    private Raw table(Account account,String method,String table,JsonObject body,UUID id) throws Exception {
        String query=method.equals("POST")?"":"?report_id=eq."+id+(method.equals("GET")?"&select=*&limit=1":"");
        return send(account,method,AppConfig.REST_URL+"/"+table+query,body);
    }
    private Raw send(Account account,String method,String url,JsonObject body) throws Exception {
        require(account==null || account.session()!=null,"Account token prerequisite failed");
        var client=SupabaseClient.getInstance();
        var builder=account==null?client.createRequest(url):client.createAuthenticatedRequest(url,account.session().getAccessToken());
        builder.timeout(Duration.ofSeconds(30));
        if(method.equals("GET")) builder.GET();
        else if(method.equals("DELETE")) builder.DELETE();
        else builder.method(method,HttpRequest.BodyPublishers.ofString(body==null?"{}":body.toString()));
        HttpResponse<String> response=client.getHttpClient().send(builder.build(),HttpResponse.BodyHandlers.ofString());
        JsonElement json;
        try { json=JsonParser.parseString(response.body()); }
        catch(RuntimeException invalid) { json=JsonNull.INSTANCE; }
        String code="";
        if(json.isJsonObject() && json.getAsJsonObject().has("code")) code=json.getAsJsonObject().get("code").getAsString();
        return new Raw(response.statusCode(),code,json);
    }
    private void mustDeny(Raw raw) throws Exception {
        require(raw.denied(),"Expected DB permission denial; HTTP="+raw.status()+", code="+safeCode(raw.code()));
    }
    private void directWritesDenied(Account account) throws Exception {
        require(account!=null,"Required role login prerequisite failed");
        UUID nonexistent=UUID.randomUUID();
        for(String table:tables()) {
            JsonObject insert;
            JsonObject update;
            if(table.equals("anonymous_report_tbl")) {
                insert=body("report_id",nonexistent.toString(),"reference_hash","invalid-permission-probe",
                        "report_category",CATEGORY,"subject","AEPM denied permission probe","report_description","Authorization test","case_status","Submitted");
                update=body("subject","AEPM denied permission probe");
            } else if(table.equals("anonymous_message_tbl")) {
                insert=body("report_id",nonexistent.toString(),"sender_type","INVALID_PERMISSION_PROBE","message_text","Authorization test");
                update=body("message_text","AEPM denied permission probe");
            } else {
                insert=body("report_id",nonexistent.toString(),"new_status","INVALID_PERMISSION_PROBE","investigation_note","Authorization test");
                update=body("investigation_note","AEPM denied permission probe");
            }
            // Invalid inserts cannot persist even if grants/RLS are broken. Updates/deletes target an unused UUID.
            mustDeny(table(account,"POST",table,insert,nonexistent));
            mustDeny(table(account,"PATCH",table,update,nonexistent));
            mustDeny(table(account,"DELETE",table,null,nonexistent));
        }
    }
    private static List<String> tables() { return List.of("anonymous_report_tbl","anonymous_message_tbl","investigation_tbl"); }
    private static JsonObject investigationBody(UUID id) {
        return body("p_report_id",id.toString(),"p_new_status","Under Review","p_investigation_note","AEPM denied permission probe");
    }
    private static JsonObject idBody(UUID id) { return body("p_report_id",id.toString()); }
    private static JsonObject body(String... pairs) {
        JsonObject body=new JsonObject();for(int i=0;i<pairs.length;i+=2) body.addProperty(pairs[i],pairs[i+1]);return body;
    }
    private static String newReference() {
        byte[] bytes=new byte[32];new java.security.SecureRandom().nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    private static void require(boolean condition,String message) throws SafeFailure { if(!condition) throw new SafeFailure(message); }
    private static final class SafeFailure extends Exception { SafeFailure(String message) { super(message); } }
    private static String safeCode(String code) { return code!=null && code.matches("[A-Z0-9_]{1,30}")?code:"unavailable"; }
    private static String classify(Throwable failure) {
        String message=Objects.toString(failure.getMessage(),"").toLowerCase(Locale.ROOT);
        if(message.contains("42883")) return "RPC dependency missing (42883); migration 003 required";
        if(message.contains("42702")) return "RPC ambiguous column (42702); migration 003 required";
        if(message.contains("invalid api key")) return "Configuration: Supabase API key rejected";
        if(message.contains("invalid login") || message.contains("invalid email or password")) return "Authentication: credentials rejected";
        if(message.contains("profile") || message.contains("user profile")) return "Authentication/profile retrieval failed";
        if(message.contains("access denied")) return "RPC authorization/role check denied";
        if(message.contains("endpoint is unavailable")) return "RPC/schema-cache endpoint unavailable";
        if(message.contains("malformed anonymous") || message.contains("invalid anonymous reporting server")) return "RPC data mapping/JSON contract mismatch";
        if(message.contains("http ")) return "RPC request failed; inspect safe diagnostic probes";
        return "Backend failure ("+failure.getClass().getSimpleName()+"); no sensitive error content logged";
    }
    private void loadCredentials() throws Exception {
        for(String role:List.of("employee","admin","manager")) {
            String prefix="AEPM_TEST_"+role.toUpperCase(Locale.ROOT)+"_";
            String email=System.getenv(prefix+"EMAIL"),password=System.getenv(prefix+"PASSWORD");
            if(email!=null && password!=null) credentials.put(role,new String[]{email,password});
        }
        if(Files.isRegularFile(Path.of("README.md"))) {
            String source=Files.readString(Path.of("README.md"));
            Matcher matcher=Pattern.compile("(?im)^\\s*(manager|admin|employee)\\s*\\R\\s*(\\S+)\\s+ps:(\\S+)").matcher(source);
            while(matcher.find()) credentials.putIfAbsent(matcher.group(1).toLowerCase(Locale.ROOT),new String[]{matcher.group(2),matcher.group(3)});
        }
    }
    private void writeReport() throws Exception {
        StringBuilder report=new StringBuilder("# Live Anonymous Reporting integration results\n\n");
        report.append("Executed: ").append(OffsetDateTime.now(ZoneId.of("Asia/Singapore"))).append("\n\n");
        report.append("Live Supabase requests via existing AuthService, anonymous services/repository, SupabaseClient and AppConfig. No fake transport. No migrations or UI changes executed.\n\n");
        report.append("Test report ID: ").append(reportId==null?"not created":reportId).append(". The labeled test report is retained; no rows were deleted. Secret reference and credentials are intentionally omitted.\n\n");
        report.append("| # | Test | Result | Evidence |\n|---|---|---|---|\n");
        for(var row:outcomes) report.append("| ").append(row.number()).append(" | ").append(row.description()).append(" | ")
                .append(row.passed()?"PASS":"FAIL").append(" | ").append(row.detail()).append(" |\n");
        report.append("\nCaptured module RPC responses inspected: ").append(observations.size()).append(". No raw responses, access tokens, passwords or reference values are persisted.\n\n");
        report.append("Scope: storage privacy is checked through identity-free application payloads/responses and the deployed module contract; a database-owner catalog/storage audit is not available using application JWTs. These results cover the RPCs exercised, not arbitrary undiscovered endpoints or infrastructure logs.\n");
        Files.createDirectories(Path.of("docs"));
        Files.writeString(Path.of("docs/anonymous-reporting-live-integration-results.md"),report.toString());
    }
}
