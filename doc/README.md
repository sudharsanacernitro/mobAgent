# MobAgent — Project Documentation

> **MobAgent** is an Android-native **multi-agent AI framework**. It provides a complete, pluggable agentic platform — agent loops, tools, skills, sub-agent delegation, memory, and a DEX-based plugin system — all running locally on an Android device, without a server or internet connection required. An Alpine Linux proot environment is embedded purely as a **tool execution sandbox**: agent tools run as binaries inside it, enabling the AI to execute real code, browse the web, and call any system capability. A terminal UI is included as a secondary convenience feature.

---


## Table of Contents

| # | Feature Area | Description |
|---|---|---|
| 1 | [Agent Loop Engine](./agent-loop-engine/agent-loop-engine.md) | The core ReAct-style agent execution loop |
| 2 | [Tools System](./tools-system/tools-system.md) | Tool registration, scanning, and execution |
| 3 | [Skills System](./skills-system/skills-system.md) | Dynamic skill discovery and sub-agent spawning |
| 4 | [Memory System](./memory-system/memory-system.md) | Conversation memory and session persistence |
| 5 | [Models Layer](./models-layer/models-layer.md) | LLM model interfaces and Ollama/LlamaCpp integration |
| 6 | [Plugin System](./plugin-system/plugin-system.md) | Dynamic DEX-based plugin loading |
| 7 | [Chat Sessions](./chat-sessions/chat-sessions.md) | Per-session plugin selection, message history, and session management |
| 8 | [LlamaCpp Integration](./llamacpp/llamacpp.md) | Local llama-server lifecycle, binary download, model selection |
| 9 | [Terminal Infrastructure](./terminal-infrastructure/terminal-infrastructure.md) | Alpine Linux rootfs, terminal sessions, and SSH |
| 10 | [UI Layer](./ui-layer/ui-layer.md) | Activities, adapters, and user-facing screens |
| 11 | [Database Layer](./database-layer/database-layer.md) | Room database, DAOs, and entity definitions |
| 12 | [Build Configuration](./build-configuration/build-configuration.md) | Gradle modules, dependencies, and build system |

---

## Project Overview

### What Is MobAgent?

MobAgent is built around three pillars:

1. **A real Linux terminal on Android** — using Alpine Linux rootfs, the app provides a proot-based Linux environment where native binaries can run. It manages multiple terminal sessions (synchronous and asynchronous) for SSH, tool execution, user interaction, and LLM serving.

2. **A local LLM runtime** — The app ships `llama.cpp` pre-compiled ARM64 binaries and can start an OpenAI-compatible HTTP server (`llama-server`) pointing at any `.gguf` model file stored on the device. This enables fully offline AI inference.

3. **A pluggable multi-agent framework** — Built entirely in Java on top of the terminal and LLM runtime, the framework supports:
   - **Tools** — executables that an agent can call, described by `config.json` files
   - **Skills** — named sub-agents with their own system prompt, private tools, and shared public tools
   - **Plugins** — dynamically loaded `.dex` (`.jar`) files that implement formatter, memory, or model interfaces
   - **A ReAct-style agent loop** — LlmHandler → ToolExecutorHandler → LlmHandler (repeat until no more function calls)

