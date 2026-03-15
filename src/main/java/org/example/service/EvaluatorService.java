package org.example.service;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluatorService {

    private final ChatModel chatModel;

    private static final String SYSTEM_PROMPT = """
            You are an expert evaluator of multi-agent AI conversations.
            Score the following kopitiam conversation on three dimensions.
            Return ONLY a valid JSON object with no extra text:

            {
              "character_consistency": {
                "score": <1-5>,
                "reason": "<one sentence>"
              },
              "conversation_naturalness": {
                "score": <1-5>,
                "reason": "<one sentence>"
              },
              "tool_usage_correctness": {
                "score": <1-5>,
                "reason": "<one sentence>"
              },
              "overall": <1-5>,
              "summary": "<one sentence overall verdict>"
            }

            Scoring guide:
            - character_consistency: Did each agent stay true to their persona?
            - conversation_naturalness: Did the conversation flow organically?
            - tool_usage_correctness: Were tools called only when truly needed?
            """;

    /**
     * Evaluate a conversation and return a JSON scorecard string.
     *
     * @param conversationText the full conversation transcript
     * @return JSON scorecard (character_consistency, conversation_naturalness,
     *         tool_usage_correctness, overall, summary)
     */
    public String evaluate(String conversationText) {
        log.info("EVALUATOR: Scoring conversation...");

        if (conversationText == null || conversationText.isBlank()) {
            log.warn("EVALUATOR: No conversation content to evaluate");
            return "{\"overall\":0,\"summary\":\"No conversation content to evaluate.\"}";
        }

        try {
            String scorecard = chatModel.chat(
                    SystemMessage.from(SYSTEM_PROMPT),
                    UserMessage.from("Evaluate this conversation:\n" + conversationText)
            ).aiMessage().text().strip();

            String overallScore = extractOverallScore(scorecard);
            log.info("EVALUATOR: Score = {}", overallScore);

            return scorecard;

        } catch (Exception e) {
            log.error("EVALUATOR: LLM failed: {}", e.getMessage());
            return "{\"overall\":0,\"summary\":\"Evaluation failed: " + e.getMessage() + "\"}";
        }
    }

    /** Best-effort extraction of the "overall" score from the raw JSON string for logging. */
    private String extractOverallScore(String json) {
        try {
            int idx = json.indexOf("\"overall\"");
            if (idx == -1) return "unknown";
            String after = json.substring(idx + 9).stripLeading();
            if (after.startsWith(":")) after = after.substring(1).stripLeading();
            StringBuilder sb = new StringBuilder();
            for (char c : after.toCharArray()) {
                if (Character.isDigit(c) || c == '.') sb.append(c);
                else if (!sb.isEmpty()) break;
            }
            return sb.isEmpty() ? "unknown" : sb.toString();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
