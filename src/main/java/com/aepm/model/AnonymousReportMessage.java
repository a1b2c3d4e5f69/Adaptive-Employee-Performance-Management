package com.aepm.model;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Safe conversation DTO. The live bigint ID is retained; sender_admin_id is never projected. */
public record AnonymousReportMessage(long messageId, UUID reportId, SenderType senderType,
        String content, OffsetDateTime sentAt, boolean read) {
    public enum SenderType {
        ANONYMOUS_EMPLOYEE("Anonymous Reporter"), ADMINISTRATOR("Administrator");
        private final String value;
        SenderType(String value) { this.value = value; }
        public String value() { return value; }
        public static SenderType fromValue(String value) {
            for (var sender : values()) if (sender.value.equals(value)) return sender;
            throw new IllegalArgumentException("Unknown anonymous message sender.");
        }
    }
}
