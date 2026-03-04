package org.example.controller;

import org.example.agent.CryptoAgent;
import org.example.config.ToolProviderLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API controller for cryptocurrency queries
 */
@RestController
@RequestMapping("/api/crypto")
public class CryptoController {

    private static final Logger log = LoggerFactory.getLogger(CryptoController.class);

    private final CryptoAgent cryptoAgent;

    public CryptoController(CryptoAgent cryptoAgent) {
        this.cryptoAgent = cryptoAgent;
        log.info("CryptoController initialized with agent: {}", cryptoAgent.getClass().getSimpleName());
    }

    /**
     * POST /api/crypto/ask
     * Sends a user query to the AI agent
     *
     * @param request JSON body with "prompt" field
     * @return AI agent's response
     */
    @PostMapping("/ask")
    public ResponseEntity<Map<String, String>> ask(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");

        log.info("Received query: {}", prompt);

        // Validate input
        if (prompt == null || prompt.trim().isEmpty()) {
            log.warn("Empty prompt received");
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("error", "Prompt cannot be empty"));
        }

        // Reset tool call counter for this request
        ToolProviderLogger.resetToolCallCount();

        int maxRetries = 3;
        int retryDelaySeconds = 5;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                if (attempt > 1) {
                    log.info("Retry attempt {}/{} for query: {}", attempt, maxRetries, prompt);
                }

                log.debug("Invoking crypto agent...");
                String response = cryptoAgent.chat(prompt);

                int toolCallCount = ToolProviderLogger.getToolCallCount();
                log.info("Agent response generated successfully (attempt {})", attempt);
                log.info("Total tools used by agent: {}", toolCallCount);

                return ResponseEntity.ok(Map.of(
                        "prompt", prompt,
                        "response", response,
                        "toolsUsed", String.valueOf(toolCallCount)
                ));

            } catch (Exception e) {
                int toolCallCount = ToolProviderLogger.getToolCallCount();
                String rootMessage = getRootCauseMessage(e);
                boolean isMcpRateLimit = rootMessage.contains("500") || rootMessage.contains("Unexpected status code");

                if (isMcpRateLimit && attempt < maxRetries) {
                    log.warn("CoinGecko MCP returned 500 on attempt {}/{} — waiting {}s before retry...",
                            attempt, maxRetries, retryDelaySeconds);
                    try {
                        Thread.sleep(retryDelaySeconds * 1000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    // Reset counter for next attempt
                    ToolProviderLogger.resetToolCallCount();
                    continue;
                }

                // Final attempt failed or non-retryable error
                log.error("Error processing query after {} attempt(s): {}", attempt, prompt, e);
                log.info("Tools used before failure: {}", toolCallCount);

                if (isMcpRateLimit) {
                    log.warn("CoinGecko MCP rate limit persisted after {} retries", maxRetries);
                    return ResponseEntity.ok(Map.of(
                            "prompt", prompt,
                            "response", "The CoinGecko data service is temporarily unavailable after " +
                                    maxRetries + " attempts. Please try again in a moment.",
                            "toolsUsed", String.valueOf(toolCallCount),
                            "warning", "MCP rate limited: " + rootMessage
                    ));
                }

                return ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of(
                                "error", "Failed to process query: " + e.getMessage(),
                                "prompt", prompt,
                                "toolsUsed", String.valueOf(toolCallCount)
                        ));
            }
        }

        // Should not reach here
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Unexpected state", "prompt", prompt));
    }

    private String getRootCauseMessage(Throwable e) {
        Throwable cause = e;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() != null ? cause.getMessage() : e.getMessage();
    }

    /**
     * GET /api/crypto/health
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        log.debug("Health check requested");
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "crypto-agent"
        ));
    }
}

