# BuiltInMemory — Default Memory Registry

> [← Back to Memory System](./memory-system.md) | [← Back to Docs Root](../README.md)

## Purpose

`BuiltInMemory` (`org.mobchain.memory`) is a static registry of memory implementations that are **bundled directly in the app code**. Users do not need to upload any DEX plugin to start using an agent — the built-in `InMemory` is always available as the default.

This mirrors the pattern used by [`BuiltInFormatters`](../plugin-system/built-in-formatters.md) for the formatter layer.

---

## Sentinel IDs

Built-in memory implementations use **negative integer IDs** that can never collide with Room's auto-generated positive primary keys.

| Constant | Value | Description |
|---|---|---|
| `IN_MEMORY_ID` | `-1` | The built-in `InMemory` sliding-window implementation |

```java
public static final int IN_MEMORY_ID = -1;
public static final String IN_MEMORY_NAME = "InMemory (Built-in)";
```

---

## API

```java
// Check if an ID refers to a built-in
boolean isBuiltIn = BuiltInMemory.isBuiltIn(memoryPluginId);

// Get ordered map for populating UI spinners
Map<Integer, String> builtIns = BuiltInMemory.getAll();
// → { -1 → "InMemory (Built-in)" }

// Instantiate a built-in memory
Memory memory = BuiltInMemory.getInstance(BuiltInMemory.IN_MEMORY_ID, chatMessageStore);
```

---

## DB Storage Contract

The `chat_sessions` table stores `memory_plugin_id` as a **FK to `plugins.id`**. Storing `-1` would violate the FK constraint. Therefore:

| Location | Value stored |
|---|---|
| **Room DB** (`chat_sessions.memory_plugin_id`) | `null` — FK-safe |
| **Agent init** (`initAgent()` parameter) | `-1` sentinel OR `null` — both map to built-in |

`MainActivity.initAgent()` treats both `null` and any `BuiltInMemory.isBuiltIn(id) == true` as "use the built-in InMemory":

```java
if (memoryPluginId == null || BuiltInMemory.isBuiltIn(memoryPluginId)) {
    int builtInId = (memoryPluginId != null) ? memoryPluginId : BuiltInMemory.IN_MEMORY_ID;
    memory = BuiltInMemory.getInstance(builtInId, chatMessageStore);
} else {
    // Custom DEX-loaded memory plugin (future)
    memory = new InMemory(chatMessageStore); // fallback for now
}
```

---

## UI Spinner Population

Both the plugin selection dialog (`MainActivity`) and the session edit dialog (`ChatSessionListActivity`) populate the memory spinner using `BuiltInMemory.getAll()` first, followed by user-uploaded memory plugins from the DB:

```
Memory spinner:
  ├── InMemory (Built-in)      ← always first, from BuiltInMemory.getAll()
  ├── VectorMemory             ← user-uploaded DEX plugins
  └── SummarizationMemory      ← user-uploaded DEX plugins
```

---

## Previous Session Compatibility

When a user **switches to a previous session** that was created before this registry existed (i.e., `memory_plugin_id = null` in DB), `initAgent()` correctly resolves `null → BuiltInMemory.IN_MEMORY_ID → new InMemory(...)`. No migration needed.

---

## Adding a New Built-In Memory

To add another bundled memory implementation:

1. Implement `Memory` in a new class (e.g., `SlidingWindowMemory`)
2. Add a new sentinel constant to `BuiltInMemory`:
   ```java
   public static final int SLIDING_WINDOW_ID = -2;
   BUILTIN_NAMES.put(SLIDING_WINDOW_ID, "Sliding Window (Built-in)");
   ```
3. Handle the new ID in `getInstance()`:
   ```java
   if (id == SLIDING_WINDOW_ID) return new SlidingWindowMemory(dbMessageStore);
   ```

---

## See Also

- [InMemory](./in-memory.md) — the concrete implementation returned by this registry
- [Session Persistence](./session-persistence.md) — how messages are loaded on session switch
- [BuiltInFormatters](../plugin-system/built-in-formatters.md) — the parallel registry for formatters
- [Plugin System](../plugin-system/plugin-system.md) — DEX-loaded custom memory plugins

