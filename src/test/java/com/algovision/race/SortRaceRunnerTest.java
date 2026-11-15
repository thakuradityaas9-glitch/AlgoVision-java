package com.algovision.race;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.algovision.algorithms.BubbleSort;
import com.algovision.algorithms.SelectionSort;
import com.algovision.algorithms.SortingAlgorithm;
import com.algovision.model.SortOperation;
import com.algovision.model.SortStep;
import org.junit.jupiter.api.Test;

class SortRaceRunnerTest {
    private final SortRaceRunner runner = new SortRaceRunner();

    @Test
    void startsBothAlgorithmRunsConcurrently() throws Exception {
        CountDownLatch bothStarted = new CountDownLatch(2);
        SortingAlgorithm first = rendezvousAlgorithm("First", bothStarted);
        SortingAlgorithm second = rendezvousAlgorithm("Second", bothStarted);

        SortRacePair pair = org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(
                Duration.ofSeconds(4),
                () -> runner.runConcurrently(first, second, new int[] {4, 2, 3}));

        assertEquals(0, bothStarted.getCount());
        assertEquals("First", pair.first().algorithm().name());
        assertEquals("Second", pair.second().algorithm().name());
    }

    @Test
    void preservesInputAndCountsEachAlgorithmsOperationsIndependently() throws Exception {
        int[] input = {3, 2, 1};
        int[] original = input.clone();

        SortRacePair pair = runner.runConcurrently(new BubbleSort(), new SelectionSort(), input);

        assertArrayEquals(original, input);
        assertArrayEquals(new int[] {1, 2, 3},
                pair.first().steps().getLast().arrayState());
        assertArrayEquals(new int[] {1, 2, 3},
                pair.second().steps().getLast().arrayState());
        assertOperationCounts(pair.first());
        assertOperationCounts(pair.second());
        assertEquals(pair.first().comparisons() + pair.first().swaps()
                + pair.first().movements(), pair.first().operations());
        assertEquals(pair.second().comparisons() + pair.second().swaps()
                + pair.second().movements(), pair.second().operations());
        assertFalse(pair.first().steps().isEmpty());
        assertFalse(pair.second().steps().isEmpty());
    }

    @Test
    void determinesWinnerAndRepresentsTiesExplicitly() {
        List<SortStep> steps = new BubbleSort().generateSteps(new int[] {2, 1});
        SortRaceResult faster = new SortRaceResult(new BubbleSort(), steps, 10);
        SortRaceResult slower = new SortRaceResult(new SelectionSort(), steps, 20);
        SortRaceResult tied = new SortRaceResult(new SelectionSort(), steps, 10);

        SortRacePair win = new SortRacePair(faster, slower);
        assertFalse(win.isTie());
        assertEquals(faster, win.winner().orElseThrow());

        SortRacePair tie = new SortRacePair(faster, tied);
        assertTrue(tie.isTie());
        assertTrue(tie.winner().isEmpty());
    }

    private SortingAlgorithm rendezvousAlgorithm(String name, CountDownLatch bothStarted) {
        return algorithm(name, input -> {
            bothStarted.countDown();
            try {
                if (!bothStarted.await(2, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Race algorithms did not start concurrently.");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Race algorithm was interrupted.", exception);
            }
            return new BubbleSort().generateSteps(input);
        });
    }

    private SortingAlgorithm algorithm(String name, Function<int[], List<SortStep>> generator) {
        return new SortingAlgorithm() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public String description() {
                return "Test algorithm";
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
                return generator.apply(input);
            }
        };
    }

    private void assertOperationCounts(SortRaceResult result) {
        assertEquals(result.steps().stream()
                .filter(step -> step.operation() == SortOperation.COMPARE).count(),
                result.comparisons());
        assertEquals(result.steps().stream()
                .filter(step -> step.operation() == SortOperation.SWAP).count(),
                result.swaps());
        assertEquals(result.steps().stream()
                .filter(step -> step.operation() == SortOperation.MOVE
                        || step.operation() == SortOperation.OVERWRITE).count(),
                result.movements());
    }
}
