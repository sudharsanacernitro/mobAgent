# ModelInterface

> [← Back to Models Layer](./models-layer.md) | [← Back to Docs Root](../models-layer.md)

## Purpose

`ModelInterface` (`org.mobchain.models`) is the **primary façade** for AI agent interaction. It bundles together a model (LLM connection + formatting), a memory store, and a list of tools — and exposes a single `chat()` method that triggers the full agent loop.

---

## Builder Pattern

```java
ModelInterface agent = ModelInterface.builder()
    .setModel(ollamaModel)       // REQUIRED: FormatterInterface
    .setMemory(memory)           // REQUIRED: Memory
    .addTool(spawnAgentTool)     // optional: single tool
    .addTools(toolsList)         // optional: multiple tools
    .build();
```

The `build()` method throws `IllegalStateException` if `model` or `memory` is not set.

---

## Tool Formatting at Construction

When tools are added via `addTool()` or `addTools()`, the `ModelInterface` constructor **reformats each tool's structure** using the model's formatter:

```java
for (Tool tool : builder.tools) {
    try {
        JSONObject updatedToolStructure = model.getStrcturedTool(tool.getStructuredTool());
        tool.setStructuredTool(updatedToolStructure);
    } catch (Exception e) {
        // silently skip — tool keeps original structure
    }
}
```

This allows different LLM backends to use different tool definition formats. For `OllamaModel`, tools are already in OpenAI format, so `getStrcturedTool()` returns them unchanged.

---

## `chat(HumanMessages message)`

The main entry point for sending a message to the agent:

```java
public String chat(HumanMessages message) {

    try {
        memory.addHumanMessage(message);      // 1. store user message in memory

        MsgContext ctx = new MsgContext();
        ctx.put("model",      getModel());    // 2. build context
        ctx.put("toolsArray", tools);
        ctx.put("memory",     getMemory());

        Handler handler = ExecutorChain.getChain();  // 3. assemble agent loop chain
        handler.handle(ctx);                          // 4. run the loop

        return ctx.get("ResponseContent", String.class);  // 5. return final text

    } catch (Exception e) {
        System.out.println(e.getMessage());
    }

    return "";   // returns empty string on error
}
```

---

## Getters

| Method | Returns |
|---|---|
| `getModel()` | The `FormatterInterface` (e.g., OllamaModel) |
| `getMemory()` | The `Memory` instance |
| `getTools()` | The tool list (reformatted for the model) |

---

## Thread Safety

`ModelInterface` is **not thread-safe** by design. Each user interaction should use its own `ModelInterface` instance (or ensure serialized access). `SpawnAgentTool` correctly creates a new `ModelInterface` per sub-agent invocation.

---

## See Also

- [FormatterInterface & OllamaModel](./formatter-and-ollama.md)
- [Agent Loop Engine](../agent-loop-engine/agent-loop-engine.md)
- [Skills System](../skills-system/sub-agent-delegation.md) — SpawnAgentTool creates new ModelInterface instances

