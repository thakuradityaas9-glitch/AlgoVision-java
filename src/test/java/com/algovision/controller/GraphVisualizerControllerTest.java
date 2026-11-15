package com.algovision.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JToggleButton;
import javax.swing.JSlider;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.algovision.graph.model.GraphEdge;
import com.algovision.graph.model.Graph;
import com.algovision.graph.model.DijkstraStep;
import com.algovision.graph.model.GraphTraversalStep;
import com.algovision.visualization.GraphCanvasView;

class GraphVisualizerControllerTest {
    @Test
    void controlsTrackCompactViewportWidthWithoutClippingButtons() throws Exception {
        GraphVisualizerController controller = createController();
        SwingUtilities.invokeAndWait(() -> {
            JScrollPane scrollPane = findScrollPane(controller, "graphControls");
            assertNotNull(scrollPane);
            assertEquals(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER,
                    scrollPane.getHorizontalScrollBarPolicy());
            scrollPane.setSize(new Dimension(320, 420));
            scrollPane.doLayout();

            JViewport viewport = scrollPane.getViewport();
            Container controls = (Container) viewport.getView();
            controls.setSize(viewport.getExtentSize().width, 900);
            layoutTree(controls);
            assertEquals(viewport.getExtentSize().width, controls.getWidth(),
                    "The controls panel must shrink to the available viewport width.");
            for (JButton button : buttons(controls)) {
                int right = SwingUtilities.convertPoint(
                        button, button.getWidth(), 0, controls).x;
                assertTrue(right <= controls.getWidth(),
                        button.getText() + " must remain within the controls panel.");
                assertTrue(button.getWidth() >= button.getMinimumSize().width,
                        button.getText() + " must retain enough width to remain usable.");
            }
        });
    }

    @Test
    void explicitModesPreventAccidentalEdgesAndSupportSelectionEditingAndDragging() throws Exception {
        GraphVisualizerController controller = createController();
        SwingUtilities.invokeAndWait(() -> {
            GraphCanvasView canvas = controller.canvasForTesting();
            canvas.setSize(500, 400);
            assertFalse(button(controller, "Start").isEnabled());
            assertTrue(button(controller, "Generate Random Graph").isEnabled());
            toggleButton(controller, "Add Node").doClick();
            click(canvas, 100, 100);
            click(canvas, 400, 200);
            assertEquals(2, controller.graphForTesting().nodeCount());
            assertEquals(0, controller.graphForTesting().edgeCount(),
                    "Adding/selecting nodes must not implicitly create edges.");

            toggleButton(controller, "Add Edge").doClick();
            click(canvas, 100, 100);
            assertEquals(1, controller.pendingNodeForTesting());
            click(canvas, 400, 200);
            assertEquals(1, controller.graphForTesting().edgeCount());
            assertEquals(1, controller.graphForTesting().getEdge(1, 2).weight());

            toggleButton(controller, "Select / Move").doClick();
            click(canvas, 250, 150);
            GraphEdge selected = controller.selectedEdgeForTesting();
            assertNotNull(selected);
            assertEquals(new GraphEdge(1, 2, 1), selected);
            textField(controller).setText("7");
            button(controller, "Update Edge Weight").doClick();
            assertEquals(7, controller.graphForTesting().getEdge(1, 2).weight());

            button(controller, "Delete Selected Edge").doClick();
            assertEquals(0, controller.graphForTesting().edgeCount());
            toggleButton(controller, "Add Edge").doClick();
            click(canvas, 101, 102);
            click(canvas, 400, 200);
            assertEquals(1, controller.graphForTesting().edgeCount());

            toggleButton(controller, "Select / Move").doClick();
            click(canvas, 101, 102);
            assertEquals(1, controller.selectedNodeForTesting());
            assertEquals(1, controller.graphForTesting().edgeCount(),
                    "Selecting nodes in Select mode must not create edges.");
            click(canvas, 250, 150);
            button(controller, "Delete Selected Edge").doClick();
            assertEquals(0, controller.graphForTesting().edgeCount());
            click(canvas, 101, 102);
            button(controller, "Delete Selected Node").doClick();
            assertEquals(1, controller.graphForTesting().nodeCount());

            toggleButton(controller, "Add Node").doClick();
            click(canvas, 250, 250, MouseEvent.BUTTON3);
            assertEquals(1, controller.graphForTesting().nodeCount(),
                    "Right-clicks must not trigger canvas actions.");
            click(canvas, 250, 250);
            assertEquals(2, controller.graphForTesting().nodeCount(),
                    "Add Node mode creates a node only on empty space.");

            toggleButton(controller, "Select / Move").doClick();
            canvas.dispatchEvent(mouse(canvas, MouseEvent.MOUSE_PRESSED, 400, 200,
                    0, MouseEvent.BUTTON1));
            canvas.dispatchEvent(mouse(canvas, MouseEvent.MOUSE_DRAGGED, 150, 150,
                    MouseEvent.BUTTON1_DOWN_MASK, MouseEvent.NOBUTTON));
            canvas.dispatchEvent(mouse(canvas, MouseEvent.MOUSE_RELEASED, 150, 150,
                    0, MouseEvent.BUTTON1));
            click(canvas, 150, 150);
            assertEquals(2, controller.graphForTesting().nodeCount(),
                    "Finishing a node drag must not create or select another node.");
            click(canvas, 150, 150);
            assertEquals(2, controller.selectedNodeForTesting());
        });
    }

