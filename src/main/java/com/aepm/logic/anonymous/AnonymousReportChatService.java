package com.aepm.logic.anonymous;

import com.aepm.data.repository.AnonymousReportRepository;
import com.aepm.model.AnonymousReportMessage;
import java.util.*;

public class AnonymousReportChatService {
    private final AnonymousReportRepository repository;
    public AnonymousReportChatService() { this(new AnonymousReportRepository()); }
    public AnonymousReportChatService(AnonymousReportRepository repository) { this.repository = Objects.requireNonNull(repository); }
    public AnonymousReportMessage sendEmployee(String reference, String content) throws Exception {
        return repository.sendEmployeeMessage(AnonymousReportingAccess.employee(), AnonymousReportingAccess.reference(reference),
                AnonymousReportingAccess.required(content, "Message", 5000));
    }
    public AnonymousReportMessage sendAdministrator(UUID reportId, String content) throws Exception {
        return repository.sendAdminMessage(AnonymousReportingAccess.administrator(), AnonymousReportingAccess.id(reportId),
                AnonymousReportingAccess.required(content, "Message", 5000));
    }
    public List<AnonymousReportMessage> employeeHistory(String reference) throws Exception {
        return repository.employeeMessages(AnonymousReportingAccess.employee(), AnonymousReportingAccess.reference(reference));
    }
    public List<AnonymousReportMessage> administratorHistory(UUID reportId) throws Exception {
        return repository.adminMessages(AnonymousReportingAccess.administrator(), AnonymousReportingAccess.id(reportId));
    }
}
