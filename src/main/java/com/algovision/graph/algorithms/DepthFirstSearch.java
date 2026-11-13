package com.algovision.graph.algorithms;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.algovision.graph.model.Graph;
import com.algovision.graph.model.GraphEdge;
import com.algovision.graph.model.GraphTraversalEvent;
import com.algovision.graph.model.GraphTraversalStep;

public final class DepthFirstSearch implements GraphTraversalAlgorithm {
    @Override
    public String name() {
        return "DFS";
    }

    @Override
    public List<GraphTraversalStep> generateSteps(Graph graph, int startNodeId) {
        if (!graph.containsNode(startNodeId)) {
            throw new IllegalArgumentException("Choose a valid starting node.");
        }

        TraversalStepRecorder recorder = new TraversalStepRecorder();
        Set<Integer> visited = new LinkedHashSet<>();
        List<Integer> order = new ArrayList<>();
        Deque<Frame> stack = new ArrayDeque<>();

        visited.add(startNodeId);
        order.add(startNodeId);
        stack.push(new Frame(startNodeId, graph.neighbors(startNodeId)));
        recorder.record(startNodeId, visited, null, order, List.of(), stackState(stack),
                GraphTraversalEvent.START);

        while (!stack.isEmpty()) {
            Frame frame = stack.peek();
            if (frame.hasNextNeighbor()) {
                int neighbor = frame.nextNeighbor();
                GraphEdge edge = graph.getEdge(frame.nodeId(), neighbor);
                recorder.record(frame.nodeId(), visited, edge, order, List.of(), stackState(stack),
                        GraphTraversalEvent.INSPECT_EDGE);

                if (visited.add(neighbor)) {
                    order.add(neighbor);
                    stack.push(new Frame(neighbor, graph.neighbors(neighbor)));
                    recorder.record(neighbor, visited, edge, order, List.of(), stackState(stack),
                            GraphTraversalEvent.DISCOVER);
                }
            } else {
                Frame completed = stack.pop();
                recorder.record(completed.nodeId(), visited, null, order, List.of(), stackState(stack),
                        GraphTraversalEvent.BACKTRACK);
            }
        }

        recorder.record(-1, visited, null, order, List.of(), List.of(), GraphTraversalEvent.COMPLETE);
        return recorder.steps();
    }

    private List<Integer> stackState(Deque<Frame> stack) {
        return stack.stream().map(Frame::nodeId).toList();
    }

    private static final class Frame {
        private final int nodeId;
        private final List<Integer> neighbors;
        private int nextIndex;

        private Frame(int nodeId, List<Integer> neighbors) {
            this.nodeId = nodeId;
            this.neighbors = neighbors;
        }

        private int nodeId() {
            return nodeId;
        }

        private boolean hasNextNeighbor() {
            return nextIndex < neighbors.size();
        }

        private int nextNeighbor() {
            return neighbors.get(nextIndex++);
        }
    }
}
