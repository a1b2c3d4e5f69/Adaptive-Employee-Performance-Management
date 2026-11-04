package com.aepm.logic.anonymous;

import com.aepm.util.SessionManager;
import java.util.UUID;

final class AnonymousReportingAccess {
    private AnonymousReportingAccess() {}
    static String employee() { return session(3); }
    static String administrator() { return session(1); }
    private static String session(long role) {
        var session = SessionManager.getAuthSession();
        var user = SessionManager.getCurrentUser();
        String token = session == null ? null : session.getAccessToken();
        if (user == null || user.getRoleId() != role || !"Active".equalsIgnoreCase(user.getAccountStatus())
                || token == null || token.isBlank() || SessionManager.getAuthSession() != session) {
            throw new IllegalStateException("Sign in with an active authorized account.");
        }
        return token;
    }
    static String required(String value, String label, int max) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required.");
        String clean = value.strip();
        if (clean.codePointCount(0, clean.length()) > max || clean.indexOf(0) >= 0)
            throw new IllegalArgumentException(label + " is too long or contains invalid characters.");
        return clean;
    }
    static String optional(String value, String label, int max) {
        return value == null || value.isBlank() ? null : required(value, label, max);
    }
    static String reference(String value) {
        // Legacy varchar(50) references remain usable byte-for-byte after hash backfill.
        // The deployed submission RPC generates the new secret reference; legacy values remain valid.
        if (value == null || value.isBlank() || value.codePointCount(0, value.length()) > 50 || value.indexOf(0) >= 0)
            throw new IllegalArgumentException("Invalid anonymous report reference.");
        return value;
    }
    static UUID id(UUID value) {
        if (value == null) throw new IllegalArgumentException("Report ID is required.");
        return value;
    }
}
