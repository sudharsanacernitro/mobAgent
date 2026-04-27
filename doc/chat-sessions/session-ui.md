# Chat Session UI

> [← Back to Chat Sessions](./chat-sessions.md) | [← Documentation Root](../chat-sessions.md)

`ChatSessionListActivity` provides the full session management screen. It is launched when the user taps the **session icon** (top-left button) in `MainActivity`.

---

## How to Open

```java
// In MainActivity
btnSessions.setOnClickListener(v ->
    sessionPickerLauncher.launch(new Intent(this, ChatSessionListActivity.class)));
```

The result is processed by `sessionPickerLauncher` (an `ActivityResultLauncher`):

```java
sessionPickerLauncher = registerForActivityResult(
    new ActivityResultContracts.StartActivityForResult(),
    result -> {
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            int sessionId = result.getData().getIntExtra(EXTRA_SESSION_ID, -1);
            if (sessionId != -1) {
                currentSessionId = sessionId;
                loadSessionChat(sessionId);         // restore message history in RecyclerView
                reinitAgentForSession(sessionId);   // re-build ModelInterface with session's plugins
            }
        }
    }
);
```

---

## Session List Features

| Gesture | Action |
|---|---|
| **Tap** | Select session → returns `RESULT_OK` with session ID to `MainActivity` |
| **Long-press** | Opens edit dialog — allows changing `modelPlugin` and `memoryPlugin` for that session |
| **Delete button** | Deletes session and all its messages (CASCADE in DB) |
| **Add (＋) button** | Creates a new session via an "add session" dialog with plugin selection |

---

## Loading Message History

When a session is selected, `MainActivity.loadSessionChat()` runs:

```java
private void loadSessionChat(int sessionId) {
    Executors.newSingleThreadExecutor().execute(() -> {
        List<ChatMessage> messages = db.chatMessageDao().getBySessionId(sessionId);
        runOnUiThread(() -> {
            chatMessages.clear();
            for (ChatMessage msg : messages) {
                String sender = "user".equals(msg.role) ? "You" : "AI";
                chatMessages.add(new ChatMessageAdapter.ChatMsg(sender, msg.content));
            }
            chatAdapter.notifyDataSetChanged();
        });
    });
}
```

The adapter then populates the RecyclerView with the full conversation history for that session.

---

## Reinitialising the Agent

After switching sessions, the agent must be rebuilt with the new session's plugins:

```java
private void reinitAgentForSession(int sessionId) {
    ChatSession session = db.chatSessionDao().getById(sessionId);
    if (session == null || session.getModelPluginId() == 0) return;
    initAgent(session.getModelPluginId(), session.getMemoryPluginId());
}
```

`initAgent()` then:
1. Creates a `ChatMessageStore` targeting the new `sessionId`
2. Creates an `InMemory` wrapping that store
3. Loads the model plugin and its formatter via `DexLoader`
4. Builds a new `ModelInterface` with model + memory + tools

---

## Session Name Auto-Generation

When the app launches and auto-creates a session:

```java
String sessionName = "Chat " + new SimpleDateFormat("MMM dd HH:mm",
        Locale.getDefault()).format(new Date());
// e.g. "Chat Apr 27 14:30"
```

---

## See Also

- [Session Entity](./session-entity.md) — DB schema for sessions
- [Plugin Selection Dialog](./plugin-selection-dialog.md) — how plugins are chosen
- [Message Persistence](./message-persistence.md) — how messages are stored and loaded

