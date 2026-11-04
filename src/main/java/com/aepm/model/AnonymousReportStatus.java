package com.aepm.model;

public enum AnonymousReportStatus {
    SUBMITTED("Submitted"), UNDER_REVIEW("Under Review"), INVESTIGATING("Under Investigation"),
    RESOLVED("Resolved"), CLOSED("Closed");

    private final String value;
    AnonymousReportStatus(String value) { this.value = value; }
    public String value() { return value; }
    public static AnonymousReportStatus fromValue(String value) {
        for (var status : values()) if (status.value.equals(value)) return status;
        throw new IllegalArgumentException("Unknown anonymous report status.");
    }
    public boolean canTransitionTo(AnonymousReportStatus next) {
        return next != null && (this == next || (this != CLOSED && next.ordinal() == ordinal() + 1));
    }
}
