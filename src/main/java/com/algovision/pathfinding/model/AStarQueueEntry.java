package com.algovision.pathfinding.model;

public record AStarQueueEntry(
        GridPosition position,
        int gScore,
        int hScore,
        int fScore,
        long sequence) {
}
