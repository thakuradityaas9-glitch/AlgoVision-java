package com.algovision.pathfinding.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

public final class PathfindingGrid {
    private final int rows;
    private final int columns;
    private final Set<GridPosition> walls = new LinkedHashSet<>();
    private GridPosition start;
    private GridPosition end;

    public PathfindingGrid(int rows, int columns, GridPosition start, GridPosition end) {
        if (rows < 1 || columns < 1) {
            throw new IllegalArgumentException("Grid dimensions must be positive.");
        }
        this.rows = rows;
        this.columns = columns;
        requirePosition(start);
        requirePosition(end);
        this.start = start;
        this.end = end;
    }

    public int rows() {
        return rows;
    }

    public int columns() {
        return columns;
    }

    public GridPosition start() {
        return start;
    }

    public GridPosition end() {
        return end;
    }

    public void setStart(GridPosition position) {
        requirePosition(position);
        walls.remove(position);
        start = position;
    }

    public void setEnd(GridPosition position) {
        requirePosition(position);
        walls.remove(position);
        end = position;
    }

    public boolean contains(GridPosition position) {
        return position != null
                && position.row() >= 0
                && position.row() < rows
                && position.column() >= 0
                && position.column() < columns;
    }

    public boolean isWall(GridPosition position) {
        return walls.contains(position);
    }

    public boolean setWall(GridPosition position, boolean wall) {
        requirePosition(position);
        if (position.equals(start) || position.equals(end)) {
            return false;
        }
        return wall ? walls.add(position) : walls.remove(position);
    }

    public Set<GridPosition> walls() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(walls));
    }

    public List<GridPosition> neighbors(GridPosition position) {
        requirePosition(position);
        List<GridPosition> candidates = List.of(
                new GridPosition(position.row() - 1, position.column()),
                new GridPosition(position.row(), position.column() + 1),
                new GridPosition(position.row() + 1, position.column()),
                new GridPosition(position.row(), position.column() - 1));
        List<GridPosition> validNeighbors = new ArrayList<>(4);
        for (GridPosition candidate : candidates) {
            if (contains(candidate)) {
                validNeighbors.add(candidate);
            }
        }
        return List.copyOf(validNeighbors);
    }

    public void clearWalls() {
        walls.clear();
    }

    public void randomizeWalls(double probability, Random random) {
        if (probability < 0 || probability > 1) {
            throw new IllegalArgumentException("Wall probability must be between zero and one.");
        }
        Objects.requireNonNull(random, "random");
        walls.clear();
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                GridPosition position = new GridPosition(row, column);
                if (!position.equals(start) && !position.equals(end)
                        && random.nextDouble() < probability) {
                    walls.add(position);
                }
            }
        }
    }

    private void requirePosition(GridPosition position) {
        if (!contains(position)) {
            throw new IllegalArgumentException("Cell is outside the grid.");
        }
    }
}
