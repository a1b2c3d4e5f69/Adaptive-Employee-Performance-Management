package com.aepm.model;

public enum Role {

    ADMINISTRATOR,
    MANAGER,
    EMPLOYEE;


    public static Role fromDatabaseValue(String roleName) {

        if (roleName == null) {
            return null;
        }

        return switch (roleName.trim().toLowerCase()) {

            case "administrator", "admin" ->
                    ADMINISTRATOR;

            case "manager" ->
                    MANAGER;

            case "employee" ->
                    EMPLOYEE;

            default ->
                    null;
        };
    }
}