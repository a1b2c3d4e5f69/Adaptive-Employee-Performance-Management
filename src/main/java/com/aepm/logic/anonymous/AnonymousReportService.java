package com.aepm.logic.anonymous;

import com.aepm.data.repository.AnonymousReportRepository;
import com.aepm.model.*;
import java.time.LocalDate;
import java.util.*;

public class AnonymousReportService {
    private final AnonymousReportRepository repository;
    public AnonymousReportService() { this(new AnonymousReportRepository()); }
    public AnonymousReportService(AnonymousReportRepository repository) { this.repository = Objects.requireNonNull(repository); }

    public AnonymousReportSubmission submit(String category, String subject, String description, String evidencePath) throws Exception {
        String token = AnonymousReportingAccess.employee();
        category = AnonymousReportingAccess.required(category, "Category", 100);
        subject = AnonymousReportingAccess.required(subject, "Subject", 200);
        description = AnonymousReportingAccess.required(description, "Description", 10000);
        evidencePath = AnonymousReportingAccess.optional(evidencePath, "Evidence URL/path", 2048);
        return repository.submit(token, category, subject, description, evidencePath);
    }

    public AnonymousReportPage review(AnonymousReportStatus status, String category, LocalDate from,
            LocalDate through, int limit, int offset) throws Exception {
        String token = AnonymousReportingAccess.administrator();
        if (limit < 1 || limit > 100 || offset < 0) throw new IllegalArgumentException("Use limit 1-100 and a non-negative offset.");
        if (from != null && through != null && from.isAfter(through))
            throw new IllegalArgumentException("Submitted start date must not be after end date.");
        return repository.findAll(token, status, AnonymousReportingAccess.optional(category, "Category", 100), from, through, limit, offset);
    }

    public Optional<AnonymousReport> detail(UUID reportId) throws Exception {
        return repository.findById(AnonymousReportingAccess.administrator(), AnonymousReportingAccess.id(reportId));
    }
}
