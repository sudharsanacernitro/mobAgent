# ChatMessageStore

> [← Back to Database Layer](./database-layer.md) | [← Documentation Root](../database-layer.md)

`ChatMessageStore` is the high-level **repository class** that sits between the AI framework and the raw Room DAO. It implements the `DBMessageStore` interface (from `sharedToolInterface.jar`) so that `InMemory` and other components can persist and load messages without directly depending on Android Room.

---

## Class Definition

```java
package com.example.myapplication.DAOs;

public class ChatMessageStore implements DBMessageStore {

    private final ChatMessageDao dao;
    private int sessionId;

    // Constructor — no session set yet (sessionId = -1)
    public ChatMessageStore(Context context) { ... }

    // Constructor — session known at creation time
    public ChatMessageStore(Context context, int sessionId) { ... }
}
```

---

## Methods

| Method | Description |
|---|---|
| `setSessionId(int)` | Sets the active session. All subsequent calls operate on this session. |
| `getSessionId()` | Returns the current session ID (-1 if not set). |
| `saveHumanMessage(String)` | Inserts a `role='user'` ChatMessage into the DB for the current session. |
| `saveAiMessage(String)` | Inserts a `role='assistant'` ChatMessage into the DB for the current session. |
| `getSessionMessages()` | Returns all messages for the current session as `List<Messages>`. |
| `getAllMessages()` | Returns all messages across all sessions. |
| `getHumanMessages()` | Returns only `role='user'` messages for the current session. |
| `getAiMessages()` | Returns only `role='assistant'` messages for the current session. |
| `clearSession()` | Deletes all messages for the current session. |
| `clearAll()` | Deletes all messages in the database. |

---

## DBMessageStore Interface

`ChatMessageStore` implements this interface from `sharedToolInterface.jar`:

```java
public interface DBMessageStore {
    void setSessionId(int sessionId);
    int  getSessionId();
    void saveHumanMessage(String content);
    void saveAiMessage(String content);
    List<Messages> getSessionMessages();
    List<Messages> getAllMessages();
    List<Messages> getHumanMessages();
    List<Messages> getAiMessages();
    void clearSession();
    void clearAll();
}
```

The interface is defined in the shared JAR so that `InMemory` (in the agent framework module) can use it without depending on Android-specific Room classes.

---

## Type Conversion

`ChatMessage` rows (Room entities) are converted to framework `Messages` objects:

```java
private List<Messages> toMessagesList(List<ChatMessage> rows) {
    List<Messages> result = new ArrayList<>();
    for (ChatMessage row : rows) {
        if ("user".equals(row.role)) {
            result.add(new HumanMessages(row.content));
        } else if ("assistant".equals(row.role)) {
            result.add(new AiMessages(row.content, null));
        }
    }
    return result;
}
```

---

## Usage in InMemory

```java
// At agent initialization
ChatMessageStore store = new ChatMessageStore(context, sessionId);
InMemory memory = new InMemory(store);

// InMemory calls on session switch:
memory.setSessionId(newSessionId);
// → store.setSessionId(newSessionId)
// → store.getSessionMessages() → loads all messages as JSONObjects into the List
```

---

## See Also

- [DAOs](./daos.md) — `ChatMessageDao` that this store delegates to
- [Entities](./entities.md) — `ChatMessage` entity
- [InMemory](../memory-system/in-memory.md) — consumes `DBMessageStore`
- [Chat Sessions](../chat-sessions/message-persistence.md) — how messages are saved during a conversation

