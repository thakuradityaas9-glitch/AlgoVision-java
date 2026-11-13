package com.algovision.algorithms;

import java.util.List;

import com.algovision.model.SortStep;

public interface SortingAlgorithm {
    String name();

    String description();

    String bestCaseTimeComplexity();

    String averageCaseTimeComplexity();

    String worstCaseTimeComplexity();

    String auxiliarySpaceComplexity();

    List<SortStep> generateSteps(int[] input);
}
