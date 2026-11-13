package com.algovision.graph.model;

public record GraphEdge(int fromId, int toId, long weight) {
    public GraphEdge(int fromId, int toId) {
        this(fromId, toId, 1);
    }

    public GraphEdge {
        if (fromId == toId) {
            throw new IllegalArgumentException("An edge must connect two different nodes.");
        }
        if (weight < 0) {
            throw new IllegalArgumentException("Edge weight cannot be negative.");
        }
        if (fromId > toId) {
            int swap = fromId;
            fromId = toId;
            toId = swap;
        }
    }

    public boolean connects(int firstId, int secondId) {
        return (fromId == firstId && toId == secondId)
                || (fromId == secondId && toId == firstId);
    }
}
