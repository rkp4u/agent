package org.example.service;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * SummarizerService - Generates a summary of the kopitiam conversation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SummarizerService {

    private final ChatModel chatModel;

    private static final String SUMMARY_HEADER = "=== KOPITIAM CONVERSATION SUMMARY ===\n\n";

    /**
     * Generate a summary from raw conversation text and message count.
     */
    public String summarize(String conversationText, int messageCount) {
        log.info("SUMMARIZER: Generating summary...");

        if (conversationText == null || conversationText.isBlank()) {
            log.warn("SUMMARIZER: No conversation content");
            return "No conversation content to summarize.";
        }

        try {
            String summary = chatModel.chat(
                    SystemMessage.from(buildSystemPrompt()),
                    UserMessage.from(buildUserPrompt(conversationText))
            ).aiMessage().text();

            log.info("SUMMARIZER: Summary generated successfully");
            return SUMMARY_HEADER + summary.strip();

        } catch (Exception e) {
            log.error("SUMMARIZER: LLM failed, using fallback: {}", e.getMessage());
            return SUMMARY_HEADER + "Total messages: " + messageCount +
                   "\n\nUnable to generate detailed summary at this time.";
        }
    }

    private String buildSystemPrompt() {
        return """
                You are a keen observer at a Singapore kopitiam who has been listening to the conversation.
                
                Generate a concise summary that captures:
                1. Key topics discussed
                2. The dynamics between participants
                3. Any memorable quotes or highlights
                4. The overall mood and flow of the conversation
                
                Format your summary in a clear, engaging way that captures the essence of kopitiam banter.
                Keep it concise but insightful.
                """;
    }

    private String buildUserPrompt(String conversationText) {
        return """
                Here's the conversation that took place:
                
                %s
                
                Please provide a summary of this kopitiam conversation.
                """.formatted(conversationText);
    }
}
