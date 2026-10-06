package com.aepm;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader loader =
                new FXMLLoader(
                        getClass().getResource(
                                "/fxml/auth/login.fxml"
                        )
                );

        Parent root = loader.load();

        Scene scene =
                new Scene(
                        root,
                        1200,
                        750
                );

        scene.getStylesheets().add(
                getClass()
                        .getResource(
                                "/css/app.css"
                        )
                        .toExternalForm()
        );

        stage.setTitle(
                "AEPM - Adaptive Employee Performance Management System"
        );

        stage.setMinWidth(900);
        stage.setMinHeight(600);

        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}