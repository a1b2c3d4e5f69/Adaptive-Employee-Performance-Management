package com.aepm.ui.navigation;

import com.aepm.ui.components.NotificationBell;
import com.aepm.util.SessionManager;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public final class NavigationManager {

    private NavigationManager() {
    }

    public static void navigate(Stage stage, String fxmlPath) {
        try {
            URL resource = NavigationManager.class.getResource(fxmlPath);
            if (resource == null) {
                throw new IllegalArgumentException("FXML file not found: " + fxmlPath);
            }

            Parent root = FXMLLoader.load(resource);
            updateTopBarUser(root);
            installNotificationBell(root, fxmlPath);

            Scene scene = stage.getScene();
            if (scene == null) {
                scene = new Scene(root, 1200, 750);
                URL css = NavigationManager.class.getResource("/css/app.css");
                if (css != null) {
                    scene.getStylesheets().add(css.toExternalForm());
                }
                stage.setScene(scene);
            } else {
                scene.setRoot(root);
            }

            if (!stage.isShowing()) {
                stage.show();
            }
            if (!stage.isMaximized()) {
                stage.setMaximized(true);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to load page: " + fxmlPath, e);
        }
    }

    private static void updateTopBarUser(Parent root) {
        var user = SessionManager.getCurrentUser();

        if (user == null) {
            return;
        }

        String name = user.getFullName();

        if (name == null || name.isBlank()) {
            return;
        }

        Node welcomeNode = root.lookup("#welcomeLabel");
        if (welcomeNode instanceof Label welcomeLabel) {
            welcomeLabel.setText("Welcome, " + name);
        }

        Node nameNode = root.lookup("#topNameLabel");
        if (nameNode instanceof Label topNameLabel) {
            topNameLabel.setText(name);
        }
    }

    private static void installNotificationBell(Parent root, String fxmlPath) {
        if (fxmlPath.startsWith("/fxml/auth/")
                || SessionManager.getCurrentUser() == null
                || containsNotificationBell(root)) {
            return;
        }

        // Preferred contract: fx:id="notificationTopBar" on the header HBox.
        HBox topBar = findHBoxById(root, "notificationTopBar");

        // Backward-compatible support for existing page headers.
        if (topBar == null) {
            Node header = findHeader(root);
            if (header != null) {
                topBar = findUserTopBar(header);
            }
        }

        if (topBar == null) {
            System.err.println("[Notification] Top bar not found: " + fxmlPath
                    + " (set fx:id=\"notificationTopBar\" on the header HBox)");
            return;
        }

        int userIndex = findUserContainerIndex(topBar);
        if (userIndex < 0) {
            // Explicitly tagged headers can safely append the bell at the right.
            if (!"notificationTopBar".equals(topBar.getId())) {
                System.err.println("[Notification] User info not found: " + fxmlPath);
                return;
            }
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            topBar.getChildren().add(spacer);
            userIndex = topBar.getChildren().size();
        }

        NotificationBell bell = new NotificationBell();
        bell.setMinSize(48, 48);
        bell.setPrefSize(48, 48);
        bell.setMaxSize(48, 48);
        topBar.getChildren().add(userIndex, bell);
        System.out.println("[Notification] Bell installed: " + fxmlPath);
    }

    private static Node findHeader(Parent root) {
        if (root instanceof BorderPane borderPane && borderPane.getTop() != null) {
            return borderPane.getTop();
        }
        if (root instanceof VBox vbox && !vbox.getChildren().isEmpty()) {
            return vbox.getChildren().get(0);
        }
        return root;
    }

    private static HBox findHBoxById(Node node, String id) {
        if (node == null) {
            return null;
        }
        if (node instanceof HBox hbox && id.equals(node.getId())) {
            return hbox;
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                HBox found = findHBoxById(child, id);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static HBox findUserTopBar(Node node) {
        if (node == null) {
            return null;
        }
        if (node instanceof HBox hbox && findUserContainerIndex(hbox) >= 0) {
            return hbox;
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                HBox found = findUserTopBar(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static int findUserContainerIndex(HBox hbox) {
        for (int i = 0; i < hbox.getChildren().size(); i++) {
            Node child = hbox.getChildren().get(i);
            if (containsUserLabel(child)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean containsUserLabel(Node node) {
        if (node == null) {
            return false;
        }
        String id = node.getId();
        if ("topNameLabel".equals(id) || "topRoleLabel".equals(id)) {
            return true;
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                if (containsUserLabel(child)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean containsNotificationBell(Node node) {
        if (node == null) {
            return false;
        }
        if (node instanceof NotificationBell) {
            return true;
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                if (containsNotificationBell(child)) {
                    return true;
                }
            }
        }
        return false;
    }
}
