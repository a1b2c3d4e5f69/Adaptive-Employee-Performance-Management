package com.aepm.logic.user;

import com.aepm.data.repository.UserRepository;
import com.aepm.model.User;
import com.aepm.util.SessionManager;
import com.aepm.util.ValidationUtil;

import java.util.List;

public class UserService {

    private final UserRepository userRepository;


    public UserService() {

        userRepository =
                new UserRepository();
    }


    // =========================================================
    // GET ALL USERS
    // =========================================================

    public List<User> getAllUsers()
            throws Exception {

        String accessToken =
                SessionManager
                        .getAccessToken();


        if (
                accessToken == null
                        || accessToken.isBlank()
        ) {

            throw new Exception(
                    "Administrator session is unavailable."
            );
        }


        return userRepository
                .getAllUsers(
                        accessToken
                );
    }


    // =========================================================
    // UPDATE USER
    // =========================================================

    public void updateUser(
            User user
    ) throws Exception {

        if (user == null) {

            throw new Exception(
                    "User is required."
            );
        }


        if (
                user.getFullName() == null
                        || user
                        .getFullName()
                        .isBlank()
        ) {

            throw new Exception(
                    "Full name is required."
            );
        }


        if (
                user.getUsername() == null
                        || user
                        .getUsername()
                        .isBlank()
        ) {

            throw new Exception(
                    "Username is required."
            );
        }


        if (user.getRole() == null) {

            throw new Exception(
                    "Role is required."
            );
        }


        String accessToken =
                SessionManager
                        .getAccessToken();


        if (
                accessToken == null
                        || accessToken.isBlank()
        ) {

            throw new Exception(
                    "Administrator session is unavailable."
            );
        }


        userRepository.updateUser(
                user,
                accessToken
        );
    }


    // =========================================================
    // ACCOUNT STATUS
    // =========================================================

    public void updateAccountStatus(
            String userId,
            String status
    ) throws Exception {

        if (
                userId == null
                        || userId.isBlank()
        ) {

            throw new Exception(
                    "User ID is required."
            );
        }


        if (
                !"Active".equals(
                        status
                )
                        && !"Inactive".equals(
                        status
                )
        ) {

            throw new Exception(
                    "Invalid account status."
            );
        }


        String accessToken =
                SessionManager
                        .getAccessToken();


        userRepository
                .updateAccountStatus(
                        userId,
                        status,
                        accessToken
                );
    }


    // =========================================================
    // CREATE USER VALIDATION
    // =========================================================

    public String validateCreateUser(
            String fullName,
            String employeeId,
            String department,
            String username,
            String email,
            String password,
            String confirmPassword,
            String role
    ) {

        if (ValidationUtil.isEmpty(fullName)) {
            return "Full name is required.";
        }


        if (ValidationUtil.isEmpty(employeeId)) {
            return "Staff ID is required.";
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


        if (!password.equals(confirmPassword)) {
            return "Passwords do not match.";
        }


        if (
                !"Employee".equals(role)
                        && !"Manager".equals(role)
        ) {

            return "Please select Employee or Manager.";
        }


        return null;
    }


    // =========================================================
    // CREATE USER
    // =========================================================

    public void createUser(
            String fullName,
            String employeeId,
            String department,
            String username,
            String email,
            String password,
            String confirmPassword,
            String role
    ) throws Exception {

        String validation =
                validateCreateUser(
                        fullName,
                        employeeId,
                        department,
                        username,
                        email,
                        password,
                        confirmPassword,
                        role
                );


        if (validation != null) {

            throw new Exception(
                    validation
            );
        }


        String accessToken =
                SessionManager
                        .getAccessToken();


        if (
                accessToken == null
                        || accessToken.isBlank()
        ) {

            throw new Exception(
                    "Administrator session is unavailable."
            );
        }


        userRepository.createUserByAdmin(
                fullName.trim(),
                employeeId.trim(),
                department.trim(),
                username.trim(),
                email.trim().toLowerCase(),
                password,
                role,
                accessToken
        );
    }
    
   // =========================================================
// DELETE USER
// =========================================================

public void deleteUser(
        String userId
) throws Exception {

    if (
            userId == null
                    || userId.isBlank()
    ) {

        throw new Exception(
                "User ID is required."
        );
    }


    String accessToken =
            SessionManager
                    .getAccessToken();


    if (
            accessToken == null
                    || accessToken.isBlank()
    ) {

        throw new Exception(
                "Administrator session is unavailable."
        );
    }


    userRepository.deleteUserByAdmin(
            userId,
            accessToken
    );
}
}