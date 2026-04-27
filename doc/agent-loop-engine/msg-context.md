# MsgContext

> [← Back to Agent Loop Engine](./agent-loop-engine.md) | [← Back to Docs Root](../agent-loop-engine.md)

## Purpose

`MsgContext` (`org.mobchain.agentLoopEngine`) is the **shared context object** that flows through every handler in the agent loop. It acts as a typed key-value bag, allowing handlers to read inputs, write outputs, and signal state without tight coupling.

---

## Class Definition

```java
public class MsgContext {

    private Map<String, Object> data;

    public MsgContext() { data = new HashMap<>(); }

    public void put(String key, Object value) { ... }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) { return (T) data.get(key); }

    public boolean has(String key) { return data.containsKey(key); }

    public MsgContext clone() { return new MsgContext(new HashMap<>(this.data)); }

    public boolean remove(String key) { ... }
}
```

### Key Points

- **Type-safe reads via generics** — `ctx.get("model", FormatterInterface.class)` casts and returns the value; throws `ClassCastException` or returns `null` if the key is absent.
- **`canHandle()` uses exception-based detection** — each handler wraps `ctx.get(...)` in a try-catch; any exception means the handler cannot process this context.
- **Shallow clone** — `ctx.clone()` creates a new `MsgContext` with the **same object references** (not deep copies). This is intentional: the `LlmHandler` clones ctx for its inner sub-chain so inner writes don't pollute the outer ctx, yet the objects inside (model, memory, tools) are shared.

---

## Context Keys Used in the Agent Loop

| Key | Type | Set By | Read By | Meaning |
|---|---|---|---|---|
| `"model"` | `FormatterInterface` | `ModelInterface.chat()` | `LlmHandler`, `RequestGeneratorHandler` | The LLM formatter / endpoint |
| `"memory"` | `Memory` | `ModelInterface.chat()` | `LlmHandler`, `ToolExecutorHandler` | The conversation memory |
| `"toolsArray"` | `List<Tool>` | `ModelInterface.chat()` | `RequestGeneratorHandler` | Tools available to the LLM |
| `"responseObject"` | `Response` | `LlmHandler` | `ToolExecutorHandler` | LLM response with function calls |
| `"ResponseContent"` | `String` | `LlmHandler` | `ModelInterface.chat()` | Final text answer from LLM |
| `"exception"` | `Boolean` | `LlmHandler` | (future handlers) | Signals that an error occurred |

---

## See Also

- [Handler & ExecutorChain](./handler-and-chain.md)
- [LlmHandler](./llm-handler.md)
- [ToolExecutorHandler](./tool-executor-handler.md)

