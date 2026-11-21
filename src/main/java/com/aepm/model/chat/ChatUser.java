package com.aepm.model.chat;

import java.util.UUID;

public class ChatUser {

    private final UUID userId;
    private final String fullName;
    private final String employeeId;
    private final int roleId;
    private final String department;
    private final String profileImage;

    public ChatUser(
            UUID userId,
            String fullName,
            String employeeId,
            int roleId,
            String department,
            String profileImage
    ) {
        this.userId = userId;
        this.fullName = fullName;
        this.employeeId = employeeId;
        this.roleId = roleId;
        this.department = department;
        this.profileImage = profileImage;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public int getRoleId() {
        return roleId;
    }

    public String getDepartment() {
        return department;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public String getRoleName() {
        return switch (roleId) {
            case 1 -> "Admin";
            case 2 -> "Manager";
            case 3 -> "Employee";
            default -> "User";
        };
    }

    @Override
    public String toString() {
        if (employeeId != null && !employeeId.isBlank()) {
            return fullName + " (" + employeeId + ")";
        }
        return fullName;
    }
}
