package com.aepm.ui.navigation;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public final class NavigationManager {

    private NavigationManager() {
    }


    public static void navigate(
            Stage stage,
            String fxmlPath
    ) {

        try {

            URL resource =
                    NavigationManager.class
                            .getResource(fxmlPath);


            if (resource == null) {

                throw new IllegalArgumentException(
                        "FXML file not found: "
                                + fxmlPath
                );
            }


            FXMLLoader loader =
                    new FXMLLoader(resource);


            Parent root =
                    loader.load();


            Scene scene =
                    new Scene(
                            root,
                            1200,
                            750
                    );


            URL css =
                    NavigationManager.class
                            .getResource(
                                    "/css/app.css"
                            );


            if (css != null) {

                scene
                        .getStylesheets()
                        .add(
                                css.toExternalForm()
                        );
            }


            stage.setScene(scene);

            stage.centerOnScreen();


        } catch (IOException e) {

            throw new RuntimeException(
                    "Unable to load page: "
                            + fxmlPath,
                    e
            );
        }
    }
}