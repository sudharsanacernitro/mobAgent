# SkillsScanner

> [← Back to Skills System](./skills-system.md) | [← Back to Docs Root](../skills-system.md)

## Purpose

`SkillsScanner` (`org.mobchain.skills`) discovers all skills in the skills root directory, resolves their tools (both private and public), and registers them in `ToolsManager`. It uses `ToolsScanner` internally to handle private tool discovery.

---

## Expected Directory Layout

```
alpine/root/skills/
├── researcher/
│   ├── skill.json           ← REQUIRED
│   └── tools/
│       ├── web_search/
│       │   ├── config.json
│       │   └── web_search
│       └── summarizer/
│           ├── config.json
│           └── summarizer
└── coder/
    ├── skill.json
    └── tools/
        └── code_runner/
            ├── config.json
            └── code_runner
```

> **Note:** The config file is named `skill.json` (singular), not `skills.json`.

---

## API

### Constructor

```java
SkillsScanner scanner = new SkillsScanner("/data/local/alpine/root/skills");
// or
SkillsScanner scanner = new SkillsScanner(new File(filesDir, "alpine/root/skills"));
```

### `scanAndRegister()`

Scans all skill directories, resolves tools, registers everything, and returns the count of registered skills.

```java
int count = scanner.scanAndRegister();
// Output: SkillsScanner: Scan complete. 2/2 skills registered.
```

---

## Internal Algorithm

### `processSkillDirectory(File skillDir)`

For each skill directory:

**1. Parse `skill.json`**

```java
String configContent = readFileToString(new File(skillDir, "skill.json"));
JSONObject config = new JSONObject(configContent);

String skillName   = config.getString("name");
String overview    = config.optString("overview", "");
String description = config.optString("description", "");

List<String> privateToolNames = jsonArrayToList(config.optJSONArray("privateTools"));
List<String> publicToolNames  = jsonArrayToList(config.optJSONArray("publicTools"));

Skill skill = new Skill(skillName, overview, description, privateToolNames, publicToolNames);
```

**2. Scan private tools**

```java
File toolsDir = new File(skillDir, "tools");
ToolsScanner toolsScanner = new ToolsScanner(toolsDir);
toolsScanner.scanAndRegister();
// Tools are now in ToolsManager under skillName="researcher"
```

Then validates that all declared `privateToolNames` are actually found:

```java
for (String privateName : privateToolNames) {
    Tool tool = ToolsManager.getToolByName(privateName);
    if (tool == null) {
        System.err.println("Private tool not found after scanning: " + privateName);
    }
}
```

**3. Resolve public tools**

```java
for (String publicName : publicToolNames) {
    if (ToolsManager.isToolAvailableInRoot(publicName)) {
        skill.addResolvedPublicTool(publicName);
    } else {
        System.err.println("Public tool not found in root: " + publicName);
    }
}
```

Public tools must already be registered (root `ToolsScanner` must run **before** `SkillsScanner`).

**4. Register skill**

```java
ToolsManager.addSkill(skill);
```

---

## Error Handling

| Condition | Behavior |
|---|---|
| Skills directory doesn't exist | Logs error, returns 0 |
| `skill.json` missing in a directory | Logs error, skips skill |
| A private tool binary not found | Logs warning, skill still registers |
| A public tool not in root registry | Logs warning, tool not added to skill |
| JSON parsing error | Logs error with message, skips skill |

---

## Initialization Order Requirement

The scanning must be done in this order at app startup:

```
1. ToolsScanner("root/tools").scanAndRegister()    ← root tools first
2. SkillsScanner("root/skills").scanAndRegister()  ← skills after (public tool validation)
3. Register native tools (SpawnAgentTool, etc.)
```

---

## See Also

- [Skill Model](./skill-model.md)
- [ToolsScanner](../tools-system/tools-scanner.md)
- [ToolsManager](../tools-system/tools-manager.md)
- [Sub-Agent Delegation](./sub-agent-delegation.md)

