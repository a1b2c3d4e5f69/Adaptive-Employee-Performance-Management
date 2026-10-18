package com.aepm.ui.components;

import com.aepm.model.User;
import com.aepm.util.SessionManager;
import javafx.scene.control.Label;

public class EmployeeRoleLabel extends Label {

    public EmployeeRoleLabel() {
        User user = SessionManager.getCurrentUser();

        String id = user == null
                ? null
                : user.getEmployeeId();

        setText("EMPLOYEE" +
                (id == null || id.isBlank()
                        ? ""
                        : " · " + id));
    }
}
