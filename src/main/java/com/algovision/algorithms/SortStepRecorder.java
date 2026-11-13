package com.algovision.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.algovision.model.SortOperation;
import com.algovision.model.SortStep;

final class SortStepRecorder {
    private final List<SortStep> steps = new ArrayList<>();
    private int comparisons;
    private int operations;

    void comparison(int[] values, int left, int right, boolean[] sorted) {
        comparisons++;
        add(values, left, right, false, SortOperation.COMPARE, -1, sorted);
    }

    void swap(int[] values, int left, int right, boolean[] sorted) {
        operations++;
        add(values, left, right, true, SortOperation.SWAP, -1, sorted);
    }

    void move(int[] values, int from, int to, boolean[] sorted) {
        operations++;
        add(values, from, to, false, SortOperation.MOVE, -1, sorted);
    }

    void overwrite(int[] values, int from, int to, boolean[] sorted) {
        operations++;
        add(values, from, to, false, SortOperation.OVERWRITE, -1, sorted);
    }

    void pivot(int[] values, int pivotIndex, boolean[] sorted) {
        add(values, -1, -1, false, SortOperation.PIVOT, pivotIndex, sorted);
    }

    void markSorted(int[] values, boolean[] sorted) {
        add(values, -1, -1, false, SortOperation.MARK_SORTED, -1, sorted);
    }

    void complete(int[] values, boolean[] sorted) {
        Arrays.fill(sorted, true);
        add(values, -1, -1, false, SortOperation.COMPLETE, -1, sorted);
    }

    List<SortStep> steps() {
        return List.copyOf(steps);
    }

    private void add(int[] values, int left, int right, boolean swapped,
            SortOperation operation, int pivotIndex, boolean[] sorted) {
        int sortedSuffix = 0;
        for (int index = sorted.length - 1; index >= 0 && sorted[index]; index--) {
            sortedSuffix++;
        }
        steps.add(new SortStep(values, left, right, swapped, comparisons, operations,
                sortedSuffix, operation, pivotIndex, sorted));
    }
}
