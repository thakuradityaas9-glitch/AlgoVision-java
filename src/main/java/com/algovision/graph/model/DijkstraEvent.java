package com.algovision.graph.model;

public enum DijkstraEvent {
    START,
    NODE_EXPLORATION,
    EDGE_RELAXATION,
    DISTANCE_UPDATE,
    PRIORITY_QUEUE_UPDATE,
    FINAL_SHORTEST_PATH,
    COMPLETE
}
