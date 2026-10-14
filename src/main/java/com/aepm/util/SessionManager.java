
package com.aepm.util;

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
        // Stop any scheduler belonging to a previous session.
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

        if (session == null) {
            return null;
        }

        return session.getAccessToken();
    }

    public static boolean isLoggedIn() {
        return authSession != null
                && currentUser != null;
    }

    public static synchronized void clearSession() {
        // Stop automatic escalation when logging out.
        DeadlineEscalationScheduler.getInstance().stop();

        authSession = null;
        currentUser = null;
    }
}
