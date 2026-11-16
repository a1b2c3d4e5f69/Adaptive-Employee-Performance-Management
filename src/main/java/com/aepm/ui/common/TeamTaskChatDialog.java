package com.aepm.ui.common;

import com.aepm.logic.task.TeamTaskService;
import com.aepm.model.TaskChatMessage;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;

import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import java.util.List;
import java.util.UUID;

public class TeamTaskChatDialog {

    private final TeamTaskService service =
            new TeamTaskService();

    private final UUID taskId;

    private final VBox messageBox =
            new VBox(10);

    private final TextField messageField =
            new TextField();

    private final Button sendButton =
            new Button("Send");

    private final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern(
                    "dd MMM yyyy, hh:mm a"
            );


    private TeamTaskChatDialog(
            UUID taskId
    ) {
        this.taskId = taskId;
    }


    public static void show(
            Window owner,
            UUID taskId,
            String taskTitle
    ) {

        TeamTaskChatDialog dialog =
                new TeamTaskChatDialog(
                        taskId
                );

        dialog.open(
                owner,
                taskTitle
        );
    }


    private void open(
            Window owner,
            String taskTitle
    ) {

        Stage stage =
                new Stage();

        stage.initModality(
                Modality.WINDOW_MODAL
        );

        stage.initOwner(
                owner
        );

        stage.setTitle(
                "Team Task Chat"
        );


        Label title =
                new Label(
                        taskTitle == null
                                ? "Team Task Chat"
                                : taskTitle
                );

        title.getStyleClass().add(
                "manager-section-title"
        );


        Label subtitle =
                new Label(
                        "Team members can discuss this task here."
                );

        subtitle.getStyleClass().add(
                "manager-section-subtitle"
        );


        VBox header =
                new VBox(
                        4,
                        title,
                        subtitle
                );

        header.setPadding(
                new Insets(
                        20
                )
        );


        ScrollPane scroll =
                new ScrollPane(
                        messageBox
                );

        scroll.setFitToWidth(
                true
        );

        scroll.setHbarPolicy(
                ScrollPane.ScrollBarPolicy.NEVER
        );

        messageBox.setPadding(
                new Insets(
                        16
                )
        );


        messageField.setPromptText(
                "Type a message..."
        );

        messageField
                .getStyleClass()
                .add(
                        "manager-input"
                );


        sendButton
                .getStyleClass()
                .add(
                        "manager-button-primary"
                );


        HBox composer =
                new HBox(
                        10,
                        messageField,
                        sendButton
                );

        composer.setAlignment(
                Pos.CENTER_LEFT
        );

        composer.setPadding(
                new Insets(
                        14
                )
        );

        HBox.setHgrow(
                messageField,
                javafx.scene.layout.Priority.ALWAYS
        );


        BorderPane root =
                new BorderPane();

        root.getStyleClass().add(
                "manager-root"
        );

        root.setTop(
                header
        );

        root.setCenter(
                scroll
        );

        root.setBottom(
                composer
        );


        sendButton.setOnAction(
                event -> send()
        );

        messageField.setOnAction(
                event -> send()
        );


        Scene scene =
                new Scene(
                        root,
                        680,
                        620
                );

        scene.getStylesheets().add(
                getClass()
                        .getResource(
                                "/css/app.css"
                        )
                        .toExternalForm()
        );

        scene.getStylesheets().add(
                getClass()
                        .getResource(
                                "/css/manager.css"
                        )
                        .toExternalForm()
        );


        stage.setScene(
                scene
        );

        stage.show();


        refresh();
    }


    private void refresh() {

        Task<List<TaskChatMessage>> worker =
                new Task<>() {

                    @Override
                    protected List<TaskChatMessage> call()
                            throws Exception {

                        return service
                                .getMessages(
                                        taskId
                                );
                    }
                };


        worker.setOnSucceeded(
                event -> {

                    messageBox
                            .getChildren()
                            .clear();


                    for (
                            TaskChatMessage message
                            : worker.getValue()
                    ) {

                        VBox card =
                                buildMessage(
                                        message
                                );

                        messageBox
                                .getChildren()
                                .add(
                                        card
                                );
                    }
                }
        );


        worker.setOnFailed(
                event -> {

                    Throwable error =
                            worker
                                    .getException();

                    Label label =
                            new Label(
                                    error == null
                                            ? "Unable to load messages."
                                            : error.getMessage()
                            );

                    messageBox
                            .getChildren()
                            .setAll(
                                    label
                            );
                }
        );


        Thread thread =
                new Thread(
                        worker
                );

        thread.setDaemon(
                true
        );

        thread.start();
    }


    private VBox buildMessage(
            TaskChatMessage message
    ) {

        Label sender =
                new Label(
                        message.getSenderName() == null
                                ? "Employee"
                                : message.getSenderName()
                );

        sender.setStyle(
                "-fx-font-weight: 700;"
        );


        Label text =
                new Label(
                        message.getMessageText()
                );

        text.setWrapText(
                true
        );


        String dateText =
                "";

        if (message.getCreatedAt() != null) {

            dateText =
                    message
                            .getCreatedAt()
                            .atZoneSameInstant(
                                    ZoneId.of(
                                            "Asia/Kuala_Lumpur"
                                    )
                            )
                            .format(
                                    formatter
                            );
        }


        Label date =
                new Label(
                        dateText
                );

        date.getStyleClass().add(
                "manager-helper-text"
        );


        VBox card =
                new VBox(
                        5,
                        sender,
                        text,
                        date
                );

        card.getStyleClass().add(
                "manager-card"
        );

        card.setPadding(
                new Insets(
                        12
                )
        );


        return card;
    }


    private void send() {

        String text =
                messageField
                        .getText()
                        .trim();

        if (text.isEmpty()) {
            return;
        }


        sendButton.setDisable(
                true
        );


        Task<Void> worker =
                new Task<>() {

                    @Override
                    protected Void call()
                            throws Exception {

                        service.sendMessage(
                                taskId,
                                text
                        );

                        return null;
                    }
                };


        worker.setOnSucceeded(
                event -> {

                    messageField.clear();

                    sendButton.setDisable(
                            false
                    );

                    refresh();
                }
        );


        worker.setOnFailed(
                event -> {

                    sendButton.setDisable(
                            false
                    );
                }
        );


        Thread thread =
                new Thread(
                        worker
                );

        thread.setDaemon(
                true
        );

        thread.start();
    }
}