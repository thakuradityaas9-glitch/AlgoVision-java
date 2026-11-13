package com.algovision.graph.algorithms;

import java.util.List;

import com.algovision.graph.model.Graph;
import com.algovision.graph.model.GraphVisualizationStep;

public interface GraphTraversalAlgorithm {
    String name();

    List<? extends GraphVisualizationStep> generateSteps(Graph graph, int startNodeId);

    default List<? extends GraphVisualizationStep> generateSteps(
            Graph graph, int sourceNodeId, int destinationNodeId) {
        return generateSteps(graph, sourceNodeId);
    }
}
