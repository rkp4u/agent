package org.example.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.service.GraphService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * GraphController - Triggers the full multi-agent conversation graph.
 * Mirrors the main() function in main.py.
 */
@Slf4j
@RestController
@RequestMapping("/api/graph")
@RequiredArgsConstructor
public class GraphController {

    private final GraphService graphService;

    /**
     * Start a new kopitiam conversation.
     * Mirrors graph.invoke(initial_state) from main.py.
     */
    @GetMapping("/invoke")
    public Map<String, Object> invoke() {
        log.info("=== SINGAPORE KOPITIAM CHATTER ===");

        try {
            graphService.invoke();
            return Map.of(
                    "status", "completed",
                    "message", "Conversation ended successfully. Thank you! Come back to kopitiam anytime lah!"
            );
        } catch (Exception e) {
            log.error("GRAPH: Error during conversation: {}", e.getMessage());
            return Map.of(
                    "status", "error",
                    "message", e.getMessage()
            );
        }
    }
}
