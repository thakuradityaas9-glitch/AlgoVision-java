package com.algovision.controller;

import java.util.List;
import java.util.Random;

import com.algovision.algorithms.BubbleSort;
import com.algovision.algorithms.InsertionSort;
import com.algovision.algorithms.MergeSort;
import com.algovision.algorithms.QuickSort;
import com.algovision.algorithms.SelectionSort;
import com.algovision.algorithms.SortingAlgorithm;
import com.algovision.model.SortStep;
import com.algovision.visualization.ArrayBarsView;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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

public class SortingVisualizerController {
    private static final int RANDOM_ARRAY_SIZE = 18;
    private static final int MAX_ARRAY_SIZE = 50;

    private final ScrollPane view;
    private final List<SortingAlgorithm> algorithms = List.of(
            new BubbleSort(),
            new SelectionSort(),
            new InsertionSort(),
            new MergeSort(),
            new QuickSort());
    private final TextField arrayInput = new TextField();
    private final ComboBox<SortingAlgorithm> algorithmChoice = new ComboBox<>();
    private final Slider speedSlider = new Slider(0, 100, 50);
    private final ArrayBarsView barsView = new ArrayBarsView();
    private final Label algorithmDescription = new Label();
    private final Label bestTimeValue = new Label();
    private final Label averageTimeValue = new Label();
    private final Label worstTimeValue = new Label();
    private final Label auxiliarySpaceValue = new Label();
    private final Label errorLabel = new Label();
    private final Label sizeValue = new Label("0");
    private final Label comparisonsValue = new Label("0");
    private final Label swapsValue = new Label("0");
    private final Label statusValue = new Label("Ready");
    private final Button playButton = new Button("Play");
    private final Button pauseButton = new Button("Pause");
    private final Button resetButton = new Button("Reset");

    private int[] initialArray = {42, 18, 67, 29, 85, 36, 54, 11, 73, 24, 91, 48};
    private SortingAlgorithm selectedAlgorithm = algorithms.get(0);
    private List<SortStep> steps = List.of();
    private int stepIndex;
    private Timeline timeline;

    public SortingVisualizerController() {
        view = createView();
        arrayInput.setText(join(initialArray));
        updateComplexity();
        generateSteps();
        renderInitialState();
        updateButtons();
    }

    public Node getView() {
        return view;
    }

    private ScrollPane createView() {
        VBox content = new VBox(22);
        content.getStyleClass().add("sorting-content");
        content.setPadding(new Insets(38, 42, 32, 42));

        VBox introduction = new VBox(8);
        Label eyebrow = new Label("SORTING ALGORITHMS");
        eyebrow.getStyleClass().add("eyebrow");
        Label heading = new Label("Sorting Visualizer");
        heading.getStyleClass().add("welcome-heading");
        algorithmDescription.getStyleClass().add("welcome-subtitle");
        algorithmDescription.setWrapText(true);
        introduction.getChildren().addAll(eyebrow, heading, algorithmDescription);

        VBox controls = createControls();
        HBox statistics = new HBox(12,
                createStatCard("ARRAY SIZE", sizeValue),
                createStatCard("COMPARISONS", comparisonsValue),
                createStatCard("SWAPS / MOVES", swapsValue),
                createStatCard("STATUS", statusValue));
        statistics.setAlignment(Pos.CENTER);

        VBox complexity = createComplexityPanel();

        VBox visualizationPanel = new VBox(12);
        HBox chartHeading = new HBox();
        Label chartTitle = new Label("Array");
        chartTitle.getStyleClass().add("panel-heading");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        chartHeading.getChildren().addAll(chartTitle, spacer);
        visualizationPanel.getChildren().addAll(chartHeading, barsView);

        content.getChildren().addAll(introduction, controls, statistics, complexity, visualizationPanel);
        VBox.setVgrow(visualizationPanel, Priority.ALWAYS);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("content-scroll");
        return scrollPane;
    }

