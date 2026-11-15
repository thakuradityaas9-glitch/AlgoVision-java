package com.algovision.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.algovision.algorithms.BubbleSort;
import com.algovision.algorithms.QuickSort;
import com.algovision.model.SortOperation;
import org.junit.jupiter.api.Test;

class AlgorithmRaceControllerTest {
    @Test
    void runsBothLanesToCompletionWithIndependentOperationCounts() throws Exception {
        AlgorithmRaceController controller = createController();
        setInput(controller, "5, 4, 3, 2, 1");
        click(controller, "Start Race");

        await(() -> "Completed".equals(read(controller::raceStatusForTesting)));

        List<?> bubbleSteps = new BubbleSort().generateSteps(new int[] {5, 4, 3, 2, 1});
        List<?> quickSteps = new QuickSort().generateSteps(new int[] {5, 4, 3, 2, 1});
        assertEquals((int) count(bubbleSteps, SortOperation.COMPARE),
                read(() -> controller.laneComparisonsForTesting(true)).intValue());
        assertEquals((int) count(bubbleSteps, SortOperation.SWAP),
                read(() -> controller.laneSwapsForTesting(true)).intValue());
        assertEquals((int) count(bubbleSteps, SortOperation.MOVE, SortOperation.OVERWRITE),
                read(() -> controller.laneMovementsForTesting(true)).intValue());
        assertEquals((int) count(quickSteps, SortOperation.COMPARE),
                read(() -> controller.laneComparisonsForTesting(false)).intValue());
        assertEquals((int) count(quickSteps, SortOperation.SWAP),
                read(() -> controller.laneSwapsForTesting(false)).intValue());
        assertEquals((int) count(quickSteps, SortOperation.MOVE, SortOperation.OVERWRITE),
                read(() -> controller.laneMovementsForTesting(false)).intValue());
        assertEquals("Completed", read(() -> controller.laneStatusForTesting(true)));
        assertEquals("Completed", read(() -> controller.laneStatusForTesting(false)));
        assertFalse(read(() -> controller.laneTimerRunningForTesting(true)));
        assertFalse(read(() -> controller.laneTimerRunningForTesting(false)));
    }

    @Test
    void pauseResumeResetAndSpeedChangesPreserveAnimationProgress() throws Exception {
        AlgorithmRaceController controller = createController();
        setInput(controller, "8, 7, 6, 5, 4, 3, 2, 1");
        setSpeed(controller, 0);

        onEdt(() -> {
            button(controller, "Start Race").doClick();
            controller.pause();
        });
        assertEquals("Ready", read(controller::raceStatusForTesting));
        assertFalse(read(controller::preparationRunningForTesting));

        click(controller, "Start Race");
        await(() -> "Running".equals(read(controller::raceStatusForTesting)));
        await(() -> read(() -> controller.laneStepIndexForTesting(true)) > 0);
        int initialDelay = read(() -> controller.laneTimerDelayForTesting(true));

        setSpeed(controller, 100);
        int fasterDelay = read(() -> controller.laneTimerDelayForTesting(true));
        assertTrue(fasterDelay < initialDelay);
        assertEquals(fasterDelay, read(() -> controller.laneTimerDelayForTesting(false)));

        click(controller, "Pause");
        int pausedFirstStep = read(() -> controller.laneStepIndexForTesting(true));
        int pausedSecondStep = read(() -> controller.laneStepIndexForTesting(false));
        Thread.sleep(120);
        assertEquals("Paused", read(controller::raceStatusForTesting));
        assertEquals(pausedFirstStep, read(() -> controller.laneStepIndexForTesting(true)));
        assertEquals(pausedSecondStep, read(() -> controller.laneStepIndexForTesting(false)));
        assertFalse(read(() -> controller.laneTimerRunningForTesting(true)));
        assertFalse(read(() -> controller.laneTimerRunningForTesting(false)));

        click(controller, "Resume");
        await(() -> "Running".equals(read(controller::raceStatusForTesting)));
        await(() -> read(() -> controller.laneStepIndexForTesting(true)) > pausedFirstStep);
        click(controller, "Reset");
        assertEquals("Ready", read(controller::raceStatusForTesting));
        assertEquals(0, read(() -> controller.laneStepIndexForTesting(true)));
        assertEquals(0, read(() -> controller.laneStepIndexForTesting(false)));
        assertFalse(read(() -> controller.laneTimerRunningForTesting(true)));
        assertFalse(read(() -> controller.laneTimerRunningForTesting(false)));
    }

