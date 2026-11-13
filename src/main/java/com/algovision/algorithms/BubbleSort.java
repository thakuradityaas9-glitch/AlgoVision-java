package com.algovision.algorithms;

import java.util.List;
import java.util.Arrays;

import com.algovision.model.SortStep;

public final class BubbleSort implements SortingAlgorithm {
    @Override
    public String name() {
        return "Bubble Sort";
    }

    @Override
    public String description() {
        return "Compare adjacent values and move larger values toward the end.";
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

        for (int end = values.length - 1; end > 0; end--) {
            boolean swappedInPass = false;
            for (int index = 0; index < end; index++) {
                recorder.comparison(values, index, index + 1, sorted);
                if (values[index] > values[index + 1]) {
                    int value = values[index];
                    values[index] = values[index + 1];
                    values[index + 1] = value;
                    recorder.swap(values, index, index + 1, sorted);
                    swappedInPass = true;
                }
            }

            sorted[end] = true;
            if (!swappedInPass) {
                Arrays.fill(sorted, 0, end, true);
            }
            recorder.markSorted(values, sorted);
            if (!swappedInPass) {
                break;
            }
        }

        recorder.complete(values, sorted);
        return recorder.steps();
    }
}
