package com.algovision.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.algovision.pathfinding.model.GridPosition;
import com.algovision.pathfinding.model.PathfindingTool;
import com.algovision.visualization.PathfindingGridView;

class PathfindingControllerTest {
    @Test
    void gridToolsMoveEndpointsDrawEraseAndDragWalls() throws Exception {
        PathfindingController controller = createController();
        SwingUtilities.invokeAndWait(() -> {
            PathfindingGridView grid = controller.gridViewForTesting();
            grid.setSize(500, 340);
            JComboBox<?> tools = comboBox(controller);

            tools.setSelectedItem(PathfindingTool.SET_START);
            assertEquals(PathfindingTool.SET_START, tools.getSelectedItem());
            click(grid, 3, 4);
            assertEquals(new GridPosition(3, 4), controller.gridForTesting().start());

            tools.setSelectedItem(PathfindingTool.SET_END);
            click(grid, 6, 7);
            assertEquals(new GridPosition(6, 7), controller.gridForTesting().end());

            tools.setSelectedItem(PathfindingTool.WALL);
            press(grid, 1, 1);
            drag(grid, 1, 2);
            release(grid, 1, 2);
            assertTrue(controller.gridForTesting().isWall(new GridPosition(1, 1)));
            assertTrue(controller.gridForTesting().isWall(new GridPosition(1, 2)));

            tools.setSelectedItem(PathfindingTool.SET_START);
            click(grid, 1, 1);
            assertEquals(new GridPosition(1, 1), controller.gridForTesting().start());
            assertFalse(controller.gridForTesting().isWall(new GridPosition(1, 1)));

            tools.setSelectedItem(PathfindingTool.ERASE);
            click(grid, 1, 2);
            assertFalse(controller.gridForTesting().isWall(new GridPosition(1, 2)));
        });
    }

    @Test
    void randomizeAndClearPreserveEndpointsAndKeepGridValid() throws Exception {
        PathfindingController controller = createController();
        GridPosition start = controller.gridForTesting().start();
        GridPosition end = controller.gridForTesting().end();

        SwingUtilities.invokeAndWait(() -> button(controller, "Random Obstacles").doClick());
        assertTrue(onEdt(() -> controller.gridForTesting().walls().size()) > 0);
        assertFalse(onEdt(() -> controller.gridForTesting()
                .isWall(controller.gridForTesting().start())));
        assertFalse(onEdt(() -> controller.gridForTesting()
                .isWall(controller.gridForTesting().end())));

        SwingUtilities.invokeAndWait(() -> button(controller, "Clear Grid").doClick());
        assertTrue(onEdt(() -> controller.gridForTesting().walls().isEmpty()));
        assertEquals(start, onEdt(() -> controller.gridForTesting().start()));
        assertEquals(end, onEdt(() -> controller.gridForTesting().end()));
    }

    @Test
    void pauseResumeSpeedAndResetKeepSinglePlaybackRun() throws Exception {
        PathfindingController controller = createController();
        SwingUtilities.invokeAndWait(() -> button(controller, "Start").doClick());
        awaitStatus(controller, "Running");
        assertTrue(onEdt(controller::isAnimationTimerRunningForTesting));

        int originalDelay = onEdt(controller::timerDelayForTesting);
        SwingUtilities.invokeAndWait(() -> slider(controller).setValue(100));
        assertTrue(onEdt(controller::timerDelayForTesting) < originalDelay);

        SwingUtilities.invokeAndWait(() -> button(controller, "Pause").doClick());
        assertEquals("Paused", onEdt(controller::statusForTesting));
        assertFalse(onEdt(controller::isAnimationTimerRunningForTesting));
        int pausedAt = onEdt(controller::stepIndexForTesting);
        Thread.sleep(150);
        assertEquals(pausedAt, onEdt(controller::stepIndexForTesting));

        SwingUtilities.invokeAndWait(() -> button(controller, "Resume").doClick());
        assertTrue(onEdt(controller::isAnimationTimerRunningForTesting));
        awaitStepAdvance(controller, pausedAt);

        SwingUtilities.invokeAndWait(() -> button(controller, "Reset").doClick());
        assertEquals("Ready", onEdt(controller::statusForTesting));
        assertFalse(onEdt(controller::isAnimationTimerRunningForTesting));
        assertEquals(0, onEdt(controller::stepIndexForTesting));
        Thread.sleep(120);
        assertEquals(0, onEdt(controller::stepIndexForTesting),
                "Reset must invalidate callbacks queued by the previous timer.");
    }

    @Test
    void reconstructedPathAppearsOneCellAtATimeAndResumesAtItsCurrentStep() throws Exception {
        PathfindingController controller = createController();
        SwingUtilities.invokeAndWait(() -> {
            controller.gridForTesting().clearWalls();
            controller.gridForTesting().setStart(new GridPosition(8, 0));
            controller.gridForTesting().setEnd(new GridPosition(8, 2));
            slider(controller).setValue(0);
            button(controller, "Start").doClick();
        });
        awaitStatus(controller, "Running");
        awaitPathProgress(controller, 1);
        SwingUtilities.invokeAndWait(() -> button(controller, "Pause").doClick());
        int pausedAt = onEdt(controller::pathAnimationIndexForTesting);
        assertEquals(1, pausedAt);
        Thread.sleep(220);
        assertEquals(pausedAt, onEdt(controller::pathAnimationIndexForTesting));

        SwingUtilities.invokeAndWait(() -> button(controller, "Resume").doClick());
        awaitPathProgress(controller, pausedAt + 1);
        assertTrue(onEdt(controller::pathAnimationIndexForTesting) > pausedAt);
        SwingUtilities.invokeAndWait(() -> button(controller, "Reset").doClick());
        assertEquals(0, onEdt(controller::pathAnimationIndexForTesting));
    }

