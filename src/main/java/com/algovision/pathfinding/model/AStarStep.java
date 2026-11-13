package com.algovision.pathfinding.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record AStarStep(
        GridPosition currentCell,
        GridPosition evaluatedNeighbor,
        Set<GridPosition> openCells,
        Set<GridPosition> exploredCells,
        Map<GridPosition, Integer> gScores,
        Map<GridPosition, Integer> hScores,
        Map<GridPosition, Integer> fScores,
        List<AStarQueueEntry> priorityQueueState,
        List<GridPosition> path,
        Integer candidateGScore,
        Integer candidateHScore,
        Integer candidateFScore,
        AStarEvent event) {

    public AStarStep {
        openCells = Collections.unmodifiableSet(new LinkedHashSet<>(openCells));
        exploredCells = Collections.unmodifiableSet(new LinkedHashSet<>(exploredCells));
        gScores = Collections.unmodifiableMap(new LinkedHashMap<>(gScores));
        hScores = Collections.unmodifiableMap(new LinkedHashMap<>(hScores));
        fScores = Collections.unmodifiableMap(new LinkedHashMap<>(fScores));
        priorityQueueState = List.copyOf(priorityQueueState);
        path = List.copyOf(path);
    }
}
