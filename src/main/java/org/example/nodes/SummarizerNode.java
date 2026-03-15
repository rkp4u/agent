package org.example.nodes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.service.SummarizerService;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class SummarizerNode {

    private final SummarizerService summarizerService;

    private static final String FAREWELL = "Thank you! Come back to kopitiam anytime lah!";

    /**
     * Generate and print the conversation summary.
     * Called from KopitiamGraph summarizer node action.
     */
    public void process(String conversationText, int messageCount) {
        log.info("SUMMARIZER NODE: Generating summary...");
        log.info("=== CONVERSATION ENDING ===");
        String summary = summarizerService.summarize(conversationText, messageCount);
        log.info(summary);
        log.info(FAREWELL);
        log.info("SUMMARIZER NODE: Done");
    }
}