    @Test
    void playbackPauseResumeSpeedAlgorithmSwitchAndGraphEditStopCurrentTimer() throws Exception {
        GraphVisualizerController controller = createController();
        prepareTwoNodeGraph(controller);

        SwingUtilities.invokeAndWait(() -> button(controller, "Start").doClick());
        awaitStatus(controller, "Running");
        assertTrue(onEdt(controller::isAnimationTimerRunning));
        int originalDelay = onEdt(controller::timerDelayForTesting);
        SwingUtilities.invokeAndWait(() -> slider(controller).setValue(100));
        assertTrue(onEdt(controller::timerDelayForTesting) < originalDelay,
                "Increasing speed should reduce the timer delay.");

        SwingUtilities.invokeAndWait(() -> button(controller, "Pause").doClick());
        assertEquals("Paused", onEdt(controller::statusForTesting));
        assertFalse(onEdt(controller::isAnimationTimerRunning));
        int pausedAt = onEdt(controller::stepIndexForTesting);
        Thread.sleep(400);
        assertEquals(pausedAt, onEdt(controller::stepIndexForTesting),
                "Paused traversal must not advance.");

        SwingUtilities.invokeAndWait(() -> button(controller, "Resume").doClick());
        assertTrue(onEdt(controller::isAnimationTimerRunning));
        long stepDeadline = System.nanoTime() + 2_000_000_000L;
        while (onEdt(controller::stepIndexForTesting) == pausedAt
                && System.nanoTime() < stepDeadline) {
            Thread.sleep(10);
        }
        assertTrue(onEdt(controller::stepIndexForTesting) > pausedAt,
                "Resume should continue beyond the exact paused step.");
        SwingUtilities.invokeAndWait(() -> comboBox(controller).setSelectedIndex(1));
        assertEquals("Ready", onEdt(controller::statusForTesting));
        assertFalse(onEdt(controller::isAnimationTimerRunning),
                "Changing algorithms must stop the previous timer.");

        SwingUtilities.invokeAndWait(() -> button(controller, "Start").doClick());
        awaitStatus(controller, "Running");
        SwingUtilities.invokeAndWait(() -> {
            toggleButton(controller, "Add Node").doClick();
            click(canvas(controller), 250, 320);
        });
        assertEquals(3, onEdt(() -> controller.graphForTesting().nodeCount()));
        assertEquals("Ready", onEdt(controller::statusForTesting));
        assertFalse(onEdt(controller::isAnimationTimerRunning),
                "Editing the graph must stop traversal before changing graph state.");

        SwingUtilities.invokeAndWait(() -> button(controller, "Start").doClick());
        awaitStatus(controller, "Running");
        SwingUtilities.invokeAndWait(() -> button(controller, "Reset").doClick());
        assertEquals("Ready", onEdt(controller::statusForTesting));
        assertFalse(onEdt(controller::isAnimationTimerRunning));
    }

