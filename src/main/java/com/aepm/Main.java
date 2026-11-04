
package com.aepm;

import com.aepm.ui.navigation.NavigationManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        stage.setTitle(
            "AEPM - Adaptive Employee Performance Management System"
        );

        stage.setMinWidth(900);
        stage.setMinHeight(600);

        // Open Login page using the same navigation system.
        NavigationManager.navigate(
            stage,
            "/fxml/auth/login.fxml"
        );

        // Keep the application maximized.
        stage.setMaximized(true);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
