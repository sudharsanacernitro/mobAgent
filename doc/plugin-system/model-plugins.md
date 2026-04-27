# Plugin System — Model Plugins

> [← Back to Plugin System](./plugin-system.md) | [← Documentation Root](../plugin-system.md)

A **Model Plugin** stores the configuration for one specific LLM API endpoint — the base URL, model name, streaming flag, timeout, and a reference to the **Formatter Plugin** that should be used to format requests/parse responses for that API.

---

## Global Plugin ID Scheme

Every plugin type in MobAgent — model, memory, formatter — uses the **same ID** from the parent `Plugin` table. There is no separate auto-generated `id` inside `ModelPlugin`.

```
Plugin table:
  id=5, name="MyLlama", type=1

model_plugin table:
  plugin_id=5   ← same as Plugin.id (PK, no auto-generate)
  model_name="llama3"
  api_url="http://127.0.0.1:8080/v1/chat/completions"
  ...
```

This enforces a strict 1:1 relationship between `Plugin` and `ModelPlugin`.

---

## ModelPlugin Entity

```java
@Entity(
    tableName = "model_plugin",
    foreignKeys = {
        @ForeignKey(entity = Plugin.class, parentColumns = "id",
                    childColumns = "plugin_id", onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = FormatterPlugin.class, parentColumns = "plugin_id",
                    childColumns = "formatter_id", onDelete = ForeignKey.SET_NULL)
    },
    indices = {@Index(value = "formatter_id")}
)
public class ModelPlugin {
    @PrimaryKey
    @ColumnInfo(name = "plugin_id") public int pluginId;

    @ColumnInfo(name = "model_name")   public String modelName;
    @ColumnInfo(name = "api_url")      public String apiUrl;
    @ColumnInfo(name = "is_stream")    public boolean isStream;
    @ColumnInfo(name = "timeout_ms")   public int timeoutMs;
    @ColumnInfo(name = "formatter_id") public Integer formatterId;  // nullable FK
}
```

---

## Fields

| Field | Type | Description |
|---|---|---|
| `pluginId` | `int` (PK) | Global plugin ID — same as the parent `Plugin.id` |
| `modelName` | `String` | Model name string sent in API requests (e.g. `"llama3"`, `"gpt-4"`) |
| `apiUrl` | `String` | Full endpoint URL (e.g. `http://127.0.0.1:8080/v1/chat/completions`) |
| `isStream` | `boolean` | Whether to use streaming responses |
| `timeoutMs` | `int` | HTTP request timeout in milliseconds |
| `formatterId` | `Integer` | FK → `formatter_plugin.plugin_id` — the formatter JAR to load for this model |

---

## Inserting a Model Plugin

Because there is no auto-generated ID, inserting a `ModelPlugin` requires first inserting the parent `Plugin`:

```java
// 1. Insert parent Plugin row
Plugin plugin = new Plugin(pluginName, "1.0", true, PLUGIN_TYPE_MODEL, "");
long pluginId = pluginDao.insert(plugin);  // get the auto-generated ID

// 2. Insert ModelPlugin using that same ID
ModelPlugin mp = new ModelPlugin((int) pluginId, modelName, apiUrl, false, 30000);
modelPluginDao.insert(mp);
```

---

## JOINs

Two Room POJO classes provide enriched views:

### `ModelPluginWithPluginName`
Joins `model_plugin` with `plugins` to display the plugin's human-readable name in lists.

### `ModelPluginWithFormatterPath`
Joins `model_plugin` with `formatter_plugin` and `plugins` to get the formatter JAR path — used in `MainActivity.initAgent()` to load the DEX plugin.

---

## See Also

- [Plugin Entity](../database-layer/entities.md) — parent `Plugin` table
- [Formatter Plugins](./formatter-plugins.md) — the formatter referenced by `formatterId`
- [DexLoader](./dex-loader.md) — loads the formatter JAR at runtime
- [Database Layer](../database-layer/database-layer.md) — `ModelPluginDao`

