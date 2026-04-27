# Memory System

> [← Back to Documentation Root](../README.md)

The **Memory System** in MobAgent manages the conversation history for AI agents. It provides an abstraction over how messages are stored, retrieved, and persisted — ensuring that the LLM always has the right context window of previous messages when generating a response.

---

## Sub-Features

| File | Description |
|---|---|
| [InMemory](./in-memory.md) | The in-process RAM-based memory implementation |
| [MemoryPluginRegistry](./memory-plugin-registry.md) | Dynamic memory plugin loading and management |
| [Session Persistence](./session-persistence.md) | How chat sessions and messages are persisted to Room DB |

---

## Memory Interface

All memory implementations conform to the `Memory` interface (`org.mobAgent.plugin.interfaces`):

```java
public interface Memory {
    void setSystemPrompt(Messages systemPrompt);
    void addHumanMessage(Messages humanMessage) throws JSONException;
    void addAiMessage(Messages aiMessage) throws JSONException;
    void addAiMessage(JSONObject aiMessage);
    void addToolMessage(Messages toolMessage) throws JSONException;
    List<JSONObject> getAllMessages();
    void clearMemory();
}
```

The memory holds messages in the OpenAI chat format:

```json
[
  { "role": "system",    "content": "You are a helpful assistant..." },
  { "role": "user",      "content": "What is quantum computing?" },
  { "role": "assistant", "content": "Quantum computing is..." },
  { "role": "tool",      "tool_call_id": "call_1", "content": "{...}" },
  { "role": "assistant", "content": null, "tool_calls": [...] }
]
```

---

## Message Types

| Class | Package | Role |
|---|---|---|
| `SystemMessages` | `org.mobchain.messages` | Sets the agent's persona/instructions |
| `HumanMessages` | `org.mobchain.messages` | User's input message |
| `AiMessages` | `org.mobchain.messages` | LLM's text response |
| `ToolMessages` | `org.mobchain.messages` | Tool execution result (fed back to LLM) |

---

## Memory Lifecycle per Agent

```
1. InMemory created (empty)
         │
2. setSessionId(sessionId)   ← loads existing messages from Room DB if resuming session
         │
3. setSystemPrompt(new SystemMessages("..."))
         │
4. Chat loop:
         │
         ├── addHumanMessage(user input)
         ├── [LLM called with getAllMessages()]
         ├── addAiMessage(LLM response)
         ├── [If tool call:]
         │       addToolMessage(tool result)
         │       [LLM called again]
         │       addAiMessage(LLM final response)
         └── Return final response text
```

---

## Memory Window Management

`InMemory` enforces a sliding window of **30 messages** (configurable via `MEMORY_SIZE`). When the window is exceeded:

```java
if (memory.size() > MEMORY_SIZE && memory.size() > 1) {
    memory.remove(1);   // removes the oldest non-system message (index 1)
}
```

The system prompt (index 0) is always preserved.

---

## See Also

- [Database Layer](../database-layer/database-layer.md) — Room DB persistence for chat messages
- [Agent Loop Engine](../agent-loop-engine/agent-loop-engine.md) — how memory is used during the agent loop
- [Plugin System](../plugin-system/plugin-system.md) — custom memory plugins via DEX loading