    private PathfindingController createController() throws Exception {
        AtomicReference<PathfindingController> reference = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> reference.set(new PathfindingController()));
        return reference.get();
    }

    private void awaitStatus(PathfindingController controller, String expected) throws Exception {
        long deadline = System.nanoTime() + 10_000_000_000L;
        while (System.nanoTime() < deadline) {
            if (expected.equals(onEdt(controller::statusForTesting))) {
                return;
            }
            Thread.sleep(10);
        }
        assertEquals(expected, onEdt(controller::statusForTesting));
    }

    private void awaitStepAdvance(PathfindingController controller, int initialStep)
            throws Exception {
        long deadline = System.nanoTime() + 2_000_000_000L;
        while (System.nanoTime() < deadline
                && onEdt(controller::stepIndexForTesting) == initialStep) {
            Thread.sleep(10);
        }
        assertTrue(onEdt(controller::stepIndexForTesting) > initialStep);
    }

    private void awaitPathProgress(PathfindingController controller, int minimumProgress)
            throws Exception {
        long deadline = System.nanoTime() + 10_000_000_000L;
        while (System.nanoTime() < deadline
                && onEdt(controller::pathAnimationIndexForTesting) < minimumProgress) {
            Thread.sleep(10);
        }
        assertTrue(onEdt(controller::pathAnimationIndexForTesting) >= minimumProgress);
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

    private JButton button(Container parent, String text) {
        for (Component component : parent.getComponents()) {
            if (component instanceof JButton button && text.equals(button.getText())) {
                return button;
            }
            if (component instanceof Container container) {
                JButton found = buttonIn(container, text);
                if (found != null) {
                    return found;
                }
            }
        }
        throw new AssertionError("Button not found: " + text);
    }

    private JButton buttonIn(Container parent, String text) {
        for (Component component : parent.getComponents()) {
            if (component instanceof JButton button && text.equals(button.getText())) {
                return button;
            }
            if (component instanceof Container container) {
                JButton found = buttonIn(container, text);
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
                JComboBox<?> found = comboBoxIn(container);
                if (found != null) {
                    return found;
                }
            }
        }
        throw new AssertionError("Tool selection combo box not found.");
    }

    private JComboBox<?> comboBoxIn(Container parent) {
        for (Component component : parent.getComponents()) {
            if (component instanceof JComboBox<?> comboBox) {
                return comboBox;
            }
            if (component instanceof Container container) {
                JComboBox<?> found = comboBoxIn(container);
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
                JSlider found = sliderIn(container);
                if (found != null) {
                    return found;
                }
            }
        }
        throw new AssertionError("Speed slider not found.");
    }

    private JSlider sliderIn(Container parent) {
        for (Component component : parent.getComponents()) {
            if (component instanceof JSlider slider) {
                return slider;
            }
            if (component instanceof Container container) {
                JSlider found = sliderIn(container);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private void click(PathfindingGridView grid, int row, int column) {
        PointForCell cell = pointForCell(grid, row, column);
        dispatch(grid, MouseEvent.MOUSE_PRESSED, cell.x(), cell.y(),
                MouseEvent.BUTTON1_DOWN_MASK, MouseEvent.BUTTON1);
        dispatch(grid, MouseEvent.MOUSE_RELEASED, cell.x(), cell.y(), 0, MouseEvent.BUTTON1);
    }

    private void press(PathfindingGridView grid, int row, int column) {
        PointForCell cell = pointForCell(grid, row, column);
        dispatch(grid, MouseEvent.MOUSE_PRESSED, cell.x(), cell.y(),
                MouseEvent.BUTTON1_DOWN_MASK, MouseEvent.BUTTON1);
    }

    private void drag(PathfindingGridView grid, int row, int column) {
        PointForCell cell = pointForCell(grid, row, column);
        dispatch(grid, MouseEvent.MOUSE_DRAGGED, cell.x(), cell.y(),
                MouseEvent.BUTTON1_DOWN_MASK, MouseEvent.NOBUTTON);
    }

    private void release(PathfindingGridView grid, int row, int column) {
        PointForCell cell = pointForCell(grid, row, column);
        dispatch(grid, MouseEvent.MOUSE_RELEASED, cell.x(), cell.y(), 0, MouseEvent.BUTTON1);
    }

    private PointForCell pointForCell(PathfindingGridView grid, int row, int column) {
        return new PointForCell(
                (column * grid.getWidth() / 25) + grid.getWidth() / 50,
                (row * grid.getHeight() / 17) + grid.getHeight() / 34);
    }

    private void dispatch(PathfindingGridView grid, int id, int x, int y,
            int modifiers, int button) {
        MouseEvent event = new MouseEvent(grid, id, System.currentTimeMillis(),
                modifiers, x, y, 1, false, button);
        if (id == MouseEvent.MOUSE_DRAGGED) {
            for (MouseMotionListener listener : grid.getMouseMotionListeners()) {
                listener.mouseDragged(event);
            }
        } else {
            for (MouseListener listener : grid.getMouseListeners()) {
                if (id == MouseEvent.MOUSE_PRESSED) {
                    listener.mousePressed(event);
                } else if (id == MouseEvent.MOUSE_RELEASED) {
                    listener.mouseReleased(event);
                }
            }
        }
    }

    private record PointForCell(int x, int y) {
    }
}
