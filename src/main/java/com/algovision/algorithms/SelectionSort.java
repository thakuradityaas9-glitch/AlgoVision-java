package com.algovision.algorithms;

import java.util.List;

import com.algovision.model.SortStep;

public final class SelectionSort implements SortingAlgorithm {
    @Override
    public String name() {
        return "Selection Sort";
    }

    @Override
    public String description() {
        return "Find the smallest remaining value and place it at the next position.";
    }

    @Override
    public String bestCaseTimeComplexity() {
        return "O(n^2)";
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

        for (int start = 0; start < values.length - 1; start++) {
            int minimum = start;
            for (int index = start + 1; index < values.length; index++) {
                recorder.comparison(values, minimum, index, sorted);
                if (values[index] < values[minimum]) {
                    minimum = index;
                }
            }

            if (minimum != start) {
                int value = values[start];
                values[start] = values[minimum];
                values[minimum] = value;
                recorder.swap(values, start, minimum, sorted);
            }
            sorted[start] = true;
            recorder.markSorted(values, sorted);
        }

        recorder.complete(values, sorted);
        return recorder.steps();
    }
}
