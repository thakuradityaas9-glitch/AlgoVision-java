package com.algovision.controller;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import com.algovision.graph.algorithms.BreadthFirstSearch;
import com.algovision.graph.algorithms.DepthFirstSearch;
import com.algovision.graph.algorithms.DijkstraShortestPath;
import com.algovision.graph.algorithms.GraphTraversalAlgorithm;
import com.algovision.graph.model.DijkstraEvent;
import com.algovision.graph.model.DijkstraStep;
import com.algovision.graph.model.Graph;
import com.algovision.graph.model.GraphEdge;
import com.algovision.graph.model.GraphNode;
import com.algovision.graph.model.GraphTraversalStep;
import com.algovision.graph.model.GraphVisualizationStep;
import com.algovision.visualization.GraphCanvasListener;
import com.algovision.visualization.GraphCanvasView;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class GraphVisualizerController {
    private static final int RANDOM_MIN_NODES = 7;
    private static final int RANDOM_MAX_NODES = 9;

    private final Graph graph = new Graph();
    private final Map<Integer, Point2D> nodePositions = new LinkedHashMap<>();
    private final List<GraphTraversalAlgorithm> algorithms = List.of(
            new BreadthFirstSearch(),
            new DepthFirstSearch(),
            new DijkstraShortestPath());
    private final GraphCanvasView canvas = new GraphCanvasView();
    private final ComboBox<GraphTraversalAlgorithm> algorithmChoice = new ComboBox<>();
    private final ComboBox<Integer> startNodeChoice = new ComboBox<>();
    private final ComboBox<Integer> destinationNodeChoice = new ComboBox<>();
    private final TextField edgeWeightInput = new TextField("1");
    private final Slider speedSlider = new Slider(0, 100, 50);
    private final Label nodeCountValue = new Label("0");
    private final Label edgeCountValue = new Label("0");
    private final Label visitedCountValue = new Label("0");
    private final Label edgesExaminedValue = new Label("0");
    private final Label algorithmValue = new Label("BFS");
    private final Label statusValue = new Label("Ready");
    private final Label currentDistanceValue = new Label("--");
    private final Label finalDistanceValue = new Label("--");
    private final Label traversalOrderValue = new Label("--");
    private final Label frontierValue = new Label("--");
    private final Label shortestPathValue = new Label("--");
    private final Label messageValue = new Label("Add nodes on the canvas or generate a random graph.");
    private final Button playButton = new Button("Play");
    private final Button pauseButton = new Button("Pause");
    private final Button resetButton = new Button("Reset");
    private final Button deleteNodeButton = new Button("Delete Selected Node");
    private final Button deleteEdgeButton = new Button("Delete Selected Edge");
    private final Button updateEdgeWeightButton = new Button("Update Edge Weight");
    private final Button clearGraphButton = new Button("Clear Graph");

    private final ScrollPane view;
    private GraphTraversalAlgorithm selectedAlgorithm = algorithms.get(0);
    private List<? extends GraphVisualizationStep> steps = List.of();
    private GraphVisualizationStep currentStep;
    private Timeline timeline;
    private int stepIndex;
    private Integer pendingNodeId;
    private GraphEdge selectedEdge;
    private boolean updatingStartNodes;

    public GraphVisualizerController() {
        canvas.setListener(new GraphCanvasListener() {
            @Override
            public void onEmptyCanvasClick(double xRatio, double yRatio) {
                addNode(xRatio, yRatio);
            }

            @Override
            public void onNodeClick(int nodeId) {
                selectNode(nodeId);
            }

            @Override
            public void onEdgeClick(GraphEdge edge) {
                selectEdge(edge);
            }
        });
        view = createView();
        updateStartNodeChoices();
        updateStatistics();
        updateButtons();
        renderCanvas();
    }

    public Node getView() {
        return view;
    }

    public void pause() {
        if (timeline != null && "Running".equals(statusValue.getText())) {
            timeline.pause();
            setStatus("Paused");
            updateButtons();
        }
    }

    private ScrollPane createView() {
        VBox content = new VBox(18);
        content.getStyleClass().add("graph-content");
        content.setPadding(new Insets(34, 40, 30, 40));

        VBox introduction = new VBox(8);
        Label eyebrow = new Label("GRAPH ALGORITHMS");
        eyebrow.getStyleClass().add("eyebrow");
        Label heading = new Label("Graph Visualizer");
        heading.getStyleClass().add("welcome-heading");
        Label description = new Label(
                "Build a weighted graph and explore BFS, DFS, or Dijkstra's shortest path.");
        description.getStyleClass().add("welcome-subtitle");
        description.setWrapText(true);
        introduction.getChildren().addAll(eyebrow, heading, description);

        VBox controls = createControls();
        HBox graphStatistics = new HBox(12,
                createStatCard("TOTAL NODES", nodeCountValue),
                createStatCard("TOTAL EDGES", edgeCountValue),
                createStatCard("NODES VISITED", visitedCountValue),
                createStatCard("EDGES EXAMINED", edgesExaminedValue));
        HBox pathStatistics = new HBox(12,
                createStatCard("ALGORITHM", algorithmValue),
                createStatCard("STATUS", statusValue),
                createStatCard("CURRENT SHORTEST DISTANCE", currentDistanceValue),
                createStatCard("FINAL SHORTEST DISTANCE", finalDistanceValue));
        graphStatistics.setAlignment(Pos.CENTER);
        pathStatistics.setAlignment(Pos.CENTER);
        VBox statistics = new VBox(10, graphStatistics, pathStatistics);

        HBox traversalDetails = new HBox(14,
                createDetailCard("TRAVERSAL ORDER", traversalOrderValue),
                createDetailCard("QUEUE / STACK / PRIORITY QUEUE", frontierValue),
                createDetailCard("SHORTEST PATH", shortestPathValue));
        traversalDetails.setAlignment(Pos.CENTER);

        VBox canvasPanel = new VBox(10);
        canvasPanel.getStyleClass().add("graph-canvas-panel");
        canvasPanel.setPadding(new Insets(16));
        Label instructions = new Label(
                "Click empty space to add a node. Select two nodes to connect them using the edge weight above; click an edge to select it.");
        instructions.getStyleClass().add("graph-instructions");
        instructions.setWrapText(true);
        canvasPanel.getChildren().addAll(instructions, canvas);
        VBox.setVgrow(canvas, Priority.ALWAYS);

        messageValue.getStyleClass().add("graph-message");
        messageValue.setWrapText(true);
        content.getChildren().addAll(introduction, controls, statistics, traversalDetails, canvasPanel,
                messageValue);
        VBox.setVgrow(canvasPanel, Priority.ALWAYS);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("content-scroll");
        return scrollPane;
    }

    private VBox createControls() {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("controls-card");
        panel.setPadding(new Insets(16));

        algorithmChoice.getItems().setAll(algorithms);
        algorithmChoice.getSelectionModel().selectFirst();
        algorithmChoice.setMaxWidth(Double.MAX_VALUE);
        algorithmChoice.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(GraphTraversalAlgorithm item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item.name());
            }
        });
        algorithmChoice.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(GraphTraversalAlgorithm item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item.name());
            }
        });
        algorithmChoice.setOnAction(event -> selectAlgorithm());

        startNodeChoice.setPromptText("Select source node");
        startNodeChoice.setMaxWidth(Double.MAX_VALUE);
        startNodeChoice.setOnAction(event -> {
            if (!updatingStartNodes) {
                resetTraversal();
            }
        });

        destinationNodeChoice.setPromptText("Select destination");
        destinationNodeChoice.setMaxWidth(Double.MAX_VALUE);
        destinationNodeChoice.setDisable(true);
        destinationNodeChoice.setOnAction(event -> {
            if (!updatingStartNodes) {
                resetTraversal();
            }
        });

        Button randomGraphButton = new Button("Generate Random Graph");
        randomGraphButton.getStyleClass().add("secondary-button");
        randomGraphButton.setOnAction(event -> generateRandomGraph());
        clearGraphButton.getStyleClass().add("secondary-button");
        clearGraphButton.setOnAction(event -> clearGraph());

        HBox selectionRow = new HBox(10,
                labeledControl("ALGORITHM", algorithmChoice),
                labeledControl("SOURCE NODE", startNodeChoice),
                labeledControl("DESTINATION NODE", destinationNodeChoice));
        selectionRow.setAlignment(Pos.BOTTOM_LEFT);
        HBox.setHgrow(selectionRow.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(selectionRow.getChildren().get(1), Priority.ALWAYS);
        HBox.setHgrow(selectionRow.getChildren().get(2), Priority.ALWAYS);

        edgeWeightInput.getStyleClass().add("array-input");
        edgeWeightInput.setPromptText("Non-negative integer");
        edgeWeightInput.setPrefColumnCount(10);
        updateEdgeWeightButton.getStyleClass().add("secondary-button");
        updateEdgeWeightButton.setOnAction(event -> updateSelectedEdgeWeight());
        HBox weightRow = new HBox(10,
                labeledControl("EDGE WEIGHT (NON-NEGATIVE INTEGER)", edgeWeightInput),
                updateEdgeWeightButton,
                createSpacer(),
                randomGraphButton,
                clearGraphButton);
        weightRow.setAlignment(Pos.BOTTOM_LEFT);

        playButton.getStyleClass().add("primary-button");
        pauseButton.getStyleClass().add("secondary-button");
        resetButton.getStyleClass().add("secondary-button");
        deleteNodeButton.getStyleClass().add("secondary-button");
        deleteEdgeButton.getStyleClass().add("secondary-button");
        playButton.setOnAction(event -> play());
        pauseButton.setOnAction(event -> pause());
        resetButton.setOnAction(event -> resetTraversal());
        deleteNodeButton.setOnAction(event -> deleteSelectedNode());
        deleteEdgeButton.setOnAction(event -> deleteSelectedEdge());

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
        HBox playbackRow = new HBox(10, playButton, pauseButton, resetButton,
                createSpacer(), speedLabel, speedSlider);
        playbackRow.setAlignment(Pos.CENTER_LEFT);

        HBox editRow = new HBox(10, deleteNodeButton, deleteEdgeButton);
        editRow.setAlignment(Pos.CENTER_LEFT);
        panel.getChildren().addAll(selectionRow, weightRow, playbackRow, editRow);
        return panel;
    }

    private Node labeledControl(String labelText, Node control) {
        VBox field = new VBox(7);
        Label label = new Label(labelText);
        label.getStyleClass().add("control-label");
        field.getChildren().addAll(label, control);
        return field;
    }

    private Region createSpacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private VBox createStatCard(String title, Label value) {
        VBox card = new VBox(8, new Label(title), value);
        card.getStyleClass().add("stat-card");
        card.getChildren().get(0).getStyleClass().add("stat-title");
        value.getStyleClass().add("stat-value");
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private VBox createDetailCard(String title, Label value) {
        VBox card = new VBox(8, new Label(title), value);
        card.getStyleClass().add("stat-card");
        card.getChildren().get(0).getStyleClass().add("stat-title");
        value.getStyleClass().add("graph-detail-value");
        value.setWrapText(true);
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private void addNode(double xRatio, double yRatio) {
        resetTraversal();
        GraphNode node = graph.addNode();
        nodePositions.put(node.id(), new Point2D(xRatio, yRatio));
        pendingNodeId = node.id();
        selectedEdge = null;
        updateStartNodeChoices();
        setMessage("Node " + node.label() + " added and selected. Select another node to connect it.");
        refreshGraph();
    }

    private void selectNode(int nodeId) {
        resetTraversal();
        selectedEdge = null;
        if (pendingNodeId == null) {
            pendingNodeId = nodeId;
            setMessage("Node " + nodeId + " selected. Select another node to connect, or delete it.");
        } else if (pendingNodeId == nodeId) {
            pendingNodeId = null;
            setMessage("Node selection cleared.");
        } else {
            Long weight = readEdgeWeight();
            if (weight == null) {
                refreshGraph();
                return;
            }
            int firstNode = pendingNodeId;
            if (graph.connect(firstNode, nodeId, weight)) {
                pendingNodeId = null;
                setMessage("Connected nodes " + firstNode + " and " + nodeId + " with weight " + weight + ".");
            } else {
                pendingNodeId = null;
                setMessage("Those nodes are already connected.");
            }
        }
        refreshGraph();
    }

    private void selectEdge(GraphEdge edge) {
        resetTraversal();
        pendingNodeId = null;
        selectedEdge = edge;
        edgeWeightInput.setText(Long.toString(edge.weight()));
        setMessage("Edge " + edge.fromId() + " - " + edge.toId() + " selected.");
        refreshGraph();
    }

    private void updateSelectedEdgeWeight() {
        if (selectedEdge == null) {
            setMessage("Select an edge before updating its weight.");
            return;
        }
        Long weight = readEdgeWeight();
        if (weight == null) {
            return;
        }
        resetTraversal();
        graph.setEdgeWeight(selectedEdge.fromId(), selectedEdge.toId(), weight);
        selectedEdge = graph.getEdge(selectedEdge.fromId(), selectedEdge.toId());
        setMessage("Updated edge " + selectedEdge.fromId() + " - " + selectedEdge.toId()
                + " to weight " + weight + ".");
        refreshGraph();
    }

    private Long readEdgeWeight() {
        String text = edgeWeightInput.getText();
        try {
            long weight = Long.parseLong(text.trim());
            if (weight < 0) {
                setMessage("Edge weights must be non-negative.");
                return null;
            }
            return weight;
        } catch (NumberFormatException exception) {
            setMessage("Enter a non-negative whole-number edge weight.");
            return null;
        }
    }

    private void deleteSelectedNode() {
        if (pendingNodeId == null) {
            return;
        }
        int nodeId = pendingNodeId;
        resetTraversal();
        graph.removeNode(nodeId);
        nodePositions.remove(nodeId);
        pendingNodeId = null;
        selectedEdge = null;
        updateStartNodeChoices();
        setMessage("Node " + nodeId + " deleted.");
        refreshGraph();
    }

    private void deleteSelectedEdge() {
        if (selectedEdge == null) {
            return;
        }
        GraphEdge edge = selectedEdge;
        resetTraversal();
        graph.removeEdge(edge);
        selectedEdge = null;
        pendingNodeId = null;
        setMessage("Edge " + edge.fromId() + " - " + edge.toId() + " deleted.");
        refreshGraph();
    }

    private void clearGraph() {
        resetTraversal();
        graph.clear();
        nodePositions.clear();
        pendingNodeId = null;
        selectedEdge = null;
        updateStartNodeChoices();
        setMessage("Graph cleared. Click on the canvas to add nodes.");
        refreshGraph();
    }

    private void generateRandomGraph() {
        resetTraversal();
        graph.clear();
        nodePositions.clear();
        pendingNodeId = null;
        selectedEdge = null;

        Random random = new Random();
        int nodeCount = RANDOM_MIN_NODES + random.nextInt(RANDOM_MAX_NODES - RANDOM_MIN_NODES + 1);
        List<GraphNode> nodes = new ArrayList<>();
        for (int index = 0; index < nodeCount; index++) {
            nodes.add(graph.addNode());
        }

        double angleOffset = random.nextDouble() * Math.PI * 2;
        for (int index = 0; index < nodeCount; index++) {
            double angle = angleOffset + index * Math.PI * 2 / nodeCount;
            nodePositions.put(nodes.get(index).id(), new Point2D(
                    0.5 + 0.38 * Math.cos(angle),
                    0.5 + 0.38 * Math.sin(angle)));
            if (index > 0) {
                graph.connect(nodes.get(index).id(), nodes.get(random.nextInt(index)).id(),
                        1 + random.nextInt(9));
            }
        }

        int targetEdges = Math.min(nodeCount * (nodeCount - 1) / 2,
                nodeCount + random.nextInt(nodeCount / 2 + 1));
        while (graph.edgeCount() < targetEdges) {
            int first = random.nextInt(nodeCount);
            int second = random.nextInt(nodeCount);
            if (first != second) {
                graph.connect(nodes.get(first).id(), nodes.get(second).id(),
                        1 + random.nextInt(9));
            }
        }

        updateStartNodeChoices();
        setMessage("Random connected weighted graph generated. Choose an algorithm and press Play.");
        refreshGraph();
    }

    private void selectAlgorithm() {
        GraphTraversalAlgorithm algorithm = algorithmChoice.getValue();
        if (algorithm == null || algorithm == selectedAlgorithm) {
            return;
        }
        selectedAlgorithm = algorithm;
        destinationNodeChoice.setDisable(!(selectedAlgorithm instanceof DijkstraShortestPath));
        resetTraversal();
        setMessage(selectedAlgorithm.name() + " selected.");
        refreshGraph();
    }

    private void updateStartNodeChoices() {
        Integer previousSource = startNodeChoice.getValue();
        Integer previousDestination = destinationNodeChoice.getValue();
        updatingStartNodes = true;
        List<Integer> nodeIds = graph.nodes().stream().map(GraphNode::id).toList();
        startNodeChoice.getItems().setAll(nodeIds);
        destinationNodeChoice.getItems().setAll(nodeIds);
        selectAvailableNode(startNodeChoice, previousSource, nodeIds);
        selectAvailableNode(destinationNodeChoice, previousDestination, nodeIds);
        updatingStartNodes = false;
    }

    private void selectAvailableNode(ComboBox<Integer> choice, Integer previous, List<Integer> nodeIds) {
        if (previous != null && nodeIds.contains(previous)) {
            choice.getSelectionModel().select(previous);
        } else if (!nodeIds.isEmpty()) {
            choice.getSelectionModel().selectFirst();
        } else {
            choice.getSelectionModel().clearSelection();
        }
    }

    private void play() {
        Integer startNode = startNodeChoice.getValue();
        if (graph.nodeCount() == 0) {
            setMessage("Add nodes or generate a random graph before starting a traversal.");
            updateButtons();
            return;
        }
        if (startNode == null || !graph.containsNode(startNode)) {
            setMessage("Choose a valid source node.");
            updateButtons();
            return;
        }
        Integer destinationNode = destinationNodeChoice.getValue();
        if (selectedAlgorithm instanceof DijkstraShortestPath
                && (destinationNode == null || !graph.containsNode(destinationNode))) {
            setMessage("Choose a valid destination node for Dijkstra.");
            updateButtons();
            return;
        }
        if ("Completed".equals(statusValue.getText()) || stepIndex >= steps.size() && !steps.isEmpty()) {
            return;
        }
        if (steps.isEmpty()) {
            steps = selectedAlgorithm.generateSteps(graph, startNode,
                    destinationNode == null ? startNode : destinationNode);
            stepIndex = 0;
        }
        if (timeline == null) {
            timeline = new Timeline(new KeyFrame(Duration.millis(480), event -> advance()));
            timeline.setCycleCount(Timeline.INDEFINITE);
        }
        timeline.setRate(playbackRate(speedSlider.getValue()));
        timeline.play();
        setStatus("Running");
        updateButtons();
    }

    private void advance() {
        if (stepIndex >= steps.size()) {
            finish();
            return;
        }
        currentStep = steps.get(stepIndex++);
        if (currentStep instanceof DijkstraStep dijkstraStep) {
            setMessage(messageFor(dijkstraStep));
        }
        renderCanvas();
        updateStatistics();
        if (stepIndex >= steps.size()) {
            finish();
        }
    }

    private void finish() {
        stopTimeline();
        setStatus("Completed");
        updateStatistics();
        updateButtons();
        if (currentStep instanceof DijkstraStep dijkstraStep) {
            if (dijkstraStep.shortestDistance() == null) {
                setMessage("No path exists from node " + dijkstraStep.sourceNode()
                        + " to node " + dijkstraStep.destinationNode() + ".");
            } else {
                setMessage("Shortest path found: " + formatPath(dijkstraStep.shortestPath())
                        + " (distance " + dijkstraStep.shortestDistance() + ").");
            }
            return;
        }
        if (currentStep != null && currentStep.visitedNodes().size() < graph.nodeCount()) {
            setMessage("Traversal complete. " + currentStep.visitedNodes().size() + " of "
                    + graph.nodeCount() + " nodes are reachable from the selected start.");
        } else {
            setMessage("Traversal complete.");
        }
    }

    private void resetTraversal() {
        stopTimeline();
        steps = List.of();
        stepIndex = 0;
        currentStep = null;
        setStatus("Ready");
        updateStatistics();
        updateButtons();
        renderCanvas();
    }

    private void stopTimeline() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
    }

    private void refreshGraph() {
        updateStatistics();
        updateButtons();
        renderCanvas();
    }

    private void renderCanvas() {
        canvas.render(graph, nodePositions, currentStep, pendingNodeId, selectedEdge);
    }

    private void updateStatistics() {
        nodeCountValue.setText(Integer.toString(graph.nodeCount()));
        edgeCountValue.setText(Integer.toString(graph.edgeCount()));
        visitedCountValue.setText(Integer.toString(
                currentStep == null ? 0 : currentStep.visitedNodes().size()));
        edgesExaminedValue.setText(Integer.toString(edgesExaminedSoFar()));
        algorithmValue.setText(selectedAlgorithm.name());
        traversalOrderValue.setText(currentStep == null || currentStep.traversalOrder().isEmpty()
                ? "--"
                : currentStep.traversalOrder().stream().map(String::valueOf).collect(Collectors.joining(" -> ")));
        if (currentStep instanceof DijkstraStep dijkstraStep) {
            frontierValue.setText("Priority queue: " + dijkstraStep.priorityQueueState().stream()
                    .map(entry -> entry.nodeId() + " (" + entry.distance() + ")")
                    .collect(Collectors.joining(", ", "[", "]")));
            BigInteger currentDistance = dijkstraStep.distances().get(dijkstraStep.destinationNode());
            currentDistanceValue.setText(currentDistance == null ? "INF" : currentDistance.toString());
            finalDistanceValue.setText(dijkstraStep.shortestDistance() == null
                    ? dijkstraStep.event() == DijkstraEvent.COMPLETE
                            || dijkstraStep.event() == DijkstraEvent.FINAL_SHORTEST_PATH ? "No path" : "--"
                    : dijkstraStep.shortestDistance().toString());
            shortestPathValue.setText(dijkstraStep.event() == DijkstraEvent.COMPLETE
                    || dijkstraStep.event() == DijkstraEvent.FINAL_SHORTEST_PATH
                            ? dijkstraStep.shortestPath().isEmpty() ? "No path" : formatPath(dijkstraStep.shortestPath())
                            : "--");
        } else if (currentStep == null) {
            frontierValue.setText("--");
            currentDistanceValue.setText("--");
            finalDistanceValue.setText("--");
            shortestPathValue.setText("--");
        } else if (selectedAlgorithm == algorithms.get(0)) {
            frontierValue.setText("Queue: " + currentStep.queueState());
            currentDistanceValue.setText("--");
            finalDistanceValue.setText("--");
            shortestPathValue.setText("--");
        } else {
            frontierValue.setText("Stack: " + currentStep.stackState());
            currentDistanceValue.setText("--");
            finalDistanceValue.setText("--");
            shortestPathValue.setText("--");
        }
    }

    private int edgesExaminedSoFar() {
        if (currentStep instanceof DijkstraStep dijkstraStep) {
            return dijkstraStep.edgesExamined();
        }
        return (int) steps.stream()
                .limit(stepIndex)
                .filter(GraphTraversalStep.class::isInstance)
                .map(GraphTraversalStep.class::cast)
                .filter(step -> step.event()
                        == com.algovision.graph.model.GraphTraversalEvent.INSPECT_EDGE)
                .count();
    }

    private String messageFor(DijkstraStep step) {
        return switch (step.event()) {
            case START -> "Starting Dijkstra from node " + step.sourceNode() + ".";
            case NODE_EXPLORATION -> "Exploring node " + step.currentNode() + ".";
            case EDGE_RELAXATION -> "Examining edge " + step.currentEdge().fromId() + " - "
                    + step.currentEdge().toId() + " (weight " + step.currentEdge().weight()
                    + ", candidate distance " + step.candidateDistance() + ").";
            case DISTANCE_UPDATE -> "Updated distance to node "
                    + otherEnd(step.currentEdge(), step.currentNode()) + " to "
                    + step.candidateDistance() + ".";
            case PRIORITY_QUEUE_UPDATE -> "Priority queue updated.";
            case FINAL_SHORTEST_PATH -> step.shortestDistance() == null
                    ? "No path exists to destination node " + step.destinationNode() + "."
                    : "Shortest path: " + formatPath(step.shortestPath()) + ".";
            case COMPLETE -> "Dijkstra completed.";
        };
    }

    private int otherEnd(GraphEdge edge, int nodeId) {
        return edge.fromId() == nodeId ? edge.toId() : edge.fromId();
    }

    private String formatPath(List<Integer> path) {
        return path.stream().map(String::valueOf).collect(Collectors.joining(" -> "));
    }

    private void updateButtons() {
        boolean running = "Running".equals(statusValue.getText());
        boolean hasStartNode = startNodeChoice.getValue() != null
                && graph.containsNode(startNodeChoice.getValue());
        boolean needsDestination = selectedAlgorithm instanceof DijkstraShortestPath;
        boolean hasDestination = destinationNodeChoice.getValue() != null
                && graph.containsNode(destinationNodeChoice.getValue());
        playButton.setDisable(running || graph.nodeCount() == 0 || !hasStartNode
                || needsDestination && !hasDestination
                || "Completed".equals(statusValue.getText()));
        pauseButton.setDisable(!running);
        deleteNodeButton.setDisable(pendingNodeId == null);
        deleteEdgeButton.setDisable(selectedEdge == null);
        updateEdgeWeightButton.setDisable(selectedEdge == null);
        clearGraphButton.setDisable(graph.nodeCount() == 0);
    }

    private void setStatus(String status) {
        statusValue.setText(status);
    }

    private void setMessage(String message) {
        messageValue.setText(message);
    }

    private double playbackRate(double value) {
        return 0.25 + value / 100.0 * 2.75;
    }
}
