package com.aepm.ui.components;

import javafx.scene.control.Label;

public final class UIHelper {

    private UIHelper() {
    }


    public static void showError(
            Label label,
            String message
    ) {

        label.setText(message);

        label
                .getStyleClass()
                .setAll(
                        "message-error"
                );

        label.setVisible(true);

        label.setManaged(true);
    }


    public static void showSuccess(
            Label label,
            String message
    ) {

        label.setText(message);

        label
                .getStyleClass()
                .setAll(
                        "message-success"
                );

        label.setVisible(true);

        label.setManaged(true);
    }


    public static void hideMessage(
            Label label
    ) {

        label.setVisible(false);

        label.setManaged(false);
    }
}