# Plugin System

> [← Back to Documentation Root](../README.md)

The **Plugin System** gives MobAgent its extensibility superpower. Rather than baking LLM formatters, memory strategies, and model implementations into the APK, MobAgent supports loading custom implementations at runtime from `.dex` (compiled Java/Kotlin) files. This means anyone can write a new formatter, memory backend, or model adapter and install it into the running app without rebuilding.

---

## Sub-Features

| File | Description |
|---|---|
| [DexLoader](./dex-loader.md) | The Android DEX class loading mechanism |
| [Formatter Plugins](./formatter-plugins.md) | Custom LLM request/response formatters |
| [Memory Plugins](./memory-plugins.md) | Custom conversation memory implementations |
| [Model Plugins](./model-plugins.md) | Full custom model implementations |
| [Plugin Database](./plugin-database.md) | How plugins are stored and managed in Room DB |

---

## Plugin Architecture

```
Plugin .jar/.dex file (user-provided)
    └── org.mobAgent.FormatterBuilderImpl
            └── implements org.mobAgent.plugin.interfaces.FormatterBuilder
                    └── build() → returns FormatterInterface

                                          ┌──────────────────────────┐
                                          │  sharedToolInterface.jar  │
                                          │  (shipped with APK)       │
                                          │                           │
                                          │  Interfaces:              │
                                          │  - FormatterInterface     │
                                          │  - FormatterBuilder       │
                                          │  - Memory                 │
                                          │  - Tool                   │
                                          │  - DBMessageStore         │
                                          │  - Messages               │
                                          └──────────────────────────┘
```

### Why DEX Loading?

Android uses the Dalvik/ART bytecode format (`.dex`) rather than standard JVM bytecode (`.class`). Plugins are distributed as `.jar` files containing `.dex` bytecode (not standard Java `.class` files). `DexClassLoader` handles loading these at runtime.

### Shared Interface JAR

`app/libs/sharedToolInterface.jar` contains **only interfaces** — no implementations. Plugin authors compile their plugins **against this JAR**, ensuring that the app and the plugin share the same interface types at the class loader level. This is essential because:

```
App class loader           Plugin class loader
     │                             │
FormatterInterface ←──────── FormatterBuilderImpl.build()
(from SharedJAR)              (returns FormatterInterface)
```

Both must reference the **same** `FormatterInterface` class. Since the plugin's class loader uses the **app's class loader as parent**, and the interfaces are in the app's classpath, this works correctly.

---

## Plugin Types

| Plugin Type | Interface | Builder Class | Use Case |
|---|---|---|---|
| **Formatter** | `FormatterInterface` | `FormatterBuilder` | Custom LLM API format (Claude, Gemini, etc.) |
| **Memory** | `Memory` | (direct) | Vector DB, summarization, persistent memory |
| **Model** | `FormatterInterface` | `FormatterBuilder` | Complete model implementation |

---

## Plugin Lifecycle

```
1. User uploads .dex plugin file via UI (SettingsActivity → FormatterPluginActivity)

2. Plugin metadata saved to Room DB:
   Plugin { id, name, path, type, description }
   FormatterPlugin / MemoryPlugin / ModelPlugin (extends Plugin)

3. At agent startup (MainActivity):
   DexLoader.loadFormatter(formatterPluginId)
       └── DexClassLoader loads the .dex
       └── Instantiates "org.mobAgent.FormatterBuilderImpl"
       └── Calls .build(context, config) → FormatterInterface

4. FormatterInterface used in ModelInterface.builder().setModel(...)
```

---

## DefaultPlugin

`DefaultPlugin` is a Room DB entity that records which plugin is currently selected as the default for each plugin type. When building an agent, the app queries `DefaultPluginDao` to find the active formatter, memory, and model plugins.

---

## See Also

- [Models Layer](../models-layer/models-layer.md) — FormatterInterface used by agents
- [Memory System](../memory-system/memory-system.md) — Memory interface used by InMemory and plugins
- [Database Layer](../database-layer/database-layer.md) — Plugin entities and DAOs
- [UI Layer](../ui-layer/ui-layer.md) — SettingsActivity plugin management screens

