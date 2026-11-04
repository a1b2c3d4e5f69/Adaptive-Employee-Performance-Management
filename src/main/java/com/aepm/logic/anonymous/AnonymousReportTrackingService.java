package com.aepm.logic.anonymous;

import com.aepm.data.repository.AnonymousReportRepository;
import com.aepm.model.AnonymousReport;
import java.util.*;

public class AnonymousReportTrackingService {
    private final AnonymousReportRepository repository;
    public AnonymousReportTrackingService() { this(new AnonymousReportRepository()); }
    public AnonymousReportTrackingService(AnonymousReportRepository repository) { this.repository = Objects.requireNonNull(repository); }
    /** Requires employee authorization, but never binds a report to the employee's identity. */
    public Optional<AnonymousReport> track(String reference) throws Exception {
        return repository.track(AnonymousReportingAccess.employee(), AnonymousReportingAccess.reference(reference));
    }
}
