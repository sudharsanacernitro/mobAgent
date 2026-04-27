# Entities

> [← Back to Database Layer](./database-layer.md) | [← Back to Docs Root](../database-layer.md)

## Plugin (Base Entity)

```java
@Entity(tableName = "plugins")
public class Plugin {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String name;        // display name
    public String path;        // filesystem path to the .dex/.jar file
    public String type;        // "formatter", "memory", "model"
    public String description;
}
```

All plugin types extend or reference `Plugin`. The `DexLoader` queries `pluginDao().getById(id)` to get the `.path` before loading.

---

## ModelPlugin

```java
@Entity(tableName = "model_plugins")
public class ModelPlugin {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String name;
    public int formatterPluginId;   // FK to plugins.id (where type = "formatter")
    public String modelName;        // e.g., "llama3.2", "mistral"
    public String baseUrl;          // API endpoint
}
```

When the user selects a model plugin, the app:
1. Loads the `FormatterPlugin` via its ID
2. Loads all `ConfigHeader` rows for this `modelPluginId`
3. Builds `OllamaModel` (or loads custom formatter) with the headers and URL

---

## ConfigHeader

```java
@Entity(tableName = "config_headers")
public class ConfigHeader {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int modelPluginId;   // FK → ModelPlugin.id
    public String headerName;   // e.g., "Authorization"
    public String headerValue;  // e.g., "Bearer sk-abc123"
}
```

Used to pass API keys and custom headers to remote LLM endpoints.

---

## FormatterPlugin

```java
@Entity(tableName = "formatter_plugins")
public class FormatterPlugin extends Plugin {
    // inherits id, name, path, description
    // path points to the .dex file containing FormatterBuilderImpl
}
```

---

## MemoryPlugin

```java
@Entity(tableName = "memory_plugins")
public class MemoryPlugin extends Plugin {
    // inherits id, name, path, description
}
```

---

## DefaultPlugin

```java
@Entity(tableName = "default_plugins")
public class DefaultPlugin {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String pluginType;    // "formatter", "memory", "model"
    public int pluginId;         // FK → the selected plugin's id
}
```

Only one `DefaultPlugin` row should exist per `pluginType`. The app queries this to know which plugin to use when building the agent.

---

## ChatSession

```java
@Entity(tableName = "chat_sessions")
public class ChatSession {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String name;        // user-given session name
    public long createdAt;     // Unix timestamp
}
```

---

## ChatMessage

```java
@Entity(tableName = "chat_messages")
public class ChatMessage {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int sessionId;       // FK → ChatSession.id
    public String role;         // "user", "assistant", "tool", "system"
    public String content;      // text content
    public String toolCallsJson;  // JSON string for tool_calls (nullable)
    public long timestamp;
}
```

---

## ModelPluginWithFormatterPath & ModelPluginWithPluginName

These are Room **POJO** classes (not entities) used with `@Relation` or `@Query` joins:

```java
// Joins ModelPlugin + Plugin to get the formatter's .dex file path
public class ModelPluginWithFormatterPath {
    @Embedded ModelPlugin modelPlugin;
    @Relation(parentColumn = "formatterPluginId", entityColumn = "id")
    Plugin formatterPlugin;  // has .path field
}
```

---

## See Also

- [PluginDatabase](./plugin-database.md)
- [DAOs](./daos.md)
- [ChatMessageStore](./chat-message-store.md)

