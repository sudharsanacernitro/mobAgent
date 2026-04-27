# ToolExecutorHandler

> [← Back to Agent Loop Engine](./agent-loop-engine.md) | [← Back to Docs Root](../agent-loop-engine.md)

## Purpose

`ToolExecutorHandler` (`org.mobchain.agentLoopEngine.Handlers`) is responsible for **executing the tool call** that the LLM requested. It reads the `Response` object from context (which contains function name + arguments), dispatches it to the appropriate tool via `ToolsManager`, and stores the tool's output in memory so the next LLM invocation sees it.

---

## Guard: `canHandle()`

```java
@Override
public boolean canHandle(MsgContext ctx) {
    ctx.get("responseObject", Response.class);   // must be present
    ctx.get("memory", Memory.class);             // must be present
    return true;
}
```

This handler only activates when `"responseObject"` is in the context — which only happens when `LlmHandler` detected a function call. If `LlmHandler` produced a final text answer it never sets `responseObject`, so `ToolExecutorHandler` gracefully skips.

---

## Processing: `process()`

```java
Response res   = ctx.get("responseObject", Response.class);
Memory memory  = ctx.get("memory", Memory.class);

Executor.execute(res, memory);   // runs the tool + stores result in memory

ctx.remove("responseObject");    // clean up so handler chain terminates if needed
```

After `ctx.remove("responseObject")`, the chain's next iteration starts at `LlmHandler` again. Because `responseObject` is absent, `ToolExecutorHandler.canHandle()` will return `false` after the next LLM call (unless the LLM makes another tool call).

---

## Executor Dispatch

`Executor.java` (`org.mobchain.tools.executor`) handles the actual dispatch:

```java
public static void execute(Response res, Memory memory) throws JSONException {

    List<Response.Function> functionList = res.getFunctions();
    Response.Function function = functionList.get(0);          // single tool call per turn

    Tool tool = ToolsManager.getToolByName(function.getFunctionName());

    JSONObject toolOutput = tool.runTool(function.getArg());   // ← tool binary invoked here

    memory.addToolMessage(
        new ToolMessages(function.getFunctionName(), toolOutput)
    );
}
```

> **Note:** The current implementation executes only the **first** function call per turn. Multiple simultaneous function calls are not yet supported.

---

## See Also

- [Handler & ExecutorChain](./handler-and-chain.md)
- [LlmHandler](./llm-handler.md)
- [Tools System](../tools-system/tools-system.md)
- [Memory System](../memory-system/memory-system.md)

