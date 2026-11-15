package com.algovision.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.border.EmptyBorder;

import com.algovision.ai.AnalysisResponse;

public final class AiAnalysisDashboard extends JPanel {
    private static final System.Logger LOGGER =
            System.getLogger(AiAnalysisDashboard.class.getName());
    private static final Pattern COMPLEXITY_TOKEN =
            Pattern.compile("(?i)O\\s*\\([^)]*\\)");
    private static final Pattern REPORTED_LINE =
            Pattern.compile("(?i)\\b(?:line|l)\\s*[:#]?\\s*(\\d+)\\b");
    private static final Color SURFACE = AlgoVisionTheme.CARD_BACKGROUND;
    private static final Color BORDER = AlgoVisionTheme.BORDER;
    private static final Color BODY = AlgoVisionTheme.TEXT_BODY;
    private static final Color MUTED = AlgoVisionTheme.TEXT_MUTED;
    private static final Color TEXT = AlgoVisionTheme.TEXT_PRIMARY;

    private final JTabbedPane tabs = new JTabbedPane();
    private final JPanel overview = contentPanel();
    private final JPanel complexity = contentPanel();
    private final JPanel bugs = contentPanel();
    private final JPanel explanation = contentPanel();
    private final JPanel optimization = contentPanel();
    private final JPanel interview = contentPanel();
    private final JButton copyButton = new JButton("Copy optimized code");
    private AnalysisResponse response;
    private String optimizedCode = "";
    private Set<Integer> problemLines = Set.of();
    private Consumer<String> copyAction = ignored -> { };

    public AiAnalysisDashboard() {
        setName("analysisDashboard");
        setLayout(new BorderLayout());
        setBackground(AlgoVisionTheme.PANEL_BACKGROUND);
        tabs.setName("analysisTabs");
        tabs.setFont(AlgoVisionTheme.font(Font.PLAIN, 11));
        tabs.setBackground(AlgoVisionTheme.PANEL_BACKGROUND);
        tabs.setForeground(TEXT);
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        addTab("Overview", overview);
        addTab("Complexity", complexity);
        addTab("Bugs & Edge Cases", bugs);
        addTab("Code Explanation", explanation);
        addTab("Optimization", optimization);
        addTab("Interview Questions", interview);
        add(tabs, BorderLayout.CENTER);
        AlgoVisionTheme.styleButton(copyButton, false);
        copyButton.addActionListener(event -> copyAction.accept(optimizedCode));
        clear();
    }

    public void setCopyAction(Consumer<String> action) {
        copyAction = action == null ? ignored -> { } : action;
    }

    public void setCopyEnabled(boolean enabled) {
        copyButton.setEnabled(enabled && !optimizedCode.isBlank());
    }

    public String optimizedCode() {
        return optimizedCode;
    }

    public Set<Integer> problemLines() {
        return problemLines;
    }

    public void clear() {
        response = null;
        optimizedCode = "";
        problemLines = Set.of();
        copyButton.setEnabled(false);
        showPlaceholder(overview, "Run an analysis to see the key findings.");
        showPlaceholder(complexity, "Complexity metrics will appear after analysis.");
        showPlaceholder(bugs, "Bug and edge-case findings will appear after analysis.");
        showPlaceholder(explanation, "The model's explanation steps will appear after analysis.");
        showPlaceholder(optimization, "Code comparison will appear after analysis.");
        showPlaceholder(interview, "Interview questions will appear after analysis.");
    }

    public void render(AnalysisResponse result, String originalSource, String language) {
        response = result;
        AnalysisResponse.Optimization optimizationResult = result.optimization();
        optimizedCode = optimizationResult == null || optimizationResult.optimizedCode() == null
                ? "" : optimizationResult.optimizedCode();
        problemLines = collectProblemLines(result.bugDetection());
        renderOverview(result);
        renderComplexity(result.complexityAnalysis());
        renderBugs(result.bugDetection());
        renderExplanation(result.codeExplanation());
        renderOptimization(result, originalSource, language);
        renderInterview(result.interviewQuestions());
        copyButton.setEnabled(!optimizedCode.isBlank());
        revalidate();
        repaint();
        LOGGER.log(System.Logger.Level.INFO,
                "Analysis dashboard rendered {0} tabs; attached={1}.",
                tabs.getTabCount(), tabs.getParent() == this);
    }

