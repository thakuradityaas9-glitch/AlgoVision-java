package com.algovision.ai;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class GroqConfig {
    private static final System.Logger LOGGER = System.getLogger(GroqConfig.class.getName());
    private static final String API_KEY_NAME = "GROQ_API_KEY";

    public static final String API_KEY = resolveApiKey(System.getenv());
    public static final String MODEL = "openai/gpt-oss-120b";
    public static final String API_ENDPOINT =
            "https://api.groq.com/openai/v1/chat/completions";

    private GroqConfig() {
    }

    static String resolveApiKey(Map<String, String> environment) {
        return resolveApiKey(environment, projectRoot().resolve(".env"));
    }

    static String resolveApiKey(Map<String, String> environment, Path dotenvFile) {
        String environmentValue = environment.get(API_KEY_NAME);
        String environmentKey = normalize(environmentValue);
        if (environmentKey != null) {
            LOGGER.log(System.Logger.Level.INFO,
                    "Groq API key source=environment; found=true; blank=false.");
            return environmentKey;
        }
        if (environmentValue != null) {
            LOGGER.log(System.Logger.Level.WARNING,
                    "Groq API key source=environment; found=false; blank=true; checking project .env.");
        }

        DotEnvResult dotenv = readDotEnv(dotenvFile);
        if (dotenv.value() != null) {
            LOGGER.log(System.Logger.Level.INFO,
                    "Groq API key source=project .env; found=true; blank=false.");
            return dotenv.value();
        }
        boolean blank = environmentValue != null || dotenv.blank();
        LOGGER.log(System.Logger.Level.WARNING,
                "Groq API key source=none; found=false; blank={0}; project .env path={1}.",
                blank, dotenvFile == null ? "unavailable" : dotenvFile.toAbsolutePath().normalize());
        return null;
    }

    private static Path projectRoot() {
        String configuredRoot = System.getProperty("algovision.projectRoot");
        if (configuredRoot != null && !configuredRoot.isBlank()) {
            return Path.of(configuredRoot).toAbsolutePath().normalize();
        }
        return Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
    }

    private static DotEnvResult readDotEnv(Path dotenvFile) {
        if (dotenvFile == null || !Files.isRegularFile(dotenvFile)) {
            return new DotEnvResult(false, null);
        }
        try {
            for (String line : Files.readAllLines(dotenvFile, StandardCharsets.UTF_8)) {
                ParsedDotEnvEntry entry = parseDotEnvLine(line);
                if (entry.keyPresent()) {
                    return new DotEnvResult(entry.value() == null, entry.value());
                }
            }
        } catch (IOException exception) {
            LOGGER.log(System.Logger.Level.WARNING,
                    "Could not read the project .env file; check its permissions and encoding.");
        }
        return new DotEnvResult(false, null);
    }

    private static ParsedDotEnvEntry parseDotEnvLine(String line) {
        String candidate = line.trim();
        if (candidate.isEmpty() || candidate.startsWith("#")) {
            return ParsedDotEnvEntry.NOT_FOUND;
        }
        if (candidate.startsWith("export ")) {
            candidate = candidate.substring("export ".length()).trim();
        }
        int separator = candidate.indexOf('=');
        if (separator < 0 || !API_KEY_NAME.equals(candidate.substring(0, separator).trim())) {
            return ParsedDotEnvEntry.NOT_FOUND;
        }

        String value = candidate.substring(separator + 1).trim();
        if (value.length() >= 2) {
            char quote = value.charAt(0);
            if ((quote == '"' || quote == '\'') && value.charAt(value.length() - 1) == quote) {
                value = value.substring(1, value.length() - 1);
            } else if (quote != '"' && quote != '\'') {
                int commentStart = value.indexOf(" #");
                if (commentStart >= 0) {
                    value = value.substring(0, commentStart).trim();
                }
            }
        }
        return new ParsedDotEnvEntry(true, normalize(value));
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private record ParsedDotEnvEntry(boolean keyPresent, String value) {
        private static final ParsedDotEnvEntry NOT_FOUND = new ParsedDotEnvEntry(false, null);
    }

    private record DotEnvResult(boolean blank, String value) {
    }
}