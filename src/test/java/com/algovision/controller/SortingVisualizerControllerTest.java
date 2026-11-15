package com.algovision.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

class SortingVisualizerControllerTest {
    @Test
    void navigationPauseStopsPlaybackAndCancelsPreparation() throws Exception {
        AtomicReference<SortingVisualizerController> reference = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> reference.set(new SortingVisualizerController()));
        SortingVisualizerController controller = reference.get();

        SwingUtilities.invokeAndWait(() -> {
            button(controller, "Start").doClick();
            controller.pause();
        });
        assertEquals("Ready", onEdt(controller::statusForTesting));
        assertFalse(onEdt(controller::isPreparingSteps));
        assertFalse(onEdt(controller::isAnimationTimerRunning));

        SwingUtilities.invokeAndWait(() -> button(controller, "Start").doClick());
        awaitStatus(controller, "Running");
        SwingUtilities.invokeAndWait(controller::pause);
        assertEquals("Paused", onEdt(controller::statusForTesting));
        assertFalse(onEdt(controller::isAnimationTimerRunning));
    }

    @Test
    void pauseResumeAlgorithmChangeAndResetControlOneTimer() throws Exception {
        AtomicReference<SortingVisualizerController> reference = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> reference.set(new SortingVisualizerController()));
        SortingVisualizerController controller = reference.get();

        SwingUtilities.invokeAndWait(() -> button(controller, "Start").doClick());
        awaitStatus(controller, "Running");
        assertTrue(onEdt(controller::isAnimationTimerRunning));

        SwingUtilities.invokeAndWait(() -> button(controller, "Pause").doClick());
        assertEquals("Paused", onEdt(controller::statusForTesting));
        assertFalse(onEdt(controller::isAnimationTimerRunning));
        int pausedAt = onEdt(controller::stepIndexForTesting);
        Thread.sleep(400);
        assertEquals(pausedAt, onEdt(controller::stepIndexForTesting),
                "Paused playback must not advance.");

        SwingUtilities.invokeAndWait(() -> button(controller, "Resume").doClick());
        assertTrue(onEdt(controller::isAnimationTimerRunning));

        SwingUtilities.invokeAndWait(() -> comboBox(controller).setSelectedIndex(1));
        assertFalse(onEdt(controller::isAnimationTimerRunning),
                "Changing algorithms must stop the previous animation timer.");
        assertEquals("Ready", onEdt(controller::statusForTesting));

        SwingUtilities.invokeAndWait(() -> button(controller, "Start").doClick());
        awaitStatus(controller, "Running");
        SwingUtilities.invokeAndWait(() -> button(controller, "Reset").doClick());
        assertFalse(onEdt(controller::isAnimationTimerRunning),
                "Reset must stop playback.");
        assertFalse(onEdt(controller::isPreparingSteps),
                "Reset must invalidate and cancel pending step generation.");
        assertEquals("Ready", onEdt(controller::statusForTesting));

        Thread.sleep(400);
        assertEquals("Ready", onEdt(controller::statusForTesting),
                "A cancelled run must not restart playback after reset.");
        assertFalse(onEdt(controller::isAnimationTimerRunning));
    }

    private void awaitStatus(SortingVisualizerController controller, String expected) throws Exception {
        long deadline = System.nanoTime() + 5_000_000_000L;
        while (System.nanoTime() < deadline) {
            if (expected.equals(onEdt(controller::statusForTesting))) {
                return;
            }
            Thread.sleep(10);
        }
        assertEquals(expected, onEdt(controller::statusForTesting),
                "Sorting workspace did not reach expected status.");
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
}
