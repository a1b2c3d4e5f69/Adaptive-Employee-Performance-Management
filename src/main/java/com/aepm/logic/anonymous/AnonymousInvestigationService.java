package com.aepm.logic.anonymous;

import com.aepm.data.repository.AnonymousReportRepository;
import com.aepm.model.*;
import java.util.*;

public class AnonymousInvestigationService {
    private final AnonymousReportRepository repository;
    public AnonymousInvestigationService() { this(new AnonymousReportRepository()); }
    public AnonymousInvestigationService(AnonymousReportRepository repository) { this.repository = Objects.requireNonNull(repository); }
    public Optional<AnonymousInvestigation> get(UUID reportId) throws Exception {
        return repository.investigation(AnonymousReportingAccess.administrator(), AnonymousReportingAccess.id(reportId));
    }
    public List<AnonymousInvestigation> history(UUID reportId) throws Exception {
        return repository.investigationHistory(AnonymousReportingAccess.administrator(), AnonymousReportingAccess.id(reportId));
    }
    /** expectedStatus detects concurrent workflow changes. Null assignee means unassigned. */
    public AnonymousInvestigation save(UUID reportId, AnonymousReportStatus expectedStatus,
            AnonymousReportStatus nextStatus, String notes, UUID assignedAdministrator, String resolution) throws Exception {
        String token = AnonymousReportingAccess.administrator();
        AnonymousReportingAccess.id(reportId);
        // The deployed RPC records auth.uid() as the acting administrator; it does not support reassignment.
        if (assignedAdministrator != null && !assignedAdministrator.toString().equals(
                com.aepm.util.SessionManager.getCurrentUser().getUserId()))
            throw new IllegalArgumentException("The deployed investigation RPC records the signed-in administrator only.");
        if (expectedStatus == null || !expectedStatus.canTransitionTo(nextStatus) || expectedStatus == AnonymousReportStatus.CLOSED)
            throw new IllegalArgumentException("Invalid investigation status transition.");
        notes = AnonymousReportingAccess.optional(notes, "Investigation notes", 20000);
        resolution = AnonymousReportingAccess.optional(resolution, "Resolution", 10000);
        if ((nextStatus == AnonymousReportStatus.RESOLVED || nextStatus == AnonymousReportStatus.CLOSED) && resolution == null)
            throw new IllegalArgumentException("A resolution is required before resolving or closing a report.");
        return repository.saveInvestigation(token, reportId, expectedStatus, nextStatus,
                notes == null ? "" : notes, assignedAdministrator, resolution);
    }
}