    public String contentForTesting(String section) {
        if (response == null) {
            return "";
        }
        return switch (section) {
            case "complexity" -> String.valueOf(response.complexityAnalysis());
            case "bugs" -> String.valueOf(response.bugDetection());
            case "review" -> String.valueOf(response.codeReview());
            case "optimization" -> String.valueOf(response.optimization());
            case "interview" -> String.valueOf(response.interviewQuestions());
            case "explanation" -> String.valueOf(response.codeExplanation());
            default -> "";
        };
    }

    public boolean copyEnabledForTesting() {
        return copyButton.isEnabled();
    }

    public int tabCountForTesting() {
        return tabs.getTabCount();
    }

    public String tabTitleForTesting(int index) {
        return tabs.getTitleAt(index);
    }

    private void addTab(String title, JPanel content) {
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setBackground(AlgoVisionTheme.PANEL_BACKGROUND);
        scroll.getViewport().setBackground(AlgoVisionTheme.PANEL_BACKGROUND);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        tabs.addTab(title, scroll);
    }

    private JPanel contentPanel() {
        JPanel content = new JPanel();
        content.setName("analysis-content");
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(AlgoVisionTheme.PANEL_BACKGROUND);
        content.setBorder(new EmptyBorder(14, 14, 14, 14));
        return content;
    }

    private void showPlaceholder(JPanel target, String message) {
        target.removeAll();
        target.add(card("Analysis", message, AlgoVisionTheme.ACCENT));
        target.add(Box.createVerticalGlue());
        target.revalidate();
        target.repaint();
    }

    private void renderOverview(AnalysisResponse result) {
        overview.removeAll();
        addSectionTitle(overview, "Analysis overview",
                "A concise view of the response returned by Groq.");
        AnalysisResponse.ComplexityAnalysis metrics = result.complexityAnalysis();
        JPanel metricCards = new JPanel(new java.awt.GridLayout(1, 2, 10, 0));
        metricCards.setOpaque(false);
        metricCards.add(new ComplexityIndicator("TIME COMPLEXITY",
                metrics == null ? null : metrics.timeComplexity()));
        metricCards.add(new ComplexityIndicator("SPACE COMPLEXITY",
                metrics == null ? null : metrics.spaceComplexity()));
        metricCards.setAlignmentX(Component.LEFT_ALIGNMENT);
        overview.add(metricCards);
        overview.add(Box.createVerticalStrut(10));

        AnalysisResponse.BugDetection detection = result.bugDetection();
        if (detection != null) {
            int findingCount = findingCount(detection);
            long highSeverityCount = allFindings(detection).stream()
                    .filter(finding -> isHighSeverity(finding.severity())).count();
            overview.add(card("Correctness signals",
                    findingCount + " finding(s) returned; " + highSeverityCount
                            + " marked high or critical by the model.",
                    highSeverityCount > 0 ? AlgoVisionTheme.ERROR : AlgoVisionTheme.SUCCESS));
            overview.add(Box.createVerticalStrut(10));
        }

        AnalysisResponse.CodeReview review = result.codeReview();
        if (review != null) {
            overview.add(card("Code review", value(review.summary()),
                    AlgoVisionTheme.ACCENT));
            overview.add(Box.createVerticalStrut(10));
            addListCard(overview, "Inefficient operations", review.inefficientOperations());
            addListCard(overview, "Unnecessary computations",
                    review.unnecessaryComputations());
            addListCard(overview, "Implementation choices", review.implementationChoices());
        }
        if (metrics != null && !isMissing(metrics.explanation())) {
            overview.add(Box.createVerticalStrut(10));
            overview.add(card("Complexity notes", metrics.explanation(),
                    AlgoVisionTheme.ACCENT));
        }
        overview.add(Box.createVerticalGlue());
        refresh(overview);
    }

