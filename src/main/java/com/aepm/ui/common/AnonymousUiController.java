package com.aepm.ui.common;

import com.aepm.model.*;
import com.aepm.ui.navigation.NavigationManager;
import com.aepm.util.SessionManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

/** UI-only lifecycle, navigation and safe asynchronous service calls. Never handles REST or credentials. */
public abstract class AnonymousUiController {
    public static final List<String> CATEGORIES=List.of("Harassment","Discrimination","Unethical Behaviour",
            "Abuse of Authority","Safety Concern","Other");
    @FXML protected BorderPane root;
    @FXML protected VBox pageContent;
    @FXML protected Label messageLabel;
    @FXML protected ProgressIndicator busyIndicator;
    private Object session;
    private long role;
    private long generation;
    private boolean busy;
    private Task<?> task;
    private Timeline sessionWatch;

    protected final boolean initializeView(long requiredRole) {
        role=requiredRole; session=SessionManager.getAuthSession();
        busyIndicator.managedProperty().bind(busyIndicator.visibleProperty());
        sessionWatch=new Timeline(new KeyFrame(Duration.seconds(1),event->{if(!sameSession()) invalidateSession();}));
        sessionWatch.setCycleCount(Timeline.INDEFINITE);
        pageContent.sceneProperty().addListener((observable,oldScene,newScene)->{
            if(newScene==null && oldScene!=null) {
                generation++; if(task!=null) task.cancel(); sessionWatch.stop(); onLeave();
            } else if(newScene!=null) sessionWatch.play();
        });
        if(!sameSession()) {invalidateSession();return false;}
        return true;
    }
    private boolean sameSession() {
        var user=SessionManager.getCurrentUser();
        return session!=null && session==SessionManager.getAuthSession() && user!=null
                && user.getRoleId()==role && "Active".equalsIgnoreCase(user.getAccountStatus())
                && SessionManager.getAccessToken()!=null;
    }
    protected final boolean authorized() {
        if(sameSession()) return true;
        invalidateSession(); return false;
    }
    private void invalidateSession() {
        generation++; if(task!=null) task.cancel(); if(sessionWatch!=null) sessionWatch.stop();
        setBusy(false);onLeave();pageContent.setDisable(true);
        message("Access denied or session changed. Sign in with the appropriate account.",false);
    }
    protected void onLeave() {}
    public final boolean isBusy() {return busy;}
    private void setBusy(boolean value) {
        busy=value;root.setDisable(value);busyIndicator.setVisible(value);
    }
    protected final <T> void run(Callable<T> work,Consumer<T> success,String failureMessage) {
        if(busy || !authorized()) return;
        long ticket=++generation;
        setBusy(true); message("Working...",true);
        Task<T> operation=new Task<>() {
            @Override protected T call() throws Exception {
                if(!sameSession()) throw new IllegalStateException("Session changed");
                return work.call();
            }
        };
        task=operation;
        operation.setOnSucceeded(event->{
            if(!sameSession()) {invalidateSession();return;}
            if(ticket!=generation || pageContent.getScene()==null) return;
            setBusy(false);success.accept(operation.getValue());
        });
        operation.setOnFailed(event->{
            if(!sameSession()) {invalidateSession();return;}
            if(ticket!=generation || pageContent.getScene()==null) return;
            setBusy(false);message(failureMessage,false);
        });
        operation.setOnCancelled(event->{if(ticket==generation) setBusy(false);});
        Thread worker=new Thread(operation,"anonymous-ui-service");worker.setDaemon(true);worker.start();
    }
    protected final void message(String text,boolean success) {
        messageLabel.setText(text);messageLabel.getStyleClass().removeAll("message-error","message-success");
        messageLabel.getStyleClass().add(success?"message-success":"message-error");
    }
    protected final boolean required(String value,String label,int maximum) {
        if(value==null || value.isBlank()) {message(label+" is required.",false);return false;}
        return optional(value,label,maximum);
    }
    protected final boolean optional(String value,String label,int maximum) {
        if(value!=null && (value.codePointCount(0,value.length())>maximum || value.indexOf(0)>=0)) {
            message(label+" is too long or contains invalid characters.",false);return false;
        }
        return true;
    }
    protected final boolean validReference(String reference) {
        if(reference==null || reference.isBlank() || reference.codePointCount(0,reference.length())>50 || reference.indexOf(0)>=0) {
            message("Report not found or invalid reference.",false);return false;
        }
        return true; // Never trim an existing secret reference.
    }
    public static String date(OffsetDateTime value) {
        return value==null?"-":value.atZoneSameInstant(ZoneId.of("Asia/Singapore"))
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm",Locale.ENGLISH));
    }
    protected static String text(String value) {return value==null || value.isBlank()?"-":value;}
    protected static String status(AnonymousReportStatus value) {return value==null?"-":value.value();}
    protected static <T> void column(TableView<T> table,String heading,double width,Function<T,String> value) {
        TableColumn<T,String> column=new TableColumn<>(heading);column.setPrefWidth(width);
        column.setCellValueFactory(cell->new SimpleStringProperty(value.apply(cell.getValue())));
        column.setCellFactory(ignored->new TableCell<>() {
            final Label label=new Label();
            {label.getStyleClass().add("task-detail-value");label.setWrapText(true);label.maxWidthProperty().bind(column.widthProperty().subtract(20));}
            @Override protected void updateItem(String item,boolean empty) {
                super.updateItem(item,empty);label.setText(empty?"":item);setGraphic(empty?null:label);setText(null);
            }
        });
        table.getColumns().add(column);
    }
    protected static void conversation(TableView<AnonymousReportMessage> table) {
        table.getColumns().clear();
        column(table,"Sender",165,row->row.senderType().value());
        column(table,"Sent (UTC+08)",170,row->date(row.sentAt()));
        column(table,"Message",480,AnonymousReportMessage::content);
        Label placeholder=new Label("No messages yet.");placeholder.getStyleClass().add("card-description");table.setPlaceholder(placeholder);
    }
    protected <C> void navigate(String path,Consumer<C> configure) {
        if(root.getScene()==null || !(root.getScene().getWindow() instanceof Stage stage)) return;
        try {NavigationManager.navigate(stage,path,configure);}
        catch(RuntimeException error) {message("Unable to open this page. Please try again.",false);}
    }
    protected final void navigate(String path) {navigate(path,ignored->{});}
    private String portal() {return role==3?"employee":"admin";}
    @FXML protected void handleHome() {
        var user=SessionManager.getCurrentUser();
        String area=user!=null && user.getRoleId()==2?"manager":portal();
        navigate("/fxml/"+area+"/"+area+"Home.fxml");
    }
    @FXML protected void handleProfile() {navigate("/fxml/"+portal()+"/"+portal()+"Profile.fxml");}
    @FXML protected void handleTasks() {navigate("/fxml/employee/employeeTask.fxml");}
    @FXML protected void handleKpi() {navigate("/fxml/employee/employeePerformance.fxml");}
    @FXML protected void handleAttendance() {navigate("/fxml/employee/employeeAttendance.fxml");}
    @FXML protected void handleReports() {
        if(role==1) navigate("/fxml/admin/adminReport.fxml");else message("Employee Reports is not available yet.",false);
    }
    @FXML protected void handleAnonymousReport() {navigate("/fxml/employee/employeeAnonymousReport.fxml");}
    @FXML protected void handleAnonymousReports() {navigate("/fxml/admin/adminAnonymousReports.fxml");}
    @FXML protected void handleUserManagement() {navigate("/fxml/admin/adminUserManagement.fxml");}
    @FXML protected void handleTaskManagement() {navigate("/fxml/admin/adminTask.fxml");}
    @FXML protected void handleKpiManagement() {navigate("/fxml/admin/adminKpi.fxml");}
    @FXML protected void handleEscalationRules() {navigate("/fxml/admin/adminEscalationRules.fxml");}
    @FXML protected void handleSystemConfiguration() {navigate("/fxml/admin/systemConfiguration.fxml");}
    @FXML protected void handleAttendanceManagement() {navigate("/fxml/admin/adminAttendance.fxml");}
    @FXML protected void handleAIConfiguration() {navigate("/fxml/admin/adminAIConfiguration.fxml");}
    @FXML protected void handleAnnouncements() {
        if(root.getScene()!=null) com.aepm.ui.admin.AdminAnnouncementsDialog.show(root.getScene().getWindow());
    }
    @FXML protected void handleLogout() {SessionManager.clearSession();navigate("/fxml/auth/login.fxml");}
}
