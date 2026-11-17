package com.aepm.ui.admin;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
            String subtitleText,
            BooleanSupplier canLeave
    ) {

        // =====================================================
        // PAGE STYLE
        // =====================================================

        if (
                !page.getStyleClass()
                        .contains("admin-content")
        ) {

            page.getStyleClass().add(
                    "admin-content"
            );
        }


        // =====================================================
        // TITLE
        // =====================================================

        Label heading =
                new Label(
                        "Notification Center"
                );


        heading.getStyleClass().add(
                "admin-page-title"
        );


        Label subtitle =
                new Label(
                        subtitleText
                );


        subtitle.setWrapText(
                true
        );


        subtitle.getStyleClass().add(
                "admin-page-subtitle"
        );


        VBox titleBox =
                new VBox(
                        heading,
                        subtitle
                );


        titleBox.getStyleClass().add(
                "notification-center-title-box"
        );


        HBox titleRow =
                new HBox(
                        titleBox
                );


        titleRow.setAlignment(
                Pos.CENTER_LEFT
        );


        // =====================================================
        // NAVIGATION BUTTONS
        // =====================================================

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


        // =====================================================
        // BUTTON ACTIONS
        // =====================================================

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


        // =====================================================
        // NAVIGATION ROW
        // =====================================================

        HBox navigation =
                new HBox(
                        announcements,
                        templates,
                        rules
                );


        navigation.getStyleClass().add(
                "notification-center-navigation"
        );


        // =====================================================
        // HEADER
        // =====================================================

        VBox header =
                new VBox(
                        titleRow,
                        navigation
                );


        header.getStyleClass().add(
                "notification-center-header"
        );


        page.setTop(
                header
        );


        // =====================================================
        // SIDEBAR
        // =====================================================

        if (
                stage.getScene() != null
                        && stage.getScene()
                        .getRoot()
                        instanceof BorderPane shell
        ) {

            Node sidebar =
                    shell.getLeft();


            if (sidebar != null) {

                for (
                        Node node
                        : sidebar.lookupAll(
                                ".button"
                        )
                ) {

                    if (
                            !(node
                                    instanceof Button button)
                    ) {

                        continue;
                    }


                    String text =
                            button.getText();


                    if (text == null) {

                        continue;
                    }


                    // =========================================
                    // REPORTS
                    // =========================================

                    if (
                            text.contains(
                                    "Reports"
                            )
                                    && !text.contains(
                                    "Anonymous"
                            )
                    ) {

                        button.setOnAction(
                                event -> {

                                    if (
                                            !canLeave
                                                    .getAsBoolean()
                                    ) {

                                        return;
                                    }


                                    com.aepm.ui.navigation
                                            .NavigationManager
                                            .navigate(

                                                    stage,

                                                    "/fxml/admin/adminReport.fxml"
                                            );
                                }
                        );
                    }


                    // =========================================
                    // SIDEBAR ACTIVE STYLE
                    // =========================================

                    button
                            .getStyleClass()
                            .remove(
                                    "admin-nav-button-active"
                            );


                    if (
                            text.contains(
                                    "Notifications"
                            )
                                    || text.contains(
                                    "Notification/Announcements"
                            )
                    ) {

                        button
                                .getStyleClass()
                                .remove(
                                        "admin-nav-button"
                                );


                        if (
                                !button
                                        .getStyleClass()
                                        .contains(
                                                "admin-nav-button-active"
                                        )
                        ) {

                            button
                                    .getStyleClass()
                                    .add(
                                            "admin-nav-button-active"
                                    );
                        }

                    } else if (
                            !button
                                    .getStyleClass()
                                    .contains(
                                            "logout-button"
                                    )
                    ) {

                        if (
                                !button
                                        .getStyleClass()
                                        .contains(
                                                "admin-nav-button"
                                        )
                        ) {

                            button
                                    .getStyleClass()
                                    .add(
                                            "admin-nav-button"
                                    );
                        }
                    }
                }
            }
        }
    }


    // =========================================================
    // NOTIFICATION NAV BUTTON STYLE
    // =========================================================

    private static void style(
            Button button,
            boolean selected
    ) {

        button
                .getStyleClass()
                .removeAll(

                        "notification-nav-button",

                        "notification-nav-button-active"
                );


        button
                .getStyleClass()
                .add(
                        "notification-nav-button"
                );


        if (selected) {

            button
                    .getStyleClass()
                    .add(
                            "notification-nav-button-active"
                    );
        }
    }
}