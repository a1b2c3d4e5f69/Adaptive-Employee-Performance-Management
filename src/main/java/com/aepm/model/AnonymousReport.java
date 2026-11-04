package com.aepm.model;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Safe for both administrator review and reference-based employee tracking. No identity or secret token. */
public record AnonymousReport(UUID reportId, String category, String subject, String description,
        String evidencePath, AnonymousReportStatus status, OffsetDateTime submittedAt,
        OffsetDateTime updatedAt, String resolution) {}
