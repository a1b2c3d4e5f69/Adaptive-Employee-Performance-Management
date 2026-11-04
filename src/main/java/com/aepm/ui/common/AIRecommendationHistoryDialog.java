
package com.aepm.ui.common;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.*;

import java.net.URL;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class AIRecommendationHistoryDialog {

    private static final ZoneId MYT =
            ZoneId.of("Asia/Kuala_Lumpur");

    private AIRecommendationHistoryDialog() {}

    public static void show(
            Window owner,
            String employeeName,
            String employeeCode,
            String department,
            JsonArray rows
    ) {

        // =====================================================
        // MODAL WINDOW
        // =====================================================

        Stage dialog = new Stage(StageStyle.TRANSPARENT);

        dialog.setTitle("AI Recommendation History");

        if (owner != null) {
            dialog.initOwner(owner);
            dialog.initModality(Modality.WINDOW_MODAL);
        } else {
            dialog.initModality(Modality.APPLICATION_MODAL);
        }

        // =====================================================
        // HEADER ICON
        // =====================================================

        Label icon = new Label("▤");
        icon.getStyleClass().add("ai-history-icon");

        StackPane iconBox = new StackPane(icon);
        iconBox.getStyleClass().add("ai-history-icon-box");

        iconBox.setMinSize(58, 58);
        iconBox.setPrefSize(58, 58);
        iconBox.setMaxSize(58, 58);

        // =====================================================
        // HEADER TEXT
        // =====================================================

        Label title = new Label(
                "AI Recommendation History"
        );

        title.getStyleClass().add(
                "ai-history-title"
        );

        title.setWrapText(true);

        Label employee = new Label(
                display(employeeCode)
                + " — "
                + display(employeeName)
                + "  •  "
                + display(department)
        );

        employee.getStyleClass().add(
                "ai-history-employee"
        );

        employee.setWrapText(true);

        VBox headingText = new VBox(
                7,
                title,
                employee
        );

        headingText.setAlignment(Pos.CENTER_LEFT);

        HBox.setHgrow(
                headingText,
                Priority.ALWAYS
        );

        // =====================================================
        // TOP RIGHT CLOSE BUTTON
        // =====================================================

        Button topClose = new Button("✕");

        topClose.getStyleClass().add(
                "ai-history-close-icon"
        );

        topClose.setMinSize(36, 36);
        topClose.setPrefSize(36, 36);

        topClose.setOnAction(
                event -> dialog.close()
        );

        // =====================================================
        // HEADER LAYOUT
        // =====================================================

        HBox headerTop = new HBox(
                18,
                iconBox,
                headingText,
                topClose
        );

        headerTop.setAlignment(
                Pos.CENTER_LEFT
        );

        Label hint = new Label(
                "Latest 50 saved records. "
                + "Select a row and click View Details."
        );

        hint.getStyleClass().add(
                "ai-history-hint"
        );

        hint.setWrapText(true);

        VBox header = new VBox(
                17,
                headerTop,
                hint
        );

        header.setPadding(
                new Insets(28, 28, 22, 28)
        );

        // =====================================================
        // HISTORY TABLE
        // =====================================================

        TableView<JsonObject> table =
                new TableView<>();

        table.getStyleClass().add(
                "ai-history-table"
        );

        table.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );

        table.setFixedCellSize(43);

        table.setPrefHeight(330);
        table.setMinHeight(180);

        VBox.setVgrow(
                table,
                Priority.ALWAYS
        );

        // =====================================================
        // EMPTY HISTORY MESSAGE
        // =====================================================

        Label emptyTitle = new Label(
                "No AI Recommendations Yet"
        );

        emptyTitle.getStyleClass().add(
                "ai-history-empty-title"
        );

        Label emptyDescription = new Label(
                "Generated AI recommendations will appear here."
        );

        emptyDescription.getStyleClass().add(
                "ai-history-empty-description"
        );

        VBox emptyState = new VBox(
                8,
                emptyTitle,
                emptyDescription
        );

        emptyState.setAlignment(Pos.CENTER);

        table.setPlaceholder(emptyState);

        // =====================================================
        // TABLE COLUMNS
        // =====================================================

        TableColumn<JsonObject, String> generated =
                new TableColumn<>("Generated At (MYT)");

        generated.setPrefWidth(255);

        generated.setCellValueFactory(
                cell -> new SimpleStringProperty(
                        formatTime(
                                value(
                                        cell.getValue(),
                                        "generated_at"
                                )
                        )
                )
        );

        TableColumn<JsonObject, String> period =
                new TableColumn<>("Evaluation Period");

        period.setPrefWidth(290);

        period.setCellValueFactory(
                cell -> new SimpleStringProperty(
                        formatPeriod(
                                cell.getValue()
                        )
                )
        );

        TableColumn<JsonObject, String> kpi =
                column(
                        "KPI Result",
                        "kpi_result_id"
                );

        kpi.setPrefWidth(150);

        TableColumn<JsonObject, String> model =
                column(
                        "Model",
                        "model_name"
                );

        model.setPrefWidth(210);

        table.getColumns().addAll(
                generated,
                period,
                kpi,
                model
        );

        // =====================================================
        // LOAD SUPABASE HISTORY DATA
        // =====================================================

        if (rows != null) {

            for (JsonElement element : rows) {

                if (element != null
                        && element.isJsonObject()) {

                    table.getItems().add(
                            element.getAsJsonObject()
                                    .deepCopy()
                    );
                }
            }
        }

        // =====================================================
        // TABLE CONTAINER
        // =====================================================

        VBox tableContainer = new VBox(table);

        tableContainer.setPadding(
                new Insets(0, 28, 0, 28)
        );

        VBox.setVgrow(
                tableContainer,
                Priority.ALWAYS
        );

        // =====================================================
        // VIEW DETAILS BUTTON
        // =====================================================

        Button view = new Button(
                "View Details"
        );

        view.getStyleClass().addAll(
                "ai-history-button",
                "ai-history-primary-button"
        );

        view.setPrefWidth(160);
        view.setMinHeight(43);
        view.setDefaultButton(true);

        view.disableProperty().bind(
                table.getSelectionModel()
                        .selectedItemProperty()
                        .isNull()
        );

        view.setOnAction(event -> {

            JsonObject selected =
                    table.getSelectionModel()
                            .getSelectedItem();

            if (selected == null) {
                return;
            }

            try {

                JsonObject evidence =
                        selected.getAsJsonObject(
                                "performance_evidence"
                        );

                if (evidence == null
                        || !evidence.has("recommendation")
                        || !evidence.get(
                                "recommendation"
                        ).isJsonObject()) {

                    throw new IllegalStateException(
                            "Structured recommendation not found."
                    );
                }

                JsonObject recommendation =
                        evidence.getAsJsonObject(
                                "recommendation"
                        ).deepCopy();

                LocalDate start = LocalDate.parse(
                        value(
                                selected,
                                "period_start"
                        )
                );

                LocalDate end = LocalDate.parse(
                        value(
                                selected,
                                "period_end"
                        )
                );

                JsonElement snapshotElement = evidence.get("snapshot");

                JsonObject savedSnapshot =
                        snapshotElement != null && snapshotElement.isJsonObject()
                                ? snapshotElement.getAsJsonObject().deepCopy()
                                : null;

                AIRecommendationDialog.show(
                        dialog,
                        employeeName,
                        employeeCode,
                        department,
                        start,
                        end,
                        recommendation,
                        savedSnapshot,
                        value(selected, "generated_at"),
                        value(selected, "model_name")
                );

            } catch (RuntimeException ex) {

                AEPMDialog.showError(
                        dialog,
                        "Unable to Open Recommendation",
                        "The saved record is incomplete "
                        + "or has an unsupported format. "
                        + "Please review the record."
                );
            }
        });

        // =====================================================
        // DOUBLE CLICK TO VIEW DETAILS
        // =====================================================

        table.setRowFactory(tv -> {

            TableRow<JsonObject> row =
                    new TableRow<>();

            row.setOnMouseClicked(event -> {

                if (event.getClickCount() == 2
                        && !row.isEmpty()) {

                    table.getSelectionModel()
                            .select(row.getItem());

                    view.fire();
                }
            });

            return row;
        });

        // =====================================================
        // CLOSE BUTTON
        // =====================================================

        Button close = new Button(
                "Close"
        );

        close.getStyleClass().addAll(
                "ai-history-button",
                "ai-history-secondary-button"
        );

        close.setPrefWidth(135);
        close.setMinHeight(43);
        close.setCancelButton(true);

        close.setOnAction(
                event -> dialog.close()
        );

        // =====================================================
        // FOOTER
        // =====================================================

        Label footerHint = new Label(
                "Double-click a record to view details"
        );

        footerHint.getStyleClass().add(
                "ai-history-footer-hint"
        );

        Region spacer = new Region();

        HBox.setHgrow(
                spacer,
                Priority.ALWAYS
        );

        HBox footer = new HBox(
                12,
                footerHint,
                spacer,
                close,
                view
        );

        footer.setAlignment(
                Pos.CENTER_RIGHT
        );

        footer.setPadding(
                new Insets(22, 28, 28, 28)
        );

        // =====================================================
        // MAIN WHITE CARD
        // =====================================================

        BorderPane panel = new BorderPane();

        panel.getStyleClass().add(
                "ai-history-panel"
        );

        panel.setTop(header);
        panel.setCenter(tableContainer);
        panel.setBottom(footer);

        panel.setEffect(
                new DropShadow(
                        BlurType.GAUSSIAN,
                        Color.rgb(15, 23, 42, 0.20),
                        35,
                        0.12,
                        0,
                        12
                )
        );

        // =====================================================
        // DIMMED BACKGROUND OVER OWNER WINDOW
        // =====================================================

        StackPane root = new StackPane(panel);

        root.getStyleClass().add(
                "ai-history-overlay"
        );

        root.setAlignment(Pos.CENTER);

        root.setPadding(
                new Insets(20)
        );

        // =====================================================
        // WINDOW SIZE / POSITION
        // =====================================================

        Screen screen = Screen.getPrimary();

        if (owner != null) {

            var matches =
                    Screen.getScreensForRectangle(
                            owner.getX(),
                            owner.getY(),
                            owner.getWidth(),
                            owner.getHeight()
                    );

            if (!matches.isEmpty()) {
                screen = matches.get(0);
            }
        }

        var bounds = screen.getVisualBounds();

        double width;
        double height;
        double x;
        double y;

        if (owner != null && owner.isShowing()
                && owner.getWidth() > 0
                && owner.getHeight() > 0) {

            // Cover the current application window.
            width = owner.getWidth();
            height = owner.getHeight();

            x = owner.getX();
            y = owner.getY();

        } else {

            // Fallback when no owner exists.
            width = Math.min(
                    1200,
                    bounds.getWidth()
            );

            height = Math.min(
                    760,
                    bounds.getHeight()
            );

            x = bounds.getMinX()
                    + (bounds.getWidth() - width) / 2;

            y = bounds.getMinY()
                    + (bounds.getHeight() - height) / 2;
        }

        panel.setMaxWidth(
                Math.max(
                        300,
                        Math.min(1060, width - 40)
                )
        );

        panel.setMaxHeight(
                Math.max(
                        260,
                        Math.min(620, height - 40)
                )
        );

        panel.setPrefWidth(
                Math.min(1060, width - 40)
        );

        panel.setPrefHeight(
                Math.min(620, height - 40)
        );

        Scene scene = new Scene(
                root,
                width,
                height
        );

        scene.setFill(Color.TRANSPARENT);

        // =====================================================
        // LOAD HISTORY CSS
        // =====================================================

        URL stylesheet =
                AIRecommendationHistoryDialog.class
                        .getResource(
                                "/css/ai-recommendation-history.css"
                        );

        if (stylesheet != null) {

            scene.getStylesheets().add(
                    stylesheet.toExternalForm()
            );
        }

        // =====================================================
        // ESC KEY TO CLOSE
        // =====================================================

        scene.setOnKeyPressed(event -> {

            if (event.getCode() == KeyCode.ESCAPE) {
                dialog.close();
            }
        });

        dialog.setScene(scene);
        dialog.setResizable(false);

        dialog.setX(x);
        dialog.setY(y);

        dialog.showAndWait();
    }

    // =========================================================
    // TABLE COLUMN HELPER
    // =========================================================

    private static TableColumn<JsonObject, String> column(
            String title,
            String key
    ) {

        TableColumn<JsonObject, String> column =
                new TableColumn<>(title);

        column.setCellValueFactory(
                cell -> new SimpleStringProperty(
                        value(
                                cell.getValue(),
                                key
                        )
                )
        );

        return column;
    }

    // =========================================================
    // JSON VALUE
    // =========================================================

    private static String value(
            JsonObject object,
            String key
    ) {

        if (object == null
                || !object.has(key)) {

            return "-";
        }

        JsonElement element = object.get(key);

        if (element == null
                || element.isJsonNull()) {

            return "-";
        }

        return element.getAsString();
    }

    // =========================================================
    // DISPLAY TEXT
    // =========================================================

    private static String display(String text) {

        return text == null || text.isBlank()
                ? "Not available"
                : text;
    }

    // =========================================================
    // PERIOD FORMAT
    // =========================================================

    private static String formatPeriod(
            JsonObject object
    ) {

        String start = value(
                object,
                "period_start"
        );

        String end = value(
                object,
                "period_end"
        );

        if ("-".equals(start)
                || "-".equals(end)) {

            return "-";
        }

        return start + " — " + end;
    }

    // =========================================================
    // DATE TIME FORMAT
    // =========================================================

    private static String formatTime(String text) {

        try {

            return OffsetDateTime.parse(text)
                    .atZoneSameInstant(MYT)
                    .format(
                            DateTimeFormatter.ofPattern(
                                    "dd MMM yyyy HH:mm:ss",
                                    Locale.ENGLISH
                            )
                    );

        } catch (RuntimeException ex) {

            return text;
        }
    }
}
