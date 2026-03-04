# Crypto Agent

An AI-powered cryptocurrency assistant built with Spring Boot, LangChain4j, and the CoinGecko MCP server.

## Tech Stack

- **Java 21** + **Spring Boot 3.4.1**
- **LangChain4j 1.1.0-beta7** — AI services & MCP client
- **OpenAI GPT-4o-mini** — LLM
- **CoinGecko MCP** — Real-time crypto data via Model Context Protocol

---

## Setup

### Prerequisites

- Java 21+
- Gradle
- OpenAI API key

### Environment Variables

Copy the example env file and fill in your key:

```bash
cp .env.example .env
```

Then edit `.env`:

```env
OPENAI_API_KEY=sk-your-openai-api-key-here
```

> ⚠️ `.env` is in `.gitignore` and will **never be pushed** to GitHub. Only `.env.example` is committed.

### Run

```bash
# Load env vars then run
export $(cat .env | xargs) && ./gradlew bootRun
```

The server starts on **http://localhost:8080**

---

## API Endpoints

### POST `/api/crypto/ask`

Send a natural language query to the crypto agent.

**Request:**
```bash
curl -X POST http://localhost:8080/api/crypto/ask \
  -H "Content-Type: application/json" \
  -d '{"prompt": "What is the current price of Bitcoin?"}'
```

**Response:**
```json
{
  "prompt": "What is the current price of Bitcoin?",
  "response": "The current price of Bitcoin is $84,231.00 USD.",
  "toolsUsed": "1"
}
```

---

### More Example Queries

**Get Ethereum price:**
```bash
curl -X POST http://localhost:8080/api/crypto/ask \
  -H "Content-Type: application/json" \
  -d '{"prompt": "What is the current price of Ethereum?"}'
```

**Compare Bitcoin and Ethereum:**
```bash
curl -X POST http://localhost:8080/api/crypto/ask \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Compare the prices of Bitcoin and Ethereum"}'
```

**Get trending coins:**
```bash
curl -X POST http://localhost:8080/api/crypto/ask \
  -H "Content-Type: application/json" \
  -d '{"prompt": "What are the trending cryptocurrencies today?"}'
```

**Get top gainers:**
```bash
curl -X POST http://localhost:8080/api/crypto/ask \
  -H "Content-Type: application/json" \
  -d '{"prompt": "What are the top gaining coins in the last 24 hours?"}'
```

**Get market overview:**
```bash
curl -X POST http://localhost:8080/api/crypto/ask \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Give me a global crypto market overview"}'
```

**Get coin market data:**
```bash
curl -X POST http://localhost:8080/api/crypto/ask \
  -H "Content-Type: application/json" \
  -d '{"prompt": "What is the market cap and 24h volume of Solana?"}'
```

---

### GET `/api/crypto/health`

Health check endpoint.

```bash
curl http://localhost:8080/api/crypto/health
```

**Response:**
```json
{
  "status": "UP",
  "service": "crypto-agent"
}
```

---

## Notes

### CoinGecko Free Tier
- The app uses the **free public CoinGecko MCP server** (`https://mcp.api.coingecko.com/sse`)
- No API key required
- Free tier has rate limits — the app automatically retries up to **3 times** with a **5 second delay** between attempts
- For higher limits, use the [CoinGecko Pro MCP server](https://mcp.pro-api.coingecko.com) with your own API key

### MCP Endpoints
| Endpoint | Purpose |
|---|---|
| `https://mcp.api.coingecko.com/sse` | SSE (used by this app) |
| `https://mcp.api.coingecko.com/mcp` | HTTP Streaming (primary) |
| `https://mcp.pro-api.coingecko.com` | Pro tier (requires API key) |


