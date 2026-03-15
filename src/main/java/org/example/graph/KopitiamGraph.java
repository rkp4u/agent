package org.example.graph;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncEdgeAction;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.example.nodes.EvaluatorNode;
import org.example.nodes.OrchestratorNode;
import org.example.nodes.SummarizerNode;
import org.example.service.ParticipantService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;


@Slf4j
@Configuration
@RequiredArgsConstructor
public class KopitiamGraph {

    private final OrchestratorNode orchestratorNode;
    private final SummarizerNode summarizerNode;
    private final EvaluatorNode evaluatorNode;
    private final ParticipantService participantService;

    private static final String HUMAN        = "human";
    private static final String ORCHESTRATOR = "orchestrator";
    private static final String PARTICIPANT  = "participant";
    private static final String SUMMARIZER   = "summarizer";
    private static final String EVALUATOR    = "evaluator_node";

    private static final String PLACEHOLDER_INPUT = "Hello everyone! What's happening at the kopitiam today?";
    private static final int    DEFAULT_VOLLEYS   = 4;

    /**
     * Singleton compiled graph with a MemorySaver checkpointer.
     * Spring creates this once on startup; every request shares the same instance.
     */
    @Bean
    public CompiledGraph<KopitiamState> compiledGraph() throws GraphStateException {
        MemorySaver memory = new MemorySaver();
        CompileConfig compileConfig = CompileConfig.builder()
                .checkpointSaver(memory)
                .build();

        return buildStateGraph().compile(compileConfig);
    }

    /**
     * Construct the StateGraph (topology only — no compilation here).
     */
    private StateGraph<KopitiamState> buildStateGraph() throws GraphStateException {

        // --- Node Actions ---

        AsyncNodeAction<KopitiamState> humanAction = state ->
                CompletableFuture.completedFuture(Map.of(
                        "messages",        List.of(Map.of("role", "user", "content", "You: " + PLACEHOLDER_INPUT)),
                        "volley_msg_left", DEFAULT_VOLLEYS
                ));

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

        AsyncNodeAction<KopitiamState> participantAction = state -> {
            String speaker = state.nextSpeaker().orElse("ah_seng");
            log.info("GRAPH → [participant_node]: '{}'", speaker);
            String conversationText = buildConversationText(state.messages());
            Map<String, String> message = participantService.respond(speaker, conversationText);
            return CompletableFuture.completedFuture(Map.of("messages", List.of(message)));
        };

        AsyncNodeAction<KopitiamState> summarizerAction = state -> {
            log.info("GRAPH → [summarizer_node]");
            List<Map<String, String>> msgs = state.messages();
            summarizerNode.process(buildConversationText(msgs), msgs.size());
            return CompletableFuture.completedFuture(Map.of());
        };

        AsyncNodeAction<KopitiamState> evaluatorAction = state -> {
            log.info("GRAPH → [evaluator_node]");
            List<Map<String, String>> msgs = state.messages();
            String scorecard = evaluatorNode.process(buildConversationText(msgs));
            return CompletableFuture.completedFuture(Map.of("evaluation", scorecard));
        };

        // --- Edge Routing ---

        AsyncEdgeAction<KopitiamState> orchestratorRouting = state -> {
            int volleyLeft = state.volleyMsgLeft();
            String route = volleyLeft > 0 ? PARTICIPANT : SUMMARIZER;
            log.info("orchestrator_routing → '{}' (volley_left={})", route, volleyLeft);
            return CompletableFuture.completedFuture(route);
        };

        // --- Build Graph ---
        return new StateGraph<>(KopitiamState.SCHEMA, KopitiamState::new)
                .addNode(HUMAN,        humanAction)
                .addNode(ORCHESTRATOR, orchestratorAction)
                .addNode(PARTICIPANT,  participantAction)
                .addNode(SUMMARIZER,   summarizerAction)
                .addNode(EVALUATOR,    evaluatorAction)
                .addEdge(START,        HUMAN)
                .addEdge(HUMAN,        ORCHESTRATOR)
                .addConditionalEdges(ORCHESTRATOR, orchestratorRouting, Map.of(
                        PARTICIPANT, PARTICIPANT,
                        SUMMARIZER,  SUMMARIZER
                ))
                .addEdge(PARTICIPANT,  ORCHESTRATOR)
                .addEdge(SUMMARIZER,   EVALUATOR)
                .addEdge(EVALUATOR,    END);
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
