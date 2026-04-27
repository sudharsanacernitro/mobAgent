# Tool Execution Layer (Terminal Infrastructure)

> [← Back to Documentation Root](../README.md)

The **Tool Execution Layer** is the infrastructure that gives the MobAgent agentic framework its ability to execute real-world actions. It manages a proot-based Alpine Linux sandbox where all tool binaries live and run. This is **supporting infrastructure** for the agent system — the terminal UI is a secondary convenience on top of it.

---

## Sub-Features

| File | Description |
|---|---|
| [TerminalSessionManager](./terminal-session-manager.md) | Central manager for all terminal sessions |
| [Alpine Rootfs](./alpine-rootfs.md) | The Alpine Linux sandbox setup and role in tool execution |
| [SSH Service](./ssh-service.md) | Optional SSH server for remote access |
| [Session Codes](./session-codes.md) | Named session slots and their purposes |

---

## Role in the Agentic Framework

```
MobAgent Agent Loop
    │
    ├── LlmHandler → decides to call a tool (e.g. "web_search")
    │
    ├── ToolExecutorHandler → calls TerminalTool.runTool(args)
    │
    └── TerminalTool
            │
            ├── Gets "tools" session (synchronous, blocking)
            │       └── TerminalSynchronousSessionHandler inside Alpine proot
            │
            ├── Sends: /root/ToolsWrapper.sh /root/tools/<skill>/<binary> '<json_args>'
            │
            ├── Waits for output (20s timeout)
            │
            └── Parses content between ==Result== markers → JSONObject
                        │
                        └── Returns to LlmHandler as tool result → next LLM turn
```

The Alpine sandbox is the **execution runtime for tools**. The agent does not "use a terminal" — it calls tools as function calls, and under the hood those calls go through the proot shell.

---

## Overview

### What Is proot?

**proot** is a user-space implementation of `chroot` that does **not** require root privileges. It intercepts system calls and redirects filesystem access to a specified root directory. MobAgent uses proot to run Alpine Linux inside the Android app's files directory, making native Linux binaries available without rooting the device.

### Alpine Linux Sandbox Layout

```
<app_files_dir>/local/alpine/root/
├── tools/                     ← public tools (scanned by ToolsScanner at startup)
│   ├── web_search/
│   │   ├── config.json
│   │   └── web_search         ← binary executed by TerminalTool
│   └── ...
├── skills/                    ← skills (scanned by SkillsScanner at startup)
│   ├── researcher/
│   │   ├── skill.json
│   │   └── tools/             ← private tools for this skill
│   └── ...
├── ToolsWrapper.sh             ← wrapper script: sanitises env, runs binary, wraps output in ==Result==
└── llamaCpp/                   ← llama-server + shared libs (for local LLM inference)
```

### Multi-Session Architecture

Different agent operations need different session types. All are named and managed by `TerminalSessionManager`:

```
TerminalSessionManager
    ├── "tools"              → TerminalSynchronousSessionHandler   ★ tool execution (blocking)
    ├── "llama_cpp_server"   → TerminalAsynchronousSessionHandler  ★ llama-server process
    ├── "temp_sync"          → TerminalSynchronousSessionHandler     temporary blocking commands
    ├── "tempAsync"          → TerminalAsynchronousSessionHandler    temporary background commands
    └── "userTerminalSession"→ TerminalAsynchronousSessionHandler    interactive shell for user
```

---

## Terminal Session Types

### Synchronous Sessions (`TerminalSynchronousSessionHandler`)

Block until the command completes (or times out). **Critical for tool execution** — the agent must wait for the tool result before continuing the loop.

```java
String output = session.executeCommandSync(command, 20000);  // 20s timeout
```

### Asynchronous Sessions (`TerminalAsynchronousSessionHandler`)

Run commands in the background, attached to a `TerminalView` for display. Used for:
- Long-running processes (llama-server, SSH daemon)
- User-facing interactive shell

---

## ToolsWrapper.sh

Every tool binary is invoked through `ToolsWrapper.sh` rather than directly. The wrapper:
1. Sets the correct `PATH` and environment inside Alpine
2. Passes the JSON arguments string as the first argument to the binary
3. Surrounds the binary's output with `==Result==` markers so `TerminalTool` can extract it cleanly regardless of any shell preamble

```bash
# Inside ToolsWrapper.sh
echo "==Result=="
"$@"    # run the tool binary
echo "==Result=="
```

---

## Terminal Emulator Libraries

The session and rendering functionality comes from a **forked Termux terminal library** (`core/terminal-emulator` and `core/terminal-view`):

| Class | Module | Purpose |
|---|---|---|
| `TerminalSession` | terminal-emulator | Manages a pseudoterminal (pty) connected to a process |
| `TerminalEmulator` | terminal-emulator | VT100/xterm state machine |
| `TerminalView` | terminal-view | Android `View` for rendering terminal output |
| `MkSession` | terminal-emulator | Factory that creates proot/Alpine sessions |
| `TerminalSynchronousSessionHandler` | terminal-emulator | Blocking `executeCommandSync()` for tool calls |
| `TerminalAsynchronousSessionHandler` | terminal-emulator | Non-blocking `write()` for background processes |

---

## See Also

- [Tools System](../tools-system/tools-system.md) — tools deployed into Alpine and executed here
- [Skills System](../skills-system/skills-system.md) — skill binaries stored inside Alpine
- [LlamaCpp Integration](../llamacpp/llamacpp.md) — llama-server runs inside this sandbox
