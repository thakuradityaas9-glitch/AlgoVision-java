package com.algovision.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpTimeoutException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GroqServiceTest {
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
    private AtomicReference<String> requestMethod;
    private AtomicReference<String> requestContentType;
    private AtomicReference<String> requestPath;
    private int responseCode;
    private String responseBody;

    @BeforeEach
    void startServer() throws IOException {
        requestBody = new AtomicReference<>();
        requestApiKey = new AtomicReference<>();
        requestQuery = new AtomicReference<>();
        requestMethod = new AtomicReference<>();
        requestContentType = new AtomicReference<>();
        requestPath = new AtomicReference<>();
        responseCode = 200;
        responseBody = wrap(ANALYSIS_JSON);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            requestApiKey.set(exchange.getRequestHeaders().getFirst("Authorization"));
            requestQuery.set(exchange.getRequestURI().getRawQuery());
            requestMethod.set(exchange.getRequestMethod());
            requestContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            requestPath.set(exchange.getRequestURI().getPath());
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
        assertTrue(requestBody.get().contains("openai/gpt-oss-120b"));
        assertTrue(requestBody.get().contains("\"messages\""));
        assertTrue(requestBody.get().contains("\"response_format\":{\"type\":\"json_object\"}"));
        assertEquals("POST", requestMethod.get());
        assertEquals("application/json", requestContentType.get());
        assertEquals("/chat/completions", requestPath.get());
        assertEquals("Bearer test-key", requestApiKey.get());
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
    void acceptsConfiguredDummyKey() {
        service().analyze("int main() {}", "C++", "Other").join();
        assertEquals("Bearer test-key", requestApiKey.get());
    }

    @Test
    void rejectsPlaceholderKeyWithoutSendingRequest() {
        assertRejectedApiKey("PASTE_GROQ_API_KEY_HERE");
    }

    @Test
    void rejectsEmptyAndBlankKeysWithoutSendingRequest() {
        assertRejectedApiKey("");
        assertRejectedApiKey("   ");
    }

    @Test
    void rejectsNullKeyWithoutSendingRequest() {
        assertRejectedApiKey(null);
    }

    private void assertRejectedApiKey(String apiKey) {
        GroqService missingKey = new GroqService(HttpClient.newHttpClient(), endpoint(), apiKey);
        CompletionException failure = assertThrows(CompletionException.class,
                () -> missingKey.analyze("int main() {}", "C++", "Other").join());
        assertEquals(
                "Groq API key not found. Add GROQ_API_KEY to the project-root .env file.",
                failure.getCause().getMessage());
        assertNull(requestBody.get());
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
    void reportsInvalidApiKeyWithoutIncludingTheCredentialInTheError() {
        responseCode = 401;
        responseBody = "{\"error\":{\"message\":\"Invalid API key test-key. Bearer gsk_secret\"}}";
        CompletionException failure = assertThrows(CompletionException.class,
                () -> service().analyze("code", "Java", "Arrays").join());

        assertTrue(failure.getCause() instanceof GroqService.GroqException);
        assertEquals(GroqService.ErrorType.API_ERROR,
                ((GroqService.GroqException) failure.getCause()).type());
        assertTrue(failure.getCause().getMessage().contains("authentication failed (HTTP 401)"));
        assertFalse(failure.getCause().getMessage().contains("test-key"));
        assertFalse(failure.getCause().getMessage().contains("gsk_secret"));
    }

    @Test
    void reportsMissingGroqAnalysisText() {
        responseBody = "{\"choices\":[]}";
        CompletionException failure = assertThrows(CompletionException.class,
                () -> service().analyze("code", "Java", "Arrays").join());
        assertTrue(failure.getCause().getMessage().contains("no analysis text"));
    }

    @Test
    void requestDoesNotAddApiKeyToRequestBody() {
        service().analyze("sample", "C++", "Sorting").join();
        assertFalse(requestBody.get().contains("test-key"));
        assertEquals("Bearer test-key", requestApiKey.get());
        assertNull(requestQuery.get());
    }

    @Test
    void reportsNetworkDisconnection() {
        URI unavailableEndpoint = endpoint();
        server.stop(0);
        server = null;
        GroqService disconnected = new GroqService(HttpClient.newHttpClient(),
                unavailableEndpoint, "test-key");

        CompletionException failure = assertThrows(CompletionException.class,
                () -> disconnected.analyze("code", "C++", "Graphs").join());
        assertTrue(failure.getCause().getMessage().contains("Could not connect to Groq"));
    }

    @Test
    void mapsRequestTimeoutToAnActionableMessage() {
        GroqService.GroqException failure =
                service().mapFailure(new HttpTimeoutException("request timed out"));

        assertEquals(GroqService.ErrorType.NETWORK, failure.type());
        assertTrue(failure.getMessage().contains("too long to respond"));
        assertFalse(failure.getMessage().contains("test-key"));
    }

    private GroqService service() {
        return new GroqService(HttpClient.newHttpClient(), endpoint(), "test-key");
    }

    private URI endpoint() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/chat/completions");
    }

    private String wrap(String text) {
        return "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":"
                + com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .valueToTree(text).toString() + "}}]}";
    }
}
