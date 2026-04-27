# InMemory

> [← Back to Memory System](./memory-system.md) | [← Back to Docs Root](../memory-system.md)

## Purpose

`InMemory` (`org.mobchain.memory`) is the **default memory implementation** in MobAgent. It stores the conversation history in a Java `ArrayList<JSONObject>` in the process heap, while delegating persistence to a `DBMessageStore` (Room DB) for session resumption.

---

## Class Structure

```java
public class InMemory implements Memory {

    List<JSONObject> memory;         // in-RAM message store
    int MEMORY_SIZE = 30;            // sliding window size
    int sessionId = 0;               // current session
    DBMessageStore dbMessageStore;   // Room DB persistence

    public InMemory(DBMessageStore dbMessageStore) {
        this.memory = new ArrayList<>();
        this.dbMessageStore = dbMessageStore;
    }
}
```

---

## Methods

### `setSessionId(int sessionId)`

Switches to a different chat session. Clears the in-memory list, updates the DB message store's session, then **reloads all previous messages** from the database:

```java
public void setSessionId(int sessionId) {
    this.sessionId = sessionId;
    memory.clear();
    dbMessageStore.setSessionId(sessionId);

    List<Messages> msgList = dbMessageStore.getSessionMessages();
    for (Messages msg : msgList) {
        memory.add(msg.toJson());
    }
}
```

This enables session resumption — when a user returns to an existing chat, all prior messages are loaded back into the context window.

### `setSystemPrompt(Messages systemPrompt)`

Inserts the system prompt at index 0, or replaces it if one already exists:

```java
if (!memory.isEmpty() && memory.get(0).optString("role").equals("system")) {
    memory.set(0, systemPrompt.toJson());   // replace
} else {
    memory.add(0, systemPrompt.toJson());   // insert at front
}
```

### `addHumanMessage(Messages humanMessage)`

Appends the user's message to the list. Silently swallows exceptions (non-critical).

### `addAiMessage(Messages aiMessage)` / `addAiMessage(JSONObject aiMessage)`

Appends the AI's response and then **trims** the window if it exceeds `MEMORY_SIZE`:

```java
memory.add(aiMessage.toJson());
if (memory.size() > MEMORY_SIZE && memory.size() > 1) {
    memory.remove(1);   // evict oldest non-system message
}
```

### `addToolMessage(Messages toolMessage)`

Appends the tool execution result. No window trimming applied here (tool messages are short-lived context needed for the current agent loop iteration).

### `getAllMessages()`

Returns the full message list. Called by `RequestGeneratorHandler` to build the LLM request payload.

---

## Sliding Window Eviction Strategy

When memory exceeds 30 messages:
- Index 0 (system prompt) is **always preserved**
- Index 1 (oldest human/AI/tool message) is removed
- This is a **FIFO eviction** for the conversation window

```
Before eviction (31 messages):
[system, human_1, ai_1, tool_1, ai_2, human_2, ai_3, ..., human_15, ai_15]
  idx=0    idx=1

After eviction (30 messages):
[system, ai_1, tool_1, ai_2, human_2, ai_3, ..., human_15, ai_15]
  idx=0   idx=1 (human_1 removed)
```

---

## DBMessageStore Integration

`DBMessageStore` (`org.mobAgent.plugin.interfaces`) is an interface abstraction over the Room database. The concrete implementation is `ChatMessageStore` which wraps the Room `ChatMessageDao`. `InMemory` only uses it for:

1. **Loading history** on `setSessionId()`
2. The interface does **not** auto-persist new messages — persistence must be handled separately (e.g., in the UI adapter or repo layer)

---

## See Also

- [Session Persistence](./session-persistence.md)
- [MemoryPluginRegistry](./memory-plugin-registry.md)
- [Database Layer](../database-layer/database-layer.md)

