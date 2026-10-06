package com.aepm.logic.auth;

import com.aepm.data.repository.AuthRepository;
import com.aepm.data.repository.UserRepository;

import com.aepm.model.AuthSession;
import com.aepm.model.User;

import com.aepm.util.SessionManager;
import com.aepm.util.ValidationUtil;

public class AuthService {

    private final AuthRepository authRepository;

    private final UserRepository userRepository;


    public AuthService() {

        authRepository =
                new AuthRepository();

        userRepository =
                new UserRepository();
    }


    public String validateLoginInput(
            String email,
            String password
    ) {

        if (ValidationUtil.isEmpty(email)) {

            return "Email is required.";
        }


        if (!ValidationUtil.isValidEmail(email)) {

            return "Please enter a valid email address.";
        }


        if (ValidationUtil.isEmpty(password)) {

            return "Password is required.";
        }


        return null;
    }


    public User login(
            String email,
            String password
    ) throws Exception {

        String validationMessage =
                validateLoginInput(
                        email,
                        password
                );


        if (validationMessage != null) {

            throw new Exception(
                    validationMessage
            );
        }


        AuthSession authSession =
                authRepository.login(
                        email,
                        password
                );


        User user =
                userRepository.findUserById(
                        authSession.getUserId(),
                        authSession.getAccessToken()
                );


        if (user == null) {

            throw new Exception(
                    "Unable to load user profile."
            );
        }


        if (
                user.getAccountStatus() != null
                && user
                        .getAccountStatus()
                        .equalsIgnoreCase(
                                "Inactive"
                        )
        ) {

            throw new Exception(
                    "Your account is inactive. Please contact the administrator."
            );
        }


        if (user.getRole() == null) {

            throw new Exception(
                    "No role is assigned to this account."
            );
        }


        SessionManager.setSession(
                authSession,
                user
        );


        return user;
    }

    public void changePassword(
        String currentPassword,
        String newPassword
) throws Exception {

    if (SessionManager.getCurrentUser() == null) {

        throw new Exception(
                "No user is currently logged in."
        );
    }


    String email =
            SessionManager
                    .getCurrentUser()
                    .getEmail();


    if (
            currentPassword == null
                    || currentPassword.isBlank()
    ) {

        throw new Exception(
                "Current password is required."
        );
    }


    if (
            newPassword == null
                    || newPassword.isBlank()
    ) {

        throw new Exception(
                "New password is required."
        );
    }


    if (newPassword.length() < 6) {

        throw new Exception(
                "New password must contain at least 6 characters."
        );
    }


    /*
     * Verify current password first.
     */

    AuthSession verificationSession =
            authRepository.login(
                    email,
                    currentPassword
            );


    if (verificationSession == null) {

        throw new Exception(
                "Current password is incorrect."
        );
    }


    String accessToken =
            SessionManager
                    .getAccessToken();


    authRepository.updatePassword(
            accessToken,
            newPassword
    );
}


}