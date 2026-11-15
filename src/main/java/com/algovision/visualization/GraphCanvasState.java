package com.algovision.visualization;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.algovision.graph.model.GraphEdge;
import com.algovision.graph.model.GraphNode;
import com.algovision.graph.model.GraphVisualizationStep;

public record GraphCanvasState(
        List<GraphNode> nodes,
        List<GraphEdge> edges,
        Map<Integer, NodePosition> positions,
        GraphVisualizationStep traversalStep,
        Set<GraphEdge> exploredEdges,
        Integer selectedNodeId,
        GraphEdge selectedEdge) {

    public GraphCanvasState {
        nodes = List.copyOf(nodes);
        edges = List.copyOf(edges);
        positions = Map.copyOf(positions);
        exploredEdges = Set.copyOf(exploredEdges);
    }

    public static GraphCanvasState empty() {
        return new GraphCanvasState(List.of(), List.of(), Map.of(), null, Set.of(), null, null);
    }

    public record NodePosition(double xRatio, double yRatio) {
        public NodePosition {
            if (!Double.isFinite(xRatio) || !Double.isFinite(yRatio)
                    || xRatio < 0 || xRatio > 1 || yRatio < 0 || yRatio > 1) {
                throw new IllegalArgumentException("Node position ratios must be between 0 and 1.");
            }
        }
    }
}
