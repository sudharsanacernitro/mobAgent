# ChatSession Entity

> [← Back to Chat Sessions](./chat-sessions.md) | [← Documentation Root](../chat-sessions.md)

`ChatSession` is the Room entity that represents a single named conversation. Each session holds references to the model plugin and (optionally) the memory plugin used for that conversation.

---

## Entity Definition

```java
@Entity(
    tableName = "chat_sessions",
    foreignKeys = {
        @ForeignKey(
            entity = Plugin.class,
            parentColumns = "id",
            childColumns = "model_plugin_id",
            onDelete = ForeignKey.CASCADE          // delete session if model plugin deleted
        ),
        @ForeignKey(
            entity = Plugin.class,
            parentColumns = "id",
            childColumns = "memory_plugin_id",
            onDelete = ForeignKey.SET_NULL         // keep session, just null out memory
        )
    },
    indices = {
        @Index(value = "model_plugin_id"),
        @Index(value = "memory_plugin_id")
    }
)
public class ChatSession {
    @PrimaryKey(autoGenerate = true) public int id;
    @ColumnInfo(name = "session_name")    public String sessionName;
    @ColumnInfo(name = "model_plugin_id") public int modelPluginId;
    @ColumnInfo(name = "memory_plugin_id") public Integer memoryPluginId;  // nullable
    @ColumnInfo(name = "created_at")      public long createdAt;
}
```

---

## Fields

| Field | Type | Description |
|---|---|---|
| `id` | `int` (auto) | Primary key |
| `sessionName` | `String` | Human-readable label, e.g. `"Chat Apr 27 14:30"` |
| `modelPluginId` | `int` | FK → `plugins.id` — the model plugin for this session |
| `memoryPluginId` | `Integer` (nullable) | FK → `plugins.id` — optional memory plugin; `null` = use default `InMemory` |
| `createdAt` | `long` | Unix timestamp (milliseconds) |

---

## Foreign Key Behaviour

| Reference | On Delete |
|---|---|
| `model_plugin_id → plugins.id` | **CASCADE** — removes session when model plugin is deleted |
| `memory_plugin_id → plugins.id` | **SET NULL** — session survives, memory reverts to in-memory |

---

## ChatSessionDao

```java
@Dao
public interface ChatSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ChatSession session);

    @Query("SELECT * FROM chat_sessions ORDER BY created_at DESC")
    List<ChatSession> getAll();

    @Query("SELECT * FROM chat_sessions WHERE id = :id")
    ChatSession getById(int id);

    @Query("UPDATE chat_sessions SET model_plugin_id=:modelId, memory_plugin_id=:memoryId WHERE id=:sessionId")
    void updatePlugins(int sessionId, int modelId, Integer memoryId);

    @Query("DELETE FROM chat_sessions WHERE id = :id")
    void deleteById(int id);
}
```

---

## See Also

- [Message Persistence](./message-persistence.md) — `ChatMessage` FK to this entity
- [Session UI](./session-ui.md) — how sessions are listed and selected
- [Database Layer](../database-layer/database-layer.md) — full schema

