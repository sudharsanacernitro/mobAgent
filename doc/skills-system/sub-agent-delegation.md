# Sub-Agent Delegation

> [← Back to Skills System](./skills-system.md) | [← Back to Docs Root](../skills-system.md)

## Overview

Sub-agent delegation is the mechanism by which the **root agent** hands off a specialized task to a **skill-specific sub-agent**. This is one of the most powerful features of MobAgent: it enables a multi-agent hierarchy where different AI personas with different tools collaborate to answer complex user queries.

---

## End-to-End Delegation Flow

```
User: "Research the latest papers on quantum computing and write a Python script to simulate a qubit"
         │
         ▼
Root Agent (LlmHandler + all skills listed in system prompt)
         │
         ├── "I need to research → use researcher skill"
         │        │
         │        ▼
         │   spawn_agent({ skillName: "researcher", userMessage: "latest quantum computing papers" })
         │        │
         │        ▼
         │   SpawnAgentTool.runTool()
         │        │
         │        ├── Creates: InMemory (fresh), SystemPrompt = researcher.description
         │        ├── Tools: [web_search, summarizer] + public tools
         │        ├── Model: OllamaModel → llama-server at port 8080
         │        └── ModelInterface.chat("latest quantum computing papers")
         │                │
         │                └── [researcher sub-agent loop runs independently]
         │                        ├── LlmHandler calls web_search
         │                        ├── LlmHandler calls summarizer
         │                        └── Returns: "Here are the key findings: ..."
         │        │
         │        ▼
         │   result: { "outputOfTheSkill": "Here are the key findings: ..." }
         │
         ├── Now has research results in memory
         │
         ├── "I need to write code → use coder skill"
         │        │
         │        ▼
         │   spawn_agent({ skillName: "coder", userMessage: "write Python qubit simulation" })
         │        └── [coder sub-agent loop runs independently]
         │
         └── Root LLM combines outputs → final answer to user
```

---

## Memory Isolation

Each sub-agent created by `SpawnAgentTool` gets its **own fresh `InMemory` instance**:

```java
InMemory memory = new InMemory(new ChatMessageStore(MainActivity.getAppContext()));
memory.setSystemPrompt(new SystemMessages(skill.getDescription()));
```

- Sub-agents do **not** share the root agent's conversation history
- Sub-agents do **not** share memory with each other
- Sub-agent results are returned as a plain string to the root agent, which adds them to the root memory as a tool response

---

## System Prompt Hierarchy

| Agent Level | System Prompt Source |
|---|---|
| Root agent | User-configured + `ToolsManager.getSystemPromptForSkills()` |
| Sub-agent | `skill.getDescription()` (from `skill.json`) |

---

## Recursive Delegation

Sub-agents can themselves call `spawn_agent` if they have access to it (through public tools). This enables arbitrarily deep agent hierarchies. The depth is limited only by:
- Available memory (each level holds its own conversation)
- Stack depth (Java call stack)
- LLM context window (each level uses tokens)

---

## Fixed Model Endpoint

Currently, all sub-agents connect to the same model endpoint:

```java
OllamaModel model = OllamaModel.builder()
    .baseURL("http://127.0.0.1:8080/v1/chat/completions")
    .model("model")
    .build();
```

This means all agents (root and sub-agents) use the same local `llama-server` instance. Different skills using different models would require enhancing the `Skill` schema to include a model reference.

---

## See Also

- [SpawnAgentTool](../tools-system/native-tools.md) — Java implementation
- [Skill Model](./skill-model.md) — skill data structure
- [ModelInterface](../models-layer/models-layer.md) — used to create sub-agents
- [Memory System](../memory-system/memory-system.md) — InMemory per sub-agent

