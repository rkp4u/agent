package org.example.nodes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.service.OrchestratorService;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class OrchestratorNode {

    private final OrchestratorService orchestratorService;

    /**
     * Select the next speaker using LLM — called from KopitiamGraph node action.
     */
    public String selectSpeaker(String conversationText) {
        log.info("ORCHESTRATOR NODE: Selecting speaker via LLM");
        return orchestratorService.selectSpeakerWithLLM(conversationText);
    }
}
