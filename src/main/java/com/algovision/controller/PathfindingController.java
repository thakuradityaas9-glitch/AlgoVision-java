package com.algovision.controller;

import java.util.List;
import java.util.Random;

import com.algovision.pathfinding.algorithms.AStarSearch;
import com.algovision.pathfinding.model.AStarEvent;
import com.algovision.pathfinding.model.AStarStep;
import com.algovision.pathfinding.model.GridPosition;
import com.algovision.pathfinding.model.PathfindingGrid;
import com.algovision.pathfinding.model.PathfindingTool;
import com.algovision.visualization.PathfindingGridView;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public final class PathfindingController {
    private static final int GRID_ROWS = 17;
    private static final int GRID_COLUMNS = 25;
    private static final double RANDOM_WALL_PROBABILITY = 0.24;

    private final PathfindingGrid grid = new PathfindingGrid(
            GRID_ROWS,
            GRID_COLUMNS,
            new GridPosition(GRID_ROWS / 2, 2),
            new GridPosition(GRID_ROWS / 2, GRID_COLUMNS - 3));
    private final AStarSearch algorithm = new AStarSearch();
    private final PathfindingGridView gridView = new PathfindingGridView(grid);
    private final ComboBox<PathfindingTool> toolChoice = new ComboBox<>();
    private final Slider speedSlider = new Slider(0, 100, 60);
    private final Label visitedValue = new Label("0");
    private final Label pathLengthValue = new Label("--");
    private final Label exploredValue = new Label("0");
    private final Label statusValue = new Label("Ready");
    private final Label stepDetailsValue = new Label("Choose a tool, then start A* to explore the grid.");
    private final Label messageValue = new Label(
            "Select a tool to place endpoints, draw walls, or erase walls. Wall drawing supports click and drag.");
    private final Button startButton = new Button("Start");
    private final Button pauseButton = new Button("Pause");
    private final Button resumeButton = new Button("Resume");
    private final Button resetButton = new Button("Reset");
    private final Button clearGridButton = new Button("Clear Grid");
    private final Button randomObstaclesButton = new Button("Random Obstacles");

    private final ScrollPane view;
    private PathfindingTool selectedTool = PathfindingTool.WALL;
    private List<AStarStep> steps = List.of();
    private int stepIndex;
    private AStarStep currentStep;
    private Timeline timeline;

    public PathfindingController() {
        gridView.setCellActionHandler(this::editCell);
        view = createView();
        renderGrid();
        updateStatistics();
        updateButtons();
    }

    public Node getView() {
        return view;
    }

    public void pause() {
        if (timeline != null && "Running".equals(statusValue.getText())) {
            timeline.pause();
            setStatus("Paused");
            setMessage("Search paused. Resume to continue or reset to start again.");
            updateButtons();
        }
    }

    private ScrollPane createView() {
        VBox content = new VBox(16);
        content.getStyleClass().add("pathfinding-content");
        content.setPadding(new Insets(28, 34, 24, 34));

        VBox introduction = new VBox(7);
        Label eyebrow = new Label("PATHFINDING ALGORITHMS");
        eyebrow.getStyleClass().add("eyebrow");
        Label heading = new Label("A* Pathfinding Visualizer");
        heading.getStyleClass().add("welcome-heading");
        Label description = new Label(
                "Watch A* explore a grid and find a shortest four-directional route around obstacles.");
        description.getStyleClass().add("welcome-subtitle");
        description.setWrapText(true);
        introduction.getChildren().addAll(eyebrow, heading, description);

        HBox statistics = new HBox(12,
                createStatCard("VISITED CELLS", visitedValue),
                createStatCard("PATH LENGTH", pathLengthValue),
                createStatCard("EXPLORED NODES", exploredValue),
                createStatCard("STATUS", statusValue));
        statistics.setAlignment(Pos.CENTER);

        VBox gridPanel = new VBox(10);
        gridPanel.getStyleClass().add("pathfinding-grid-panel");
        gridPanel.setPadding(new Insets(14));
        HBox gridHeading = new HBox(10);
        Label gridTitle = new Label("Grid workspace");
        gridTitle.getStyleClass().add("panel-heading");
        Region headingSpacer = createSpacer();
        gridHeading.getChildren().addAll(gridTitle, headingSpacer);
        gridPanel.getChildren().addAll(createLegend(), gridHeading, gridView);
        VBox.setVgrow(gridView, Priority.ALWAYS);

        VBox informationPanel = createInformationPanel();
        stepDetailsValue.getStyleClass().add("pathfinding-step-details");
        stepDetailsValue.setWrapText(true);
        messageValue.getStyleClass().add("graph-message");
        messageValue.setWrapText(true);
        content.getChildren().addAll(
                introduction,
                createControls(),
                statistics,
                informationPanel,
                gridPanel,
                stepDetailsValue,
                messageValue);
        VBox.setVgrow(gridPanel, Priority.ALWAYS);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("content-scroll");
        return scrollPane;
    }

    private VBox createControls() {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("controls-card");
        panel.setPadding(new Insets(14));

        toolChoice.getItems().setAll(PathfindingTool.values());
        toolChoice.getSelectionModel().select(PathfindingTool.WALL);
        toolChoice.setMaxWidth(Double.MAX_VALUE);
        toolChoice.setOnAction(event -> selectedTool = toolChoice.getValue());

        clearGridButton.getStyleClass().add("secondary-button");
        randomObstaclesButton.getStyleClass().add("secondary-button");
        clearGridButton.setOnAction(event -> clearGrid());
        randomObstaclesButton.setOnAction(event -> randomizeObstacles());
        HBox toolRow = new HBox(10,
                labeledControl("GRID TOOL", toolChoice),
                createSpacer(),
                clearGridButton,
                randomObstaclesButton);
        toolRow.setAlignment(Pos.BOTTOM_LEFT);

        startButton.getStyleClass().add("primary-button");
        pauseButton.getStyleClass().add("secondary-button");
        resumeButton.getStyleClass().add("secondary-button");
        resetButton.getStyleClass().add("secondary-button");
        startButton.setOnAction(event -> startSearch());
        pauseButton.setOnAction(event -> pause());
        resumeButton.setOnAction(event -> resume());
        resetButton.setOnAction(event -> resetSearch());

        Label speedLabel = new Label("SPEED");
        speedLabel.getStyleClass().add("control-label");
        speedSlider.setShowTickLabels(false);
        speedSlider.setShowTickMarks(false);
        speedSlider.setBlockIncrement(10);
        speedSlider.setPrefWidth(150);
        speedSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (timeline != null && "Running".equals(statusValue.getText())) {
                timeline.setRate(playbackRate(newValue.doubleValue()));
            }
        });
        HBox playbackRow = new HBox(9,
                startButton, pauseButton, resumeButton, resetButton,
                createSpacer(), speedLabel, speedSlider);
        playbackRow.setAlignment(Pos.CENTER_LEFT);
        panel.getChildren().addAll(toolRow, playbackRow);
        return panel;
    }

    private VBox createInformationPanel() {
        VBox panel = new VBox(6);
        panel.getStyleClass().add("pathfinding-info-panel");
        Label heading = new Label("How A* chooses a route");
        heading.getStyleClass().add("panel-heading");
        Label explanation = new Label(
                "A* prioritizes the lowest f(n) = g(n) + h(n): g is the path cost so far, "
                        + "and h is Manhattan distance to the destination. Manhattan distance "
                        + "fits four-directional, unit-cost movement. With a consistent heuristic, "
                        + "this grid search runs in O(V log V) time and O(V) space for V walkable cells.");
        explanation.getStyleClass().add("pathfinding-info-text");
        explanation.setWrapText(true);
        panel.getChildren().addAll(heading, explanation);
        return panel;
    }

    private HBox createLegend() {
        HBox legend = new HBox(12,
                createLegendItem("Start", "path-cell-start"),
                createLegendItem("Destination", "path-cell-end"),
                createLegendItem("Wall", "path-cell-wall"),
                createLegendItem("Frontier", "path-cell-open"),
                createLegendItem("Exploring", "path-cell-current"),
                createLegendItem("Explored", "path-cell-explored"),
                createLegendItem("Path", "path-cell-final"));
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.setSpacing(8);
        return legend;
    }

    private HBox createLegendItem(String text, String stateClass) {
        Region swatch = new Region();
        swatch.getStyleClass().addAll("pathfinding-legend-swatch", stateClass);
        Label label = new Label(text);
        label.getStyleClass().add("pathfinding-legend-label");
        HBox item = new HBox(5, swatch, label);
        item.setAlignment(Pos.CENTER_LEFT);
        return item;
    }

    private VBox createStatCard(String title, Label value) {
        VBox card = new VBox(7, new Label(title), value);
        card.getStyleClass().add("stat-card");
        card.getChildren().get(0).getStyleClass().add("stat-title");
        value.getStyleClass().add("stat-value");
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private VBox labeledControl(String labelText, Node control) {
        VBox field = new VBox(6);
        Label label = new Label(labelText);
        label.getStyleClass().add("control-label");
        field.getChildren().addAll(label, control);
        HBox.setHgrow(field, Priority.ALWAYS);
        return field;
    }

    private Region createSpacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private void editCell(GridPosition position) {
        if ("Running".equals(statusValue.getText()) || "Paused".equals(statusValue.getText())
                || currentStep != null) {
            resetSearch();
        }

        switch (selectedTool) {
            case SET_START -> {
                grid.setStart(position);
                setMessage("Start moved to " + formatPosition(position) + ".");
            }
            case SET_END -> {
                grid.setEnd(position);
                setMessage("Destination moved to " + formatPosition(position) + ".");
            }
            case WALL -> {
                if (grid.setWall(position, true)) {
                    setMessage("Wall added. Drag across cells to draw a continuous obstacle.");
                }
            }
            case ERASE -> {
                if (grid.setWall(position, false)) {
                    setMessage("Wall erased.");
                }
            }
        }
        renderGrid();
        updateStatistics();
        updateButtons();
    }

    private void startSearch() {
        if ("Running".equals(statusValue.getText()) || "Paused".equals(statusValue.getText())) {
            return;
        }
        try {
            steps = algorithm.generateSteps(grid);
        } catch (IllegalArgumentException exception) {
            setMessage(exception.getMessage());
            return;
        }

        stepIndex = 0;
        currentStep = null;
        timeline = new Timeline(new KeyFrame(Duration.millis(65), event -> advance()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.setRate(playbackRate(speedSlider.getValue()));
        setStatus("Running");
        setMessage("A* search started.");
        timeline.play();
        updateButtons();
    }

    private void resume() {
        if (timeline != null && "Paused".equals(statusValue.getText())) {
            timeline.setRate(playbackRate(speedSlider.getValue()));
            timeline.play();
            setStatus("Running");
            setMessage("A* search resumed.");
            updateButtons();
        }
    }

    private void advance() {
        if (stepIndex >= steps.size()) {
            finishSearch();
            return;
        }
        currentStep = steps.get(stepIndex++);
        gridView.render(currentStep);
        updateStatistics();
        stepDetailsValue.setText(detailsFor(currentStep));
        setMessage(messageFor(currentStep));
        if (currentStep.event() == AStarEvent.COMPLETE) {
            finishSearch();
        }
    }

    private void finishSearch() {
        stopTimeline();
        setStatus("Completed");
        updateStatistics();
        updateButtons();
        if (currentStep != null && currentStep.path().isEmpty()) {
            setMessage("No path exists. Try erasing walls or randomizing the obstacles.");
        } else if (currentStep != null) {
            setMessage("Shortest path found with " + Math.max(0, currentStep.path().size() - 1)
                    + " moves.");
        }
    }

    private void resetSearch() {
        stopTimeline();
        steps = List.of();
        stepIndex = 0;
        currentStep = null;
        gridView.render(null);
        stepDetailsValue.setText("Choose a tool, then start A* to explore the grid.");
        setStatus("Ready");
        setMessage("Search reset. Grid walls and endpoints are unchanged.");
        updateStatistics();
        updateButtons();
    }

    private void clearGrid() {
        resetSearch();
        grid.clearWalls();
        renderGrid();
        setMessage("Grid cleared. Start and destination are preserved.");
        updateButtons();
    }

    private void randomizeObstacles() {
        resetSearch();
        grid.randomizeWalls(RANDOM_WALL_PROBABILITY, new Random());
        renderGrid();
        setMessage("Random obstacle layout generated. A* will report if no route is available.");
        updateButtons();
    }

    private void stopTimeline() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
    }

    private void renderGrid() {
        gridView.render(currentStep);
    }

    private void updateStatistics() {
        if (currentStep == null) {
            visitedValue.setText("0");
            pathLengthValue.setText("--");
            exploredValue.setText("0");
            return;
        }
        int visitedCells = currentStep.openCells().size() + currentStep.exploredCells().size();
        visitedValue.setText(Integer.toString(visitedCells));
        exploredValue.setText(Integer.toString(currentStep.exploredCells().size()));
        if (!currentStep.path().isEmpty()) {
            pathLengthValue.setText(Integer.toString(currentStep.path().size() - 1));
        } else if (currentStep.event() == AStarEvent.NO_PATH
                || currentStep.event() == AStarEvent.COMPLETE) {
            pathLengthValue.setText("No path");
        } else {
            pathLengthValue.setText("--");
        }
    }

    private void updateButtons() {
        String status = statusValue.getText();
        boolean active = "Running".equals(status) || "Paused".equals(status);
        boolean completed = "Completed".equals(status);
        startButton.setDisable(active || completed);
        pauseButton.setDisable(!"Running".equals(status));
        resumeButton.setDisable(!"Paused".equals(status));
        resetButton.setDisable(!active && steps.isEmpty());
        clearGridButton.setDisable(grid.walls().isEmpty() && !active && steps.isEmpty());
    }

    private String messageFor(AStarStep step) {
        return switch (step.event()) {
            case INITIALIZED -> "Start is in the priority queue with g = 0 and h = "
                    + step.hScores().get(grid.start()) + ".";
            case PRIORITY_QUEUE_UPDATE -> "Priority queue selected "
                    + (step.currentCell() == null ? "a cell" : formatPosition(step.currentCell())) + ".";
            case CURRENT_CELL -> "Exploring " + formatPosition(step.currentCell())
                    + " with g = " + step.gScores().get(step.currentCell())
                    + ", h = " + step.hScores().get(step.currentCell())
                    + ", f = " + step.fScores().get(step.currentCell()) + ".";
            case NEIGHBOR_EVALUATION -> "Evaluating " + formatPosition(step.evaluatedNeighbor())
                    + ": g = " + step.candidateGScore()
                    + ", h = " + step.candidateHScore()
                    + ", f = " + step.candidateFScore() + ".";
            case QUEUE_ADD -> step.evaluatedNeighbor() == null
                    ? "Added the start cell to the priority queue."
                    : "Added " + formatPosition(step.evaluatedNeighbor())
                            + " to the priority queue with f = " + step.candidateFScore() + ".";
            case PATH_FOUND -> "Destination reached. Highlighting the shortest path.";
            case NO_PATH -> "The priority queue is empty; no route reaches the destination.";
            case COMPLETE -> "A* search complete.";
        };
    }

    private String detailsFor(AStarStep step) {
        String current = formatPosition(step.currentCell());
        String neighbor = formatPosition(step.evaluatedNeighbor());
        return switch (step.event()) {
            case INITIALIZED -> "Start cell: " + formatPosition(grid.start())
                    + " | Destination: " + formatPosition(grid.end())
                    + " | Open cells: " + step.openCells().size();
            case PRIORITY_QUEUE_UPDATE -> "Priority queue entries: "
                    + step.priorityQueueState().size()
                    + " | Expanded cells: " + step.exploredCells().size();
            case CURRENT_CELL -> "Current cell " + current
                    + " | g = " + step.gScores().get(step.currentCell())
                    + " | h = " + step.hScores().get(step.currentCell())
                    + " | f = " + step.fScores().get(step.currentCell());
            case NEIGHBOR_EVALUATION -> "From " + current + " evaluating " + neighbor
                    + " | candidate g = " + step.candidateGScore()
                    + " | h = " + step.candidateHScore()
                    + " | f = " + step.candidateFScore();
            case QUEUE_ADD -> "Priority queue entries: "
                    + step.priorityQueueState().size()
                    + " | Open cells: " + step.openCells().size();
            case PATH_FOUND -> "Route contains " + step.path().size()
                    + " cells and " + Math.max(0, step.path().size() - 1) + " moves.";
            case NO_PATH -> "All reachable cells were explored; the destination is unreachable.";
            case COMPLETE -> step.path().isEmpty()
                    ? "Search complete without a route."
                    : "Search complete. Shortest path length: "
                            + Math.max(0, step.path().size() - 1) + " moves.";
        };
    }

    private String formatPosition(GridPosition position) {
        if (position == null) {
            return "unknown cell";
        }
        return "(" + (position.row() + 1) + ", " + (position.column() + 1) + ")";
    }

    private double playbackRate(double value) {
        return 0.35 + value / 100.0 * 4.65;
    }

    private void setStatus(String status) {
        statusValue.setText(status);
    }

    private void setMessage(String message) {
        messageValue.setText(message);
    }
}
