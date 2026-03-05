package org.example.service;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.config.PersonaRegistry;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

/**
 * OrchestratorService - Selects the next speaker using LLM.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrchestratorService {

    private final PersonaRegistry personaRegistry;
    private final ChatModel chatModel;
    private final Random random = new Random();

    /**
     * Use LLM to select the next speaker based on conversation context.
     * Falls back to random selection if LLM fails or returns an invalid speaker.
     */
    public String selectSpeakerWithLLM(String conversationText) {
        try {
            String response = chatModel.chat(
                    SystemMessage.from(buildSystemPrompt()),
                    UserMessage.from(buildUserPrompt(conversationText))
            ).aiMessage().text();

            String selected = extractSpeakerId(response.trim().toLowerCase());

            if (personaRegistry.hasPersona(selected)) {
                log.info("ORCHESTRATOR: LLM selected '{}'", selected);
                return selected;
            }

            log.warn("ORCHESTRATOR: Invalid speaker '{}', using fallback", selected);
        } catch (Exception e) {
            log.warn("ORCHESTRATOR: LLM error '{}', using fallback", e.getMessage());
        }

        return fallbackSelection();
    }

    private String buildSystemPrompt() {
        return """
                You are managing a lively conversation at a Singapore kopitiam.
                
                Available speakers:
                - ah_seng: Uncle Ah Seng, 68yo kopi uncle, speaks Singlish, knows about drinks and weather
                - mei_qi: Young 21yo content creator, social media savvy, knows latest news and trends
                - bala: Ex-statistician turned football tipster, dry humor, analytical
                - dr_tan: Retired 72yo philosophy professor, thoughtful and deep thinker
                
                Based on the conversation flow, select who should speak next to keep the conversation lively and natural.
                Consider who hasn't spoken recently, who has relevant expertise, and natural kopitiam banter flow.
                
                Respond with ONLY the speaker ID (ah_seng, mei_qi, bala, or dr_tan).
                """;
    }

    private String buildUserPrompt(String conversationText) {
        return """
                Recent conversation:
                %s
                
                Who should speak next to keep this kopitiam conversation lively?
                """.formatted(conversationText.isBlank() ? "(conversation just started)" : conversationText);
    }

    private String extractSpeakerId(String response) {
        for (String speaker : List.of("ah_seng", "mei_qi", "bala", "dr_tan")) {
            if (response.contains(speaker)) return speaker;
        }
        return response.trim();
    }

    private String fallbackSelection() {
        List<String> speakers = personaRegistry.getAllPersonaIds();
        String selected = speakers.get(random.nextInt(speakers.size()));
        log.info("ORCHESTRATOR: Fallback selected '{}'", selected);
        return selected;
    }
}
