# Native Tools — SpawnAgentTool

> [← Back to Tools System](./tools-system.md) | [← Back to Docs Root](../tools-system.md)

## Overview

**Native tools** are tools implemented entirely in Java (not via external binaries). They are registered under the `"root"` skill and available to every agent. The primary built-in native tool is `SpawnAgentTool`.

---

## SpawnAgentTool

**Class:** `org.mobchain.tools.OwnTools.NativeTools.SpawnAgentTool`  
**Tool Name:** `spawn_agent`  
**Skill:** `root` (globally available)

### Purpose

`SpawnAgentTool` enables **multi-agent delegation**. When the root agent determines that a task requires a specialized capability (a "skill"), it calls `spawn_agent` with the skill name and the user's message. A brand new sub-agent is created for that skill, processes the request, and returns its output.

### Structured Tool Definition (auto-generated)

```json
{
  "type": "function",
  "function": {
    "name": "spawn_agent",
    "description": "Tool to spawn a new agent with specific attributes and goals",
    "parameters": {
      "type": "object",
      "required": ["skillName", "userMessage"],
      "properties": {
        "skillName": {
          "type": "string",
          "description": "name of the skill used to spawn the agent with"
        },
        "userMessage": {
          "type": "string",
          "description": "The last user message that requires this skill/sub-agent"
        }
      }
    }
  }
}
```

### Execution: `runTool(JSONObject args)`

```java
String skillName = args.getString("skillName");

Skill skill = ToolsManager.skillsRegistry.get(skillName);

List<Tool> privateTools = ToolsManager.getToolsBySkill(skillName);
List<Tool> publicTools  = ToolsManager.getPublicToolsBySkill(skill.getPublicToolNames());

// Create fresh memory and model for the sub-agent
InMemory memory = new InMemory(new ChatMessageStore(MainActivity.getAppContext()));
memory.setSystemPrompt(new SystemMessages(skill.getDescription()));

OllamaModel model = OllamaModel.builder()
        .baseURL("http://127.0.0.1:8080/v1/chat/completions")
        .model("model")
        .build();

ModelInterface agentInterface = ModelInterface.builder()
        .setModel(model)
        .setMemory(memory)
        .addTools(privateTools)
        .addTools(publicTools)
        .build();

String agentOutput = agentInterface.chat(new HumanMessages(userMessage));

result.put("outputOfTheSkill", agentOutput);
```

### Key Design Points

1. **Each sub-agent gets its own `InMemory` instance** — sub-agent conversations are isolated
2. **System prompt = skill description** — the sub-agent's persona is defined by the skill's `description` field in `skill.json`
3. **Private + public tools** — the sub-agent has access to both its skill-specific private tools and any shared root public tools
4. **Sub-agents can spawn further sub-agents** — because `SpawnAgentTool` is registered under `"root"` and is shared, any agent with access to root tools can delegate further
5. **Fixed endpoint** — currently hard-coded to `http://127.0.0.1:8080/v1/chat/completions` (the LlamaCpp server)

---

## NativeTool Base Class

`NativeTool.java` (`org.mobchain.tools.OwnTools`) is the abstract base class for all Java-native tools. It provides common boilerplate and ensures the `Tool` interface contract is met without requiring file-system resources.

---

## See Also

- [Skills System](../skills-system/skills-system.md) — how skills are defined and discovered
- [ToolsManager](./tools-manager.md) — root tool registration
- [Models Layer](../models-layer/models-layer.md) — ModelInterface builder used when spawning sub-agents

