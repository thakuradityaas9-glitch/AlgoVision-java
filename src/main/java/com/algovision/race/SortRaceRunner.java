package com.algovision.race;

import java.util.Objects;

import com.algovision.algorithms.SortingAlgorithm;

public final class SortRaceRunner {
    public SortRaceResult run(SortingAlgorithm algorithm, int[] input) {
        Objects.requireNonNull(algorithm, "algorithm");
        Objects.requireNonNull(input, "input");

        int[] algorithmInput = input.clone();
        long startTime = System.nanoTime();
        var steps = algorithm.generateSteps(algorithmInput);
        long elapsedNanos = System.nanoTime() - startTime;
        return new SortRaceResult(algorithm, steps, elapsedNanos);
    }
}
