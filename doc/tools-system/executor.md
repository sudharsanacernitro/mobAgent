# Executor

> [← Back to Tools System](./tools-system.md) | [← Back to Docs Root](../tools-system.md)

## Purpose

`Executor` (`org.mobchain.tools.executor`) is the **dispatch layer** between the agent loop and the tool implementations. It takes a parsed `Response` object (which contains the function call(s) requested by the LLM), looks up the correct tool, calls it, and stores the result in memory.

---

## Static Method: `execute()`

```java
public static void execute(Response res, Memory memory) throws JSONException {

    List<Response.Function> functionList = res.getFunctions();

    if (functionList.isEmpty()) return;

    Response.Function function = functionList.get(0);  // first function call only

    Tool tool = ToolsManager.getToolByName(function.getFunctionName());

    assert tool != null;

    JSONObject toolOutput = tool.runTool(function.getArg());

    memory.addToolMessage(new ToolMessages(function.getFunctionName(), toolOutput));
}
```

---

## Execution Flow Detail

```
Response.getFunctions()
    │
    ├── [empty]  → return (no-op, this shouldn't happen since ToolExecutorHandler
    │                       checks canHandle() which requires a response object)
    │
    └── [function at index 0]
            │
            ├── functionName: "web_search"
            ├── arg: { "query": "latest AI research" }
            │
            ▼
        ToolsManager.getToolByName("web_search")
            │
            ▼
        tool.runTool({ "query": "latest AI research" })
            │
            ├── TerminalTool → executes binary in Alpine Linux session
            ├── SpawnAgentTool → creates sub-agent, runs full agent loop
            └── RAGTool → performs document retrieval
            │
            ▼
        { "results": [...] }
            │
            ▼
        memory.addToolMessage(
            new ToolMessages("web_search", { "results": [...] })
        )
            │
            ▼
        LLM now sees: role="tool", content=<result>, tool_call_id=<id>
```

---

## ToolMessages

`ToolMessages` (`org.mobchain.messages`) implements the `Messages` interface and wraps a tool result into the correct chat format for the LLM:

```json
{
  "role": "tool",
  "tool_call_id": "call_abc123",
  "name": "web_search",
  "content": "{\"results\": [...]}"
}
```

---

## Current Limitations

| Limitation | Description |
|---|---|
| **Single function call per turn** | Only `functionList.get(0)` is executed; if the LLM requests multiple simultaneous function calls, only the first is processed |
| **No assertion safety** | `assert tool != null` — if the LLM hallucinates a non-existent tool name, this will throw `AssertionError` |
| **No timeout handling** | The executor itself doesn't have a timeout; tool-level timeouts exist in `TerminalTool` (20 seconds) |

---

## See Also

- [ToolExecutorHandler](../agent-loop-engine/tool-executor-handler.md)
- [TerminalTool](./terminal-tool.md)
- [Memory System](../memory-system/memory-system.md)

