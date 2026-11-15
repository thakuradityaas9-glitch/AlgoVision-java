package com.algovision.controller;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import com.algovision.pathfinding.algorithms.AStarSearch;
import com.algovision.pathfinding.model.AStarEvent;
import com.algovision.pathfinding.model.AStarStep;
import com.algovision.pathfinding.model.GridPosition;
import com.algovision.pathfinding.model.PathfindingGrid;
import com.algovision.pathfinding.model.PathfindingTool;
import com.algovision.ui.AlgoVisionTheme;
import com.algovision.visualization.PathfindingGridView;

public final class PathfindingController extends JPanel {
    private static final Color APP_BACKGROUND = AlgoVisionTheme.APP_BACKGROUND;
    private static final Color PANEL_BACKGROUND = AlgoVisionTheme.PANEL_BACKGROUND;
    private static final Color BORDER_COLOR = AlgoVisionTheme.BORDER;
    private static final Color PRIMARY_TEXT = AlgoVisionTheme.TEXT_PRIMARY;
    private static final Color BODY_TEXT = AlgoVisionTheme.TEXT_BODY;
    private static final Color ACCENT_TEXT = AlgoVisionTheme.ACCENT;
    private static final int GRID_ROWS = 17;
    private static final int GRID_COLUMNS = 25;
    private static final double RANDOM_WALL_PROBABILITY = 0.24;
    private static final int BASE_INTERVAL_MILLIS = 65;

    private final PathfindingGrid grid = new PathfindingGrid(
            GRID_ROWS,
            GRID_COLUMNS,
            new GridPosition(GRID_ROWS / 2, 2),
            new GridPosition(GRID_ROWS / 2, GRID_COLUMNS - 3));
    private final AStarSearch algorithm = new AStarSearch();
    private final PathfindingGridView gridView = new PathfindingGridView(grid);
    private final JComboBox<PathfindingTool> toolChoice = new JComboBox<>();
    private final JSlider speedSlider = new JSlider(0, 100, 60);
    private final JLabel visitedValue = new JLabel("0");
    private final JLabel pathLengthValue = new JLabel("--");
    private final JLabel exploredValue = new JLabel("0");
    private final JLabel statusValue = new JLabel("Ready");
    private final JTextArea stepDetailsValue = createTextArea(
            "Choose a tool, then start A* to explore the grid.", 1);
    private final JTextArea messageValue = createTextArea(
            "Select a tool to place endpoints, draw walls, or erase walls. "
                    + "Wall drawing supports click and drag.", 2);
    private final JButton startButton = new JButton("Start");
    private final JButton pauseButton = new JButton("Pause");
    private final JButton resumeButton = new JButton("Resume");
    private final JButton resetButton = new JButton("Reset");
    private final JButton clearGridButton = new JButton("Clear Grid");
    private final JButton randomObstaclesButton = new JButton("Random Obstacles");

    private PathfindingTool selectedTool = PathfindingTool.WALL;
    private List<AStarStep> steps = List.of();
    private int stepIndex;
    private AStarStep currentStep;
    private AStarStep pathAnimationStep;
    private List<GridPosition> pathToAnimate = List.of();
    private int pathAnimationIndex;
    private SwingWorker<List<AStarStep>, Void> stepWorker;
    private Timer animationTimer;
    private int generationId;
    private int timerDelayMillis = intervalFor(speedSlider.getValue());

