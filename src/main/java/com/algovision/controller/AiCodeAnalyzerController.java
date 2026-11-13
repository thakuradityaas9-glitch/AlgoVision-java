package com.algovision.controller;

import java.util.ArrayList;
import java.util.List;

import com.algovision.ai.AnalysisResponse;
import com.algovision.ai.GroqService;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

public final class AiCodeAnalyzerController {
    private static final String CPP_SAMPLE = """
            #include <iostream>
            #include <vector>
            using namespace std;

            int binarySearch(const vector<int>& values, int target) {
                int left = 0, right = static_cast<int>(values.size()) - 1;
                while (left <= right) {
                    int mid = left + (right - left) / 2;
                    if (values[mid] == target) return mid;
                    if (values[mid] < target) left = mid + 1;
                    else right = mid - 1;
                }
                return -1;
            }
            """;
    private static final String JAVA_SAMPLE = """
            public class BinarySearch {
                public static int search(int[] values, int target) {
                    int left = 0, right = values.length - 1;
                    while (left <= right) {
                        int mid = left + (right - left) / 2;
                        if (values[mid] == target) return mid;
                        if (values[mid] < target) left = mid + 1;
                        else right = mid - 1;
                    }
                    return -1;
                }
            }
            """;

    private final GroqService groqService = new GroqService();
    private final ComboBox<String> languageChoice = new ComboBox<>();
    private final ComboBox<String> categoryChoice = new ComboBox<>();
    private final TextArea codeEditor = new TextArea();
    private final Button analyzeButton = new Button("Analyze with Groq");
    private final Button clearButton = new Button("Clear");
    private final Button sampleButton = new Button("Load sample");
    private final ProgressIndicator loadingIndicator = new ProgressIndicator();
    private final Label statusLabel = new Label();
    private final VBox resultSections = new VBox(14);
    private final ScrollPane view;

    public AiCodeAnalyzerController() {
        view = createView();
    }

    public Node getView() {
        return view;
    }

    private ScrollPane createView() {
        VBox content = new VBox(18);
        content.getStyleClass().add("analyzer-content");
        content.setPadding(new Insets(30, 36, 30, 36));

        VBox introduction = new VBox(7);
        Label eyebrow = new Label("AI-POWERED CODE REVIEW");
        eyebrow.getStyleClass().add("eyebrow");
        Label heading = new Label("AI Code Analyzer");
        heading.getStyleClass().add("welcome-heading");
        Label subtitle = new Label(
                "Get complexity insights, bug checks, and interview prep for your DSA solutions.");
        subtitle.getStyleClass().add("welcome-subtitle");
        subtitle.setWrapText(true);
        introduction.getChildren().addAll(eyebrow, heading, subtitle);

        VBox editorPanel = new VBox(13);
        editorPanel.getStyleClass().add("analyzer-panel");
        editorPanel.setPadding(new Insets(16));
        languageChoice.getItems().setAll("C++", "Java");
        languageChoice.getSelectionModel().selectFirst();
        categoryChoice.getItems().setAll(
                "Arrays", "Strings", "Linked Lists", "Stacks and Queues", "Trees",
                "Graphs", "Sorting", "Searching", "Dynamic Programming", "Greedy",
                "Backtracking", "Other");
        categoryChoice.getSelectionModel().selectFirst();
        languageChoice.setMaxWidth(Double.MAX_VALUE);
        categoryChoice.setMaxWidth(Double.MAX_VALUE);
        HBox selectors = new HBox(12,
                labeledControl("LANGUAGE", languageChoice),
                labeledControl("DSA CATEGORY", categoryChoice));
        HBox.setHgrow(selectors.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(selectors.getChildren().get(1), Priority.ALWAYS);

        codeEditor.setPromptText("Paste your C++ or Java solution here...");
        codeEditor.setWrapText(false);
        codeEditor.setPrefRowCount(15);
        codeEditor.getStyleClass().add("analyzer-code-editor");
        HBox.setHgrow(codeEditor, Priority.ALWAYS);

        analyzeButton.getStyleClass().add("primary-button");
        clearButton.getStyleClass().add("secondary-button");
        sampleButton.getStyleClass().add("secondary-button");
        analyzeButton.setOnAction(event -> analyze());
        clearButton.setOnAction(event -> clear());
        sampleButton.setOnAction(event -> loadSample());
        loadingIndicator.setPrefSize(19, 19);
        loadingIndicator.setMaxSize(19, 19);
        loadingIndicator.setVisible(false);
        loadingIndicator.setManaged(false);
        statusLabel.getStyleClass().add("analyzer-status");
        statusLabel.setWrapText(true);
        HBox actions = new HBox(10,
                analyzeButton, clearButton, sampleButton, loadingIndicator);
        actions.setAlignment(Pos.CENTER_LEFT);
        editorPanel.getChildren().addAll(selectors, codeEditor, actions, statusLabel);

        resultSections.setFillWidth(true);
        content.getChildren().addAll(introduction, editorPanel, resultSections);
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("content-scroll");
        return scrollPane;
    }

    private VBox labeledControl(String title, ComboBox<String> control) {
        VBox wrapper = new VBox(6);
        Label label = new Label(title);
        label.getStyleClass().add("control-label");
        wrapper.getChildren().addAll(label, control);
        return wrapper;
    }

    private void analyze() {
        String code = codeEditor.getText();
        if (code == null || code.isBlank()) {
            showError("Add some code before starting the analysis.");
            codeEditor.requestFocus();
            return;
        }
        setLoading(true);
        statusLabel.setText("Sending your code to Groq...");
        statusLabel.getStyleClass().remove("error-message");
        resultSections.getChildren().clear();
        groqService.analyze(code, languageChoice.getValue(), categoryChoice.getValue())
                .whenComplete((analysis, error) -> Platform.runLater(() -> {
                    setLoading(false);
                    if (error != null) {
                        showError(rootCause(error).getMessage());
                        return;
                    }
                    statusLabel.setText("Analysis complete.");
                    renderAnalysis(analysis);
                }));
    }

    private Throwable rootCause(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null
                && (cause instanceof java.util.concurrent.CompletionException
                || cause instanceof java.util.concurrent.ExecutionException)) {
            cause = cause.getCause();
        }
        return cause;
    }

