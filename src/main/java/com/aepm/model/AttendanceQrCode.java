
package com.aepm.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public class AttendanceQrCode {

    private UUID qrId;
    private String qrToken;
    private OffsetDateTime expiresAt;

    public AttendanceQrCode(
            UUID qrId,
            String qrToken,
            OffsetDateTime expiresAt
    ) {
        this.qrId = qrId;
        this.qrToken = qrToken;
        this.expiresAt = expiresAt;
    }

    public UUID getQrId() {
        return qrId;
    }

    public String getQrToken() {
        return qrToken;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setQrId(UUID qrId) {
        this.qrId = qrId;
    }

    public void setQrToken(String qrToken) {
        this.qrToken = qrToken;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
