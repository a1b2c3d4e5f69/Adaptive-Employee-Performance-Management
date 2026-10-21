package com.aepm.ui.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import com.aepm.util.SessionManager;

public final class AIRecommendationDialog {

    private AIRecommendationDialog() {}

    public static void show(
        Window owner,
        String employeeName,
        String employeeCode,
        String department,
        LocalDate start,
        LocalDate end,
        JsonObject recommendation,
        JsonObject snapshot,
        String generatedAt,
        String modelName
    ) {
        Stage dialog = new Stage(StageStyle.TRANSPARENT);
        dialog.setTitle("AI Performance Recommendations");

        if (owner != null) {
            dialog.initOwner(owner);
            dialog.initModality(Modality.WINDOW_MODAL);
        } else {
            dialog.initModality(Modality.APPLICATION_MODAL);
        }

        Label icon = label("AI", 18, "#2563EB", true);
        icon.setAlignment(Pos.CENTER);
        icon.setMinSize(52, 52);
        icon.setStyle(icon.getStyle()
                + "-fx-background-color: #DBEAFE;"
                + "-fx-background-radius: 16;");

        VBox heading = new VBox(
                5,
                label("AI Performance Recommendations",
                        23, "#0F172A", true),
                label("Review insights for the selected evaluation.",
                        13, "#64748B", false)
        );
        HBox.setHgrow(heading, Priority.ALWAYS);

        HBox header = new HBox(16, icon, heading);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(24));

        DateTimeFormatter dateFormat =
                DateTimeFormatter.ofPattern(
                        "dd MMM yyyy", Locale.ENGLISH
                );

        GridPane details = new GridPane();
        details.setHgap(16);
        details.setVgap(16);
        details.setPadding(new Insets(20));
        details.setStyle(
                "-fx-background-color: #EFF6FF;"
                + "-fx-background-radius: 14;"
        );

        for (int i = 0; i < 2; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(50);
            column.setFillWidth(true);
            details.getColumnConstraints().add(column);
        }

        details.add(info("Employee Name", employeeName), 0, 0);
        details.add(info("Employee ID", employeeCode), 1, 0);
        details.add(info("Department", department), 0, 1);
        details.add(info(
                "Evaluation Period",
                start.format(dateFormat) + " – "
                        + end.format(dateFormat)
        ), 1, 1);

        VBox sections = new VBox(16);
        sections.setPadding(new Insets(0, 24, 24, 24));
        sections.getChildren().addAll(
            details,
            aiInputSelectionCard(snapshot),
            performanceEvidence(snapshot, generatedAt, modelName),
            historicalEvidence(snapshot),
            deadlineEvidenceCard(snapshot),
            label(
                        "Based on the indicators selected when this recommendation was generated. "
                        + "Suggestions require manager review.",
                        13, "#64748B", false
                ),
                card(
                        "Overall Assessment",
                        recommendation.get("overall_assessment")
                                .getAsString()
                ),
                card(
                        "Strengths",
                        list(recommendation, "strengths")
                ),
                card(
                        "Areas for Improvement",
                        list(recommendation, "areas_for_improvement")
                )
        );

        VBox actions = new VBox(12);
        actions.getChildren().add(
                label("Recommended Actions", 17, "#0F172A", true)
        );

        int number = 1;

        for (JsonElement element :
                recommendation.getAsJsonArray("recommendations")) {
            JsonObject item = element.getAsJsonObject();

            actions.getChildren().add(card(
                    "Action " + number++,
                    item.get("suggested_action").getAsString()
                            + "\n\nReason\n"
                            + item.get("reason").getAsString()
                            + "\n\nEvidence\n"
                            + item.get("evidence").getAsString()
            ));
        }

        if (number == 1) {
            actions.getChildren().add(label(
                    "No actions suggested from the available evidence.",
                    14, "#475569", false
            ));
        }

        sections.getChildren().addAll(
                actions,
                card(
                        "Suggested Training",
                        list(recommendation, "suggested_training")
                )
        );

        // Manager Coaching is displayed only in the manager view.
        var currentUser = SessionManager.getCurrentUser();

        if (currentUser != null && currentUser.getRoleId() == 2L) {
            sections.getChildren().add(
                    card(
                            "Manager Coaching",
                            list(recommendation, "manager_interventions")
                    )
            );
        }

        ScrollPane scroll = new ScrollPane(sections);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setMinHeight(0);
        scroll.setStyle(
                "-fx-background: white;"
                + "-fx-background-color: transparent;"
                + "-fx-padding: 0;"
        );

