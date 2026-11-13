package com.algovision.pathfinding.model;

public enum AStarEvent {
    INITIALIZED,
    PRIORITY_QUEUE_UPDATE,
    CURRENT_CELL,
    NEIGHBOR_EVALUATION,
    QUEUE_ADD,
    PATH_FOUND,
    NO_PATH,
    COMPLETE
}
