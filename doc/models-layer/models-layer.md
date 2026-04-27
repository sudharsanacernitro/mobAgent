# Models Layer

> [← Back to Documentation Root](../README.md)

The **Models Layer** abstracts the LLM backend from the rest of the framework. It defines how requests are formatted, where they are sent, and how responses are parsed — enabling the framework to work with different LLM backends (Ollama, LlamaCpp, remote OpenAI-compatible APIs) by swapping the model plugin.

---

## Sub-Features

| File | Description |
|---|---|
| [ModelInterface](./model-interface.md) | The builder-pattern facade for creating and chatting with agents |
| [FormatterInterface & OllamaModel](./formatter-and-ollama.md) | The LLM formatting/connection abstraction |
| [ChatModel](./chat-model.md) | Alternative model implementation |
| [ModelRegistry](./model-registry.md) | Registry of loaded model plugins |
| [LlamaCppServerRepo](./llamacpp-server.md) | Managing the local llama-server process |

---

## Overview

### Layer Responsibilities

```
User Request
     │
     ▼
ModelInterface.chat(HumanMessages)
     │  Uses: FormatterInterface (model), Memory, List<Tool>
     ▼
Agent Loop (LlmHandler)
     │
     ▼
RequestGeneratorHandler
     │  Uses: FormatterInterface.toJSON(model, stream, tools, memory)
     │        → builds OpenAI-compatible request body
     ▼
RequestHandler
     │  Uses: FormatterInterface.getApiURL(), getHeaders()
     │        → sends HTTP POST
     ▼
ResponseHandler
     │  Uses: FormatterInterface (as Parser).fromJSONString(body)
     │        → parses response into Response object
     ▼
Response { content, functions }
```

### FormatterInterface

`FormatterInterface` combines two concerns:
1. **Formatting** — knows how to build and parse the LLM API request/response (implements `Parser`)
2. **Connection** — knows the API URL, model name, and authentication headers

```java
public interface FormatterInterface extends Parser {
    String getApiURL();
    String getModelName();
    HashMap<String, String> getHeaders();
    JSONObject getStrcturedTool(JSONObject tool) throws JSONException;
}
```

The `getStrcturedTool()` method allows the model to **transform** tool definitions if needed (e.g., a model that uses a different tool schema than OpenAI would reformat here).

### OllamaModel

`OllamaModel` is the primary `FormatterInterface` implementation. It supports the **OpenAI-compatible chat completions API**, meaning it works with:
- `llama-server` (from llama.cpp) — used for local on-device inference
- Ollama server — if running on a local network
- Any OpenAI-compatible API (LM Studio, etc.)

---

## ModelInterface — The Agent Facade

`ModelInterface` is the **entry point** for all chat interactions. Built via a fluent builder:

```java
ModelInterface agent = ModelInterface.builder()
    .setModel(ollamaModel)                    // LLM formatter
    .setMemory(memory)                        // conversation memory
    .addTool(spawnAgentTool)                  // add individual tool
    .addTools(ToolsManager.getToolsArray())   // or add all tools
    .build();

String response = agent.chat(new HumanMessages("Hello!"));
```

At construction time, `ModelInterface` calls `model.getStrcturedTool(tool.getStructuredTool())` for each tool — allowing the model to reformat the tool definition if its schema differs from the input format.

---

## Local LLM: LlamaCpp Integration

MobAgent ships with pre-compiled `llama-server` ARM64 binaries (`llamaCppBinariesForAndroid.zip`). The app:

1. Extracts the binaries to the app's files directory
2. Makes them executable
3. Starts `llama-server` as a background process on a configurable port
4. Configures `OllamaModel` to point at `http://127.0.0.1:<port>/v1/chat/completions`

The `LlamaCppServerRepo` handles this process lifecycle.

---

## See Also

- [Agent Loop Engine](../agent-loop-engine/agent-loop-engine.md) — consumes FormatterInterface
- [Plugin System](../plugin-system/plugin-system.md) — model plugins loaded via DexLoader
- [Terminal Infrastructure](../terminal-infrastructure/terminal-infrastructure.md) — llama-server process management

