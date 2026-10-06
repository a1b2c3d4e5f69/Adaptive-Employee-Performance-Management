package com.aepm.util;

import com.aepm.model.AuthSession;
import com.aepm.model.User;

public final class SessionManager {

    private static AuthSession authSession;

    private static User currentUser;


    private SessionManager() {
    }


    public static void setSession(
            AuthSession session,
            User user
    ) {

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

        if (authSession == null) {
            return null;
        }

        return authSession.getAccessToken();
    }


    public static boolean isLoggedIn() {

        return authSession != null
                && currentUser != null;
    }


    public static void clearSession() {

        authSession = null;
        currentUser = null;
    }
}