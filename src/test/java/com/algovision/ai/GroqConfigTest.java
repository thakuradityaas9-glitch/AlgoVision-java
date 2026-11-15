package com.algovision.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GroqConfigTest {
    @TempDir
    Path tempDirectory;

    @Test
    void loadsEnvironmentVariableAndTrimsWhitespace() {
        String key = GroqConfig.resolveApiKey(
                Map.of("GROQ_API_KEY", " environment-key "), tempDirectory.resolve(".env"));

        assertEquals("environment-key", key);
    }

    @Test
    void loadsKeyFromProjectDotEnvFile() throws IOException {
        Path dotenvFile = tempDirectory.resolve(".env");
        Files.writeString(dotenvFile, "# local credentials\nGROQ_API_KEY=\"dotenv-key\"\n");

        assertEquals("dotenv-key", GroqConfig.resolveApiKey(Map.of(), dotenvFile));
    }

    @Test
    void environmentVariableTakesPriorityOverDotEnvFile() throws IOException {
        Path dotenvFile = tempDirectory.resolve(".env");
        Files.writeString(dotenvFile, "GROQ_API_KEY=dotenv-key\n");

        assertEquals("environment-key", GroqConfig.resolveApiKey(
                Map.of("GROQ_API_KEY", "environment-key"), dotenvFile));
    }

    @Test
    void blankEnvironmentValueFallsBackToDotEnvFile() throws IOException {
        Path dotenvFile = tempDirectory.resolve(".env");
        Files.writeString(dotenvFile, "GROQ_API_KEY=dotenv-key\n");

        assertEquals("dotenv-key", GroqConfig.resolveApiKey(
                Map.of("GROQ_API_KEY", "   "), dotenvFile));
    }

    @Test
    void loadsDotEnvFromConfiguredMavenProjectRoot() throws IOException {
        Path projectRoot = tempDirectory.resolve("project");
        Files.createDirectories(projectRoot);
        Files.writeString(projectRoot.resolve(".env"), "GROQ_API_KEY=project-key\n");
        String previousRoot = System.getProperty("algovision.projectRoot");

        try {
            System.setProperty("algovision.projectRoot", projectRoot.toString());
            assertEquals("project-key", GroqConfig.resolveApiKey(Map.of()));
        } finally {
            if (previousRoot == null) {
                System.clearProperty("algovision.projectRoot");
            } else {
                System.setProperty("algovision.projectRoot", previousRoot);
            }
        }
    }

    @Test
    void missingOrBlankConfigurationReturnsNoKey() throws IOException {
        assertNull(GroqConfig.resolveApiKey(Map.of(), tempDirectory.resolve(".env")));

        Path dotenvFile = tempDirectory.resolve(".env");
        Files.writeString(dotenvFile, "GROQ_API_KEY=  \n");
        assertNull(GroqConfig.resolveApiKey(Map.of("GROQ_API_KEY", " "), dotenvFile));
    }
}