        Button close = new Button("Close");
        close.setPrefSize(120, 42);
        close.setDefaultButton(true);
        close.setCancelButton(true);

        String buttonStyle =
                "-fx-background-radius: 10;"
                + "-fx-text-fill: white;"
                + "-fx-font-size: 14px;"
                + "-fx-font-weight: bold;"
                + "-fx-cursor: hand;";

        close.setStyle(buttonStyle
                + "-fx-background-color: #2563EB;");

        close.setOnMouseEntered(event -> close.setStyle(
                buttonStyle + "-fx-background-color: #1D4ED8;"
        ));
        close.setOnMouseExited(event -> close.setStyle(
                buttonStyle + "-fx-background-color: #2563EB;"
        ));
        close.setOnAction(event -> dialog.close());

        HBox footer = new HBox(close);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(16, 24, 20, 24));

        BorderPane panel = new BorderPane();
        panel.setTop(header);
        panel.setCenter(scroll);
        panel.setBottom(footer);
        panel.setStyle(
                "-fx-font-family: 'Segoe UI';"
                + "-fx-background-color: white;"
                + "-fx-background-radius: 18;"
                + "-fx-border-color: #DBE4F0;"
                + "-fx-border-radius: 18;"
        );

        StackPane root = new StackPane(panel);
        root.setPadding(new Insets(8));
        root.setStyle("-fx-background-color: transparent;");

        Screen screen = Screen.getPrimary();

        if (owner != null) {
            var screens = Screen.getScreensForRectangle(
                    owner.getX(), owner.getY(),
                    owner.getWidth(), owner.getHeight()
            );
            if (!screens.isEmpty()) {
                screen = screens.get(0);
            }
        }

        Rectangle2D bounds = screen.getVisualBounds();
        double width = Math.min(880, bounds.getWidth() - 32);
        double height = Math.min(760, bounds.getHeight() - 32);

        Scene scene = new Scene(root, width, height);
        scene.setFill(Color.TRANSPARENT);
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                dialog.close();
            }
        });

        dialog.setScene(scene);
        dialog.setResizable(false);

        double desiredX = owner == null
                ? bounds.getMinX() + (bounds.getWidth() - width) / 2
                : owner.getX() + (owner.getWidth() - width) / 2;

        double desiredY = owner == null
                ? bounds.getMinY() + (bounds.getHeight() - height) / 2
                : owner.getY() + (owner.getHeight() - height) / 2;

        dialog.setX(Math.max(bounds.getMinX(),
                Math.min(desiredX, bounds.getMaxX() - width)));
        dialog.setY(Math.max(bounds.getMinY(),
                Math.min(desiredY, bounds.getMaxY() - height)));

        dialog.showAndWait();
    }

    private static VBox info(String title, String value) {
        return new VBox(
                6,
                label(title, 12, "#64748B", false),
                label(
                        value == null || value.isBlank()
                                ? "Not available" : value,
                        15, "#0F172A", true
                )
        );
    }

    private static VBox card(String title, String text) {
        VBox card = new VBox(
                10,
                label(title, 16, "#0F172A", true),
                label(text, 14, "#475569", false)
        );
        card.setPadding(new Insets(18));
        card.setStyle(
                "-fx-background-color: #F8FAFC;"
                + "-fx-background-radius: 12;"
                + "-fx-border-color: #E2E8F0;"
                + "-fx-border-radius: 12;"
        );
        return card;
    }

    private static Label label(
            String text, int size, String color, boolean bold
    ) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setMinHeight(Region.USE_PREF_SIZE);
        label.setStyle(
                "-fx-font-size: " + size + "px;"
                + "-fx-text-fill: " + color + ";"
                + (bold ? "-fx-font-weight: bold;" : "")
        );
        return label;
    }

    private static String list(JsonObject object, String key) {
        var items = object.getAsJsonArray(key);

        if (items.isEmpty()) {
            return "No suggestions provided from the available evidence.";
        }

        StringBuilder text = new StringBuilder();

        for (JsonElement item : items) {
            if (!text.isEmpty()) {
                text.append("\n\n");
            }
            text.append("• ").append(item.getAsString());
        }

        return text.toString();
    }

    private static VBox performanceEvidence(
            JsonObject snapshot,
            String generatedAt,
            String modelName
    ) {
        JsonObject metrics = evidenceObject(
                snapshot, "additional_metrics"
        );
        JsonObject breakdown = evidenceObject(
                metrics, "kpi_breakdown"
        );

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(18);

        for (int i = 0; i < 2; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(50);
            column.setFillWidth(true);
            grid.getColumnConstraints().add(column);
        }

        grid.add(info(
                "KPI Result ID",
                evidenceText(metrics, "kpi_result_id")
        ), 0, 0);

        grid.add(info(
                "Overall KPI",
                evidencePercentage(snapshot, "kpi_score")
        ), 1, 0);

        grid.add(info(
                "Productivity",
                evidencePercentage(breakdown, "productivity_score")
        ), 0, 1);

        grid.add(info(
                "Timeliness",
                evidencePercentage(breakdown, "timeliness_score")
        ), 1, 1);

        grid.add(info(
                "Completion Rate",
                evidencePercentage(breakdown, "completion_rate")
        ), 0, 2);

        grid.add(info(
                "Attendance",
                evidencePercentage(breakdown, "attendance_score")
        ), 1, 2);

        grid.add(info(
                "Generated / Saved At (MYT)",
                evidenceTime(generatedAt)
        ), 0, 3);

        grid.add(info("AI Model", modelName), 1, 3);

        String explanation = snapshot == null
                ? "This older record has no saved KPI snapshot. "
                + "Unavailable values are not replaced with current scores."
                : "These values were saved with this recommendation. "
                + "Recalculating KPI does not change this evidence. "
                + "Attendance measures recorded attendance coverage; "
                + "it does not establish zero late arrivals or early departures.";

        VBox box = new VBox(
                16,
                label("Performance Evidence", 17, "#0F172A", true),
                grid,
                label(explanation, 12, "#64748B", false)
        );

        box.setPadding(new Insets(20));
        box.setStyle(
                "-fx-background-color: #F8FAFC;"
                + "-fx-background-radius: 14;"
                + "-fx-border-color: #DBE4F0;"
                + "-fx-border-radius: 14;"
        );

        return box;
    }

    private static JsonObject evidenceObject(
            JsonObject parent,
            String key
    ) {
        if (parent == null) {
            return null;
        }

        JsonElement value = parent.get(key);

        return value != null && value.isJsonObject()
                ? value.getAsJsonObject() : null;
    }

    private static String evidenceText(
            JsonObject parent,
            String key
    ) {
        if (parent == null) {
            return "Not available";
        }

        JsonElement value = parent.get(key);

        if (value == null
                || value.isJsonNull()
                || !value.isJsonPrimitive()) {
            return "Not available";
        }

        return value.getAsString();
    }

    private static String evidencePercentage(
            JsonObject parent,
            String key
    ) {
        if (parent == null) {
            return "Not available";
        }

        JsonElement value = parent.get(key);

        if (value == null
                || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isNumber()) {
            return "Not available";
        }

        double number = value.getAsDouble();

        if (!Double.isFinite(number)
                || number < 0
                || number > 100) {
            return "Invalid saved value";
        }

        return String.format(Locale.ENGLISH, "%.2f%%", number);
    }

    private static String evidenceTime(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            return "Not available";
        }

        try {
            return java.time.OffsetDateTime.parse(timestamp)
                    .atZoneSameInstant(
                            java.time.ZoneId.of("Asia/Kuala_Lumpur")
                    )
                    .format(DateTimeFormatter.ofPattern(
                            "dd MMM yyyy HH:mm:ss",
                            Locale.ENGLISH
                    ));
        } catch (java.time.format.DateTimeParseException ex) {
            return "Not available";
        }
    }

    private static VBox historicalEvidence(JsonObject snapshot) {

        JsonObject metrics = evidenceObject(
                snapshot, "additional_metrics"
        );

        JsonObject history = evidenceObject(
                metrics, "historical_performance"
        );

        if (history == null || !history.has("enabled")) {
            return card(
                    "Historical Performance Evidence",
                    "This older recommendation did not record "
                    + "the historical performance setting."
            );
        }

        if (!history.get("enabled").getAsBoolean()) {
            return card(
                    "Historical Performance Evidence",
                    "Disabled when generated. "
                    + "No historical KPI records were supplied to AI."
            );
        }

        var records = history.getAsJsonArray("records");

        if (records == null || records.isEmpty()) {
            return card(
                    "Historical Performance Evidence",
                    "Enabled when generated, but no eligible earlier "
                    + "KPI periods were available."
            );
        }

        StringBuilder text = new StringBuilder(
                "Saved historical KPI records retained for reference: "
                        + records.size()
                        + "\nLatest periods first. "
                        + "Period lengths and calculation rules may differ."
        );

        for (JsonElement element : records) {

            JsonObject row = element.getAsJsonObject();

            text.append("\n\n")
                    .append(evidenceText(row, "period_start"))
                    .append(" — ")
                    .append(evidenceText(row, "period_end"))
                    .append("\nKPI Result: ")
                    .append(evidenceText(row, "result_id"))
                    .append("\nOverall KPI: ")
                    .append(evidencePercentage(row, "overall_kpi_score"))
                    .append("\nProductivity: ")
                    .append(evidencePercentage(row, "productivity_score"))
                    .append(" | Timeliness: ")
                    .append(evidencePercentage(row, "timeliness_score"))
                    .append("\nCompletion: ")
                    .append(evidencePercentage(row, "completion_rate"))
                    .append(" | Attendance: ")
                    .append(evidencePercentage(row, "attendance_score"));
        }

        return card(
                "Historical Performance Evidence",
                text.toString()
        );
    }

    private static VBox deadlineEvidenceCard(JsonObject snapshot) {

        JsonObject metrics = evidenceObject(
                snapshot, "additional_metrics"
        );

        JsonObject evidence = evidenceObject(
                metrics, "deadline_evidence"
        );

        if (evidence == null || !evidence.has("enabled")) {
            return card(
                    "Missed Deadlines Evidence",
                    "This older recommendation has no saved "
                    + "task-level deadline evidence."
            );
        }

        if (!evidence.get("enabled").getAsBoolean()) {
            return card(
                    "Missed Deadlines Evidence",
                    "Disabled when generated. No task-level deadline "
                    + "records were supplied to AI."
            );
        }

        StringBuilder text = new StringBuilder();

        text.append("Tasks checked: ")
                .append(evidenceText(evidence, "task_count"))
                .append("\nCompleted late: ")
                .append(evidenceText(evidence, "completed_late_count"))
                .append("\nDeadline passed; no completion recorded: ")
                .append(evidenceText(
                        evidence, "deadline_passed_no_completion_count"))
                .append("\nCompleted on time: ")
                .append(evidenceText(evidence, "completed_on_time_count"))
                .append("\nNot due by evaluation end: ")
                .append(evidenceText(evidence, "not_due_count"))
                .append("\nNo deadline: ")
                .append(evidenceText(evidence, "no_deadline_count"));

        var records = evidence.getAsJsonArray("records");

        if (records != null) {
            for (JsonElement element : records) {
                JsonObject row = element.getAsJsonObject();

                JsonElement titleValue = row.get("task_title");

                String taskName = titleValue != null
                        && !titleValue.isJsonNull()
                        && !titleValue.getAsString().isBlank()
                        ? titleValue.getAsString()
                        : "Name not saved (older record)";

                text.append("\n\nTask Name: ")
                        .append(taskName)
                        .append("\n")
                        .append(evidenceText(row, "classification"))
                        .append("\nDue (MYT): ")
                        .append(evidenceText(row, "due_at_myt"))
                        .append("\nCompleted (MYT): ")
                        .append(evidenceText(row, "completed_at_myt"));
            }
        }

        text.append("\n\n")
                .append(evidenceText(evidence, "limitations"));

        return card("Missed Deadlines Evidence", text.toString());
    }

    private static VBox aiInputSelectionCard(JsonObject snapshot) {

        JsonObject metrics = evidenceObject(
                snapshot, "additional_metrics"
        );
        JsonObject input = evidenceObject(metrics, "ai_input");

        if (input == null) {
            return card(
                    "AI Input Selection",
                    "This older recommendation does not contain "
                    + "a separate record of the structured AI input."
            );
        }

        boolean attendance = input.has("attendance_enabled")
                && input.get("attendance_enabled").getAsBoolean();

        boolean completion = input.has("task_completion_rate_enabled")
                && input.get("task_completion_rate_enabled").getAsBoolean();

        return card(
                "AI Input Selection",
                "Attendance score: "
                        + (attendance ? "Included" : "Excluded")
                        + "\nTask completion rate: "
                        + (completion ? "Included" : "Excluded")
                        + "\nThese selections apply to current and historical "
                        + "component scores."
                        + "\n\nThe KPI evidence cards below retain the full "
                        + "saved records for reference. Excluded component "
                        + "scores were not supplied to AI."
                        + "\nOverall KPI retains its original calculation."
        );
    }
}