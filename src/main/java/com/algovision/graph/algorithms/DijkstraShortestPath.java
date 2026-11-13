package com.algovision.graph.algorithms;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import com.algovision.graph.model.DijkstraEvent;
import com.algovision.graph.model.DijkstraStep;
import com.algovision.graph.model.Graph;
import com.algovision.graph.model.GraphEdge;
import com.algovision.graph.model.PriorityQueueEntry;

public final class DijkstraShortestPath implements GraphTraversalAlgorithm {
    private static final Comparator<PriorityQueueEntry> ENTRY_ORDER =
            Comparator.comparing(PriorityQueueEntry::distance)
                    .thenComparingInt(PriorityQueueEntry::nodeId);

    @Override
    public String name() {
        return "Dijkstra";
    }

    @Override
    public List<DijkstraStep> generateSteps(Graph graph, int sourceNodeId) {
        return generateSteps(graph, sourceNodeId, sourceNodeId);
    }

    @Override
    public List<DijkstraStep> generateSteps(Graph graph, int sourceNodeId, int destinationNodeId) {
        if (!graph.containsNode(sourceNodeId)) {
            throw new IllegalArgumentException("Choose a valid source node.");
        }
        if (!graph.containsNode(destinationNodeId)) {
            throw new IllegalArgumentException("Choose a valid destination node.");
        }

        List<DijkstraStep> steps = new ArrayList<>();
        Set<Integer> visited = new LinkedHashSet<>();
        List<Integer> explorationOrder = new ArrayList<>();
        Map<Integer, BigInteger> distances = new LinkedHashMap<>();
        Map<Integer, Integer> predecessors = new LinkedHashMap<>();
        PriorityQueue<PriorityQueueEntry> priorityQueue = new PriorityQueue<>(ENTRY_ORDER);
        int edgesExamined = 0;

        BigInteger zero = BigInteger.ZERO;
        distances.put(sourceNodeId, zero);
        priorityQueue.add(new PriorityQueueEntry(sourceNodeId, zero));
        record(steps, sourceNodeId, destinationNodeId, sourceNodeId, visited, null,
                explorationOrder, distances, predecessors, priorityQueue, edgesExamined,
                null, List.of(), null, DijkstraEvent.START);
        record(steps, sourceNodeId, destinationNodeId, sourceNodeId, visited, null,
                explorationOrder, distances, predecessors, priorityQueue, edgesExamined,
                null, List.of(), null, DijkstraEvent.PRIORITY_QUEUE_UPDATE);

        while (!priorityQueue.isEmpty()) {
            PriorityQueueEntry entry = priorityQueue.remove();
            record(steps, sourceNodeId, destinationNodeId, entry.nodeId(), visited, null,
                    explorationOrder, distances, predecessors, priorityQueue, edgesExamined,
                    null, List.of(), null, DijkstraEvent.PRIORITY_QUEUE_UPDATE);

            BigInteger knownDistance = distances.get(entry.nodeId());
            if (!entry.distance().equals(knownDistance) || !visited.add(entry.nodeId())) {
                continue;
            }

            int currentNode = entry.nodeId();
            explorationOrder.add(currentNode);
            record(steps, sourceNodeId, destinationNodeId, currentNode, visited, null,
                    explorationOrder, distances, predecessors, priorityQueue, edgesExamined,
                    null, List.of(), null, DijkstraEvent.NODE_EXPLORATION);

            if (currentNode == destinationNodeId) {
                break;
            }

            for (int neighborId : graph.neighbors(currentNode)) {
                GraphEdge edge = graph.getEdge(currentNode, neighborId);
                BigInteger candidate = knownDistance.add(BigInteger.valueOf(edge.weight()));
                edgesExamined++;
                record(steps, sourceNodeId, destinationNodeId, currentNode, visited, edge,
                        explorationOrder, distances, predecessors, priorityQueue, edgesExamined,
                        candidate, List.of(), null, DijkstraEvent.EDGE_RELAXATION);

                BigInteger previousDistance = distances.get(neighborId);
                if (previousDistance == null || candidate.compareTo(previousDistance) < 0) {
                    distances.put(neighborId, candidate);
                    predecessors.put(neighborId, currentNode);
                    record(steps, sourceNodeId, destinationNodeId, currentNode, visited, edge,
                            explorationOrder, distances, predecessors, priorityQueue, edgesExamined,
                            candidate, List.of(), null, DijkstraEvent.DISTANCE_UPDATE);

                    priorityQueue.add(new PriorityQueueEntry(neighborId, candidate));
                    record(steps, sourceNodeId, destinationNodeId, currentNode, visited, edge,
                            explorationOrder, distances, predecessors, priorityQueue, edgesExamined,
                            candidate, List.of(), null, DijkstraEvent.PRIORITY_QUEUE_UPDATE);
                }
            }
        }

        BigInteger shortestDistance = distances.get(destinationNodeId);
        List<Integer> shortestPath = shortestDistance == null
                ? List.of()
                : reconstructPath(sourceNodeId, destinationNodeId, predecessors);
        record(steps, sourceNodeId, destinationNodeId, destinationNodeId, visited, null,
                explorationOrder, distances, predecessors, priorityQueue, edgesExamined,
                null, shortestPath, shortestDistance, DijkstraEvent.FINAL_SHORTEST_PATH);
        record(steps, sourceNodeId, destinationNodeId, destinationNodeId, visited, null,
                explorationOrder, distances, predecessors, priorityQueue, edgesExamined,
                null, shortestPath, shortestDistance, DijkstraEvent.COMPLETE);
        return List.copyOf(steps);
    }

    private List<Integer> reconstructPath(
            int sourceNodeId, int destinationNodeId, Map<Integer, Integer> predecessors) {
        List<Integer> reversedPath = new ArrayList<>();
        int currentNode = destinationNodeId;
        reversedPath.add(currentNode);
        while (currentNode != sourceNodeId) {
            Integer predecessor = predecessors.get(currentNode);
            if (predecessor == null) {
                return List.of();
            }
            currentNode = predecessor;
            reversedPath.add(currentNode);
        }
        Collections.reverse(reversedPath);
        return List.copyOf(reversedPath);
    }

    private void record(
            List<DijkstraStep> steps,
            int sourceNode,
            int destinationNode,
            int currentNode,
            Set<Integer> visited,
            GraphEdge currentEdge,
            List<Integer> explorationOrder,
            Map<Integer, BigInteger> distances,
            Map<Integer, Integer> predecessors,
            PriorityQueue<PriorityQueueEntry> priorityQueue,
            int edgesExamined,
            BigInteger candidateDistance,
            List<Integer> shortestPath,
            BigInteger shortestDistance,
            DijkstraEvent event) {
        steps.add(new DijkstraStep(
                sourceNode,
                destinationNode,
                currentNode,
                visited,
                currentEdge,
                explorationOrder,
                distances,
                predecessors,
                priorityQueue.stream().sorted(ENTRY_ORDER).toList(),
                edgesExamined,
                candidateDistance,
                shortestPath,
                shortestDistance,
                event));
    }
}
