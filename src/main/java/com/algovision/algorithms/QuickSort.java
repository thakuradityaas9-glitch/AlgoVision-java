package com.algovision.algorithms;

import java.util.List;

import com.algovision.model.SortStep;

public final class QuickSort implements SortingAlgorithm {
    @Override
    public String name() {
        return "Quick Sort";
    }

    @Override
    public String description() {
        return "Partition values around a pivot, then sort each side recursively.";
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
        return "O(n^2)";
    }

    @Override
    public String auxiliarySpaceComplexity() {
        return "O(log n) avg, O(n) worst";
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

    private void sort(int[] values, int low, int high, SortStepRecorder recorder, boolean[] sorted) {
        if (low >= high) {
            if (low == high) {
                sorted[low] = true;
                recorder.markSorted(values, sorted);
            }
            return;
        }

        int pivotIndex = partition(values, low, high, recorder, sorted);
        sorted[pivotIndex] = true;
        recorder.markSorted(values, sorted);
        sort(values, low, pivotIndex - 1, recorder, sorted);
        sort(values, pivotIndex + 1, high, recorder, sorted);
    }

    private int partition(int[] values, int low, int high,
            SortStepRecorder recorder, boolean[] sorted) {
        int pivot = values[high];
        recorder.pivot(values, high, sorted);
        int boundary = low;
        for (int index = low; index < high; index++) {
            recorder.comparison(values, index, high, sorted);
            if (values[index] <= pivot) {
                if (boundary != index) {
                    swap(values, boundary, index);
                    recorder.swap(values, boundary, index, sorted);
                }
                boundary++;
            }
        }

        if (boundary != high) {
            swap(values, boundary, high);
            recorder.swap(values, boundary, high, sorted);
        }
        return boundary;
    }

    private void swap(int[] values, int left, int right) {
        int value = values[left];
        values[left] = values[right];
        values[right] = value;
    }
}
