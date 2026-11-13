package com.algovision.graph.model;

public record GraphNode(int id) {
    public String label() {
        return Integer.toString(id);
    }
}
