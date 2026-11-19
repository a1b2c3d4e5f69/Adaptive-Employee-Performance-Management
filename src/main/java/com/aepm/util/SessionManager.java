package com.aepm.util;

import com.aepm.data.repository.AuthRepository;
import com.aepm.logic.deadline.DeadlineEscalationScheduler;
import com.aepm.model.AuthSession;
import com.aepm.model.User;

public final class SessionManager {

    private static volatile AuthSession authSession;
    private static volatile User currentUser;

    private SessionManager() {
    }

    public static synchronized void setSession(
            AuthSession session,
            User user
    ) {
        DeadlineEscalationScheduler.getInstance().stop();
        authSession = session;
        currentUser = user;
    }

    public static AuthSession getAuthSession() {
        return authSession;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static String getAccessToken() {
        AuthSession session = authSession;
        return session == null ? null : session.getAccessToken();
    }

    public static String getRefreshToken() {
        AuthSession session = authSession;
        return session == null ? null : session.getRefreshToken();
    }

    public static synchronized String refreshAccessToken()
            throws Exception {

        AuthSession oldSession = authSession;

        if (oldSession == null) {
            throw new IllegalStateException(
                    "No active login session."
            );
        }

        String refreshToken = oldSession.getRefreshToken();

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalStateException(
                    "Refresh token is unavailable. Please login again."
            );
        }

        AuthSession refreshed =
                new AuthRepository().refreshSession(refreshToken);

        String userId = refreshed.getUserId();
        if (userId == null || userId.isBlank()) {
            userId = oldSession.getUserId();
        }

        String email = refreshed.getEmail();
        if (email == null || email.isBlank()) {
            email = oldSession.getEmail();
        }

        String newRefreshToken = refreshed.getRefreshToken();
        if (newRefreshToken == null || newRefreshToken.isBlank()) {
            newRefreshToken = oldSession.getRefreshToken();
        }

        authSession = new AuthSession(
                refreshed.getAccessToken(),
                newRefreshToken,
                userId,
                email
        );

        System.out.println(
                "[Session] Access token refreshed successfully."
        );

        return authSession.getAccessToken();
    }

    public static boolean isAuthenticationExpired(
            Throwable error
    ) {
        Throwable current = error;

        while (current != null) {
            String message = current.getMessage();

            if (message != null) {
                String lower = message.toLowerCase();

                if (message.contains("401")
                        || message.contains("PGRST303")
                        || lower.contains("jwt expired")) {
                    return true;
                }
            }

            current = current.getCause();
        }

        return false;
    }

    public static boolean isLoggedIn() {
        return authSession != null
                && currentUser != null;
    }

    public static synchronized void clearSession() {
        DeadlineEscalationScheduler.getInstance().stop();
        authSession = null;
        currentUser = null;
    }
}
