# ChatModel

> [← Back to Models Layer](./models-layer.md) | [← Documentation Root](../models-layer.md)

`ChatModel` is an alternative `FormatterInterface` implementation alongside `OllamaModel`. While `OllamaModel` targets OpenAI-compatible chat completion APIs, `ChatModel` provides a more directly configurable model connection with explicit header management.

---

## Purpose

`ChatModel` is used when a model plugin JAR provides a custom formatter. The `FormatterBuilder` interface (from `sharedToolInterface.jar`) produces a `FormatterInterface` — `ChatModel` is the base class used by many such custom implementations.

---

## FormatterInterface Contract

All models — `OllamaModel`, `ChatModel`, and any DEX-loaded formatter — must implement `FormatterInterface`:

```java
public interface FormatterInterface extends Parser {

    /**
     * Returns the full endpoint URL for the LLM API.
     * e.g. "http://127.0.0.1:8080/v1/chat/completions"
     */
    String getApiURL();

    /**
     * Returns the model identifier string sent in requests.
     * e.g. "llama3", "gpt-4"
     */
    String getModelName();

    /**
     * Returns HTTP headers to include in requests (auth, content-type, etc.)
     */
    HashMap<String, String> getHeaders();

    /**
     * Transforms a tool definition JSONObject to the format expected by this model.
     * For OpenAI-compatible models, this is a no-op (return the same object).
     * For Claude/Gemini models with different schemas, reformat here.
     */
    JSONObject getStrcturedTool(JSONObject tool) throws JSONException;
}
```

---

## Parser Interface

`FormatterInterface` extends `Parser`, which adds:

```java
public interface Parser {
    /**
     * Serializes model name + memory messages + tools into the API request body JSON.
     */
    JSONObject toJSON(Memory memory, List<Tool> toolsList) throws JSONException;

    /**
     * Parses the raw API response JSON string into a Response object.
     */
    Response fromJSONString(String jsonString) throws JSONException;
}
```

---

## OllamaModel Implementation (Concrete Example)

`OllamaModel` is the primary implementation:

```java
// Building with OllamaModel
OllamaModel model = OllamaModel.builder()
    .baseURL("http://127.0.0.1:8080/v1/chat/completions")
    .model("llama3")
    .stream(false)
    .addHeader("Authorization", "Bearer <token>")  // optional
    .build();
```

`toJSON()` output:
```json
{
    "model": "llama3",
    "messages": [ { "role": "user", "content": "..." }, ... ],
    "stream": false,
    "tools": [ { "type": "function", "function": { ... } }, ... ]
}
```

`fromJSONString()` parses the OpenAI response format:
```json
{
    "choices": [{
        "message": {
            "content": "...",
            "tool_calls": [{ "function": { "name": "...", "arguments": "..." } }]
        }
    }]
}
```

---

## See Also

- [FormatterInterface & OllamaModel](./formatter-and-ollama.md) — detailed formatting logic
- [ModelInterface](./model-interface.md) — uses `FormatterInterface` to build agents
- [Formatter Plugins](../plugin-system/formatter-plugins.md) — custom formatters loaded via DexLoader
- [Agent Loop Engine](../agent-loop-engine/agent-loop-engine.md) — `LlmHandler` uses `FormatterInterface`

