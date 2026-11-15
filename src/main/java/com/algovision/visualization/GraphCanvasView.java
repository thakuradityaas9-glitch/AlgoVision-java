package com.algovision.visualization;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Comparator;
import java.util.Objects;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.algovision.graph.model.DijkstraEvent;
import com.algovision.graph.model.DijkstraStep;
import com.algovision.graph.model.GraphEdge;
import com.algovision.graph.model.GraphNode;

public final class GraphCanvasView extends JPanel {
    private static final int NODE_RADIUS = 28;
    private static final int EDGE_HIT_WIDTH = 18;
    private static final int CANVAS_MARGIN = 42;
    private static final Color CANVAS_BACKGROUND = new Color(0x0c1426);
    private static final Color EDGE_COLOR = new Color(0x45536d);
    private static final Color EXPLORED_EDGE_COLOR = new Color(0x279a79);
    private static final Color ACTIVE_EDGE_COLOR = new Color(0xf0a64b);
    private static final Color PATH_COLOR = new Color(0xa78bfa);
    private static final Color SELECTED_COLOR = new Color(0x8d89ff);
    private static final Color NODE_TEXT = new Color(0xf3f5ff);
    private static final Color DISTANCE_TEXT = new Color(0x9cecff);

    private GraphCanvasState canvasState = GraphCanvasState.empty();
    private GraphInteractionMode interactionMode = GraphInteractionMode.SELECT;
    private GraphCanvasListener listener;
    private int pressedNodeId = -1;
    private Point pressedAt;
    private boolean draggingNode;

    public GraphCanvasView() {
        setOpaque(true);
        setBackground(CANVAS_BACKGROUND);
        setMinimumSize(new Dimension(400, 360));
        setPreferredSize(new Dimension(900, 470));
        setFocusable(true);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                pressedNodeId = SwingUtilities.isLeftMouseButton(event)
                        && interactionMode == GraphInteractionMode.SELECT
                                ? nodeAt(event.getPoint()) : -1;
                pressedAt = event.getPoint();
                draggingNode = false;
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                pressedNodeId = -1;
                pressedAt = null;
            }

