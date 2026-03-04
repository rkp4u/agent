package org.example.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolProvider;
import org.example.agent.CryptoAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Manually wires CryptoAgent with the MCP ToolProvider so that
 * CoinGecko tools are sent to OpenAI on every request.
 *
 * The @AiService annotation alone cannot reference a ToolProvider bean —
 * we must use AiServices.builder() to attach it explicitly.
 */
@Configuration
public class AgentConfig {

    private static final Logger log = LoggerFactory.getLogger(AgentConfig.class);

    @Bean
    public CryptoAgent cryptoAgent(ChatModel chatModel,
                                   ToolProvider toolProvider) {
        log.info("Building CryptoAgent with ChatModel: {} and ToolProvider: {}",
                chatModel.getClass().getSimpleName(),
                toolProvider.getClass().getSimpleName());

        return AiServices.builder(CryptoAgent.class)
                .chatModel(chatModel)
                .toolProvider(toolProvider)
                .build();
    }
}


