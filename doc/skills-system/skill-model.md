# Skill Model

> [← Back to Skills System](./skills-system.md) | [← Back to Docs Root](../skills-system.md)

## Class: `Skill`

**Package:** `org.mobchain.skills`

The `Skill` class is a **data model** that represents a parsed and resolved skill definition. It is created by `SkillsScanner` from a `skill.json` file and registered in `ToolsManager.skillsRegistry`.

---

## Fields

```java
public class Skill {

    private final String name;            // unique skill identifier
    private final String overview;        // short description for root agent
    private final String description;     // system prompt for sub-agent

    private final List<String> privateToolNames;  // from skill.json
    private final List<String> publicToolNames;   // from skill.json

    // Populated by SkillsScanner after validation
    private final List<String> resolvedPublicToolNames = new ArrayList<>();
}
```

---

## Key Methods

### `getResolvedPrivateTools()`

Delegates to `ToolsManager.getToolsBySkill(name)`. Returns the `Tool` objects registered under this skill's name. Dynamically fetches from the registry rather than storing copies.

```java
public List<Tool> getResolvedPrivateTools() {
    return ToolsManager.getToolsBySkill(name);
}
```

### `getResolvedPublicTools()`

Returns the list of public tool names that were validated during scanning (i.e., confirmed to exist in root).

### `getAllTools()`

Combines private + public tools into a single list, used when constructing a sub-agent's `ModelInterface`:

```java
public List<Tool> getAllTools() {
    List<Tool> all = ToolsManager.getToolsBySkill(name);          // private
    for (String publicToolName : resolvedPublicToolNames) {
        Tool publicTool = ToolsManager.getToolByName(publicToolName);
        if (publicTool != null) all.add(publicTool);
    }
    return all;
}
```

### `addResolvedPublicTool(String toolName)`

Called by `SkillsScanner` during resolution to confirm a public tool was successfully found in the root registry.

---

## Private Tools vs. Public Tools

| Aspect | Private Tools | Public Tools |
|---|---|---|
| **Definition** | Listed in `skill.json` under `"privateTools"` | Listed in `skill.json` under `"publicTools"` |
| **Storage location** | `skills/<skillName>/tools/` filesystem folder | `alpine/root/tools/` (root-level) |
| **Registration** | By `ToolsScanner(skill/tools).scanAndRegister()` | Already registered by root `ToolsScanner` |
| **Skill scope** | Only accessible to this skill's sub-agent | Accessible to any skill that declares them |
| **Validation** | Binary existence verified by ToolsScanner | `ToolsManager.isToolAvailableInRoot()` check |

---

## Example

Given this `skill.json`:
```json
{
  "name": "researcher",
  "overview": "Researches topics and summarizes findings",
  "description": "You are a researcher...",
  "privateTools": ["web_search"],
  "publicTools": ["file_writer"]
}
```

The resulting `Skill` object:
- `getName()` → `"researcher"`
- `getOverview()` → `"Researches topics and summarizes findings"`
- `getDescription()` → `"You are a researcher..."` (becomes sub-agent's system prompt)
- `getResolvedPrivateTools()` → `[TerminalTool(web_search, skillName="researcher")]`
- `getResolvedPublicTools()` → `["file_writer"]` (if validated)

---

## See Also

- [SkillsScanner](./skills-scanner.md) — creates and populates Skill instances
- [ToolsManager](../tools-system/tools-manager.md) — stores skills and resolves tool lookups
- [SpawnAgentTool](../tools-system/native-tools.md) — uses Skill at runtime

