# TerminalTool

> [← Back to Tools System](./tools-system.md) | [← Back to Docs Root](../tools-system.md)

## Purpose

`TerminalTool` (`org.mobchain.tools.OwnTools`) is the primary tool type in MobAgent. It wraps an **Alpine Linux binary** and executes it inside a synchronous terminal session, allowing any compiled program to act as an AI tool.

---

## Constructor

```java
public TerminalTool(
    String toolName,
    String toolPath,          // absolute path to the binary inside Alpine rootfs
    String description,
    JSONObject structuredTool,  // OpenAI-compatible function definition
    JSONArray requiredParameters,
    String skillName
)
```

---

## Execution: `runTool(JSONObject args)`

### Step 1 — Parameter Validation

Before executing, the tool checks that all `requiredParameters` are present in the `args` JSON. If any are missing, it returns an error JSON immediately:

```json
{ "error": "Missing required parameter: query" }
```

### Step 2 — Command Construction

The tool builds a shell command using the `/root/ToolsWrapper.sh` script:

```
/root/ToolsWrapper.sh <tool_binary_path> '<args_json>'
```

The arguments JSON is wrapped in single quotes and passed as a single argument.

### Step 3 — Synchronous Terminal Execution

```java
TerminalSessionManager terminalSessionManager = TerminalSessionManager.getInstance();
TerminalSynchronousSessionHandler session =
    (TerminalSynchronousSessionHandler) terminalSessionManager.getSession("tools");

String output = session.executeCommandSync(commandBuilder.toString(), 20000); // 20s timeout
```

The `"tools"` session is a dedicated synchronous terminal session that blocks until the command completes or times out.

### Step 4 — Result Extraction

The `ToolsWrapper.sh` script must wrap its output with `==Result==` delimiters:

```
==Result==
{ "answer": "42" }
==Result==
```

The code extracts the content between the **first** and **last** occurrence of `==Result==`:

```java
int firstIdx = output.indexOf(RESULT_DELIMITER);
int lastIdx  = output.lastIndexOf(RESULT_DELIMITER);
output = output.substring(firstIdx + RESULT_DELIMITER.length(), lastIdx).trim();
```

### Step 5 — JSON Parsing

The extracted string is parsed into a `JSONObject`. If parsing fails, an error object with `raw_output` is returned.

---

## Tool Binary Requirements

A valid TerminalTool binary must:
1. Accept a single argument: a JSON string with the tool's parameters
2. Print output to stdout with `==Result==` delimiters around the JSON result
3. Be executable (`chmod +x`)
4. Be located in the same directory as its `config.json`

---

## ToolsWrapper.sh Role

`/root/ToolsWrapper.sh` is a shell script that:
- Sets up the environment for tool execution (PATH, Alpine libraries, etc.)
- Calls the binary with the JSON arguments
- Ensures the `==Result==` output format is respected

---

## See Also

- [ToolsScanner](./tools-scanner.md) — how TerminalTools are discovered from the filesystem
- [ToolsManager](./tools-manager.md) — where TerminalTools are registered
- [Terminal Infrastructure](../terminal-infrastructure/terminal-infrastructure.md) — TerminalSessionManager sessions

