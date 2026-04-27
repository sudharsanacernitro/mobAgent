# Tools System

> [← Back to Documentation Root](../README.md)

The **Tools System** is the action layer of MobAgent's agentic framework. When the LLM decides to perform an action, it emits a function call. The `ToolExecutorHandler` looks up the named tool in the registry and calls `runTool(args)`. The result goes back to the LLM as a tool message, completing one iteration of the ReAct loop.

Every tool is a self-describing callable that receives a JSON argument object and returns a JSON result. MobAgent supports **three tool kinds**: Alpine-binary-backed tools (`TerminalTool`), Java-native tools (`SpawnAgentTool`), and a RAG retrieval tool. A **plug-and-play scanner** discovers tools from the Alpine filesystem on startup, so new tools can be added just by dropping files into the right directory.

---

## Sub-Features

| File | Description |
|---|---|
| [TerminalTool](./terminal-tool.md) | File-system-backed tools that run as Alpine Linux binaries |
| [NativeTool & SpawnAgentTool](./native-tools.md) | Java-native tools: SpawnAgentTool (sub-agent delegation) |
| [RAGTool](./rag-tool.md) | Retrieval-Augmented Generation tool |
| [ToolsScanner](./tools-scanner.md) | Plug-and-play discovery of tools from the file system |
| [ToolsManager](./tools-manager.md) | Central registry for all tools and skills |
| [Executor](./executor.md) | Dispatches LLM tool-call requests to the correct tool |

---

## Overview

### Tool Interface

All tools implement the `Tool` interface (from `org.mobAgent.plugin.interfaces`):

```java
public interface Tool {
    String getToolName();
    String getDescription();
    String getSkillName();
    JSONObject getStructuredTool();
    void setStructuredTool(JSONObject structuredTool);
    JSONObject runTool(JSONObject args);
}
```

- **`getStructuredTool()`** — returns an OpenAI-compatible function definition JSON that describes the tool to the LLM
- **`runTool(JSONObject args)`** — executes the tool with the provided arguments and returns a JSON result
- **`getSkillName()`** — returns which skill "owns" this tool (`"root"` for global tools)

---

## Tool Types

### 1. TerminalTool — File-System Tools

Each `TerminalTool` corresponds to a **binary** in the Alpine Linux rootfs. The tool is executed via a special wrapper script (`/root/ToolsWrapper.sh`) in a synchronous terminal session. The output is parsed for a `==Result==` delimiter to extract the JSON result.

**Directory layout required:**
```
tools/
└── web_search/
    ├── config.json      ← tool metadata
    └── web_search       ← the binary to run
```

**`config.json` format:**
```json
{
  "name": "web_search",
  "description": "Searches the web for a given query",
  "binary": "web_search",
  "requiredParams": ["query"],
  "structuredTool": {
    "type": "function",
    "function": {
      "name": "web_search",
      "description": "Searches the web",
      "parameters": {
        "type": "object",
        "required": ["query"],
        "properties": {
          "query": { "type": "string", "description": "The search query" }
        }
      }
    }
  }
}
```

### 2. NativeTool — Java-Native Tools

These are Java classes that implement `Tool` directly. The key example is `SpawnAgentTool` which is registered under the `"root"` skill and allows one agent to delegate work to a sub-agent.

### 3. RAGTool — Retrieval Tool

`RAGTool` provides Retrieval-Augmented Generation capability, allowing agents to query a document knowledge base.

---

## Tool Registration Flow

```
App Startup
    │
    ├── ToolsScanner("alpine/root/tools").scanAndRegister()
    │       └── For each tool directory:
    │               reads config.json → creates TerminalTool → ToolsManager.addTools("root", tool)
    │
    ├── SkillsScanner("alpine/root/skills").scanAndRegister()
    │       └── For each skill directory:
    │               ToolsScanner(skill/tools).scanAndRegister()  ← private tools
    │               Resolves public tools from root in ToolsManager
    │
    └── SpawnAgentTool registered as native tool under "root"
```

---

## Tool Execution Flow

```
LLM Response contains function call { name: "web_search", args: { query: "..." } }
                    │
                    ▼
          Executor.execute(response, memory)
                    │
                    ▼
          ToolsManager.getToolByName("web_search")
                    │
                    ▼
          TerminalTool.runTool({ query: "..." })
                    │
                    ├── Validates required params
                    ├── Builds command: /root/ToolsWrapper.sh /path/to/web_search '{"query":"..."}'
                    ├── Executes via TerminalSynchronousSessionHandler (20s timeout)
                    ├── Extracts output between ==Result== delimiters
                    └── Returns JSONObject result
                    │
                    ▼
          memory.addToolMessage(new ToolMessages("web_search", result))
```

---

## See Also

- [Agent Loop Engine](../agent-loop-engine/agent-loop-engine.md) — how tools are called from the loop
- [Skills System](../skills-system/skills-system.md) — skills group tools into named sub-agents
- [Terminal Infrastructure](../terminal-infrastructure/terminal-infrastructure.md) — how terminal sessions work

