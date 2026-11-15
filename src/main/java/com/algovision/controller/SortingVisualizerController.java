package com.algovision.controller;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import com.algovision.algorithms.BubbleSort;
import com.algovision.algorithms.InsertionSort;
import com.algovision.algorithms.MergeSort;
import com.algovision.algorithms.QuickSort;
import com.algovision.algorithms.SelectionSort;
import com.algovision.algorithms.SortingAlgorithm;
import com.algovision.model.SortOperation;
import com.algovision.model.SortStep;
import com.algovision.ui.AlgoVisionTheme;
import com.algovision.ui.ScrollablePanel;

public final class SortingVisualizerController extends JPanel {
    private static final Color APP_BACKGROUND = AlgoVisionTheme.APP_BACKGROUND;
    private static final Color PANEL_BACKGROUND = AlgoVisionTheme.PANEL_BACKGROUND;
    private static final Color INPUT_BACKGROUND = AlgoVisionTheme.SURFACE_BACKGROUND;
    private static final Color BORDER_COLOR = AlgoVisionTheme.BORDER;
    private static final Color PRIMARY_TEXT = AlgoVisionTheme.TEXT_PRIMARY;
    private static final Color BODY_TEXT = AlgoVisionTheme.TEXT_BODY;
    private static final Color ACCENT_TEXT = AlgoVisionTheme.ACCENT;
    private static final Color BAR_COLOR = new Color(0x5552c9);
    private static final Color COMPARED_COLOR = new Color(0xf0a64b);
    private static final Color PIVOT_COLOR = new Color(0x48c8d6);
    private static final Color SORTED_COLOR = new Color(0x36b891);

    private static final int RANDOM_ARRAY_SIZE = 18;
    private static final int MAX_ARRAY_SIZE = 50;
    private static final int BASE_INTERVAL_MILLIS = 480;
    private static final int MIN_INTERVAL_MILLIS = 120;
    private static final int MAX_INTERVAL_MILLIS = 1920;

    private final List<SortingAlgorithm> algorithms = List.of(
            new BubbleSort(),
            new SelectionSort(),
            new InsertionSort(),
            new MergeSort(),
            new QuickSort());
    private final JTextField arrayInput = new JTextField();
    private final JComboBox<SortingAlgorithm> algorithmChoice = new JComboBox<>();
    private final JSlider speedSlider = new JSlider(0, 100, 50);
    private final JLabel algorithmDescription = new JLabel();
    private final JLabel bestTimeValue = new JLabel();
    private final JLabel averageTimeValue = new JLabel();
    private final JLabel worstTimeValue = new JLabel();
    private final JLabel auxiliarySpaceValue = new JLabel();
    private final JLabel errorLabel = new JLabel(" ");
    private final JLabel sizeValue = new JLabel("0");
    private final JLabel comparisonsValue = new JLabel("0");
    private final JLabel swapsValue = new JLabel("0");
    private final JLabel statusValue = new JLabel("Ready");
    private final JLabel stepDescription = new JLabel(" ");
    private final JButton startButton = new JButton("Start");
    private final JButton pauseButton = new JButton("Pause");
    private final JButton resumeButton = new JButton("Resume");
    private final JButton resetButton = new JButton("Reset");
    private final JButton randomizeButton = new JButton("Randomize");
    private final JButton generateButton = new JButton("Generate");
    private final SortingBarsPanel barsPanel = new SortingBarsPanel();
    private final Timer animationTimer = new Timer(BASE_INTERVAL_MILLIS, event -> advance());

    private int[] initialArray = {42, 18, 67, 29, 85, 36, 54, 11, 73, 24, 91, 48};
    private SortingAlgorithm selectedAlgorithm = algorithms.getFirst();
    private List<SortStep> steps = List.of();
    private int stepIndex;
    private int generationId;
    private SwingWorker<List<SortStep>, Void> stepWorker;
    private boolean adjustingAlgorithmChoice;

