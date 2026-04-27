# UI Layer

> [← Back to Documentation Root](../README.md)

The **UI Layer** provides all the user-facing screens in MobAgent. The app follows the Android `AppCompatActivity` pattern with `RecyclerView` adapters for lists and `Intent`-based navigation between screens.

---

## Sub-Features

| File | Description |
|---|---|
| [MainActivity](./main-activity.md) | The main chat screen and app entry point |
| [Settings & Configuration](./settings-screens.md) | All settings screens (model, plugins, skills, tools, LlamaCpp) |
| [Chat Session Management](../chat-sessions/chat-sessions.md) | Creating and switching between named chat sessions |
| [Adapters](./adapters.md) | RecyclerView adapters for all list views |

---

## Screen Map

```
MainActivity (Chat Screen)
    ├── SettingsActivity
    │   ├── ModelPluginListActivity
    │   │   └── ModelPluginDetailActivity  (create/edit model plugin)
    │   ├── SkillsActivity               (upload/manage skills)
    │   ├── PublicToolsActivity          (upload/manage root tools)
    │   ├── FormatterPluginActivity      (upload/manage formatter plugins)
    │   ├── MemoryPluginActivity         (upload/manage memory plugins)
    │   ├── LlamaCppActivity             (configure llama-server)
    │   ├── LocalLLMActivity             (manage .gguf model files)
    │   └── TerminalActivity             (direct Alpine terminal access)
    │
    └── ChatSessionListActivity          (switch between sessions)
```

---

## Navigation

Navigation between activities is handled via Android `Intent`:

```java
// From MainActivity
startActivity(new Intent(this, SettingsActivity.class));

// From SettingsActivity
setupMenuItem(view, "Skills", "...", SkillsActivity.class);
```

`ChatSessionListActivity` uses an `ActivityResultLauncher` to return the selected `sessionId` back to `MainActivity`:

```java
sessionPickerLauncher = registerForActivityResult(
    new ActivityResultContracts.StartActivityForResult(),
    result -> {
        if (result.getResultCode() == RESULT_OK) {
            int sessionId = result.getData().getIntExtra("sessionId", -1);
            setCurrentSession(sessionId);
        }
    }
);
```

---

## Key UI Components

### RecyclerViews

Every list in the app uses `RecyclerView` with a dedicated adapter:

| Screen | Adapter | Data |
|---|---|---|
| Main chat | `ChatMessageAdapter` | `ChatMsg` (role + content) |
| Model plugins | `ModelPluginListAdapter` | `ModelPlugin` entities |
| Available models | `ModelsListAdapter` | Model name strings |
| Skills | `SkillsListAdapter` | `JSONObject` skill configs |
| Root tools | `ToolsListAdapter` | Tool files/configs |
| Public tools | `PublicToolsListAdapter` | Public tool entries |
| Config headers | `ConfigHeaderListAdapter` | `ConfigHeader` entities |

### ChatMessageAdapter

`ChatMessageAdapter` renders the chat conversation. It differentiates message types:
- User messages (right-aligned bubble)
- AI messages (left-aligned bubble, with optional Markdown rendering)
- System/tool messages (smaller, dimmed style)

---

## See Also

- [Database Layer](../database-layer/database-layer.md) — data displayed in UI
- [Plugin System](../plugin-system/plugin-system.md) — plugin management screens
- [Terminal Infrastructure](../terminal-infrastructure/terminal-infrastructure.md) — TerminalActivity

