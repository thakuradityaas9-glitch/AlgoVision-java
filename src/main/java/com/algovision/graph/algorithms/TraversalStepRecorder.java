package com.algovision.graph.algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.algovision.graph.model.GraphEdge;
import com.algovision.graph.model.GraphTraversalEvent;
import com.algovision.graph.model.GraphTraversalStep;

final class TraversalStepRecorder {
    private final List<GraphTraversalStep> steps = new ArrayList<>();

    void record(int currentNode, Set<Integer> visited, GraphEdge edge, List<Integer> order,
            List<Integer> queue, List<Integer> stack, GraphTraversalEvent event) {
        steps.add(new GraphTraversalStep(currentNode, visited, edge, order, queue, stack, event));
    }

    List<GraphTraversalStep> steps() {
        return List.copyOf(steps);
    }
}
