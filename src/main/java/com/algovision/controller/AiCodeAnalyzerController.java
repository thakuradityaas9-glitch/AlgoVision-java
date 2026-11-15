package com.algovision.controller;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import com.algovision.ai.AnalysisClient;
import com.algovision.ai.AnalysisResponse;
import com.algovision.ai.GroqService;
import com.algovision.ui.AiAnalysisDashboard;
import com.algovision.ui.AlgoVisionTheme;
import com.algovision.ui.SyntaxCodeEditor;

public final class AiCodeAnalyzerController extends JPanel {
    private static final System.Logger LOGGER =
            System.getLogger(AiCodeAnalyzerController.class.getName());
    private static final Color APP_BACKGROUND = AlgoVisionTheme.APP_BACKGROUND;
    private static final Color PANEL_BACKGROUND = AlgoVisionTheme.PANEL_BACKGROUND;
    private static final Color EDITOR_BACKGROUND = AlgoVisionTheme.SURFACE_BACKGROUND;
    private static final Color BORDER_COLOR = AlgoVisionTheme.BORDER;
    private static final Color PRIMARY_TEXT = AlgoVisionTheme.TEXT_PRIMARY;
    private static final Color BODY_TEXT = AlgoVisionTheme.TEXT_BODY;
    private static final Color MUTED_TEXT = AlgoVisionTheme.TEXT_MUTED;
    private static final Color ACCENT = AlgoVisionTheme.ACCENT;

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
    private static final String PYTHON_SAMPLE = """
            def binary_search(values, target):
                left, right = 0, len(values) - 1
                while left <= right:
                    middle = left + (right - left) // 2
                    if values[middle] == target:
                        return middle
                    if values[middle] < target:
                        left = middle + 1
                    else:
                        right = middle - 1
                return -1
            """;

    private final AnalysisClient analysisClient;
    private final JComboBox<String> languageChoice = new JComboBox<>(
            new String[] {"C++", "Java", "Python"});
    private final JComboBox<String> categoryChoice = new JComboBox<>(new String[] {
            "Arrays", "Strings", "Linked Lists", "Stacks and Queues", "Trees",
            "Graphs", "Sorting", "Searching", "Dynamic Programming", "Greedy",
            "Backtracking", "Other"
    });
    private final SyntaxCodeEditor sourceEditor = new SyntaxCodeEditor(true);
    private final JTextPane codeEditor = sourceEditor.editor();
    private final JButton analyzeButton = new JButton("Analyze with Groq");
    private final JButton clearButton = new JButton("Clear");
    private final JButton sampleButton = new JButton("Load sample");
    private final JProgressBar loadingIndicator = new JProgressBar();
    private final JLabel statusLabel = new JLabel("Paste code then analyze.");
    private final AiAnalysisDashboard resultDashboard = new AiAnalysisDashboard();

    private CompletableFuture<AnalysisResponse> activeRequest;
    private long requestGeneration;
    private boolean loading;
    private boolean lastCompletionOnEdt;

    public AiCodeAnalyzerController() {
        this(new GroqService());
    }

