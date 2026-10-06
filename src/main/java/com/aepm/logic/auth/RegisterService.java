package com.aepm.logic.auth;

import com.aepm.data.repository.AuthRepository;
import com.aepm.data.repository.UserRepository;
import com.aepm.model.AuthSession;
import com.aepm.util.ValidationUtil;

public class RegisterService {

    private final AuthRepository authRepository;
    private final UserRepository userRepository;

    public RegisterService() {
        authRepository = new AuthRepository();
        userRepository = new UserRepository();
    }

    public String validateRegistration(
            String fullName,
            String employeeId,
            String department,
            String username,
            String email,
            String password,
            String confirmPassword
    ) {

        if (ValidationUtil.isEmpty(fullName)) {
            return "Full name is required.";
        }

        if (ValidationUtil.isEmpty(employeeId)) {
            return "Employee ID is required.";
        }

        if (ValidationUtil.isEmpty(department)) {
            return "Department is required.";
        }

        if (ValidationUtil.isEmpty(username)) {
            return "Username is required.";
        }

        if (ValidationUtil.isEmpty(email)) {
            return "Email is required.";
        }

        if (!ValidationUtil.isValidEmail(email)) {
            return "Please enter a valid email address.";
        }

        if (ValidationUtil.isEmpty(password)) {
            return "Password is required.";
        }

        if (password.length() < 6) {
            return "Password must contain at least 6 characters.";
        }

        if (ValidationUtil.isEmpty(confirmPassword)) {
            return "Please confirm your password.";
        }

        if (!password.equals(confirmPassword)) {
            return "Passwords do not match.";
        }

        return null;
    }

    public void registerEmployee(
            String fullName,
            String employeeId,
            String department,
            String username,
            String email,
            String password,
            String confirmPassword
    ) throws Exception {

        String validationMessage = validateRegistration(
                fullName,
                employeeId,
                department,
                username,
                email,
                password,
                confirmPassword
        );

        if (validationMessage != null) {
            throw new Exception(validationMessage);
        }

        AuthSession authSession = authRepository.signUp(
                email,
                password
        );

        if (authSession == null) {
            throw new Exception(
                    "Unable to create authentication account."
            );
        }

        if (authSession.getAccessToken() == null
                || authSession.getAccessToken().isBlank()) {

            throw new Exception(
                    "Account was created, but no login session was returned. "
                            + "Please check the Supabase email confirmation setting."
            );
        }

        long employeeRoleId =
                userRepository.findRoleIdByName(
                        "Employee",
                        authSession.getAccessToken()
                );

        userRepository.createEmployeeProfile(
                authSession.getUserId(),
                employeeId,
                fullName,
                username,
                email,
                department,
                employeeRoleId,
                authSession.getAccessToken()
        );
    }
}