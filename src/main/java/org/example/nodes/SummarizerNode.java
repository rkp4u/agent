package org.example.nodes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.service.SummarizerService;
import org.springframework.stereotype.Component;

/**
 * SummarizerNode - End node that generates and prints the conversation summary.
 * Called directly from KopitiamGraph as a node action.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SummarizerNode {

    private final SummarizerService summarizerService;

    private static final String FAREWELL = "\nThank you! Come back to kopitiam anytime lah!";

    /**
     * Generate and print the conversation summary.
     * Called from KopitiamGraph summarizer node action.
     */
    public void process(String conversationText, int messageCount) {
        log.info("SUMMARIZER NODE: Generating summary...");
        System.out.println("\n=== CONVERSATION ENDING ===\n");
        String summary = summarizerService.summarize(conversationText, messageCount);
        System.out.println(summary);
        System.out.println(FAREWELL);
        log.info("SUMMARIZER NODE: Done");
    }
}
