package com.algovision.visualization;

import java.math.BigInteger;
import java.util.Map;
import java.util.Set;

import com.algovision.graph.model.DijkstraEvent;
import com.algovision.graph.model.DijkstraStep;
import com.algovision.graph.model.Graph;
import com.algovision.graph.model.GraphEdge;
import com.algovision.graph.model.GraphNode;
import com.algovision.graph.model.GraphVisualizationStep;

import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

public class GraphCanvasView extends Pane {
    private static final double NODE_RADIUS = 28;
    private static final double EDGE_HIT_WIDTH = 18;
    private static final double CANVAS_MARGIN = 42;

    private Graph graph = new Graph();
    private Map<Integer, Point2D> positions = Map.of();
    private GraphVisualizationStep traversalStep;
    private Integer selectedNodeId;
    private GraphEdge selectedEdge;
    private GraphCanvasListener listener;

    public GraphCanvasView() {
        getStyleClass().add("graph-canvas");
        setMinSize(400, 360);
        setPrefSize(900, 470);
        setPickOnBounds(true);
        setOnMouseClicked(event -> {
            if (event.getTarget() == this && listener != null) {
                double usableWidth = Math.max(1, getWidth() - 2 * CANVAS_MARGIN);
                double usableHeight = Math.max(1, getHeight() - 2 * CANVAS_MARGIN);
                double xRatio = clamp((event.getX() - CANVAS_MARGIN) / usableWidth);
                double yRatio = clamp((event.getY() - CANVAS_MARGIN) / usableHeight);
                listener.onEmptyCanvasClick(xRatio, yRatio);
            }
        });
        widthProperty().addListener((observable, oldValue, newValue) -> render());
        heightProperty().addListener((observable, oldValue, newValue) -> render());
    }

    public void setListener(GraphCanvasListener listener) {
        this.listener = listener;
    }

    public void render(Graph graph, Map<Integer, Point2D> positions,
            GraphVisualizationStep traversalStep, Integer selectedNodeId, GraphEdge selectedEdge) {
        this.graph = graph;
        this.positions = Map.copyOf(positions);
        this.traversalStep = traversalStep;
        this.selectedNodeId = selectedNodeId;
        this.selectedEdge = selectedEdge;
        render();
    }

    private void render() {
        if (graph == null || positions == null) {
            return;
        }
        getChildren().clear();
        for (GraphEdge edge : graph.edges()) {
            addEdge(edge);
        }
        for (GraphNode node : graph.nodes()) {
            StackPane nodeView = createNodeView(node);
            Point2D point = canvasPoint(node.id());
            nodeView.setLayoutX(point.getX() - NODE_RADIUS);
            nodeView.setLayoutY(point.getY() - NODE_RADIUS);
            getChildren().add(nodeView);
        }
    }

    private void addEdge(GraphEdge edge) {
        Point2D from = canvasPoint(edge.fromId());
        Point2D to = canvasPoint(edge.toId());
        double deltaX = to.getX() - from.getX();
        double deltaY = to.getY() - from.getY();
        double length = Math.max(1, Math.hypot(deltaX, deltaY));
        double unitX = deltaX / length;
        double unitY = deltaY / length;
        double startX = from.getX() + unitX * NODE_RADIUS;
        double startY = from.getY() + unitY * NODE_RADIUS;
        double endX = to.getX() - unitX * NODE_RADIUS;
        double endY = to.getY() - unitY * NODE_RADIUS;

        Line hitTarget = new Line(startX, startY, endX, endY);
        hitTarget.setStroke(Color.TRANSPARENT);
        hitTarget.setStrokeWidth(EDGE_HIT_WIDTH);
        hitTarget.setOnMouseClicked(event -> {
            if (listener != null) {
                listener.onEdgeClick(edge);
            }
            event.consume();
        });

        Line visibleLine = new Line(
                startX,
                startY,
                endX,
                endY);
        visibleLine.getStyleClass().add("graph-edge-visible");
        if (edge.equals(selectedEdge)) {
            visibleLine.getStyleClass().add("selected");
        }
        if (isShortestPathEdge(edge)) {
            visibleLine.getStyleClass().add("shortest-path");
        }
        if (traversalStep != null && edge.equals(traversalStep.currentEdge())) {
            visibleLine.getStyleClass().add("active");
        }
        visibleLine.setStrokeWidth(edge.equals(selectedEdge) || isShortestPathEdge(edge)
                || (traversalStep != null && edge.equals(traversalStep.currentEdge())) ? 3 : 2);
        visibleLine.setMouseTransparent(true);
        Label weightLabel = new Label(Long.toString(edge.weight()));
        weightLabel.getStyleClass().add("graph-edge-weight");
        weightLabel.setMouseTransparent(true);
        double middleX = (startX + endX) / 2 - unitY * 12;
        double middleY = (startY + endY) / 2 + unitX * 12;
        weightLabel.setLayoutX(middleX - 10);
        weightLabel.setLayoutY(middleY - 9);
        getChildren().addAll(hitTarget, visibleLine, weightLabel);
    }

