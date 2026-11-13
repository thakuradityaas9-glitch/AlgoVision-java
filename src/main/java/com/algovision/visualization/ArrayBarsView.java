package com.algovision.visualization;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class ArrayBarsView extends VBox {
    private static final double MIN_BAR_HEIGHT = 42;
    private static final double MAX_BAR_HEIGHT = 238;
    private static final double MIN_BAR_WIDTH = 18;

    public ArrayBarsView() {
        getStyleClass().add("visualization-card");
        setPadding(new Insets(18));
        setMinHeight(350);
        VBox.setVgrow(this, Priority.ALWAYS);
    }

    public void render(int[] values, int comparedLeft, int comparedRight,
            int pivotIndex, boolean[] sortedElements) {
        getChildren().clear();
        if (values.length == 0) {
            return;
        }

        int minimum = values[0];
        int maximum = values[0];
        for (int value : values) {
            minimum = Math.min(minimum, value);
            maximum = Math.max(maximum, value);
        }

        double width = Math.max(640, getWidth() - 40);
        double barWidth = Math.max(MIN_BAR_WIDTH, Math.min(42, (width - values.length * 8) / values.length));
        HBox bars = new HBox(8);
        bars.setAlignment(Pos.BOTTOM_CENTER);
        bars.setMinHeight(290);
        bars.setPadding(new Insets(14, 8, 8, 8));

        List<VBox> columns = new ArrayList<>();
        for (int index = 0; index < values.length; index++) {
            int value = values[index];
            VBox column = new VBox(7);
            column.setAlignment(Pos.BOTTOM_CENTER);
            column.setMinWidth(barWidth);
            column.setPrefWidth(barWidth);
            column.setMaxWidth(barWidth);

            Label valueLabel = new Label(Integer.toString(value));
            valueLabel.getStyleClass().add("bar-value");
            valueLabel.setMinWidth(Region.USE_PREF_SIZE);

            Region bar = new Region();
            bar.getStyleClass().add("array-bar");
            bar.setMinWidth(barWidth);
            bar.setPrefWidth(barWidth);
            bar.setMaxWidth(barWidth);
            bar.setPrefHeight(heightFor(value, minimum, maximum));

            if (sortedElements[index]) {
                bar.getStyleClass().add("sorted");
            }
            if (index == comparedLeft || index == comparedRight) {
                bar.getStyleClass().add("compared");
            }
            if (index == pivotIndex) {
                bar.getStyleClass().add("pivot");
            }

            column.getChildren().addAll(valueLabel, bar);
            columns.add(column);
        }
        bars.getChildren().setAll(columns);

        ScrollPane scrollPane = new ScrollPane(bars);
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("visualization-scroll");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        getChildren().add(scrollPane);
    }

    private double heightFor(int value, int minimum, int maximum) {
        if (minimum == maximum) {
            return (MIN_BAR_HEIGHT + MAX_BAR_HEIGHT) / 2;
        }
        double ratio = (double) (value - minimum) / (maximum - minimum);
        return MIN_BAR_HEIGHT + ratio * (MAX_BAR_HEIGHT - MIN_BAR_HEIGHT);
    }
}