    public SortingVisualizerController() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Create the sorting workspace on the Swing event dispatch thread.");
        }
        setLayout(new BorderLayout());
        setBackground(APP_BACKGROUND);
        add(createContent(), BorderLayout.CENTER);

        arrayInput.setText(join(initialArray));
        configureControls();
        updateComplexity();
        renderInitialState();
        updateButtons();
    }

    public JPanel getView() {
        return this;
    }

    private JScrollPane createContent() {
        JPanel content = new ScrollablePanel();
        content.setBackground(APP_BACKGROUND);
        content.setBorder(new EmptyBorder(34, 38, 30, 38));
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        content.add(label("SORTING ALGORITHMS", ACCENT_TEXT, Font.BOLD, 10));
        content.add(Box.createVerticalStrut(8));
        content.add(label("Sorting Visualizer", PRIMARY_TEXT, Font.BOLD, 30));
        content.add(Box.createVerticalStrut(7));
        algorithmDescription.setForeground(BODY_TEXT);
        algorithmDescription.setFont(font(Font.PLAIN, 13));
        content.add(algorithmDescription);
        content.add(Box.createVerticalStrut(20));
        content.add(createControls());
        content.add(Box.createVerticalStrut(14));
        content.add(createStatistics());
        content.add(Box.createVerticalStrut(14));
        content.add(createComplexityPanel());
        content.add(Box.createVerticalStrut(14));
        content.add(createVisualizationPanel());

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(APP_BACKGROUND);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return scrollPane;
    }

    private JPanel createControls() {
        JPanel panel = cardPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(15, 16, 13, 16));

        algorithmChoice.setRenderer(algorithmRenderer());
        AlgoVisionTheme.styleComboBox(algorithmChoice);
        algorithms.forEach(algorithmChoice::addItem);
        algorithmChoice.setSelectedItem(selectedAlgorithm);

        styleTextField(arrayInput);
        arrayInput.setToolTipText("Enter up to 50 comma-separated integers.");

        styleButton(randomizeButton, false);
        styleButton(generateButton, false);
        randomizeButton.addActionListener(event -> randomizeArray());
        generateButton.addActionListener(event -> generateFromInput());

        JPanel inputRow = new JPanel(new GridLayout(1, 4, 10, 0));
        inputRow.setOpaque(false);
        inputRow.add(labeledControl("ALGORITHM", algorithmChoice));
        inputRow.add(labeledControl("ARRAY VALUES", arrayInput));
        inputRow.add(labeledControl(" ", randomizeButton));
        inputRow.add(labeledControl(" ", generateButton));

        styleButton(startButton, true);
        styleButton(pauseButton, false);
        styleButton(resumeButton, false);
        styleButton(resetButton, false);
        startButton.addActionListener(event -> start());
        pauseButton.addActionListener(event -> pause());
        resumeButton.addActionListener(event -> resume());
        resetButton.addActionListener(event -> reset());

        JLabel speedLabel = label("SPEED", BODY_TEXT, Font.BOLD, 9);
        speedSlider.setOpaque(false);
        speedSlider.setMajorTickSpacing(25);
        speedSlider.setPaintTicks(false);
        speedSlider.setToolTipText("Animation speed");
        speedSlider.addChangeListener(event -> updateTimerSpeed());

        JPanel playbackRow = new JPanel();
        playbackRow.setOpaque(false);
        playbackRow.setLayout(new BoxLayout(playbackRow, BoxLayout.Y_AXIS));
        JPanel playbackButtons = new JPanel(new GridLayout(1, 4, 8, 0));
        playbackButtons.setOpaque(false);
        playbackButtons.add(startButton);
        playbackButtons.add(pauseButton);
        playbackButtons.add(resumeButton);
        playbackButtons.add(resetButton);
        JPanel speedRow = new JPanel(new BorderLayout(8, 0));
        speedRow.setOpaque(false);
        speedSlider.setPreferredSize(new Dimension(150, 28));
        speedRow.add(speedLabel, BorderLayout.WEST);
        speedRow.add(speedSlider, BorderLayout.CENTER);
        playbackRow.add(playbackButtons);
        playbackRow.add(Box.createVerticalStrut(8));
        playbackRow.add(speedRow);

        errorLabel.setForeground(new Color(0xff8a9b));
        errorLabel.setFont(font(Font.PLAIN, 11));
        errorLabel.setAlignmentX(LEFT_ALIGNMENT);

        panel.add(inputRow);
        panel.add(Box.createVerticalStrut(12));
        panel.add(playbackRow);
        panel.add(Box.createVerticalStrut(6));
        panel.add(errorLabel);
        return panel;
    }

    private ListCellRenderer<? super SortingAlgorithm> algorithmRenderer() {
        DefaultListCellRenderer renderer = new DefaultListCellRenderer();
        renderer.setBackground(INPUT_BACKGROUND);
        renderer.setForeground(PRIMARY_TEXT);
        renderer.setBorder(new EmptyBorder(7, 9, 7, 9));
        return (list, value, index, isSelected, cellHasFocus) -> {
            JLabel cell = (JLabel) renderer.getListCellRendererComponent(
                    list, value == null ? "" : value.name(), index, isSelected, cellHasFocus);
            if (isSelected) {
                cell.setBackground(new Color(0x242654));
                cell.setForeground(PRIMARY_TEXT);
            } else {
                cell.setBackground(INPUT_BACKGROUND);
                cell.setForeground(PRIMARY_TEXT);
            }
            return cell;
        };
    }

    private JPanel createStatistics() {
        JPanel row = new JPanel(new GridLayout(1, 4, 12, 0));
        row.setOpaque(false);
        row.add(statCard("ARRAY SIZE", sizeValue));
        row.add(statCard("COMPARISONS", comparisonsValue));
        row.add(statCard("SWAPS / MOVES", swapsValue));
        row.add(statCard("STATUS", statusValue));
        return row;
    }

    private JPanel createComplexityPanel() {
        JPanel section = new JPanel(new BorderLayout(0, 9));
        section.setOpaque(false);
        section.add(label("Algorithm complexity", PRIMARY_TEXT, Font.BOLD, 13), BorderLayout.NORTH);
        JPanel values = new JPanel(new GridLayout(1, 4, 12, 0));
        values.setOpaque(false);
        values.add(statCard("BEST CASE", bestTimeValue));
        values.add(statCard("AVERAGE CASE", averageTimeValue));
        values.add(statCard("WORST CASE", worstTimeValue));
        values.add(statCard("AUXILIARY SPACE", auxiliarySpaceValue));
        section.add(values, BorderLayout.CENTER);
        return section;
    }

    private JPanel createVisualizationPanel() {
        JPanel panel = cardPanel();
        panel.setLayout(new BorderLayout(0, 9));
        panel.setBorder(new EmptyBorder(14, 16, 14, 16));
        panel.add(label("Array", PRIMARY_TEXT, Font.BOLD, 13), BorderLayout.NORTH);

        JScrollPane barsScroll = new JScrollPane(barsPanel);
        barsScroll.setBorder(BorderFactory.createEmptyBorder());
        barsScroll.setOpaque(false);
        barsScroll.getViewport().setBackground(PANEL_BACKGROUND);
        barsScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        barsScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        barsScroll.getHorizontalScrollBar().setUnitIncrement(24);
        panel.add(barsScroll, BorderLayout.CENTER);

        JPanel details = new JPanel(new BorderLayout());
        details.setOpaque(false);
        stepDescription.setForeground(BODY_TEXT);
        stepDescription.setFont(font(Font.PLAIN, 11));
        details.add(stepDescription, BorderLayout.CENTER);
        panel.add(details, BorderLayout.SOUTH);
        panel.setPreferredSize(new Dimension(600, 360));
        return panel;
    }

    private JPanel statCard(String title, JLabel value) {
        JPanel card = cardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(12, 14, 12, 14));
        JLabel heading = label(title, new Color(0x71809b), Font.BOLD, 9);
        value.setForeground(new Color(0xf0f2fc));
        value.setFont(font(Font.BOLD, 16));
        value.setAlignmentX(LEFT_ALIGNMENT);
        card.add(heading);
        card.add(Box.createVerticalStrut(7));
        card.add(value);
        return card;
    }

    private JPanel labeledControl(String title, java.awt.Component control) {
        JPanel field = new JPanel();
        field.setOpaque(false);
        field.setLayout(new BoxLayout(field, BoxLayout.Y_AXIS));
        JLabel heading = label(title, new Color(0x71809b), Font.BOLD, 9);
        heading.setAlignmentX(LEFT_ALIGNMENT);
        control.setFont(font(Font.PLAIN, 12));
        control.setPreferredSize(new Dimension(100, 34));
        if (control instanceof javax.swing.JComponent component) {
            component.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
            component.setAlignmentX(LEFT_ALIGNMENT);
        }
        field.add(heading);
        field.add(Box.createVerticalStrut(6));
        field.add(control);
        return field;
    }

    private JPanel cardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(PANEL_BACKGROUND);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                new EmptyBorder(1, 1, 1, 1)));
        return panel;
    }

    private void configureControls() {
        adjustingAlgorithmChoice = true;
        algorithmChoice.addActionListener(event -> {
            if (!adjustingAlgorithmChoice) {
                selectAlgorithm();
            }
        });
        adjustingAlgorithmChoice = false;
        arrayInput.addActionListener(event -> generateFromInput());
    }

    private void styleTextField(JTextField field) {
        field.setBackground(AlgoVisionTheme.CONTROL_BACKGROUND);
        field.setForeground(PRIMARY_TEXT);
        field.setCaretColor(PRIMARY_TEXT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AlgoVisionTheme.BORDER_SUBTLE),
                new EmptyBorder(7, 9, 7, 9)));
        field.setFont(font(Font.PLAIN, 12));
    }

    private void styleButton(JButton button, boolean primary) {
        AlgoVisionTheme.styleButton(button, primary);
    }

    private JLabel label(String text, Color color, int style, int size) {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(font(style, size));
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private Font font(int style, int size) {
        return AlgoVisionTheme.font(style, size);
    }

    private void generateFromInput() {
        try {
            int[] parsed = parseArray(arrayInput.getText());
            cancelCurrentRun();
            initialArray = parsed;
            clearError();
            renderInitialState();
            setStatus("Ready");
            updateButtons();
        } catch (IllegalArgumentException exception) {
            showError(exception.getMessage());
        }
    }

    private int[] parseArray(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Enter at least one comma-separated integer.");
        }
        String[] parts = text.split(",", -1);
        if (parts.length > MAX_ARRAY_SIZE) {
            throw new IllegalArgumentException("Use no more than " + MAX_ARRAY_SIZE + " values.");
        }
        int[] values = new int[parts.length];
        for (int index = 0; index < parts.length; index++) {
            String part = parts[index].trim();
            if (part.isEmpty()) {
                throw new IllegalArgumentException("Every array value must be an integer.");
            }
            try {
                values[index] = Integer.parseInt(part);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Invalid integer: \"" + part + "\".");
            }
        }
        return values;
    }

    private void randomizeArray() {
        cancelCurrentRun();
        Random random = new Random();
        int[] values = new int[RANDOM_ARRAY_SIZE];
        for (int index = 0; index < values.length; index++) {
            values[index] = random.nextInt(91) + 10;
        }
        initialArray = values;
        arrayInput.setText(join(values));
        clearError();
        renderInitialState();
        setStatus("Ready");
        updateButtons();
    }

    private void selectAlgorithm() {
        SortingAlgorithm algorithm = (SortingAlgorithm) algorithmChoice.getSelectedItem();
        if (algorithm == null || algorithm == selectedAlgorithm) {
            return;
        }
        cancelCurrentRun();
        selectedAlgorithm = algorithm;
        updateComplexity();
        clearError();
        renderInitialState();
        setStatus("Ready");
        updateButtons();
    }

    private void updateComplexity() {
        algorithmDescription.setText(selectedAlgorithm.name() + " - " + selectedAlgorithm.description());
        bestTimeValue.setText(selectedAlgorithm.bestCaseTimeComplexity());
        averageTimeValue.setText(selectedAlgorithm.averageCaseTimeComplexity());
        worstTimeValue.setText(selectedAlgorithm.worstCaseTimeComplexity());
        auxiliarySpaceValue.setText(selectedAlgorithm.auxiliarySpaceComplexity());
    }

    private void start() {
        if (stepWorker != null || animationTimer.isRunning()
                || "Completed".equals(statusValue.getText())) {
            return;
        }
        cancelCurrentRun();
        clearError();
        setStatus("Preparing");
        stepDescription.setText("Preparing visualization steps...");
        updateButtons();

        int runId = generationId;
        int[] input = initialArray.clone();
        SortingAlgorithm algorithm = selectedAlgorithm;
        stepWorker = new SwingWorker<>() {
            @Override
            protected List<SortStep> doInBackground() {
                return algorithm.generateSteps(input);
            }

            @Override
            protected void done() {
                if (runId != generationId) {
                    return;
                }
                stepWorker = null;
                try {
                    steps = get();
                    stepIndex = 0;
                    if (steps.isEmpty()) {
                        finish();
                        return;
                    }
                    setStatus("Running");
                    updateTimerSpeed();
                    animationTimer.start();
                    updateButtons();
                } catch (CancellationException exception) {
                    setStatus("Ready");
                    updateButtons();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("Sorting step generation was interrupted.");
                    setStatus("Failed");
                    updateButtons();
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    showError("Could not prepare sorting steps: "
                            + (cause == null ? "unknown error" : cause.getMessage()));
                    setStatus("Failed");
                    updateButtons();
                }
            }
        };
        stepWorker.execute();
    }

    public void pause() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::pause);
            return;
        }
        if ("Preparing".equals(statusValue.getText())) {
            cancelCurrentRun();
            renderInitialState();
            stepDescription.setText("Sorting preparation was canceled while leaving the workspace.");
            setStatus("Ready");
            updateButtons();
            return;
        }
        if (animationTimer.isRunning()) {
            animationTimer.stop();
            setStatus("Paused");
            updateButtons();
        }
    }

    private void resume() {
        if ("Paused".equals(statusValue.getText()) && stepIndex < steps.size()) {
            setStatus("Running");
            updateTimerSpeed();
            animationTimer.start();
            updateButtons();
        }
    }

    private void reset() {
        cancelCurrentRun();
        steps = List.of();
        stepIndex = 0;
        arrayInput.setText(join(initialArray));
        clearError();
        renderInitialState();
        stepDescription.setText(" ");
        setStatus("Ready");
        updateButtons();
    }

    private void advance() {
        if (stepIndex >= steps.size()) {
            finish();
            return;
        }
        SortStep step = steps.get(stepIndex++);
        barsPanel.render(step.arrayState(), step.comparedIndexLeft(), step.comparedIndexRight(),
                step.pivotIndex(), step.sortedElements());
        comparisonsValue.setText(Integer.toString(step.comparisons()));
        swapsValue.setText(Integer.toString(step.swaps()));
        stepDescription.setText(descriptionFor(step));
        if (step.operation() == SortOperation.COMPLETE || stepIndex >= steps.size()) {
            finish();
        }
    }

    private String descriptionFor(SortStep step) {
        return switch (step.operation()) {
            case COMPARE -> "Comparing indices " + step.comparedIndexLeft()
                    + " and " + step.comparedIndexRight() + ".";
            case SWAP -> "Swapped indices " + step.comparedIndexLeft()
                    + " and " + step.comparedIndexRight() + ".";
            case MOVE -> "Shifted the value at index " + step.comparedIndexLeft()
                    + " to index " + step.comparedIndexRight() + ".";
            case OVERWRITE -> "Wrote the value at index " + step.comparedIndexRight() + ".";
            case PIVOT -> "Selected index " + step.pivotIndex() + " as the pivot.";
            case MARK_SORTED -> "Marked sorted elements.";
            case COMPLETE -> "Sorting complete.";
        };
    }

    private void finish() {
        animationTimer.stop();
        setStatus("Completed");
        updateButtons();
    }

    private void cancelCurrentRun() {
        generationId++;
        animationTimer.stop();
        if (stepWorker != null) {
            stepWorker.cancel(true);
            stepWorker = null;
        }
        steps = List.of();
        stepIndex = 0;
    }

    private void renderInitialState() {
        barsPanel.render(initialArray, -1, -1, -1, new boolean[initialArray.length]);
        sizeValue.setText(Integer.toString(initialArray.length));
        comparisonsValue.setText("0");
        swapsValue.setText("0");
        stepDescription.setText(" ");
    }

    private void updateTimerSpeed() {
        double rate = 0.25 + speedSlider.getValue() / 100.0 * 2.75;
        int delay = (int) Math.round(BASE_INTERVAL_MILLIS / rate);
        animationTimer.setDelay(Math.max(MIN_INTERVAL_MILLIS, Math.min(MAX_INTERVAL_MILLIS, delay)));
    }

    private void updateButtons() {
        String status = statusValue.getText();
        boolean preparing = "Preparing".equals(status);
        boolean running = "Running".equals(status);
        boolean paused = "Paused".equals(status);
        boolean completed = "Completed".equals(status);
        algorithmChoice.setEnabled(!preparing);
        arrayInput.setEnabled(!preparing);
        generateButton.setEnabled(!preparing);
        randomizeButton.setEnabled(!preparing);
        startButton.setEnabled(!preparing && !running && !paused && !completed);
        pauseButton.setEnabled(running);
        resumeButton.setEnabled(paused);
        resetButton.setEnabled(true);
    }

    private void setStatus(String status) {
        statusValue.setText(status);
        statusValue.setForeground(switch (status) {
            case "Running" -> AlgoVisionTheme.SUCCESS;
            case "Paused" -> AlgoVisionTheme.WARNING;
            case "Failed" -> AlgoVisionTheme.ERROR;
            case "Completed" -> AlgoVisionTheme.ACCENT;
            default -> AlgoVisionTheme.TEXT_PRIMARY;
        });
    }

    private void showError(String message) {
        errorLabel.setText(message == null || message.isBlank()
                ? "Could not generate sorting steps." : message);
    }

    private void clearError() {
        errorLabel.setText(" ");
    }

    private String join(int[] values) {
        return Arrays.stream(values)
                .mapToObj(Integer::toString)
                .collect(java.util.stream.Collectors.joining(", "));
    }

    boolean isAnimationTimerRunning() {
        return animationTimer.isRunning();
    }

    boolean isPreparingSteps() {
        return stepWorker != null;
    }

    String statusForTesting() {
        return statusValue.getText();
    }

    int stepIndexForTesting() {
        return stepIndex;
    }

    private final class SortingBarsPanel extends JPanel {
        private static final int MIN_BAR_HEIGHT = 42;
        private static final int MAX_BAR_HEIGHT = 238;
        private static final int MIN_BAR_WIDTH = 8;
        private static final int GAP = 8;

        private int[] values = new int[0];
        private boolean[] sorted = new boolean[0];
        private int comparedLeft = -1;
        private int comparedRight = -1;
        private int pivotIndex = -1;

        private SortingBarsPanel() {
            setBackground(PANEL_BACKGROUND);
            setPreferredSize(new Dimension(640, 310));
            setMinimumSize(new Dimension(320, 260));
        }

        private void render(int[] nextValues, int left, int right, int pivot, boolean[] sortedElements) {
            values = nextValues.clone();
            comparedLeft = left;
            comparedRight = right;
            pivotIndex = pivot;
            sorted = sortedElements.clone();
            int preferredWidth = Math.max(640, values.length * 42 + 32);
            setPreferredSize(new Dimension(preferredWidth, 310));
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            if (values.length == 0) {
                g2.setColor(BODY_TEXT);
                g2.setFont(font(Font.PLAIN, 12));
                g2.drawString("No values to display.", 18, getHeight() / 2);
                g2.dispose();
                return;
            }

            int minimum = values[0];
            int maximum = values[0];
            for (int value : values) {
                minimum = Math.min(minimum, value);
                maximum = Math.max(maximum, value);
            }

            int horizontalPadding = 12;
            int usableWidth = Math.max(1, getWidth() - horizontalPadding * 2);
            int gap = values.length > 1 ? Math.min(GAP, usableWidth / (values.length * 2)) : 0;
            int barWidth = Math.max(MIN_BAR_WIDTH,
                    Math.min(42, (usableWidth - gap * (values.length - 1)) / values.length));
            int totalWidth = barWidth * values.length + gap * (values.length - 1);
            int startX = Math.max(horizontalPadding, (getWidth() - totalWidth) / 2);
            int baseline = getHeight() - 28;
            int barAreaHeight = Math.max(1, baseline - 48);
            double range = (double) maximum - minimum;

            g2.setFont(font(Font.PLAIN, 10));
            for (int index = 0; index < values.length; index++) {
                int value = values[index];
                double ratio = range == 0 ? 0.5 : (value - (double) minimum) / range;
                int barHeight = (int) Math.round(MIN_BAR_HEIGHT
                        + ratio * (MAX_BAR_HEIGHT - MIN_BAR_HEIGHT));
                barHeight = Math.min(barAreaHeight, barHeight);
                int x = startX + index * (barWidth + gap);
                int y = baseline - barHeight;

                if (index == pivotIndex) {
                    g2.setColor(PIVOT_COLOR);
                } else if (index == comparedLeft || index == comparedRight) {
                    g2.setColor(COMPARED_COLOR);
                } else if (index < sorted.length && sorted[index]) {
                    g2.setColor(SORTED_COLOR);
                } else {
                    g2.setColor(BAR_COLOR);
                }
                g2.fillRoundRect(x, y, barWidth, barHeight, 8, 8);

                String valueText = Integer.toString(value);
                int textWidth = g2.getFontMetrics().stringWidth(valueText);
                g2.setColor(new Color(0xaab5cb));
                g2.drawString(valueText, x + Math.max(0, (barWidth - textWidth) / 2), y - 6);
            }

            g2.setColor(new Color(0x26334c));
            g2.drawLine(horizontalPadding, baseline, getWidth() - horizontalPadding, baseline);
            g2.dispose();
        }
    }
}
