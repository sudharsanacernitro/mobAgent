# Plugin Selection Dialog

> [← Back to Chat Sessions](./chat-sessions.md) | [← Documentation Root](../chat-sessions.md)

The **Plugin Selection Dialog** is a non-cancelable `AlertDialog` that appears every time the app launches. It forces the user to select which **model plugin** and **memory plugin** to use for the auto-created session, or to open Settings if they haven't added any plugins yet.

---

## When It Appears

The dialog is always shown on app startup via `autoCreateSessionAndShowDialog()` in `MainActivity.onCreate()`:

```java
// Auto-create a new session on every app launch and show plugin selection dialog
autoCreateSessionAndShowDialog();
```

---

## Dialog UI

The dialog inflates `R.layout.dialog_select_plugins` which contains:

| Widget | ID | Purpose |
|---|---|---|
| `Spinner` | `spinnerModelPlugin` | Pick a model plugin by name |
| `Spinner` | `spinnerMemoryPlugin` | Pick a memory plugin (or "None") |
| `Button` | `btnGoToSettings` | Dismiss dialog and open `SettingsActivity` |

---

## Behaviour

```
Spinners populated from DB:
  modelNames  ← all ModelPlugin rows (joined with Plugin for display name)
  memoryNames ← ["None (In-Memory)"] + MemoryPlugin rows

No model plugins?
  Spinner shows "No model plugins — add in Settings"

"Confirm" button pressed:
  1. Reads selected model ID + memory ID from spinners
  2. Calls chatSessionDao().updatePlugins(currentSessionId, modelId, memoryId)
  3. If alpine is ready: initAgent(modelId, memoryId)

"Skip" button pressed:
  Dialog closes without initialising the agent
  (agent == null → next message shows toast "Default agent is not initialized")

"Go to Settings" pressed:
  Dismisses dialog and starts SettingsActivity
```

---

## Non-Cancelable Behaviour

```java
AlertDialog dialog = new AlertDialog.Builder(this)
    .setTitle("Select Plugins for Session")
    .setView(dialogView)
    .setCancelable(false)   // ← cannot be dismissed by back press or outside touch
    .setPositiveButton("Confirm", ...)
    .setNegativeButton("Skip", null)
    .create();
```

The dialog is intentionally non-cancelable so the user is aware they need to configure plugins. However, "Skip" is provided as an escape hatch.

---

## First-Time Users

If no model plugins exist yet, the model spinner shows a placeholder text. The "Go to Settings" button lets users navigate to `SettingsActivity → Model Plugins` to add their first model before returning to configure a session.

---

## See Also

- [Session Entity](./session-entity.md) — includes `model_plugin_id` and `memory_plugin_id` per session
- [Session UI](./session-ui.md) — plugin can also be changed later per session via long-press
- [Plugin System](../plugin-system/plugin-system.md) — how model/formatter plugins are stored