            @Override
            public void mouseClicked(MouseEvent event) {
                if (!isEnabled() || !SwingUtilities.isLeftMouseButton(event)
                        || event.getClickCount() != 1 || draggingNode || listener == null) {
                    draggingNode = false;
                    return;
                }
                int nodeId = nodeAt(event.getPoint());
                if (nodeId >= 0) {
                    listener.onNodeClick(nodeId);
                    return;
                }
                GraphEdge edge = edgeAt(event.getPoint());
                if (edge != null) {
                    listener.onEdgeClick(edge);
                    return;
                }
                double usableWidth = Math.max(1, getWidth() - 2.0 * CANVAS_MARGIN);
                double usableHeight = Math.max(1, getHeight() - 2.0 * CANVAS_MARGIN);
                listener.onEmptyCanvasClick(
                        clamp((event.getX() - CANVAS_MARGIN) / usableWidth),
                        clamp((event.getY() - CANVAS_MARGIN) / usableHeight));
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent event) {
                if (!isEnabled() || pressedNodeId < 0 || pressedAt == null || listener == null) {
                    return;
                }
                if (!draggingNode && pressedAt.distance(event.getPoint()) < 4) {
                    return;
                }
                draggingNode = true;
                double usableWidth = Math.max(1, getWidth() - 2.0 * CANVAS_MARGIN);
                double usableHeight = Math.max(1, getHeight() - 2.0 * CANVAS_MARGIN);
                listener.onNodeMoved(pressedNodeId,
                        clamp((event.getX() - CANVAS_MARGIN) / usableWidth),
                        clamp((event.getY() - CANVAS_MARGIN) / usableHeight));
            }
        });
    }

    public void setListener(GraphCanvasListener listener) {
        this.listener = listener;
    }

    public void setInteractionMode(GraphInteractionMode interactionMode) {
        this.interactionMode = Objects.requireNonNull(interactionMode, "interactionMode");
        setCursor(interactionMode == GraphInteractionMode.ADD_NODE
                ? java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.CROSSHAIR_CURSOR)
                : java.awt.Cursor.getDefaultCursor());
    }

    public void render(GraphCanvasState canvasState) {
        this.canvasState = Objects.requireNonNull(canvasState, "canvasState");
        repaint();
    }

    public GraphCanvasState currentState() {
        return canvasState;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        for (GraphEdge edge : canvasState.edges()) {
            drawEdge(g2, edge);
        }
        for (GraphNode node : canvasState.nodes()) {
            drawNode(g2, node);
        }
        g2.dispose();
    }

    private void drawEdge(Graphics2D g2, GraphEdge edge) {
        Point2D.Double from = canvasPoint(edge.fromId());
        Point2D.Double to = canvasPoint(edge.toId());
        double deltaX = to.x - from.x;
        double deltaY = to.y - from.y;
        double length = Math.max(1, Math.hypot(deltaX, deltaY));
        double unitX = deltaX / length;
        double unitY = deltaY / length;
        double startX = from.x + unitX * NODE_RADIUS;
        double startY = from.y + unitY * NODE_RADIUS;
        double endX = to.x - unitX * NODE_RADIUS;
        double endY = to.y - unitY * NODE_RADIUS;

        boolean onPath = isShortestPathEdge(edge);
        boolean active = canvasState.traversalStep() != null
                && edge.equals(canvasState.traversalStep().currentEdge());
        boolean explored = canvasState.exploredEdges().contains(edge);
        g2.setColor(edge.equals(canvasState.selectedEdge()) ? SELECTED_COLOR
                : onPath ? PATH_COLOR
                : active ? ACTIVE_EDGE_COLOR
                : explored ? EXPLORED_EDGE_COLOR
                : EDGE_COLOR);
        g2.setStroke(new BasicStroke(edge.equals(canvasState.selectedEdge()) || onPath || active ? 3 : 2,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new Line2D.Double(startX, startY, endX, endY));

        String weight = Long.toString(edge.weight());
        Font labelFont = font(Font.BOLD, 10);
        g2.setFont(labelFont);
        FontMetrics metrics = g2.getFontMetrics();
        double middleX = (startX + endX) / 2 - unitY * 12;
        double middleY = (startY + endY) / 2 + unitX * 12;
        int labelWidth = metrics.stringWidth(weight) + 10;
        int labelHeight = metrics.getHeight();
        int labelX = (int) Math.round(middleX - labelWidth / 2.0);
        int labelY = (int) Math.round(middleY - labelHeight / 2.0);
        g2.setColor(new Color(0x111b30));
        g2.fillRoundRect(labelX, labelY, labelWidth, labelHeight, 8, 8);
        g2.setColor(new Color(0xd7def0));
        g2.drawString(weight, labelX + 5, labelY + metrics.getAscent());
    }

    private void drawNode(Graphics2D g2, GraphNode node) {
        Point2D.Double center = canvasPoint(node.id());
        int x = (int) Math.round(center.x - NODE_RADIUS);
        int y = (int) Math.round(center.y - NODE_RADIUS);
        Color fill = nodeColor(node.id());
        g2.setColor(fill);
        g2.fill(new Ellipse2D.Double(x, y, NODE_RADIUS * 2.0, NODE_RADIUS * 2.0));
        g2.setColor(nodeBorderColor(node.id()));
        g2.setStroke(new BasicStroke(
                Objects.equals(node.id(), canvasState.selectedNodeId()) ? 3 : 2));
        g2.draw(new Ellipse2D.Double(x, y, NODE_RADIUS * 2.0, NODE_RADIUS * 2.0));

        g2.setFont(font(Font.BOLD, 12));
        FontMetrics metrics = g2.getFontMetrics();
        String label = node.label();
        g2.setColor(NODE_TEXT);
        g2.drawString(label,
                (int) Math.round(center.x - metrics.stringWidth(label) / 2.0),
                (int) Math.round(center.y + (metrics.getAscent() - metrics.getDescent()) / 2.0));

        if (canvasState.traversalStep() instanceof DijkstraStep dijkstraStep) {
            var distance = dijkstraStep.distances().get(node.id());
            String distanceText = distance == null ? "INF" : distance.toString();
            g2.setFont(font(Font.BOLD, 10));
            FontMetrics distanceMetrics = g2.getFontMetrics();
            int width = distanceMetrics.stringWidth(distanceText) + 10;
            int labelX = (int) Math.round(center.x - width / 2.0);
            int labelY = y - distanceMetrics.getHeight() - 4;
            g2.setColor(new Color(0x111b30));
            g2.fillRoundRect(labelX, labelY, width, distanceMetrics.getHeight(), 8, 8);
            g2.setColor(DISTANCE_TEXT);
            g2.drawString(distanceText, labelX + 5, labelY + distanceMetrics.getAscent());
        }
    }

    private Color nodeColor(int nodeId) {
        if (canvasState.traversalStep() != null
                && canvasState.traversalStep().currentNode() == nodeId) {
            return new Color(0xe99e45);
        }
        if (isShortestPathNode(nodeId)) {
            return new Color(0x694dcc);
        }
        if (canvasState.traversalStep() != null
                && canvasState.traversalStep().visitedNodes().contains(nodeId)) {
            return new Color(0x279a79);
        }
        if (Objects.equals(nodeId, canvasState.selectedNodeId())) {
            return new Color(0x4947b8);
        }
        return new Color(0x202c43);
    }

    private Color nodeBorderColor(int nodeId) {
        if (Objects.equals(nodeId, canvasState.selectedNodeId())) {
            return new Color(0xaaa7ff);
        }
        if (canvasState.traversalStep() != null
                && canvasState.traversalStep().currentNode() == nodeId) {
            return new Color(0xffd18e);
        }
        if (isDistanceUpdatedNode(nodeId)) {
            return new Color(0x9cecff);
        }
        if (isShortestPathNode(nodeId)) {
            return new Color(0xc4aaff);
        }
        if (canvasState.traversalStep() != null
                && canvasState.traversalStep().visitedNodes().contains(nodeId)) {
            return new Color(0x65d8b5);
        }
        return new Color(0x66748d);
    }

    private boolean isShortestPathEdge(GraphEdge edge) {
        if (!(canvasState.traversalStep() instanceof DijkstraStep dijkstraStep)) {
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
        return canvasState.traversalStep() instanceof DijkstraStep dijkstraStep
                && dijkstraStep.shortestPath().contains(nodeId);
    }

    private boolean isDistanceUpdatedNode(int nodeId) {
        if (!(canvasState.traversalStep() instanceof DijkstraStep dijkstraStep)
                || dijkstraStep.event() != DijkstraEvent.DISTANCE_UPDATE
                || dijkstraStep.currentEdge() == null) {
            return false;
        }
        int updatedNode = dijkstraStep.currentEdge().fromId() == dijkstraStep.currentNode()
                ? dijkstraStep.currentEdge().toId() : dijkstraStep.currentEdge().fromId();
        return nodeId == updatedNode;
    }

    private int nodeAt(Point point) {
        return canvasState.nodes().stream()
                .filter(node -> canvasPoint(node.id()).distance(point) <= NODE_RADIUS + 8)
                .min(Comparator.comparingDouble(node -> canvasPoint(node.id()).distance(point)))
                .map(GraphNode::id)
                .orElse(-1);
    }

    private GraphEdge edgeAt(Point point) {
        for (GraphEdge edge : canvasState.edges()) {
            Point2D.Double from = canvasPoint(edge.fromId());
            Point2D.Double to = canvasPoint(edge.toId());
            double dx = to.x - from.x;
            double dy = to.y - from.y;
            double length = Math.max(1, Math.hypot(dx, dy));
            double startX = from.x + dx / length * NODE_RADIUS;
            double startY = from.y + dy / length * NODE_RADIUS;
            double endX = to.x - dx / length * NODE_RADIUS;
            double endY = to.y - dy / length * NODE_RADIUS;
            if (Line2D.ptSegDist(startX, startY, endX, endY, point.x, point.y)
                    <= EDGE_HIT_WIDTH / 2.0) {
                return edge;
            }
        }
        return null;
    }

    private Point2D.Double canvasPoint(int nodeId) {
        GraphCanvasState.NodePosition position = canvasState.positions().getOrDefault(
                nodeId, new GraphCanvasState.NodePosition(0.5, 0.5));
        double usableWidth = Math.max(1, getWidth() - 2.0 * CANVAS_MARGIN);
        double usableHeight = Math.max(1, getHeight() - 2.0 * CANVAS_MARGIN);
        return new Point2D.Double(
                CANVAS_MARGIN + position.xRatio() * usableWidth,
                CANVAS_MARGIN + position.yRatio() * usableHeight);
    }

    private double clamp(double ratio) {
        return Math.max(0, Math.min(1, ratio));
    }

    private Font font(int style, int size) {
        return new Font("Segoe UI", style, size);
    }
}
