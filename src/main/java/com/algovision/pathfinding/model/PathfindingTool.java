package com.algovision.pathfinding.model;

public enum PathfindingTool {
    SET_START("Set start"),
    SET_END("Set destination"),
    WALL("Draw walls"),
    ERASE("Erase walls");

    private final String label;

    PathfindingTool(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
