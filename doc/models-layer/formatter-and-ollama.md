# FormatterInterface & OllamaModel

> [← Back to Models Layer](./models-layer.md) | [← Back to Docs Root](../models-layer.md)

## FormatterInterface

**Package:** `org.mobAgent.plugin.interfaces`

`FormatterInterface` is the core abstraction that decouples the agent loop from any specific LLM backend. It extends `Parser` and adds connection-related methods:

```java
public interface FormatterInterface extends Parser {
    String getApiURL();                              // e.g., "http://127.0.0.1:8080/v1/chat/completions"
    String getModelName();                           // e.g., "model" or "llama3.2"
    HashMap<String, String> getHeaders();            // auth headers if needed
    JSONObject getStrcturedTool(JSONObject tool) throws JSONException;  // tool reformatting
}
```

### Parser Interface

`FormatterInterface` extends `Parser`, which handles request/response serialization:

```java
public interface Parser {
    Response fromJSONString(String jsonString) throws JSONException;

    // Default method — builds OpenAI-compatible request body
    default JSONObject toJSON(String modelName, Boolean isStream,
                              List<Tool> toolsList, List<JSONObject> memory)
            throws JSONException { ... }
}
```

`toJSON()` is shared across all formatter implementations. It produces:
```json
{
  "model": "model_name",
  "stream": false,
  "messages": [ ...memory... ],
  "tools": [ ...toolStructures... ]
}
```

---

## OllamaModel

**Package:** `org.mobchain.models`

`OllamaModel` is the default `FormatterInterface` implementation that connects to any **OpenAI-compatible chat completions API** — including `llama-server`, Ollama, LM Studio, and OpenAI itself.

### Builder

```java
OllamaModel model = OllamaModel.builder()
    .baseURL("http://127.0.0.1:8080/v1/chat/completions")
    .model("model")          // model name expected by the API
    .stream(false)           // streaming not yet supported in agent loop
    .headers(headersMap)     // optional: API key or custom headers
    .build();
```

### Implemented Methods

| Method | Behavior |
|---|---|
| `getApiURL()` | Returns the base URL set in the builder |
| `getModelName()` | Returns the model name |
| `getHeaders()` | Returns `null` for localhost (no auth needed) |
| `isStream()` | Returns stream flag |
| `getStrcturedTool(tool)` | Returns `tool` unchanged (already OpenAI-compatible) |
| `fromJSONString(jsonString)` | Parses the LLM response JSON into a `Response` object |

### Response Parsing (`fromJSONString`)

The response is expected in OpenAI chat completions format:

```json
{
  "choices": [{
    "message": {
      "role": "assistant",
      "content": "text answer or null",
      "tool_calls": [{
        "id": "call_abc",
        "type": "function",
        "function": {
          "name": "web_search",
          "arguments": "{\"query\": \"...\"}"
        }
      }]
    }
  }]
}
```

Parsed into:
```java
Response {
    content = "text answer or null",
    functions = [
        Function { functionName = "web_search", arg = { "query": "..." } }
    ]
}
```

---

## Model Plugins (Dynamic Loading)

In addition to the built-in `OllamaModel`, MobAgent supports loading **custom model implementations** as `.dex` plugins. These must implement `FormatterInterface` (or more precisely, provide a `FormatterBuilder` that creates a `FormatterInterface`).

This allows supporting:
- Anthropic Claude format
- Google Gemini format
- Custom proprietary LLM APIs
- Different tokenization/prompt formats

---

## ChatModel

`ChatModel` (`org.mobchain.models`) is an alternative model implementation that may support different interaction patterns (e.g., non-streaming multi-turn with different parsing logic).

---

## See Also

- [ModelInterface](./model-interface.md)
- [LlamaCppServerRepo](./llamacpp-server.md)
- [Plugin System](../plugin-system/plugin-system.md) — custom formatter plugins
- [Agent Loop Engine LLM Sub-Handlers](../agent-loop-engine/llm-sub-handlers.md)

