# AI Agent — Spring Boot 4 + LangChain4j + CoinGecko MCP

## Goal
Build a single agent (`@AiService`) in Spring Boot 4 that answers crypto questions
by calling tools exposed by the CoinGecko MCP server over SSE.

---

## Assumptions
- Build tool: Gradle Kotlin DSL (build.gradle.kts)
- Java: 25 (via Gradle toolchain)
- Spring Boot: 4.0.3
- LLM Provider: OpenAI (gpt-4o-mini)
- MCP Server: https://mcp.api.coingecko.com/sse (no API key required)
- Framework: LangChain4j (NOT Spring AI)

---

## build.gradle.kts

plugins {
id("java")
id("org.springframework.boot") version "4.0.3"
id("io.spring.dependency-management") version "1.1.7"
}

group = "org.example"
version = "1.0-SNAPSHOT"

java {
toolchain {
languageVersion = JavaLanguageVersion.of(25)
}
}

repositories {
mavenCentral()
}

dependencies {
implementation("org.springframework.boot:spring-boot-starter-web")

    // LangChain4j BOM — manages versions for core modules
    implementation(platform("dev.langchain4j:langchain4j-bom:1.1.0"))

    // Core modules — version resolved by BOM
    implementation("dev.langchain4j:langchain4j-mcp")
    implementation("dev.langchain4j:langchain4j-open-ai")

    // Spring Boot starters — separate release track, explicit version required
    implementation("dev.langchain4j:langchain4j-spring-boot-starter:1.1.0-beta7")
    implementation("dev.langchain4j:langchain4j-open-ai-spring-boot-starter:1.1.0-beta7")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
useJUnitPlatform()
}

---

## Project Structure

src/main/java/org/example/
Application.java
config/McpConfig.java
agent/CryptoAgent.java
controller/CryptoController.java
src/main/resources/
application.properties

---

## application.properties

langchain4j.open-ai.chat-model.api-key=${OPENAI_API_KEY}
langchain4j.open-ai.chat-model.model-name=gpt-4o-mini
langchain4j.open-ai.chat-model.log-requests=true
langchain4j.open-ai.chat-model.log-responses=true

---

## Application.java

package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
public static void main(String[] args) {
SpringApplication.run(Application.class, args);
}
}

---

## McpConfig.java

package org.example.config;

import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.tool.McpToolProvider;
import dev.langchain4j.agent.tool.ToolProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
public class McpConfig {

    @Bean
    public McpClient coinGeckoMcpClient() {
        var transport = new HttpMcpTransport.Builder()
                .sseUrl("https://mcp.api.coingecko.com/sse")
                .logRequests(true)
                .logResponses(true)
                .build();

        return new DefaultMcpClient.Builder()
                .transport(transport)
                .build();
    }

    @Bean
    public ToolProvider toolProvider(List<McpClient> mcpClients) {
        return McpToolProvider.builder()
                .mcpClients(mcpClients)
                .failIfOneServerFails(false)
                .toolNameMapper((client, toolSpec) ->
                        client.key() + "_" + toolSpec.name())
                .build();
    }
}

---

## CryptoAgent.java

package org.example.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface CryptoAgent {

    @SystemMessage("""
        You are a crypto market analyst assistant.
        You have access to real-time cryptocurrency data via tools.
        For prices, market cap, trending coins, or market stats,
        always call your tools first before answering.
        If the user asks for a comparison, fetch data for both before responding.
        """)
    String chat(String userMessage);
}

---

## CryptoController.java

package org.example.controller;

import org.example.agent.CryptoAgent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/crypto")
public class CryptoController {

    private final CryptoAgent cryptoAgent;

    public CryptoController(CryptoAgent cryptoAgent) {
        this.cryptoAgent = cryptoAgent;
    }

    @PostMapping("/ask")
    public ResponseEntity<String> ask(@RequestBody Map<String, String> body) {
        String prompt = body.getOrDefault("prompt", "");
        return ResponseEntity.ok(cryptoAgent.chat(prompt));
    }
}

---

## Run in IntelliJ

1. Reload Gradle (click elephant icon → Reload All Gradle Projects)
2. Set env variable: Run → Edit Configurations → Environment Variables
   OPENAI_API_KEY=sk-your-key-here
3. Right-click Application.java → Run

---

## Test with curl

curl -X POST http://localhost:8080/api/crypto/ask \
-H "Content-Type: application/json" \
-d '{"prompt":"What is the current price of Bitcoin in USD?"}'

curl -X POST http://localhost:8080/api/crypto/ask \
-H "Content-Type: application/json" \
-d '{"prompt":"What are the top 5 trending coins right now?"}'

curl -X POST http://localhost:8080/api/crypto/ask \
-H "Content-Type: application/json" \
-d '{"prompt":"Compare market cap of Ethereum vs Solana"}'

---

## How the Agent Flow Works

POST /api/crypto/ask { "prompt": "What is BTC price?" }
↓
CryptoController
↓
CryptoAgent (@AiService — LangChain4j auto-implements this interface)
↓
OpenAI LLM reasons: "I need real-time BTC price, I'll call a tool"
↓
McpToolProvider routes call → mcp.api.coingecko.com/sse
↓
CoinGecko MCP executes tool → returns live JSON data
↓
LLM synthesizes natural language answer
↓
HTTP Response

---

## Common Pitfalls

- @AiService bean not created:
  Missing langchain4j-spring-boot-starter dependency

- application.properties API key ignored:
  Using langchain4j-open-ai (core) instead of
  langchain4j-open-ai-spring-boot-starter

- MCP tools not visible to agent:
  ToolProvider bean not created, or SSE URL unreachable at startup

- Version conflicts:
  Spring Boot starters (langchain4j-spring-boot-starter) are NOT
  managed by langchain4j-bom — always specify their version explicitly

---

## Scaling to Multiple MCP Servers Later

// In McpConfig.java — just add more @Bean McpClient entries:

@Bean
public McpClient newsMcpClient() {
var transport = new HttpMcpTransport.Builder()
.sseUrl("https://your-news-mcp-server.com/sse")
.build();
return new DefaultMcpClient.Builder()
.transport(transport)
.build();
}

// The toolProvider bean already accepts List<McpClient>
// Spring auto-injects ALL McpClient beans — no other changes needed.
// toolNameMapper namespaces tools: coingecko_get_price, news_get_headlines
