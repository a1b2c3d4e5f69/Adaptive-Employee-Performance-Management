package com.aepm.ui.anonymous;

import com.aepm.data.repository.AnonymousReportRepository;
import com.aepm.logic.anonymous.*;
import com.aepm.model.*;
import com.aepm.ui.common.AnonymousUiController;
import com.aepm.ui.employee.*;
import com.aepm.ui.admin.*;
import com.aepm.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.control.*;
import java.io.IOException;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/** Loads real FXML/controllers and real services against an in-memory repository. No live requests/windows. */
public final class AnonymousUiSmokeTest {
    private record View(Parent root,AnonymousUiController controller) {}
    private static final Fixture repository=new Fixture();
    private static Scene scene;
    private static View current;
    private static String destination;
    private static int checks;
    private static Path screenshots;
    private static AnonymousReportService reports() {return new AnonymousReportService(repository);}
    private static AnonymousReportTrackingService tracking() {return new AnonymousReportTrackingService(repository);}
    private static AnonymousReportChatService chat() {return new AnonymousReportChatService(repository);}
    private static AnonymousInvestigationService investigations() {return new AnonymousInvestigationService(repository);}
    private static class EmployeeReport extends EmployeeAnonymousReportController {
        EmployeeReport() {super(reports(),tracking());}
        @Override protected <T> void navigate(String path,Consumer<T> configure) {route(path,configure);}
    }
    private static class EmployeeChat extends EmployeeAnonymousChatController {
        EmployeeChat() {super(chat(),tracking());}
        @Override protected <T> void navigate(String path,Consumer<T> configure) {route(path,configure);}
    }
    private static class AdminList extends AdminAnonymousReportsController {
        AdminList() {super(reports());}
        @Override protected <T> void navigate(String path,Consumer<T> configure) {route(path,configure);}
    }
    private static class AdminDetail extends AdminAnonymousReportDetailController {
        AdminDetail() {super(reports(),chat(),investigations());}
        @Override protected <T> void navigate(String path,Consumer<T> configure) {route(path,configure);}
    }
    @SuppressWarnings("unchecked") private static <T> void route(String path,Consumer<T> configure) {
        try {
            destination=path;View view=load(path);configure.accept((T)view.controller());attach(view);
        } catch(Exception error) {throw new RuntimeException(error);}
    }
    private static View load(String path) throws Exception {
        FXMLLoader loader=new FXMLLoader(AnonymousUiSmokeTest.class.getResource(path));
        loader.setControllerFactory(type->{
            if(type==EmployeeAnonymousReportController.class) return new EmployeeReport();
            if(type==EmployeeAnonymousChatController.class) return new EmployeeChat();
            if(type==AdminAnonymousReportsController.class) return new AdminList();
            if(type==AdminAnonymousReportDetailController.class) return new AdminDetail();
            throw new IllegalArgumentException("Unexpected controller");
        });
        return new View(loader.load(),loader.getController());
    }
    private static void attach(View view) {
        current=view;if(scene==null) scene=new Scene(view.root(),1280,900);else scene.setRoot(view.root());
        view.root().resize(1280,900);view.root().applyCss();view.root().layout();
    }
    private static <T> T fx(Callable<T> work) throws Exception {
        CompletableFuture<T> done=new CompletableFuture<>();
        Platform.runLater(()->{try {done.complete(work.call());}catch(Throwable error){done.completeExceptionally(error);}});
        return done.get(15,TimeUnit.SECONDS);
    }
    private static void idle() throws Exception {
        long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
        while(System.nanoTime()<until) {
            if(fx(()->!current.controller().isBusy())) {fx(()->null);return;}
            Thread.sleep(15);
        }
        throw new AssertionError("UI service operation timed out");
    }
    private static void session(long role) {
        User user=new User();String id=UUID.randomUUID().toString();user.setUserId(id);user.setRoleId(role);user.setAccountStatus("Active");
        SessionManager.setSession(new AuthSession("fixture-token",null,id,null),user);
    }
    private static <T extends Node> T node(String id,Class<T> type) {return type.cast(current.root().lookup("#"+id));}
    private static TextField field(String id) {return node(id,TextField.class);}
    private static TextArea area(String id) {return node(id,TextArea.class);}
    private static Label label(String id) {return node(id,Label.class);}
    private static Button button(String id) {return node(id,Button.class);}
    @SuppressWarnings("unchecked") private static <T> ComboBox<T> combo(String id) {return (ComboBox<T>)node(id,ComboBox.class);}
    @SuppressWarnings("unchecked") private static <T> TableView<T> table(String id) {return (TableView<T>)node(id,TableView.class);}
    private static void check(boolean condition,String message) {checks++;if(!condition) throw new AssertionError(message);}
    private static void snapshot(String file) throws Exception {
        if(screenshots==null) return;
        fx(()->{
            current.root().applyCss();current.root().layout();
            for(String id:List.of("trackedSubject","trackedCategory","trackedStatus","trackedSubmitted","trackedUpdated","trackedResolution",
                    "caseLabel","categoryLabel","statusLabel","submittedLabel","updatedLabel","publishedResolutionLabel","pageLabel")) {
                Node candidate=current.root().lookup("#"+id);
                if(candidate instanceof Label label) check(!javafx.scene.paint.Color.WHITE.equals(label.getTextFill()),"Unreadable white-on-white data label: "+id);
            }
            var image=current.root().snapshot(new SnapshotParameters(),null);
            int w=(int)image.getWidth(),h=(int)image.getHeight();int[] pixels=new int[w*h];
            image.getPixelReader().getPixels(0,0,w,h,javafx.scene.image.PixelFormat.getIntArgbInstance(),pixels,0,w);
            var output=new java.awt.image.BufferedImage(w,h,java.awt.image.BufferedImage.TYPE_INT_ARGB);
            output.setRGB(0,0,w,h,pixels,0,w);javax.imageio.ImageIO.write(output,"png",screenshots.resolve(file).toFile());return null;
        });
    }
    public static void main(String[] args) throws Exception {
        if(args.length>0) {screenshots=Path.of(args[0]);Files.createDirectories(screenshots);}
        Platform.startup(()->Platform.setImplicitExit(false));
        try {
            fx(()->{session(3);attach(load("/fxml/employee/employeeAnonymousReport.fxml"));return null;});
            fx(()->{button("submitButton").fire();check(label("messageLabel").getText().contains("Category"),"Required category not validated");check(repository.submissions==0,"Blank form submitted");return null;});
            fx(()->{combo("categoryCombo").setValue("Harassment");field("subjectField").setText("UI test report");area("descriptionArea").setText("UI description");field("evidenceField").setText("evidence.txt");button("submitButton").fire();return null;});idle();
            String reference=fx(()->{String value=field("referenceDisplay").getText();check(!value.isBlank(),"Employee receipt missing");check(!button("copyReferenceButton").isDisabled(),"Copy reference unavailable");check(field("subjectField").getText().isEmpty(),"Successful form not cleared");return value;});
            UUID id=repository.referenceIds.get(reference);check(id!=null,"Submitted reference not linked to fixture report");snapshot("employee-report.png");
            fx(()->{button("trackButton").fire();return null;});idle();
            fx(()->{check(label("trackedStatus").getText().equals("Submitted"),"Tracking status wrong");check(!label("trackedResolution").isVisible(),"Missing resolution should remain hidden");check(label("trackedSubject").getText().equals("UI test report"),"Tracking subject wrong");field("trackingReferenceField").setText("unknown reference");check(button("openChatButton").isDisabled(),"Stale chat handoff remained enabled");button("trackButton").fire();return null;});idle();
            fx(()->{check(label("messageLabel").getText().equals("Report not found or invalid reference."),"Unsafe invalid-reference error");field("trackingReferenceField").setText(reference);button("trackButton").fire();return null;});idle();
            View reportView=current;
            fx(()->{button("openChatButton").fire();check(destination.endsWith("employeeAnonymousChat.fxml"),"Chat navigation target wrong");return null;});idle();
            fx(()->{check(field("referenceField").getText().equals(reference),"Reference handoff failed");check(((TextField)reportView.root().lookup("#referenceDisplay")).getText().isEmpty(),"Receipt was retained in detached page");button("sendButton").fire();check(label("messageLabel").getText().contains("Message is required"),"Blank chat validation missing");area("messageArea").setText("Employee follow-up");button("sendButton").fire();return null;});idle();
            fx(()->{check(table("conversationTable").getItems().size()==1,"Employee message missing");check(table("conversationTable").getColumns().size()==3,"Unsafe conversation columns");check(area("messageArea").getText().isEmpty(),"Sent composer not cleared");return null;});snapshot("employee-chat.png");
            fx(()->{button("backButton").fire();return null;});idle();
            fx(()->{check(destination.endsWith("employeeAnonymousReport.fxml"),"Chat back target wrong");check(field("trackingReferenceField").getText().equals(reference),"Back lost reference");return null;});
            repository.seed(30);
            fx(()->{session(1);attach(load("/fxml/admin/adminAnonymousReports.fxml"));return null;});idle();
            fx(()->{check(table("reportsTable").getItems().size()==25,"Page size incorrect");check(!button("nextButton").isDisabled(),"Next page unavailable");check(table("reportsTable").getColumns().size()==5,"Unsafe report columns");button("nextButton").fire();return null;});idle();
            fx(()->{check(table("reportsTable").getItems().size()==6,"Next page data wrong");button("previousButton").fire();return null;});idle();
            fx(()->{check(table("reportsTable").getItems().size()==25,"Previous page failed");combo("categoryFilter").setValue("Harassment");combo("statusFilter").setValue("Submitted");node("startDate",DatePicker.class).setValue(LocalDate.now().minusDays(1));node("endDate",DatePicker.class).setValue(LocalDate.now().plusDays(1));button("applyFiltersButton").fire();return null;});idle();
            fx(()->{check(table("reportsTable").getItems().size()==1,"Combined filters failed");check(button("nextButton").isDisabled(),"Filtered pagination not reset");node("startDate",DatePicker.class).setValue(LocalDate.now().plusDays(2));button("applyFiltersButton").fire();check(label("messageLabel").getText().contains("Start date"),"Invalid date range accepted");button("clearFiltersButton").fire();return null;});idle();
            fx(()->{check(table("reportsTable").getItems().size()==25,"Clear filters failed");node("startDate",DatePicker.class).getEditor().setText("invalid-date");button("applyFiltersButton").fire();check(label("messageLabel").getText().contains("valid submitted dates"),"Typed invalid date was accepted");node("startDate",DatePicker.class).getEditor().clear();combo("categoryFilter").getEditor().setText("No matches");button("applyFiltersButton").fire();return null;});idle();
            fx(()->{check(table("reportsTable").getItems().isEmpty(),"Empty filter retains stale rows");check(button("detailButton").isDisabled(),"Detail enabled without selection");combo("categoryFilter").setValue("Harassment");combo("categoryFilter").getEditor().setText("Harassment");button("applyFiltersButton").fire();return null;});idle();snapshot("admin-list.png");
            fx(()->{table("reportsTable").getSelectionModel().select(0);button("detailButton").fire();return null;});idle();
            fx(()->{check(destination.endsWith("adminAnonymousReportDetail.fxml"),"Detail navigation target wrong");check(label("subjectLabel").getText().equals("UI test report"),"Detail subject wrong");check(!area("descriptionArea").isEditable(),"Description should be read-only");check(table("historyTable").getColumns().size()==5,"History columns incorrect");area("investigationNoteArea").setText("Private UI note");area("replyArea").setText("Administrator reply");button("replyButton").fire();return null;});idle();
            fx(()->{check(table("conversationTable").getItems().size()==2,"Admin reply missing");check(area("investigationNoteArea").getText().equals("Private UI note"),"Chat refresh discarded private draft");button("saveInvestigationButton").fire();return null;});idle();
            fx(()->{check(table("historyTable").getItems().size()==1,"Private note history missing");check(combo("newStatusCombo").getItems().equals(List.of(AnonymousReportStatus.SUBMITTED,AnonymousReportStatus.UNDER_REVIEW)),"Invalid status choices offered");return null;});
            for(var status:List.of(AnonymousReportStatus.UNDER_REVIEW,AnonymousReportStatus.INVESTIGATING)) {
                fx(()->{combo("newStatusCombo").setValue(status);area("investigationNoteArea").setText("Private note "+status.value());button("saveInvestigationButton").fire();return null;});idle();
                fx(()->{check(label("statusLabel").getText().equals(status.value()),"Status transition failed");return null;});
            }
            fx(()->{combo("newStatusCombo").setValue(AnonymousReportStatus.RESOLVED);area("resolutionArea").clear();button("saveInvestigationButton").fire();check(label("messageLabel").getText().contains("Resolution is required"),"Resolution validation missing");area("resolutionArea").setText("Safe published outcome");button("saveInvestigationButton").fire();return null;});idle();
            fx(()->{check(label("publishedResolutionLabel").getText().equals("Safe published outcome"),"Published resolution missing");check(table("historyTable").getItems().size()==4,"History was overwritten");return null;});snapshot("admin-detail.png");
            fx(()->{session(3);attach(load("/fxml/employee/employeeAnonymousReport.fxml"));((EmployeeAnonymousReportController)current.controller()).setTrackingReference(reference);return null;});idle();
            fx(()->{check(label("trackedResolution").getText().equals("Safe published outcome"),"Employee cannot see resolution");check(label("trackedResolution").isVisible(),"Published resolution should be visible");check(current.root().lookup("#historyTable")==null && current.root().lookup("#investigationNoteArea")==null,"Private investigation exposed to employee");session(1);View detail=load("/fxml/admin/adminAnonymousReportDetail.fxml");attach(detail);((AdminAnonymousReportDetailController)detail.controller()).setReportId(id);return null;});idle();
            fx(()->{combo("newStatusCombo").setValue(AnonymousReportStatus.CLOSED);button("saveInvestigationButton").fire();return null;});idle();
            fx(()->{check(label("statusLabel").getText().equals("Closed"),"Close failed");check(table("historyTable").getItems().size()==5,"Final history count wrong");check(button("saveInvestigationButton").isDisabled(),"Closed investigation still editable");button("backButton").fire();return null;});idle();
            fx(()->{check(destination.endsWith("adminAnonymousReports.fxml"),"Detail back navigation failed");session(3);attach(load("/fxml/employee/employeeAnonymousChat.fxml"));((EmployeeAnonymousChatController)current.controller()).setReference(reference);return null;});idle();
            repository.rejectClosedMessages=true;
            fx(()->{check(label("caseLabel").getText().contains("Closed"),"Closed chat status missing");check(!button("sendButton").isDisabled(),"UI should defer message acceptance to backend");area("messageArea").setText("Message after hardening");button("sendButton").fire();return null;});idle();
            fx(()->{String message=label("messageLabel").getText();check(message.contains("no longer accept"),"Closed rejection not readable");check(!message.contains("SQL") && !message.contains("reference_hash"),"Backend error details leaked");check(!area("messageArea").getText().isEmpty(),"Failed message was discarded");return null;});
            for(String file:List.of("employee/employeeAnonymousReport.fxml","employee/employeeAnonymousChat.fxml","admin/adminAnonymousReports.fxml","admin/adminAnonymousReportDetail.fxml")) {
                int before=repository.calls.get();fx(()->{session(2);attach(load("/fxml/"+file));if(current.controller() instanceof AdminAnonymousReportDetailController detail) detail.setReportId(id);return null;});
                fx(()->{check(node("pageContent",javafx.scene.layout.VBox.class).isDisabled(),"Manager page access not blocked");check(repository.calls.get()==before,"Manager triggered a backend request");return null;});
            }
            fx(()->{session(3);attach(load("/fxml/employee/employeeAnonymousReport.fxml"));combo("categoryCombo").setValue("Other");field("subjectField").setText("Delayed result");area("descriptionArea").setText("Test stale response");repository.blockSubmit=true;button("submitButton").fire();return null;});
            check(repository.entered.await(5,TimeUnit.SECONDS),"Deferred request did not start");fx(()->{session(2);repository.release.countDown();return null;});idle();
            fx(()->{check(field("referenceDisplay").getText().isEmpty(),"Secret displayed after session change");check(node("pageContent",javafx.scene.layout.VBox.class).isDisabled(),"Changed session remained usable");return null;});
            navigationChecks();check(repository.offFxCalls.get()>0,"No async service calls observed");
            System.out.println("PASS: "+checks+" JavaFX FXML/controller checks; real services with in-memory repository, no live requests or visible windows.");
        } finally {fx(()->{if(scene!=null) scene.setRoot(new javafx.scene.layout.Pane());SessionManager.clearSession();return null;});Platform.exit();}
    }
    private static void navigationChecks() throws Exception {
        for(String role:List.of("employee","admin")) try(var files=Files.list(Path.of("src/main/resources/fxml",role))) {
            for(Path file:files.filter(p->p.toString().endsWith(".fxml") && !p.getFileName().toString().contains("Anonymous")).toList()) {
                String source=Files.readString(file);String action=role.equals("employee")?"handleAnonymousReport":"handleAnonymousReports";
                if(!source.contains("#"+action)) continue;
                var matcher=java.util.regex.Pattern.compile("fx:controller=\"([^\"]+)\"").matcher(source);
                check(matcher.find(),"Missing controller");Class<?> controller=Class.forName(matcher.group(1));
                check(Arrays.stream(controller.getDeclaredMethods()).anyMatch(method->method.getName().equals(action)),"Anonymous sidebar handler missing");
            }
        }
        try(var files=Files.walk(Path.of("src/main/resources/fxml/manager"))) {
            for(Path file:files.filter(Files::isRegularFile).toList()) check(!Files.readString(file).contains("Anonymous"),"Anonymous menu exposed to manager");
        }
    }
    private static final class Fixture extends AnonymousReportRepository {
        final Map<UUID,AnonymousReport> records=new ConcurrentHashMap<>();
        final Map<String,UUID> referenceIds=new ConcurrentHashMap<>();
        final Map<UUID,List<AnonymousReportMessage>> conversations=new ConcurrentHashMap<>();
        final Map<UUID,List<AnonymousInvestigation>> histories=new ConcurrentHashMap<>();
        final AtomicInteger calls=new AtomicInteger(),offFxCalls=new AtomicInteger();
        final CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
        volatile boolean blockSubmit,rejectClosedMessages;int submissions;long messageId,investigationId;
        void call() throws IOException {calls.incrementAndGet();if(Platform.isFxApplicationThread()) throw new IOException("Service called on FX thread");offFxCalls.incrementAndGet();}
        void seed(int count) {for(int i=0;i<count;i++) {UUID id=UUID.randomUUID();OffsetDateTime time=OffsetDateTime.now().minusDays(10+i);records.put(id,new AnonymousReport(id,"Other","Fixture "+i,"Test-only fixture",null,AnonymousReportStatus.SUBMITTED,time,time,null));}}
        @Override public AnonymousReportSubmission submit(String token,String category,String subject,String description,String evidence) throws Exception {
            call();if(blockSubmit) {entered.countDown();release.await(5,TimeUnit.SECONDS);blockSubmit=false;}
            UUID id=UUID.randomUUID();String reference=String.format("%048x",++submissions);OffsetDateTime now=OffsetDateTime.now();
            var report=new AnonymousReport(id,category,subject,description,evidence,AnonymousReportStatus.SUBMITTED,now,now,null);
            records.put(id,report);referenceIds.put(reference,id);return new AnonymousReportSubmission(reference,report);
        }
        @Override public Optional<AnonymousReport> track(String token,String reference) throws Exception {call();UUID id=referenceIds.get(reference);return Optional.ofNullable(id==null?null:records.get(id));}
        @Override public Optional<AnonymousReport> findById(String token,UUID id) throws Exception {call();return Optional.ofNullable(records.get(id));}
        @Override public AnonymousReportPage findAll(String token,AnonymousReportStatus status,String category,LocalDate from,LocalDate through,int limit,int offset) throws Exception {
            call();var rows=records.values().stream().filter(r->status==null||r.status()==status).filter(r->category==null||r.category().equalsIgnoreCase(category))
                .filter(r->from==null||!r.submittedAt().toLocalDate().isBefore(from)).filter(r->through==null||!r.submittedAt().toLocalDate().isAfter(through))
                .sorted(Comparator.comparing(AnonymousReport::submittedAt).reversed()).toList();
            return new AnonymousReportPage(rows.stream().skip(offset).limit(limit).toList(),rows.size(),limit,offset);
        }
        @Override public List<AnonymousReportMessage> employeeMessages(String token,String reference) throws Exception {call();return List.copyOf(conversations.getOrDefault(referenceIds.get(reference),List.of()));}
        @Override public List<AnonymousReportMessage> adminMessages(String token,UUID id) throws Exception {call();return List.copyOf(conversations.getOrDefault(id,List.of()));}
        private AnonymousReportMessage add(UUID id,AnonymousReportMessage.SenderType sender,String content) throws IOException {
            if(rejectClosedMessages && records.get(id).status()==AnonymousReportStatus.CLOSED) throw new IOException("SQL private reference_hash must never reach the UI");
            var message=new AnonymousReportMessage(++messageId,id,sender,content,OffsetDateTime.now(),false);
            conversations.computeIfAbsent(id,key->new CopyOnWriteArrayList<>()).add(message);return message;
        }
        @Override public AnonymousReportMessage sendEmployeeMessage(String token,String reference,String content) throws Exception {call();return add(referenceIds.get(reference),AnonymousReportMessage.SenderType.ANONYMOUS_EMPLOYEE,content);}
        @Override public AnonymousReportMessage sendAdminMessage(String token,UUID id,String content) throws Exception {call();return add(id,AnonymousReportMessage.SenderType.ADMINISTRATOR,content);}
        @Override public List<AnonymousInvestigation> investigationHistory(String token,UUID id) throws Exception {call();return List.copyOf(histories.getOrDefault(id,List.of()));}
        @Override public AnonymousInvestigation saveInvestigation(String token,UUID id,AnonymousReportStatus expected,AnonymousReportStatus next,String notes,UUID admin,String resolution) throws Exception {
            call();var previous=records.get(id);OffsetDateTime now=OffsetDateTime.now();
            var snapshot=new AnonymousInvestigation(++investigationId,id,notes,admin,previous.status(),next,resolution,now,now);
            histories.computeIfAbsent(id,key->new CopyOnWriteArrayList<>()).add(snapshot);
            records.put(id,new AnonymousReport(id,previous.category(),previous.subject(),previous.description(),previous.evidencePath(),next,previous.submittedAt(),now,
                    next==AnonymousReportStatus.RESOLVED || next==AnonymousReportStatus.CLOSED?resolution:null));return snapshot;
        }
    }
}
