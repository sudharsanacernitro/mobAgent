# ToolsScanner

> [← Back to Tools System](./tools-system.md) | [← Back to Docs Root](../tools-system.md)

## Purpose

`ToolsScanner` (`org.mobchain.tools`) is the **plug-and-play tool discovery engine**. It scans a root tools directory on the filesystem, reads each tool's `config.json`, validates the binary exists, constructs a `TerminalTool`, and registers it in `ToolsManager`.

---

## Expected Directory Layout

```
alpine/root/tools/          ← toolsRootDir
├── web_search/
│   ├── config.json
│   └── web_search           ← binary (must match "name" in config.json)
├── file_reader/
│   ├── config.json
│   └── file_reader
└── code_runner/
    ├── config.json
    └── code_runner
```

---

## config.json Schema

```json
{
  "name": "web_search",
  "description": "Searches the web for a given query",
  "binary": "web_search",
  "requiredParams": ["query"],
  "structuredTool": {
    "type": "function",
    "function": {
      "name": "web_search",
      "description": "Searches the web",
      "parameters": {
        "type": "object",
        "required": ["query"],
        "properties": {
          "query": { "type": "string", "description": "The search query" }
        }
      }
    }
  }
}
```

| Field | Required | Description |
|---|---|---|
| `name` | ✅ | Tool name — must match the binary filename |
| `description` | ✅ | Human-readable description |
| `requiredParams` | ✅ | Array of parameter names the tool must receive |
| `structuredTool` | ✅ | Full OpenAI-compatible function JSON |
| `binary` | optional | Binary name (defaults to `name`) |

---

## API

### Constructor

```java
ToolsScanner scanner = new ToolsScanner("/data/local/alpine/root/tools");
// or
ToolsScanner scanner = new ToolsScanner(new File(filesDir, "alpine/root/tools"));
```

### `scanAndRegister()`

Scans the tools directory, registers all valid tools in `ToolsManager`, and returns the count of successfully registered tools.

```java
int count = scanner.scanAndRegister();
// Output: ToolsScanner: Scan complete. 3/3 tools registered.
```

**Internal flow:**

1. Validates `toolsRootDir` exists and is a directory
2. Lists all subdirectories (each subdirectory = one tool)
3. For each subdirectory:
   - Reads `config.json`
   - Extracts `name`, `description`, `requiredParams`, `structuredTool`
   - Verifies the binary file (`toolDir/<name>`) exists
   - Constructs `TerminalTool`
   - Calls `ToolsManager.addTools(skillName, tool)`
4. The `skillName` is derived from the **parent directory of `toolsRootDir`** (e.g., if the path is `skills/researcher/tools`, skill name = `"researcher"`)

### Skill Name Derivation

```java
String[] paths = toolsRootDir.getAbsolutePath().split("/");
String skillName = paths[paths.length - 2];  // parent of "tools" folder
```

- Root tools → `ToolsScanner("alpine/root/tools")` → skillName = `"root"`
- Skill tools → `ToolsScanner("skills/researcher/tools")` → skillName = `"researcher"`

---

## Error Handling

| Condition | Behavior |
|---|---|
| Tools root dir doesn't exist | Logs error, returns 0 |
| No subdirectories found | Logs info, returns 0 |
| `config.json` missing | Logs error, skips tool |
| Binary file missing | Logs error, skips tool |
| JSON parsing error | Logs error with stack trace, skips tool |

---

## See Also

- [TerminalTool](./terminal-tool.md) — the tool type ToolsScanner creates
- [ToolsManager](./tools-manager.md) — where discovered tools are registered
- [Skills System](../skills-system/skills-system.md) — SkillsScanner uses ToolsScanner internally