    @Test
    void randomGenerationRepeatedlyReplacesGraphAndClearsTraversalState() throws Exception {
        GraphVisualizerController controller = createController();
        SwingUtilities.invokeAndWait(() -> {
            JButton generate = button(controller, "Generate Random Graph");
            assertEquals(1, generate.getActionListeners().length,
                    "Repeated random generation must not register additional listeners.");
            for (int iteration = 0; iteration < 75; iteration++) {
                generate.doClick();
                assertTrue(controller.graphForTesting().nodeCount() >= 7);
                assertEquals(controller.graphForTesting().nodeCount(),
                        controller.canvasStateForTesting().nodes().size());
                assertEquals(controller.graphForTesting().edgeCount(),
                        controller.canvasStateForTesting().edges().size());
                assertNull(controller.canvasStateForTesting().traversalStep());
                assertEquals(1, controller.startNodeForTesting());
                assertEquals(controller.graphForTesting().nodeCount(),
                        controller.destinationNodeForTesting());
            }
        });

        SwingUtilities.invokeAndWait(() -> button(controller, "Start").doClick());
        awaitStatus(controller, "Running");
        SwingUtilities.invokeAndWait(() -> button(controller, "Pause").doClick());
        assertEquals("Paused", onEdt(controller::statusForTesting));
        int pausedAt = onEdt(controller::stepIndexForTesting);
        Thread.sleep(250);
        assertEquals(pausedAt, onEdt(controller::stepIndexForTesting));

        SwingUtilities.invokeAndWait(() -> button(controller, "Resume").doClick());
        assertTrue(onEdt(controller::isAnimationTimerRunning));
        Graph oldGraph = onEdt(controller::graphForTesting);
        SwingUtilities.invokeAndWait(() -> button(controller, "Generate Random Graph").doClick());
        assertFalse(onEdt(controller::isAnimationTimerRunning));
        assertNotNull(onEdt(controller::graphForTesting));
        assertFalse(oldGraph == onEdt(controller::graphForTesting),
                "Random generation must replace the old graph instance.");
        assertNull(onEdt(controller::canvasStateForTesting).traversalStep());
        assertEquals("Ready", onEdt(controller::statusForTesting));
    }

    @Test
    void workflowRunsDfsAndDijkstraAndSwitchingAlgorithmsInvalidatesPlayback() throws Exception {
        GraphVisualizerController controller = createController();
        SwingUtilities.invokeAndWait(() -> button(controller, "Generate Random Graph").doClick());
        SwingUtilities.invokeAndWait(() -> button(controller, "Start").doClick());
        awaitStatus(controller, "Running");
        SwingUtilities.invokeAndWait(() -> comboBox(controller).setSelectedIndex(1));
        assertEquals("Ready", onEdt(controller::statusForTesting));
        assertFalse(onEdt(controller::isAnimationTimerRunning));

        SwingUtilities.invokeAndWait(() -> button(controller, "Start").doClick());
        awaitStatus(controller, "Running");
        SwingUtilities.invokeAndWait(() -> slider(controller).setValue(100));
        assertTrue(onEdt(controller::timerDelayForTesting) <= 120);
        awaitStatus(controller, "Completed");
        GraphTraversalStep dfsResult = (GraphTraversalStep) onEdt(
                () -> controller.canvasStateForTesting().traversalStep());
        assertEquals(onEdt(() -> controller.graphForTesting().nodeCount()),
                dfsResult.visitedNodes().size());

        SwingUtilities.invokeAndWait(() -> button(controller, "Reset").doClick());
        SwingUtilities.invokeAndWait(() -> button(controller, "Clear Graph").doClick());
        assertEquals(0, onEdt(() -> controller.graphForTesting().nodeCount()));
        assertEquals(0, onEdt(() -> controller.graphForTesting().edgeCount()));
        assertFalse(onEdt(() -> button(controller, "Start").isEnabled()));

        SwingUtilities.invokeAndWait(() -> {
            GraphCanvasView canvas = canvas(controller);
            canvas.setSize(500, 400);
            toggleButton(controller, "Add Node").doClick();
            click(canvas, 100, 100);
            click(canvas, 250, 100);
            click(canvas, 400, 100);
            toggleButton(controller, "Add Edge").doClick();
            textField(controller).setText("2");
            click(canvas, 100, 100);
            click(canvas, 250, 100);
            textField(controller).setText("3");
            click(canvas, 250, 100);
            click(canvas, 400, 100);
            textField(controller).setText("20");
            click(canvas, 100, 100);
            click(canvas, 400, 100);
            comboBoxes(controller).get(0).setSelectedIndex(2);
            comboBoxes(controller).get(1).setSelectedItem(1);
            comboBoxes(controller).get(2).setSelectedItem(3);
        });
        assertEquals(3, onEdt(() -> controller.graphForTesting().edgeCount()));

        SwingUtilities.invokeAndWait(() -> button(controller, "Start").doClick());
        awaitStatus(controller, "Completed");
        DijkstraStep dijkstraResult = (DijkstraStep) onEdt(
                () -> controller.canvasStateForTesting().traversalStep());
        assertEquals(5L, dijkstraResult.shortestDistance().longValue());
        assertEquals(List.of(1, 2, 3), dijkstraResult.shortestPath());
        assertEquals(3, onEdt(() -> controller.graphForTesting().nodeCount()),
                "Resetting traversal must not delete the graph.");
    }

