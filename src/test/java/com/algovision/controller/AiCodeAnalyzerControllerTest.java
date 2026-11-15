package com.algovision.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import javax.swing.SwingUtilities;
import javax.swing.JTabbedPane;

import com.algovision.ai.AnalysisClient;
import com.algovision.ai.AnalysisResponse;
import org.junit.jupiter.api.Test;

class AiCodeAnalyzerControllerTest {
    @Test
    void rejectsEmptyCodeWithoutSubmittingAnApiRequest() throws Exception {
        FakeAnalysisClient client = new FakeAnalysisClient();
        AiCodeAnalyzerController controller = createController(client);

        onEdt(() -> controller.analyzeButtonForTesting().doClick());

        assertEquals(0, client.calls.get());
        assertFalse(read(controller::isLoadingForTesting));
        assertEquals("Add some code before starting the analysis.",
                read(controller::statusForTesting));
    }

    @Test
    void preventsDuplicateRequestsAndRendersAllResultsOnTheEdt() throws Exception {
        FakeAnalysisClient client = new FakeAnalysisClient();
        AiCodeAnalyzerController controller = createController(client);
        onEdt(() -> {
            controller.codeEditorForTesting().setText("int search() { return 1; }");
            controller.analyzeButtonForTesting().doClick();
            controller.analyzeButtonForTesting().doClick();
        });

        assertEquals(1, client.calls.get());
        assertTrue(read(controller::isLoadingForTesting));
        assertEquals("int search() { return 1; }", client.code.get());
        assertEquals("C++", client.language.get());
        assertEquals("Arrays", client.category.get());

        client.currentRequest.complete(analysisResponse("linear implementation"));
        await(() -> "Analysis complete.".equals(read(controller::statusForTesting)));

        assertFalse(read(controller::isLoadingForTesting));
        assertTrue(read(controller::lastCompletionOnEdtForTesting));
        assertTrue(read(() -> controller.resultTextForTesting("complexity")).contains("O(n)"));
        assertTrue(read(() -> controller.resultTextForTesting("bugs")).contains("Empty array"));
        assertTrue(read(() -> controller.resultTextForTesting("review")).contains("Readable"));
        assertTrue(read(() -> controller.resultTextForTesting("optimization"))
                .contains("Use an iterative loop"));
        assertEquals("linear implementation", read(controller::optimizedCodeForTesting));
        assertTrue(read(controller::copyEnabledForTesting));
        assertTrue(read(() -> controller.resultTextForTesting("interview"))
                .contains("Why is the input sorted?"));
        assertTrue(read(() -> controller.resultTextForTesting("explanation"))
                .contains("Compare the middle element"));
        assertTrue(read(() -> findTabPane(controller) != null));
    }

    @Test
    void displaysApiErrorsAndAllowsAnotherAnalysisWithoutStaleResults() throws Exception {
        FakeAnalysisClient client = new FakeAnalysisClient();
        AiCodeAnalyzerController controller = createController(client);
        onEdt(() -> {
            controller.codeEditorForTesting().setText("first input");
            controller.analyzeButtonForTesting().doClick();
        });
        client.currentRequest.completeExceptionally(new IllegalStateException("private detail"));
        await(() -> !read(controller::isLoadingForTesting));

        assertEquals("Groq analysis failed unexpectedly. Please try again.",
                read(controller::statusForTesting));
        assertTrue(read(() -> controller.analyzeButtonForTesting().isEnabled()));

        client.currentRequest = new CompletableFuture<>();
        onEdt(() -> {
            controller.codeEditorForTesting().setText("second input");
            controller.analyzeButtonForTesting().doClick();
        });
        assertEquals(2, client.calls.get());
        assertEquals("", read(() -> controller.resultTextForTesting("complexity")));
        assertEquals("", read(controller::optimizedCodeForTesting));

        client.currentRequest.complete(analysisResponse("second optimized result"));
        await(() -> "Analysis complete.".equals(read(controller::statusForTesting)));
        assertEquals("second optimized result", read(controller::optimizedCodeForTesting));
    }

    private AnalysisResponse analysisResponse(String optimizedCode) {
        return new AnalysisResponse(
                new AnalysisResponse.ComplexityAnalysis(
                        "O(n)", "O(1)", "O(1)", "O(n)", "O(n)", "One pass through input."),
                new AnalysisResponse.BugDetection(
                        "Check empty inputs.",
                        List.of(new AnalysisResponse.Finding("search()", "No issue", "low")),
                        List.of(),
                        List.of(new AnalysisResponse.Finding("input", "Empty array", "low"))),
                new AnalysisResponse.CodeReview(
                        "Readable implementation.", List.of(), List.of(), List.of("Simple loop")),
                new AnalysisResponse.Optimization(
                        List.of("Use an iterative loop"), optimizedCode, "O(n) to O(n)"),
                List.of(new AnalysisResponse.InterviewQuestion(
                        "Why is the input sorted?", "It enables binary search.", "Precondition")),
                new AnalysisResponse.CodeExplanation(
                        "Search a sorted sequence.",
                        List.of(new AnalysisResponse.ExplanationSection(
                                "Loop", "Compare the middle element")),
                        List.of("Initialize bounds", "Narrow the range")));
    }

    private AiCodeAnalyzerController createController(FakeAnalysisClient client) throws Exception {
        return onEdt(() -> new AiCodeAnalyzerController(client));
    }

    private void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + 5_000_000_000L;
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

    private JTabbedPane findTabPane(java.awt.Component component) {
        if (component instanceof JTabbedPane tabs) {
            return tabs;
        }
        if (component instanceof java.awt.Container container) {
            for (java.awt.Component child : container.getComponents()) {
                JTabbedPane tabs = findTabPane(child);
                if (tabs != null) {
                    return tabs;
                }
            }
        }
        return null;
    }

    private static final class FakeAnalysisClient implements AnalysisClient {
        private final AtomicInteger calls = new AtomicInteger();
        private final AtomicReference<String> code = new AtomicReference<>();
        private final AtomicReference<String> language = new AtomicReference<>();
        private final AtomicReference<String> category = new AtomicReference<>();
        private volatile CompletableFuture<AnalysisResponse> currentRequest =
                new CompletableFuture<>();

        @Override
        public CompletableFuture<AnalysisResponse> analyze(
                String submittedCode, String submittedLanguage, String submittedCategory) {
            calls.incrementAndGet();
            code.set(submittedCode);
            language.set(submittedLanguage);
            category.set(submittedCategory);
            return currentRequest;
        }
    }
}
