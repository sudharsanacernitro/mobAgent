# Skills System

> [← Back to Documentation Root](../README.md)

The **Skills System** provides a way to organize tools and agent personas into named "skills". Each skill is a self-contained sub-agent definition with its own system prompt, private tools, and references to shared public tools. Skills enable the root agent to **delegate specialized tasks** to purpose-built sub-agents via the `spawn_agent` tool.

---

## Sub-Features

| File | Description |
|---|---|
| [Skill Model](./skill-model.md) | The `Skill` data class and its properties |
| [SkillsScanner](./skills-scanner.md) | File-system discovery and registration of skills |
| [Sub-Agent Delegation](./sub-agent-delegation.md) | How the root agent uses SpawnAgentTool to delegate |

---

## Overview

### What Is a Skill?

A **skill** is defined by a `skill.json` file in a directory that also contains a `tools/` subfolder. It has:

- A **name** — unique identifier (e.g., `"researcher"`)
- An **overview** — short description shown to the root agent so it knows when to use this skill
- A **description** — the system prompt for the sub-agent running this skill
- **Private tools** — tools only available to this skill's sub-agent (executable binaries in `tools/`)
- **Public tools** — tools shared from the root skill, validated to already exist in `ToolsManager`

### Directory Layout

```
alpine/root/skills/
├── researcher/
│   ├── skill.json
│   └── tools/
│       ├── web_search/
│       │   ├── config.json
│       │   └── web_search
│       └── summarizer/
│           ├── config.json
│           └── summarizer
├── coder/
│   ├── skill.json
│   └── tools/
│       └── code_runner/
│           ├── config.json
│           └── code_runner
```

### skill.json Schema

```json
{
  "name": "researcher",
  "overview": "Can research topics from the internet and summarize findings",
  "description": "You are an excellent researcher. You search the web, read articles, and produce clear summaries. You always cite your sources.",
  "privateTools": ["web_search", "summarizer"],
  "publicTools": ["file_writer"]
}
```

| Field | Description |
|---|---|
| `name` | Skill identifier — must match directory name |
| `overview` | Shown to root agent in its system prompt |
| `description` | System prompt given to the sub-agent |
| `privateTools` | Tool names in this skill's `tools/` folder |
| `publicTools` | Tool names from the root tools (must already be registered) |

---

## Skill Lifecycle

```
App Startup
    │
    ▼
SkillsScanner("alpine/root/skills").scanAndRegister()
    │
    ├── For each skill directory (e.g., "researcher/"):
    │
    │   1. Parse skill.json
    │       └── Create Skill(name, overview, description, privateTools, publicTools)
    │
    │   2. Scan private tools
    │       └── ToolsScanner("researcher/tools").scanAndRegister()
    │           └── Registers tools under skillName="researcher" in ToolsManager
    │
    │   3. Resolve public tools
    │       └── For each publicToolName:
    │               ToolsManager.isToolAvailableInRoot(name) → validate
    │               skill.addResolvedPublicTool(name)
    │
    │   4. Register skill
    │       └── ToolsManager.addSkill(skill)
    │
    └── Done: skillsRegistry has all skills
```

---

## How the Root Agent Discovers Skills

After `SkillsScanner` runs, `ToolsManager.getSystemPromptForSkills()` generates:

```
The following skills are available to you:

- researcher: Can research topics from the internet and summarize findings
- coder: Can write and run code to solve computational problems
```

This text is prepended to the root agent's system prompt, informing the LLM which sub-agents exist and what they're good for. When the LLM decides to delegate, it calls `spawn_agent` with the appropriate `skillName`.

---

## See Also

- [Tools System](../tools-system/tools-system.md) — tools that skills use
- [SpawnAgentTool](../tools-system/native-tools.md) — the mechanism for delegation
- [Memory System](../memory-system/memory-system.md) — each sub-agent gets its own memory
- [Models Layer](../models-layer/models-layer.md) — ModelInterface used when creating sub-agents

