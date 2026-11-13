package com.algovision.controller;

import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Collectors;

import com.algovision.algorithms.BubbleSort;
import com.algovision.algorithms.InsertionSort;
import com.algovision.algorithms.MergeSort;
import com.algovision.algorithms.QuickSort;
import com.algovision.algorithms.SelectionSort;
import com.algovision.algorithms.SortingAlgorithm;
import com.algovision.model.SortOperation;
import com.algovision.model.SortStep;
import com.algovision.race.SortRaceResult;
import com.algovision.race.SortRaceRunner;
import com.algovision.visualization.ArrayBarsView;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public final class AlgorithmRaceController {
    private static final int MAX_ARRAY_SIZE = 50;
    private static final double STEP_INTERVAL_MILLIS = 90;

    private final List<SortingAlgorithm> algorithms = List.of(
            new BubbleSort(),
            new SelectionSort(),
            new InsertionSort(),
            new MergeSort(),
            new QuickSort());
    private final SortRaceRunner raceRunner = new SortRaceRunner();
    private final ComboBox<SortingAlgorithm> algorithmAChoice = new ComboBox<>();
    private final ComboBox<SortingAlgorithm> algorithmBChoice = new ComboBox<>();
    private final ComboBox<Integer> arraySizeChoice = new ComboBox<>();
    private final TextField arrayInput = new TextField();
    private final Slider speedSlider = new Slider(0, 100, 60);
    private final Label raceStatusValue = new Label("Ready");
    private final Label resultHeading = new Label();
    private final Label resultDetails = new Label();
    private final Label messageValue = new Label("Generate an array or enter comma-separated integers, then start the race.");
    private final Button generateArrayButton = new Button("Generate Random Array");
    private final Button startRaceButton = new Button("Start Race");
    private final Button pauseButton = new Button("Pause");
    private final Button resumeButton = new Button("Resume");
    private final Button resetButton = new Button("Reset");

    private final RaceLane laneA = new RaceLane("ALGORITHM A");
    private final RaceLane laneB = new RaceLane("ALGORITHM B");
    private final VBox resultBanner = new VBox(7, resultHeading, resultDetails);
    private final ScrollPane view;

    private int[] initialArray = {42, 18, 67, 29, 85, 36, 54, 11, 73, 24, 91, 48};
    private String raceStatus = "Ready";
    private Task<List<SortRaceResult>> preparationTask;
    private long raceGeneration;

    public AlgorithmRaceController() {
        view = createView();
        arrayInput.setText(join(initialArray));
        laneA.setAlgorithm(algorithms.get(0));
        laneB.setAlgorithm(algorithms.get(4));
        laneA.renderInitial(initialArray);
        laneB.renderInitial(initialArray);
        resultBanner.setVisible(false);
        resultBanner.setManaged(false);
        updateControls();
    }

    public Node getView() {
        return view;
    }

    public void pause() {
        if ("Preparing".equals(raceStatus)) {
            raceGeneration++;
            if (preparationTask != null) {
                preparationTask.cancel(true);
                preparationTask = null;
            }
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

    private ScrollPane createView() {
        VBox content = new VBox(16);
        content.getStyleClass().add("race-content");
        content.setPadding(new Insets(28, 30, 24, 30));

        VBox introduction = new VBox(7);
        Label eyebrow = new Label("SORTING ALGORITHMS");
        eyebrow.getStyleClass().add("eyebrow");
        Label heading = new Label("Algorithm Race Mode");
        heading.getStyleClass().add("welcome-heading");
        Label description = new Label(
                "Compare two sorting algorithms on identical data. Timing is measured before playback and is approximate.");
        description.getStyleClass().add("welcome-subtitle");
        description.setWrapText(true);
        introduction.getChildren().addAll(eyebrow, heading, description);

        HBox raceStatus = new HBox(8, new Label("RACE STATUS"), raceStatusValue);
        raceStatus.getStyleClass().add("race-status-strip");

        HBox lanes = new HBox(14, laneA.panel, laneB.panel);
        lanes.setAlignment(Pos.TOP_CENTER);
        laneA.panel.setMinWidth(0);
        laneB.panel.setMinWidth(0);
        HBox.setHgrow(laneA.panel, Priority.ALWAYS);
        HBox.setHgrow(laneB.panel, Priority.ALWAYS);

        resultBanner.getStyleClass().add("race-result-banner");
        resultHeading.getStyleClass().add("race-result-heading");
        resultDetails.getStyleClass().add("race-result-details");
        resultDetails.setWrapText(true);
        messageValue.getStyleClass().add("graph-message");
        messageValue.setWrapText(true);

        content.getChildren().addAll(
                introduction,
                createControls(),
                raceStatus,
                resultBanner,
                lanes,
                messageValue);
        VBox.setVgrow(lanes, Priority.ALWAYS);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("content-scroll");
        return scrollPane;
    }

    private VBox createControls() {
        VBox controls = new VBox(11);
        controls.getStyleClass().add("controls-card");
        controls.setPadding(new Insets(14));

        configureAlgorithmChoice(algorithmAChoice);
        configureAlgorithmChoice(algorithmBChoice);
        algorithmAChoice.getItems().setAll(algorithms);
        algorithmBChoice.getItems().setAll(algorithms);
        algorithmAChoice.getSelectionModel().select(algorithms.get(0));
        algorithmBChoice.getSelectionModel().select(algorithms.get(4));
        algorithmAChoice.setOnAction(event -> configureSelectedAlgorithms());
        algorithmBChoice.setOnAction(event -> configureSelectedAlgorithms());

        arraySizeChoice.getItems().setAll(8, 12, 16, 20, 24, 30, 40, 50);
        arraySizeChoice.getSelectionModel().select(Integer.valueOf(12));
        arraySizeChoice.setMaxWidth(Double.MAX_VALUE);
        arrayInput.setPromptText("e.g. 8, -3, 8, 1");
        arrayInput.getStyleClass().add("array-input");
        arrayInput.setMaxWidth(Double.MAX_VALUE);
        generateArrayButton.getStyleClass().add("secondary-button");
        generateArrayButton.setOnAction(event -> generateRandomArray());

        HBox selectionRow = new HBox(10,
                labeledControl("ALGORITHM A", algorithmAChoice),
                labeledControl("ALGORITHM B", algorithmBChoice),
                labeledControl("RANDOM ARRAY SIZE", arraySizeChoice),
                generateArrayButton);
        selectionRow.setAlignment(Pos.BOTTOM_LEFT);
        HBox.setHgrow(selectionRow.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(selectionRow.getChildren().get(1), Priority.ALWAYS);

        Label inputLabel = new Label("SHARED INPUT ARRAY (COMMA-SEPARATED)");
        inputLabel.getStyleClass().add("control-label");
        VBox inputField = new VBox(6, inputLabel, arrayInput);
        HBox.setHgrow(inputField, Priority.ALWAYS);
        HBox inputRow = new HBox(inputField);
        inputRow.setAlignment(Pos.BOTTOM_LEFT);

        startRaceButton.getStyleClass().add("primary-button");
        pauseButton.getStyleClass().add("secondary-button");
        resumeButton.getStyleClass().add("secondary-button");
        resetButton.getStyleClass().add("secondary-button");
        startRaceButton.setOnAction(event -> startRace());
        pauseButton.setOnAction(event -> pause());
        resumeButton.setOnAction(event -> resume());
        resetButton.setOnAction(event -> resetRace());

        Label speedLabel = new Label("ANIMATION SPEED");
        speedLabel.getStyleClass().add("control-label");
        speedSlider.setShowTickLabels(false);
        speedSlider.setShowTickMarks(false);
        speedSlider.setBlockIncrement(10);
        speedSlider.setPrefWidth(140);
        speedSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if ("Running".equals(raceStatus)) {
                updateTimelineRate(laneA, newValue.doubleValue());
                updateTimelineRate(laneB, newValue.doubleValue());
            }
        });
        HBox playbackRow = new HBox(9,
                startRaceButton, pauseButton, resumeButton, resetButton,
                createSpacer(), speedLabel, speedSlider);
        playbackRow.setAlignment(Pos.CENTER_LEFT);

        Label timingNote = new Label(
                "Execution time measures each algorithm's step-generation run with System.nanoTime; it excludes animation but includes step-recording overhead. Operations = comparisons + swaps + moves. Timings are approximate.");
        timingNote.getStyleClass().add("race-timing-note");
        timingNote.setWrapText(true);
        controls.getChildren().addAll(selectionRow, inputRow, playbackRow, timingNote);
        return controls;
    }

    private void configureAlgorithmChoice(ComboBox<SortingAlgorithm> choice) {
        choice.setMaxWidth(Double.MAX_VALUE);
        choice.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(SortingAlgorithm item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name());
            }
        });
        choice.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(SortingAlgorithm item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name());
            }
        });
    }

    private VBox labeledControl(String labelText, Node control) {
        VBox field = new VBox(6);
        Label label = new Label(labelText);
        label.getStyleClass().add("control-label");
        field.getChildren().addAll(label, control);
        HBox.setHgrow(field, Priority.ALWAYS);
        return field;
    }

    private Region createSpacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private VBox createStatCard(String title, Label value) {
        VBox card = new VBox(5, new Label(title), value);
        card.getStyleClass().add("race-stat-card");
        card.getChildren().get(0).getStyleClass().add("stat-title");
        value.getStyleClass().add("race-stat-value");
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private void configureSelectedAlgorithms() {
        if (isRaceActive()) {
            return;
        }
        resetRace();
        laneA.setAlgorithm(algorithmAChoice.getValue());
        laneB.setAlgorithm(algorithmBChoice.getValue());
        laneA.renderInitial(initialArray);
        laneB.renderInitial(initialArray);
    }

    private void generateRandomArray() {
        if (isRaceActive()) {
            return;
        }
        resetRace();
        int size = arraySizeChoice.getValue();
        Random random = new Random();
        int[] values = new int[size];
        for (int index = 0; index < size; index++) {
            values[index] = random.nextInt(201) - 100;
        }
        initialArray = values;
        arrayInput.setText(join(values));
        laneA.renderInitial(initialArray);
        laneB.renderInitial(initialArray);
        setMessage("Generated " + size + " values. Both algorithms will receive identical array copies.");
    }

    private void startRace() {
        if (isRaceActive()) {
            return;
        }
        if ("Completed".equals(raceStatus)) {
            resetRace();
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
        laneA.setAlgorithm(algorithmAChoice.getValue());
        laneB.setAlgorithm(algorithmBChoice.getValue());
        laneA.setStatus("Preparing");
        laneB.setStatus("Preparing");
        clearResultBanner();
        setRaceStatus("Preparing");
        setMessage("Preparing both algorithm runs before the visual race begins.");
        updateControls();

        long generation = ++raceGeneration;
        SortingAlgorithm firstAlgorithm = algorithmAChoice.getValue();
        SortingAlgorithm secondAlgorithm = algorithmBChoice.getValue();
        preparationTask = new Task<>() {
            @Override
            protected List<SortRaceResult> call() {
                SortRaceResult first = raceRunner.run(firstAlgorithm, input.clone());
                if (isCancelled()) {
                    return List.of();
                }
                SortRaceResult second = raceRunner.run(secondAlgorithm, input.clone());
                return List.of(first, second);
            }
        };
        preparationTask.setOnSucceeded(event -> {
            if (generation != raceGeneration) {
                return;
            }
            List<SortRaceResult> results = preparationTask.getValue();
            preparationTask = null;
            if (results.size() != 2) {
                setRaceStatus("Ready");
                laneA.setStatus("Ready");
                laneB.setStatus("Ready");
                setMessage("Race preparation was cancelled.");
                updateControls();
                return;
            }
            laneA.prepare(results.get(0), initialArray);
            laneB.prepare(results.get(1), initialArray);
            startAnimations();
        });
        preparationTask.setOnFailed(event -> {
            if (generation != raceGeneration) {
                return;
            }
            Throwable failure = preparationTask.getException();
            preparationTask = null;
            laneA.setStatus("Ready");
            laneB.setStatus("Ready");
            setRaceStatus("Failed");
            setMessage("Could not prepare the race: "
                    + (failure == null ? "unknown error" : failure.getMessage()));
            updateControls();
        });
        preparationTask.setOnCancelled(event -> {
            if (generation == raceGeneration) {
                preparationTask = null;
                setRaceStatus("Ready");
                updateControls();
            }
        });
        Thread worker = new Thread(preparationTask, "algovision-sort-race");
        worker.setDaemon(true);
        worker.start();
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

    private void startAnimations() {
        laneA.startTimeline(STEP_INTERVAL_MILLIS, playbackRate(speedSlider.getValue()),
                () -> advanceLane(laneA));
        laneB.startTimeline(STEP_INTERVAL_MILLIS, playbackRate(speedSlider.getValue()),
                () -> advanceLane(laneB));
        setRaceStatus("Running");
        setMessage("Race in progress. Each visualization advances independently.");
        updateControls();
    }

    private void advanceLane(RaceLane lane) {
        if (lane.result == null) {
            finishLane(lane);
            return;
        }
        if (lane.stepIndex >= lane.result.steps().size()) {
            finishLane(lane);
            return;
        }

        SortStep step = lane.result.steps().get(lane.stepIndex++);
        lane.acceptStep(step);
        if (lane.stepIndex >= lane.result.steps().size()
                || step.operation() == SortOperation.COMPLETE) {
            finishLane(lane);
        }
    }

    private void finishLane(RaceLane lane) {
        lane.stopTimeline();
        lane.setStatus("Completed");
        if (laneA.isComplete() && laneB.isComplete()) {
            finishRace();
        }
    }

    private void finishRace() {
        setRaceStatus("Completed");
        showRaceResult();
        setMessage("Both algorithms completed. Compare the counts and approximate execution times below.");
        updateControls();
    }

    private void showRaceResult() {
        SortRaceResult first = laneA.result;
        SortRaceResult second = laneB.result;
        if (first.executionTimeNanos() < second.executionTimeNanos()) {
            resultHeading.setText(first.algorithm().name() + " wins this run");
        } else if (second.executionTimeNanos() < first.executionTimeNanos()) {
            resultHeading.setText(second.algorithm().name() + " wins this run");
        } else {
            resultHeading.setText("This run is a tie");
        }

        long timeDifference = Math.abs(first.executionTimeNanos() - second.executionTimeNanos());
        String comparison;
        if (first.executionTimeNanos() == second.executionTimeNanos()) {
            comparison = "Both measured " + formatExecutionTime(first.executionTimeNanos()) + ".";
        } else {
            SortRaceResult faster = first.executionTimeNanos() < second.executionTimeNanos()
                    ? first
                    : second;
            SortRaceResult slower = faster == first ? second : first;
            comparison = faster.algorithm().name() + " measured "
                    + formatExecutionTime(faster.executionTimeNanos()) + " versus "
                    + formatExecutionTime(slower.executionTimeNanos()) + " for "
                    + slower.algorithm().name() + " (difference "
                    + formatExecutionTime(timeDifference) + ").";
        }
        SortRaceResult faster = first.executionTimeNanos() <= second.executionTimeNanos()
                ? first
                : second;
        SortRaceResult slower = faster == first ? second : first;
        resultDetails.setText(comparison + " "
                + first.algorithm().name() + ": " + first.comparisons() + " comparisons, "
                + first.swaps() + " swaps, " + first.movements() + " moves; "
                + second.algorithm().name() + ": " + second.comparisons() + " comparisons, "
                + second.swaps() + " swaps, " + second.movements() + " moves. "
                + explainObservedResult(faster, slower));
        resultBanner.setManaged(true);
        resultBanner.setVisible(true);
        resultBanner.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(520), resultBanner);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    private String explainObservedResult(SortRaceResult faster, SortRaceResult slower) {
        if (faster.executionTimeNanos() == slower.executionTimeNanos()) {
            return "The measured times tied on this input. This is one approximate run, not a universal ranking.";
        }
        if (faster.operations() < slower.operations()) {
            return faster.algorithm().name() + " also recorded fewer counted operations on this input, "
                    + "which may help explain this run; timing varies and this is not a universal ranking.";
        }
        if (faster.operations() > slower.operations()) {
            return faster.algorithm().name() + " was faster despite recording more counted operations; "
                    + "runtime variation and implementation overhead can affect a single run.";
        }
        return "Both recorded the same number of counted operations on this input; timing varies between runs.";
    }

    private void resetRace() {
        raceGeneration++;
        if (preparationTask != null) {
            preparationTask.cancel(true);
            preparationTask = null;
        }
        laneA.reset(initialArray);
        laneB.reset(initialArray);
        clearResultBanner();
        setRaceStatus("Ready");
        setMessage("Race reset. Both lanes are ready to run again.");
        updateControls();
    }

    private void clearResultBanner() {
        resultBanner.setOpacity(1);
        resultBanner.setVisible(false);
        resultBanner.setManaged(false);
        resultHeading.setText("");
        resultDetails.setText("");
    }

    private void pauseLane(RaceLane lane) {
        if (lane.timeline != null && "Running".equals(lane.status)) {
            lane.timeline.pause();
            lane.setStatus("Paused");
        }
    }

    private void resume() {
        if (!"Paused".equals(raceStatus)) {
            return;
        }
        resumeLane(laneA);
        resumeLane(laneB);
        setRaceStatus("Running");
        setMessage("Race resumed. Each lane continues from its current step.");
        updateControls();
    }

    private void resumeLane(RaceLane lane) {
        if (lane.timeline != null && "Paused".equals(lane.status)) {
            lane.timeline.setRate(playbackRate(speedSlider.getValue()));
            lane.timeline.play();
            lane.setStatus("Running");
        }
    }

    private void updateTimelineRate(RaceLane lane, double speed) {
        if (lane.timeline != null && "Running".equals(lane.status)) {
            lane.timeline.setRate(playbackRate(speed));
        }
    }

    private void updateControls() {
        boolean active = isRaceActive();
        boolean running = "Running".equals(raceStatus);
        boolean paused = "Paused".equals(raceStatus);
        algorithmAChoice.setDisable(active);
        algorithmBChoice.setDisable(active);
        arraySizeChoice.setDisable(active);
        arrayInput.setDisable(active);
        generateArrayButton.setDisable(active);
        startRaceButton.setDisable(active);
        pauseButton.setDisable(!running);
        resumeButton.setDisable(!paused);
        resetButton.setDisable("Ready".equals(raceStatus));
    }

    private boolean isRaceActive() {
        return "Preparing".equals(raceStatus)
                || "Running".equals(raceStatus)
                || "Paused".equals(raceStatus);
    }

    private void setRaceStatus(String status) {
        raceStatus = status;
        raceStatusValue.setText(status);
    }

    private void setMessage(String message) {
        messageValue.setText(message);
    }

    private double playbackRate(double value) {
        return 0.35 + value / 100.0 * 2.65;
    }

    private String formatExecutionTime(long nanoseconds) {
        return String.format(Locale.ROOT, "%.3f ms", nanoseconds / 1_000_000.0);
    }

    private String join(int[] values) {
        return java.util.Arrays.stream(values)
                .mapToObj(Integer::toString)
                .collect(Collectors.joining(", "));
    }

    private final class RaceLane {
        private final Label laneTitle = new Label();
        private final Label algorithmName = new Label();
        private final Label comparisonsValue = new Label("0");
        private final Label swapsValue = new Label("0");
        private final Label movementsValue = new Label("0");
        private final Label operationsValue = new Label("0");
        private final Label executionTimeValue = new Label("--");
        private final Label statusValue = new Label("Ready");
        private final Label complexityValue = new Label();
        private final ArrayBarsView barsView = new ArrayBarsView();
        private final VBox panel;

        private SortRaceResult result;
        private Timeline timeline;
        private int stepIndex;
        private int comparisons;
        private int swaps;
        private int movements;
        private int operations;
        private String status = "Ready";

        private RaceLane(String title) {
            laneTitle.setText(title);
            panel = createPanel();
        }

        private VBox createPanel() {
            VBox lanePanel = new VBox(10);
            lanePanel.getStyleClass().add("race-lane-panel");
            lanePanel.setPadding(new Insets(14));

            VBox heading = new VBox(5);
            laneTitle.getStyleClass().add("eyebrow");
            algorithmName.getStyleClass().add("race-algorithm-name");
            heading.getChildren().addAll(laneTitle, algorithmName);

            barsView.setMinHeight(350);
            barsView.setMinWidth(0);
            HBox firstStats = new HBox(7,
                    createStatCard("COMPARISONS", comparisonsValue),
                    createStatCard("SWAPS", swapsValue),
                    createStatCard("MOVES", movementsValue));
            HBox secondStats = new HBox(7,
                    createStatCard("OPERATIONS", operationsValue),
                    createStatCard("EXECUTION TIME", executionTimeValue),
                    createStatCard("STATUS", statusValue));
            firstStats.setAlignment(Pos.CENTER);
            secondStats.setAlignment(Pos.CENTER);

            Label complexityHeading = new Label("COMPLEXITY");
            complexityHeading.getStyleClass().add("control-label");
            complexityValue.getStyleClass().add("race-complexity");
            complexityValue.setWrapText(true);
            lanePanel.getChildren().addAll(
                    heading, barsView, firstStats, secondStats, complexityHeading, complexityValue);
            VBox.setVgrow(barsView, Priority.ALWAYS);
            return lanePanel;
        }

        private void setAlgorithm(SortingAlgorithm algorithm) {
            if (algorithm == null) {
                return;
            }
            algorithmName.setText(algorithm.name());
            complexityValue.setText("Best " + algorithm.bestCaseTimeComplexity()
                    + " | Avg " + algorithm.averageCaseTimeComplexity()
                    + " | Worst " + algorithm.worstCaseTimeComplexity()
                    + " | Space " + algorithm.auxiliarySpaceComplexity());
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

        private void startTimeline(double intervalMillis, double rate, Runnable advance) {
            stepIndex = 0;
            setStatus("Running");
            timeline = new Timeline(new KeyFrame(Duration.millis(intervalMillis), event -> advance.run()));
            timeline.setCycleCount(Timeline.INDEFINITE);
            timeline.setRate(rate);
            timeline.play();
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
            barsView.render(step.arrayState(), step.comparedIndexLeft(), step.comparedIndexRight(),
                    step.pivotIndex(), step.sortedElements());
            comparisonsValue.setText(Integer.toString(comparisons));
            swapsValue.setText(Integer.toString(swaps));
            movementsValue.setText(Integer.toString(movements));
            operationsValue.setText(Integer.toString(operations));
        }

        private void setStatus(String value) {
            status = value;
            statusValue.setText(value);
        }

        private boolean isComplete() {
            return "Completed".equals(status);
        }

        private void stopTimeline() {
            if (timeline != null) {
                timeline.stop();
                timeline = null;
            }
        }

        private void reset(int[] array) {
            stopTimeline();
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
