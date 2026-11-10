package com.aepm.ui.admin;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;
import javafx.stage.Stage;

import java.util.function.BooleanSupplier;

public final class NotificationCenterNavigation {

    private NotificationCenterNavigation() {
    }

    public static void install(
            BorderPane page,
            Stage stage,
            String selected,
            String subtitleText,
            BooleanSupplier canLeave
    ) {

        page.setPadding(
                new Insets(
                        30,
                        32,
                        36,
                        32
                )
        );

        page.setStyle(
                "-fx-background-color: #F3F6FD;"
                + "-fx-font-family: 'Plus Jakarta Sans';"
                + "-fx-font-size: 12px;"
        );


        Label heading =
                new Label(
                        "Notification Center"
                );

        heading
                .getStyleClass()
                .add(
                        "admin-page-title"
                );


        Label subtitle =
                new Label(
                        subtitleText
                );

        subtitle.setWrapText(
                true
        );

        subtitle
                .getStyleClass()
                .add(
                        "admin-page-subtitle"
                );


        StackPane iconBadge =
                new StackPane(
                        createNotificationIcon()
                );

        iconBadge.setMinSize(
                52,
                52
        );

        iconBadge.setPrefSize(
                52,
                52
        );

        iconBadge.setMaxSize(
                52,
                52
        );

        iconBadge.setStyle(
                "-fx-background-color: #E8EEFC;"
                + "-fx-background-radius: 12;"
        );


        VBox titleBox =
                new VBox(
                        5,
                        heading,
                        subtitle
                );


        HBox titleRow =
                new HBox(
                        16,
                        iconBadge,
                        titleBox
                );

        titleRow.setAlignment(
                Pos.CENTER_LEFT
        );


        Button announcements =
                new Button(
                        "Announcements"
                );

        Button templates =
                new Button(
                        "Templates"
                );

        Button rules =
                new Button(
                        "Automated Rules"
                );


        style(
                announcements,
                "Announcements".equals(
                        selected
                )
        );

        style(
                templates,
                "Templates".equals(
                        selected
                )
        );

        style(
                rules,
                "Automated Rules".equals(
                        selected
                )
        );


        announcements.setOnAction(
                event -> {

                    if (
                            !"Announcements".equals(
                                    selected
                            )
                                    && canLeave.getAsBoolean()
                    ) {

                        AdminAnnouncementsDialog.show(
                                stage
                        );
                    }
                }
        );


        templates.setOnAction(
                event -> {

                    if (
                            !"Templates".equals(
                                    selected
                            )
                                    && canLeave.getAsBoolean()
                    ) {

                        AdminNotificationTemplatesPage.show(
                                stage
                        );
                    }
                }
        );


        rules.setOnAction(
                event -> {

                    if (
                            !"Automated Rules".equals(
                                    selected
                            )
                                    && canLeave.getAsBoolean()
                    ) {

                        AdminNotificationRulesPage.show(
                                stage
                        );
                    }
                }
        );


        HBox navigation =
                new HBox(
                        10,
                        announcements,
                        templates,
                        rules
                );

        navigation.setAlignment(
                Pos.CENTER_LEFT
        );


        VBox header =
                new VBox(
                        22,
                        titleRow,
                        navigation
                );

        header.setPadding(
                new Insets(
                        0,
                        0,
                        22,
                        0
                )
        );


        page.setTop(
                header
        );
            if (stage.getScene() != null
            && stage.getScene().getRoot() instanceof BorderPane shell) {

        Node sidebar = shell.getLeft();

        if (sidebar != null) {

            for (Node node : sidebar.lookupAll(".button")) {

                if (!(node instanceof Button button)) {
                    continue;
                }

                String text = button.getText();

                if (text == null) {
                    continue;
                }


                // -------------------------------------------------
                // REPORTS
                // -------------------------------------------------

                if (text.contains("Reports")
                        && !text.contains("Anonymous")) {

                    button.setOnAction(event -> {

                        if (!canLeave.getAsBoolean()) {
                            return;
                        }

                        com.aepm.ui.navigation.NavigationManager.navigate(
                                stage,
                                "/fxml/admin/adminReport.fxml"
                        );
                    });
                }


                // -------------------------------------------------
                // NOTIFICATIONS ACTIVE STYLE
                // -------------------------------------------------

                button.getStyleClass()
                        .remove("admin-nav-button-active");

                if (text.contains("Notifications")
                        || text.contains("Notification/Announcements")) {

                    button.getStyleClass()
                            .remove("admin-nav-button");

                    if (!button.getStyleClass()
                            .contains("admin-nav-button-active")) {

                        button.getStyleClass()
                                .add("admin-nav-button-active");
                    }

                } else if (!button.getStyleClass()
                        .contains("logout-button")) {

                    if (!button.getStyleClass()
                            .contains("admin-nav-button")) {

                        button.getStyleClass()
                                .add("admin-nav-button");
                    }
                }
            }
        }
    }
    }


    private static void style(
            Button button,
            boolean selected
    ) {

        button.setMinHeight(
                38
        );

        button.setPrefHeight(
                38
        );

        button.setMaxHeight(
                38
        );


        if (selected) {

            button.setStyle(
                    "-fx-background-color: #1E3A8A;"
                    + "-fx-text-fill: #FFFFFF;"
                    + "-fx-background-radius: 8;"
                    + "-fx-border-color: #1E3A8A;"
                    + "-fx-border-radius: 8;"
                    + "-fx-border-width: 1;"
                    + "-fx-padding: 8 18;"
                    + "-fx-font-family: 'Plus Jakarta Sans';"
                    + "-fx-font-size: 12px;"
                    + "-fx-font-weight: bold;"
                    + "-fx-cursor: hand;"
            );

        } else {

            button.setStyle(
                    "-fx-background-color: #FFFFFF;"
                    + "-fx-text-fill: #1E3A8A;"
                    + "-fx-background-radius: 8;"
                    + "-fx-border-color: #D9DEE8;"
                    + "-fx-border-radius: 8;"
                    + "-fx-border-width: 1;"
                    + "-fx-padding: 8 18;"
                    + "-fx-font-family: 'Plus Jakarta Sans';"
                    + "-fx-font-size: 12px;"
                    + "-fx-font-weight: bold;"
                    + "-fx-cursor: hand;"
            );
        }
    }


    private static Node createNotificationIcon() {

        Polyline speaker =
                new Polyline(
                        2, 13,
                        8, 13,
                        15, 8,
                        15, 22,
                        8, 17,
                        2, 17,
                        2, 13
                );

        speaker.setFill(
                Color.TRANSPARENT
        );

        speaker.setStroke(
                Color.web(
                        "#1E3A8A"
                )
        );

        speaker.setStrokeWidth(
                2
        );


        Line sound1 =
                new Line(
                        18,
                        11,
                        21,
                        8
                );

        Line sound2 =
                new Line(
                        18,
                        19,
                        21,
                        22
                );


        sound1.setStroke(
                Color.web(
                        "#1E3A8A"
                )
        );

        sound2.setStroke(
                Color.web(
                        "#1E3A8A"
                )
        );


        sound1.setStrokeWidth(
                2
        );

        sound2.setStrokeWidth(
                2
        );


        StackPane icon =
                new StackPane(
                        speaker,
                        sound1,
                        sound2
                );

        icon.setPrefSize(
                30,
                30
        );


        return icon;
    }
}