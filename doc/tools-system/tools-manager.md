# ToolsManager

> [← Back to Tools System](./tools-system.md) | [← Back to Docs Root](../tools-system.md)

## Purpose

`ToolsManager` (`org.mobchain.tools`) is the **central static registry** for all tools and skills in MobAgent. It maintains three data structures and provides APIs for registering, querying, and resolving tools and skills at runtime.

---

## Internal Data Structures

```java
// Maps tool name → Tool instance
public static HashMap<String, Tool> toolsRegistry = new HashMap<>();

// Maps skill name → List<Tool> (the tools belonging to each skill)
public static HashMap<String, List<Tool>> skillsToToolsMapping = new HashMap<>();

// Maps skill name → Skill instance
public static HashMap<String, Skill> skillsRegistry = new HashMap<>();
```

All three are **static** — there is one shared registry for the entire app process.

---

## Tool Registry API

### `addTools(String skillName, Tool tool)`

Registers a tool under a specific skill. Adds to both `toolsRegistry` (by tool name) and `skillsToToolsMapping` (by skill name).

```java
ToolsManager.addTools("root", myTool);
ToolsManager.addTools("researcher", summarizerTool);
```

### `getToolByName(String name)`

```java
Tool tool = ToolsManager.getToolByName("web_search");
// Returns null if not found
```

### `getToolsArray()`

Returns all registered tools as a `List<Tool>` — used when passing all tools to an LLM.

### `getToolsBySkill(String skillName)`

Returns only the tools registered under the given skill name. Used by `SpawnAgentTool` when setting up a sub-agent.

### `getPublicToolsBySkill(List<String> publicToolNames)`

Resolves a list of tool names to actual `Tool` instances from the global registry. Used to attach public/shared tools to sub-agents.

### `isToolAvailableInRoot(String toolName)`

Returns `true` if the tool exists AND belongs to the `"root"` skill. Used by `SkillsScanner` to validate public tool references.

---

## Skill Registry API

### `addSkill(Skill skill)`

Registers a `Skill` in `skillsRegistry`.

### `getSkill(String skillName)`

Returns the `Skill` instance or `null`.

### `getAllSkills()`

Returns all registered skills as a list.

### `getSystemPromptForSkills()`

Generates a formatted system prompt listing all available skills:

```
The following skills are available to you:

- researcher: can research about topics from the internet
- coder: writes and executes code
```

This is prepended to the root agent's system prompt so the LLM knows which sub-agents it can delegate to via `spawn_agent`.

### `isSkillRegistered(String skillName)`

Quick lookup used during scanning to avoid duplicate registrations.

---

## Relationship Diagram

```
toolsRegistry
  "web_search" → TerminalTool(skillName="root")
  "summarizer" → TerminalTool(skillName="researcher")
  "spawn_agent" → SpawnAgentTool(skillName="root")

skillsToToolsMapping
  "root"       → [TerminalTool(web_search), SpawnAgentTool]
  "researcher" → [TerminalTool(summarizer)]

skillsRegistry
  "researcher" → Skill{ name, overview, description, privateTools, publicTools }
```

---

## See Also

- [ToolsScanner](./tools-scanner.md) — populates toolsRegistry via scanAndRegister()
- [Skills System](../skills-system/skills-system.md) — populates skillsRegistry
- [Executor](./executor.md) — uses getToolByName() at runtime
- [SpawnAgentTool](./native-tools.md) — uses skillsRegistry and getToolsBySkill()

