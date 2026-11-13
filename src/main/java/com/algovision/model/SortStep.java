package com.algovision.model;

import java.util.Arrays;

public record SortStep(
        int[] arrayState,
        int comparedIndexLeft,
        int comparedIndexRight,
        boolean swapOccurred,
        int comparisons,
        int swaps,
        int sortedCount,
        SortOperation operation,
        int pivotIndex,
        boolean[] sortedElements) {

    public SortStep {
        arrayState = arrayState.clone();
        sortedElements = sortedElements.clone();
    }

    public SortStep(int[] arrayState, int comparedIndexLeft, int comparedIndexRight,
            boolean swapOccurred, int comparisons, int swaps, int sortedCount) {
        this(arrayState, comparedIndexLeft, comparedIndexRight, swapOccurred,
                comparisons, swaps, sortedCount,
                swapOccurred ? SortOperation.SWAP : SortOperation.COMPARE,
                -1, suffixFlags(arrayState.length, sortedCount));
    }

    @Override
    public int[] arrayState() {
        return arrayState.clone();
    }

    @Override
    public boolean[] sortedElements() {
        return sortedElements.clone();
    }

    public boolean isSorted(int index) {
        return sortedElements[index];
    }

    private static boolean[] suffixFlags(int size, int sortedCount) {
        boolean[] flags = new boolean[size];
        Arrays.fill(flags, Math.max(0, size - sortedCount), size, true);
        return flags;
    }
}
