package com.algovision.ai;

import java.util.List;

public record AnalysisResponse(
        ComplexityAnalysis complexityAnalysis,
        BugDetection bugDetection,
        CodeReview codeReview,
        Optimization optimization,
        List<InterviewQuestion> interviewQuestions,
        CodeExplanation codeExplanation) {

    public record ComplexityAnalysis(
            String timeComplexity,
            String spaceComplexity,
            String bestCase,
            String averageCase,
            String worstCase,
            String explanation) {
    }

    public record BugDetection(
            String summary,
            List<Finding> logicalErrors,
            List<Finding> potentialBugs,
            List<Finding> edgeCases) {
    }

    public record Finding(String reference, String description, String severity) {
    }

    public record CodeReview(
            String summary,
            List<String> inefficientOperations,
            List<String> unnecessaryComputations,
            List<String> implementationChoices) {
    }

    public record Optimization(
            List<String> suggestions,
            String optimizedCode,
            String beforeAfterComplexity) {
    }

    public record InterviewQuestion(String question, String answer, String concept) {
    }

    public record CodeExplanation(
            String approach,
            List<ExplanationSection> importantSections,
            List<String> algorithmSteps) {
    }

    public record ExplanationSection(String section, String explanation) {
    }
}
