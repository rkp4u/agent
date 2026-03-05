package org.example.graph;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncEdgeAction;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.example.nodes.OrchestratorNode;
import org.example.nodes.SummarizerNode;
import org.example.service.ParticipantService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;

/**
 * KopitiamGraph - Java equivalent of build_graph() in main.py.
 *
 * START → human → orchestrator → orchestrator_routing()
 *   ├─ volley > 0 → participant → orchestrator (loop)
 *   └─ volley = 0 → summarizer → END
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KopitiamGraph {

    private final OrchestratorNode orchestratorNode;
    private final SummarizerNode summarizerNode;
    private final ParticipantService participantService;

    private static final String HUMAN        = "human";
    private static final String ORCHESTRATOR = "orchestrator";
    private static final String PARTICIPANT  = "participant";
    private static final String SUMMARIZER   = "summarizer";

    private static final String PLACEHOLDER_INPUT = "Hello everyone! What's happening at the kopitiam today?";
    private static final int    DEFAULT_VOLLEYS   = 4;

    /**
     * Build and compile the StateGraph using LangGraph4j 1.8.4 API.
     */
    public CompiledGraph<KopitiamState> build() throws GraphStateException {

        // --- Node Actions ---

        // human_node: inject placeholder message + set volley count
        AsyncNodeAction<KopitiamState> humanAction = state ->
                CompletableFuture.completedFuture(Map.of(
                        "messages",        List.of(Map.of("role", "user", "content", "You: " + PLACEHOLDER_INPUT)),
                        "volley_msg_left", DEFAULT_VOLLEYS
                ));

        // orchestrator_node: pick next speaker via LLM, decrement volley
        AsyncNodeAction<KopitiamState> orchestratorAction = state -> {
            log.info("GRAPH → [orchestrator_node]");
            String nextSpeaker = orchestratorNode.selectSpeaker(
                    buildConversationText(state.messages()));
            int volleyLeft = Math.max(0, state.volleyMsgLeft() - 1);
            log.info("ORCHESTRATOR: selected='{}', volley_left={}", nextSpeaker, volleyLeft);
            return CompletableFuture.completedFuture(Map.of(
                    "next_speaker",    nextSpeaker,
                    "volley_msg_left", volleyLeft
            ));
        };

        // participant_node: selected persona runs ReAct loop and responds
        AsyncNodeAction<KopitiamState> participantAction = state -> {
            String speaker = state.nextSpeaker().orElse("ah_seng");
            log.info("GRAPH → [participant_node]: '{}'", speaker);
            String conversationText = buildConversationText(state.messages());
            Map<String, String> message = participantService.respond(speaker, conversationText);
            return CompletableFuture.completedFuture(Map.of("messages", List.of(message)));
        };

        // summarizer_node: generate and print summary, then end
        AsyncNodeAction<KopitiamState> summarizerAction = state -> {
            log.info("GRAPH → [summarizer_node]");
            List<Map<String, String>> msgs = state.messages();
            summarizerNode.process(buildConversationText(msgs), msgs.size());
            return CompletableFuture.completedFuture(Map.of());
        };

        // --- Edge Routing ---

        // orchestrator_routing: volleys left → participant, else → summarizer (done)
        AsyncEdgeAction<KopitiamState> orchestratorRouting = state -> {
            int volleyLeft = state.volleyMsgLeft();
            String route = volleyLeft > 0 ? PARTICIPANT : SUMMARIZER;
            log.info("orchestrator_routing → '{}' (volley_left={})", route, volleyLeft);
            return CompletableFuture.completedFuture(route);
        };

        // --- Build Graph ---
        // Pass KopitiamState.SCHEMA so "messages" uses an AppenderChannel
        return new StateGraph<>(KopitiamState.SCHEMA, KopitiamState::new)
                .addNode(HUMAN,        humanAction)
                .addNode(ORCHESTRATOR, orchestratorAction)
                .addNode(PARTICIPANT,  participantAction)
                .addNode(SUMMARIZER,   summarizerAction)
                .addEdge(START,        HUMAN)
                .addEdge(HUMAN,        ORCHESTRATOR)
                .addConditionalEdges(ORCHESTRATOR, orchestratorRouting, Map.of(
                        PARTICIPANT, PARTICIPANT,
                        SUMMARIZER,  SUMMARIZER
                ))
                .addEdge(PARTICIPANT,  ORCHESTRATOR)
                .addEdge(SUMMARIZER,   END)
                .compile();
    }

    private String buildConversationText(List<Map<String, String>> messages) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, String> msg : messages) {
            String content = msg.getOrDefault("content", "");
            if (!content.isBlank()) sb.append(content).append("\n");
        }
        return sb.toString();
    }
}
