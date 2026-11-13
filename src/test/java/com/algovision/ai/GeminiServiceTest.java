package com.algovision.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GeminiServiceTest {
    private static final String ANALYSIS_JSON = """
            {
              "complexityAnalysis": {
                "timeComplexity": "O(log n)", "spaceComplexity": "O(1)",
                "bestCase": "O(1)", "averageCase": "O(log n)",
                "worstCase": "O(log n)", "explanation": "Halves the search interval."
              },
              "bugDetection": {
                "summary": "Check empty inputs.", "logicalErrors": [],
                "potentialBugs": [], "edgeCases": [
                  {"reference":"search()","description":"Empty array","severity":"low"}
                ]
              },
              "codeReview": {
                "summary": "Clear implementation.", "inefficientOperations": [],
                "unnecessaryComputations": [], "implementationChoices": []
              },
              "optimization": {
                "suggestions":["Keep the midpoint overflow-safe."],
                "optimizedCode":"int mid = left + (right - left) / 2;",
                "beforeAfterComplexity":"O(log n) to O(log n)"
              },
              "interviewQuestions":[
                {"question":"Why sorted input?","answer":"Binary search needs ordering.","concept":"Preconditions"},
                {"question":"What if duplicates?","answer":"Return any matching index.","concept":"Search variants"},
                {"question":"Alternative?","answer":"Use a linear scan for tiny arrays.","concept":"Tradeoffs"}
              ],
              "codeExplanation": {
                "approach":"Repeatedly halves the search interval.",
                "importantSections":[{"section":"Loop","explanation":"Updates interval bounds."}],
                "algorithmSteps":["Compare midpoint","Keep relevant half"]
              }
            }
            """;

    private HttpServer server;
    private AtomicReference<String> requestBody;
    private AtomicReference<String> requestApiKey;
    private AtomicReference<String> requestQuery;
    private int responseCode;
    private String responseBody;

    @BeforeEach
    void startServer() throws IOException {
        requestBody = new AtomicReference<>();
        requestApiKey = new AtomicReference<>();
        requestQuery = new AtomicReference<>();
        responseCode = 200;
        responseBody = wrap(ANALYSIS_JSON);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/generateContent", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            requestApiKey.set(exchange.getRequestHeaders().getFirst("x-goog-api-key"));
            requestQuery.set(exchange.getRequestURI().getRawQuery());
            byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(responseCode, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void sendsCodeAndParsesStructuredAnalysis() {
        String cppCode = "int search(const vector<int>& values, int target) { return -1; }";
        AnalysisResponse analysis = service().analyze(cppCode, "C++", "Searching").join();

        assertEquals("O(log n)", analysis.complexityAnalysis().timeComplexity());
        assertEquals("Empty array", analysis.bugDetection().edgeCases().getFirst().description());
        assertEquals(3, analysis.interviewQuestions().size());
        assertEquals("Repeatedly halves the search interval.", analysis.codeExplanation().approach());
        assertTrue(requestBody.get().contains(cppCode));
        assertTrue(requestBody.get().contains("C++"));
        assertTrue(requestBody.get().contains("Searching"));
        assertEquals("test-key", requestApiKey.get());
        assertNull(requestQuery.get());
    }

    @Test
    void acceptsJavaAndSubmitsBuggyInefficientSourceUnmodified() {
        String javaCode = "for (int i = 0; i < values.length; i++) { if (values[i] == target) return i; }";
        service().analyze(javaCode, "Java", "Arrays").join();
        assertTrue(requestBody.get().contains(javaCode));
        assertTrue(requestBody.get().contains("Java"));
    }

    @Test
    void reportsMissingApiKeyWithoutSendingRequest() {
        GeminiService missingKey = new GeminiService(HttpClient.newHttpClient(),
                endpoint(), "PASTE_YOUR_API_KEY_HERE");
        CompletionException failure = assertThrows(CompletionException.class,
                () -> missingKey.analyze("int main() {}", "C++", "Other").join());
        assertTrue(failure.getCause().getMessage().contains("Add your Gemini API key"));
        assertEquals(null, requestBody.get());
    }

    @Test
    void reportsRateLimitAndMalformedJsonResponses() {
        responseCode = 429;
        CompletionException rateLimit = assertThrows(CompletionException.class,
                () -> service().analyze("code", "Java", "Arrays").join());
        assertTrue(rateLimit.getCause().getMessage().contains("rate limit"));

        responseCode = 200;
        responseBody = wrap("{not-json");
        CompletionException malformed = assertThrows(CompletionException.class,
                () -> service().analyze("code", "Java", "Arrays").join());
        assertTrue(malformed.getCause().getMessage().contains("unreadable response"));
    }

    @Test
    void reportsMissingGeminiAnalysisText() {
        responseBody = "{\"candidates\":[]}";
        CompletionException failure = assertThrows(CompletionException.class,
                () -> service().analyze("code", "Java", "Arrays").join());
        assertTrue(failure.getCause().getMessage().contains("no analysis text"));
    }

    @Test
    void requestDoesNotAddApiKeyToRequestBody() {
        service().analyze("sample", "C++", "Sorting").join();
        assertFalse(requestBody.get().contains("test-key"));
        assertEquals("test-key", requestApiKey.get());
        assertNull(requestQuery.get());
    }

    private GeminiService service() {
        return new GeminiService(HttpClient.newHttpClient(), endpoint(), "test-key");
    }

    private URI endpoint() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/generateContent");
    }

    private String wrap(String text) {
        return "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":"
                + com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .valueToTree(text).toString() + "}]}}]}";
    }
}
