package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.RunnableConfig;
import org.example.graph.KopitiamState;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;


@Slf4j
@Service
@RequiredArgsConstructor
public class GraphService {

    /** Injected singleton bean from KopitiamGraph.compiledGraph() — carries the MemorySaver. */
    private final CompiledGraph<KopitiamState> compiledGraph;

    /**
     * Invoke the graph for a given session and return the evaluation scorecard JSON.
     *
     * @param sessionId unique session identifier (used as the checkpoint threadId)
     * @param message   the user's opening message seeded into the messages channel
     * @return JSON scorecard string from the evaluator_node, or empty string if unavailable
     */
    public String invoke(String sessionId, String message) throws Exception {
        log.info("=== SINGAPORE KOPITIAM CHATTER === [session={}] message='{}'", sessionId, message);

        RunnableConfig runnableConfig = RunnableConfig.builder()
                .threadId(sessionId)
                .build();

        java.util.List<Map<String, String>> seedMessages = new java.util.ArrayList<>();
        seedMessages.add(Map.of("role", "user", "content", "You: " + message));

        Map<String, Object> initialData = new HashMap<>();
        initialData.put("messages", seedMessages);
        initialData.put("volley_msg_left", 0);
        initialData.put("next_speaker", null);

        java.util.Optional<KopitiamState> result = compiledGraph.invoke(initialData, runnableConfig);

        String evaluation = result
                .flatMap(KopitiamState::evaluation)
                .orElse("");

        log.info("GRAPH: Conversation ended [session={}]", sessionId);
        return evaluation;
    }
}
