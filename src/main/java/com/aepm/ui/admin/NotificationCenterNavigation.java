package com.aepm.ui.admin;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.function.BooleanSupplier;

public final class NotificationCenterNavigation {

    private NotificationCenterNavigation() {
    }

    public static void install(
            BorderPane page,
            Stage stage,
            String selected,
            BooleanSupplier canLeave
    ) {
        Button announcements = new Button("Announcements");
        Button templates = new Button("Templates");
        Button rules = new Button("Automated Rules");

        style(announcements, "Announcements".equals(selected));
        style(templates, "Templates".equals(selected));
        style(rules, "Automated Rules".equals(selected));

        announcements.setOnAction(event -> {
            if (!"Announcements".equals(selected)
                    && canLeave.getAsBoolean()) {
                AdminAnnouncementsDialog.show(stage);
            }
        });

        templates.setOnAction(event -> {
            if (!"Templates".equals(selected)
                    && canLeave.getAsBoolean()) {
                AdminNotificationTemplatesPage.show(stage);
            }
        });
        rules.setOnAction(event -> {
            if (!"Automated Rules".equals(selected)
                    && canLeave.getAsBoolean()) {
                AdminNotificationRulesPage.show(stage);
            }
        });

        HBox navigation = new HBox(10, announcements, templates, rules);
        navigation.setPadding(new Insets(0, 0, 18, 0));

        Node originalHeader = page.getTop();
        page.setTop(null);

        VBox header = new VBox();

        if (originalHeader != null) {
            header.getChildren().add(originalHeader);
        }

        header.getChildren().add(navigation);
        page.setTop(header);
    }

    private static void style(Button button, boolean selected) {
        button.setMinHeight(44);
        button.setStyle(
                "-fx-background-color: "
                + (selected ? "#2563EB" : "#EAF2FF") + ";"
                + "-fx-text-fill: "
                + (selected ? "white" : "#2563EB") + ";"
                + "-fx-background-radius: 10;"
                + "-fx-border-color: transparent;"
                + "-fx-padding: 10 22;"
                + "-fx-font-size: 14px;"
                + "-fx-font-weight: bold;"
                + "-fx-cursor: hand;"
        );
    }
}