    private void setLoading(boolean loading) {
        analyzeButton.setDisable(loading);
        clearButton.setDisable(loading);
        sampleButton.setDisable(loading);
        languageChoice.setDisable(loading);
        categoryChoice.setDisable(loading);
        codeEditor.setDisable(loading);
        loadingIndicator.setManaged(loading);
        loadingIndicator.setVisible(loading);
    }

    private void showError(String message) {
        statusLabel.setText(message == null || message.isBlank()
                ? "Groq analysis failed. Please try again." : message);
        if (!statusLabel.getStyleClass().contains("error-message")) {
            statusLabel.getStyleClass().add("error-message");
        }
    }

    private void clear() {
        codeEditor.clear();
        resultSections.getChildren().clear();
        statusLabel.setText("");
        statusLabel.getStyleClass().remove("error-message");
    }

    private void loadSample() {
        codeEditor.setText("Java".equals(languageChoice.getValue()) ? JAVA_SAMPLE : CPP_SAMPLE);
        statusLabel.setText("");
        statusLabel.getStyleClass().remove("error-message");
    }

    private void renderAnalysis(AnalysisResponse result) {
        resultSections.getChildren().clear();
        if (result == null) {
            showError("Groq returned an empty analysis. Please try again.");
            return;
        }
        AnalysisResponse.ComplexityAnalysis complexity = result.complexityAnalysis();
        if (complexity != null) {
            resultSections.getChildren().add(section("1. Complexity Analysis",
                    "Time: " + text(complexity.timeComplexity()) + "\n"
                            + "Space: " + text(complexity.spaceComplexity()) + "\n"
                            + "Best case: " + text(complexity.bestCase()) + "\n"
                            + "Average case: " + text(complexity.averageCase()) + "\n"
                            + "Worst case: " + text(complexity.worstCase()) + "\n\n"
                            + text(complexity.explanation())));
        }
        AnalysisResponse.BugDetection bugs = result.bugDetection();
        if (bugs != null) {
            List<String> findings = new ArrayList<>();
            findings.add(text(bugs.summary()));
            addFindings(findings, "Logical errors", bugs.logicalErrors());
            addFindings(findings, "Potential bugs", bugs.potentialBugs());
            addFindings(findings, "Edge cases", bugs.edgeCases());
            resultSections.getChildren().add(section("2. Bug Detection", String.join("\n\n", findings)));
        }
        AnalysisResponse.CodeReview review = result.codeReview();
        if (review != null) {
            resultSections.getChildren().add(section("3. Code Review",
                    text(review.summary()) + "\n\nInefficient operations:\n"
                            + bullets(review.inefficientOperations())
                            + "\n\nUnnecessary computations:\n" + bullets(review.unnecessaryComputations())
                            + "\n\nImplementation choices:\n" + bullets(review.implementationChoices())));
        }
        AnalysisResponse.Optimization optimization = result.optimization();
        if (optimization != null) {
            resultSections.getChildren().add(section("4. Optimization",
                    "Suggestions:\n" + bullets(optimization.suggestions())
                            + "\n\nBefore / after complexity:\n" + text(optimization.beforeAfterComplexity())));
            resultSections.getChildren().add(codeSection(text(optimization.optimizedCode())));
        }
        resultSections.getChildren().add(section("6. Interview Questions",
                interviewQuestions(result.interviewQuestions())));
        AnalysisResponse.CodeExplanation explanation = result.codeExplanation();
        if (explanation != null) {
            StringBuilder details = new StringBuilder("Approach:\n")
                    .append(text(explanation.approach()))
                    .append("\n\nImportant sections:\n");
            if (explanation.importantSections() != null) {
                explanation.importantSections().forEach(section -> details
                        .append("- ").append(text(section.section())).append(": ")
                        .append(text(section.explanation())).append('\n'));
            }
            details.append("\nAlgorithm steps:\n").append(bullets(explanation.algorithmSteps()));
            resultSections.getChildren().add(section("7. Code Explanation", details.toString()));
        }
    }

