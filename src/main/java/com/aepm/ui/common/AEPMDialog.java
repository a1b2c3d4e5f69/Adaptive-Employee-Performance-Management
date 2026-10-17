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
        messageLabel.setPrefWidth(430);
        messageLabel.setMinHeight(Region.USE_PREF_SIZE);
        messageLabel.setMaxHeight(Double.MAX_VALUE);
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

        if (lower.contains("42501")
                || lower.contains("row-level security policy")) {
            return "Unable to save this record because the database "
                + "security policy does not allow this operation.\n\n"
                + "Please check the Supabase RLS permissions.";
        }

        if (lower.contains("403")
                || lower.contains("permission denied")) {
            return "You do not have permission to perform this action. "
                + "Please check your account permissions.";
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
    
    public static boolean showConfirmation(
            Window owner, String title, String message) {

        Stage dialog = new Stage();
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.initModality(Modality.APPLICATION_MODAL);

        if (owner != null) {
            dialog.initOwner(owner);
        }

        dialog.setResizable(false);

        final boolean[] confirmed = {false};

        Label icon = new Label("?");
        icon.setAlignment(Pos.CENTER);
        icon.setMinSize(44, 44);
        icon.setMaxSize(44, 44);
        icon.setStyle(
            "-fx-background-color: #DBEAFE;" +
            "-fx-background-radius: 14;" +
            "-fx-text-fill: #2563EB;" +
            "-fx-font-size: 23px;" +
            "-fx-font-weight: bold;"
        );

        Label titleLabel = new Label(title);
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
        HBox header = new HBox(14, icon, heading);
        header.setAlignment(Pos.CENTER_LEFT);

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setStyle(
            "-fx-font-size: 14px;" +
            "-fx-text-fill: #475569;"
        );

        Button cancelButton = new Button("Cancel");
        cancelButton.setPrefSize(105, 38);
        cancelButton.setStyle(
            "-fx-background-color: #F1F5F9;" +
            "-fx-background-radius: 9;" +
            "-fx-text-fill: #475569;" +
            "-fx-font-weight: bold;" +
            "-fx-cursor: hand;"
        );
        cancelButton.setOnAction(e -> dialog.close());

        boolean isDelete = title != null
            && title.toLowerCase().contains("delete");

        Button confirmButton = new Button(
            isDelete ? "Delete" : "Confirm"
        );

        confirmButton.setPrefSize(120, 38);
        confirmButton.setDefaultButton(true);

        confirmButton.setStyle(
            "-fx-background-color: "
            + (isDelete ? "#DC3545" : "#2563EB") + ";"
            + "-fx-background-radius: 9;"
            + "-fx-text-fill: white;"
            + "-fx-font-size: 13px;"
            + "-fx-font-weight: bold;"
            + "-fx-cursor: hand;"
        );

        confirmButton.setOnAction(e -> {
            confirmed[0] = true;
            dialog.close();
        });


        HBox footer = new HBox(10, cancelButton, confirmButton);
        footer.setAlignment(Pos.CENTER_RIGHT);

        VBox content = new VBox(22, header, messageLabel, footer);
        content.setPadding(new Insets(28));
        content.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 16;" +
            "-fx-border-color: #E2E8F0;" +
            "-fx-border-radius: 16;"
        );

        VBox root = new VBox(content);
        root.setPadding(new Insets(12));
        root.setStyle("-fx-background-color: transparent;");

        Scene scene = new Scene(root, 500, 300);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);

        dialog.setScene(scene);

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
        return confirmed[0];
    }

    
    public static void showNotice(
            Window owner,
            String title,
            String message) {

        Stage dialog = new Stage();
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.initModality(Modality.APPLICATION_MODAL);

        if (owner != null) {
            dialog.initOwner(owner);
        }

        Label icon = new Label("i");
        icon.setAlignment(Pos.CENTER);
        icon.setMinSize(48, 48);
        icon.setMaxSize(48, 48);
        icon.setStyle(
                "-fx-background-color: #DBEAFE;" +
                "-fx-background-radius: 14;" +
                "-fx-text-fill: #2563EB;" +
                "-fx-font-size: 25px;" +
                "-fx-font-weight: bold;"
        );

        Label heading = new Label(
                title == null ? "Notification" : title
        );
        heading.setWrapText(true);
        heading.setMaxWidth(380);
        heading.setStyle(
                "-fx-font-size: 20px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #10264A;"
        );

        Label subtitle = new Label("System Configuration");
        subtitle.setStyle(
                "-fx-font-size: 12px;" +
                "-fx-text-fill: #7187AC;"
        );

        VBox headingBox = new VBox(6, heading, subtitle);
        headingBox.setAlignment(Pos.CENTER_LEFT);

        HBox header = new HBox(16, icon, headingBox);
        header.setAlignment(Pos.CENTER_LEFT);

        Label messageLabel = new Label(
                message == null ? "" : message
        );
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(Double.MAX_VALUE);
        messageLabel.setStyle(
                "-fx-font-size: 14px;" +
                "-fx-text-fill: #475569;" +
                "-fx-line-spacing: 5px;"
        );

        VBox messageCard = new VBox(messageLabel);
        messageCard.setPadding(new Insets(20));
        messageCard.setMinHeight(Region.USE_PREF_SIZE);
        messageCard.setMaxWidth(Double.MAX_VALUE);
        messageCard.setStyle(
                "-fx-background-color: #F5F8FD;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: #E4ECF7;" +
                "-fx-border-radius: 12;"
        );

        Button closeButton = new Button("Close");
        closeButton.setPrefSize(120, 40);
        closeButton.setDefaultButton(true);
        closeButton.setStyle(
                "-fx-background-color: #2563EB;" +
                "-fx-background-radius: 9;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-cursor: hand;"
        );
        closeButton.setOnAction(e -> dialog.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox footer = new HBox(spacer, closeButton);
        footer.setAlignment(Pos.CENTER_RIGHT);

        VBox content = new VBox(22, header, messageCard, footer);
        content.setPadding(new Insets(28));
        content.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 16;" +
                "-fx-border-color: #E2E8F0;" +
                "-fx-border-radius: 16;"
        );

        VBox root = new VBox(content);
        root.setPadding(new Insets(12));
        root.setStyle("-fx-background-color: transparent;");

        Scene scene = new Scene(root, 540, Region.USE_COMPUTED_SIZE);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);

        dialog.setScene(scene);
        dialog.sizeToScene();
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


}