    private void renderComplexity(AnalysisResponse.ComplexityAnalysis metrics) {
        complexity.removeAll();
        addSectionTitle(complexity, "Complexity profile",
                "Growth classes are qualitative indicators parsed from the model's response, not measured runtimes.");
        if (metrics == null) {
            complexity.add(card("Unavailable", "The response did not include complexity metrics.",
                    AlgoVisionTheme.WARNING));
            refresh(complexity);
            return;
        }
        JPanel primary = new JPanel(new java.awt.GridLayout(1, 2, 10, 0));
        primary.setOpaque(false);
        primary.add(new ComplexityIndicator("TIME", metrics.timeComplexity()));
        primary.add(new ComplexityIndicator("SPACE", metrics.spaceComplexity()));
        primary.setAlignmentX(Component.LEFT_ALIGNMENT);
        complexity.add(primary);
        complexity.add(Box.createVerticalStrut(12));

        JPanel cases = new JPanel(new java.awt.GridLayout(1, 3, 10, 0));
        cases.setOpaque(false);
        cases.add(new ComplexityIndicator("BEST CASE", metrics.bestCase()));
        cases.add(new ComplexityIndicator("AVERAGE CASE", metrics.averageCase()));
        cases.add(new ComplexityIndicator("WORST CASE", metrics.worstCase()));
        cases.setAlignmentX(Component.LEFT_ALIGNMENT);
        complexity.add(cases);
        if (!isMissing(metrics.explanation())) {
            complexity.add(Box.createVerticalStrut(12));
            complexity.add(card("Model explanation", metrics.explanation(),
                    AlgoVisionTheme.ACCENT));
        }
        complexity.add(Box.createVerticalGlue());
        refresh(complexity);
    }

    private void renderBugs(AnalysisResponse.BugDetection detection) {
        bugs.removeAll();
        addSectionTitle(bugs, "Bugs & edge cases",
                "Only findings returned by the analysis are shown. Reported line references are highlighted in the editor.");
        if (detection == null) {
            bugs.add(card("Unavailable", "The response did not include bug analysis.",
                    AlgoVisionTheme.WARNING));
            refresh(bugs);
            return;
        }
        if (!isMissing(detection.summary())) {
            bugs.add(card("Summary", detection.summary(), AlgoVisionTheme.ACCENT));
            bugs.add(Box.createVerticalStrut(10));
        }
        addFindingCards(bugs, "Logical errors", detection.logicalErrors());
        addFindingCards(bugs, "Potential bugs", detection.potentialBugs());
        addFindingCards(bugs, "Edge cases", detection.edgeCases());
        if (findingCount(detection) == 0) {
            bugs.add(card("No findings returned",
                    "The model did not report findings in these categories.",
                    AlgoVisionTheme.SUCCESS));
        }
        bugs.add(Box.createVerticalGlue());
        refresh(bugs);
    }

    private void renderExplanation(AnalysisResponse.CodeExplanation result) {
        explanation.removeAll();
        addSectionTitle(explanation, "Code explanation",
                "The flow below uses the algorithm steps returned by the model.");
        if (result == null) {
            explanation.add(card("Unavailable", "The response did not include an explanation.",
                    AlgoVisionTheme.WARNING));
            refresh(explanation);
            return;
        }
        if (!isMissing(result.approach())) {
            explanation.add(card("Approach", result.approach(), AlgoVisionTheme.ACCENT));
            explanation.add(Box.createVerticalStrut(12));
        }
        List<String> steps = nonBlank(result.algorithmSteps());
        if (steps.isEmpty()) {
            explanation.add(card("Steps unavailable",
                    "The response did not include structured algorithm steps.",
                    AlgoVisionTheme.WARNING));
        } else {
            explanation.add(stepFlow(steps));
        }
        if (result.importantSections() != null
                && result.importantSections().stream().anyMatch(section -> section != null)) {
            explanation.add(Box.createVerticalStrut(14));
            addSectionTitle(explanation, "Important sections", null);
            result.importantSections().stream().filter(section -> section != null)
                    .forEach(section -> {
                        explanation.add(card(value(section.section()),
                                value(section.explanation()), AlgoVisionTheme.PRIMARY));
                        explanation.add(Box.createVerticalStrut(8));
                    });
        }
        explanation.add(Box.createVerticalGlue());
        refresh(explanation);
    }

