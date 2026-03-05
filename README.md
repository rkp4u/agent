# ☕ Singapore Kopitiam Chatter

Ever wondered what it would be like if four Singapore kopitiam regulars could chat amongst themselves — powered by AI? That's exactly what this project does.

**Singapore Kopitiam Chatter** is a multi-agent conversation system where four distinct AI personas engage in natural, lively banter at a virtual kopitiam (Singapore coffee shop). An orchestrator LLM manages the flow, picks who speaks next, and the whole thing wraps up with an AI-generated summary of the conversation.

---

## 🧑‍🤝‍🧑 Meet the Regulars

| Persona | Who They Are |
|---|---|
| **Uncle Ah Seng** | 68-year-old kopi uncle. 30+ years running the drinks stall. Speaks heavy Singlish. Complains about rising costs. |
| **Mei Qi** | 21-year-old content creator. Always on her phone, posting about kopitiam life. Uses OMG and yasss liberally. |
| **Bala Nair** | 45-year-old ex-statistician turned football tipster. Sees patterns in everything. Dry humour. |
| **Dr. Tan** | 72-year-old retired philosophy professor. Thoughtful, deep, sips his kopi-o slowly. |

---

## 🏗️ How It Works

The system is built as a state machine graph using **LangGraph4j**, where each node is an agent with a specific role:

```
START → human (inject opening message)
          ↓
      orchestrator (LLM picks who speaks next)
          ↓
      participant (selected persona responds)
          ↓
      orchestrator → ... (loops for N volleys)
          ↓
      summarizer → END
```

1. **Human node** — Seeds the conversation with an opening line and sets the volley count (how many turns the AI will take).
2. **Orchestrator node** — Calls GPT-4o-mini to decide which persona should speak next, based on conversation history.
3. **Participant node** — The selected persona generates a response in character, optionally using tools (time, weather, news).
4. **Summarizer node** — Once volleys are exhausted, generates a conversation summary and ends the graph.

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.4.1 |
| Graph / Orchestration | LangGraph4j 1.8.4 |
| LLM | LangChain4j 1.1.0 + OpenAI GPT-4o-mini |
| Build | Gradle (Kotlin DSL) |

---

## 🚀 Getting Started

### Prerequisites

- Java 21+
- An OpenAI API key

### 1. Set your API key

The app reads your OpenAI key from the `OPENAI_API_KEY` environment variable:

```bash
export OPENAI_API_KEY=sk-your-key-here
```

Alternatively, you can hardcode it directly in `src/main/resources/application.yml` (not recommended for production).

### 2. Build the project

```bash
./gradlew build
```

### 3. Run it

```bash
./gradlew bootRun
```

The app starts on **port 8080**.

### 4. Start a conversation

Hit this endpoint in your browser or with `curl`:

```bash
curl http://localhost:8080/api/graph/invoke
```

Sit back and watch the logs — the full kopitiam conversation plays out, and you'll get a JSON response once it's done:

```json
{
  "status": "completed",
  "message": "Conversation ended successfully. Thank you! Come back to kopitiam anytime lah!"
}
```

---

## 📁 Project Structure

```
src/main/java/org/example/
├── Main.java                    # Spring Boot entry point
├── config/
│   └── PersonaRegistry.java     # Defines all 4 persona configs
├── controller/
│   ├── GraphController.java     # POST /api/graph/invoke
│   └── PersonaController.java   # Persona inspection endpoints
├── graph/
│   ├── KopitiamGraph.java       # Builds the LangGraph4j state graph
│   └── KopitiamState.java       # Shared state (messages, volley count, next speaker)
├── model/
│   └── Persona.java             # Persona data model
├── nodes/
│   ├── OrchestratorNode.java    # Calls LLM to pick the next speaker
│   └── SummarizerNode.java      # Generates the end-of-conversation summary
├── service/
│   ├── GraphService.java        # Wires up and runs the graph
│   ├── OrchestratorService.java # Speaker selection logic
│   ├── ParticipantService.java  # ReAct loop for each persona
│   └── SummarizerService.java   # Summary generation
└── tools/
    ├── SingaporeNewsService.java    # Fetches Singapore news headlines
    ├── SingaporeTimeService.java    # Returns current Singapore time
    ├── SingaporeWeatherService.java # Returns Singapore weather info
    └── ToolExecutor.java            # Routes tool calls to the right service
```

---

## ⚙️ Configuration

Key settings are in `src/main/resources/application.yml`:

| Setting | Default | Description |
|---|---|---|
| `langchain4j.open-ai.chat-model.model-name` | `gpt-4o-mini` | The OpenAI model to use |
| `langchain4j.open-ai.chat-model.temperature` | `0.7` | Controls response creativity |
| `server.port` | `8080` | Port the app listens on |

The number of conversation turns is controlled by `DEFAULT_VOLLEYS` in `KopitiamGraph.java` (default: **4 turns**). Increase it for longer conversations.

---

## 💡 Example Conversation

Here's what a typical run looks like in the logs:

```
[orchestrator] Selected: ah_seng
[ah_seng]  Wah, today very hot lah! Must drink more kopi, keep awake lor.

[orchestrator] Selected: mei_qi
[mei_qi]   Yasss, kopi is life! ☕️ Did anyone try the new prata stall? Cheese prata is da bomb! 🤤

[orchestrator] Selected: bala
[bala]     Ah, kopi and prata — the quintessential Singaporean combination. 
           I'll need to analyse the data before endorsing the cheese prata, though.

[summarizer] Kopitiam Banter Summary: The group bonded over hot weather, rising kopi prices, 
             and excitement about a new prata stall. Mood: upbeat. Cultural pride: high.
```

---

## 🤝 Contributing

Feel free to:
- Add new personas in `PersonaRegistry.java`
- Add new tools in the `tools/` package and register them in `ToolExecutor.java`
- Tweak the orchestrator prompt in `OrchestratorNode.java` to change conversation dynamics

---

*Alamak, what are you waiting for? Go run it lah!* ☕

