# ☕ Singapore Kopitiam Chatter

A multi-agent AI conversation system where four distinct Singapore kopitiam regulars chat amongst themselves — orchestrated by an LLM, evaluated by a judge, and persisted across sessions.

Built on **Spring Boot + LangChain4j + LangGraph4j**.

---

## 🧑‍🤝‍🧑 Meet the Regulars

| Persona | Age | Background | Speech Style | Tools |
|---|---|---|---|---|
| **Uncle Ah Seng** | 68 | 30+ years running the drinks stall. Pragmatic, thrifty, complains about costs. | Heavy Singlish — lah, lor, wah | `time`, `weather` |
| **Mei Qi** | 21 | Content creator promoting kopitiam online. Social media savvy, very chatty. | Mix of English and Singlish — OMG, yasss, emojis | `time`, `news` |
| **Bala Nair** | 45 | Ex-statistician turned football tipster. Sees patterns in everything. Dry humour. | Formal English with occasional Singlish, statistical references | `time` |
| **Dr. Tan** | 72 | Retired philosophy professor. Thoughtful, deep, sips his kopi-o slowly. | Measured English with philosophical tangents | _(none)_ |

---

## 🏗️ System Architecture

### Graph Topology

```
START
  │
  ▼
[human_node]          ← Seeds the opening message + sets volley count
  │
  ▼
[orchestrator_node]   ← LLM picks who speaks next
  │
  ├─ volley > 0 ──► [participant_node]  ← Selected persona responds (ReAct loop)
  │                       │
  │                       └──────────────► [orchestrator_node]  (loops)
  │
  └─ volley = 0 ──► [summarizer_node]   ← Generates conversation summary
                          │
                          ▼
                   [evaluator_node]      ← LLM-as-Judge scores the conversation
                          │
                         END
```

### Node Responsibilities

| Node | Class | What It Does |
|---|---|---|
| `human_node` | inline in `KopitiamGraph` | Seeds `messages` with the user's opening message and resets `volley_msg_left` |
| `orchestrator_node` | `OrchestratorNode` | Calls GPT-4o-mini to select the next speaker based on conversation history |
| `participant_node` | `ParticipantService` | Runs a ReAct loop — the selected persona may call tools before responding |
| `summarizer_node` | `SummarizerNode` | Generates a narrative summary of the full conversation |
| `evaluator_node` | `EvaluatorNode` | Scores the conversation on 3 dimensions and returns a JSON scorecard |

### Session Persistence (MemorySaver)

Every request carries an `X-Session-Id` header. The compiled graph is a **Spring singleton** with a `MemorySaver` checkpointer attached. Each session ID maps to its own checkpoint thread — so the same session ID on a follow-up request resumes the exact conversation state (message history, volley count, next speaker) from where it left off.

```
Request 1  X-Session-Id: abc  →  MemorySaver stores checkpoint["abc"]
Request 2  X-Session-Id: abc  →  MemorySaver restores checkpoint["abc"] → conversation continues
Request 3  X-Session-Id: xyz  →  No checkpoint["xyz"] → fresh conversation
```

### Tools

Each persona has access to a subset of real-time tools, called via a simple `TOOL:<name>` protocol in the ReAct loop:

| Tool | Class | Returns |
|---|---|---|
| `time` | `SingaporeTimeService` | Current Singapore time (SGT) |
| `weather` | `SingaporeWeatherService` | Current Singapore weather summary |
| `news` | `SingaporeNewsService` | Recent Singapore news headlines |

---

## 🛠️ Tech Stack

| Layer | Technology | Version |
|---|---|---|
| Language | Java | 21 |
| Framework | Spring Boot | 3.4.1 |
| Graph / Orchestration | LangGraph4j | 1.8.4 |
| LLM Client | LangChain4j | 1.1.0 / 1.1.0-beta7 |
| LLM Model | OpenAI GPT-4o-mini | — |
| Build | Gradle Kotlin DSL | — |

---

## 🚀 Getting Started

### Prerequisites

- Java 21+
- An OpenAI API key

### Step 1 — Set your OpenAI API key

The application requires an `OPENAI_API_KEY` environment variable. Without it the app will not start.

```bash
export OPENAI_API_KEY=sk-your-key-here
```

You can also prefix it inline when running:

```bash
OPENAI_API_KEY=sk-your-key-here ./gradlew bootRun
```

> **Note:** Never paste your key into `application.yml` directly. The file uses
> `${OPENAI_API_KEY}` so it always reads from the environment.

### Step 2 — Build the project

```bash
./gradlew build
```

### Step 3 — Run the application

```bash
./gradlew bootRun
```

Or, if you want to pass the API key inline without exporting:

```bash
OPENAI_API_KEY=sk-your-key-here ./gradlew bootRun
```

The app starts on **http://localhost:8080**.

### Step 4 — Start a conversation

```bash
curl http://localhost:8080/api/graph/invoke
```

The four regulars will chatter away. When done, you receive a JSON response:

```json
{
  "status": "completed",
  "sessionId": "3f7a2b1c-49de-4a1b-bf23-9c1e7d8a0f22",
  "evaluation": {
    "character_consistency":     { "score": 5, "reason": "Each agent stayed firmly in character throughout." },
    "conversation_naturalness":  { "score": 4, "reason": "The banter flowed organically with good topic transitions." },
    "tool_usage_correctness":    { "score": 5, "reason": "Tools were only invoked when real-time data was genuinely needed." },
    "overall": 5,
    "summary": "A lively and authentic kopitiam conversation with strong character voices and natural flow."
  },
  "message": "Conversation ended successfully. Come back anytime lah!"
}
```

