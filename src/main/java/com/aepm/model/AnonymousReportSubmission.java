package com.aepm.model;

/** Returned only to the submitting employee. The reference is a bearer secret; do not log it. */
public record AnonymousReportSubmission(String reference, AnonymousReport report) {
    @Override public String toString() { return "AnonymousReportSubmission[reference=REDACTED]"; }
}
