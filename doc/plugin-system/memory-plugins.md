# Plugin System — Memory Plugins

> [← Back to Plugin System](./plugin-system.md) | [← Documentation Root](../plugin-system.md)

A **Memory Plugin** is a dynamically loadable `.dex` JAR that implements a custom conversation memory strategy — for example, vector similarity search, summarisation-based memory, or any other approach beyond the default sliding window.

---

## MemoryPlugin Entity

```java
@Entity(
    tableName = "memory_plugins",
    foreignKeys = @ForeignKey(
        entity = Plugin.class,
        parentColumns = "id",
        childColumns = "plugin_id",
        onDelete = ForeignKey.CASCADE
    )
)
public class MemoryPlugin {
    @PrimaryKey
    @ColumnInfo(name = "plugin_id") public int pluginId;
    // Additional fields for memory-specific configuration can be added here
}
```

Like all plugin sub-types, `plugin_id` is the global PK shared with the parent `Plugin` row — no separate auto-generated ID.

---

## When Is a Memory Plugin Used?

In `MainActivity.initAgent()`, if the selected session has a `memoryPluginId`, the app would load a custom memory implementation. Currently, if no memory plugin is selected, `InMemory` (the built-in sliding-window implementation) is used by default.

---

## DefaultPlugin for Memory

The `default_plugins` table stores which plugin is the default for each type:

```sql
INSERT INTO default_plugins (plugin_type, plugin_id) VALUES ('memory', <id>);
```

`DefaultPluginDao.getDefaultPluginId("memory")` returns the ID of the memory plugin to use when initialising the agent.

---

## Built-in Fallback: InMemory

If no memory plugin is configured (or `memoryPluginId == null`), the agent falls back to `InMemory` — a simple in-process `ArrayList<JSONObject>` with a 30-message sliding window, backed by `ChatMessageStore` for persistence.

See [InMemory](../memory-system/in-memory.md) for full details.

---

## Custom Memory Plugin Interface

A custom memory plugin must implement the `Memory` interface from `sharedToolInterface.jar`:

```java
public interface Memory {
    void setSystemPrompt(Messages systemPrompt);
    void addHumanMessage(Messages humanMessage) throws JSONException;
    void addAiMessage(Messages aiMessage) throws JSONException;
    void addAiMessage(JSONObject aiMessage);
    void addToolMessage(Messages toolMessage) throws JSONException;
    List<JSONObject> getAllMessages();
    void clearMemory();
}
```

---

## See Also

- [InMemory](../memory-system/in-memory.md) — default built-in memory implementation
- [DexLoader](./dex-loader.md) — how custom plugin JARs are loaded
- [Default Plugins](../database-layer/entities.md) — `DefaultPlugin` entity
- [Database Layer](../database-layer/database-layer.md) — `MemoryPluginDao`

