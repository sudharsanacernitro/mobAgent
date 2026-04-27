# MemoryPluginRegistry

> [← Back to Memory System](./memory-system.md) | [← Back to Docs Root](../memory-system.md)

## Purpose

`MemoryPluginRegistry` (`org.mobchain.memory`) provides a **dynamic registry** for custom memory plugins. Rather than being limited to the built-in `InMemory`, developers can provide alternative memory implementations (e.g., vector memory, summarization-based memory, persistent memory with semantic search) as `.dex` plugins that are loaded at runtime.

---

## How Memory Plugins Work

Memory plugins are loaded via the Android `DexClassLoader` (same mechanism as formatter and model plugins). A memory plugin must implement the `Memory` interface.

```
Plugin .dex file
    └── implements org.mobAgent.plugin.interfaces.Memory
            ├── setSystemPrompt()
            ├── addHumanMessage()
            ├── addAiMessage()
            ├── addToolMessage()
            ├── getAllMessages()
            └── clearMemory()
```

The `MemoryPluginRegistry` keeps track of which memory plugin is currently active and provides it to `ModelInterface` builders when constructing agents.

---

## Room DB Entity: `MemoryPlugin`

Memory plugins are persisted in the Room database through the `MemoryPlugin` entity:

```java
@Entity
public class MemoryPlugin {
    @PrimaryKey(autoGenerate = true)
    int id;
    String name;
    String path;        // path to the .dex/.jar file
    String description;
}
```

This allows the user to install, switch, and manage memory plugins through the **Memory Plugin** screen in Settings.

---

## UI Integration

The Memory Plugin activity (`MemoryPluginActivity`) allows users to:
1. Browse installed memory plugins
2. Upload a new memory plugin (`.dex` file)
3. Set a memory plugin as the default

When a memory plugin is set as default, it is used for all new agent interactions instead of `InMemory`.

---

## See Also

- [InMemory](./in-memory.md) — the built-in default memory
- [Plugin System](../plugin-system/plugin-system.md) — DEX loading mechanism
- [Database Layer](../database-layer/database-layer.md) — MemoryPlugin entity and DAO

