# UI Adapters

> [← Back to UI Layer](./ui-layer.md) | [← Documentation Root](../ui-layer.md)

MobAgent uses several `RecyclerView.Adapter` implementations to render lists throughout the app. Each adapter is defined in `com.example.myapplication.ui.adapter`.

---

## Adapter Overview

| Adapter Class | Used In | Data Type |
|---|---|---|
| `ChatMessageAdapter` | `MainActivity` | Chat messages (user + AI) |
| `ModelPluginListAdapter` | `ModelPluginListActivity` | `ModelPluginWithPluginName` |
| `ConfigHeaderListAdapter` | `ModelPluginDetailActivity` | `ConfigHeader` (HTTP headers) |
| `SkillsListAdapter` | `SkillsActivity` | `Skill` objects |
| `ToolsListAdapter` | `SkillsActivity` / tool views | `Tool` objects |
| `PublicToolsListAdapter` | `PublicToolsActivity` | Public `Tool` names |
| `ModelsListAdapter` | `LocalLLMActivity` | Model names (`.gguf` files) |

---

## ChatMessageAdapter

The primary adapter for the chat interface in `MainActivity`.

```java
public class ChatMessageAdapter 
        extends RecyclerView.Adapter<ChatMessageAdapter.ViewHolder> {

    public static class ChatMsg {
        public final String sender;   // "You" or "AI"
        public final String content;
        public ChatMsg(String sender, String content) { ... }
    }
}
```

### Layout
- Uses two different view types: `VIEW_TYPE_USER` (right-aligned) and `VIEW_TYPE_AI` (left-aligned)
- Styling uses Material 3 card components for bubble UI

### Usage in MainActivity
```java
List<ChatMessageAdapter.ChatMsg> chatMessages = new ArrayList<>();
ChatMessageAdapter chatAdapter = new ChatMessageAdapter(chatMessages);
recyclerChat.setLayoutManager(new LinearLayoutManager(this));
recyclerChat.setAdapter(chatAdapter);

// Adding a message
chatAdapter.addMessage(new ChatMessageAdapter.ChatMsg("You", inputText));
chatAdapter.addMessage(new ChatMessageAdapter.ChatMsg("AI", response));

// Scroll to latest
recyclerChat.scrollToPosition(chatMessages.size() - 1);
```

---

## ModelPluginListAdapter

Displays the list of model plugins in `ModelPluginListActivity`.

```java
// Binds ModelPluginWithPluginName (join of ModelPlugin + Plugin)
// Displays: pluginName (from Plugin table) + modelName + apiUrl
```

Shows `pluginName` (from `Plugin.name`) rather than `modelName` (from `ModelPlugin.modelName`) as the primary label — since different APIs can serve the same model.

---

## ConfigHeaderListAdapter

Lists HTTP headers (`ConfigHeader`) for a model plugin in `ModelPluginDetailActivity`.

```java
// ConfigHeader entity:
// - id (auto)
// - model_plugin_id (FK)
// - key   (e.g. "Authorization")
// - value (e.g. "Bearer sk-...")
```

---

## SkillsListAdapter

Renders skill names and descriptions from `ToolsManager.skillsRegistry`. Used in `SkillsActivity` to show which skills are loaded.

---

## ToolsListAdapter / PublicToolsListAdapter

- `ToolsListAdapter` — shows all tools registered under a skill (private tools)
- `PublicToolsListAdapter` — shows the public tools that a skill can call from the root registry

---

## See Also

- [Main Activity](./main-activity.md) — uses `ChatMessageAdapter`
- [Settings Screens](./settings-screens.md) — uses plugin list adapters
- [Skills System](../skills-system/skills-system.md) — data source for `SkillsListAdapter`