    private VBox createControls() {
        VBox panel = new VBox(14);
        panel.getStyleClass().add("controls-card");
        panel.setPadding(new Insets(18));

        algorithmChoice.getItems().setAll(algorithms);
        algorithmChoice.getSelectionModel().selectFirst();
        algorithmChoice.setMaxWidth(Double.MAX_VALUE);
        algorithmChoice.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(SortingAlgorithm item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item.name());
            }
        });
        algorithmChoice.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(SortingAlgorithm item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item.name());
            }
        });
        algorithmChoice.setOnAction(event -> selectAlgorithm());

        arrayInput.setPromptText("e.g. 8, 3, -2, 10");
        arrayInput.getStyleClass().add("array-input");
        Button randomizeButton = new Button("Randomize");
        randomizeButton.getStyleClass().add("secondary-button");
        randomizeButton.setOnAction(event -> randomizeArray());
        Button generateButton = new Button("Generate");
        generateButton.getStyleClass().add("secondary-button");
        generateButton.setOnAction(event -> generateFromInput());

        HBox inputRow = new HBox(10,
                labeledControl("ALGORITHM", algorithmChoice),
                labeledControl("ARRAY VALUES", arrayInput),
                randomizeButton,
                generateButton);
        inputRow.setAlignment(Pos.BOTTOM_LEFT);
        HBox.setHgrow(inputRow.getChildren().get(1), Priority.ALWAYS);

        playButton.getStyleClass().add("primary-button");
        pauseButton.getStyleClass().add("secondary-button");
        resetButton.getStyleClass().add("secondary-button");
        playButton.setOnAction(event -> play());
        pauseButton.setOnAction(event -> pause());
        resetButton.setOnAction(event -> reset());

        Label speedLabel = new Label("SPEED");
        speedLabel.getStyleClass().add("control-label");
        speedSlider.setShowTickLabels(false);
        speedSlider.setShowTickMarks(false);
        speedSlider.setBlockIncrement(10);
        speedSlider.setPrefWidth(160);
        speedSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (timeline != null && "Running".equals(statusValue.getText())) {
                timeline.setRate(0.25 + newValue.doubleValue() / 100.0 * 2.75);
            }
        });
        HBox playback = new HBox(10, playButton, pauseButton, resetButton,
                createSpacer(), speedLabel, speedSlider);
        playback.setAlignment(Pos.CENTER_LEFT);

        errorLabel.getStyleClass().add("error-message");
        errorLabel.setWrapText(true);
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);

        panel.getChildren().addAll(inputRow, playback, errorLabel);
        return panel;
    }

    private Node labeledControl(String labelText, Node control) {
        VBox field = new VBox(7);
        Label label = new Label(labelText);
        label.getStyleClass().add("control-label");
        field.getChildren().addAll(label, control);
        if (control == arrayInput) {
            HBox.setHgrow(field, Priority.ALWAYS);
            arrayInput.setMaxWidth(Double.MAX_VALUE);
        }
        return field;
    }

    private Region createSpacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private VBox createComplexityPanel() {
        VBox panel = new VBox(10);
        Label title = new Label("Algorithm complexity");
        title.getStyleClass().add("panel-heading");
        HBox values = new HBox(12,
                createComplexityCard("BEST CASE", bestTimeValue),
                createComplexityCard("AVERAGE CASE", averageTimeValue),
                createComplexityCard("WORST CASE", worstTimeValue),
                createComplexityCard("AUXILIARY SPACE", auxiliarySpaceValue));
        panel.getChildren().addAll(title, values);
        return panel;
    }

    private VBox createComplexityCard(String title, Label value) {
        VBox card = new VBox(7, new Label(title), value);
        card.getStyleClass().add("complexity-card");
        card.getChildren().get(0).getStyleClass().add("stat-title");
        value.getStyleClass().add("complexity-value");
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private VBox createStatCard(String title, Label value) {
        VBox card = new VBox(8, new Label(title), value);
        card.getStyleClass().add("stat-card");
        card.getChildren().get(0).getStyleClass().add("stat-title");
        value.getStyleClass().add("stat-value");
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private void generateFromInput() {
        try {
            int[] parsed = parseArray(arrayInput.getText());
            stopTimeline();
            initialArray = parsed;
            stepIndex = 0;
            generateSteps();
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
        stopTimeline();
        Random random = new Random();
        int[] values = new int[RANDOM_ARRAY_SIZE];
        for (int index = 0; index < values.length; index++) {
            values[index] = random.nextInt(91) + 10;
        }
        initialArray = values;
        arrayInput.setText(join(values));
        stepIndex = 0;
        generateSteps();
        clearError();
        renderInitialState();
        setStatus("Ready");
        updateButtons();
    }

    private void generateSteps() {
        steps = selectedAlgorithm.generateSteps(initialArray);
    }

    private void selectAlgorithm() {
        SortingAlgorithm algorithm = algorithmChoice.getValue();
        if (algorithm == null || algorithm == selectedAlgorithm) {
            return;
        }
        stopTimeline();
        selectedAlgorithm = algorithm;
        updateComplexity();
        generateSteps();
        stepIndex = 0;
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

    private void play() {
        if (stepIndex >= steps.size()) {
            return;
        }
        if (timeline == null) {
            timeline = new Timeline(new KeyFrame(Duration.millis(480), event -> advance()));
            timeline.setCycleCount(Timeline.INDEFINITE);
        }
        timeline.setRate(0.25 + speedSlider.getValue() / 100.0 * 2.75);
        timeline.play();
        setStatus("Running");
        updateButtons();
    }

    public void pause() {
        if (timeline != null) {
            timeline.pause();
            setStatus("Paused");
            updateButtons();
        }
    }

    private void advance() {
        if (stepIndex >= steps.size()) {
            finish();
            return;
        }
        SortStep step = steps.get(stepIndex++);
        double scrollPosition = view.getVvalue();
        barsView.render(step.arrayState(), step.comparedIndexLeft(), step.comparedIndexRight(),
                step.pivotIndex(), step.sortedElements());
        view.setVvalue(scrollPosition);
        comparisonsValue.setText(Integer.toString(step.comparisons()));
        swapsValue.setText(Integer.toString(step.swaps()));
        if (stepIndex >= steps.size()) {
            finish();
        }
    }

    private void finish() {
        stopTimeline();
        setStatus("Completed");
        updateButtons();
    }

    private void reset() {
        stopTimeline();
        stepIndex = 0;
        arrayInput.setText(join(initialArray));
        renderInitialState();
        view.setVvalue(0);
        clearError();
        setStatus("Ready");
        updateButtons();
    }

    private void renderInitialState() {
        barsView.render(initialArray, -1, -1, -1, new boolean[initialArray.length]);
        view.setVvalue(0);
        sizeValue.setText(Integer.toString(initialArray.length));
        comparisonsValue.setText("0");
        swapsValue.setText("0");
    }

    private void stopTimeline() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
    }

    private void setStatus(String status) {
        statusValue.setText(status);
    }

    private void updateButtons() {
        boolean running = "Running".equals(statusValue.getText());
        playButton.setDisable(running || "Completed".equals(statusValue.getText()));
        pauseButton.setDisable(!running);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
    }

    private void clearError() {
        errorLabel.setText("");
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);
    }

    private String join(int[] values) {
        return java.util.Arrays.stream(values)
                .mapToObj(Integer::toString)
                .collect(java.util.stream.Collectors.joining(", "));
    }
}
