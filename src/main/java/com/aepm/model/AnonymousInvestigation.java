package com.aepm.model;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Administrator-only history snapshot. Existing history rows are never overwritten by save. */
public record AnonymousInvestigation(long investigationId, UUID reportId, String notes,
        UUID assignedAdministratorId, AnonymousReportStatus previousStatus,
        AnonymousReportStatus status, String resolution, OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {}