---

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Android App                             │
│  ┌──────────────┐  ┌───────────────────────────────────────┐   │
│  │   UI Layer   │  │          Core AI Framework             │   │
│  │ MainActivity │  │  ModelInterface (Builder pattern)      │   │
│  │ SettingsAct. │  │  ┌──────────┐  ┌──────────────────┐   │   │
│  │ SkillsAct.   │  │  │  Memory  │  │  Tools / Skills  │   │   │
│  │ ChatSession  │  │  │(InMemory)│  │  ToolsManager    │   │   │
│  └──────────────┘  │  └──────────┘  └──────────────────┘   │   │
│                    │         │               │               │   │
│  ┌──────────────┐  │  ┌──────────────────────────────────┐  │   │
│  │  Plugin Sys. │  │  │     Agent Loop Engine            │  │   │
│  │  DexLoader   │  │  │  LlmHandler ⇄ ToolExecutorHandler│  │   │
│  └──────────────┘  │  └──────────────────────────────────┘  │   │
│                    │         │                               │   │
│  ┌──────────────┐  │  ┌──────▼──────────┐                   │   │
│  │   Room DB    │  │  │  Models Layer   │                   │   │
│  │ PluginDB     │  │  │  OllamaModel    │                   │   │
│  └──────────────┘  │  │  ChatModel      │                   │   │
│                    │  └─────────────────┘                   │   │
│                    └───────────────────────────────────────┘    │
│                                                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │              Terminal Infrastructure                       │  │
│  │  TerminalSessionManager (SSH / tools / LlamaCpp / user)   │  │
│  │  Alpine Linux rootfs  ·  proot  ·  llama-server (ARM64)   │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Module Structure

```
MobAgent/
├── app/                        ← Application entry point & packaging
├── core/
│   ├── main/                   ← All Java source code (AI framework + app logic)
│   ├── components/             ← Shared UI components
│   ├── resources/              ← String resources, drawables, layouts
│   ├── terminal-emulator/      ← Terminal emulator library (fork of Termux)
│   └── terminal-view/          ← Terminal view widget
├── gradle/
│   └── libs.versions.toml      ← Version catalog
└── doc/                        ← ← YOU ARE HERE — full documentation
```

---

## Key Design Patterns

| Pattern | Where Used |
|---|---|
| **Chain of Responsibility** | `ExecutorChain`, `LlmHandler`, `ToolExecutorHandler` |
| **Builder** | `ModelInterface.Builder`, `OllamaModel.Builder` |
| **Singleton** | `ToolsManager`, `TerminalSessionManager`, `PluginDatabase` |
| **Registry / Service Locator** | `ToolsManager` (toolsRegistry, skillsRegistry) |
| **Plug-and-Play Discovery** | `ToolsScanner`, `SkillsScanner` |
| **Dynamic Class Loading** | `DexLoader` (loads `.dex` plugins at runtime) |
| **Context Object** | `MsgContext` — carries state through the handler chain |

---

## Feature Deep-Dives

- [Agent Loop Engine](./agent-loop-engine/agent-loop-engine.md) — How the ReAct loop works, handler chain, MsgContext
- [Tools System](./tools-system/tools-system.md) — TerminalTool, NativeTool, RAGTool, ToolsScanner, ToolsManager, Executor
- [Skills System](./skills-system/skills-system.md) — Skill model, SkillsScanner, SpawnAgentTool, sub-agent delegation
- [Memory System](./memory-system/memory-system.md) — BuiltInMemory, InMemory, MemoryPluginRegistry, session persistence
- [Models Layer](./models-layer/models-layer.md) — FormatterInterface, OllamaModel, ChatModel, ModelRegistry
- [Plugin System](./plugin-system/plugin-system.md) — BuiltInFormatters, DexLoader, FormatterPlugin, MemoryPlugin, ModelPlugin
- [Chat Sessions](./chat-sessions/chat-sessions.md) — Session entity, plugin selection dialog, message persistence
- [LlamaCpp Integration](./llamacpp/llamacpp.md) — Server lifecycle, binary download, model discovery
- [Terminal Infrastructure](./terminal-infrastructure/terminal-infrastructure.md) — TerminalSessionManager, SshService, Alpine rootfs, llama.cpp
- [UI Layer](./ui-layer/ui-layer.md) — MainActivity, Settings, Skills, ChatSessions, Adapters
- [Database Layer](./database-layer/database-layer.md) — PluginDatabase, entities, DAOs, ChatMessageStore
- [Build Configuration](./build-configuration/build-configuration.md) — Gradle modules, version catalog, build scripts