    private StackPane createNodeView(GraphNode node) {
        StackPane nodeView = new StackPane();
        nodeView.setAlignment(Pos.CENTER);
        nodeView.setMinSize(NODE_RADIUS * 2, NODE_RADIUS * 2);
        nodeView.setPrefSize(NODE_RADIUS * 2, NODE_RADIUS * 2);
        nodeView.setMaxSize(NODE_RADIUS * 2, NODE_RADIUS * 2);

        Circle circle = new Circle(NODE_RADIUS);
        circle.getStyleClass().add("graph-node-circle");
        Set<Integer> visited = traversalStep == null ? Set.of() : traversalStep.visitedNodes();
        int currentNode = traversalStep == null ? -1 : traversalStep.currentNode();
        DijkstraStep dijkstraStep = traversalStep instanceof DijkstraStep step ? step : null;
        if (selectedNodeId != null && selectedNodeId == node.id()) {
            circle.getStyleClass().add("selected");
        } else if (isDistanceUpdatedNode(node.id())) {
            circle.getStyleClass().add("distance-updated");
        } else if (currentNode == node.id()) {
            circle.getStyleClass().add("active");
        } else if (isShortestPathNode(node.id())) {
            circle.getStyleClass().add("shortest-path");
        } else if (visited.contains(node.id())) {
            circle.getStyleClass().add("visited");
        } else {
            circle.getStyleClass().add("unvisited");
        }

        Label label = new Label(node.label());
        label.getStyleClass().add("graph-node-label");
        nodeView.getChildren().addAll(circle, label);
        nodeView.setOnMouseClicked(event -> {
            if (listener != null) {
                listener.onNodeClick(node.id());
            }
            event.consume();
        });
        if (dijkstraStep != null) {
            BigInteger distance = dijkstraStep.distances().get(node.id());
            Label distanceLabel = new Label(distance == null ? "INF" : distance.toString());
            distanceLabel.getStyleClass().add("graph-distance-label");
            distanceLabel.setMouseTransparent(true);
            distanceLabel.setLayoutX(canvasPoint(node.id()).getX() - 24);
            distanceLabel.setLayoutY(canvasPoint(node.id()).getY() - NODE_RADIUS - 20);
            getChildren().add(distanceLabel);
        }
        return nodeView;
    }

    private boolean isShortestPathEdge(GraphEdge edge) {
        if (!(traversalStep instanceof DijkstraStep dijkstraStep)) {
            return false;
        }
        for (int index = 1; index < dijkstraStep.shortestPath().size(); index++) {
            if (edge.connects(
                    dijkstraStep.shortestPath().get(index - 1),
                    dijkstraStep.shortestPath().get(index))) {
                return true;
            }
        }
        return false;
    }

    private boolean isShortestPathNode(int nodeId) {
        return traversalStep instanceof DijkstraStep dijkstraStep
                && dijkstraStep.shortestPath().contains(nodeId);
    }

    private boolean isDistanceUpdatedNode(int nodeId) {
        if (!(traversalStep instanceof DijkstraStep dijkstraStep)
                || dijkstraStep.event() != DijkstraEvent.DISTANCE_UPDATE
                || dijkstraStep.currentEdge() == null) {
            return false;
        }
        int updatedNode = dijkstraStep.currentEdge().fromId() == dijkstraStep.currentNode()
                ? dijkstraStep.currentEdge().toId()
                : dijkstraStep.currentEdge().fromId();
        return nodeId == updatedNode;
    }

    private Point2D canvasPoint(int nodeId) {
        Point2D position = positions.getOrDefault(nodeId, new Point2D(0.5, 0.5));
        double usableWidth = Math.max(1, getWidth() - 2 * CANVAS_MARGIN);
        double usableHeight = Math.max(1, getHeight() - 2 * CANVAS_MARGIN);
        return new Point2D(
                CANVAS_MARGIN + position.getX() * usableWidth,
                CANVAS_MARGIN + position.getY() * usableHeight);
    }

    private double clamp(double ratio) {
        return Math.max(0, Math.min(1, ratio));
    }
}
