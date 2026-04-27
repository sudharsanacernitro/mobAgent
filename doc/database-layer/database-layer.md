# Database Layer

> [← Back to Documentation Root](../README.md)

The **Database Layer** uses **Android Room** to persist application state including plugin configurations, chat sessions, chat messages, and model settings. Room provides type-safe, compile-time verified SQL access through DAOs (Data Access Objects).

---

## Sub-Features

| File | Description |
|---|---|
| [PluginDatabase](./plugin-database.md) | The Room database singleton with all DAOs |
| [Entities](./entities.md) | All entity (table) definitions |
| [DAOs](./daos.md) | Data Access Object interfaces for each entity |
| [ChatMessageStore](./chat-message-store.md) | High-level wrapper implementing DBMessageStore |

---

## Overview

### Database Configuration

```java
@Database(
    entities = {
        Plugin.class,
        ModelPlugin.class,
        ConfigHeader.class,
        ChatMessage.class,
        ChatSession.class,
        MemoryPlugin.class,
        FormatterPlugin.class,
        DefaultPlugin.class
    },
    version = 8,
    exportSchema = false
)
public abstract class PluginDatabase extends RoomDatabase { ... }
```

- **Database name:** `plugin_database`
- **Version:** 8 (with `fallbackToDestructiveMigration` — schema changes wipe data)
- **Singleton** pattern with double-checked locking

---

## Entity Relationship Diagram

```
Plugin (base)
    ├── ModelPlugin   (references FormatterPlugin via formatterPluginId)
    ├── FormatterPlugin
    ├── MemoryPlugin
    └── DefaultPlugin (records the default for each plugin type)

ConfigHeader   → FK: modelPluginId → ModelPlugin

ChatSession    (parent of conversation)
    └── ChatMessage → FK: sessionId → ChatSession

ModelPluginWithFormatterPath   (Room @Relation joining ModelPlugin + Plugin path)
ModelPluginWithPluginName      (Room @Relation joining ModelPlugin + Plugin name)
```

---

## Tables Summary

| Table | Purpose |
|---|---|
| `plugins` | Base plugin registry (name, path, type) |
| `model_plugins` | Model configurations (base URL, model name, formatter ID) |
| `formatter_plugins` | Formatter plugin metadata |
| `memory_plugins` | Memory plugin metadata |
| `default_plugins` | Which plugin is the current default per type |
| `config_headers` | HTTP headers per model plugin |
| `chat_sessions` | Named conversation sessions |
| `chat_messages` | Individual messages with role, content, session FK |

---

## See Also

- [Memory System](../memory-system/memory-system.md) — uses ChatMessageStore for persistence
- [Plugin System](../plugin-system/plugin-system.md) — all plugin entities stored here
- [UI Layer](../ui-layer/ui-layer.md) — all screens query this database

