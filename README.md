# MobAgent

**MobAgent** is an Android-native multi-agent AI framework. It lets you run autonomous AI agents entirely on an Android device — no server, no internet required. The agents can reason, use tools, delegate to specialised sub-agents, and remember conversations across sessions, all powered by a local LLM.

> 📖 [Full Documentation](./doc/README.md)

---

## What It Does

You chat with an AI agent. The agent decides what to do — it can answer directly, or call a **tool** to take real action (search the web, read a file, run a command, call an API). If the task needs special expertise, it **spawns a sub-agent** trained for that specific job. The whole loop runs until the agent has a final answer, then replies.

Everything — the LLM, the tools, the memory, the execution sandbox — runs locally on the device.

---

## How It Works

### 1. The Agent Loop

The core is a **ReAct loop** (Reason + Act):

```
User Message
     │
     ▼
LlmHandler ──► calls the LLM (local llama-server or any OpenAI-compatible API)
     │
     ├── LLM replies with text only? → done, return answer
     │
     └── LLM replies with a function call? → ToolExecutorHandler runs the tool
                                                    │
                                                    └── result added to memory → back to LlmHandler
```

This loop repeats until the LLM stops calling tools. The implementation uses a **Chain of Responsibility** pattern — `LlmHandler` and `ToolExecutorHandler` point to each other, forming the cycle.

### 2. Tools

A tool is any executable the agent can call. MobAgent supports two kinds:

- **Terminal tools** — shell scripts or binaries that live inside the Alpine Linux sandbox. The agent passes JSON arguments; the tool runs, writes output between `==Result==` markers, and the result is returned as JSON. To add a new tool, just drop a binary + `config.json` into the `tools/` folder.
- **Native tools** — Java classes implementing the `Tool` interface directly. The key built-in one is `SpawnAgentTool`.

### 3. Skills and Sub-Agents

