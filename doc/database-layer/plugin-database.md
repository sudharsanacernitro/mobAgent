# PluginDatabase

> [← Back to Database Layer](./database-layer.md) | [← Documentation Root](../database-layer.md)

`PluginDatabase` is the single **Room database singleton** for the entire application. All persistence — plugins, sessions, messages, headers — flows through this class.

---

## Class Definition

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
public abstract class PluginDatabase extends RoomDatabase {

    private static volatile PluginDatabase INSTANCE;

    // DAOs
    public abstract PluginDao pluginDao();
    public abstract ModelPluginDao modelPluginDao();
    public abstract ConfigHeaderDao configHeaderDao();
    public abstract ChatMessageDao chatMessageDao();
    public abstract MemoryPluginDao memoryPluginDao();
    public abstract FormatterPluginDao formatterDao();
    public abstract DefaultPluginDao defaultPluginDao();
    public abstract ChatSessionDao chatSessionDao();

    public static PluginDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (PluginDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        PluginDatabase.class,
                        "plugin_database"
                    ).fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }
}
```

---

## Configuration

| Property | Value |
|---|---|
| Database name | `plugin_database` |
| Current version | `8` |
| Export schema | `false` |
| Migration strategy | `fallbackToDestructiveMigration()` — schema changes wipe and recreate all tables |
| Singleton pattern | Double-checked locking (thread-safe) |

---

## Version History

| Version | Changes |
|---|---|
| 1 | Initial schema: `Plugin`, `ModelPlugin`, `ConfigHeader` |
| 2–3 | Added `MemoryPlugin`, `FormatterPlugin` |
| 4 | `Plugin` table: added unique index on `name` |
| 5 | `ModelPlugin`: `plugin_id` is now `@PrimaryKey` (no auto-generate) |
| 6 | Added `DefaultPlugin` table |
| 7 | (reserved) |
| 8 | Added `ChatSession` and `ChatMessage` tables; `ChatMessage.session_id` FK to `ChatSession` |

---

## Accessing the Database

All code uses the singleton via:

```java
PluginDatabase db = PluginDatabase.getInstance(context);
db.modelPluginDao().getAll();
db.chatSessionDao().getById(sessionId);
// etc.
```

---

## Important Note on Migration

`fallbackToDestructiveMigration()` means that when the DB version is incremented **without a provided migration**, all data is wiped and the schema is rebuilt from scratch. This is acceptable during development but should be replaced with explicit migrations before a production release.

---

## See Also

- [Entities](./entities.md) — all entity classes registered in this database
- [DAOs](./daos.md) — data access interfaces exposed by this class
- [Chat Sessions](../chat-sessions/chat-sessions.md) — uses `chatSessionDao()`
- [Memory System](../memory-system/memory-system.md) — uses `chatMessageDao()` via `ChatMessageStore`

