package com.aepm.ui.components;

import com.aepm.model.User;
import com.aepm.util.SessionManager;
import javafx.scene.control.Label;

public class EmployeeWelcomeLabel extends Label {

    public EmployeeWelcomeLabel() {
        User user = SessionManager.getCurrentUser();

        String name = user == null
                ? "Employee"
                : user.getFullName();

        if (name == null || name.isBlank()) {
            name = "Employee";
        }

        setText("Welcome, " + name);
    }
}
