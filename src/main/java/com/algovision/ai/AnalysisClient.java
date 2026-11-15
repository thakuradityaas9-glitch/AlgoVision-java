package com.algovision.ai;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface AnalysisClient {
    CompletableFuture<AnalysisResponse> analyze(String code, String language, String category);
}
