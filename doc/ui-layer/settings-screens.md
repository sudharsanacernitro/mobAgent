# Settings & Configuration Screens

> [← Back to UI Layer](./ui-layer.md) | [← Back to Docs Root](../ui-layer.md)

## SettingsActivity

`SettingsActivity` is the **hub screen** for all configuration options. It presents a list of menu items, each navigating to a specific configuration screen.

```java
// Each menu item follows this pattern:
setupMenuItem(
    view,                          // View to attach click listener
    "Skills",                      // Title
    "Upload and manage agent skills",  // Subtitle
    SkillsActivity.class           // Target activity
);
```

### Menu Items

| Menu Item | Target Activity | Purpose |
|---|---|---|
| Local LLM Upload | `LocalLLMActivity` | Upload and manage `.gguf` model files |
| Model Plugin | `ModelPluginListActivity` | Configure model endpoints and settings |
| Skills | `SkillsActivity` | Upload and manage agent skills |
| Public Tools | `PublicToolsActivity` | Upload and manage root-level tools |
| Formatter Plugin | `FormatterPluginActivity` | Manage LLM response formatters |
| Memory Plugin | `MemoryPluginActivity` | Manage memory backend plugins |
| LlamaCpp | `LlamaCppActivity` | Configure and control local LLM server |
| Access Terminal | `TerminalActivity` | Direct Alpine Linux terminal access |
| SSH Toggle | (inline switch) | Enable/disable SSH service |

---

## ModelPluginListActivity & ModelPluginDetailActivity

**ModelPluginListActivity** shows all configured model plugins in a `RecyclerView` using `ModelPluginListAdapter`. Users can:
- See all saved model configurations
- Tap a model to view/edit details
- Delete model configurations

**ModelPluginDetailActivity** allows creating or editing a model plugin:
- **Base URL** — API endpoint (e.g., `http://127.0.0.1:8080/v1/chat/completions`)
- **Model name** — the model identifier
- **Formatter plugin** — which `FormatterPlugin` to use for request/response formatting
- **Config headers** — HTTP headers (e.g., `Authorization: Bearer <key>`) managed via `ConfigHeaderListAdapter`

---

## SkillsActivity

Shows all installed skills (read from `alpine/root/skills/` directory) as JSON objects in a RecyclerView using `SkillsListAdapter`.

**Upload flow:**
1. User taps upload button
2. File picker opens (filtered for `.zip` files)
3. Selected zip is extracted via `ZipUtils` into `skills/<skillName>/`
4. `SkillsScanner` is re-run to register the new skill

---

## PublicToolsActivity

Shows all installed root-level tools in a RecyclerView using `PublicToolsListAdapter`.

**Upload flow:** Same as skills but extracts to `tools/<toolName>/`.

---

## FormatterPluginActivity

Shows all installed formatter plugins stored in the Room DB. Users can:
- Upload a new `.dex`/`.jar` formatter plugin
- Set a formatter as the default
- Delete formatter plugins

---

## MemoryPluginActivity

Shows installed memory plugins. Same pattern as formatter plugins. Allows switching between `InMemory` (built-in) and custom DEX-based memory implementations.

---

## LlamaCppActivity

Provides UI to:
- **Select model** — choose which `.gguf` file to load (from `LocalLLMActivity`)
- **Set port** — configure the server port (default: `8080`)
- **Start server** — calls `LlamaCppServerRepo.startLlama(port, modelPath)`
- **Stop server** — calls `LlamaCppServerRepo.stopLlama()`
- **View status** — shows whether the server is online

---

## LocalLLMActivity

Allows users to:
- Browse `.gguf` model files stored on the device
- Upload new model files via file picker
- List available models using `ModelsListAdapter`

---

## TerminalActivity

Renders the `userTerminalSession` in a `TerminalView`, providing a **full interactive command-line interface** to the Alpine Linux environment. Users can:
- Run any Alpine Linux command
- Install packages via `apk add`
- Inspect and modify tool/skill binaries
- Debug the environment

---

## See Also

- [MainActivity](./main-activity.md)
- [Plugin System](../plugin-system/plugin-system.md)
- [Terminal Infrastructure](../terminal-infrastructure/terminal-infrastructure.md)
- [Database Layer](../database-layer/database-layer.md)

