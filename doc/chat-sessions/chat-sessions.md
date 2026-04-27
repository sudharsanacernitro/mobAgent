# Chat Sessions

> [← Back to Documentation Root](../README.md)

**Chat Sessions** give every conversation in MobAgent its own isolated context — each session carries its own model plugin selection, memory plugin selection, and message history. A new session is automatically created on every app launch, and all sessions are persisted in the Room database.

---

## Sub-Features

| File | Description |
|---|---|
| [Session Entity](./session-entity.md) | `ChatSession` Room entity and its foreign key relationships |
| [Session UI](./session-ui.md) | `ChatSessionListActivity` — listing, selecting, and editing sessions |
| [Plugin Selection Dialog](./plugin-selection-dialog.md) | Non-cancelable dialog shown on launch to pick model + memory plugins |
| [Message Persistence](./message-persistence.md) | How messages are saved to and loaded from the DB per session |

---

## Overview

### Lifecycle of a Session

```
App Launches
    │
    ▼
autoCreateSessionAndShowDialog()
    │
    ├─→ Creates a new ChatSession row (session_name = "Chat MMM dd HH:mm")
    │
    └─→ Runs showPluginSelectionDialog()  (non-cancelable)
              │
              ├─ User picks ModelPlugin + MemoryPlugin
              │   └─→ updatePlugins(sessionId, modelId, memoryId)
              │   └─→ initAgent(modelId, memoryId)
              │
              └─ User taps "Go to Settings"
                  └─→ Opens SettingsActivity to add plugins first
```

### Switching Sessions

```
User taps Session Icon (top-left)
    │
    ▼
ChatSessionListActivity (list of all sessions)
    │
    ├─ Tap session → RESULT_OK with EXTRA_SESSION_ID
    │       └─→ MainActivity.loadSessionChat(sessionId)
    │               └─→ Queries ChatMessageDao.getBySessionId()
    │               └─→ Populates RecyclerView with history
    │       └─→ MainActivity.reinitAgentForSession(sessionId)
    │               └─→ Reads session.modelPluginId + session.memoryPluginId
    │               └─→ Calls initAgent(modelId, memoryId)
    │
    └─ Long-press → Edit dialog (change model/memory plugin for that session)
```

---

## Key Classes

| Class | Location |
|---|---|
| `ChatSession` | `DAOs/entities/ChatSession.java` |
| `ChatSessionDao` | `DAOs/ChatSessionDao.java` |
| `ChatMessage` | `DAOs/entities/ChatMessage.java` |
| `ChatMessageDao` | `DAOs/ChatMessageDao.java` |
| `ChatSessionListActivity` | `ui/ChatSessionListActivity.java` |
| `ChatMessageStore` | `DAOs/ChatMessageStore.java` |

---

## Database Schema (Relevant Tables)

```sql
CREATE TABLE chat_sessions (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    session_name    TEXT,
    model_plugin_id INTEGER REFERENCES plugins(id) ON DELETE CASCADE,
    memory_plugin_id INTEGER REFERENCES plugins(id) ON DELETE SET NULL,
    created_at      INTEGER
);

CREATE TABLE chat_messages (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id  INTEGER REFERENCES chat_sessions(id) ON DELETE CASCADE,
    role        TEXT,       -- 'user' or 'assistant'
    content     TEXT,
    created_at  INTEGER
);
```

---

## See Also

- [Memory System](../memory-system/memory-system.md) — InMemory loads messages from DB on `setSessionId()`
- [Database Layer](../database-layer/database-layer.md) — full DB schema
- [UI Layer](../ui-layer/ui-layer.md) — MainActivity and ChatSessionListActivity

