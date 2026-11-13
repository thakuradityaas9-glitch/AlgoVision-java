package com.algovision.graph.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Graph {
    private final Map<Integer, GraphNode> nodes = new LinkedHashMap<>();
    private final Map<Integer, LinkedHashMap<Integer, Long>> adjacencyList = new LinkedHashMap<>();
    private int nextNodeId = 1;

    public GraphNode addNode() {
        GraphNode node = new GraphNode(nextNodeId++);
        nodes.put(node.id(), node);
        adjacencyList.put(node.id(), new LinkedHashMap<>());
        return node;
    }

    public boolean removeNode(int nodeId) {
        Map<Integer, Long> neighbors = adjacencyList.remove(nodeId);
        if (neighbors == null) {
            return false;
        }
        nodes.remove(nodeId);
        for (int neighborId : neighbors.keySet()) {
            adjacencyList.get(neighborId).remove(nodeId);
        }
        return true;
    }

    public boolean connect(int firstId, int secondId) {
        return connect(firstId, secondId, 1);
    }

    public boolean connect(int firstId, int secondId, long weight) {
        requireNode(firstId);
        requireNode(secondId);
        if (firstId == secondId) {
            throw new IllegalArgumentException("A node cannot connect to itself.");
        }
        requireWeight(weight);
        boolean added = !adjacencyList.get(firstId).containsKey(secondId);
        if (added) {
            adjacencyList.get(firstId).put(secondId, weight);
            adjacencyList.get(secondId).put(firstId, weight);
        }
        return added;
    }

    public void setEdgeWeight(int firstId, int secondId, long weight) {
        requireNode(firstId);
        requireNode(secondId);
        requireWeight(weight);
        if (!adjacencyList.get(firstId).containsKey(secondId)) {
            throw new IllegalArgumentException("The selected nodes are not connected.");
        }
        adjacencyList.get(firstId).put(secondId, weight);
        adjacencyList.get(secondId).put(firstId, weight);
    }

    public GraphEdge getEdge(int firstId, int secondId) {
        requireNode(firstId);
        requireNode(secondId);
        Long weight = adjacencyList.get(firstId).get(secondId);
        if (weight == null) {
            throw new IllegalArgumentException("The selected nodes are not connected.");
        }
        return new GraphEdge(firstId, secondId, weight);
    }

    public boolean removeEdge(GraphEdge edge) {
        if (!containsNode(edge.fromId()) || !containsNode(edge.toId())) {
            return false;
        }
        boolean removed = adjacencyList.get(edge.fromId()).remove(edge.toId()) != null;
        adjacencyList.get(edge.toId()).remove(edge.fromId());
        return removed;
    }

    public void clear() {
        nodes.clear();
        adjacencyList.clear();
        nextNodeId = 1;
    }

    public boolean containsNode(int nodeId) {
        return nodes.containsKey(nodeId);
    }

    public GraphNode getNode(int nodeId) {
        requireNode(nodeId);
        return nodes.get(nodeId);
    }

    public List<GraphNode> nodes() {
        return List.copyOf(nodes.values());
    }

    public List<GraphEdge> edges() {
        List<GraphEdge> edges = new ArrayList<>();
        adjacencyList.forEach((nodeId, neighbors) -> {
            for (Map.Entry<Integer, Long> neighbor : neighbors.entrySet()) {
                int neighborId = neighbor.getKey();
                if (nodeId < neighborId) {
                    edges.add(new GraphEdge(nodeId, neighborId, neighbor.getValue()));
                }
            }
        });
        return List.copyOf(edges);
    }

    public List<Integer> neighbors(int nodeId) {
        requireNode(nodeId);
        return List.copyOf(adjacencyList.get(nodeId).keySet());
    }

    public Map<Integer, List<Integer>> adjacencyList() {
        Map<Integer, List<Integer>> snapshot = new LinkedHashMap<>();
        adjacencyList.forEach((nodeId, neighbors) ->
                snapshot.put(nodeId, List.copyOf(neighbors.keySet())));
        return Collections.unmodifiableMap(snapshot);
    }

    public int nodeCount() {
        return nodes.size();
    }

    public int edgeCount() {
        return edges().size();
    }

    private void requireNode(int nodeId) {
        if (!containsNode(nodeId)) {
            throw new IllegalArgumentException("Node " + nodeId + " does not exist.");
        }
    }

    private void requireWeight(long weight) {
        if (weight < 0) {
            throw new IllegalArgumentException("Edge weight cannot be negative.");
        }
    }
}
