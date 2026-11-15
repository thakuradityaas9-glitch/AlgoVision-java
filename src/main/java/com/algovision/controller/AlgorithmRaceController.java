package com.algovision.controller;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;

import com.algovision.algorithms.BubbleSort;
import com.algovision.algorithms.InsertionSort;
import com.algovision.algorithms.MergeSort;
import com.algovision.algorithms.QuickSort;
import com.algovision.algorithms.SelectionSort;
import com.algovision.algorithms.SortingAlgorithm;
import com.algovision.model.SortOperation;
import com.algovision.model.SortStep;
import com.algovision.race.SortRacePair;
import com.algovision.race.SortRaceResult;
import com.algovision.race.SortRaceRunner;
import com.algovision.ui.AlgoVisionTheme;
import com.algovision.ui.ScrollablePanel;
import com.algovision.visualization.RaceBarsView;

public final class AlgorithmRaceController extends JPanel {
    private static final Color APP_BACKGROUND = AlgoVisionTheme.APP_BACKGROUND;
    private static final Color PANEL_BACKGROUND = AlgoVisionTheme.PANEL_BACKGROUND;
    private static final Color CONTROL_BACKGROUND = AlgoVisionTheme.CONTROL_BACKGROUND;
    private static final Color BORDER_COLOR = AlgoVisionTheme.BORDER;
    private static final Color PRIMARY_TEXT = AlgoVisionTheme.TEXT_PRIMARY;
    private static final Color BODY_TEXT = AlgoVisionTheme.TEXT_BODY;
    private static final Color MUTED_TEXT = AlgoVisionTheme.TEXT_MUTED;
    private static final Color ACCENT = AlgoVisionTheme.ACCENT;
    private static final int MAX_ARRAY_SIZE = 50;
    private static final int STEP_INTERVAL_MILLIS = 90;

    private final List<SortingAlgorithm> algorithms = List.of(
            new BubbleSort(),
            new SelectionSort(),
            new InsertionSort(),
            new MergeSort(),
            new QuickSort());
    private final SortRaceRunner raceRunner = new SortRaceRunner();
    private final JComboBox<SortingAlgorithm> algorithmAChoice = new JComboBox<>();
    private final JComboBox<SortingAlgorithm> algorithmBChoice = new JComboBox<>();
    private final JComboBox<Integer> arraySizeChoice = new JComboBox<>();
    private final JTextField arrayInput = new JTextField();
    private final JSlider speedSlider = new JSlider(0, 100, 60);
    private final JLabel raceStatusValue = new JLabel("Ready");
    private final JLabel resultHeading = new JLabel();
    private final JTextArea resultDetails = new JTextArea();
    private final JLabel messageValue = new JLabel(
            "Generate an array or enter comma-separated integers, then start the race.");
    private final JButton generateArrayButton = new JButton("Generate Random Array");
    private final JButton startRaceButton = new JButton("Start Race");
    private final JButton pauseButton = new JButton("Pause");
    private final JButton resumeButton = new JButton("Resume");
    private final JButton resetButton = new JButton("Reset");
    private final RaceLane laneA = new RaceLane("ALGORITHM A");
    private final RaceLane laneB = new RaceLane("ALGORITHM B");
    private final JPanel resultBanner = new JPanel(new BorderLayout(0, 5));

    private SortingAlgorithm selectedAlgorithmA;
    private SortingAlgorithm selectedAlgorithmB;
    private int[] initialArray = {42, 18, 67, 29, 85, 36, 54, 11, 73, 24, 91, 48};
    private String raceStatus = "Ready";
    private SwingWorker<SortRacePair, Void> preparationWorker;
    private long raceGeneration;
    private boolean syncingAlgorithmChoices;