    public PathfindingController() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException(
                    "Create the pathfinding workspace on the Swing event dispatch thread.");
        }
        setLayout(new BorderLayout());
        setBackground(APP_BACKGROUND);
        setBorder(new EmptyBorder(18, 22, 16, 22));
        gridView.setCellActionHandler(this::editCell);
        configureControls();
        add(createView(), BorderLayout.CENTER);
        renderGrid();
        updateStatistics();
        updateButtons();
    }

    public JPanel getView() {
        return this;
    }

    public void pause() {
        if (isAnimationTimerRunning()) {
            stopAnimationTimer();
            setStatus("Paused");
            setMessage("Search paused. Resume to continue or reset to start again.");
            updateButtons();
        } else if (stepWorker != null) {
            cancelCurrentRun();
            setStatus("Ready");
            setMessage("Search preparation cancelled.");
            updateButtons();
        }
    }

    private JPanel createView() {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setOpaque(false);
        root.add(createHeader(), BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);
        content.add(createControls(), BorderLayout.NORTH);
        content.add(createGridPanel(), BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);
        root.add(createStatusArea(), BorderLayout.SOUTH);
        return root;
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);
        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.add(label("PATHFINDING ALGORITHMS", ACCENT_TEXT, Font.BOLD, 10));
        titleBlock.add(Box.createVerticalStrut(4));
        titleBlock.add(label("A* Pathfinding Visualizer", PRIMARY_TEXT, Font.BOLD, 25));
        titleBlock.add(Box.createVerticalStrut(4));
        titleBlock.add(label(
                "Explore a four-directional grid using f(n) = g(n) + h(n) and Manhattan distance.",
                BODY_TEXT, Font.PLAIN, 11));
        header.add(titleBlock, BorderLayout.CENTER);
        return header;
    }

    private JPanel createControls() {
        JPanel panel = cardPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                new EmptyBorder(12, 14, 12, 14)));

        JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 0));
        tools.setOpaque(false);
        toolChoice.setPreferredSize(new Dimension(190, 34));
        tools.add(labeledControl("GRID TOOL", toolChoice));
        tools.add(clearGridButton);
        tools.add(randomObstaclesButton);

        JPanel playback = new JPanel();
        playback.setOpaque(false);
        playback.setLayout(new BoxLayout(playback, BoxLayout.X_AXIS));
        playback.add(startButton);
        playback.add(Box.createHorizontalStrut(7));
        playback.add(pauseButton);
        playback.add(Box.createHorizontalStrut(7));
        playback.add(resumeButton);
        playback.add(Box.createHorizontalStrut(7));
        playback.add(resetButton);
        playback.add(Box.createHorizontalGlue());
        JLabel speedLabel = label("SPEED", new Color(0x8793aa), Font.BOLD, 9);
        playback.add(speedLabel);
        playback.add(Box.createHorizontalStrut(9));
        playback.add(speedSlider);
        speedSlider.setPreferredSize(new Dimension(132, 30));
        speedSlider.setMaximumSize(new Dimension(160, 34));

        panel.add(tools);
        panel.add(Box.createVerticalStrut(11));
        panel.add(playback);
        return panel;
    }

    private JPanel createGridPanel() {
        JPanel panel = cardPanel();
        panel.setLayout(new BorderLayout(0, 9));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                new EmptyBorder(12, 14, 10, 14)));

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.add(label("Grid workspace", PRIMARY_TEXT, Font.BOLD, 15));
        heading.add(Box.createVerticalStrut(8));

        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 0));
        legend.setOpaque(false);
        legend.add(legendItem("Start", new Color(0x2fb889)));
        legend.add(legendItem("Destination", new Color(0xe56776)));
        legend.add(legendItem("Wall", new Color(0x060a13)));
        legend.add(legendItem("Frontier", new Color(0x355b82)));
        legend.add(legendItem("Exploring", new Color(0xf0a64b)));
        legend.add(legendItem("Explored", new Color(0x247e83)));
        legend.add(legendItem("Path", new Color(0x8056dc)));
        heading.add(legend);
        panel.add(heading, BorderLayout.NORTH);

        JPanel canvasFrame = new JPanel(new BorderLayout());
        canvasFrame.setOpaque(true);
        canvasFrame.setBackground(new Color(0x0c1426));
        canvasFrame.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x2a3955)),
                new EmptyBorder(3, 3, 3, 3)));
        canvasFrame.add(gridView, BorderLayout.CENTER);
        panel.add(canvasFrame, BorderLayout.CENTER);

        JTextArea explanation = createTextArea(
                "A* prioritizes the lowest f(n) = g(n) + h(n). Manhattan distance is admissible "
                        + "for four-directional, unit-cost movement.", 1);
        explanation.setForeground(BODY_TEXT);
        explanation.setFont(font(Font.PLAIN, 10));
        panel.add(explanation, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createStatusArea() {
        JPanel area = new JPanel(new BorderLayout(0, 7));
        area.setOpaque(false);
        JPanel statistics = new JPanel(new GridLayout(1, 4, 8, 0));
        statistics.setOpaque(false);
        statistics.add(statCard("VISITED CELLS", visitedValue));
        statistics.add(statCard("PATH LENGTH", pathLengthValue));
        statistics.add(statCard("EXPLORED NODES", exploredValue));
        statistics.add(statCard("STATUS", statusValue));
        area.add(statistics, BorderLayout.NORTH);

        JPanel details = cardPanel();
        details.setLayout(new BorderLayout(10, 0));
        details.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                new EmptyBorder(7, 10, 7, 10)));
        details.add(label("CURRENT STEP", new Color(0x71809b), Font.BOLD, 9),
                BorderLayout.WEST);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(stepDetailsValue);
        text.add(Box.createVerticalStrut(3));
        text.add(messageValue);
        details.add(text, BorderLayout.CENTER);
        area.add(details, BorderLayout.CENTER);
        return area;
    }

    private JPanel labeledControl(String title, Component control) {
        JPanel field = new JPanel();
        field.setOpaque(false);
        field.setLayout(new BoxLayout(field, BoxLayout.Y_AXIS));
        JLabel heading = label(title, new Color(0x8793aa), Font.BOLD, 9);
        heading.setAlignmentX(LEFT_ALIGNMENT);
        control.setFont(font(Font.PLAIN, 12));
        if (control instanceof JComponent component) {
            component.setAlignmentX(LEFT_ALIGNMENT);
        }
        field.add(heading);
        field.add(Box.createVerticalStrut(5));
        field.add(control);
        field.setAlignmentX(LEFT_ALIGNMENT);
        return field;
    }

    private JPanel legendItem(String text, Color color) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        item.setOpaque(false);
        JPanel swatch = new JPanel();
        swatch.setOpaque(true);
        swatch.setBackground(color);
        swatch.setPreferredSize(new Dimension(11, 11));
        swatch.setBorder(BorderFactory.createLineBorder(new Color(0x3a4963)));
        item.add(swatch);
        item.add(label(text, BODY_TEXT, Font.PLAIN, 9));
        return item;
    }

    private JPanel statCard(String title, JLabel value) {
        JPanel card = cardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                new EmptyBorder(7, 9, 7, 9)));
        JLabel heading = label(title, new Color(0x71809b), Font.BOLD, 9);
        value.setForeground(PRIMARY_TEXT);
        value.setFont(font(Font.BOLD, 16));
        card.add(heading);
        card.add(Box.createVerticalStrut(4));
        card.add(value);
        return card;
    }

    private void configureControls() {
        toolChoice.addItem(PathfindingTool.SET_START);
        toolChoice.addItem(PathfindingTool.SET_END);
        toolChoice.addItem(PathfindingTool.WALL);
        toolChoice.addItem(PathfindingTool.ERASE);
        toolChoice.setSelectedItem(PathfindingTool.WALL);
        AlgoVisionTheme.styleComboBox(toolChoice);
        toolChoice.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    javax.swing.JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBackground(isSelected
                        ? AlgoVisionTheme.PRIMARY_SELECTED : AlgoVisionTheme.SURFACE_BACKGROUND);
                setForeground(AlgoVisionTheme.TEXT_PRIMARY);
                setBorder(new EmptyBorder(5, 8, 5, 8));
                return this;
            }
        });
        toolChoice.addActionListener(event -> {
            PathfindingTool tool = (PathfindingTool) toolChoice.getSelectedItem();
            if (tool != null) {
                selectedTool = tool;
            }
        });

        styleButton(startButton, true, 78);
        styleButton(pauseButton, false, 78);
        styleButton(resumeButton, false, 82);
        styleButton(resetButton, false, 78);
        styleButton(clearGridButton, false, 100);
        styleButton(randomObstaclesButton, false, 138);
        startButton.addActionListener(event -> startSearch());
        pauseButton.addActionListener(event -> pause());
        resumeButton.addActionListener(event -> resume());
        resetButton.addActionListener(event -> resetSearch());
        clearGridButton.addActionListener(event -> clearGrid());
        randomObstaclesButton.addActionListener(event -> randomizeObstacles());
        speedSlider.setOpaque(false);
        speedSlider.setMajorTickSpacing(50);
        speedSlider.setPaintTicks(true);
        speedSlider.setToolTipText("Adjust A* animation speed");
        speedSlider.addChangeListener(event -> updateTimerSpeed());
    }

    private void editCell(GridPosition position) {
        PathfindingTool tool = (PathfindingTool) toolChoice.getSelectedItem();
        if (tool != null) {
            selectedTool = tool;
        }
        if (isSearchActive() || stepWorker != null || currentStep != null) {
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
        String status = statusValue.getText();
        if (stepWorker != null || isAnimationTimerRunning()
                || "Paused".equals(status) || "Preparing".equals(status)
                || "Completed".equals(status)) {
            return;
        }

        cancelCurrentRun();
        int runId = generationId;
        PathfindingGrid searchGrid = grid.copy();
        steps = List.of();
        stepIndex = 0;
        currentStep = null;
        pathAnimationStep = null;
        pathToAnimate = List.of();
        pathAnimationIndex = 0;
        gridView.render(null);
        setStatus("Preparing");
        setMessage("Preparing A* search steps.");
        updateStatistics();
        updateButtons();

        stepWorker = new SwingWorker<>() {
            @Override
            protected List<AStarStep> doInBackground() {
                return algorithm.generateSteps(searchGrid);
            }

            @Override
            protected void done() {
                if (runId != generationId || stepWorker != this) {
                    return;
                }
                stepWorker = null;
                try {
                    steps = get();
                    stepIndex = 0;
                    if (steps.isEmpty()) {
                        finishSearch();
                        return;
                    }
                    setStatus("Running");
                    setMessage("A* search started.");
                    updateTimerSpeed();
                    startAnimationTimer(runId);
                    updateButtons();
                } catch (CancellationException exception) {
                    setStatus("Ready");
                    setMessage("A* search preparation was cancelled.");
                    updateButtons();
                } catch (InterruptedException exception) {
                    failSearch("A* search result retrieval was interrupted: " + exception.getMessage());
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause() == null
                            ? exception : exception.getCause();
                    String detail = cause.getMessage();
                    failSearch("A* search failed: "
                            + (detail == null || detail.isBlank()
                                    ? cause.getClass().getSimpleName() : detail));
                }
            }
        };
        stepWorker.execute();
    }

    private void resume() {
        if (!"Paused".equals(statusValue.getText()) || steps.isEmpty()) {
            return;
        }
        setStatus("Running");
        setMessage("A* search resumed.");
        updateTimerSpeed();
        startAnimationTimer(generationId);
        updateButtons();
    }

    private void startAnimationTimer(int runId) {
        stopAnimationTimer();
        Timer timer = new Timer(timerDelayMillis, event -> {
            if (animationTimer != event.getSource() || runId != generationId) {
                return;
            }
            advance();
        });
        timer.setCoalesce(true);
        animationTimer = timer;
        timer.start();
    }

    private void advance() {
        if (pathAnimationStep != null) {
            if (pathAnimationIndex < pathToAnimate.size()) {
                advancePathAnimation();
                return;
            }
            pathAnimationStep = null;
            pathToAnimate = List.of();
            pathAnimationIndex = 0;
        }
        if (stepIndex >= steps.size()) {
            finishSearch();
            return;
        }
        currentStep = steps.get(stepIndex++);
        if (currentStep.event() == AStarEvent.PATH_FOUND && !currentStep.path().isEmpty()) {
            pathAnimationStep = currentStep;
            pathToAnimate = currentStep.path();
            pathAnimationIndex = 0;
            advancePathAnimation();
            return;
        }
        gridView.render(currentStep);
        updateStatistics();
        stepDetailsValue.setText(detailsFor(currentStep));
        setMessage(messageFor(currentStep));
        if (currentStep.event() == AStarEvent.COMPLETE) {
            finishSearch();
        }
    }

    private void advancePathAnimation() {
        pathAnimationIndex++;
        gridView.render(pathAnimationStep, pathToAnimate.subList(0, pathAnimationIndex));
        updateStatistics();
        stepDetailsValue.setText("Reconstructing shortest path: "
                + pathAnimationIndex + " of " + pathToAnimate.size() + " cells.");
        setMessage("Drawing the final path one cell at a time.");
    }

    private void finishSearch() {
        stopAnimationTimer();
        setStatus("Completed");
        updateStatistics();
        updateButtons();
        if (currentStep != null && currentStep.path().isEmpty()) {
            setMessage("No path exists. Try erasing walls or randomizing the obstacles.");
        } else if (currentStep != null) {
            setMessage("Shortest path found with "
                    + Math.max(0, currentStep.path().size() - 1) + " moves.");
        }
    }

    private void resetSearch() {
        cancelCurrentRun();
        steps = List.of();
        stepIndex = 0;
        currentStep = null;
        pathAnimationStep = null;
        pathToAnimate = List.of();
        pathAnimationIndex = 0;
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

    private void renderGrid() {
        gridView.render(currentStep);
    }

    private void cancelCurrentRun() {
        generationId++;
        if (stepWorker != null) {
            stepWorker.cancel(true);
            stepWorker = null;
        }
        stopAnimationTimer();
    }

    private void stopAnimationTimer() {
        if (animationTimer != null) {
            animationTimer.stop();
            animationTimer = null;
        }
    }

    private void updateTimerSpeed() {
        timerDelayMillis = intervalFor(speedSlider.getValue());
        if (animationTimer != null) {
            animationTimer.setDelay(timerDelayMillis);
            animationTimer.setInitialDelay(timerDelayMillis);
        }
    }

    private void updateStatistics() {
        if (currentStep == null) {
            visitedValue.setText("0");
            pathLengthValue.setText("--");
            exploredValue.setText("0");
            return;
        }
        visitedValue.setText(Integer.toString(
                currentStep.openCells().size() + currentStep.exploredCells().size()));
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
        boolean active = isSearchActive();
        boolean completed = "Completed".equals(status);
        startButton.setEnabled(!active && !completed && stepWorker == null);
        pauseButton.setEnabled("Running".equals(status) && animationTimer != null);
        resumeButton.setEnabled("Paused".equals(status) && !steps.isEmpty());
        resetButton.setEnabled(active || stepWorker != null || !steps.isEmpty()
                || currentStep != null);
        clearGridButton.setEnabled(!grid.walls().isEmpty() || active || stepWorker != null
                || !steps.isEmpty());
    }

    private boolean isSearchActive() {
        String status = statusValue.getText();
        return "Preparing".equals(status) || "Running".equals(status)
                || "Paused".equals(status);
    }

    private boolean isAnimationTimerRunning() {
        return animationTimer != null && animationTimer.isRunning();
    }

    private void failSearch(String message) {
        setStatus("Failed");
        setMessage(message);
        updateButtons();
    }

    private String messageFor(AStarStep step) {
        return switch (step.event()) {
            case INITIALIZED -> "Start is in the priority queue with g = 0 and h = "
                    + step.hScores().get(grid.start()) + ".";
            case PRIORITY_QUEUE_UPDATE -> "Priority queue selected "
                    + (step.currentCell() == null
                            ? "a cell" : formatPosition(step.currentCell())) + ".";
            case CURRENT_CELL -> "Exploring " + formatPosition(step.currentCell())
                    + " with g = " + step.gScores().get(step.currentCell())
                    + ", h = " + step.hScores().get(step.currentCell())
                    + ", f = " + step.fScores().get(step.currentCell()) + ".";
            case NEIGHBOR_EVALUATION -> "Evaluating "
                    + formatPosition(step.evaluatedNeighbor())
                    + ": g = " + step.candidateGScore()
                    + ", h = " + step.candidateHScore()
                    + ", f = " + step.candidateFScore() + ".";
            case QUEUE_ADD -> step.evaluatedNeighbor() == null
                    ? "Added the start cell to the priority queue."
                    : "Added " + formatPosition(step.evaluatedNeighbor())
                            + " to the priority queue with f = "
                            + step.candidateFScore() + ".";
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

    private void setStatus(String status) {
        statusValue.setText(status);
        statusValue.setForeground(switch (status) {
            case "Running" -> AlgoVisionTheme.SUCCESS;
            case "Paused" -> AlgoVisionTheme.WARNING;
            case "Completed" -> AlgoVisionTheme.ACCENT;
            case "Failed" -> AlgoVisionTheme.ERROR;
            default -> PRIMARY_TEXT;
        });
    }

    private void setMessage(String message) {
        messageValue.setText(message);
    }

    private JPanel cardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(PANEL_BACKGROUND);
        panel.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        return panel;
    }

    private JLabel label(String text, Color color, int style, int size) {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(font(style, size));
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private JTextArea createTextArea(String text, int rows) {
        JTextArea area = new JTextArea(text, rows, 0);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setForeground(new Color(0xc5cbea));
        area.setFont(font(Font.PLAIN, 10));
        area.setBorder(null);
        area.setFocusable(false);
        return area;
    }

    private void styleButton(JButton button, boolean primary, int width) {
        AlgoVisionTheme.styleButton(button, primary, width);
        button.setPreferredSize(new Dimension(width, 36));
        button.setMaximumSize(new Dimension(width, 36));
    }

    private Font font(int style, int size) {
        return AlgoVisionTheme.font(style, size);
    }

    private static int intervalFor(int value) {
        double rate = 0.35 + value / 100.0 * 4.65;
        return Math.max(1, (int) Math.round(BASE_INTERVAL_MILLIS / rate));
    }

    int stepIndexForTesting() {
        return stepIndex;
    }

    int pathAnimationIndexForTesting() {
        return pathAnimationIndex;
    }

    int timerDelayForTesting() {
        return timerDelayMillis;
    }

    boolean isAnimationTimerRunningForTesting() {
        return isAnimationTimerRunning();
    }

    String statusForTesting() {
        return statusValue.getText();
    }

    PathfindingGrid gridForTesting() {
        return grid;
    }

    PathfindingGridView gridViewForTesting() {
        return gridView;
    }
}
