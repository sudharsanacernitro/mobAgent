# MainActivity

> [← Back to UI Layer](./ui-layer.md) | [← Back to Docs Root](../ui-layer.md)

## Purpose

`MainActivity` (`com.example.myapplication`) is the **app's primary screen** and orchestration hub. It hosts the chat interface, initializes the entire AI framework (Alpine environment, terminal sessions, tools, skills, model), and manages the agent interaction loop.

---

## Key Responsibilities

| Responsibility | Details |
|---|---|
| **Chat UI** | RecyclerView with `ChatMessageAdapter` |
| **Agent initialization** | Creates `ModelInterface` with model, memory, and tools |
| **Session management** | Maintains `currentSessionId`, switches sessions via `ChatSessionListActivity` |
| **App startup** | Bootstraps terminal sessions, scans tools/skills, waits for LLM server |
| **Static context** | Provides `getAppContext()` for non-Activity classes that need `Context` |

---

## Initialization Sequence

```
onCreate()
    │
    ├── 1. Initialize TerminalSessionManager (all 6 sessions)
    │
    ├── 2. Start Alpine environment (proot boot)
    │         └── countDownLatch wait for Alpine to be ready
    │
    ├── 3. ToolsScanner("root/tools").scanAndRegister()
    │
    ├── 4. SkillsScanner("root/skills").scanAndRegister()
    │
    ├── 5. Register SpawnAgentTool (native tool)
    │
    ├── 6. Start llama-server (LlamaCppServerRepo.startLlama)
    │         └── CountDownLatch serverReady.await()
    │
    ├── 7. Load formatter plugin (DexLoader.loadFormatter)
    │
    ├── 8. Build ModelInterface:
    │         ModelInterface.builder()
    │             .setModel(ollamaModel)
    │             .setMemory(inMemory)
    │             .addTools(ToolsManager.getToolsArray())
    │             .build()
    │
    └── 9. Set system prompt with skills overview
              memory.setSystemPrompt(
                  new SystemMessages(ToolsManager.getSystemPromptForSkills())
              )
```

---

## Chat Flow

```
User types message → clicks Send
    │
    ▼
sendMsg.setOnClickListener
    │
    ├── chatMessages.add(ChatMsg("user", userInput))
    ├── chatAdapter.notifyDataSetChanged()
    │
    └── Executors.newSingleThreadExecutor().execute(() -> {
                String response = agent.chat(new HumanMessages(userInput));
                runOnUiThread(() -> {
                    chatMessages.add(ChatMsg("assistant", response));
                    chatAdapter.notifyDataSetChanged();
                });
            });
```

The `agent.chat()` call runs on a **background thread** to avoid blocking the UI thread during potentially long LLM inference.

---

## Server Status Check

The `checkServerStatus` button triggers:

```java
checkServerStatus.setOnClickListener(v -> {
    boolean online = LlamaCppServerRepo.isServerOnline(port);
    Toast.makeText(this, online ? "Server Online" : "Server Offline", Toast.LENGTH_SHORT).show();
});
```

---

## `getAppContext()`

```java
private static Context context;

public static Context getAppContext() {
    return MainActivity.context;
}
```

This static method allows non-Activity classes (like `SpawnAgentTool`, `ChatMessageStore`) to access the application context. `context` is set in `onCreate()` as `context = getApplicationContext()`.

---

## Session Picker

```java
sessionPickerLauncher = registerForActivityResult(
    new ActivityResultContracts.StartActivityForResult(),
    result -> {
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            int sessionId = result.getData().getIntExtra("sessionId", -1);
            if (sessionId != -1) {
                currentSessionId = sessionId;
                memory.setSessionId(sessionId);
                loadSessionMessages(sessionId);
            }
        }
    }
);
```

When the user switches sessions, `memory.setSessionId()` reloads the conversation from Room DB and `loadSessionMessages()` updates the RecyclerView.

---

## See Also

- [Chat Session Management](../chat-sessions/chat-sessions.md)
- [Agent Loop Engine](../agent-loop-engine/agent-loop-engine.md)
- [Terminal Infrastructure](../terminal-infrastructure/terminal-infrastructure.md)
- [Plugin System](../plugin-system/plugin-system.md)

