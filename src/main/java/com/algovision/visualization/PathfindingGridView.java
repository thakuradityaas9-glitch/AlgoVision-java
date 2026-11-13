package com.algovision.visualization;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.algovision.pathfinding.model.AStarStep;
import com.algovision.pathfinding.model.GridPosition;
import com.algovision.pathfinding.model.PathfindingGrid;

import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;

public final class PathfindingGridView extends GridPane {
    private final PathfindingGrid grid;
    private final List<List<Region>> cells = new ArrayList<>();
    private Consumer<GridPosition> cellActionHandler;
    private AStarStep currentStep;

    public PathfindingGridView(PathfindingGrid grid) {
        this.grid = grid;
        getStyleClass().add("pathfinding-grid");
        setMinSize(0, 0);
        setPrefSize(760, 450);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        setHgap(1);
        setVgap(1);

        double columnPercent = 100.0 / grid.columns();
        for (int column = 0; column < grid.columns(); column++) {
            ColumnConstraints constraints = new ColumnConstraints();
            constraints.setPercentWidth(columnPercent);
            constraints.setHgrow(Priority.ALWAYS);
            constraints.setHalignment(HPos.CENTER);
            getColumnConstraints().add(constraints);
        }
        double rowPercent = 100.0 / grid.rows();
        for (int row = 0; row < grid.rows(); row++) {
            RowConstraints constraints = new RowConstraints();
            constraints.setPercentHeight(rowPercent);
            constraints.setVgrow(Priority.ALWAYS);
            constraints.setValignment(VPos.CENTER);
            getRowConstraints().add(constraints);
        }

        for (int row = 0; row < grid.rows(); row++) {
            List<Region> cellRow = new ArrayList<>(grid.columns());
            for (int column = 0; column < grid.columns(); column++) {
                GridPosition position = new GridPosition(row, column);
                Region cell = createCell(position);
                cellRow.add(cell);
                add(cell, column, row);
            }
            cells.add(cellRow);
        }
        render();
    }

    public void setCellActionHandler(Consumer<GridPosition> handler) {
        cellActionHandler = handler;
    }

    public void render(AStarStep step) {
        currentStep = step;
        render();
    }

    private Region createCell(GridPosition position) {
        Region cell = new Region();
        cell.getStyleClass().addAll("pathfinding-cell", "path-cell-empty");
        cell.setMinSize(0, 0);
        cell.setPrefSize(24, 20);
        cell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        cell.setOnMousePressed(event -> {
            if (event.isPrimaryButtonDown() && cellActionHandler != null) {
                cellActionHandler.accept(position);
            }
        });
        cell.setOnMouseEntered(event -> {
            if (event.isPrimaryButtonDown() && cellActionHandler != null) {
                cellActionHandler.accept(position);
            }
        });
        return cell;
    }

    private void render() {
        for (int row = 0; row < grid.rows(); row++) {
            for (int column = 0; column < grid.columns(); column++) {
                GridPosition position = new GridPosition(row, column);
                Region cell = cells.get(row).get(column);
                cell.getStyleClass().setAll("pathfinding-cell", stateClass(position));
            }
        }
    }

    private String stateClass(GridPosition position) {
        if (position.equals(grid.start())) {
            return "path-cell-start";
        }
        if (position.equals(grid.end())) {
            return "path-cell-end";
        }
        if (grid.isWall(position)) {
            return "path-cell-wall";
        }
        if (currentStep == null) {
            return "path-cell-empty";
        }
        if (currentStep.path().contains(position)) {
            return "path-cell-final";
        }
        if (position.equals(currentStep.currentCell())) {
            return "path-cell-current";
        }
        if (position.equals(currentStep.evaluatedNeighbor())) {
            return "path-cell-neighbor";
        }
        if (currentStep.exploredCells().contains(position)) {
            return "path-cell-explored";
        }
        if (currentStep.openCells().contains(position)) {
            return "path-cell-open";
        }
        return "path-cell-empty";
    }
}
