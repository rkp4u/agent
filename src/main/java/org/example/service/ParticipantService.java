package org.example.service;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.config.PersonaRegistry;
import org.example.model.Persona;
import org.example.tools.ToolExecutor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipantService {

    private final ChatModel chatModel;
    private final PersonaRegistry personaRegistry;
    private final ToolExecutor toolExecutor;

    private static final int MAX_REACT_ITERATIONS = 5;

    // Tool call marker — LLM signals a tool call with this prefix
    private static final String TOOL_CALL_PREFIX = "TOOL:";

    /**
     * Run the ReAct loop for the selected persona.
     *
     * @param personaId        The persona to respond (e.g. "ah_seng")
     * @param conversationText Full conversation history as text
     * @return Message map with role, name, content
     */
    public Map<String, String> respond(String personaId, String conversationText) {
        log.info("PARTICIPANT [{}]: Starting ReAct loop", personaId);

        Persona persona = personaRegistry.getPersona(personaId)
                .orElseGet(() -> {
                    log.warn("PARTICIPANT: Unknown persona '{}', falling back to ah_seng", personaId);
                    return personaRegistry.getPersona("ah_seng").orElseThrow();
                });

        // Build message history for the LLM
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(buildSystemPrompt(persona)));
        messages.add(UserMessage.from(buildUserPrompt(conversationText, persona)));

        String finalResponse = null;

        // ReAct loop
        for (int i = 0; i < MAX_REACT_ITERATIONS; i++) {
            log.debug("PARTICIPANT [{}]: ReAct iteration {}", personaId, i + 1);

            String llmResponse = chatModel.chat(messages).aiMessage().text().strip();
            log.debug("PARTICIPANT [{}]: LLM response: {}", personaId, llmResponse);

            // Check if LLM wants to call a tool
            if (llmResponse.startsWith(TOOL_CALL_PREFIX)) {
                String toolName = extractToolName(llmResponse);

                // Validate persona has access to this tool
                if (!persona.getTools().contains(toolName)) {
                    log.warn("PARTICIPANT [{}]: No access to tool '{}', skipping", personaId, toolName);
                    messages.add(AiMessage.from(llmResponse));
                    messages.add(UserMessage.from("You don't have access to that tool. Please respond directly."));
                    continue;
                }

                log.info("PARTICIPANT [{}]: Calling tool '{}'", personaId, toolName);
                String toolResult = toolExecutor.execute(toolName);
                log.debug("PARTICIPANT [{}]: Tool '{}' result: {}", personaId, toolName, toolResult);

                // Feed tool result back into the conversation
                messages.add(AiMessage.from(llmResponse));
                messages.add(UserMessage.from("Tool result: " + toolResult + "\n\nNow give your response in character."));

            } else {
                // LLM gave a direct response — ReAct loop complete
                finalResponse = llmResponse;
                log.info("PARTICIPANT [{}]: Final response generated after {} iteration(s)", personaId, i + 1);
                break;
            }
        }

        // Fallback if loop exhausted without final response
        if (finalResponse == null) {
            log.warn("PARTICIPANT [{}]: ReAct loop exhausted, using fallback", personaId);
            finalResponse = persona.getName() + ": Aiyah, I don't know what to say lah!";
        }

        // Log the final response
        log.info(finalResponse);

        return Map.of(
                "role",    "assistant",
                "name",    personaId,
                "content", finalResponse
        );
    }

    /**
     * Build system prompt for the persona — tells LLM who it is and how to use tools.
     */
    private String buildSystemPrompt(Persona persona) {
        String toolInstructions = persona.getTools().isEmpty() ? "" : """

                You have access to the following tools: %s
                To call a tool, respond ONLY with: TOOL:<tool_name>
                Available tool names: time, weather, news
                After receiving the tool result, respond in character.
                """.formatted(String.join(", ", persona.getTools()));

        return """
                You are %s, a %d year old kopitiam regular in Singapore.
                
                Background: %s
                Personality: %s
                Speech style: %s
                
                Stay completely in character. Respond naturally as this person would in a kopitiam conversation.
                Keep your response concise — 1 to 3 sentences maximum.
                Do NOT break character or acknowledge that you are an AI.
                %s
                """.formatted(
                persona.getName(),
                persona.getAge(),
                persona.getBackstory(),
                persona.getPersonality(),
                persona.getSpeechStyle(),
                toolInstructions
        );
    }

    /**
     * Build user prompt with the full conversation history.
     */
    private String buildUserPrompt(String conversationText, Persona persona) {
        return """
                Here is the conversation so far at the kopitiam:
                
                %s
                
                Now respond as %s. Remember to stay in character.
                If you need real-time information (time, weather, news), use your tools first.
                Otherwise respond directly.
                """.formatted(conversationText.isBlank() ? "(conversation just started)" : conversationText,
                persona.getName());
    }

    /**
     * Extract tool name from LLM tool call string e.g. "TOOL:weather" → "weather"
     */
    private String extractToolName(String llmResponse) {
        return llmResponse.substring(TOOL_CALL_PREFIX.length()).strip().toLowerCase();
    }
}

