package com.aepm.ui.components;

import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

public final class DashboardTaskChart {

    private DashboardTaskChart() {}

    public static void update(
            BarChart<String, Number> chart,
            int completed,
            int inProgress,
            int overdue,
            int pending,
            int other
    ) {
        String[] names = {
            "Completed",
            "In Progress",
            "Overdue",
            "Pending",
            "Other"
        };

        int[] counts = {
            completed,
            inProgress,
            overdue,
            pending,
            other
        };

        String[] colors = {
            "#2563EB",
            "#F59E0B",
            "#FB7185",
            "#8B5CF6",
            "#64748B"
        };

        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setMinHeight(300);
        chart.setPrefHeight(330);

        chart.getXAxis().setTickLabelsVisible(true);
        chart.getXAxis().setTickLabelFill(Color.web("#334155"));
        chart.getXAxis().setTickLabelRotation(0);
        chart.getXAxis().setLabel(null);

        chart.getYAxis().setTickLabelsVisible(true);
        chart.getYAxis().setTickLabelFill(Color.web("#334155"));
        chart.getYAxis().setLabel(null);

        int largest = 0;

        for (int count : counts) {
            largest = Math.max(largest, count);
        }

        if (chart.getYAxis() instanceof NumberAxis axis) {
            double step = Math.max(
                1,
                Math.ceil(largest / 5.0)
            );

            axis.setAutoRanging(false);
            axis.setLowerBound(0);

            // Leave space above the tallest bar for its number.
            axis.setUpperBound(
                Math.max(
                    5,
                    (Math.ceil(largest / step) + 1) * step
                )
            );

            axis.setTickUnit(step);
            axis.setMinorTickVisible(false);
        }

        XYChart.Series<String, Number> series =
            new XYChart.Series<>();

        for (int i = 0; i < names.length; i++) {
            XYChart.Data<String, Number> item =
                new XYChart.Data<>(names[i], counts[i]);

            StackPane bar = new StackPane();
            bar.setAlignment(Pos.TOP_CENTER);
            bar.setStyle(
                "-fx-bar-fill: " + colors[i] + ";"
            );
            bar.setAccessibleText(
                names[i] + ": " + counts[i]
            );

            Text value = new Text(
                Integer.toString(counts[i])
            );

            value.setManaged(false);
            value.setMouseTransparent(true);

            value.setStyle(
                "-fx-fill: #11254C;"
                + "-fx-font-size: 14px;"
                + "-fx-font-weight: bold;"
            );

            Runnable position = () -> {
                value.setLayoutX(
                    (bar.getWidth()
                        - value.getLayoutBounds().getWidth()) / 2
                );

                value.setLayoutY(-8);
            };

            bar.widthProperty().addListener(
                (observable, oldValue, newValue) ->
                    position.run()
            );

            value.layoutBoundsProperty().addListener(
                (observable, oldValue, newValue) ->
                    position.run()
            );

            bar.getChildren().add(value);
            item.setNode(bar);
            series.getData().add(item);

            position.run();
        }

        chart.getData().clear();
        chart.getData().add(series);
    }
}