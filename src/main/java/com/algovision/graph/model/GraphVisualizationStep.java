package com.algovision.graph.model;

import java.util.List;
import java.util.Set;

public interface GraphVisualizationStep {
    int currentNode();

    Set<Integer> visitedNodes();

    GraphEdge currentEdge();

    List<Integer> traversalOrder();

    List<Integer> queueState();

    List<Integer> stackState();
}