---

## 📡 API Reference

### `GET /api/graph/invoke`

Starts a new kopitiam conversation or resumes an existing session.

| Parameter | Type | Where | Required | Default | Description |
|---|---|---|---|---|---|
| `X-Session-Id` | `string` | Header | No | auto UUID | Session identifier. Reuse to resume a previous conversation. |
| `message` | `string` | Query param | No | `"Hello everyone! What's happening at the kopitiam today?"` | The opening message injected into the conversation. |

**Example — new session with default greeting:**
```bash
curl http://localhost:8080/api/graph/invoke
```

**Example — new session with a custom topic:**
```bash
curl "http://localhost:8080/api/graph/invoke?message=What+do+you+all+think+about+the+new+MRT+line"
```

**Example — resume a previous session:**
```bash
curl -H "X-Session-Id: 3f7a2b1c-49de-4a1b-bf23-9c1e7d8a0f22" \
     "http://localhost:8080/api/graph/invoke?message=What+did+Ah+Seng+say+earlier"
```

**Example — pin your own session ID:**
```bash
curl -H "X-Session-Id: my-kopitiam-session" \
     http://localhost:8080/api/graph/invoke
```

---

## 📁 Project Structure

```
src/main/java/org/example/
├── Main.java
├── config/
│   └── PersonaRegistry.java        # Defines all 4 persona configs (name, background, tools)
├── controller/
│   ├── GraphController.java        # GET /api/graph/invoke — session + message routing
│   └── PersonaController.java      # Persona inspection endpoints
├── graph/
│   ├── KopitiamGraph.java          # @Configuration — builds and exposes the compiled graph @Bean
│   └── KopitiamState.java          # Shared state: messages (appender), volley_msg_left, next_speaker, evaluation
├── model/
│   └── Persona.java                # Persona data model
├── nodes/
│   ├── OrchestratorNode.java       # Speaker selection via LLM
│   ├── SummarizerNode.java         # End-of-conversation summary
│   └── EvaluatorNode.java          # LLM-as-Judge scoring node
├── service/
│   ├── GraphService.java           # Invokes the compiled graph with per-session RunnableConfig
│   ├── OrchestratorService.java    # Speaker selection logic
│   ├── ParticipantService.java     # ReAct loop — persona responds, optionally calling tools
│   ├── SummarizerService.java      # Summary generation
│   └── EvaluatorService.java       # Scores conversation on 3 dimensions, returns JSON scorecard
└── tools/
    ├── SingaporeNewsService.java    # Returns Singapore news headlines
    ├── SingaporeTimeService.java    # Returns current Singapore time (SGT)
    ├── SingaporeWeatherService.java # Returns Singapore weather
    └── ToolExecutor.java            # Routes TOOL:<name> calls to the right service
```

---

## ⚙️ Configuration

All settings live in `src/main/resources/application.yml`:

| Key | Default | Description |
|---|---|---|
| `langchain4j.open-ai.chat-model.model-name` | `gpt-4o-mini` | OpenAI model used by all agents |
| `langchain4j.open-ai.chat-model.temperature` | `0.7` | Creativity level for agent responses |
| `langchain4j.open-ai.chat-model.log-requests` | `true` | Logs full HTTP request to OpenAI |
| `langchain4j.open-ai.chat-model.log-responses` | `true` | Logs full HTTP response from OpenAI |
| `server.port` | `8080` | Port the app listens on |

**Conversation length** is controlled by `DEFAULT_VOLLEYS` in `KopitiamGraph.java` (default: **4 turns**). Increase it for longer conversations.

---

## 🧑‍⚖️ Evaluation Scorecard

After every conversation the `evaluator_node` calls GPT-4o-mini with a structured judge prompt and scores the exchange on three dimensions:

| Dimension | What It Measures |
|---|---|
| `character_consistency` | Did each agent stay true to their persona throughout? |
| `conversation_naturalness` | Did the conversation flow organically, like real kopitiam banter? |
| `tool_usage_correctness` | Were tools (time / weather / news) called only when genuinely needed? |

Each dimension gets a score of 1–5 with a one-sentence reason. An `overall` score (1–5) and a one-sentence `summary` verdict are also returned. The scorecard is embedded directly in the API response as structured JSON.

---

## 💡 Example Conversation Flow

```
You: Hello everyone! What's happening at the kopitiam today?

[orchestrator] → selected: ah_seng
Ah Seng:  Wah, today very hot lah! Must drink more kopi, keep awake lor.
          You all try the new kaya toast or not? Very nice, leh!

[orchestrator] → selected: mei_qi
Mei Qi:   OMG, I haven't tried it yet! 😍 Must go get some later!
          Anyone know if they serve it with half-boiled eggs? That's the best combo, yasss! 🥚✨

[orchestrator] → selected: bala
Bala:     Ah, the new kaya toast — an intriguing variable in our breakfast equation.
          If they serve it with half-boiled eggs, satisfaction levels would increase by a
          significant margin, statistically speaking. Must try later, confirm!

[summarizer] → Kopitiam Conversation Summary:
  Key topics: Hot weather, new kaya toast, ideal breakfast combos.
  Dynamics: Casual and lively, mix of enthusiasm and dry analytical humour.
  Mood: Light-hearted and jovial.

[evaluator] → Score: 5/5
  "A lively and authentic kopitiam exchange with strong character voices and natural topic flow."
```

---

*Alamak, what are you waiting for? Go run it lah!* ☕

