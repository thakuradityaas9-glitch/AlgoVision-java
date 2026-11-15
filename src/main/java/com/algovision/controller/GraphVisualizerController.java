package com.algovision.controller;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;

import com.algovision.graph.algorithms.BreadthFirstSearch;
import com.algovision.graph.algorithms.DepthFirstSearch;
import com.algovision.graph.algorithms.DijkstraShortestPath;
import com.algovision.graph.algorithms.GraphTraversalAlgorithm;
import com.algovision.graph.model.DijkstraEvent;
import com.algovision.graph.model.DijkstraStep;
import com.algovision.graph.model.Graph;
import com.algovision.graph.model.GraphEdge;
import com.algovision.graph.model.GraphNode;
import com.algovision.graph.model.RandomGraphGenerator;
import com.algovision.graph.model.GraphTraversalEvent;
import com.algovision.graph.model.GraphTraversalStep;
import com.algovision.graph.model.GraphVisualizationStep;
import com.algovision.visualization.GraphCanvasListener;
import com.algovision.visualization.GraphCanvasState;
import com.algovision.visualization.GraphCanvasView;
import com.algovision.visualization.GraphInteractionMode;
import com.algovision.ui.AlgoVisionTheme;
import com.algovision.ui.ScrollablePanel;

public final class GraphVisualizerController extends JPanel {
    private static final Color APP_BACKGROUND = AlgoVisionTheme.APP_BACKGROUND;
    private static final Color PANEL_BACKGROUND = AlgoVisionTheme.PANEL_BACKGROUND;
    private static final Color BORDER_COLOR = AlgoVisionTheme.BORDER;
    private static final Color PRIMARY_TEXT = AlgoVisionTheme.TEXT_PRIMARY;
    private static final Color BODY_TEXT = AlgoVisionTheme.TEXT_BODY;
    private static final Color ACCENT_TEXT = AlgoVisionTheme.ACCENT;
    private static final int BASE_INTERVAL_MILLIS = 480;
    private static final int MIN_INTERVAL_MILLIS = 120;
    private static final int MAX_INTERVAL_MILLIS = 1920;

    private Graph graph = new Graph();
    private final Map<Integer, GraphCanvasState.NodePosition> nodePositions = new LinkedHashMap<>();
    private final List<GraphTraversalAlgorithm> algorithms = List.of(
            new BreadthFirstSearch(),
            new DepthFirstSearch(),
            new DijkstraShortestPath());
    private final GraphCanvasView canvas = new GraphCanvasView();
    private final JComboBox<GraphTraversalAlgorithm> algorithmChoice = new JComboBox<>();
    private final JComboBox<Integer> startNodeChoice = new JComboBox<>();
    private final JComboBox<Integer> destinationNodeChoice = new JComboBox<>();
    private final javax.swing.JTextField edgeWeightInput = new javax.swing.JTextField("1");
    private final JSlider speedSlider = new JSlider(0, 100, 50);
    private final JLabel nodeCountValue = new JLabel("0");
    private final JLabel edgeCountValue = new JLabel("0");
    private final JLabel visitedCountValue = new JLabel("0");
    private final JLabel edgesExaminedValue = new JLabel("0");
    private final JLabel algorithmValue = new JLabel("BFS");
    private final JLabel statusValue = new JLabel("Ready");
    private final JLabel stepValue = new JLabel("0 / 0");
    private final JLabel currentDistanceValue = new JLabel("--");
    private final JLabel finalDistanceValue = new JLabel("--");
    private final JLabel traversalOrderValue = new JLabel("--");
    private final JLabel frontierValue = new JLabel("--");
    private final JLabel shortestPathValue = new JLabel("--");
    private final javax.swing.JTextArea messageValue = new javax.swing.JTextArea(
            "Add nodes on the canvas or generate a random graph.");
    private final JButton startButton = new JButton("Start");
    private final JButton pauseButton = new JButton("Pause");
    private final JButton resumeButton = new JButton("Resume");
    private final JButton resetButton = new JButton("Reset");
    private final JButton deleteNodeButton = new JButton("Delete Selected Node");
    private final JButton deleteEdgeButton = new JButton("Delete Selected Edge");
    private final JButton updateEdgeWeightButton = new JButton("Update Edge Weight");
    private final JButton clearGraphButton = new JButton("Clear Graph");
    private final JButton randomGraphButton = new JButton("Generate Random Graph");
    private final JToggleButton selectModeButton = new JToggleButton("Select / Move");
    private final JToggleButton addNodeModeButton = new JToggleButton("Add Node");
    private final JToggleButton addEdgeModeButton = new JToggleButton("Add Edge");
    private final ButtonGroup interactionModeGroup = new ButtonGroup();
    private final JPanel view;
    private final Set<GraphEdge> exploredEdges = new LinkedHashSet<>();

    private GraphTraversalAlgorithm selectedAlgorithm = algorithms.getFirst();
    private List<? extends GraphVisualizationStep> steps = List.of();
    private GraphVisualizationStep currentStep;
    private SwingWorker<List<GraphVisualizationStep>, Void> stepWorker;
    private Timer animationTimer;
    private int generationId;
    private int stepIndex;
    private int edgesExamined;
    private int timerDelayMillis = BASE_INTERVAL_MILLIS;
    private Integer pendingNodeId;
    private Integer selectedNodeId;
    private GraphEdge selectedEdge;
    private GraphInteractionMode interactionMode = GraphInteractionMode.SELECT;
    private boolean updatingNodeChoices;

