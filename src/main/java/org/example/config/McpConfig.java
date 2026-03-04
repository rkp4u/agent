package org.example.config;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.service.tool.ToolProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
public class McpConfig {

    private static final Logger log = LoggerFactory.getLogger(McpConfig.class);

    // CoinGecko free public MCP server — no API key required
    // Docs: https://mcp.api.coingecko.com/
    // SSE endpoint is the legacy fallback; /mcp is the primary (HTTP Streaming)
    private static final String COINGECKO_SSE_URL = "https://mcp.api.coingecko.com/sse";

    @Bean
    public McpClient coinGeckoMcpClient() {
        log.info("Initializing CoinGecko MCP client (SSE: {})", COINGECKO_SSE_URL);

        McpTransport transport = new HttpMcpTransport.Builder()
                .sseUrl(COINGECKO_SSE_URL)
                .timeout(Duration.ofSeconds(600)) // 10 min — SSE is a long-lived connection
                .logRequests(true)
                .logResponses(true)
                .build();

        McpClient client = new DefaultMcpClient.Builder()
                .key("coingecko")
                .clientName("crypto-agent")
                .transport(transport)
                .build();

        log.info("CoinGecko MCP client initialized successfully");
        return client;
    }

    @Bean
    public ToolProvider toolProvider(List<McpClient> mcpClients) {
        log.info("Creating tool provider with {} MCP client(s)", mcpClients.size());

        ToolProvider baseProvider = McpToolProvider.builder()
                .mcpClients(mcpClients)
                .failIfOneServerFails(false)
                .build();

        ToolProvider loggingProvider = new ToolProviderLogger(baseProvider);
        log.info("Tool provider created successfully");
        return loggingProvider;
    }
}
