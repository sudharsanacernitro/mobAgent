# BuiltInFormatters — Default Formatter Registry

> [← Back to Plugin System](./plugin-system.md) | [← Back to Docs Root](../README.md)

## Purpose

`BuiltInFormatters` (`org.mobchain.models`) is a static registry of formatter implementations that are **bundled directly in the app code**. Users do not need to upload any DEX formatter JAR to add their first model plugin — the built-in `OpenAIFormatter` is always available and works with any OpenAI-compatible API out of the box.

This mirrors the pattern used by [`BuiltInMemory`](../memory-system/built-in-memory.md) for the memory layer.

---

## Sentinel IDs

Built-in formatters use **negative integer IDs** that can never collide with Room's auto-generated positive primary keys.

| Constant | Value | Description |
|---|---|---|
| `OPENAI_FORMATTER_ID` | `-1` | The built-in OpenAI-compatible formatter |

```java
public static final int OPENAI_FORMATTER_ID = -1;
public static final String OPENAI_FORMATTER_NAME = "OpenAI Formatter (Built-in)";
```

---

## API

```java
// Check if an ID refers to a built-in
boolean isBuiltIn = BuiltInFormatters.isBuiltIn(formatterPluginId);

// Get ordered map for populating UI spinners
Map<Integer, String> builtIns = BuiltInFormatters.getAll();
// → { -1 → "OpenAI Formatter (Built-in)" }

// Instantiate a fresh FormatterBuilder for a built-in
FormatterBuilder builder = BuiltInFormatters.getBuilder(BuiltInFormatters.OPENAI_FORMATTER_ID);
```

---

## OpenAIFormatterBuilder

`OpenAIFormatterBuilder` (`org.mobchain.models`) implements the `FormatterBuilder` interface (from `mobAgentInterfaces.jar`) and wraps the internal `OpenAIFormatter.Builder`. It is returned by `BuiltInFormatters.getBuilder(-1)`:

```java
// What DexLoader does for built-in IDs:
FormatterBuilder builder = new OpenAIFormatterBuilder();
FormatterInterface formatter = builder
    .baseURL("http://localhost:11434/v1/chat/completions")
    .model("llama3.2")
    .build();
```

`OpenAIFormatter`/`OpenAIFormatterBuilder` supports any OpenAI-compatible endpoint:
- **Ollama** (`http://localhost:11434`)
- **llama-server** (`http://127.0.0.1:<port>/v1/chat/completions`)
- **OpenAI** (`https://api.openai.com/v1/chat/completions`)
- **LM Studio**, **Groq**, **Together AI**, etc.

---

## DexLoader Integration

`DexLoader.loadFormatter()` short-circuits **before** any DEX loading if the formatter ID is a built-in:

```java
public FormatterBuilder loadFormatter(int formatterPluginId) {
    // Built-in formatters — no DEX loading required
    if (BuiltInFormatters.isBuiltIn(formatterPluginId)) {
        return BuiltInFormatters.getBuilder(formatterPluginId);
    }
    // ... existing DEX loading for uploaded plugins
}
```

No JAR file, no `DexClassLoader`, no DB lookup — just a direct Java object instantiation.

---

## UI: Formatter Spinner in "Add Model Plugin" Dialog

`ModelPluginListActivity` populates the formatter spinner with built-in entries first, then user-uploaded ones:

```
Formatter spinner:
  ├── None
  ├── OpenAI Formatter (Built-in)   ← from BuiltInFormatters.getAll(), pre-selected by default
  └── <user-uploaded formatter JARs>
```

The built-in entry is **pre-selected** so a fresh install can create a working model plugin immediately without uploading anything.

---

## DB Storage

When a user picks the built-in formatter in the "Add Model Plugin" dialog, `ModelPlugin.formatterId` is stored as `-1` in the `model_plugin` table. This is valid — there is no FK constraint on `formatterId` in `ModelPlugin`.

---

## Adding a New Built-In Formatter

To add another bundled formatter (e.g., Anthropic Claude):

1. Create `AnthropicFormatterBuilder` implementing `FormatterBuilder`
2. Add a sentinel constant to `BuiltInFormatters`:
   ```java
   public static final int ANTHROPIC_FORMATTER_ID = -2;
   BUILTIN_NAMES.put(ANTHROPIC_FORMATTER_ID, "Anthropic Claude (Built-in)");
   ```
3. Handle in `getBuilder()`:
   ```java
   if (id == ANTHROPIC_FORMATTER_ID) return new AnthropicFormatterBuilder();
   ```

---

## See Also

- [OpenAIFormatter & FormatterInterface](../models-layer/formatter-and-ollama.md) — the underlying formatter class
- [DexLoader](./dex-loader.md) — how uploaded formatter plugins are loaded
- [Formatter Plugins](./formatter-plugins.md) — user-uploadable formatter JARs
- [BuiltInMemory](../memory-system/built-in-memory.md) — the parallel registry for memory