    public AlgorithmRaceController() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("AlgorithmRaceController must be created on the EDT.");
        }
        selectedAlgorithmA = algorithms.getFirst();
        selectedAlgorithmB = algorithms.getLast();
        setLayout(new BorderLayout());
        setBackground(APP_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 22, 18, 22));

        algorithmAChoice.setModel(new javax.swing.DefaultComboBoxModel<>(
                algorithms.toArray(SortingAlgorithm[]::new)));
        algorithmBChoice.setModel(new javax.swing.DefaultComboBoxModel<>(
                algorithms.toArray(SortingAlgorithm[]::new)));
        algorithmAChoice.setSelectedItem(selectedAlgorithmA);
        algorithmBChoice.setSelectedItem(selectedAlgorithmB);
        configureAlgorithmChoice(algorithmAChoice);
        configureAlgorithmChoice(algorithmBChoice);
        arraySizeChoice.setModel(new javax.swing.DefaultComboBoxModel<>(
                new Integer[] {8, 12, 16, 20, 24, 30, 40, 50}));
        arraySizeChoice.setSelectedItem(12);
        AlgoVisionTheme.styleComboBox(arraySizeChoice);
        arrayInput.setText(join(initialArray));
        arrayInput.setToolTipText("Enter up to 50 comma-separated integers.");

        laneA.setAlgorithm(selectedAlgorithmA);
        laneB.setAlgorithm(selectedAlgorithmB);
        laneA.renderInitial(initialArray);
        laneB.renderInitial(initialArray);
        configureListeners();
        add(createWorkspace(), BorderLayout.CENTER);
        updateControls();
    }

    public void pause() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::pause);
            return;
        }
        if ("Preparing".equals(raceStatus)) {
            raceGeneration++;
            cancelPreparation();
            laneA.reset(initialArray);
            laneB.reset(initialArray);
            setRaceStatus("Ready");
            setMessage("Race preparation was canceled while leaving the race screen.");
            updateControls();
            return;
        }
        if (!"Running".equals(raceStatus)) {
            return;
        }
        pauseLane(laneA);
        pauseLane(laneB);
        setRaceStatus("Paused");
        setMessage("Race paused. Resume both visualizations or reset the race.");
        updateControls();
    }

    private JScrollPane createWorkspace() {
        JPanel workspace = new ScrollablePanel();
        workspace.setLayout(new BorderLayout(0, 13));
        workspace.setBackground(APP_BACKGROUND);
        workspace.setBorder(BorderFactory.createEmptyBorder(20, 22, 18, 22));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(createHeader());
        top.add(Box.createVerticalStrut(13));
        top.add(createControls());
        workspace.add(top, BorderLayout.NORTH);

        JPanel lanes = new JPanel(new GridLayout(1, 2, 13, 0));
        lanes.setOpaque(false);
        lanes.add(laneA.panel);
        lanes.add(laneB.panel);
        workspace.add(lanes, BorderLayout.CENTER);
        workspace.add(createStatusPanel(), BorderLayout.SOUTH);
        JScrollPane scrollPane = new JScrollPane(workspace);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(APP_BACKGROUND);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return scrollPane;
    }

    private JPanel createHeader() {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JLabel eyebrow = label("SORTING ALGORITHMS", ACCENT, Font.BOLD, 11);
        JLabel heading = label("Algorithm Race Mode", PRIMARY_TEXT, Font.BOLD, 26);
        JLabel description = label(
                "Compare two algorithms on identical data. Execution timings are measured independently before playback.",
                BODY_TEXT, Font.PLAIN, 13);
        header.add(eyebrow);
        header.add(Box.createVerticalStrut(4));
        header.add(heading);
        header.add(Box.createVerticalStrut(3));
        header.add(description);
        return header;
    }

    private JPanel createControls() {
        JPanel controls = new JPanel();
        controls.setBackground(PANEL_BACKGROUND);
        controls.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(12, 13, 11, 13)));
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));

        JPanel selections = new JPanel(new GridLayout(1, 4, 10, 0));
        selections.setOpaque(false);
        selections.add(labeledControl("ALGORITHM A", algorithmAChoice));
        selections.add(labeledControl("ALGORITHM B", algorithmBChoice));
        selections.add(labeledControl("RANDOM ARRAY SIZE", arraySizeChoice));
        JPanel generateField = labeledControl("ARRAY", generateArrayButton);
        selections.add(generateField);

        arrayInput.setFont(font(Font.PLAIN, 13));
        arrayInput.setForeground(PRIMARY_TEXT);
        arrayInput.setCaretColor(PRIMARY_TEXT);
        arrayInput.setBackground(CONTROL_BACKGROUND);
        arrayInput.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(7, 9, 7, 9)));
        JPanel inputRow = labeledControl("SHARED INPUT ARRAY (COMMA-SEPARATED)", arrayInput);
        inputRow.setBorder(BorderFactory.createEmptyBorder(9, 0, 0, 0));

        styleButton(startRaceButton, true);
        styleButton(pauseButton, false);
        styleButton(resumeButton, false);
        styleButton(resetButton, false);
        styleButton(generateArrayButton, false);

        JPanel playback = new JPanel();
        playback.setOpaque(false);
        playback.setLayout(new BoxLayout(playback, BoxLayout.Y_AXIS));
        playback.setBorder(BorderFactory.createEmptyBorder(9, 0, 0, 0));
        JPanel playbackButtons = new JPanel(new GridLayout(1, 4, 7, 0));
        playbackButtons.setOpaque(false);
        playbackButtons.add(startRaceButton);
        playbackButtons.add(pauseButton);
        playbackButtons.add(resumeButton);
        playbackButtons.add(resetButton);

        JPanel speedRow = new JPanel(new BorderLayout(8, 0));
        speedRow.setOpaque(false);
        speedRow.add(label("ANIMATION SPEED", BODY_TEXT, Font.BOLD, 10), BorderLayout.WEST);
        speedSlider.setOpaque(false);
        speedSlider.setForeground(ACCENT);
        speedSlider.setToolTipText("Adjust both animation speeds.");
        speedSlider.setPreferredSize(new Dimension(160, 34));
        speedSlider.setMaximumSize(new Dimension(220, 34));
        speedRow.add(speedSlider, BorderLayout.CENTER);
        playback.add(playbackButtons);
        playback.add(Box.createVerticalStrut(8));
        playback.add(speedRow);

        JLabel timingNote = label(
                "Execution time includes step recording but excludes animation; one run is not a universal algorithm ranking.",
                MUTED_TEXT, Font.PLAIN, 11);
        timingNote.setBorder(BorderFactory.createEmptyBorder(7, 0, 0, 0));

        controls.add(selections);
        controls.add(inputRow);
        controls.add(playback);
        controls.add(timingNote);
        return controls;
    }

    private JPanel createStatusPanel() {
        JPanel status = new JPanel();
        status.setBackground(PANEL_BACKGROUND);
        status.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(9, 12, 9, 12)));
        status.setLayout(new BoxLayout(status, BoxLayout.Y_AXIS));

        JPanel statusRow = new JPanel(new BorderLayout(8, 0));
        statusRow.setOpaque(false);
        statusRow.add(label("RACE STATUS", MUTED_TEXT, Font.BOLD, 10), BorderLayout.WEST);
        raceStatusValue.setForeground(ACCENT);
        raceStatusValue.setFont(font(Font.BOLD, 12));
        statusRow.add(raceStatusValue, BorderLayout.CENTER);

        resultBanner.setBackground(new Color(0x202c44));
        resultBanner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x3c4771)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        resultHeading.setForeground(PRIMARY_TEXT);
        resultHeading.setFont(font(Font.BOLD, 14));
        resultDetails.setEditable(false);
        resultDetails.setLineWrap(true);
        resultDetails.setWrapStyleWord(true);
        resultDetails.setOpaque(false);
        resultDetails.setForeground(BODY_TEXT);
        resultDetails.setFont(font(Font.PLAIN, 11));
        resultDetails.setFocusable(false);
        resultBanner.add(resultHeading, BorderLayout.NORTH);
        resultBanner.add(resultDetails, BorderLayout.CENTER);
        resultBanner.setVisible(false);

        messageValue.setForeground(BODY_TEXT);
        messageValue.setFont(font(Font.PLAIN, 12));
        messageValue.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        status.add(statusRow);
        status.add(resultBanner);
        status.add(messageValue);
        return status;
    }

    private JPanel labeledControl(String title, JComponent control) {
        JPanel field = new JPanel();
        field.setOpaque(false);
        field.setLayout(new BoxLayout(field, BoxLayout.Y_AXIS));
        JLabel titleLabel = label(title, MUTED_TEXT, Font.BOLD, 10);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        field.add(titleLabel);
        if (control instanceof JComboBox<?> comboBox) {
            comboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
            comboBox.setPreferredSize(new Dimension(130, 34));
        } else if (control instanceof JButton button) {
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
            button.setPreferredSize(new Dimension(130, 34));
        } else if (control instanceof JTextField textField) {
            textField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
            textField.setPreferredSize(new Dimension(250, 34));
        }
        field.add(control);
        return field;
    }

    private void configureAlgorithmChoice(JComboBox<SortingAlgorithm> choice) {
        AlgoVisionTheme.styleComboBox(choice);
        choice.setFont(font(Font.PLAIN, 12));
        choice.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setText(value instanceof SortingAlgorithm algorithm ? algorithm.name() : "");
                setForeground(isSelected ? PRIMARY_TEXT : BODY_TEXT);
                setBackground(isSelected ? new Color(0x35345f) : CONTROL_BACKGROUND);
                setBorder(BorderFactory.createEmptyBorder(4, 7, 4, 7));
                return this;
            }
        });
    }

    private void configureListeners() {
        algorithmAChoice.addActionListener(event -> algorithmChoicesChanged());
        algorithmBChoice.addActionListener(event -> algorithmChoicesChanged());
        generateArrayButton.addActionListener(event -> generateRandomArray());
        startRaceButton.addActionListener(event -> startRace());
        pauseButton.addActionListener(event -> pause());
        resumeButton.addActionListener(event -> resume());
        resetButton.addActionListener(event -> resetRace());
        speedSlider.addChangeListener(event -> updateAnimationSpeed());
    }

    private void algorithmChoicesChanged() {
        if (syncingAlgorithmChoices) {
            return;
        }
        if (isRaceActive()) {
            syncingAlgorithmChoices = true;
            algorithmAChoice.setSelectedItem(selectedAlgorithmA);
            algorithmBChoice.setSelectedItem(selectedAlgorithmB);
            syncingAlgorithmChoices = false;
            setMessage("Algorithm selections are locked while a race is active.");
            return;
        }

        SortingAlgorithm first = (SortingAlgorithm) algorithmAChoice.getSelectedItem();
        SortingAlgorithm second = (SortingAlgorithm) algorithmBChoice.getSelectedItem();
        if (first == null || second == null || first == second) {
            syncingAlgorithmChoices = true;
            algorithmAChoice.setSelectedItem(selectedAlgorithmA);
            algorithmBChoice.setSelectedItem(selectedAlgorithmB);
            syncingAlgorithmChoices = false;
            setMessage("Choose two different sorting algorithms for the race.");
            updateControls();
            return;
        }
        selectedAlgorithmA = first;
        selectedAlgorithmB = second;
        resetRace();
        laneA.setAlgorithm(selectedAlgorithmA);
        laneB.setAlgorithm(selectedAlgorithmB);
        laneA.renderInitial(initialArray);
        laneB.renderInitial(initialArray);
    }

    private void generateRandomArray() {
        if (isRaceActive()) {
            return;
        }
        resetRace();
        int size = (Integer) arraySizeChoice.getSelectedItem();
        int[] values = new java.util.Random().ints(size, -100, 101).toArray();
        initialArray = values;
        arrayInput.setText(join(values));
        laneA.renderInitial(initialArray);
        laneB.renderInitial(initialArray);
        setMessage("Generated " + size
                + " values. Both algorithms will receive identical array copies.");
    }

    private void startRace() {
        if (isRaceActive()) {
            return;
        }
        if ("Completed".equals(raceStatus)) {
            resetRace();
        }
        if (selectedAlgorithmA == selectedAlgorithmB) {
            setMessage("Choose two different sorting algorithms before starting the race.");
            updateControls();
            return;
        }

        final int[] input;
        try {
            input = parseArray(arrayInput.getText());
        } catch (IllegalArgumentException exception) {
            setRaceStatus("Ready");
            setMessage(exception.getMessage());
            updateControls();
            return;
        }

        initialArray = input.clone();
        laneA.reset(initialArray);
        laneB.reset(initialArray);
        laneA.setAlgorithm(selectedAlgorithmA);
        laneB.setAlgorithm(selectedAlgorithmB);
        laneA.setStatus("Preparing");
        laneB.setStatus("Preparing");
        clearResultBanner();
        setRaceStatus("Preparing");
        setMessage("Preparing both algorithm runs concurrently before animation.");
        updateControls();

        long generation = ++raceGeneration;
        SortingAlgorithm firstAlgorithm = selectedAlgorithmA;
        SortingAlgorithm secondAlgorithm = selectedAlgorithmB;
        SwingWorker<SortRacePair, Void> worker = new SwingWorker<>() {
            @Override
            protected SortRacePair doInBackground() throws Exception {
                return raceRunner.runConcurrently(
                        firstAlgorithm, secondAlgorithm, input.clone());
            }

            @Override
            protected void done() {
                if (generation != raceGeneration || preparationWorker != this) {
                    return;
                }
                preparationWorker = null;
                try {
                    SortRacePair results = get();
                    laneA.prepare(results.first(), input);
                    laneB.prepare(results.second(), input);
                    startAnimations(generation);
                } catch (CancellationException exception) {
                    laneA.reset(initialArray);
                    laneB.reset(initialArray);
                    setRaceStatus("Ready");
                    setMessage("Race preparation was canceled.");
                    updateControls();
                } catch (InterruptedException exception) {
                    failRace("Race preparation was interrupted.");
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    failRace("Could not prepare the race: "
                            + (cause == null ? exception.getMessage() : cause.getMessage()));
                }
            }
        };
        preparationWorker = worker;
        worker.execute();
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

    private void startAnimations(long generation) {
        laneA.startTimer(generation);
        laneB.startTimer(generation);
        setRaceStatus("Running");
        setMessage("Race in progress. Both lanes animate independently.");
        updateControls();
    }

    private void advanceLane(RaceLane lane, long generation, Timer timer) {
        if (generation != raceGeneration || lane.animationTimer != timer) {
            return;
        }
        if (lane.result == null || lane.stepIndex >= lane.result.steps().size()) {
            finishLane(lane, generation);
            return;
        }

        SortStep step = lane.result.steps().get(lane.stepIndex++);
        lane.acceptStep(step);
        if (lane.stepIndex >= lane.result.steps().size()
                || step.operation() == SortOperation.COMPLETE) {
            finishLane(lane, generation);
        }
    }

    private void finishLane(RaceLane lane, long generation) {
        lane.stopTimer();
        lane.markCompleted();
        lane.setStatus("Completed");
        if (laneA.isComplete() && laneB.isComplete() && generation == raceGeneration) {
            finishRace();
        }
    }

    private void finishRace() {
        setRaceStatus("Completed");
        showRaceResult();
        setMessage("Both algorithms completed. Compare the independent timings and operation counts.");
        updateControls();
    }

    private void showRaceResult() {
        SortRaceResult first = laneA.result;
        SortRaceResult second = laneB.result;
        SortRacePair pair = new SortRacePair(first, second);
        if (pair.isTie()) {
            resultHeading.setText("This run is a tie");
            resultDetails.setText("Both measured " + formatExecutionTime(first.executionTimeNanos())
                    + ". Timing varies between runs and is not a universal ranking.");
        } else {
            SortRaceResult winner = pair.winner().orElseThrow();
            SortRaceResult other = winner == first ? second : first;
            long difference = other.executionTimeNanos() - winner.executionTimeNanos();
            resultHeading.setText(winner.algorithm().name() + " wins this run");
            resultDetails.setText(winner.algorithm().name() + " measured "
                    + formatExecutionTime(winner.executionTimeNanos()) + " versus "
                    + formatExecutionTime(other.executionTimeNanos()) + " for "
                    + other.algorithm().name() + " (difference "
                    + formatExecutionTime(difference) + "). "
                    + winner.algorithm().name() + " recorded " + winner.comparisons()
                    + " comparisons, " + winner.swaps() + " swaps and "
                    + winner.movements() + " moves; " + other.algorithm().name()
                    + " recorded " + other.comparisons() + " comparisons, "
                    + other.swaps() + " swaps and " + other.movements()
                    + " moves. A single measured run is approximate.");
        }
        resultBanner.setVisible(true);
    }

    private void failRace(String message) {
        laneA.stopTimer();
        laneB.stopTimer();
        laneA.setStatus("Failed");
        laneB.setStatus("Failed");
        setRaceStatus("Failed");
        setMessage(message == null || message.isBlank()
                ? "Race preparation failed with an unspecified error."
                : message);
        updateControls();
    }

    private void resetRace() {
        raceGeneration++;
        cancelPreparation();
        laneA.reset(initialArray);
        laneB.reset(initialArray);
        clearResultBanner();
        setRaceStatus("Ready");
        setMessage("Race reset. Both lanes are ready to run again.");
        updateControls();
    }

    private void cancelPreparation() {
        if (preparationWorker != null) {
            preparationWorker.cancel(true);
            preparationWorker = null;
        }
    }

    private void clearResultBanner() {
        resultHeading.setText("");
        resultDetails.setText("");
        resultBanner.setVisible(false);
    }

    private void pauseLane(RaceLane lane) {
        if (lane.animationTimer != null && "Running".equals(lane.status)) {
            lane.animationTimer.stop();
            lane.setStatus("Paused");
        }
    }

    private void resume() {
        if (!"Paused".equals(raceStatus)) {
            return;
        }
        long generation = raceGeneration;
        resumeLane(laneA, generation);
        resumeLane(laneB, generation);
        setRaceStatus("Running");
        setMessage("Race resumed. Each lane continues from its current animation step.");
        updateControls();
    }

    private void resumeLane(RaceLane lane, long generation) {
        if ("Paused".equals(lane.status)) {
            lane.startTimer(generation);
        }
    }

    private void updateAnimationSpeed() {
        int delay = animationDelay();
        updateTimerDelay(laneA, delay);
        updateTimerDelay(laneB, delay);
    }

    private void updateTimerDelay(RaceLane lane, int delay) {
        if (lane.animationTimer != null) {
            lane.animationTimer.setDelay(delay);
            lane.animationTimer.setInitialDelay(delay);
        }
    }

    private int animationDelay() {
        double rate = 0.35 + speedSlider.getValue() / 100.0 * 2.65;
        return Math.max(1, (int) Math.round(STEP_INTERVAL_MILLIS / rate));
    }

    private void updateControls() {
        boolean active = isRaceActive();
        boolean running = "Running".equals(raceStatus);
        boolean paused = "Paused".equals(raceStatus);
        algorithmAChoice.setEnabled(!active);
        algorithmBChoice.setEnabled(!active);
        arraySizeChoice.setEnabled(!active);
        arrayInput.setEnabled(!active);
        generateArrayButton.setEnabled(!active);
        startRaceButton.setEnabled(!active);
        pauseButton.setEnabled(running || "Preparing".equals(raceStatus));
        resumeButton.setEnabled(paused);
        resetButton.setEnabled(!"Ready".equals(raceStatus));
        speedSlider.setEnabled(running || paused);
        revalidate();
        repaint();
    }

    private boolean isRaceActive() {
        return "Preparing".equals(raceStatus)
                || "Running".equals(raceStatus)
                || "Paused".equals(raceStatus);
    }

    private void setRaceStatus(String status) {
        raceStatus = status;
        raceStatusValue.setText(status);
        raceStatusValue.setForeground(switch (status) {
            case "Running" -> AlgoVisionTheme.SUCCESS;
            case "Paused" -> AlgoVisionTheme.WARNING;
            case "Completed" -> AlgoVisionTheme.ACCENT;
            case "Failed" -> AlgoVisionTheme.ERROR;
            default -> AlgoVisionTheme.TEXT_BODY;
        });
    }

    private void setMessage(String message) {
        messageValue.setText(message);
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
        AlgoVisionTheme.styleButton(button, primary, 96);
    }

    private String formatExecutionTime(long nanoseconds) {
        return String.format(Locale.ROOT, "%.3f ms", nanoseconds / 1_000_000.0);
    }

    private String join(int[] values) {
        return Arrays.stream(values)
                .mapToObj(Integer::toString)
                .collect(Collectors.joining(", "));
    }

    String raceStatusForTesting() {
        return raceStatus;
    }

    String laneStatusForTesting(boolean firstLane) {
        return (firstLane ? laneA : laneB).status;
    }

    int laneStepIndexForTesting(boolean firstLane) {
        return (firstLane ? laneA : laneB).stepIndex;
    }

    int laneComparisonsForTesting(boolean firstLane) {
        return (firstLane ? laneA : laneB).comparisons;
    }

    int laneSwapsForTesting(boolean firstLane) {
        return (firstLane ? laneA : laneB).swaps;
    }

    int laneMovementsForTesting(boolean firstLane) {
        return (firstLane ? laneA : laneB).movements;
    }

    boolean laneTimerRunningForTesting(boolean firstLane) {
        Timer timer = (firstLane ? laneA : laneB).animationTimer;
        return timer != null && timer.isRunning();
    }

    int laneTimerDelayForTesting(boolean firstLane) {
        Timer timer = (firstLane ? laneA : laneB).animationTimer;
        return timer == null ? -1 : timer.getDelay();
    }

    boolean preparationRunningForTesting() {
        return preparationWorker != null && !preparationWorker.isDone();
    }

    long raceGenerationForTesting() {
        return raceGeneration;
    }

    private final class RaceLane {
        private final JLabel laneTitle;
        private final JLabel algorithmName = new JLabel();
        private final JLabel comparisonsValue = new JLabel("0");
        private final JLabel swapsValue = new JLabel("0");
        private final JLabel movementsValue = new JLabel("0");
        private final JLabel operationsValue = new JLabel("0");
        private final JLabel executionTimeValue = new JLabel("--");
        private final JLabel statusValue = new JLabel("Ready");
        private final JLabel complexityValue = new JLabel();
        private final RaceBarsView barsView = new RaceBarsView();
        private final JPanel panel = new JPanel(new BorderLayout(0, 10));

        private SortingAlgorithm algorithm;
        private SortRaceResult result;
        private Timer animationTimer;
        private int stepIndex;
        private int comparisons;
        private int swaps;
        private int movements;
        private int operations;
        private String status = "Ready";

        private RaceLane(String title) {
            laneTitle = label(title, ACCENT, Font.BOLD, 10);
            createPanel();
        }

        private void createPanel() {
            panel.setBackground(PANEL_BACKGROUND);
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_COLOR),
                    BorderFactory.createEmptyBorder(12, 13, 12, 13)));

            JPanel heading = new JPanel();
            heading.setOpaque(false);
            heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
            algorithmName.setForeground(PRIMARY_TEXT);
            algorithmName.setFont(font(Font.BOLD, 17));
            heading.add(laneTitle);
            heading.add(Box.createVerticalStrut(3));
            heading.add(algorithmName);
            panel.add(heading, BorderLayout.NORTH);
            panel.add(barsView, BorderLayout.CENTER);

            JPanel footer = new JPanel();
            footer.setOpaque(false);
            footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
            JPanel stats = new JPanel(new GridLayout(2, 3, 7, 7));
            stats.setOpaque(false);
            stats.add(createStatCard("COMPARISONS", comparisonsValue));
            stats.add(createStatCard("SWAPS", swapsValue));
            stats.add(createStatCard("MOVES", movementsValue));
            stats.add(createStatCard("OPERATIONS", operationsValue));
            stats.add(createStatCard("EXECUTION TIME", executionTimeValue));
            stats.add(createStatCard("STATUS", statusValue));
            footer.add(stats);

            complexityValue.setForeground(BODY_TEXT);
            complexityValue.setFont(font(Font.PLAIN, 10));
            complexityValue.setBorder(BorderFactory.createEmptyBorder(8, 1, 0, 0));
            footer.add(complexityValue);
            panel.add(footer, BorderLayout.SOUTH);
        }

        private JPanel createStatCard(String title, JLabel value) {
            JPanel card = new JPanel();
            card.setBackground(CONTROL_BACKGROUND);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_COLOR),
                    BorderFactory.createEmptyBorder(6, 7, 6, 7)));
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            JLabel titleLabel = label(title, MUTED_TEXT, Font.BOLD, 9);
            titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            value.setForeground(PRIMARY_TEXT);
            value.setFont(font(Font.BOLD, 14));
            value.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(titleLabel);
            card.add(Box.createVerticalStrut(3));
            card.add(value);
            return card;
        }

        private void setAlgorithm(SortingAlgorithm nextAlgorithm) {
            algorithm = nextAlgorithm;
            algorithmName.setText(nextAlgorithm.name());
            complexityValue.setText("Best " + nextAlgorithm.bestCaseTimeComplexity()
                    + "  |  Average " + nextAlgorithm.averageCaseTimeComplexity()
                    + "  |  Worst " + nextAlgorithm.worstCaseTimeComplexity()
                    + "  |  Space " + nextAlgorithm.auxiliarySpaceComplexity());
        }

        private void prepare(SortRaceResult raceResult, int[] array) {
            result = raceResult;
            stepIndex = 0;
            comparisons = 0;
            swaps = 0;
            movements = 0;
            operations = 0;
            executionTimeValue.setText(formatExecutionTime(result.executionTimeNanos()));
            setStatus("Ready");
            renderInitial(array);
        }

        private void renderInitial(int[] array) {
            barsView.render(array, -1, -1, -1, new boolean[array.length]);
            comparisonsValue.setText("0");
            swapsValue.setText("0");
            movementsValue.setText("0");
            operationsValue.setText("0");
            if (result == null) {
                executionTimeValue.setText("--");
            }
        }

        private void startTimer(long generation) {
            if (animationTimer == null) {
                Timer timer = new Timer(animationDelay(), null);
                timer.setCoalesce(true);
                timer.addActionListener(event -> advanceLane(this, generation, timer));
                animationTimer = timer;
            }
            animationTimer.setDelay(animationDelay());
            animationTimer.setInitialDelay(animationDelay());
            setStatus("Running");
            animationTimer.start();
        }

        private void acceptStep(SortStep step) {
            switch (step.operation()) {
                case COMPARE -> comparisons++;
                case SWAP -> swaps++;
                case MOVE, OVERWRITE -> movements++;
                default -> {
                }
            }
            operations = comparisons + swaps + movements;
            boolean swapped = step.operation() == SortOperation.SWAP || step.swapOccurred();
            barsView.render(step.arrayState(), step.comparedIndexLeft(), step.comparedIndexRight(),
                    swapped ? step.comparedIndexLeft() : -1,
                    swapped ? step.comparedIndexRight() : -1,
                    step.pivotIndex(), step.sortedElements());
            comparisonsValue.setText(Integer.toString(comparisons));
            swapsValue.setText(Integer.toString(swaps));
            movementsValue.setText(Integer.toString(movements));
            operationsValue.setText(Integer.toString(operations));
        }

        private void setStatus(String value) {
            status = value;
            statusValue.setText(value);
            statusValue.setForeground("Failed".equals(value)
                    ? new Color(0xf07f87)
                    : ("Completed".equals(value) ? new Color(0x69d2a4) : PRIMARY_TEXT));
        }

        private boolean isComplete() {
            return "Completed".equals(status);
        }

        private void stopTimer() {
            if (animationTimer != null) {
                animationTimer.stop();
                animationTimer = null;
            }
        }

        private void markCompleted() {
            barsView.markCompleted();
        }

        private void reset(int[] array) {
            stopTimer();
            result = null;
            stepIndex = 0;
            comparisons = 0;
            swaps = 0;
            movements = 0;
            operations = 0;
            setStatus("Ready");
            renderInitial(array);
        }
    }
}
