package com.algovision.race;

import java.util.Objects;
import java.util.Optional;

public record SortRacePair(SortRaceResult first, SortRaceResult second) {
    public SortRacePair {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
    }

    public boolean isTie() {
        return first.executionTimeNanos() == second.executionTimeNanos();
    }

    public Optional<SortRaceResult> winner() {
        if (isTie()) {
            return Optional.empty();
        }
        return Optional.of(first.executionTimeNanos() < second.executionTimeNanos()
                ? first
                : second);
    }
}