    public GraphVisualizerController() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Create the graph workspace on the Swing event dispatch thread.");
        }
        setLayout(new BorderLayout());
        setBackground(APP_BACKGROUND);
        setBorder(new EmptyBorder(20, 24, 16, 24));
        canvas.setListener(new GraphCanvasListener() {
            @Override
            public void onEmptyCanvasClick(double xRatio, double yRatio) {
                handleEmptyCanvasClick(xRatio, yRatio);
            }

            @Override
            public void onNodeClick(int nodeId) {
                handleNodeClick(nodeId);
            }

            @Override
            public void onNodeMoved(int nodeId, double xRatio, double yRatio) {
                resetTraversal();
                nodePositions.put(nodeId, new GraphCanvasState.NodePosition(xRatio, yRatio));
                refreshGraph();
            }

            @Override
            public void onEdgeClick(GraphEdge edge) {
                selectEdge(edge);
            }
        });
        canvas.setInteractionMode(interactionMode);
        view = createView();
        configureControls();
        updateNodeChoices();
        updateStatistics();
        updateButtons();
        renderCanvas();
        add(view, BorderLayout.CENTER);
    }

    public JPanel getView() {
        return this;
    }

    public void pause() {
        if (isAnimationTimerRunning()) {
            stopAnimationTimer();
            setStatus("Paused");
            setMessage("Traversal paused. Resume to continue or reset.");
            updateButtons();
        } else if (stepWorker != null) {
            cancelCurrentRun();
            setStatus("Ready");
            setMessage("Traversal preparation canceled.");
            updateButtons();
        }
    }

    private JPanel createView() {
        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(16, 18, 12, 18));
        root.add(createHeader(), BorderLayout.NORTH);
        root.add(createWorkspace(), BorderLayout.CENTER);
        root.add(createStatusArea(), BorderLayout.SOUTH);
        return root;
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(20, 0));
        header.setOpaque(false);
        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.add(label("GRAPH ALGORITHMS", ACCENT_TEXT, Font.BOLD, 10));
        titleBlock.add(Box.createVerticalStrut(4));
        titleBlock.add(label("Graph Visualizer", PRIMARY_TEXT, Font.BOLD, 26));
        titleBlock.add(Box.createVerticalStrut(4));
        titleBlock.add(label("Build a graph, choose a traversal, and follow every step.",
                BODY_TEXT, Font.PLAIN, 12));

        JPanel badges = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 7));
        badges.setOpaque(false);
        styleBadge(algorithmValue, new Color(0x252451), new Color(0xb7b4ff));
        styleStatus();
        badges.add(algorithmValue);
        badges.add(statusValue);
        header.add(titleBlock, BorderLayout.CENTER);
        header.add(badges, BorderLayout.EAST);
        return header;
    }

    private JPanel createWorkspace() {
        JPanel workspace = new JPanel(new BorderLayout(14, 0));
        workspace.setOpaque(false);
        workspace.add(createControlPanel(), BorderLayout.WEST);
        workspace.add(createCanvasPanel(), BorderLayout.CENTER);
        return workspace;
    }

    private JScrollPane createControlPanel() {
        ScrollablePanel controls = new ScrollablePanel();
        controls.setBackground(PANEL_BACKGROUND);
        controls.setBorder(new EmptyBorder(16, 16, 16, 16));
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        controls.add(label("CONTROLS", PRIMARY_TEXT, Font.BOLD, 13));
        controls.add(Box.createVerticalStrut(14));

        controls.add(sectionHeading("GRAPH"));
        JPanel graphActions = new JPanel(new GridLayout(2, 1, 0, 7));
        graphActions.setOpaque(false);
        graphActions.add(randomGraphButton);
        graphActions.add(clearGraphButton);
        graphActions.setAlignmentX(LEFT_ALIGNMENT);
        controls.add(graphActions);
        controls.add(Box.createVerticalStrut(16));

        controls.add(sectionHeading("CANVAS MODE"));
        JPanel modes = new JPanel(new GridLayout(1, 3, 5, 0));
        modes.setOpaque(false);
        interactionModeGroup.add(selectModeButton);
        interactionModeGroup.add(addNodeModeButton);
        interactionModeGroup.add(addEdgeModeButton);
        selectModeButton.setSelected(true);
        for (JToggleButton button : List.of(selectModeButton, addNodeModeButton, addEdgeModeButton)) {
            styleToggleButton(button);
            modes.add(button);
        }
        updateModeStyles();
        modes.setAlignmentX(LEFT_ALIGNMENT);
        controls.add(modes);
        controls.add(Box.createVerticalStrut(16));

        controls.add(sectionHeading("ALGORITHM"));
        controls.add(labeledControl("ALGORITHM", algorithmChoice));
        controls.add(Box.createVerticalStrut(8));
        controls.add(labeledControl("START NODE", startNodeChoice));
        controls.add(Box.createVerticalStrut(8));
        controls.add(labeledControl("DESTINATION (DIJKSTRA)", destinationNodeChoice));
        controls.add(Box.createVerticalStrut(16));

        controls.add(sectionHeading("EDGE WEIGHT"));
        edgeWeightInput.setBackground(AlgoVisionTheme.CONTROL_BACKGROUND);
        edgeWeightInput.setForeground(AlgoVisionTheme.TEXT_PRIMARY);
        edgeWeightInput.setCaretColor(PRIMARY_TEXT);
        edgeWeightInput.setPreferredSize(new Dimension(240, 34));
        edgeWeightInput.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AlgoVisionTheme.BORDER_SUBTLE),
                new EmptyBorder(7, 9, 7, 9)));
        controls.add(labeledControl("NON-NEGATIVE INTEGER", edgeWeightInput));
        controls.add(Box.createVerticalStrut(7));
        controls.add(updateEdgeWeightButton);
        controls.add(Box.createVerticalStrut(7));
        controls.add(deleteNodeButton);
        controls.add(Box.createVerticalStrut(7));
        controls.add(deleteEdgeButton);
        controls.add(Box.createVerticalStrut(16));

        controls.add(sectionHeading("PLAYBACK"));
        JPanel playback = new JPanel(new GridLayout(2, 2, 7, 7));
        playback.setOpaque(false);
        playback.add(startButton);
        playback.add(pauseButton);
        playback.add(resumeButton);
        playback.add(resetButton);
        playback.setAlignmentX(LEFT_ALIGNMENT);
        controls.add(playback);
        controls.add(Box.createVerticalStrut(12));
        speedSlider.setOpaque(false);
        speedSlider.setMajorTickSpacing(50);
        speedSlider.setPaintTicks(true);
        speedSlider.setToolTipText("Adjust animation speed");
        controls.add(labeledControl("ANIMATION SPEED", speedSlider));
        controls.add(Box.createVerticalStrut(8));
        controls.add(sectionHeading("LIVE TRACE"));
        controls.add(traceCard("TRAVERSAL ORDER", traversalOrderValue));
        controls.add(Box.createVerticalStrut(6));
        controls.add(traceCard("QUEUE / STACK / PRIORITY QUEUE", frontierValue));
        controls.add(Box.createVerticalStrut(6));
        controls.add(traceCard("SHORTEST PATH", shortestPathValue));
        controls.add(Box.createVerticalStrut(10));
        controls.add(wrappedLabel(
                "Select / Move selects nodes and lets you drag them. Add Node and Add Edge are explicit canvas modes.",
                BODY_TEXT, 10));

        JScrollPane scrollPane = new JScrollPane(controls);
        scrollPane.setName("graphControls");
        scrollPane.setPreferredSize(new Dimension(320, 0));
        scrollPane.setMinimumSize(new Dimension(280, 250));
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        scrollPane.getViewport().setBackground(PANEL_BACKGROUND);
        scrollPane.getViewport().setScrollMode(JViewport.BLIT_SCROLL_MODE);
        scrollPane.setWheelScrollingEnabled(true);
        JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
        verticalScrollBar.setUnitIncrement(22);
        verticalScrollBar.setBlockIncrement(120);
        verticalScrollBar.setPreferredSize(new Dimension(11, 0));
        verticalScrollBar.setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                thumbColor = new Color(0x485674);
                trackColor = new Color(0x0d1426);
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createScrollButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createScrollButton();
            }

            @Override
            protected void paintTrack(Graphics graphics, JComponent component, Rectangle bounds) {
                graphics.setColor(new Color(0x0d1426));
                graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }

            @Override
            protected void paintThumb(Graphics graphics, JComponent component, Rectangle bounds) {
                if (bounds.isEmpty() || !scrollbar.isEnabled()) {
                    return;
                }
                Graphics2D copy = (Graphics2D) graphics.create();
                copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                copy.setColor(isDragging ? new Color(0x817dff)
                        : isThumbRollover() ? new Color(0x687698) : new Color(0x485674));
                copy.fillRoundRect(bounds.x + 2, bounds.y + 3,
                        Math.max(1, bounds.width - 4), Math.max(1, bounds.height - 6), 8, 8);
                copy.dispose();
            }
        });
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        SwingUtilities.invokeLater(() -> {
            scrollPane.getVerticalScrollBar().setValue(0);
            canvas.requestFocusInWindow();
        });
        return scrollPane;
    }

    private JButton createScrollButton() {
        JButton button = new JButton();
        Dimension zeroSize = new Dimension(0, 0);
        button.setPreferredSize(zeroSize);
        button.setMinimumSize(zeroSize);
        button.setMaximumSize(zeroSize);
        button.setOpaque(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusable(false);
        return button;
    }

    private JPanel createCanvasPanel() {
        JPanel panel = cardPanel();
        panel.setLayout(new BorderLayout(0, 12));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                new EmptyBorder(14, 16, 14, 16)));
        JPanel canvasHeader = new JPanel();
        canvasHeader.setOpaque(false);
        canvasHeader.setLayout(new BoxLayout(canvasHeader, BoxLayout.Y_AXIS));
        JPanel title = new JPanel();
        title.setOpaque(false);
        title.setLayout(new BoxLayout(title, BoxLayout.Y_AXIS));
        title.add(label("Graph canvas", PRIMARY_TEXT, Font.BOLD, 16));
        title.add(Box.createVerticalStrut(4));
        title.add(label("Use the selected mode to edit nodes and weighted edges.",
                BODY_TEXT, Font.PLAIN, 11));
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        legend.setOpaque(false);
        legend.add(legendItem("Unvisited", new Color(0x202c43)));
        legend.add(legendItem("Visited", new Color(0x279a79)));
        legend.add(legendItem("Current", new Color(0xe99e45)));
        legend.add(legendItem("Shortest path", new Color(0x694dcc)));
        title.setAlignmentX(LEFT_ALIGNMENT);
        legend.setAlignmentX(LEFT_ALIGNMENT);
        canvasHeader.add(title);
        canvasHeader.add(Box.createVerticalStrut(8));
        canvasHeader.add(legend);
        panel.add(canvasHeader, BorderLayout.NORTH);
        canvas.setPreferredSize(new Dimension(720, 470));
        canvas.setMinimumSize(new Dimension(230, 220));
        JPanel canvasFrame = new JPanel(new BorderLayout());
        canvasFrame.setOpaque(true);
        canvasFrame.setBackground(new Color(0x0c1426));
        canvasFrame.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x2a3955)),
                new EmptyBorder(3, 3, 3, 3)));
        canvasFrame.add(canvas, BorderLayout.CENTER);
        panel.add(canvasFrame, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createStatusArea() {
        JPanel area = new JPanel(new BorderLayout(0, 7));
        area.setOpaque(false);
        JPanel metrics = new JPanel(new GridLayout(1, 7, 8, 0));
        metrics.setOpaque(false);
        metrics.add(statCard("NODES", nodeCountValue));
        metrics.add(statCard("EDGES", edgeCountValue));
        metrics.add(statCard("VISITED", visitedCountValue));
        metrics.add(statCard("EDGES EXAMINED", edgesExaminedValue));
        metrics.add(statCard("EXECUTION STEP", stepValue));
        metrics.add(statCard("SHORTEST DISTANCE", currentDistanceValue));
        metrics.add(statCard("FINAL DISTANCE", finalDistanceValue));
        area.add(metrics, BorderLayout.CENTER);

        JPanel messagePanel = cardPanel();
        messagePanel.setLayout(new BorderLayout(12, 0));
        messagePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                new EmptyBorder(8, 11, 8, 11)));
        messagePanel.add(label("CURRENT OPERATION", new Color(0x71809b), Font.BOLD, 9),
                BorderLayout.WEST);
        messageValue.setEditable(false);
        messageValue.setLineWrap(true);
        messageValue.setWrapStyleWord(true);
        messageValue.setRows(2);
        messageValue.setOpaque(false);
        messageValue.setForeground(new Color(0xc5cbea));
        messageValue.setFont(font(Font.PLAIN, 11));
        messageValue.setBorder(null);
        messagePanel.add(messageValue, BorderLayout.CENTER);
        area.add(messagePanel, BorderLayout.SOUTH);
        return area;
    }

    private void configureControls() {
        algorithmChoice.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list,
                        value instanceof GraphTraversalAlgorithm algorithm ? algorithm.name() : "",
                        index, isSelected, cellHasFocus);
                setBackground(isSelected ? new Color(0x242654) : new Color(0x0d1426));
                setForeground(new Color(0xe8ecf8));
                setBorder(new EmptyBorder(6, 8, 6, 8));
                return this;
            }
        });
        algorithms.forEach(algorithmChoice::addItem);
        algorithmChoice.setSelectedItem(selectedAlgorithm);
        algorithmChoice.addActionListener(event -> selectAlgorithm());
        configureChoice(algorithmChoice);
        configureChoice(startNodeChoice);
        configureChoice(destinationNodeChoice);

        startNodeChoice.setToolTipText("Choose the traversal start node");
        startNodeChoice.addActionListener(event -> {
            if (!updatingNodeChoices) {
                resetTraversal();
            }
        });
        destinationNodeChoice.setToolTipText("Choose Dijkstra's destination node");
        destinationNodeChoice.setEnabled(false);
        destinationNodeChoice.addActionListener(event -> {
            if (!updatingNodeChoices) {
                resetTraversal();
            }
        });

        styleButton(startButton, true);
        styleButton(pauseButton, false);
        styleButton(resumeButton, false);
        styleButton(resetButton, false);
        styleButton(deleteNodeButton, false);
        styleButton(deleteEdgeButton, false);
        styleButton(updateEdgeWeightButton, false);
        styleButton(randomGraphButton, false);
        styleButton(clearGraphButton, false);
        startButton.addActionListener(event -> start());
        pauseButton.addActionListener(event -> pause());
        resumeButton.addActionListener(event -> resume());
        resetButton.addActionListener(event -> resetTraversal());
        deleteNodeButton.addActionListener(event -> deleteSelectedNode());
        deleteEdgeButton.addActionListener(event -> deleteSelectedEdge());
        updateEdgeWeightButton.addActionListener(event -> updateSelectedEdgeWeight());
        clearGraphButton.addActionListener(event -> clearGraph());
        randomGraphButton.addActionListener(event -> generateRandomGraph());
        edgeWeightInput.addActionListener(event -> updateSelectedEdgeWeight());
        speedSlider.addChangeListener(event -> updateTimerSpeed());

        selectModeButton.addActionListener(event -> setInteractionMode(GraphInteractionMode.SELECT));
        addNodeModeButton.addActionListener(event -> setInteractionMode(GraphInteractionMode.ADD_NODE));
        addEdgeModeButton.addActionListener(event -> setInteractionMode(GraphInteractionMode.ADD_EDGE));
    }

    private void configureChoice(JComboBox<?> choice) {
        AlgoVisionTheme.styleComboBox(choice);
        choice.setFont(font(Font.PLAIN, 12));
        choice.setPreferredSize(new Dimension(240, 34));
        choice.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
    }

    private JPanel labeledControl(String title, Component control) {
        JPanel field = new JPanel();
        field.setOpaque(false);
        field.setLayout(new BoxLayout(field, BoxLayout.Y_AXIS));
        JLabel heading = label(title, new Color(0x8793aa), Font.BOLD, 9);
        heading.setAlignmentX(LEFT_ALIGNMENT);
        control.setFont(font(Font.PLAIN, 12));
        if (control instanceof javax.swing.JComponent component) {
            component.setAlignmentX(LEFT_ALIGNMENT);
            component.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                    component.getPreferredSize().height));
        }
        field.add(heading);
        field.add(Box.createVerticalStrut(5));
        field.add(control);
        field.setAlignmentX(LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, field.getPreferredSize().height));
        return field;
    }

    private JPanel statCard(String title, JLabel value) {
        JPanel card = cardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                new EmptyBorder(7, 6, 7, 6)));
        String wrappedTitle = switch (title) {
            case "EDGES EXAMINED" -> "EDGES<br>EXAMINED";
            case "EXECUTION STEP" -> "EXECUTION<br>STEP";
            case "SHORTEST DISTANCE" -> "SHORTEST<br>DISTANCE";
            case "FINAL DISTANCE" -> "FINAL<br>DISTANCE";
            default -> title;
        };
        JLabel heading = label("<html><div style='text-align:center'>"
                + wrappedTitle + "</div></html>", new Color(0x8290aa), Font.BOLD, 8);
        heading.setHorizontalAlignment(JLabel.CENTER);
        heading.setAlignmentX(CENTER_ALIGNMENT);
        heading.setToolTipText(title);
        value.setForeground(new Color(0xf0f2fc));
        value.setFont(font(Font.BOLD, 17));
        value.setHorizontalAlignment(JLabel.CENTER);
        value.setAlignmentX(CENTER_ALIGNMENT);
        value.setToolTipText(value.getText());
        card.add(heading);
        card.add(Box.createVerticalStrut(4));
        card.add(value);
        return card;
    }

    private JPanel traceCard(String title, JLabel value) {
        JPanel card = cardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                new EmptyBorder(7, 9, 7, 9)));
        card.add(label(title, new Color(0x71809b), Font.BOLD, 8));
        card.add(Box.createVerticalStrut(4));
        value.setForeground(new Color(0xe0e5f3));
        value.setFont(font(Font.PLAIN, 10));
        value.setToolTipText(value.getText());
        card.add(value);
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return card;
    }

    private JPanel legendItem(String text, Color color) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        item.setOpaque(false);
        JPanel swatch = new JPanel();
        swatch.setBackground(color);
        swatch.setPreferredSize(new Dimension(9, 9));
        item.add(swatch);
        item.add(label(text, BODY_TEXT, Font.PLAIN, 10));
        return item;
    }

    private JLabel sectionHeading(String text) {
        JLabel heading = label(text, ACCENT_TEXT, Font.BOLD, 9);
        heading.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                new EmptyBorder(0, 0, 7, 0)));
        heading.setAlignmentX(LEFT_ALIGNMENT);
        return heading;
    }

    private JPanel cardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(PANEL_BACKGROUND);
        panel.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        return panel;
    }

    private void styleButton(JButton button, boolean primary) {
        AlgoVisionTheme.styleButton(button, primary, 120);
        button.setAlignmentX(LEFT_ALIGNMENT);
    }

    private void styleToggleButton(JToggleButton button) {
        AlgoVisionTheme.styleToggleButton(button);
        button.setPreferredSize(new Dimension(84, 36));
    }

    private void updateModeStyles() {
        for (JToggleButton button : List.of(selectModeButton, addNodeModeButton, addEdgeModeButton)) {
            boolean selected = button.isSelected();
            button.setForeground(selected ? Color.WHITE : AlgoVisionTheme.TEXT_BODY);
            button.setBackground(selected ? AlgoVisionTheme.PRIMARY_SELECTED
                    : AlgoVisionTheme.SECONDARY);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(selected
                            ? AlgoVisionTheme.ACCENT : AlgoVisionTheme.BORDER_SUBTLE),
                    new EmptyBorder(7, 3, 7, 3)));
        }
    }

    private void styleBadge(JLabel badge, Color background, Color foreground) {
        badge.setOpaque(true);
        badge.setBackground(background);
        badge.setForeground(foreground);
        badge.setFont(font(Font.BOLD, 10));
        badge.setBorder(new EmptyBorder(7, 10, 7, 10));
    }

    private void styleStatus() {
        Color background;
        Color foreground;
        switch (statusValue.getText()) {
            case "Preparing" -> {
                background = new Color(0x18324a);
                foreground = new Color(0x8bd5ff);
            }
            case "Running" -> {
                background = new Color(0x16372f);
                foreground = new Color(0x7ce2bf);
            }
            case "Paused" -> {
                background = new Color(0x40301d);
                foreground = AlgoVisionTheme.WARNING;
            }
            case "Completed" -> {
                background = new Color(0x30264b);
                foreground = new Color(0xc4aaff);
            }
            case "Failed" -> {
                background = new Color(0x46252d);
                foreground = AlgoVisionTheme.ERROR;
            }
            default -> {
                background = AlgoVisionTheme.SECONDARY;
                foreground = AlgoVisionTheme.TEXT_BODY;
            }
        }
        styleBadge(statusValue, background, foreground);
    }

    private JLabel label(String text, Color color, int style, int size) {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(font(style, size));
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private JLabel wrappedLabel(String text, Color color, int size) {
        JLabel label = new JLabel("<html><div style='width: 100%;'>" + escapeHtml(text) + "</div></html>");
        label.setForeground(color);
        label.setFont(font(Font.PLAIN, size));
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void setTraceValue(JLabel label, String value) {
        label.setText("<html><div style='width: 220px;'>" + escapeHtml(value) + "</div></html>");
        label.setToolTipText(value);
    }

    private Font font(int style, int size) {
        return AlgoVisionTheme.font(style, size);
    }

    private void setInteractionMode(GraphInteractionMode mode) {
        interactionMode = mode;
        canvas.setInteractionMode(mode);
        updateModeStyles();
        setMessage(switch (mode) {
            case SELECT -> "Select / Move mode: click a node or edge; drag nodes to reposition them.";
            case ADD_NODE -> "Add Node mode: click empty canvas space to place a node.";
            case ADD_EDGE -> "Add Edge mode: select two nodes to connect them.";
        });
    }

    private void handleEmptyCanvasClick(double xRatio, double yRatio) {
        switch (interactionMode) {
            case ADD_NODE -> addNode(xRatio, yRatio);
            case ADD_EDGE -> setMessage("Add Edge mode: select two existing nodes to connect them.");
            case SELECT -> {
                selectedNodeId = null;
                pendingNodeId = null;
                selectedEdge = null;
                setMessage("Selection cleared.");
                refreshGraph();
            }
        }
    }

    private void handleNodeClick(int nodeId) {
        if (interactionMode == GraphInteractionMode.ADD_EDGE) {
            selectEdgeEndpoint(nodeId);
        } else {
            selectNode(nodeId);
        }
    }

    private void addNode(double xRatio, double yRatio) {
        resetTraversal();
        GraphNode node = graph.addNode();
        nodePositions.put(node.id(), new GraphCanvasState.NodePosition(xRatio, yRatio));
        selectedNodeId = node.id();
        pendingNodeId = null;
        selectedEdge = null;
        updateNodeChoices();
        setMessage("Node " + node.label() + " added. Choose Add Edge mode to connect nodes.");
        refreshGraph();
    }

    private void selectNode(int nodeId) {
        if (!graph.containsNode(nodeId)) {
            return;
        }
        selectedEdge = null;
        pendingNodeId = null;
        if (selectedNodeId != null && selectedNodeId == nodeId) {
            selectedNodeId = null;
            setMessage("Node selection cleared.");
        } else {
            selectedNodeId = nodeId;
            setMessage("Node " + nodeId + " selected.");
        }
        refreshGraph();
    }

    private void selectEdgeEndpoint(int nodeId) {
        if (!graph.containsNode(nodeId)) {
            return;
        }
        selectedEdge = null;
        if (pendingNodeId == null) {
            pendingNodeId = nodeId;
            selectedNodeId = nodeId;
            setMessage("Node " + nodeId + " selected. Choose another node to create an edge.");
            refreshGraph();
            return;
        }
        if (pendingNodeId == nodeId) {
            pendingNodeId = null;
            setMessage("Edge selection cleared.");
            refreshGraph();
            return;
        }

        Long weight = readEdgeWeight();
        if (weight == null) {
            refreshGraph();
            return;
        }
        int firstNode = pendingNodeId;
        resetTraversal();
        if (graph.connect(firstNode, nodeId, weight)) {
            selectedNodeId = nodeId;
            setMessage("Connected nodes " + firstNode + " and " + nodeId + " with weight " + weight + ".");
        } else {
            selectedEdge = graph.getEdge(firstNode, nodeId);
            edgeWeightInput.setText(Long.toString(selectedEdge.weight()));
            selectedNodeId = null;
            setMessage("Those nodes are already connected. The existing edge is selected.");
        }
        pendingNodeId = null;
        refreshGraph();
    }

    private void selectEdge(GraphEdge edge) {
        pendingNodeId = null;
        selectedNodeId = null;
        selectedEdge = edge;
        edgeWeightInput.setText(Long.toString(edge.weight()));
        setMessage("Edge " + edge.fromId() + " - " + edge.toId() + " selected.");
        refreshGraph();
    }

    private void updateSelectedEdgeWeight() {
        if (selectedEdge == null) {
            setMessage("Select an edge before updating its weight.");
            return;
        }
        Long weight = readEdgeWeight();
        if (weight == null) {
            return;
        }
        resetTraversal();
        graph.setEdgeWeight(selectedEdge.fromId(), selectedEdge.toId(), weight);
        selectedEdge = graph.getEdge(selectedEdge.fromId(), selectedEdge.toId());
        setMessage("Updated edge " + selectedEdge.fromId() + " - " + selectedEdge.toId()
                + " to weight " + weight + ".");
        refreshGraph();
    }

    private void deleteSelectedNode() {
        if (selectedNodeId == null || !graph.containsNode(selectedNodeId)) {
            return;
        }
        int nodeId = selectedNodeId;
        resetTraversal();
        graph.removeNode(nodeId);
        nodePositions.remove(nodeId);
        pendingNodeId = null;
        selectedNodeId = null;
        selectedEdge = null;
        updateNodeChoices();
        setMessage("Node " + nodeId + " deleted.");
        refreshGraph();
    }

    private void deleteSelectedEdge() {
        if (selectedEdge == null) {
            return;
        }
        GraphEdge edge = selectedEdge;
        resetTraversal();
        graph.removeEdge(edge);
        selectedEdge = null;
        pendingNodeId = null;
        selectedNodeId = null;
        setMessage("Edge " + edge.fromId() + " - " + edge.toId() + " deleted.");
        refreshGraph();
    }

    private void clearGraph() {
        resetTraversal();
        graph = new Graph();
        nodePositions.clear();
        pendingNodeId = null;
        selectedNodeId = null;
        selectedEdge = null;
        updateNodeChoices();
        setMessage("Graph cleared. Choose Add Node mode to create a node.");
        refreshGraph();
    }

    private void generateRandomGraph() {
        resetTraversal();
        Random random = new Random();
        graph = RandomGraphGenerator.generate(random);
        nodePositions.clear();
        pendingNodeId = null;
        selectedNodeId = null;
        selectedEdge = null;
        List<GraphNode> nodes = graph.nodes();
        int nodeCount = nodes.size();
        double angleOffset = random.nextDouble() * Math.PI * 2;
        for (int index = 0; index < nodeCount; index++) {
            double angle = angleOffset + index * Math.PI * 2 / nodeCount;
            double radius = index % 2 == 0 ? 0.36 : 0.39;
            nodePositions.put(nodes.get(index).id(), new GraphCanvasState.NodePosition(
                    0.5 + radius * Math.cos(angle),
                    0.5 + radius * Math.sin(angle)));
        }
        updateNodeChoices(true);
        setMessage("Random connected weighted graph generated. Choose an algorithm and press Start.");
        refreshGraph();
    }

    private void selectAlgorithm() {
        GraphTraversalAlgorithm algorithm = (GraphTraversalAlgorithm) algorithmChoice.getSelectedItem();
        if (algorithm == null || algorithm == selectedAlgorithm) {
            return;
        }
        selectedAlgorithm = algorithm;
        destinationNodeChoice.setEnabled(selectedAlgorithm instanceof DijkstraShortestPath);
        resetTraversal();
        setMessage(selectedAlgorithm.name() + " selected.");
        refreshGraph();
    }

    private void updateNodeChoices() {
        updateNodeChoices(false);
    }

    private void updateNodeChoices(boolean resetSelection) {
        Integer previousSource = resetSelection ? null : (Integer) startNodeChoice.getSelectedItem();
        Integer previousDestination = resetSelection
                ? null : (Integer) destinationNodeChoice.getSelectedItem();
        updatingNodeChoices = true;
        try {
            List<Integer> nodeIds = graph.nodes().stream().map(GraphNode::id).toList();
            startNodeChoice.removeAllItems();
            destinationNodeChoice.removeAllItems();
            nodeIds.forEach(startNodeChoice::addItem);
            nodeIds.forEach(destinationNodeChoice::addItem);
            selectAvailableNode(startNodeChoice, previousSource, nodeIds);
            selectAvailableNode(destinationNodeChoice, previousDestination, nodeIds);
            if (resetSelection && !nodeIds.isEmpty()) {
                destinationNodeChoice.setSelectedIndex(nodeIds.size() - 1);
            }
        } finally {
            updatingNodeChoices = false;
        }
    }

    private void selectAvailableNode(JComboBox<Integer> choice, Integer previous, List<Integer> nodeIds) {
        if (previous != null && nodeIds.contains(previous)) {
            choice.setSelectedItem(previous);
        } else if (!nodeIds.isEmpty()) {
            choice.setSelectedIndex(0);
        }
    }

    private void start() {
        if (stepWorker != null || isAnimationTimerRunning()
                || "Paused".equals(statusValue.getText())
                || "Completed".equals(statusValue.getText())) {
            return;
        }
        Integer source = (Integer) startNodeChoice.getSelectedItem();
        if (source == null || !graph.containsNode(source)) {
            setMessage("Choose a valid source node.");
            return;
        }
        Integer destination = (Integer) destinationNodeChoice.getSelectedItem();
        if (selectedAlgorithm instanceof DijkstraShortestPath
                && (destination == null || !graph.containsNode(destination))) {
            setMessage("Choose a valid destination node for Dijkstra.");
            return;
        }

        cancelCurrentRun();
        int runId = generationId;
        Graph graphSnapshot = graph.copy();
        GraphTraversalAlgorithm algorithm = selectedAlgorithm;
        int sourceId = source;
        int destinationId = destination == null ? source : destination;
        setStatus("Preparing");
        setMessage("Preparing " + algorithm.name() + " traversal steps.");
        updateButtons();

        stepWorker = new SwingWorker<>() {
            @Override
            protected List<GraphVisualizationStep> doInBackground() {
                return new ArrayList<>(algorithm.generateSteps(
                        graphSnapshot, sourceId, destinationId));
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
                    setMessage(algorithm.name() + " traversal started.");
                    updateTimerSpeed();
                    startAnimationTimer(runId);
                    updateButtons();
                } catch (CancellationException exception) {
                    setStatus("Ready");
                    updateButtons();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    setStatus("Failed");
                    setMessage("Traversal step generation was interrupted.");
                    updateButtons();
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    setStatus("Failed");
                    setMessage("Could not prepare traversal: "
                            + (cause == null ? "unknown error" : cause.getMessage()));
                    updateButtons();
                }
            }
        };
        stepWorker.execute();
    }

    private void advance() {
        if (stepIndex >= steps.size()) {
            finish();
            return;
        }
        currentStep = steps.get(stepIndex++);
        if (currentStep instanceof GraphTraversalStep traversalStep
                && traversalStep.event() == GraphTraversalEvent.INSPECT_EDGE
                && traversalStep.currentEdge() != null) {
            exploredEdges.add(traversalStep.currentEdge());
            edgesExamined++;
        } else if (currentStep instanceof DijkstraStep dijkstraStep
                && dijkstraStep.event() == DijkstraEvent.EDGE_RELAXATION
                && dijkstraStep.currentEdge() != null) {
            exploredEdges.add(dijkstraStep.currentEdge());
            edgesExamined = dijkstraStep.edgesExamined();
        }
        if (currentStep instanceof DijkstraStep dijkstraStep) {
            setMessage(messageFor(dijkstraStep));
        } else if (currentStep instanceof GraphTraversalStep traversalStep) {
            setMessage(messageFor(traversalStep));
        }
        renderCanvas();
        updateStatistics();
        if (stepIndex >= steps.size()) {
            finish();
        }
    }

    private void finish() {
        stopAnimationTimer();
        setStatus("Completed");
        updateStatistics();
        updateButtons();
        if (currentStep instanceof DijkstraStep dijkstraStep) {
            if (dijkstraStep.shortestDistance() == null) {
                setMessage("No path exists from node " + dijkstraStep.sourceNode()
                        + " to node " + dijkstraStep.destinationNode() + ".");
            } else {
                setMessage("Shortest path found: " + formatPath(dijkstraStep.shortestPath())
                        + " (distance " + dijkstraStep.shortestDistance() + ").");
            }
            return;
        }
        if (currentStep != null && currentStep.visitedNodes().size() < graph.nodeCount()) {
            setMessage("Traversal complete. " + currentStep.visitedNodes().size() + " of "
                    + graph.nodeCount() + " nodes are reachable from the selected start.");
        } else {
            setMessage("Traversal complete.");
        }
    }

    private void resume() {
        if ("Paused".equals(statusValue.getText()) && stepIndex < steps.size()) {
            setStatus("Running");
            setMessage("Traversal resumed.");
            updateTimerSpeed();
            startAnimationTimer(generationId);
            updateButtons();
        }
    }

    private void resetTraversal() {
        cancelCurrentRun();
        currentStep = null;
        setStatus("Ready");
        updateStatistics();
        updateButtons();
        renderCanvas();
    }

    private void cancelCurrentRun() {
        generationId++;
        stopAnimationTimer();
        if (stepWorker != null) {
            stepWorker.cancel(true);
            stepWorker = null;
        }
        steps = List.of();
        stepIndex = 0;
        edgesExamined = 0;
        exploredEdges.clear();
    }

    private void refreshGraph() {
        updateStatistics();
        updateButtons();
        renderCanvas();
    }

    private void renderCanvas() {
        canvas.render(new GraphCanvasState(
                graph.nodes(), graph.edges(), nodePositions, currentStep, exploredEdges,
                selectedNodeId, selectedEdge));
    }

    private void updateStatistics() {
        nodeCountValue.setText(Integer.toString(graph.nodeCount()));
        edgeCountValue.setText(Integer.toString(graph.edgeCount()));
        visitedCountValue.setText(Integer.toString(
                currentStep == null ? 0 : currentStep.visitedNodes().size()));
        stepValue.setText(stepIndex + " / " + steps.size());
        edgesExaminedValue.setText(Integer.toString(edgesExamined));
        algorithmValue.setText(selectedAlgorithm.name());
        setTraceValue(traversalOrderValue, currentStep == null || currentStep.traversalOrder().isEmpty()
                ? "--"
                : currentStep.traversalOrder().stream()
                        .map(String::valueOf).collect(Collectors.joining(" -> ")));
        if (currentStep instanceof DijkstraStep dijkstraStep) {
            setTraceValue(frontierValue, "Priority queue: " + dijkstraStep.priorityQueueState().stream()
                    .map(entry -> entry.nodeId() + " (" + entry.distance() + ")")
                    .collect(Collectors.joining(", ", "[", "]")));
            BigInteger currentDistance = dijkstraStep.distances().get(dijkstraStep.destinationNode());
            currentDistanceValue.setText(currentDistance == null ? "INF" : currentDistance.toString());
            finalDistanceValue.setText(dijkstraStep.shortestDistance() == null
                    ? dijkstraStep.event() == DijkstraEvent.COMPLETE
                            || dijkstraStep.event() == DijkstraEvent.FINAL_SHORTEST_PATH ? "No path" : "--"
                    : dijkstraStep.shortestDistance().toString());
            setTraceValue(shortestPathValue, dijkstraStep.event() == DijkstraEvent.COMPLETE
                    || dijkstraStep.event() == DijkstraEvent.FINAL_SHORTEST_PATH
                            ? dijkstraStep.shortestPath().isEmpty()
                                    ? "No path" : formatPath(dijkstraStep.shortestPath())
                            : "--");
        } else if (currentStep == null) {
            setTraceValue(frontierValue, "--");
            currentDistanceValue.setText("--");
            finalDistanceValue.setText("--");
            setTraceValue(shortestPathValue, "--");
        } else if (selectedAlgorithm == algorithms.getFirst()) {
            setTraceValue(frontierValue, "Queue: " + currentStep.queueState());
            currentDistanceValue.setText("--");
            finalDistanceValue.setText("--");
            setTraceValue(shortestPathValue, "--");
        } else {
            setTraceValue(frontierValue, "Stack: " + currentStep.stackState());
            currentDistanceValue.setText("--");
            finalDistanceValue.setText("--");
            setTraceValue(shortestPathValue, "--");
        }
    }

    private String messageFor(DijkstraStep step) {
        return switch (step.event()) {
            case START -> "Starting Dijkstra from node " + step.sourceNode() + ".";
            case NODE_EXPLORATION -> "Exploring node " + step.currentNode() + ".";
            case EDGE_RELAXATION -> "Examining edge " + step.currentEdge().fromId() + " - "
                    + step.currentEdge().toId() + " (weight " + step.currentEdge().weight()
                    + ", candidate distance " + step.candidateDistance() + ").";
            case DISTANCE_UPDATE -> "Updated distance to node "
                    + otherEnd(step.currentEdge(), step.currentNode()) + " to "
                    + step.candidateDistance() + ".";
            case PRIORITY_QUEUE_UPDATE -> "Priority queue updated.";
            case FINAL_SHORTEST_PATH -> step.shortestDistance() == null
                    ? "No path exists to destination node " + step.destinationNode() + "."
                    : "Shortest path: " + formatPath(step.shortestPath()) + ".";
            case COMPLETE -> "Dijkstra completed.";
        };
    }

    private String messageFor(GraphTraversalStep step) {
        return switch (step.event()) {
            case START -> "Starting " + selectedAlgorithm.name() + " from node " + step.currentNode() + ".";
            case VISIT -> "Visiting node " + step.currentNode() + ".";
            case INSPECT_EDGE -> "Exploring edge " + step.currentEdge().fromId()
                    + " - " + step.currentEdge().toId() + ".";
            case DISCOVER -> "Discovered node " + step.currentNode() + ".";
            case BACKTRACK -> "Backtracking from node " + step.currentNode() + ".";
            case COMPLETE -> "Traversal complete.";
        };
    }

    private int otherEnd(GraphEdge edge, int nodeId) {
        return edge.fromId() == nodeId ? edge.toId() : edge.fromId();
    }

    private String formatPath(List<Integer> path) {
        return path.stream().map(String::valueOf).collect(Collectors.joining(" -> "));
    }

    private Long readEdgeWeight() {
        String text = edgeWeightInput.getText();
        try {
            long weight = Long.parseLong(text.trim());
            if (weight < 0) {
                setMessage("Edge weights must be non-negative.");
                return null;
            }
            return weight;
        } catch (NumberFormatException exception) {
            setMessage("Enter a non-negative whole-number edge weight.");
            return null;
        }
    }

    private void updateTimerSpeed() {
        double rate = 0.25 + speedSlider.getValue() / 100.0 * 3.75;
        int delay = (int) Math.round(BASE_INTERVAL_MILLIS / rate);
        timerDelayMillis = Math.max(MIN_INTERVAL_MILLIS, Math.min(MAX_INTERVAL_MILLIS, delay));
        if (animationTimer != null) {
            animationTimer.setDelay(timerDelayMillis);
            animationTimer.setInitialDelay(timerDelayMillis);
        }
    }

    private void startAnimationTimer(int runId) {
        stopAnimationTimer();
        Timer timer = new Timer(timerDelayMillis, null);
        timer.addActionListener(event -> {
            if (animationTimer != timer || runId != generationId
                    || !"Running".equals(statusValue.getText())) {
                return;
            }
            advance();
        });
        timer.setCoalesce(true);
        timer.setInitialDelay(timerDelayMillis);
        animationTimer = timer;
        timer.start();
    }

    private void stopAnimationTimer() {
        if (animationTimer != null) {
            animationTimer.stop();
            animationTimer = null;
        }
    }

    private void updateButtons() {
        String status = statusValue.getText();
        boolean preparing = "Preparing".equals(status);
        boolean running = "Running".equals(status);
        boolean paused = "Paused".equals(status);
        boolean completed = "Completed".equals(status);
        algorithmChoice.setEnabled(!preparing);
        startNodeChoice.setEnabled(!preparing && graph.nodeCount() > 0);
        destinationNodeChoice.setEnabled(!preparing
                && selectedAlgorithm instanceof DijkstraShortestPath && graph.nodeCount() > 0);
        edgeWeightInput.setEnabled(!preparing);
        randomGraphButton.setEnabled(!preparing);
        clearGraphButton.setEnabled(!preparing && graph.nodeCount() > 0);
        deleteNodeButton.setEnabled(!preparing && selectedNodeId != null);
        deleteEdgeButton.setEnabled(!preparing && selectedEdge != null);
        updateEdgeWeightButton.setEnabled(!preparing && selectedEdge != null);
        canvas.setEnabled(!preparing);
        selectModeButton.setEnabled(!preparing);
        addNodeModeButton.setEnabled(!preparing);
        addEdgeModeButton.setEnabled(!preparing);
        startButton.setEnabled(!preparing && !running && !paused && !completed
                && graph.nodeCount() > 0 && startNodeChoice.getSelectedItem() != null
                && (!(selectedAlgorithm instanceof DijkstraShortestPath)
                        || destinationNodeChoice.getSelectedItem() != null));
        pauseButton.setEnabled(running);
        resumeButton.setEnabled(paused);
        resetButton.setEnabled(preparing || running || paused || completed || !steps.isEmpty());
    }

    private void setStatus(String status) {
        statusValue.setText(status);
        styleStatus();
    }

    private void setMessage(String message) {
        messageValue.setText(message == null ? "" : message);
        messageValue.setCaretPosition(0);
    }

    Graph graphForTesting() {
        return graph;
    }

    GraphCanvasView canvasForTesting() {
        return canvas;
    }

    GraphCanvasState canvasStateForTesting() {
        return canvas.currentState();
    }

    boolean isAnimationTimerRunning() {
        return animationTimer != null && animationTimer.isRunning();
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

    Integer pendingNodeForTesting() {
        return pendingNodeId;
    }

    Integer selectedNodeForTesting() {
        return selectedNodeId;
    }

    Integer startNodeForTesting() {
        return (Integer) startNodeChoice.getSelectedItem();
    }

    Integer destinationNodeForTesting() {
        return (Integer) destinationNodeChoice.getSelectedItem();
    }

    GraphEdge selectedEdgeForTesting() {
        return selectedEdge;
    }

    int timerDelayForTesting() {
        return timerDelayMillis;
    }
}
