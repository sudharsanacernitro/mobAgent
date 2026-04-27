# Handler & ExecutorChain

> [← Back to Agent Loop Engine](./agent-loop-engine.md) | [← Back to Docs Root](../agent-loop-engine.md)

## Abstract Handler

`Handler.java` (`org.mobchain.agentLoopEngine`) is the abstract base class for every node in the processing chain. It uses the **Chain of Responsibility** pattern.

### Class Contract

```java
public abstract class Handler {

    private Handler next;

    /** Links this handler to the next one in the chain. */
    public Handler link(Handler next) { ... }

    /** Guard — returns true if this handler can process the given context. */
    public abstract boolean canHandle(MsgContext ctx);

    /** Core logic — mutates ctx, then optionally passes to next. */
    public abstract void process(MsgContext ctx) throws Exception;

    /** Template method: calls canHandle → process → (next.handle if not done). */
    public void handle(MsgContext ctx) { ... }
}
```

### `handle()` Template Method

1. Calls `canHandle(ctx)` — if `false`, returns immediately (no-op)
2. Calls `process(ctx)` — performs the handler's logic
3. If a `next` handler is set **and** `ctx` still has work (i.e., `responseObject` present), calls `next.handle(ctx)`

This enables the **bidirectional loop**: after `ToolExecutorHandler` runs, it calls back to `LlmHandler` without any explicit loop construct.

---

## ExecutorChain

`ExecutorChain.java` (`org.mobchain.agentLoopEngine.Handlers`) is a **factory** that assembles the agent loop.

```java
public class ExecutorChain {

    public static Handler getChain() {

        LlmHandler llmHandler = new LlmHandler();
        ToolExecutorHandler toolExecutorHandler = new ToolExecutorHandler();

        llmHandler.link( toolExecutorHandler );       // LLM → Tool
        toolExecutorHandler.link( llmHandler );       // Tool → LLM (loop back)

        return llmHandler;  // entry point
    }
}
```

### Why Bidirectional Linking?

- `LlmHandler` links forward to `ToolExecutorHandler` so that after detecting a function call, execution flows to tool execution.
- `ToolExecutorHandler` links back to `LlmHandler` so that after a tool result is stored in memory, the LLM is called again with the enriched context.
- The loop terminates naturally when `LlmHandler` produces a response with **no function calls**, at which point it sets `ResponseContent` in ctx and does not propagate further (no `responseObject` key is set for `ToolExecutorHandler.canHandle()` to detect).

---

## See Also

- [LlmHandler](./llm-handler.md)
- [ToolExecutorHandler](./tool-executor-handler.md)
- [MsgContext](./msg-context.md)

