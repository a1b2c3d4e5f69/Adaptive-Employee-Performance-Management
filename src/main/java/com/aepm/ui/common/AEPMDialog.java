package com.aepm.ui.common;

import com.aepm.model.User;
import com.aepm.util.SessionManager;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
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

        boolean adminTheme =
        isAdminOwner(owner);

        String accent =
                error ? "#DC3545" : adminTheme ? "#1E3A8A" : "#2563EB";

        String softAccent =
                error ? "#FEE2E2" : adminTheme ? "#E8EEFC" : "#DBEAFE";

        String symbol =
                error ? "!" : "✓";

        Label icon = new Label(symbol);
        icon.setAlignment(Pos.CENTER);
        icon.setMinSize(44, 44);
        icon.setMaxSize(44, 44);
        icon.setStyle(
            "-fx-background-color: "
                    + softAccent
                    + ";"
                    + "-fx-background-radius: 12;"
                    + "-fx-text-fill: "
                    + accent
                    + ";"
                    + "-fx-font-size: 22px;"
                    + "-fx-font-weight: bold;"
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
            "-fx-background-color: "
                    + accent
                    + ";"
                    + "-fx-background-radius: 8;"
                    + "-fx-border-radius: 8;"
                    + "-fx-text-fill: white;"
                    + "-fx-font-family: 'Plus Jakarta Sans';"
                    + "-fx-font-size: 12px;"
                    + "-fx-font-weight: bold;"
                    + "-fx-cursor: hand;"
        );
        okButton.setOnAction(e -> dialog.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox footer = new HBox(spacer, okButton);
        footer.setAlignment(Pos.CENTER_RIGHT);

        VBox content = new VBox(22, header, messageLabel, footer);
        content.setPadding(new Insets(28));
        content.setStyle(
            "-fx-background-color: white;"
                    + "-fx-background-radius: 12;"
                    + "-fx-border-color: #E3E7EF;"
                    + "-fx-border-radius: 12;"
                    + "-fx-border-width: 1;"
                    + "-fx-font-family: 'Plus Jakarta Sans';"
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
    
    public static boolean showConfirmation(Window owner, String title, String message) {
        boolean adminTheme = isAdminOwner(owner);

        String primary = adminTheme ? "#1E3A8A" : "#2563EB";

        String soft = adminTheme ? "#E8EEFC" : "#DBEAFE";
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
            "-fx-background-color: "
                    + soft
                    + ";"
                    + "-fx-background-radius: 12;"
                    + "-fx-text-fill: "
                    + primary
                    + ";"
                    + "-fx-font-size: 23px;"
                    + "-fx-font-weight: bold;"
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
            "-fx-background-color: #FFFFFF;"
                    + "-fx-border-color: #D9DEE8;"
                    + "-fx-border-width: 1;"
                    + "-fx-background-radius: 8;"
                    + "-fx-border-radius: 8;"
                    + "-fx-text-fill: "
                    + (adminTheme ? "#1E3A8A" : "#475569")
                    + ";"
                    + "-fx-font-family: 'Plus Jakarta Sans';"
                    + "-fx-font-size: 12px;"
                    + "-fx-font-weight: bold;"
                    + "-fx-cursor: hand;"
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
                    + (isDelete
                        ? "#DC3545"
                        : primary)
                    + ";"
                    + "-fx-background-radius: 8;"
                    + "-fx-border-radius: 8;"
                    + "-fx-text-fill: white;"
                    + "-fx-font-family: 'Plus Jakarta Sans';"
                    + "-fx-font-size: 12px;"
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
                String message
        ) {

        User currentUser =
                SessionManager.getCurrentUser();

        boolean isEmployee =
                currentUser != null
                        && currentUser.getRoleId() == 3;

        boolean isManager =
                currentUser != null
                        && currentUser.getRoleId() == 2;


        // =========================================================
        // DIALOG
        // =========================================================

        javafx.scene.control.Dialog<Void> dialog =
                new javafx.scene.control.Dialog<>();

        dialog.setTitle(
                title == null || title.isBlank()
                        ? "Notification"
                        : title
        );

        dialog.setHeaderText(null);

        if (owner != null) {
                dialog.initOwner(owner);
        }

        javafx.scene.control.DialogPane pane =
                dialog.getDialogPane();

        pane.getButtonTypes().add(
                javafx.scene.control.ButtonType.CLOSE
        );

        // Same style direction as Task Details
        pane.setPrefSize(
                720,
                400
        );

        pane.setMinSize(
                560,
                340
        );

        dialog.setResizable(
                true
        );


        // Hide JavaFX default Close button
        Button defaultClose =
                (Button) pane.lookupButton(
                        javafx.scene.control.ButtonType.CLOSE
                );

        defaultClose.setVisible(false);
        defaultClose.setManaged(false);


        // =========================================================
        // HEADER
        // =========================================================

        Label heading =
                new Label(
                        title == null || title.isBlank()
                                ? "Notification"
                                : title
                );

        heading.setWrapText(true);

        heading.setStyle(
                "-fx-font-family: 'Plus Jakarta Sans';"
                + "-fx-font-size: 22px;"
                + "-fx-font-weight: 800;"
                + "-fx-text-fill: #10264A;"
        );


        Label subtitle =
                new Label(
                        "Review the notification information."
                );

        subtitle.setStyle(
                "-fx-font-family: 'Plus Jakarta Sans';"
                + "-fx-font-size: 12px;"
                + "-fx-text-fill: #7184A4;"
        );


        VBox headerText =
                new VBox(
                        4,
                        heading,
                        subtitle
                );


        Label headerIcon =
                new Label("✓");

        headerIcon.setAlignment(
                Pos.CENTER
        );

        headerIcon.setMinSize(
                54,
                54
        );

        headerIcon.setMaxSize(
                54,
                54
        );

        headerIcon.setStyle(
                "-fx-background-color: #FFF0B8;"
                + "-fx-background-radius: 15;"
                + "-fx-text-fill: #B86A00;"
                + "-fx-font-size: 24px;"
                + "-fx-font-weight: bold;"
        );


        Region headerSpacer =
                new Region();

        HBox.setHgrow(
                headerSpacer,
                Priority.ALWAYS
        );


        Label headerBadge =
                new Label(
                        "NOTIFICATION"
                );

        headerBadge.setStyle(
                "-fx-background-color: #FFF2DC;"
                + "-fx-text-fill: #A75A08;"
                + "-fx-background-radius: 18;"
                + "-fx-padding: 7 14;"
                + "-fx-font-family: 'Plus Jakarta Sans';"
                + "-fx-font-size: 10px;"
                + "-fx-font-weight: 800;"
        );


        HBox header =
                new HBox(
                        16,
                        headerIcon,
                        headerText,
                        headerSpacer,
                        headerBadge
                );

        header.setAlignment(
                Pos.CENTER_LEFT
        );

        header.setPadding(
                new Insets(
                        18,
                        22,
                        12,
                        22
                )
        );


        // =========================================================
        // NOTIFICATION CARD
        // =========================================================

        Label sectionTitle =
                new Label(
                        "Notification Information"
                );

        sectionTitle.setStyle(
                "-fx-font-family: 'Plus Jakarta Sans';"
                + "-fx-font-size: 14px;"
                + "-fx-font-weight: 800;"
                + "-fx-text-fill: #10264A;"
        );


        Label messageLabel =
                new Label(
                        message == null
                                ? ""
                                : message
                );

        messageLabel.setWrapText(
                true
        );

        messageLabel.setMaxWidth(
                Double.MAX_VALUE
        );

        messageLabel.setMinHeight(
                Region.USE_PREF_SIZE
        );

        messageLabel.setPadding(
                new Insets(
                        14
                )
        );

        messageLabel.setStyle(
                "-fx-background-color: #F1F6FD;"
                + "-fx-background-radius: 10;"
                + "-fx-text-fill: #273D60;"
                + "-fx-font-family: 'Plus Jakarta Sans';"
                + "-fx-font-size: 13px;"
        );


        VBox infoSection =
                new VBox(
                        10,
                        sectionTitle,
                        messageLabel
                );


        VBox card =
                new VBox(
                        14,
                        infoSection
                );

        card.setPadding(
                new Insets(
                        20
                )
        );

        card.setFillWidth(
                true
        );

        card.setStyle(
                "-fx-background-color: white;"
                + "-fx-background-radius: 14;"
                + "-fx-border-color: #DCE6F4;"
                + "-fx-border-radius: 14;"
        );


        // =========================================================
        // BODY
        // =========================================================

        VBox body =
                new VBox(
                        0,
                        header,
                        card
                );

        body.setPadding(
                new Insets(
                        8,
                        16,
                        16,
                        16
                )
        );


        ScrollPane scroll =
                new ScrollPane(
                        body
                );

        scroll.setMinHeight(
                0
        );

        scroll.setPrefViewportHeight(
                290
        );

        scroll.setFitToWidth(
                true
        );

        scroll.setPannable(
                true
        );

        scroll.setHbarPolicy(
                ScrollPane.ScrollBarPolicy.NEVER
        );

        scroll.setVbarPolicy(
                ScrollPane.ScrollBarPolicy.AS_NEEDED
        );

        scroll.setStyle(
                "-fx-background: #F6F8FC;"
                + "-fx-background-color: #F6F8FC;"
                + "-fx-border-color: transparent;"
        );

        VBox.setVgrow(
                scroll,
                Priority.ALWAYS
        );


        // =========================================================
        // CLOSE BUTTON
        // =========================================================

        Button close =
                new Button(
                        "Close"
                );


        if (isEmployee) {

                close.setStyle(
                        "-fx-background-color: #FFC700;"
                        + "-fx-text-fill: #111111;"
                        + "-fx-font-family: 'Plus Jakarta Sans';"
                        + "-fx-font-size: 12px;"
                        + "-fx-font-weight: 700;"
                        + "-fx-border-color: #FFC700;"
                        + "-fx-border-radius: 9;"
                        + "-fx-background-radius: 9;"
                        + "-fx-padding: 11 22;"
                        + "-fx-cursor: hand;"
                );

        } else if (isManager) {

                close.setStyle(
                        "-fx-background-color: #0F766E;"
                        + "-fx-text-fill: white;"
                        + "-fx-font-family: 'Plus Jakarta Sans';"
                        + "-fx-font-size: 12px;"
                        + "-fx-font-weight: 700;"
                        + "-fx-border-color: #0F766E;"
                        + "-fx-border-radius: 9;"
                        + "-fx-background-radius: 9;"
                        + "-fx-padding: 11 22;"
                        + "-fx-cursor: hand;"
                );

        } else {

                close.setStyle(
                        "-fx-background-color: #10264A;"
                        + "-fx-text-fill: white;"
                        + "-fx-font-family: 'Plus Jakarta Sans';"
                        + "-fx-font-size: 12px;"
                        + "-fx-font-weight: 700;"
                        + "-fx-border-color: #10264A;"
                        + "-fx-border-radius: 9;"
                        + "-fx-background-radius: 9;"
                        + "-fx-padding: 11 22;"
                        + "-fx-cursor: hand;"
                );
        }


        close.setOnAction(
                e -> dialog.close()
        );


        Region spacer =
                new Region();

        HBox.setHgrow(
                spacer,
                Priority.ALWAYS
        );


        HBox footer =
                new HBox(
                        10,
                        spacer,
                        close
                );

        footer.setAlignment(
                Pos.CENTER_RIGHT
        );

        footer.setPadding(
                new Insets(
                        14,
                        20,
                        14,
                        20
                )
        );

        footer.setStyle(
                "-fx-background-color: white;"
                + "-fx-border-color: #E5EBF4;"
                + "-fx-border-width: 1 0 0 0;"
        );


        // =========================================================
        // FINAL LAYOUT
        // =========================================================

        VBox dialogLayout =
                new VBox(
                        scroll,
                        footer
                );

        VBox.setVgrow(
                scroll,
                Priority.ALWAYS
        );


        pane.setContent(
                dialogLayout
        );


        dialog.showAndWait();
    }

    public enum RecommendationChoice {
        VIEW_HISTORY,
        GENERATE_NEW,
        CANCEL
    }

    public static RecommendationChoice showRecommendationChoice(Window owner) {
        boolean adminTheme = isAdminOwner(owner);

        String primary = adminTheme ? "#1E3A8A" : "#2563EB";

        String soft = adminTheme ? "#E8EEFC" : "#DBEAFE";

        String verySoft = adminTheme ? "#F3F6FD" : "#EFF6FF";
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
            "-fx-background-color: "
                    + soft
                    + ";"
                    + "-fx-background-radius: 12;"
                    + "-fx-text-fill: "
                    + primary
                    + ";"
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
                "-fx-font-family: 'Plus Jakarta Sans';"
                + "-fx-background-radius: 8;"
                + "-fx-border-radius: 8;"
                + "-fx-padding: 9 16;"
                + "-fx-min-height: 38;"
                + "-fx-font-size: 12px;"
                + "-fx-font-weight: bold;"
                + "-fx-cursor: hand;";

        cancel.setStyle(base
                + "-fx-background-color: #FFFFFF;"
                + "-fx-border-color: #C9DAD6;"
                + "-fx-border-width: 1;"
                + "-fx-text-fill: #0F766E;");

        history.setStyle(base
                + "-fx-background-color: #0F766E;"
                + "-fx-border-color: #0F766E;"
                + "-fx-text-fill: #FFFFFF;");

        generate.setStyle(base
                + "-fx-background-color: #DFF4F1;"
                + "-fx-border-color: #DFF4F1;"
                + "-fx-text-fill: #0F766E;");

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
            "-fx-font-family: 'Plus Jakarta Sans';"
                    + "-fx-background-color: white;"
                    + "-fx-background-radius: 12;"
                    + "-fx-border-color: #E3E7EF;"
                    + "-fx-border-radius: 12;"
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

    private static boolean isAdminOwner(Window owner) {

        if (owner == null
                || owner.getScene() == null
                || owner.getScene().getRoot() == null) {

            return false;
        }

        return owner.getScene()
                .getRoot()
                .getStyleClass()
                .contains("admin-root");
    }
}
