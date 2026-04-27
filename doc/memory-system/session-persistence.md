# Session Persistence

> [← Back to Memory System](./memory-system.md) | [← Back to Docs Root](../memory-system.md)

## Overview

MobAgent supports **named chat sessions** — each conversation is saved to a Room database and can be resumed later. The session system allows users to maintain multiple parallel conversations with the AI agent.

---

## Data Model

### ChatSession

```java
@Entity(tableName = "chat_sessions")
public class ChatSession {
    @PrimaryKey(autoGenerate = true)
    int id;
    String name;           // session display name (e.g., "Research Session 1")
    long createdAt;        // timestamp
}
```

### ChatMessage

```java
@Entity(tableName = "chat_messages")
public class ChatMessage {
    @PrimaryKey(autoGenerate = true)
    int id;
    int sessionId;         // foreign key to ChatSession
    String role;           // "user", "assistant", "tool", "system"
    String content;        // message text content
    String toolCallsJson;  // serialized tool call data (for assistant messages with function calls)
    long timestamp;
}
```

---

## ChatMessageStore

`ChatMessageStore` (`com.example.myapplication.DAOs`) implements the `DBMessageStore` interface and wraps the Room DAOs. It provides:

- **`setSessionId(int id)`** — switches the active session
- **`getSessionMessages()`** — returns all messages for the current session as `List<Messages>`
- **`saveMessage(Messages msg)`** — persists a new message to the database

Used by `InMemory` to load history on session switch and by the UI layer to persist new messages.

---

## Session Lifecycle

```
1. User opens ChatSessionListActivity
         │
2. Selects or creates a session
         │
3. SessionPickerLauncher returns sessionId to MainActivity
         │
4. MainActivity.setCurrentSession(sessionId)
         │
         ├── memory.setSessionId(sessionId)
         │       └── InMemory reloads all messages from DB
         │
         └── UI adapter loads messages for display
```

---

## DAOs Involved

| DAO | Purpose |
|---|---|
| `ChatSessionDao` | Create, list, delete chat sessions |
| `ChatMessageDao` | Insert, query messages by session ID |
| `ChatMessageStore` | High-level wrapper implementing `DBMessageStore` |

---

## See Also

- [InMemory](./in-memory.md) — uses ChatMessageStore for session loading
- [Database Layer](../database-layer/database-layer.md) — Room DB setup and entity definitions
- [UI Layer](../ui-layer/ui-layer.md) — ChatSessionListActivity

