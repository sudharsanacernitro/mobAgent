# LLM Sub-Handlers (Request Pipeline)

> [← Back to Agent Loop Engine](./agent-loop-engine.md) | [← Back to Docs Root](../agent-loop-engine.md)

## Overview

Inside `LlmHandler.process()`, a three-stage mini-chain handles the HTTP communication with the LLM API. These handlers form a **linear pipeline** (no looping):

```
RequestGeneratorHandler → RequestHandler → ResponseHandler
```

All three operate on a **cloned** `MsgContext` to avoid polluting the outer agent loop context.

---

## RequestGeneratorHandler

**Package:** `org.mobchain.agentLoopEngine.Handlers.llmHandler`

**Responsibility:** Builds the HTTP request payload that will be sent to the LLM.

### What it reads from ctx

| Key | Type | Description |
|---|---|---|
| `"model"` | `FormatterInterface` | Provides `getModelName()`, `getApiURL()`, `getHeaders()` |
| `"memory"` | `Memory` | `getAllMessages()` — the full conversation history as `List<JSONObject>` |
| `"toolsArray"` | `List<Tool>` | Each tool's `getStructuredTool()` JSON — formatted for the LLM |

### What it writes to ctx

| Key | Type | Description |
|---|---|---|
| `"httpRequest"` | `Request` (OkHttp / custom) | The fully constructed HTTP request |

### How it works

Uses the `Parser` interface's `toJSON()` default method to build an OpenAI-compatible chat completions request:

```json
{
  "model": "model_name",
  "stream": false,
  "messages": [ ...memory... ],
  "tools": [ ...structuredTools... ]
}
```

---

## RequestHandler

**Package:** `org.mobchain.agentLoopEngine.Handlers.llmHandler`

**Responsibility:** Sends the HTTP request to the LLM API endpoint.

### What it reads from ctx

| Key | Type |
|---|---|
| `"httpRequest"` | `Request` |
| `"model"` | `FormatterInterface` (for URL and headers) |

### What it writes to ctx

| Key | Type | Description |
|---|---|---|
| `"rawResponse"` | `String` | Raw JSON string body from the HTTP response |

Uses `HttpClient.callApi()` which wraps OkHttp.

---

## ResponseHandler

**Package:** `org.mobchain.agentLoopEngine.Handlers.llmHandler`

**Responsibility:** Parses the raw JSON response string into a structured `Response` object.

### What it reads from ctx

| Key | Type |
|---|---|
| `"rawResponse"` | `String` |
| `"model"` | `FormatterInterface` (which implements `Parser` for format-specific parsing) |

### What it writes to ctx

| Key | Type | Description |
|---|---|---|
| `"responseObject"` | `Response` | Parsed response with `getContent()` and `getFunctions()` |

### Response Structure

```java
public class Response {
    String content;                  // text answer (if no function call)
    List<Function> functions;        // function calls requested by the LLM

    public class Function {
        String functionName;
        JSONObject arg;              // parsed argument JSON
    }
}
```

---

## See Also

- [LlmHandler](./llm-handler.md)
- [Models Layer](../models-layer/models-layer.md)
- [Tools System](../tools-system/tools-system.md)

