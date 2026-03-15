package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.service.GraphService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * GraphController - Triggers the full multi-agent conversation graph.
 * Mirrors the main() function in main.py.
 *
 * Session identity is carried via the X-Session-Id request header.
 * If the header is absent a fresh UUID is generated, giving the caller
 * a brand-new conversation thread.  Passing the same X-Session-Id on a
 * subsequent request resumes that thread from its last checkpoint.
 */
@Slf4j
@RestController
@RequestMapping("/api/graph")
@RequiredArgsConstructor
public class GraphController {

    private final GraphService graphService;
    private final ObjectMapper objectMapper;

    /**
     * Start or resume a kopitiam conversation.
     *
     * Header:
     *   X-Session-Id: <string>   — omit to start a new session (auto-generated UUID returned)
     *
     * Example:
     *   curl -H "X-Session-Id: my-session-1" http://localhost:8080/api/graph/invoke
     */
    @GetMapping("/invoke")
    public Map<String, Object> invoke(
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @RequestParam(value = "message", required = false,
                    defaultValue = "Hello everyone! What's happening at the kopitiam today?") String message) {

        // Auto-generate a session ID if the caller did not supply one
        final String resolvedSessionId = (sessionId != null && !sessionId.isBlank())
                ? sessionId
                : UUID.randomUUID().toString();

        log.info("=== SINGAPORE KOPITIAM CHATTER === [session={}] message='{}'",
                resolvedSessionId, message);

        try {
            String evaluationJson = graphService.invoke(resolvedSessionId, message);

            // Parse the scorecard JSON into a raw Map so the response contains structured
            // JSON rather than an escaped string. Fall back to the raw string on parse failure.
            Object evaluation;
            try {
                evaluation = evaluationJson.isBlank()
                        ? Map.of()
                        : objectMapper.readValue(evaluationJson, Map.class);
            } catch (Exception parseEx) {
                log.warn("GRAPH: Could not parse evaluation JSON, returning raw string: {}", parseEx.getMessage());
                evaluation = evaluationJson;
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status",     "completed");
            response.put("sessionId",  resolvedSessionId);
            response.put("evaluation", evaluation);
            response.put("message",    "Conversation ended successfully. Come back anytime lah!");
            return response;

        } catch (Exception e) {
            log.error("GRAPH: Error during conversation [session={}]: {}", resolvedSessionId, e.getMessage());
            return Map.of(
                    "status",    "error",
                    "sessionId", resolvedSessionId,
                    "message",   e.getMessage()
            );
        }
    }
}
