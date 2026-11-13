package com.algovision.algorithms;

import java.util.Arrays;
import java.util.List;

import com.algovision.model.SortStep;

public final class InsertionSort implements SortingAlgorithm {
    @Override
    public String name() {
        return "Insertion Sort";
    }

    @Override
    public String description() {
        return "Insert each value into its correct position in the sorted prefix.";
    }

    @Override
    public String bestCaseTimeComplexity() {
        return "O(n)";
    }

    @Override
    public String averageCaseTimeComplexity() {
        return "O(n^2)";
    }

    @Override
    public String worstCaseTimeComplexity() {
        return "O(n^2)";
    }

    @Override
    public String auxiliarySpaceComplexity() {
        return "O(1)";
    }

    @Override
    public List<SortStep> generateSteps(int[] input) {
        int[] values = input.clone();
        boolean[] sorted = new boolean[values.length];
        SortStepRecorder recorder = new SortStepRecorder();

        if (values.length > 0) {
            sorted[0] = true;
        }
        for (int index = 1; index < values.length; index++) {
            int key = values[index];
            int position = index - 1;
            boolean moved = false;
            while (position >= 0) {
                recorder.comparison(values, position, index, sorted);
                if (values[position] <= key) {
                    break;
                }
                if (!moved) {
                    Arrays.fill(sorted, 0, index + 1, false);
                    moved = true;
                }
                values[position + 1] = values[position];
                recorder.move(values, position, position + 1, sorted);
                position--;
            }

            int insertionIndex = position + 1;
            if (insertionIndex != index) {
                values[insertionIndex] = key;
                recorder.overwrite(values, index, insertionIndex, sorted);
            }
            Arrays.fill(sorted, 0, index + 1, true);
            recorder.markSorted(values, sorted);
        }

        recorder.complete(values, sorted);
        return recorder.steps();
    }
}
