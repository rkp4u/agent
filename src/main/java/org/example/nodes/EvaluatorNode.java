package org.example.nodes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.service.EvaluatorService;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class EvaluatorNode {

    private final EvaluatorService evaluatorService;

    /**
     * Score the conversation and return the JSON scorecard.
     *
     * @param conversationText full conversation transcript built by KopitiamGraph
     * @return JSON scorecard string to be stored in state field "evaluation"
     */
    public String process(String conversationText) {
        log.info("EVALUATOR NODE: Scoring conversation...");
        String scorecard = evaluatorService.evaluate(conversationText);
        log.info("EVALUATOR NODE: Done");
        return scorecard;
    }
}

