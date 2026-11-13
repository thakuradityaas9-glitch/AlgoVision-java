package com.algovision.graph.algorithms;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import com.algovision.graph.model.Graph;
import com.algovision.graph.model.GraphEdge;
import com.algovision.graph.model.GraphTraversalEvent;
import com.algovision.graph.model.GraphTraversalStep;

public final class BreadthFirstSearch implements GraphTraversalAlgorithm {
    @Override
    public String name() {
        return "BFS";
    }

    @Override
    public List<GraphTraversalStep> generateSteps(Graph graph, int startNodeId) {
        if (!graph.containsNode(startNodeId)) {
            throw new IllegalArgumentException("Choose a valid starting node.");
        }

        TraversalStepRecorder recorder = new TraversalStepRecorder();
        Set<Integer> visited = new LinkedHashSet<>();
        List<Integer> order = new ArrayList<>();
        Queue<Integer> queue = new ArrayDeque<>();

        visited.add(startNodeId);
        queue.add(startNodeId);
        recorder.record(startNodeId, visited, null, order, queueState(queue), List.of(),
                GraphTraversalEvent.START);

        while (!queue.isEmpty()) {
            int current = queue.remove();
            order.add(current);
            recorder.record(current, visited, null, order, queueState(queue), List.of(),
                    GraphTraversalEvent.VISIT);

            for (int neighbor : graph.neighbors(current)) {
                GraphEdge edge = graph.getEdge(current, neighbor);
                recorder.record(current, visited, edge, order, queueState(queue), List.of(),
                        GraphTraversalEvent.INSPECT_EDGE);
                if (visited.add(neighbor)) {
                    queue.add(neighbor);
                    recorder.record(neighbor, visited, edge, order, queueState(queue), List.of(),
                            GraphTraversalEvent.DISCOVER);
                }
            }
        }

        recorder.record(-1, visited, null, order, List.of(), List.of(), GraphTraversalEvent.COMPLETE);
        return recorder.steps();
    }

    private List<Integer> queueState(Queue<Integer> queue) {
        return List.copyOf(queue);
    }
}