    private void addFindings(List<String> output, String title, List<AnalysisResponse.Finding> findings) {
        output.add(title + ":\n" + (findings == null || findings.isEmpty()
                ? "None reported." : findings.stream()
                .map(finding -> "- [" + text(finding.severity()) + "] "
                        + text(finding.reference()) + ": " + text(finding.description()))
                .reduce((first, second) -> first + "\n" + second).orElse("None reported.")));
    }

    private String interviewQuestions(List<AnalysisResponse.InterviewQuestion> questions) {
        if (questions == null || questions.isEmpty()) {
            return "No interview questions were returned.";
        }
        return questions.stream().map(question -> "Q: " + text(question.question())
                        + "\nAnswer: " + text(question.answer())
                        + "\nConcept: " + text(question.concept()))
                .reduce((first, second) -> first + "\n\n" + second).orElse("");
    }

    private VBox section(String title, String body) {
        VBox panel = new VBox(9);
        panel.getStyleClass().add("analyzer-result-panel");
        panel.setPadding(new Insets(15));
        Label heading = new Label(title);
        heading.getStyleClass().add("panel-heading");
        Label content = new Label(body);
        content.getStyleClass().add("analyzer-result-text");
        content.setWrapText(true);
        panel.getChildren().addAll(heading, content);
        return panel;
    }

    private VBox codeSection(String code) {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("analyzer-result-panel");
        panel.setPadding(new Insets(15));
        HBox header = new HBox(10);
        Label heading = new Label("5. Optimized Code");
        heading.getStyleClass().add("panel-heading");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button copyButton = new Button("Copy code");
        copyButton.getStyleClass().add("secondary-button");
        copyButton.setOnAction(event -> {
            ClipboardContent clipboardContent = new ClipboardContent();
            clipboardContent.putString(code);
            Clipboard.getSystemClipboard().setContent(clipboardContent);
            statusLabel.setText("Optimized code copied to clipboard.");
        });
        header.getChildren().addAll(heading, spacer, copyButton);
        TextArea optimizedCode = new TextArea(code);
        optimizedCode.setEditable(false);
        optimizedCode.setWrapText(false);
        optimizedCode.setPrefRowCount(12);
        optimizedCode.getStyleClass().add("analyzer-code-editor");
        panel.getChildren().addAll(header, optimizedCode);
        return panel;
    }

    private String bullets(List<String> values) {
        return values == null || values.isEmpty()
                ? "None reported." : values.stream().map(value -> "- " + value)
                .reduce((first, second) -> first + "\n" + second).orElse("None reported.");
    }

    private String text(String value) {
        return value == null || value.isBlank() ? "Not provided." : value;
    }
}
