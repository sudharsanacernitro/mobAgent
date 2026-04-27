# ModelRegistry

> [← Back to Models Layer](./models-layer.md) | [← Documentation Root](../models-layer.md)

`ModelRegistry` is a static registry that maps model plugin IDs to their instantiated `FormatterInterface` objects. It acts as a cache so that formatter plugins (which require DEX loading) are not reloaded for every agent interaction.

---

## Purpose

Loading a formatter plugin involves:
1. Finding the JAR file path in the database
2. Creating a `DexClassLoader`
3. Loading the `org.mobAgent.FormatterBuilderImpl` class
4. Calling `build()` to get a `FormatterInterface`

This is expensive. `ModelRegistry` caches the result by plugin ID.

---

## Usage

```java
// Check if already loaded
FormatterInterface formatter = ModelRegistry.getModel(pluginId);

if (formatter == null) {
    // Load from DEX
    DexLoader dexLoader = new DexLoader(context);
    FormatterBuilder builder = dexLoader.loadFormatter(pluginId);
    formatter = builder.baseURL(apiUrl).model(modelName).build();
    ModelRegistry.register(pluginId, formatter);
}
```

---

## Relationship to DexLoader

`DexLoader` handles the raw DEX class loading. `ModelRegistry` sits above it as an optional caching layer. During agent initialisation in `MainActivity.initAgent()`, the registry prevents redundant JAR loading when multiple sessions use the same formatter plugin.

---

## See Also

- [DexLoader](../plugin-system/dex-loader.md) — performs the actual DEX loading
- [FormatterInterface & OllamaModel](./formatter-and-ollama.md) — the interface being registered
- [ModelInterface](./model-interface.md) — consumes the formatter from the registry
- [Plugin System](../plugin-system/plugin-system.md) — how formatter plugins are stored and managed

