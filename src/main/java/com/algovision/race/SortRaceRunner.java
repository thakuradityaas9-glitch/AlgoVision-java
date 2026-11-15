package com.algovision.race;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

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

    public SortRacePair runConcurrently(
            SortingAlgorithm firstAlgorithm,
            SortingAlgorithm secondAlgorithm,
            int[] input) throws InterruptedException, ExecutionException {
        Objects.requireNonNull(firstAlgorithm, "firstAlgorithm");
        Objects.requireNonNull(secondAlgorithm, "secondAlgorithm");
        Objects.requireNonNull(input, "input");

        AtomicInteger threadNumber = new AtomicInteger(1);
        ThreadFactory threadFactory = task -> {
            Thread thread = new Thread(task,
                    "algovision-sort-race-" + threadNumber.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
        ExecutorService executor = Executors.newFixedThreadPool(2, threadFactory);
        Future<SortRaceResult> first = executor.submit(
                () -> run(firstAlgorithm, input.clone()));
        Future<SortRaceResult> second = executor.submit(
                () -> run(secondAlgorithm, input.clone()));
        try {
            return new SortRacePair(first.get(), second.get());
        } catch (InterruptedException | ExecutionException exception) {
            first.cancel(true);
            second.cancel(true);
            throw exception;
        } finally {
            executor.shutdownNow();
        }
    }
}