A **skill** is a specialised sub-agent definition. It has:
- A system prompt (what it's an expert in)
- Its own private tools (binaries in its own `tools/` folder)
- Access to selected public tools from the root registry

When the root agent calls `spawn_agent("researcher", query)`, MobAgent:
1. Looks up the "researcher" skill
2. Builds a fresh `ModelInterface` with that skill's tools and system prompt
3. Runs the full agent loop for the sub-agent
4. Returns the output back to the root agent as a tool result

This makes it easy to compose specialised agents without any code changes — just add a skill folder.

### 4. Local LLM (llama.cpp)

MobAgent ships `llama-server` compiled for ARM64. On the LlamaCpp settings screen you can:
- Select any `.gguf` model file stored on the device
- Start/stop the server
- Change the port

Once running, `llama-server` exposes a standard OpenAI-compatible API at `http://127.0.0.1:<port>/v1/chat/completions`. The agent framework talks to it exactly as it would any cloud API — making it trivial to switch between local and remote models.

### 5. Plugin System

Every part of the LLM integration is hot-swappable via Android DEX plugins:

- **Formatter plugin** — a JAR that implements `FormatterBuilder`. It knows how to format requests and parse responses for a specific LLM API (OpenAI, Claude, Gemini, etc.). Upload the JAR in Settings; load it without rebuilding the app.
- **Memory plugin** — a JAR implementing a custom memory strategy (e.g. vector search, summarisation). Falls back to the built-in sliding-window memory if not configured.

All plugins share a single global ID from the `Plugin` table — one parent row, one sub-table row, one ID.

### 6. Memory and Sessions

Each **chat session** has its own:
- Model plugin selection (which LLM backend to use)
- Memory plugin selection (how to manage conversation history)
- Message history (persisted in Room DB)

`InMemory` (the default memory) keeps the last 30 messages in a list, with the system prompt pinned at index 0. When you switch sessions, it reloads the DB messages for that session — so every conversation is independently preserved.

### 7. Tool Execution Sandbox (Alpine Linux)

All tool binaries run inside a **proot Alpine Linux environment** extracted into the app's private storage. This gives them a full Linux filesystem with shell, package manager, and any runtime you install (`apk add python3`, etc.). proot intercepts syscalls and redirects paths — no Android root required.

The sandbox also hosts `llama-server`. Session management (`TerminalSessionManager`) provides named session slots: a blocking `"tools"` session for synchronous tool calls, an async `"llama_cpp_server"` session for the inference server, and an interactive shell session for the terminal UI.

---

## Architecture

```
┌───────────────────────────────────────────────────────┐
│                    Android App                        │
│                                                       │
│  Chat UI ──► ModelInterface                           │
│                   │                                   │
│            ┌──────▼──────┐                            │
│            │ Agent Loop  │  (ReAct chain)             │
│            │ LlmHandler  │◄──────────────┐            │
│            └──────┬──────┘               │            │
│                   │ function call        │ tool result │
│            ┌──────▼──────────┐           │            │
│            │ToolExecutorHndlr│───────────┘            │
│            └──────┬──────────┘                        │
│                   │                                   │
│         ┌─────────▼──────────┐                        │
│         │    ToolsManager    │  (registry)            │
│         │  TerminalTool ──►  │──► Alpine proot shell  │
│         │  SpawnAgentTool──► │──► new ModelInterface  │
│         └────────────────────┘    (sub-agent)         │
│                                                       │
│  Plugin System (DexLoader) ──► FormatterInterface     │
│  Room DB ──► Sessions, Messages, Plugins              │
│  llama-server (ARM64) ──► OpenAI-compat HTTP API      │
└───────────────────────────────────────────────────────┘
```

---

## Project Structure

```
core/main/src/main/java/
├── org/mobchain/
│   ├── agentLoopEngine/    Agent loop (Handler chain, MsgContext)
│   ├── models/             ModelInterface, OllamaModel
│   ├── memory/             InMemory
│   ├── tools/              ToolsManager, ToolsScanner, TerminalTool
│   ├── skills/             Skill, SkillsScanner
│   ├── messages/           HumanMessages, AiMessages, ToolMessages
│   └── client/             HTTP client, Request/Response
└── com/example/myapplication/
    ├── DAOs/               Room entities, DAOs, ChatMessageStore
    ├── repo/               LlamaCppServerRepo, SessionHandlingRepo
    ├── ui/                 MainActivity, Settings screens, adapters
    └── utils/              DexLoader, PropertiesReader, ZipUtils
```

---

## Quick Start

1. **Add a model plugin** — Settings → Model Plugins → ＋  
   Enter a plugin name, API URL (e.g. `http://127.0.0.1:8080/v1/chat/completions`), model name, and select a formatter JAR.

2. **Start the local LLM** (optional) — Settings → LlamaCpp  
   Download binaries, select a `.gguf` model, tap Start Server.

3. **Chat** — On launch, select a model plugin in the dialog and start chatting.

4. **Add a tool** — Place a binary + `config.json` in `alpine/root/tools/<tool_name>/` inside the app sandbox.

5. **Add a skill** — Place `skill.json` + a `tools/` subfolder in `alpine/root/skills/<skill_name>/`.

---

## Documentation

Full docs in [`doc/`](./doc/README.md) — every class, entity, pattern, and flow explained in detail.

| Section | |
|---|---|
| Agent Loop Engine | [doc/agent-loop-engine](./doc/agent-loop-engine/agent-loop-engine.md) |
| Tools System | [doc/tools-system](./doc/tools-system/tools-system.md) |
| Skills System | [doc/skills-system](./doc/skills-system/skills-system.md) |
| Memory System | [doc/memory-system](./doc/memory-system/memory-system.md) |
| Models Layer | [doc/models-layer](./doc/models-layer/models-layer.md) |
| Plugin System | [doc/plugin-system](./doc/plugin-system/plugin-system.md) |
| Chat Sessions | [doc/chat-sessions](./doc/chat-sessions/chat-sessions.md) |
| LlamaCpp Integration | [doc/llamacpp](./doc/llamacpp/llamacpp.md) |
| Tool Execution Layer | [doc/terminal-infrastructure](./doc/terminal-infrastructure/terminal-infrastructure.md) |
| UI Layer | [doc/ui-layer](./doc/ui-layer/ui-layer.md) |
| Database Layer | [doc/database-layer](./doc/database-layer/database-layer.md) |
| Build Configuration | [doc/build-configuration](./doc/build-configuration/build-configuration.md) |
