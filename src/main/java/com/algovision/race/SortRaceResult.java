package com.algovision.race;

import java.util.List;

import com.algovision.algorithms.SortingAlgorithm;
import com.algovision.model.SortOperation;
import com.algovision.model.SortStep;

public final class SortRaceResult {
    private final SortingAlgorithm algorithm;
    private final List<SortStep> steps;
    private final long executionTimeNanos;
    private final int comparisons;
    private final int swaps;
    private final int movements;

    SortRaceResult(SortingAlgorithm algorithm, List<SortStep> steps, long executionTimeNanos) {
        this.algorithm = algorithm;
        this.steps = List.copyOf(steps);
        this.executionTimeNanos = executionTimeNanos;

        int comparisonCount = 0;
        int swapCount = 0;
        int movementCount = 0;
        for (SortStep step : steps) {
            if (step.operation() == SortOperation.COMPARE) {
                comparisonCount++;
            } else if (step.operation() == SortOperation.SWAP) {
                swapCount++;
            } else if (step.operation() == SortOperation.MOVE
                    || step.operation() == SortOperation.OVERWRITE) {
                movementCount++;
            }
        }
        comparisons = comparisonCount;
        swaps = swapCount;
        movements = movementCount;
    }

    public SortingAlgorithm algorithm() {
        return algorithm;
    }

    public List<SortStep> steps() {
        return steps;
    }

    public long executionTimeNanos() {
        return executionTimeNanos;
    }

    public int comparisons() {
        return comparisons;
    }

    public int swaps() {
        return swaps;
    }

    public int movements() {
        return movements;
    }

    public int operations() {
        return comparisons + swaps + movements;
    }
}
