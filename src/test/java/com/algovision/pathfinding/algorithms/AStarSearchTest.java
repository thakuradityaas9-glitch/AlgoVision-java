package com.algovision.pathfinding.algorithms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.algovision.pathfinding.model.AStarEvent;
import com.algovision.pathfinding.model.AStarStep;
import com.algovision.pathfinding.model.GridPosition;
import com.algovision.pathfinding.model.PathfindingGrid;

class AStarSearchTest {
    private final AStarSearch search = new AStarSearch();

    @Test
    void findsTheSimpleFourDirectionalShortestPath() {
        GridPosition start = new GridPosition(1, 0);
        GridPosition end = new GridPosition(1, 3);
        PathfindingGrid grid = new PathfindingGrid(3, 4, start, end);

        AStarStep complete = completeStep(search.generateSteps(grid));

        assertEquals(3, complete.path().size() - 1);
        assertEquals(0, complete.gScores().get(start));
        assertEquals(3, complete.gScores().get(end));
        assertEquals(AStarEvent.COMPLETE, complete.event());
        complete.gScores().forEach((position, gScore) -> {
            int hScore = complete.hScores().get(position);
            assertEquals(AStarSearch.manhattanDistance(position, end), hScore);
            assertEquals(gScore + hScore, complete.fScores().get(position));
        });
        assertPathIsValid(grid, complete.path());
    }

    @Test
    void routesAroundWallsAndReconstructsTheParentPath() {
        GridPosition start = new GridPosition(2, 0);
        GridPosition end = new GridPosition(2, 4);
        PathfindingGrid grid = new PathfindingGrid(5, 5, start, end);
        grid.setWall(new GridPosition(2, 1), true);
        grid.setWall(new GridPosition(2, 2), true);
        grid.setWall(new GridPosition(2, 3), true);

        AStarStep complete = completeStep(search.generateSteps(grid));

        assertEquals(6, complete.path().size() - 1);
        assertTrue(complete.path().stream().noneMatch(grid.walls()::contains));
        assertPathIsValid(grid, complete.path());
        assertEquals(complete.path().size() - 1, complete.gScores().get(end));
    }

    @Test
    void reportsWhenDestinationIsUnreachable() {
        GridPosition start = new GridPosition(0, 0);
        GridPosition end = new GridPosition(2, 2);
        PathfindingGrid grid = new PathfindingGrid(3, 3, start, end);
        grid.setWall(new GridPosition(0, 1), true);
        grid.setWall(new GridPosition(1, 0), true);

        List<AStarStep> steps = search.generateSteps(grid);

        assertTrue(steps.stream().anyMatch(step -> step.event() == AStarEvent.NO_PATH));
        assertTrue(completeStep(steps).path().isEmpty());
    }

    @Test
    void handlesAdjacentStartAndDestination() {
        GridPosition start = new GridPosition(0, 0);
        GridPosition end = new GridPosition(0, 1);
        PathfindingGrid grid = new PathfindingGrid(2, 2, start, end);

        AStarStep complete = completeStep(search.generateSteps(grid));

        assertEquals(List.of(start, end), complete.path());
        assertEquals(1, complete.gScores().get(end));
    }

    @Test
    void supportsStartEqualToDestination() {
        GridPosition start = new GridPosition(0, 0);
        PathfindingGrid grid = new PathfindingGrid(1, 1, start, start);

        AStarStep complete = completeStep(search.generateSteps(grid));

        assertEquals(List.of(start), complete.path());
        assertEquals(0, complete.gScores().get(start));
    }

    @Test
    void respectsGridBoundariesAndProtectsEndpointsFromWalls() {
        GridPosition start = new GridPosition(0, 0);
        GridPosition end = new GridPosition(1, 2);
        PathfindingGrid grid = new PathfindingGrid(2, 3, start, end);

        assertEquals(List.of(new GridPosition(0, 1), new GridPosition(1, 0)),
                grid.neighbors(start));
        assertEquals(List.of(new GridPosition(0, 2), new GridPosition(1, 1)),
                grid.neighbors(end));
        assertFalse(grid.setWall(start, true));
        assertFalse(grid.setWall(end, true));
        assertThrows(IllegalArgumentException.class,
                () -> grid.neighbors(new GridPosition(2, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> grid.setWall(new GridPosition(-1, 0), true));
    }

    @Test
    void copyKeepsEndpointsAndWallsWithoutSharingMutableState() {
        GridPosition start = new GridPosition(0, 0);
        GridPosition end = new GridPosition(2, 2);
        PathfindingGrid original = new PathfindingGrid(3, 3, start, end);
        GridPosition wall = new GridPosition(1, 1);
        original.setWall(wall, true);

        PathfindingGrid copy = original.copy();
        copy.clearWalls();
        copy.setStart(new GridPosition(0, 1));

        assertEquals(start, original.start());
        assertEquals(end, original.end());
        assertTrue(original.isWall(wall));
        assertFalse(copy.isWall(wall));
    }

    private AStarStep completeStep(List<AStarStep> steps) {
        AStarStep complete = steps.getLast();
        assertEquals(AStarEvent.COMPLETE, complete.event());
        return complete;
    }

    private void assertPathIsValid(PathfindingGrid grid, List<GridPosition> path) {
        assertFalse(path.isEmpty());
        assertEquals(grid.start(), path.getFirst());
        assertEquals(grid.end(), path.getLast());
        for (int index = 0; index < path.size(); index++) {
            assertTrue(grid.contains(path.get(index)));
            assertFalse(grid.isWall(path.get(index)));
            if (index > 0) {
                assertEquals(1, AStarSearch.manhattanDistance(
                        path.get(index - 1), path.get(index)));
            }
        }
    }
}
