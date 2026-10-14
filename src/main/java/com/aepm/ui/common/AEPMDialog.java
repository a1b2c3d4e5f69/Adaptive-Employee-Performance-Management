package com.aepm.ui.common;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public final class AEPMDialog {

    private AEPMDialog() {}

    public static void showSuccess(
            Window owner, String title, String message) {
        show(owner, title, message, false);
    }

    public static void showError(
            Window owner, String title, String message) {
        show(owner, title, message, true);
    }

    private static void show(
            Window owner,
            String title,
            String message,
            boolean error) {

        Stage dialog = new Stage();
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.initModality(Modality.APPLICATION_MODAL);

        if (owner != null) {
            dialog.initOwner(owner);
        }

        String accent = error ? "#DC3545" : "#2563EB";
        String symbol = error ? "!" : "✓";

        Label icon = new Label(symbol);
        icon.setAlignment(Pos.CENTER);
        icon.setMinSize(44, 44);
        icon.setMaxSize(44, 44);
        icon.setStyle(
            "-fx-background-color: " +
            (error ? "#FEE2E2" : "#DBEAFE") + ";" +
            "-fx-background-radius: 14;" +
            "-fx-text-fill: " + accent + ";" +
            "-fx-font-size: 22px;" +
            "-fx-font-weight: bold;"
        );

        Label titleLabel = new Label(title);
        titleLabel.setWrapText(true);
        titleLabel.setStyle(
            "-fx-font-size: 19px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #111827;"
        );

        Label subtitle = new Label(
            "Adaptive Employee Performance Management System"
        );
        subtitle.setStyle(
            "-fx-font-size: 11px;" +
            "-fx-text-fill: #94A3B8;"
        );

        VBox heading = new VBox(4, titleLabel, subtitle);
        heading.setAlignment(Pos.CENTER_LEFT);

        HBox header = new HBox(14, icon, heading);
        header.setAlignment(Pos.CENTER_LEFT);

        Label messageLabel = new Label(
            friendlyMessage(message, error)
        );
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(Double.MAX_VALUE);
        messageLabel.setStyle(
            "-fx-font-size: 14px;" +
            "-fx-text-fill: #475569;" +
            "-fx-line-spacing: 4px;"
        );

        Button okButton = new Button("OK");
        okButton.setMinWidth(100);
        okButton.setPrefHeight(38);
        okButton.setDefaultButton(true);
        okButton.setStyle(
            "-fx-background-color: #2563EB;" +
            "-fx-background-radius: 9;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 13px;" +
            "-fx-font-weight: bold;" +
            "-fx-cursor: hand;"
        );
        okButton.setOnAction(e -> dialog.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox footer = new HBox(spacer, okButton);
        footer.setAlignment(Pos.CENTER_RIGHT);

        VBox content = new VBox(22, header, messageLabel, footer);
        content.setPadding(new Insets(28));
        content.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 16;" +
            "-fx-border-color: #E2E8F0;" +
            "-fx-border-radius: 16;" +
            "-fx-border-width: 1;"
        );

        VBox root = new VBox(content);
        root.setPadding(new Insets(12));
        root.setStyle("-fx-background-color: transparent;");

        Scene scene = new Scene(root, 460, 245);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);

        dialog.setScene(scene);
        dialog.setResizable(false);
        dialog.setOnShown(e -> {
            if (owner != null) {
                dialog.setX(
                    owner.getX() +
                    (owner.getWidth() - dialog.getWidth()) / 2
                );
                dialog.setY(
                    owner.getY() +
                    (owner.getHeight() - dialog.getHeight()) / 2
                );
            } else {
                dialog.centerOnScreen();
            }
        });

        dialog.showAndWait();
    }

    private static String friendlyMessage(
            String message, boolean error) {

        if (message == null || message.isBlank()) {
            return error
                ? "Something went wrong. Please try again."
                : "Operation completed successfully.";
        }

        String lower = message.toLowerCase();

        if (lower.contains("pgrst303")
                || lower.contains("jwt")
                || lower.contains("401")) {
            return "Your login session is invalid or has expired. "
                 + "Please log in again and retry.";
        }

        if (lower.contains("403")
                || lower.contains("permission denied")) {
            return "You do not have permission to perform "
                 + "this action. Please contact the administrator.";
        }

        if (lower.contains("duplicate key")
                || lower.contains("23505")) {
            return "This record already exists. "
                 + "Please check the information and try again.";
        }

        if (lower.contains("supabase error")
                || lower.contains("\"code\"")) {
            return "Unable to complete the request. "
                 + "Please check your connection or try again.";
        }

        return message;
    }
}
