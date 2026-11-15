package com.algovision.algorithms;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.algovision.model.SortOperation;
import com.algovision.model.SortStep;
import org.junit.jupiter.api.Test;

class SortingAlgorithmTest {
    private static final List<SortingAlgorithm> ALGORITHMS = List.of(
            new BubbleSort(),
            new SelectionSort(),
            new InsertionSort(),
            new MergeSort(),
            new QuickSort());
    private static final int[][] INPUTS = {
            {},
            {7},
            {3, 1},
            {-8, -2, 0, 4, 17},
            {9, 7, 5, 3, 1},
            {5, 2, 5, -1, 2, -1},
            {Integer.MAX_VALUE, Integer.MIN_VALUE, 0, Integer.MAX_VALUE}
    };

    @Test
    void everyAlgorithmSortsEdgeCasesAndPreservesItsInput() {
        for (SortingAlgorithm algorithm : ALGORITHMS) {
            for (int[] input : INPUTS) {
                int[] original = input.clone();
                List<SortStep> steps = algorithm.generateSteps(input);
                int[] expected = input.clone();
                Arrays.sort(expected);

                assertArrayEquals(original, input, algorithm.name() + " mutated its input");
                assertFalse(steps.isEmpty(), algorithm.name() + " should record completion");
                assertArrayEquals(expected, steps.getLast().arrayState(),
                        algorithm.name() + " failed for " + Arrays.toString(input));
                assertEquals(SortOperation.COMPLETE, steps.getLast().operation());
                assertStepTransitions(algorithm, input, steps);
            }
        }
    }

    @Test
    void visualStepsIncludeCompareMoveOverwriteAndPivotEvents() {
        int[] reverse = {5, 4, 3, 2, 1};

        assertTrue(hasOperation(new BubbleSort(), reverse, SortOperation.COMPARE));
        assertTrue(hasOperation(new BubbleSort(), reverse, SortOperation.SWAP));
        assertTrue(hasOperation(new InsertionSort(), reverse, SortOperation.MOVE));
        assertTrue(hasOperation(new InsertionSort(), reverse, SortOperation.OVERWRITE));
        assertTrue(hasOperation(new MergeSort(), reverse, SortOperation.OVERWRITE));
        assertTrue(hasOperation(new QuickSort(), reverse, SortOperation.PIVOT));
    }

    @Test
    void everyAlgorithmSortsRandomArrays() {
        Random random = new Random(20261115);
        for (SortingAlgorithm algorithm : ALGORITHMS) {
            for (int sample = 0; sample < 25; sample++) {
                int[] input = random.ints(random.nextInt(1, 21), -100, 101).toArray();
                int[] expected = input.clone();
                Arrays.sort(expected);

                List<SortStep> steps = algorithm.generateSteps(input);
                assertArrayEquals(expected, steps.getLast().arrayState(),
                        algorithm.name() + " failed on random input " + Arrays.toString(input));
                assertStepTransitions(algorithm, input, steps);
            }
        }
    }

    private void assertStepTransitions(
            SortingAlgorithm algorithm, int[] input, List<SortStep> steps) {
        int[] previous = input.clone();
        for (SortStep step : steps) {
            int[] current = step.arrayState();
            assertEquals(input.length, current.length, algorithm.name() + " changed array length");
            assertEquals(input.length, step.sortedElements().length,
                    algorithm.name() + " recorded mismatched sorted flags");
            switch (step.operation()) {
                case SWAP -> assertSwapTransition(previous, current, step, algorithm);
                case MOVE -> assertSingleTargetTransition(previous, current, step, algorithm);
                case OVERWRITE -> assertSingleTargetTransition(previous, current, step, algorithm);
                case COMPARE, PIVOT, MARK_SORTED, COMPLETE ->
                        assertArrayEquals(previous, current,
                                algorithm.name() + " changed values during " + step.operation());
            }
            previous = current;
        }
    }

    private void assertSwapTransition(int[] previous, int[] current, SortStep step,
            SortingAlgorithm algorithm) {
        int left = step.comparedIndexLeft();
        int right = step.comparedIndexRight();
        assertTrue(left >= 0 && left < previous.length, algorithm.name() + " has invalid swap index");
        assertTrue(right >= 0 && right < previous.length, algorithm.name() + " has invalid swap index");
        assertEquals(previous[left], current[right], algorithm.name() + " has invalid swap result");
        assertEquals(previous[right], current[left], algorithm.name() + " has invalid swap result");
        for (int index = 0; index < previous.length; index++) {
            if (index != left && index != right) {
                assertEquals(previous[index], current[index],
                        algorithm.name() + " changed an unrelated value during swap");
            }
        }
    }

    private void assertSingleTargetTransition(int[] previous, int[] current, SortStep step,
            SortingAlgorithm algorithm) {
        int target = step.comparedIndexRight();
        assertTrue(target >= 0 && target < previous.length,
                algorithm.name() + " has an invalid write target");
        if (step.operation() == SortOperation.MOVE) {
            assertEquals(previous[step.comparedIndexLeft()], current[target],
                    algorithm.name() + " recorded an invalid move");
        }
        for (int index = 0; index < previous.length; index++) {
            if (index != target) {
                assertEquals(previous[index], current[index],
                        algorithm.name() + " changed an unrelated value during " + step.operation());
            }
        }
    }

    private boolean hasOperation(SortingAlgorithm algorithm, int[] input, SortOperation operation) {
        return algorithm.generateSteps(input).stream().anyMatch(step -> step.operation() == operation);
    }
}