    @Test
    void canvasPaintsFromImmutableSnapshotWithoutChangingIt() throws Exception {
        GraphVisualizerController controller = createController();
        SwingUtilities.invokeAndWait(() -> {
            button(controller, "Generate Random Graph").doClick();
            GraphCanvasView canvas = canvas(controller);
            canvas.setSize(640, 420);
            var before = controller.canvasStateForTesting();
            BufferedImage image = new BufferedImage(640, 420, BufferedImage.TYPE_INT_ARGB);
            var graphics = image.createGraphics();
            canvas.paint(graphics);
            graphics.dispose();
            assertEquals(before, controller.canvasStateForTesting(),
                    "Painting must not mutate visualization state.");
            assertEquals(new java.awt.Color(0x0c1426).getRGB(), image.getRGB(2, 2));
            assertTrue(java.util.Arrays.stream(image.getRGB(0, 0, 640, 420, null, 0, 640))
                    .distinct().count() > 5,
                    "A generated graph must render nodes and edges, not one uniform fill.");
        });
    }

    private GraphVisualizerController createController() throws Exception {
        AtomicReference<GraphVisualizerController> reference = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> reference.set(new GraphVisualizerController()));
        return reference.get();
    }

    private void prepareTwoNodeGraph(GraphVisualizerController controller) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GraphCanvasView canvas = canvas(controller);
            canvas.setSize(500, 400);
            toggleButton(controller, "Add Node").doClick();
            click(canvas, 100, 100);
            click(canvas, 400, 200);
            toggleButton(controller, "Add Edge").doClick();
            click(canvas, 100, 100);
            click(canvas, 400, 200);
            assertNull(controller.pendingNodeForTesting());
            assertEquals(1, controller.graphForTesting().edgeCount());
        });
    }

    private void awaitStatus(GraphVisualizerController controller, String expected) throws Exception {
        long deadline = System.nanoTime() + 10_000_000_000L;
        while (System.nanoTime() < deadline) {
            if (expected.equals(onEdt(controller::statusForTesting))) {
                return;
            }
            Thread.sleep(10);
        }
        assertEquals(expected, onEdt(controller::statusForTesting),
                "Graph workspace did not reach expected status.");
    }

    private <T> T onEdt(java.util.concurrent.Callable<T> action) throws Exception {
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Exception> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                result.set(action.call());
            } catch (Exception exception) {
                failure.set(exception);
            }
        });
        if (failure.get() != null) {
            throw failure.get();
        }
        return result.get();
    }

    private void click(GraphCanvasView canvas, int x, int y) {
        click(canvas, x, y, MouseEvent.BUTTON1);
    }

    private void click(GraphCanvasView canvas, int x, int y, int button) {
        canvas.dispatchEvent(mouse(canvas, MouseEvent.MOUSE_CLICKED, x, y, 0, button));
    }

    private MouseEvent mouse(GraphCanvasView canvas, int id, int x, int y,
            int modifiers, int button) {
        return new MouseEvent(canvas, id, System.currentTimeMillis(), modifiers,
                x, y, 1, false, button);
    }

    private GraphCanvasView canvas(GraphVisualizerController controller) {
        return controller.canvasForTesting();
    }

    private JScrollPane findScrollPane(Container parent, String name) {
        for (Component component : parent.getComponents()) {
            if (component instanceof JScrollPane scrollPane
                    && name.equals(scrollPane.getName())) {
                return scrollPane;
            }
            if (component instanceof Container container) {
                JScrollPane found = findScrollPane(container, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private List<JButton> buttons(Container parent) {
        List<JButton> found = new ArrayList<>();
        for (Component component : parent.getComponents()) {
            if (component instanceof JButton button) {
                found.add(button);
            }
            if (component instanceof Container container) {
                found.addAll(buttons(container));
            }
        }
        return found;
    }

    private void layoutTree(Container parent) {
        parent.doLayout();
        for (Component component : parent.getComponents()) {
            if (component instanceof Container container) {
                layoutTree(container);
            }
        }
    }

    private JButton button(Container parent, String text) {
        for (Component component : parent.getComponents()) {
            if (component instanceof JButton button && text.equals(button.getText())) {
                return button;
            }
            if (component instanceof Container container) {
                JButton found = findButton(container, text);
                if (found != null) {
                    return found;
                }
            }
        }
        throw new AssertionError("Could not find button: " + text);
    }

    private JButton findButton(Container parent, String text) {
        for (Component component : parent.getComponents()) {
            if (component instanceof JButton button && text.equals(button.getText())) {
                return button;
            }
            if (component instanceof Container container) {
                JButton found = findButton(container, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private JToggleButton toggleButton(Container parent, String text) {
        for (Component component : parent.getComponents()) {
            if (component instanceof JToggleButton button && text.equals(button.getText())) {
                return button;
            }
            if (component instanceof Container container) {
                JToggleButton found = findToggleButton(container, text);
                if (found != null) {
                    return found;
                }
            }
        }
        throw new AssertionError("Could not find mode button: " + text);
    }

    private JToggleButton findToggleButton(Container parent, String text) {
        for (Component component : parent.getComponents()) {
            if (component instanceof JToggleButton button && text.equals(button.getText())) {
                return button;
            }
            if (component instanceof Container container) {
                JToggleButton found = findToggleButton(container, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private JComboBox<?> comboBox(Container parent) {
        for (Component component : parent.getComponents()) {
            if (component instanceof JComboBox<?> comboBox) {
                return comboBox;
            }
            if (component instanceof Container container) {
                JComboBox<?> found = findComboBox(container);
                if (found != null) {
                    return found;
                }
            }
        }
        throw new AssertionError("Could not find algorithm selector.");
    }

    private JComboBox<?> findComboBox(Container parent) {
        for (Component component : parent.getComponents()) {
            if (component instanceof JComboBox<?> comboBox) {
                return comboBox;
            }
            if (component instanceof Container container) {
                JComboBox<?> found = findComboBox(container);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private List<JComboBox<?>> comboBoxes(Container parent) {
        List<JComboBox<?>> found = new ArrayList<>();
        collectComboBoxes(parent, found);
        return found;
    }

    private void collectComboBoxes(Container parent, List<JComboBox<?>> found) {
        for (Component component : parent.getComponents()) {
            if (component instanceof JComboBox<?> comboBox) {
                found.add(comboBox);
            } else if (component instanceof Container container) {
                collectComboBoxes(container, found);
            }
        }
    }

    private JTextField textField(Container parent) {
        for (Component component : parent.getComponents()) {
            if (component instanceof JTextField field) {
                return field;
            }
            if (component instanceof Container container) {
                JTextField found = findTextField(container);
                if (found != null) {
                    return found;
                }
            }
        }
        throw new AssertionError("Could not find edge weight input.");
    }

    private JTextField findTextField(Container parent) {
        for (Component component : parent.getComponents()) {
            if (component instanceof JTextField field) {
                return field;
            }
            if (component instanceof Container container) {
                JTextField found = findTextField(container);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private JSlider slider(Container parent) {
        for (Component component : parent.getComponents()) {
            if (component instanceof JSlider slider) {
                return slider;
            }
            if (component instanceof Container container) {
                JSlider found = findSlider(container);
                if (found != null) {
                    return found;
                }
            }
        }
        throw new AssertionError("Could not find speed slider.");
    }

    private JSlider findSlider(Container parent) {
        for (Component component : parent.getComponents()) {
            if (component instanceof JSlider slider) {
                return slider;
            }
            if (component instanceof Container container) {
                JSlider found = findSlider(container);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
