# Alpine Rootfs — Tool Execution Sandbox

> [← Back to Tool Execution Layer](./terminal-infrastructure.md) | [← Back to Docs Root](../terminal-infrastructure.md)

## Purpose in MobAgent

The Alpine rootfs is the **sandbox where MobAgent's tool binaries run**. When the agent decides to execute a tool (e.g. `web_search`, `file_reader`), the `TerminalTool` class routes that call through a synchronous proot session into this environment. The rootfs is **not** a user-facing shell emulator — it is the execution backend for the agentic framework.

## What Is the Alpine Rootfs?

The **Alpine rootfs** is a minimal Alpine Linux file system extracted into the Android app's private storage. It provides a complete Unix-like environment where agent tool binaries run natively:
- A shell (`/bin/sh`, `/bin/bash`) — needed to run `ToolsWrapper.sh`
- Package manager (`apk add`) — install any dependency a tool needs
- Standard Unix tools (`curl`, `wget`, `grep`, `awk`, etc.) — usable inside tool scripts
- Python, Node.js, or other runtimes (if installed via `apk`) — tool interpreters
- Filesystem locations for deployed tool and skill binaries

---

## Directory Layout

```
<app_files_dir>/
└── local/
    └── alpine/
        └── root/
            ├── tools/               ← root-level tool binaries
            │   ├── web_search/
            │   │   ├── config.json
            │   │   └── web_search
            │   └── ...
            ├── skills/              ← skill definitions
            │   ├── researcher/
            │   │   ├── skill.json
            │   │   └── tools/
            │   └── ...
            ├── ToolsWrapper.sh      ← tool execution wrapper script
            └── models/              ← (optional) GGUF model storage
```

---

## proot: Running Linux on Android Without Root

**proot** intercepts system calls from Linux programs and translates filesystem paths to point inside the Alpine rootfs. From the perspective of programs running inside proot:
- `/` is `<app_files_dir>/local/alpine/`
- `/root/` contains tools, skills, and scripts
- All Alpine packages are available

From Android's perspective, this is just an app process running proot as a subprocess — no root privileges required.

---

## Initialization

At app startup, `MainActivity` (or a dedicated initializer) performs:

1. **Extract rootfs** — if not yet present, extract the Alpine rootfs tarball from app assets
2. **Extract llama.cpp binaries** — from `llamaCppBinariesForAndroid.zip`
3. **Set permissions** — `chmod +x` on all tool binaries and the proot binary
4. **Initialize terminal sessions** — `TerminalSessionManager.initSessions()`
5. **Scan tools** — `ToolsScanner("root/tools").scanAndRegister()`
6. **Scan skills** — `SkillsScanner("root/skills").scanAndRegister()`

---

## ToolsWrapper.sh

`/root/ToolsWrapper.sh` is a shell script that:
1. Sets up the Alpine environment (PATH, LD_LIBRARY_PATH)
2. Receives: `<binary_path> '<json_args>'`
3. Executes the binary with the JSON args
4. Wraps stdout with `==Result==` delimiters

Example script structure:
```bash
#!/bin/sh
TOOL_PATH="$1"
ARGS="$2"

# Run the tool
OUTPUT=$("$TOOL_PATH" "$ARGS")

echo "==Result=="
echo "$OUTPUT"
echo "==Result=="
```

---

## Uploading New Tools and Skills

Users can add tools and skills through the UI:

- **Public Tools** → `PublicToolsActivity` → uploads a `.zip` containing `config.json` + binary → extracted to `root/tools/`
- **Skills** → `SkillsActivity` → uploads a `.zip` containing `skill.json` + `tools/` folder → extracted to `root/skills/`
- After upload, `ToolsScanner`/`SkillsScanner` is re-run to register the new items

---

## ZipUtils

`ZipUtils` (`com.example.myapplication.utils`) handles extraction of uploaded `.zip` files into the correct directories in the Alpine rootfs.

---

## See Also

- [TerminalSessionManager](./terminal-session-manager.md)
- [Tools System](../tools-system/tools-system.md)
- [Skills System](../skills-system/skills-system.md)
- [UI Layer](../ui-layer/ui-layer.md) — SkillsActivity, PublicToolsActivity

