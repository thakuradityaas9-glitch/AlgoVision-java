package com.algovision.algorithms;

import java.util.Arrays;
import java.util.List;

import com.algovision.model.SortStep;

public final class MergeSort implements SortingAlgorithm {
    @Override
    public String name() {
        return "Merge Sort";
    }

    @Override
    public String description() {
        return "Split the array into halves, then merge the ordered portions.";
    }

    @Override
    public String bestCaseTimeComplexity() {
        return "O(n log n)";
    }

    @Override
    public String averageCaseTimeComplexity() {
        return "O(n log n)";
    }

    @Override
    public String worstCaseTimeComplexity() {
        return "O(n log n)";
    }

    @Override
    public String auxiliarySpaceComplexity() {
        return "O(n)";
    }

    @Override
    public List<SortStep> generateSteps(int[] input) {
        int[] values = input.clone();
        boolean[] sorted = new boolean[values.length];
        SortStepRecorder recorder = new SortStepRecorder();
        sort(values, 0, values.length - 1, recorder, sorted);
        recorder.complete(values, sorted);
        return recorder.steps();
    }

    private void sort(int[] values, int left, int right, SortStepRecorder recorder, boolean[] sorted) {
        if (left >= right) {
            return;
        }
        int middle = left + (right - left) / 2;
        sort(values, left, middle, recorder, sorted);
        sort(values, middle + 1, right, recorder, sorted);
        merge(values, left, middle, right, recorder, sorted);
    }

    private void merge(int[] values, int left, int middle, int right,
            SortStepRecorder recorder, boolean[] sorted) {
        int[] leftHalf = Arrays.copyOfRange(values, left, middle + 1);
        int[] rightHalf = Arrays.copyOfRange(values, middle + 1, right + 1);
        int leftIndex = 0;
        int rightIndex = 0;
        int target = left;

        while (leftIndex < leftHalf.length && rightIndex < rightHalf.length) {
            int originalLeftIndex = left + leftIndex;
            int originalRightIndex = middle + 1 + rightIndex;
            recorder.comparison(values, originalLeftIndex, originalRightIndex, sorted);
            if (leftHalf[leftIndex] <= rightHalf[rightIndex]) {
                values[target] = leftHalf[leftIndex++];
                recorder.overwrite(values, originalLeftIndex, target++, sorted);
            } else {
                values[target] = rightHalf[rightIndex++];
                recorder.overwrite(values, originalRightIndex, target++, sorted);
            }
        }

        while (leftIndex < leftHalf.length) {
            int source = left + leftIndex;
            values[target] = leftHalf[leftIndex++];
            recorder.overwrite(values, source, target++, sorted);
        }
        while (rightIndex < rightHalf.length) {
            int source = middle + 1 + rightIndex;
            values[target] = rightHalf[rightIndex++];
            recorder.overwrite(values, source, target++, sorted);
        }
    }
}
