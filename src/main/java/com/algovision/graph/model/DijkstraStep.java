package com.algovision.graph.model;

import java.math.BigInteger;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record DijkstraStep(
        int sourceNode,
        int destinationNode,
        int currentNode,
        Set<Integer> visitedNodes,
        GraphEdge currentEdge,
        List<Integer> traversalOrder,
        Map<Integer, BigInteger> distances,
        Map<Integer, Integer> predecessors,
        List<PriorityQueueEntry> priorityQueueState,
        int edgesExamined,
        BigInteger candidateDistance,
        List<Integer> shortestPath,
        BigInteger shortestDistance,
        DijkstraEvent event) implements GraphVisualizationStep {

    public DijkstraStep {
        visitedNodes = Collections.unmodifiableSet(new LinkedHashSet<>(visitedNodes));
        traversalOrder = List.copyOf(traversalOrder);
        distances = Collections.unmodifiableMap(new LinkedHashMap<>(distances));
        predecessors = Collections.unmodifiableMap(new LinkedHashMap<>(predecessors));
        priorityQueueState = List.copyOf(priorityQueueState);
        shortestPath = List.copyOf(shortestPath);
    }

    @Override
    public List<Integer> queueState() {
        return priorityQueueState.stream().map(PriorityQueueEntry::nodeId).toList();
    }

    @Override
    public List<Integer> stackState() {
        return List.of();
    }
}