    AiCodeAnalyzerController(AnalysisClient analysisClient) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("AiCodeAnalyzerController must be created on the EDT.");
        }
        this.analysisClient = Objects.requireNonNull(analysisClient, "analysisClient");
        setName("aiCodeAnalyzer");
        setLayout(new BorderLayout(0, 14));
        setBackground(APP_BACKGROUND);
        setBorder(new EmptyBorder(20, 22, 18, 22));

        configureControls();
        add(createHeader(), BorderLayout.NORTH);
        add(createWorkspaceSplit(), BorderLayout.CENTER);
        add(createStatusBar(), BorderLayout.SOUTH);
        configureListeners();
        setLoading(false);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(label("AI-POWERED CODE REVIEW", ACCENT, Font.BOLD, 10));
        header.add(Box.createVerticalStrut(4));
        header.add(label("AI Code Analyzer", PRIMARY_TEXT, Font.BOLD, 25));
        header.add(Box.createVerticalStrut(3));
        header.add(label(
                "Complexity, correctness, optimization, and interview insights for your DSA code.",
                BODY_TEXT, Font.PLAIN, 12));
        return header;
    }

    private JSplitPane createWorkspaceSplit() {
        JSplitPane split = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT, createEditorPanel(), createResultsPanel());
        split.setName("analyzerSplitPane");
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setDividerSize(8);
        split.setContinuousLayout(true);
        split.setResizeWeight(0.48);
        split.setDividerLocation(0.48);
        split.setBackground(APP_BACKGROUND);
        return split;
    }

    private JPanel createEditorPanel() {
        JPanel panel = panel();
        panel.setLayout(new BorderLayout(0, 10));
        panel.setBorder(cardBorder());

        JPanel editorContent = new JPanel(new BorderLayout(0, 10));
        editorContent.setOpaque(false);
        JPanel selectors = new JPanel(new BorderLayout(9, 0));
        selectors.setOpaque(false);
        selectors.add(labeledControl("LANGUAGE", languageChoice), BorderLayout.WEST);
        selectors.add(labeledControl("DSA CATEGORY", categoryChoice), BorderLayout.CENTER);
        editorContent.add(selectors, BorderLayout.NORTH);

        editorContent.add(sourceEditor, BorderLayout.CENTER);
        panel.add(label("SOURCE CODE", MUTED_TEXT, Font.BOLD, 10), BorderLayout.NORTH);
        panel.add(editorContent, BorderLayout.CENTER);
        panel.add(createEditorActions(), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createEditorActions() {
        JPanel actions = new JPanel(new BorderLayout(0, 7));
        actions.setOpaque(false);
        JPanel analyzeRow = new JPanel(new BorderLayout(8, 0));
        analyzeRow.setOpaque(false);
        JPanel secondaryActions = new JPanel(new GridLayout(1, 2, 7, 0));
        secondaryActions.setOpaque(false);
        styleButton(analyzeButton, true);
        styleButton(clearButton, false);
        styleButton(sampleButton, false);
        analyzeRow.add(analyzeButton, BorderLayout.CENTER);
        loadingIndicator.setName("analysisProgress");
        loadingIndicator.setIndeterminate(true);
        loadingIndicator.setBorderPainted(false);
        loadingIndicator.setStringPainted(false);
        loadingIndicator.setBackground(PANEL_BACKGROUND);
        loadingIndicator.setForeground(ACCENT);
        loadingIndicator.setPreferredSize(new java.awt.Dimension(100, 14));
        loadingIndicator.setVisible(false);
        analyzeRow.add(loadingIndicator, BorderLayout.EAST);
        secondaryActions.add(clearButton);
        secondaryActions.add(sampleButton);
        actions.add(analyzeRow, BorderLayout.NORTH);
        actions.add(secondaryActions, BorderLayout.CENTER);
        return actions;
    }

    private JPanel createResultsPanel() {
        JPanel panel = panel();
        panel.setLayout(new BorderLayout(0, 8));
        panel.setBorder(cardBorder());
        panel.add(label("ANALYSIS RESULTS", MUTED_TEXT, Font.BOLD, 10), BorderLayout.NORTH);
        panel.add(resultDashboard, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createStatusBar() {
        JPanel statusPanel = panel();
        statusPanel.setLayout(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(8, 11, 8, 11)));
        statusLabel.setName("analysisStatus");
        statusLabel.setFont(font(Font.PLAIN, 11));
        statusLabel.setForeground(MUTED_TEXT);
        statusPanel.add(statusLabel, BorderLayout.CENTER);
        return statusPanel;
    }

    private void configureControls() {
        languageChoice.setName("languageChoice");
        categoryChoice.setName("categoryChoice");
        styleCombo(languageChoice);
        styleCombo(categoryChoice);
        sourceEditor.setLanguage((String) languageChoice.getSelectedItem());
        resultDashboard.setCopyAction(this::copyOptimizedCode);
    }

    private void configureListeners() {
        analyzeButton.addActionListener(event -> analyze());
        clearButton.addActionListener(event -> clear());
        sampleButton.addActionListener(event -> loadSample());
        languageChoice.addActionListener(event ->
                sourceEditor.setLanguage((String) languageChoice.getSelectedItem()));
    }

    private JPanel labeledControl(String title, JComboBox<String> control) {
        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        JLabel label = label(title, MUTED_TEXT, Font.BOLD, 9);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        wrapper.add(label);
        wrapper.add(control);
        return wrapper;
    }

    private void styleCombo(JComboBox<String> combo) {
        combo.setFont(font(Font.PLAIN, 11));
        AlgoVisionTheme.styleComboBox(combo);
    }

    private void styleScrollPane(JScrollPane scroll) {
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        scroll.getViewport().setBackground(EDITOR_BACKGROUND);
        scroll.setBackground(EDITOR_BACKGROUND);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
    }

    private JPanel panel() {
        JPanel panel = new JPanel();
        panel.setBackground(PANEL_BACKGROUND);
        return panel;
    }

    private javax.swing.border.Border cardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(12, 12, 12, 12));
    }

    private void analyze() {
        if (loading) {
            return;
        }
        clearResultTabs();
        String code = codeEditor.getText();
        if (code == null || code.isBlank()) {
            showError("Add some code before starting the analysis.");
            codeEditor.requestFocusInWindow();
            return;
        }
        String language = (String) languageChoice.getSelectedItem();
        String category = (String) categoryChoice.getSelectedItem();
        if (language == null || category == null) {
            showError("Choose a language and DSA category before analyzing.");
            return;
        }

        long generation = ++requestGeneration;
        lastCompletionOnEdt = false;
        setLoading(true);
        setStatus("Sending your code to Groq...", false);
        try {
            CompletableFuture<AnalysisResponse> request =
                    analysisClient.analyze(code, language, category);
            if (request == null) {
                finishFailure(generation,
                        new IllegalStateException("Analysis service returned no request."));
                return;
            }
            activeRequest = request;
            request.whenComplete((analysis, error) -> SwingUtilities.invokeLater(
                    () -> completeRequest(generation, analysis, error)));
        } catch (RuntimeException exception) {
            finishFailure(generation, exception);
        }
    }

    private void completeRequest(long generation, AnalysisResponse analysis, Throwable error) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Analysis results must be applied on the EDT.");
        }
        lastCompletionOnEdt = true;
        if (generation != requestGeneration) {
            return;
        }
        activeRequest = null;
        setLoading(false);
        if (error != null) {
            LOGGER.log(System.Logger.Level.WARNING,
                    "Groq analysis request completed with an error for request {0}.", generation);
            showError(userFacingError(error));
            return;
        }
        if (!hasAnalysisContent(analysis)) {
            LOGGER.log(System.Logger.Level.WARNING,
                    "Groq analysis request {0} completed without renderable content.", generation);
            showError("Groq returned an empty analysis. Please try again.");
            return;
        }
        try {
            LOGGER.log(System.Logger.Level.INFO,
                    "Groq analysis request {0} received by the controller; rendering on the EDT.",
                    generation);
            renderAnalysis(analysis);
            setStatus("Analysis complete.", false);
            LOGGER.log(System.Logger.Level.INFO,
                    "Groq analysis request {0} rendered into the results dashboard.", generation);
        } catch (RuntimeException exception) {
            clearResultTabs();
            showError("Groq returned an incomplete or malformed analysis. Please try again.");
        }
    }

    private void finishFailure(long generation, Throwable error) {
        if (generation != requestGeneration) {
            return;
        }
        activeRequest = null;
        setLoading(false);
        showError(userFacingError(error));
    }

    private String userFacingError(Throwable error) {
        Throwable cause = rootCause(error);
        if (cause instanceof GroqService.GroqException groqException) {
            return groqException.getMessage();
        }
        if (cause instanceof CancellationException) {
            return "The analysis request was canceled.";
        }
        return "Groq analysis failed unexpectedly. Please try again.";
    }

    private Throwable rootCause(Throwable error) {
        Throwable cause = error;
        while (cause != null && cause.getCause() != null
                && (cause instanceof CompletionException
                || cause instanceof ExecutionException)) {
            cause = cause.getCause();
        }
        return cause == null ? error : cause;
    }

    private void setLoading(boolean isLoading) {
        loading = isLoading;
        analyzeButton.setEnabled(!isLoading);
        analyzeButton.setText(isLoading ? "Analyzing..." : "Analyze with Groq");
        clearButton.setEnabled(!isLoading);
        sampleButton.setEnabled(!isLoading);
        languageChoice.setEnabled(!isLoading);
        categoryChoice.setEnabled(!isLoading);
        codeEditor.setEnabled(!isLoading);
        resultDashboard.setCopyEnabled(!isLoading);
        loadingIndicator.setVisible(isLoading);
        loadingIndicator.setIndeterminate(isLoading);
    }

    private void showError(String message) {
        setStatus(message == null || message.isBlank()
                ? "Groq analysis failed. Please try again." : message, true);
    }

    private void setStatus(String message, boolean error) {
        statusLabel.setText(message);
        statusLabel.setForeground(error ? AlgoVisionTheme.ERROR : MUTED_TEXT);
    }

    private void clear() {
        requestGeneration++;
        if (activeRequest != null) {
            activeRequest.cancel(true);
            activeRequest = null;
        }
        setLoading(false);
        codeEditor.setText("");
        clearResultTabs();
        setStatus("Paste code then analyze.", false);
    }

    private void loadSample() {
        codeEditor.setText(switch (String.valueOf(languageChoice.getSelectedItem())) {
            case "Java" -> JAVA_SAMPLE;
            case "Python" -> PYTHON_SAMPLE;
            default -> CPP_SAMPLE;
        });
        clearResultTabs();
        setStatus("Sample loaded. Review it, then analyze.", false);
        codeEditor.requestFocusInWindow();
    }

    private void clearResultTabs() {
        resultDashboard.clear();
        sourceEditor.setProblemLines(java.util.Set.of());
    }

    private boolean hasAnalysisContent(AnalysisResponse result) {
        return result != null && (result.complexityAnalysis() != null
                || result.bugDetection() != null
                || result.codeReview() != null
                || result.optimization() != null
                || (result.interviewQuestions() != null && !result.interviewQuestions().isEmpty())
                || result.codeExplanation() != null);
    }

    private void renderAnalysis(AnalysisResponse result) {
        String language = String.valueOf(languageChoice.getSelectedItem());
        resultDashboard.render(result, codeEditor.getText(), language);
        sourceEditor.setProblemLines(resultDashboard.problemLines());
    }

    private void copyOptimizedCode(String code) {
        if (code.isEmpty()) {
            return;
        }
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(code), null);
            setStatus("Optimized code copied to clipboard.", false);
        } catch (HeadlessException | IllegalStateException | SecurityException exception) {
            showError("Could not access the system clipboard.");
        }
    }

    private JLabel label(String text, Color color, int style, int size) {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(font(style, size));
        return label;
    }

    private Font font(int style, int size) {
        return AlgoVisionTheme.font(style, size);
    }

    private void styleButton(JButton button, boolean primary) {
        AlgoVisionTheme.styleButton(button, primary);
    }

    boolean isLoadingForTesting() {
        return loading;
    }

    String statusForTesting() {
        return statusLabel.getText();
    }

    String resultTextForTesting(String name) {
        return resultDashboard.contentForTesting(name);
    }

    String optimizedCodeForTesting() {
        return resultDashboard.optimizedCode();
    }

    boolean copyEnabledForTesting() {
        return resultDashboard.copyEnabledForTesting();
    }

    boolean lastCompletionOnEdtForTesting() {
        return lastCompletionOnEdt;
    }

    JTextPane codeEditorForTesting() {
        return codeEditor;
    }

    JComboBox<String> languageChoiceForTesting() {
        return languageChoice;
    }

    AiAnalysisDashboard dashboardForTesting() {
        return resultDashboard;
    }

    JButton analyzeButtonForTesting() {
        return analyzeButton;
    }

}