    private void renderOptimization(AnalysisResponse result, String originalSource,
            String language) {
        optimization.removeAll();
        addSectionTitle(optimization, "Optimization comparison",
                "Compare the submitted source with the optimized code returned by the model.");
        AnalysisResponse.Optimization resultOptimization = result.optimization();
        if (resultOptimization == null) {
            optimization.add(card("Unavailable", "The response did not include optimization results.",
                    AlgoVisionTheme.WARNING));
            refresh(optimization);
            return;
        }

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        toolbar.add(label("SIDE-BY-SIDE SOURCE", MUTED, Font.BOLD, 10),
                BorderLayout.WEST);
        toolbar.add(copyButton, BorderLayout.EAST);
        toolbar.setAlignmentX(Component.LEFT_ALIGNMENT);
        optimization.add(toolbar);
        optimization.add(Box.createVerticalStrut(8));

        JPanel sourceComparison = new JPanel(new java.awt.GridLayout(1, 2, 10, 0));
        sourceComparison.setOpaque(false);
        sourceComparison.add(codeCard("Original code", originalSource, language));
        sourceComparison.add(codeCard("Optimized code",
                value(resultOptimization.optimizedCode()), language));
        sourceComparison.setAlignmentX(Component.LEFT_ALIGNMENT);
        optimization.add(sourceComparison);
        optimization.add(Box.createVerticalStrut(12));

        List<String> beforeAfter = complexityTokens(resultOptimization.beforeAfterComplexity());
        JPanel complexityComparison = new JPanel(new java.awt.GridLayout(1, 2, 10, 0));
        complexityComparison.setOpaque(false);
        if (beforeAfter.size() == 2) {
            complexityComparison.add(new ComplexityIndicator("ORIGINAL COMPLEXITY",
                    beforeAfter.get(0)));
            complexityComparison.add(new ComplexityIndicator("OPTIMIZED COMPLEXITY",
                    beforeAfter.get(1)));
        } else {
            complexityComparison.add(new ComplexityIndicator("ORIGINAL COMPLEXITY", null));
            complexityComparison.add(new ComplexityIndicator("OPTIMIZED COMPLEXITY", null));
        }
        complexityComparison.setAlignmentX(Component.LEFT_ALIGNMENT);
        optimization.add(complexityComparison);
        if (!isMissing(resultOptimization.beforeAfterComplexity())) {
            optimization.add(Box.createVerticalStrut(8));
            optimization.add(card("API-reported complexity comparison",
                    resultOptimization.beforeAfterComplexity(), AlgoVisionTheme.ACCENT));
        } else {
            optimization.add(Box.createVerticalStrut(8));
            optimization.add(card("Complexity comparison unavailable",
                    "The response did not provide a comparable before-and-after complexity.",
                    AlgoVisionTheme.WARNING));
        }
        optimization.add(Box.createVerticalStrut(10));
        addListCard(optimization, "Optimization suggestions", resultOptimization.suggestions());
        AnalysisResponse.CodeReview review = result.codeReview();
        if (review != null && !isMissing(review.summary())) {
            optimization.add(Box.createVerticalStrut(8));
            optimization.add(card("Improvement notes", review.summary(), AlgoVisionTheme.SUCCESS));
        }
        optimization.add(Box.createVerticalGlue());
        refresh(optimization);
    }

