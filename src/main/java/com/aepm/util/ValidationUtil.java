package com.aepm.util;

import java.util.regex.Pattern;

public final class ValidationUtil {

    private ValidationUtil() {}

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    public static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static boolean isValidEmail(String email) {
        if (isEmpty(email)) {
            return false;
        }

        return EMAIL_PATTERN.matcher(email).matches();
    }
}