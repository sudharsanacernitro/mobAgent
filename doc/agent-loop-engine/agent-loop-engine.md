# Agent Loop Engine

> [← Back to Documentation Root](../README.md)

The **Agent Loop Engine** is the heart of MobAgent's AI framework. It implements a **ReAct-style** (Reason + Act) agent execution loop where the LLM reasons about what to do, the framework executes the chosen tool, the result is fed back into the LLM's memory, and the process repeats until the LLM produces a final answer with no more tool calls.

---

## Sub-Features

| File | Description |
|---|---|
| [Handler & Chain](./handler-and-chain.md) | The abstract `Handler` class and `ExecutorChain` wiring |
| [LlmHandler](./llm-handler.md) | Calls the LLM, processes its response, detects function calls |
| [ToolExecutorHandler](./tool-executor-handler.md) | Executes tool calls returned by the LLM |
| [MsgContext](./msg-context.md) | The shared context object that flows through the handler chain |
| [LLM Sub-Handlers](./llm-sub-handlers.md) | RequestGeneratorHandler, RequestHandler, ResponseHandler |

---

## How the Loop Works

```
User Message
     │
     ▼
ModelInterface.chat()
     │
     ├── memory.addHumanMessage(message)
     │
     ▼
ExecutorChain.getChain()   ← builds: LlmHandler ⇄ ToolExecutorHandler
     │
     ▼
LlmHandler.handle(ctx)
     │
     ├── Calls LLM via HTTP (RequestGeneratorHandler → RequestHandler → ResponseHandler)
     │
     ├─── If NO function call in response:
     │       memory.addAiMessage(response)
     │       ctx.put("ResponseContent", text)
     │       STOP ← final answer returned to user
     │
     └─── If function call present:
             memory.addAiMessage(response)
             ctx.put("responseObject", response)
             ▼
         ToolExecutorHandler.handle(ctx)
             │
             ├── Executor.execute(response, memory)
             │       ├── Looks up tool by name in ToolsManager
             │       ├── Calls tool.runTool(args)
             │       └── memory.addToolMessage(toolResult)
             │
             └── ctx.remove("responseObject")
                     ▼
                 Back to LlmHandler (loop repeats)
```

---

## Files

| Java Class | Package | Role |
|---|---|---|
| `Handler.java` | `org.mobchain.agentLoopEngine` | Abstract base with `canHandle()`, `process()`, `handle()`, `link()` |
| `ExecutorChain.java` | `org.mobchain.agentLoopEngine.Handlers` | Wires LlmHandler ↔ ToolExecutorHandler |
| `LlmHandler.java` | `org.mobchain.agentLoopEngine.Handlers` | LLM invocation + function-call detection |
| `ToolExecutorHandler.java` | `org.mobchain.agentLoopEngine.Handlers` | Tool execution dispatch |
| `MsgContext.java` | `org.mobchain.agentLoopEngine` | Key-value context bag passed through handlers |
| `RequestGeneratorHandler.java` | `…Handlers.llmHandler` | Builds the HTTP request payload |
| `RequestHandler.java` | `…Handlers.llmHandler` | Sends the HTTP request |
| `ResponseHandler.java` | `…Handlers.llmHandler` | Parses the raw HTTP response into a `Response` |

---

## See Also

- [Tools System](../tools-system/tools-system.md) — how tools are registered and executed
- [Memory System](../memory-system/memory-system.md) — how messages are stored
- [Models Layer](../models-layer/models-layer.md) — FormatterInterface and OllamaModel

