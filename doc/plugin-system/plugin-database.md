# Plugin System — Plugin Database

> [← Back to Plugin System](./plugin-system.md) | [← Documentation Root](../plugin-system.md)

This page covers the database aspects of the plugin system — how plugins are stored, the global ID scheme, and the `DefaultPlugin` table.

See also the full [PluginDatabase documentation](../database-layer/plugin-database.md) for the Room database class itself.

---

## Plugin-Related Tables

| Table | Purpose |
|---|---|
| `plugins` | Base registry — every plugin has a row here (name, type, path) |
| `model_plugins` | Model API configuration for type=1 plugins |
| `formatter_plugins` | Formatter DEX plugin metadata for type=3 plugins |
| `memory_plugins` | Memory plugin metadata for type=2 plugins |
| `default_plugins` | Which plugin is the current default per type string |
| `config_headers` | HTTP headers (key-value) for a model plugin |

---

## Global Plugin ID Scheme

All plugin sub-tables use the **same `plugin_id`** as the parent `plugins.id`. There is no separate auto-generated ID inside `ModelPlugin`, `MemoryPlugin`, or `FormatterPlugin`.

```sql
-- Insert base plugin
INSERT INTO plugins (name, version, enabled, type, Path) VALUES ('MyModel', '1.0', 1, 1, '');
-- Returns id = 42

-- Insert model details using the same id
INSERT INTO model_plugin (plugin_id, model_name, api_url, is_stream, timeout_ms)
VALUES (42, 'llama3', 'http://127.0.0.1:8080/v1/chat/completions', 0, 30000);
```

This ensures a strict 1:1 relationship and a single source of truth for plugin identity.

---

## Plugin Types

| Type integer | Meaning |
|---|---|
| `1` | Model Plugin |
| `2` | Memory Plugin |
| `3` | Formatter Plugin |

---

## DefaultPlugin Table

`DefaultPlugin` stores exactly one row per plugin type, indicating which plugin ID is the currently selected default:

```java
@Entity(tableName = "default_plugins")
public class DefaultPlugin {
    @PrimaryKey
    private String plugin_type;  // "model" or "memory"
    private int plugin_id;       // FK to plugins.id
}
```

This is used in agent initialisation:

```java
int defaultModelId = db.defaultPluginDao().getDefaultPluginId("model");
int defaultMemoryId = db.defaultPluginDao().getDefaultPluginId("memory");
```

---

## Unique Plugin Names

The `plugins` table enforces a unique index on `name`:

```java
@Entity(tableName = "plugins", indices = {@Index(value = "name", unique = true)})
```

Attempting to insert two plugins with the same name will throw `SQLiteConstraintException`.

---

## See Also

- [DexLoader](./dex-loader.md) — loads the plugin JAR using the path stored here
- [Model Plugins](./model-plugins.md) — `ModelPlugin` entity details
- [Formatter Plugins](./formatter-plugins.md) — `FormatterPlugin` entity details
- [Database Layer](../database-layer/database-layer.md) — full database documentation

