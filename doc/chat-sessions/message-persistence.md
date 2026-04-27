# Message Persistence

> [← Back to Chat Sessions](./chat-sessions.md) | [← Documentation Root](../chat-sessions.md)

Every message sent and received in a chat session is persisted to the Room database. This ensures that switching between sessions (or restarting the app) correctly restores the full conversation history.

---

## ChatMessage Entity

```java
@Entity(
    tableName = "chat_messages",
    foreignKeys = @ForeignKey(
        entity = ChatSession.class,
        parentColumns = "id",
        childColumns = "session_id",
        onDelete = ForeignKey.CASCADE   // messages deleted when session is deleted
    ),
    indices = {@Index(value = "session_id")}
)
public class ChatMessage {
    @PrimaryKey(autoGenerate = true) public int id;
    @ColumnInfo(name = "session_id")  public int sessionId;
    @ColumnInfo(name = "role")        public String role;       // "user" | "assistant"
    @ColumnInfo(name = "content")     public String content;
    @ColumnInfo(name = "created_at")  public long createdAt;
}
```

---

## Saving Messages

### Human message — saved immediately when user taps Send:

```java
// In MainActivity.sendMsg click handler (before agent.chat() is called)
if (currentSessionId != -1) {
    Executors.newSingleThreadExecutor().execute(() -> {
        ChatMessageStore store = new ChatMessageStore(MainActivity.this, currentSessionId);
        store.saveHumanMessage(inputToLlm);
    });
}
```

### AI message — saved after `agent.chat()` returns:

```java
String output = agent.chat(new HumanMessages(inputToLlm));

if (currentSessionId != -1) {
    ChatMessageStore store = new ChatMessageStore(MainActivity.this, currentSessionId);
    store.saveAiMessage(output);
}
```

Both `saveHumanMessage` and `saveAiMessage` create a `ChatMessage` with the appropriate `role` and call `ChatMessageDao.insert()`.

---

## Loading Messages

When a session is activated (switched to), `loadSessionChat(sessionId)` queries the DB:

```java
List<ChatMessage> messages = db.chatMessageDao().getBySessionId(sessionId);
```

These are then converted to `ChatMessageAdapter.ChatMsg` items and displayed in the RecyclerView.

---

## DBMessageStore Interface

`ChatMessageStore` implements the `DBMessageStore` interface (from `sharedToolInterface.jar`), which is passed to `InMemory` so it can:
- Load existing messages for a session at startup
- Expose typed `Messages` objects (`HumanMessages`, `AiMessages`) to the memory system

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

---

## Message Flow Diagram

```
User types message
      │
      ▼
ChatMessageStore.saveHumanMessage()
      │            └─→ INSERT INTO chat_messages (session_id, role='user', content, ts)
      ▼
agent.chat(HumanMessages)
      │   └─→ InMemory.addHumanMessage()  (in-process list only, not re-persisted)
      │   └─→ LlmHandler → ... → returns content
      ▼
ChatMessageStore.saveAiMessage()
      │            └─→ INSERT INTO chat_messages (session_id, role='assistant', content, ts)
      ▼
chatAdapter.addMessage("AI", output)
```

---

## See Also

- [InMemory](../memory-system/in-memory.md) — uses `DBMessageStore` to load session history
- [Session Entity](./session-entity.md) — `ChatSession` entity that `ChatMessage.session_id` references
- [Database Layer](../database-layer/database-layer.md) — `ChatMessageDao` definition

