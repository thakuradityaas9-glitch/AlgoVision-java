package com.algovision.graph.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record GraphTraversalStep(
        int currentNode,
        Set<Integer> visitedNodes,
        GraphEdge currentEdge,
        List<Integer> traversalOrder,
        List<Integer> queueState,
        List<Integer> stackState,
        GraphTraversalEvent event) implements GraphVisualizationStep {

    public GraphTraversalStep {
        visitedNodes = Collections.unmodifiableSet(new LinkedHashSet<>(visitedNodes));
        traversalOrder = List.copyOf(traversalOrder);
        queueState = List.copyOf(queueState);
        stackState = List.copyOf(stackState);
    }
}