    @Test
    void preventsDuplicateStartsAndAllowsAnotherRaceAfterCompletion() throws Exception {
        AlgorithmRaceController controller = createController();
        setInput(controller, "3, 2, 1");
        setSpeed(controller, 100);

        onEdt(() -> {
            button(controller, "Start Race").doClick();
            button(controller, "Start Race").doClick();
        });
        assertEquals(1L, read(controller::raceGenerationForTesting).longValue());
        await(() -> "Completed".equals(read(controller::raceStatusForTesting)));

        click(controller, "Start Race");
        await(() -> "Completed".equals(read(controller::raceStatusForTesting))
                && read(controller::raceGenerationForTesting) > 1);
        assertEquals("Completed", read(() -> controller.laneStatusForTesting(true)));
        assertEquals("Completed", read(() -> controller.laneStatusForTesting(false)));
    }

    @Test
    void laneThatFinishesFirstDoesNotStopTheOtherLane() throws Exception {
        AlgorithmRaceController controller = createController();
        onEdt(() -> findAll(controller, JComboBox.class).get(1)
                .setSelectedItem(findAll(controller, JComboBox.class).get(1).getItemAt(1)));
        setInput(controller, "8, 7, 6, 5, 4, 3, 2, 1");
        setSpeed(controller, 100);
        click(controller, "Start Race");

        await(() -> {
            String first = read(() -> controller.laneStatusForTesting(true));
            String second = read(() -> controller.laneStatusForTesting(false));
            return ("Completed".equals(first) && "Running".equals(second))
                    || ("Running".equals(first) && "Completed".equals(second));
        });
        assertTrue(read(() -> controller.laneTimerRunningForTesting(true)
                || controller.laneTimerRunningForTesting(false)));
        await(() -> "Completed".equals(read(controller::raceStatusForTesting)));
    }

    @Test
    void rejectsIdenticalAlgorithmsWithoutStartingAWorker() throws Exception {
        AlgorithmRaceController controller = createController();
        onEdt(() -> {
            List<JComboBox> choices = findAll(controller, JComboBox.class);
            choices.get(1).setSelectedItem(choices.get(0).getSelectedItem());
        });

        assertEquals("Ready", read(controller::raceStatusForTesting));
        assertFalse(read(controller::preparationRunningForTesting));
        onEdt(() -> assertTrue(choicesAreDifferent(controller)));
    }

    @Test
    void rejectsEmptyInputAndCompletesSingleValueArrays() throws Exception {
        AlgorithmRaceController controller = createController();
        setInput(controller, "");
        click(controller, "Start Race");
        assertEquals("Ready", read(controller::raceStatusForTesting));
        assertFalse(read(controller::preparationRunningForTesting));

        setInput(controller, "7");
        click(controller, "Start Race");
        await(() -> "Completed".equals(read(controller::raceStatusForTesting)));
        assertEquals("Completed", read(() -> controller.laneStatusForTesting(true)));
        assertEquals("Completed", read(() -> controller.laneStatusForTesting(false)));
    }

    private AlgorithmRaceController createController() throws Exception {
        return onEdt(AlgorithmRaceController::new);
    }

    private void setInput(AlgorithmRaceController controller, String input) throws Exception {
        onEdt(() -> findAll(controller, JTextField.class).getFirst().setText(input));
    }

    private boolean choicesAreDifferent(AlgorithmRaceController controller) {
        List<JComboBox> choices = findAll(controller, JComboBox.class);
        return choices.get(0).getSelectedItem() != choices.get(1).getSelectedItem();
    }

    private void setSpeed(AlgorithmRaceController controller, int speed) throws Exception {
        onEdt(() -> findAll(controller, JSlider.class).getFirst().setValue(speed));
    }

    private void click(AlgorithmRaceController controller, String text) throws Exception {
        onEdt(() -> button(controller, text).doClick());
    }

    private JButton button(AlgorithmRaceController controller, String text) {
        return findAll(controller, JButton.class).stream()
                .filter(button -> text.equals(button.getText()))
                .findFirst()
                .orElseThrow();
    }

    private <T extends Component> List<T> findAll(Component root, Class<T> type) {
        List<T> matches = new ArrayList<>();
        if (type.isInstance(root)) {
            matches.add(type.cast(root));
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                matches.addAll(findAll(child, type));
            }
        }
        return matches;
    }

    private long count(List<?> steps, SortOperation... operations) {
        return steps.stream()
                .map(com.algovision.model.SortStep.class::cast)
                .filter(step -> List.of(operations).contains(step.operation()))
                .count();
    }

    private void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + 8_000_000_000L;
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(10);
        }
        throw new AssertionError("Condition was not satisfied before timeout.");
    }

    private <T> T read(java.util.concurrent.Callable<T> action) {
        try {
            return onEdt(action);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private <T> T onEdt(java.util.concurrent.Callable<T> action) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            return action.call();
        }
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                result.set(action.call());
            } catch (Throwable throwable) {
                failure.set(throwable);
            }
        });
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
        return result.get();
    }

    private void onEdt(Runnable action) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            SwingUtilities.invokeAndWait(action);
        }
    }
}