    private void renderInterview(List<AnalysisResponse.InterviewQuestion> questions) {
        interview.removeAll();
        addSectionTitle(interview, "Interview questions",
                "Questions and answers returned by the model.");
        if (questions == null || questions.stream().noneMatch(question -> question != null)) {
            interview.add(card("Unavailable", "The response did not include interview questions.",
                    AlgoVisionTheme.WARNING));
        } else {
            questions.stream().filter(question -> question != null).forEach(question -> {
                String title = value(question.question());
                String answer = "Answer: " + value(question.answer())
                        + "\n\nConcept: " + value(question.concept());
                interview.add(card(title, answer, AlgoVisionTheme.ACCENT));
                interview.add(Box.createVerticalStrut(10));
            });
        }
        interview.add(Box.createVerticalGlue());
        refresh(interview);
    }

    private JPanel codeCard(String title, String source, String language) {
        JPanel card = new JPanel(new BorderLayout(0, 7));
        card.setBackground(SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(10, 10, 10, 10)));
        card.add(label(title, TEXT, Font.BOLD, 12), BorderLayout.NORTH);
        SyntaxCodeEditor code = new SyntaxCodeEditor(false);
        code.setLanguage(language);
        code.editor().setText(source);
        code.setPreferredSize(new Dimension(200, 190));
        card.add(code, BorderLayout.CENTER);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        return card;
    }

    private JPanel stepFlow(List<String> steps) {
        JPanel flow = new JPanel();
        flow.setOpaque(false);
        flow.setLayout(new BoxLayout(flow, BoxLayout.Y_AXIS));
        flow.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (int index = 0; index < steps.size(); index++) {
            JPanel step = new JPanel(new BorderLayout(10, 0));
            step.setOpaque(false);
            JLabel badge = label(String.format("%02d", index + 1), AlgoVisionTheme.ACCENT,
                    Font.BOLD, 13);
            badge.setHorizontalAlignment(JLabel.CENTER);
            badge.setPreferredSize(new Dimension(42, 42));
            badge.setOpaque(true);
            badge.setBackground(AlgoVisionTheme.PRIMARY_SELECTED);
            badge.setBorder(BorderFactory.createLineBorder(AlgoVisionTheme.PRIMARY));
            JPanel stepCard = card("STEP " + (index + 1), steps.get(index),
                    AlgoVisionTheme.PRIMARY);
            step.add(badge, BorderLayout.NORTH);
            step.add(stepCard, BorderLayout.CENTER);
            step.setAlignmentX(Component.LEFT_ALIGNMENT);
            flow.add(step);
            if (index < steps.size() - 1) {
                JPanel connector = new JPanel();
                connector.setBackground(AlgoVisionTheme.BORDER_SUBTLE);
                connector.setMaximumSize(new Dimension(2, 14));
                connector.setPreferredSize(new Dimension(2, 14));
                connector.setAlignmentX(Component.LEFT_ALIGNMENT);
                flow.add(connector);
            }
        }
        return flow;
    }

    private void addFindingCards(JPanel target, String title,
            List<AnalysisResponse.Finding> findings) {
        target.add(sectionHeading(title));
        target.add(Box.createVerticalStrut(6));
        if (findings == null || findings.stream().noneMatch(finding -> finding != null)) {
            target.add(card(title, "No findings returned in this category.",
                    AlgoVisionTheme.SUCCESS));
        } else {
            findings.stream().filter(finding -> finding != null).forEach(finding -> {
                String reference = value(finding.reference());
                String severity = value(finding.severity());
                String line = reportedLine(reference) > 0
                        ? "Line " + reportedLine(reference) : "Line not provided";
                JPanel findingCard = new JPanel(new BorderLayout(6, 6));
                findingCard.setBackground(SURFACE);
                findingCard.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER),
                        new EmptyBorder(10, 11, 10, 11)));
                JPanel header = new JPanel(new BorderLayout(8, 0));
                header.setOpaque(false);
                JLabel heading = label(reference, TEXT, Font.BOLD, 12);
                JLabel severityBadge = label(severity.toUpperCase(), severityColor(severity),
                        Font.BOLD, 10);
                header.add(heading, BorderLayout.WEST);
                header.add(severityBadge, BorderLayout.EAST);
                findingCard.add(header, BorderLayout.NORTH);
                findingCard.add(body(finding.description()), BorderLayout.CENTER);
                findingCard.add(label(line, MUTED, Font.PLAIN, 10), BorderLayout.SOUTH);
                findingCard.setAlignmentX(Component.LEFT_ALIGNMENT);
                target.add(findingCard);
                target.add(Box.createVerticalStrut(8));
            });
        }
    }

    private JPanel card(String title, String message, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 7));
        card.setBackground(SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, accent),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER),
                        new EmptyBorder(11, 12, 11, 12))));
        card.add(label(title, TEXT, Font.BOLD, 12), BorderLayout.NORTH);
        card.add(body(message), BorderLayout.CENTER);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        return card;
    }

    private JPanel sectionHeading(String title) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(label(title, AlgoVisionTheme.ACCENT, Font.BOLD, 11),
                BorderLayout.WEST);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        return wrapper;
    }

    private void addSectionTitle(JPanel target, String title, String subtitle) {
        target.add(label(title, TEXT, Font.BOLD, 18));
        if (subtitle != null && !subtitle.isBlank()) {
            target.add(Box.createVerticalStrut(3));
            target.add(label(subtitle, MUTED, Font.PLAIN, 11));
        }
        target.add(Box.createVerticalStrut(12));
    }

    private void addListCard(JPanel target, String title, List<String> values) {
        List<String> present = nonBlank(values);
        if (!present.isEmpty()) {
            target.add(card(title, String.join("\n\n", present), AlgoVisionTheme.ACCENT));
            target.add(Box.createVerticalStrut(8));
        }
    }

    private JTextArea body(String text) {
        JTextArea area = new JTextArea(value(text));
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setForeground(BODY);
        area.setFont(AlgoVisionTheme.font(Font.PLAIN, 12));
        area.setBorder(BorderFactory.createEmptyBorder());
        area.setColumns(36);
        area.setRows(estimatedRows(text));
        return area;
    }

    private int estimatedRows(String text) {
        if (text == null || text.isBlank()) {
            return 1;
        }
        int rows = 0;
        for (String line : text.split("\\R", -1)) {
            rows += Math.max(1, (line.length() + 69) / 70);
        }
        return Math.max(1, rows);
    }

    private JLabel label(String text, Color color, int style, int size) {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(AlgoVisionTheme.font(style, size));
        return label;
    }

    private Color severityColor(String severity) {
        return switch (severity == null ? "" : severity.toLowerCase()) {
            case "critical", "high" -> AlgoVisionTheme.ERROR;
            case "medium", "moderate" -> AlgoVisionTheme.WARNING;
            case "low", "info", "informational" -> AlgoVisionTheme.SUCCESS;
            default -> MUTED;
        };
    }

    private boolean isHighSeverity(String severity) {
        return severity != null && (severity.equalsIgnoreCase("high")
                || severity.equalsIgnoreCase("critical"));
    }

    private int findingCount(AnalysisResponse.BugDetection detection) {
        return allFindings(detection).size();
    }

    private List<AnalysisResponse.Finding> allFindings(AnalysisResponse.BugDetection detection) {
        List<AnalysisResponse.Finding> result = new ArrayList<>();
        addAll(result, detection.logicalErrors());
        addAll(result, detection.potentialBugs());
        addAll(result, detection.edgeCases());
        return result;
    }

    private void addAll(List<AnalysisResponse.Finding> output,
            List<AnalysisResponse.Finding> values) {
        if (values != null) {
            values.stream().filter(value -> value != null).forEach(output::add);
        }
    }

    private Set<Integer> collectProblemLines(AnalysisResponse.BugDetection detection) {
        if (detection == null) {
            return Set.of();
        }
        Set<Integer> lines = new LinkedHashSet<>();
        allFindings(detection).stream().map(finding -> reportedLine(finding.reference()))
                .filter(line -> line > 0).forEach(lines::add);
        return Set.copyOf(lines);
    }

    private int reportedLine(String reference) {
        if (reference == null) {
            return -1;
        }
        Matcher matcher = REPORTED_LINE.matcher(reference);
        if (!matcher.find()) {
            return -1;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private List<String> complexityTokens(String value) {
        if (isMissing(value)) {
            return List.of();
        }
        Matcher matcher = COMPLEXITY_TOKEN.matcher(value);
        List<String> tokens = new ArrayList<>();
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        return tokens;
    }

    private List<String> nonBlank(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream().filter(value -> value != null && !value.isBlank()).toList();
    }

    private String value(String text) {
        return text == null || text.isBlank() ? "Not provided." : text;
    }

    private boolean isMissing(String value) {
        return value == null || value.isBlank();
    }

    private void refresh(JPanel panel) {
        panel.revalidate();
        panel.repaint();
    }

    private final class ComplexityIndicator extends JPanel {
        private final String rawValue;
        private final int growthRank;

        private ComplexityIndicator(String title, String complexityValue) {
            rawValue = complexityValue;
            growthRank = growthRank(complexityValue);
            setLayout(new BorderLayout(0, 5));
            setBackground(SURFACE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER),
                    new EmptyBorder(10, 11, 9, 11)));
            add(label(title, MUTED, Font.BOLD, 9), BorderLayout.NORTH);
            JPanel detail = new JPanel();
            detail.setOpaque(false);
            detail.setLayout(new BoxLayout(detail, BoxLayout.Y_AXIS));
            detail.add(label(isMissing(rawValue) ? "Unavailable" : rawValue,
                    growthRank < 0 ? AlgoVisionTheme.WARNING : AlgoVisionTheme.ACCENT,
                    Font.BOLD, 17));
            detail.add(Box.createVerticalStrut(2));
            detail.add(label(growthRank < 0 ? "No class in response"
                    : GROWTH_LABELS[growthRank] + " growth",
                    growthRank < 0 ? MUTED : BODY, Font.PLAIN, 10));
            add(detail, BorderLayout.CENTER);
            setPreferredSize(new Dimension(190, 108));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            int left = 12;
            int top = getHeight() - 15;
            int gap = 4;
            int width = Math.max(3, (getWidth() - left * 2 - gap * (GROWTH_LABELS.length - 1))
                    / GROWTH_LABELS.length);
            int available = width * GROWTH_LABELS.length + gap * (GROWTH_LABELS.length - 1);
            int start = Math.max(left, (getWidth() - available) / 2);
            for (int index = 0; index < GROWTH_LABELS.length; index++) {
                graphics.setColor(index == growthRank
                        ? AlgoVisionTheme.PRIMARY : AlgoVisionTheme.CONTROL_BACKGROUND);
                graphics.fillRoundRect(start + index * (width + gap), top, width, 4, 3, 3);
            }
        }
    }

    private static final String[] GROWTH_LABELS = {
        "Constant", "Logarithmic", "Linear", "Linearithmic",
        "Quadratic", "Cubic", "Exponential", "Factorial"
    };

    private static int growthRank(String complexity) {
        if (complexity == null) {
            return -1;
        }
        Matcher matcher = COMPLEXITY_TOKEN.matcher(complexity);
        if (!matcher.find()) {
            return -1;
        }
        String expression = matcher.group().toLowerCase().replaceAll("\\s+", "");
        String body = expression.substring(expression.indexOf('(') + 1,
                expression.lastIndexOf(')'));
        if (body.equals("1") || body.matches("1(?:\\+1)+")) {
            return 0;
        }
        if (body.contains("!")) {
            return 7;
        }
        if (body.contains("2^n") || body.contains("2ⁿ") || body.contains("exp")) {
            return 6;
        }
        if ((body.contains("n^3") || body.contains("n³")) && !body.contains("log")) {
            return 5;
        }
        if ((body.contains("n^2") || body.contains("n²")) && !body.contains("log")) {
            return 4;
        }
        if (body.contains("log")) {
            return body.contains("n") ? 3 : 1;
        }
        if (body.matches("[a-z](?:\\+[a-z])+")
                || body.matches("[a-z]")) {
            return 2;
        }
        return -1;
    }
}
