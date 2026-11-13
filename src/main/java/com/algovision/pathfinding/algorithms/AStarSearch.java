package com.algovision.pathfinding.algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import com.algovision.pathfinding.model.AStarEvent;
import com.algovision.pathfinding.model.AStarQueueEntry;
import com.algovision.pathfinding.model.AStarStep;
import com.algovision.pathfinding.model.GridPosition;
import com.algovision.pathfinding.model.PathfindingGrid;

public final class AStarSearch {
    private static final Comparator<AStarQueueEntry> QUEUE_ORDER =
            Comparator.comparingInt(AStarQueueEntry::fScore)
                    .thenComparingInt(AStarQueueEntry::hScore)
                    .thenComparingLong(AStarQueueEntry::sequence);

    public List<AStarStep> generateSteps(PathfindingGrid grid) {
        if (grid == null) {
            throw new IllegalArgumentException("A grid is required.");
        }
        GridPosition start = grid.start();
        GridPosition end = grid.end();
        if (!grid.contains(start) || !grid.contains(end)) {
            throw new IllegalArgumentException("Start and destination must be inside the grid.");
        }
        if (grid.isWall(start) || grid.isWall(end)) {
            throw new IllegalArgumentException("Start and destination cells cannot be blocked.");
        }

        List<AStarStep> steps = new ArrayList<>();
        Map<GridPosition, GridPosition> predecessors = new LinkedHashMap<>();
        Map<GridPosition, Integer> gScores = new LinkedHashMap<>();
        Map<GridPosition, Integer> hScores = new LinkedHashMap<>();
        Map<GridPosition, Integer> fScores = new LinkedHashMap<>();
        Set<GridPosition> openCells = new LinkedHashSet<>();
        Set<GridPosition> exploredCells = new LinkedHashSet<>();
        PriorityQueue<AStarQueueEntry> priorityQueue = new PriorityQueue<>(QUEUE_ORDER);
        long sequence = 0;

        int startHeuristic = manhattanDistance(start, end);
        gScores.put(start, 0);
        hScores.put(start, startHeuristic);
        fScores.put(start, startHeuristic);
        openCells.add(start);
        priorityQueue.add(new AStarQueueEntry(start, 0, startHeuristic, startHeuristic, sequence++));
        record(steps, null, null, openCells, exploredCells, gScores, hScores, fScores,
                priorityQueue, List.of(), null, null, null, AStarEvent.INITIALIZED);
        record(steps, start, null, openCells, exploredCells, gScores, hScores, fScores,
                priorityQueue, List.of(), null, null, null, AStarEvent.QUEUE_ADD);

        List<GridPosition> finalPath = List.of();
        boolean found = false;
        while (!priorityQueue.isEmpty()) {
            AStarQueueEntry entry = priorityQueue.remove();
            Integer bestKnownG = gScores.get(entry.position());
            if (bestKnownG == null || entry.gScore() != bestKnownG
                    || exploredCells.contains(entry.position())) {
                record(steps, entry.position(), null, openCells, exploredCells,
                        gScores, hScores, fScores, priorityQueue, finalPath,
                        null, null, null, AStarEvent.PRIORITY_QUEUE_UPDATE);
                continue;
            }

            openCells.remove(entry.position());
            record(steps, entry.position(), null, openCells, exploredCells,
                    gScores, hScores, fScores, priorityQueue, finalPath,
                    null, null, null, AStarEvent.PRIORITY_QUEUE_UPDATE);

            exploredCells.add(entry.position());
            record(steps, entry.position(), null, openCells, exploredCells,
                    gScores, hScores, fScores, priorityQueue, finalPath,
                    null, null, null, AStarEvent.CURRENT_CELL);

            if (entry.position().equals(end)) {
                finalPath = reconstructPath(start, end, predecessors);
                found = true;
                record(steps, entry.position(), null, openCells, exploredCells,
                        gScores, hScores, fScores, priorityQueue, finalPath,
                        null, null, null, AStarEvent.PATH_FOUND);
                break;
            }

            for (GridPosition neighbor : grid.neighbors(entry.position())) {
                if (grid.isWall(neighbor)) {
                    continue;
                }

                int candidateG = gScores.get(entry.position()) + 1;
                int candidateH = manhattanDistance(neighbor, end);
                int candidateF = candidateG + candidateH;
                Integer previousG = gScores.get(neighbor);
                record(steps, entry.position(), neighbor, openCells, exploredCells,
                        gScores, hScores, fScores, priorityQueue, finalPath,
                        candidateG, candidateH, candidateF, AStarEvent.NEIGHBOR_EVALUATION);

                if (previousG == null || candidateG < previousG) {
                    predecessors.put(neighbor, entry.position());
                    gScores.put(neighbor, candidateG);
                    hScores.put(neighbor, candidateH);
                    fScores.put(neighbor, candidateF);
                    exploredCells.remove(neighbor);
                    openCells.add(neighbor);
                    priorityQueue.add(new AStarQueueEntry(
                            neighbor, candidateG, candidateH, candidateF, sequence++));
                    record(steps, entry.position(), neighbor, openCells, exploredCells,
                            gScores, hScores, fScores, priorityQueue, finalPath,
                            candidateG, candidateH, candidateF, AStarEvent.QUEUE_ADD);
                }
            }
        }

        if (found) {
            record(steps, end, null, openCells, exploredCells, gScores, hScores, fScores,
                    priorityQueue, finalPath, null, null, null, AStarEvent.COMPLETE);
        } else {
            record(steps, null, null, openCells, exploredCells, gScores, hScores, fScores,
                    priorityQueue, finalPath, null, null, null, AStarEvent.NO_PATH);
            record(steps, null, null, openCells, exploredCells, gScores, hScores, fScores,
                    priorityQueue, finalPath, null, null, null, AStarEvent.COMPLETE);
        }
        return List.copyOf(steps);
    }

    public static int manhattanDistance(GridPosition first, GridPosition second) {
        return Math.abs(first.row() - second.row())
                + Math.abs(first.column() - second.column());
    }

    private List<GridPosition> reconstructPath(
            GridPosition start,
            GridPosition end,
            Map<GridPosition, GridPosition> predecessors) {
        List<GridPosition> reversedPath = new ArrayList<>();
        GridPosition current = end;
        reversedPath.add(current);
        while (!current.equals(start)) {
            current = predecessors.get(current);
            if (current == null) {
                return List.of();
            }
            reversedPath.add(current);
        }
        Collections.reverse(reversedPath);
        return List.copyOf(reversedPath);
    }

    private void record(
            List<AStarStep> steps,
            GridPosition currentCell,
            GridPosition evaluatedNeighbor,
            Set<GridPosition> openCells,
            Set<GridPosition> exploredCells,
            Map<GridPosition, Integer> gScores,
            Map<GridPosition, Integer> hScores,
            Map<GridPosition, Integer> fScores,
            PriorityQueue<AStarQueueEntry> priorityQueue,
            List<GridPosition> path,
            Integer candidateG,
            Integer candidateH,
            Integer candidateF,
            AStarEvent event) {
        steps.add(new AStarStep(
                currentCell,
                evaluatedNeighbor,
                openCells,
                exploredCells,
                gScores,
                hScores,
                fScores,
                priorityQueue.stream().sorted(QUEUE_ORDER).toList(),
                path,
                candidateG,
                candidateH,
                candidateF,
                event));
    }
}
