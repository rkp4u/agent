package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.example.graph.KopitiamGraph;
import org.example.graph.KopitiamState;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * GraphService - entry point that builds and invokes the LangGraph4j graph.
 * Mirrors graph = build_graph(); graph.invoke(initial_state) from main.py.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GraphService {

    private final KopitiamGraph kopitiamGraph;

    /**
     * Build the graph and invoke it with an empty initial state.
     */
    public void invoke() throws Exception {
        log.info("=== SINGAPORE KOPITIAM CHATTER ===");

        CompiledGraph<KopitiamState> graph = kopitiamGraph.build();

        // Initial state: empty messages, 0 volleys, no next speaker
        // Mirrors: initial_state = State(messages=[], volley_msg_left=0, next_speaker=None)
        Map<String, Object> initialData = new HashMap<>();
        initialData.put("messages", new java.util.ArrayList<>());
        initialData.put("volley_msg_left", 0);
        initialData.put("next_speaker", null);

        graph.invoke(initialData);

        log.info("GRAPH: Conversation ended");
    }
}
