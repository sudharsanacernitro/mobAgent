# LlmHandler

> [← Back to Agent Loop Engine](./agent-loop-engine.md) | [← Back to Docs Root](../agent-loop-engine.md)

## Purpose

`LlmHandler` (`org.mobchain.agentLoopEngine.Handlers`) is responsible for **invoking the LLM** and interpreting its response. It determines whether the LLM wants to call a tool (function call) or has produced a final answer.

---

## Guard: `canHandle()`

```java
@Override
public boolean canHandle(MsgContext ctx) {
    ctx.get("model", FormatterInterface.class);   // must be present
    ctx.get("memory", Memory.class);              // must be present
    return true;   // returns false on exception
}
```

The handler requires two keys in `MsgContext`:
- `"model"` — a `FormatterInterface` (e.g., `OllamaModel`) that knows how to format the request and connect to the LLM endpoint
- `"memory"` — a `Memory` instance holding the conversation history

---

## Processing: `process()`

### Step 1 — Build and execute the LLM sub-chain

```java
RequestGeneratorHandler requestGeneratorHandler = new RequestGeneratorHandler();
RequestHandler requestHandler = new RequestHandler();
ResponseHandler responseHandler = new ResponseHandler();

requestGeneratorHandler.link(requestHandler);
requestHandler.link(responseHandler);

MsgContext clonedCtx = ctx.clone();
requestGeneratorHandler.handle(clonedCtx);
```

A **cloned context** is used for the inner chain so the outer context remains clean until results are ready.

### Step 2 — Inspect the response

```java
Response res = clonedCtx.get("responseObject", Response.class);
ctx.get("memory", Memory.class).addAiMessage(res.getJsonAIMessage());
```

The AI's raw JSON message is **always** stored in memory immediately.

### Step 3 — Branch on function calls

```java
if (res.getFunctions().isEmpty()) {
    // Terminal condition: LLM gave a text answer
    ctx.put("ResponseContent", res.getContent());
    return;                     // loop stops here
}

// Function call detected — pass the response object downstream
ctx.put("responseObject", res);
// ToolExecutorHandler.canHandle() will now return true → loop continues
```

---

## Inner Sub-Chain: LLM Request Pipeline

| Handler | Responsibility |
|---|---|
| `RequestGeneratorHandler` | Reads `model`, `memory`, `toolsArray` from ctx; builds an HTTP request JSON body |
| `RequestHandler` | Sends the HTTP POST to the model's API URL using `HttpClient` |
| `ResponseHandler` | Parses the raw JSON response into a `Response` object; stores in `clonedCtx` as `"responseObject"` |

---

## See Also

- [Handler & ExecutorChain](./handler-and-chain.md)
- [ToolExecutorHandler](./tool-executor-handler.md)
- [MsgContext](./msg-context.md)
- [Models Layer](../models-layer/models-layer.md)

