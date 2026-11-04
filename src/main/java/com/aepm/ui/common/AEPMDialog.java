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
        titleLabel.setMinWidth(0);
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        titleLabel.setMinHeight(Region.USE_PREF_SIZE);
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

        subtitle.setWrapText(true);
        subtitle.setMinHeight(Region.USE_PREF_SIZE);

        VBox heading = new VBox(4, titleLabel, subtitle);
        heading.setAlignment(Pos.CENTER_LEFT);
        heading.setMinWidth(0);
        heading.setMaxWidth(Double.MAX_VALUE);

        HBox.setHgrow(heading, Priority.ALWAYS);

        HBox header = new HBox(14, icon, heading);
        header.setAlignment(Pos.CENTER_LEFT);

        Label messageLabel = new Label(
            friendlyMessage(message, error)
        );
        messageLabel.setWrapText(true);
        messageLabel.setMinWidth(0);
        messageLabel.setMaxWidth(Double.MAX_VALUE);
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

        // Keep a readable width and let the height fit wrapped content.
        root.setPrefWidth(520);
        Scene scene = new Scene(root);
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

    public enum RecommendationChoice {
        VIEW_HISTORY,
        GENERATE_NEW,
        CANCEL
    }

    public static RecommendationChoice showRecommendationChoice(
            Window owner
    ) {
        Stage dialog = new Stage(StageStyle.TRANSPARENT);
        dialog.setTitle("Recommendations Already Exist");

        if (owner != null) {
            dialog.initOwner(owner);
            dialog.initModality(Modality.WINDOW_MODAL);
        } else {
            dialog.initModality(Modality.APPLICATION_MODAL);
        }

        RecommendationChoice[] result = {
                RecommendationChoice.CANCEL
        };

        Label icon = new Label("AI");
        icon.setAlignment(Pos.CENTER);
        icon.setMinSize(48, 48);
        icon.setStyle(
                "-fx-background-color: #DBEAFE;"
                + "-fx-background-radius: 14;"
                + "-fx-text-fill: #2563EB;"
                + "-fx-font-size: 18px;"
                + "-fx-font-weight: bold;"
        );

        Label title = new Label("Recommendations Already Exist");
        title.setWrapText(true);
        title.setStyle(
                "-fx-font-size: 20px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: #0F172A;"
        );

        HBox header = new HBox(14, icon, title);
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(title, Priority.ALWAYS);

        Label message = new Label(
                "Saved recommendations are available for this KPI result.\n\n"
                + "View existing history, or generate and save a new version. "
                + "Generating again uses another AI request."
        );
        message.setWrapText(true);
        message.setMinHeight(Region.USE_PREF_SIZE);
        message.setStyle(
                "-fx-font-size: 14px;"
                + "-fx-text-fill: #475569;"
        );

        Button cancel = new Button("Cancel");
        Button history = new Button("View History");
        Button generate = new Button("Generate New Version");

        String base =
                "-fx-background-radius: 10;"
                + "-fx-border-radius: 10;"
                + "-fx-padding: 11 16;"
                + "-fx-font-size: 13px;"
                + "-fx-font-weight: bold;"
                + "-fx-cursor: hand;";

        cancel.setStyle(base
                + "-fx-background-color: white;"
                + "-fx-border-color: #CBD5E1;"
                + "-fx-text-fill: #334155;");

        history.setStyle(base
                + "-fx-background-color: #2563EB;"
                + "-fx-text-fill: white;");

        generate.setStyle(base
                + "-fx-background-color: #EFF6FF;"
                + "-fx-text-fill: #2563EB;");

        cancel.setCancelButton(true);
        history.setDefaultButton(true);

        cancel.setOnAction(event -> dialog.close());

        history.setOnAction(event -> {
            result[0] = RecommendationChoice.VIEW_HISTORY;
            dialog.close();
        });

        generate.setOnAction(event -> {
            result[0] = RecommendationChoice.GENERATE_NEW;
            dialog.close();
        });

        javafx.scene.layout.FlowPane buttons =
                new javafx.scene.layout.FlowPane(10, 10);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.getChildren().addAll(cancel, history, generate);

        VBox panel = new VBox(22, header, message, buttons);
        panel.setPadding(new Insets(26));
        panel.setStyle(
                "-fx-font-family: 'Segoe UI';"
                + "-fx-background-color: white;"
                + "-fx-background-radius: 18;"
                + "-fx-border-color: #DBE4F0;"
                + "-fx-border-radius: 18;"
        );

        var screens = owner == null
                ? java.util.List.of(javafx.stage.Screen.getPrimary())
                : javafx.stage.Screen.getScreensForRectangle(
                        owner.getX(), owner.getY(),
                        owner.getWidth(), owner.getHeight()
                );

        var screen = screens.isEmpty()
                ? javafx.stage.Screen.getPrimary() : screens.get(0);
        var bounds = screen.getVisualBounds();

        double width = Math.min(640, bounds.getWidth() - 32);
        panel.setPrefWidth(width - 16);

        VBox root = new VBox(panel);
        root.setPadding(new Insets(8));
        root.setStyle("-fx-background-color: transparent;");

        Scene scene = new Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);

        dialog.setScene(scene);
        dialog.setResizable(false);
        dialog.setOnShown(event -> {
            dialog.setX(bounds.getMinX()
                    + (bounds.getWidth() - dialog.getWidth()) / 2);
            dialog.setY(bounds.getMinY()
                    + (bounds.getHeight() - dialog.getHeight()) / 2);
        });

        dialog.showAndWait();
        return result[0];
    }
}
