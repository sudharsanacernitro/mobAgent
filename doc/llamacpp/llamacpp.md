# LlamaCpp Integration

> [← Back to Documentation Root](../README.md)

MobAgent embeds a full **llama.cpp server** inside the Alpine Linux proot environment. This makes it possible to run a local, OpenAI-compatible LLM API entirely on-device, without sending any data to external servers.

---

## Sub-Features

| File | Description |
|---|---|
| [LlamaCppServerRepo](./server-repo.md) | Server lifecycle management — start, stop, health check |
| [LlamaCppActivity](./llamacpp-activity.md) | Settings UI — status, port config, model selection, binary download |
| [Binary Setup](./binary-setup.md) | How llama-server ARM64 binaries are installed into Alpine |

---

## Architecture Overview

```
LlamaCppActivity (UI)
    │
    ▼
LlamaCppServerRepo
    ├── startLlama(port, model)   → TerminalAsynchronousSessionHandler "llama_cpp_server"
    │       └─→ cd llamaCpp && LD_LIBRARY_PATH=$PWD/lib ./llama-server -m <model> --port <port>
    │
    ├── stopLlama()               → process.destroy()
    │
    ├── isServerOnline(port)      → GET http://127.0.0.1:<port>/health
    │
    └── downloadLlamaCppBinaries()→ TerminalSynchronousSessionHandler "temp_sync"
            └─→ bash llamaCppServerSetup.sh <dir> <download_url>
```

---

## Configuration (app.properties)

| Property | Description |
|---|---|
| `LlamaServerport` | TCP port for llama-server (default: `8080`) |
| `llmFilePath` | Directory path inside Alpine where `.gguf` models are stored |
| `LlamaCppDir` | Directory name inside Alpine for llama-server binaries |
| `rootDirFromLocalDir` | Absolute path to Alpine rootfs on Android |
| `setUpFileName` | Name of the setup shell script |

---

## Server Startup Flow

1. `LlamaCppActivity` reads the list of `.gguf` files from `llmFilePath`
2. User selects a model file from the list
3. User taps **Start Server** → calls `llamaCppServerRepo.startLlama(port, modelFileName)`
4. Repo runs `llama-server` via `TerminalAsynchronousSessionHandler` on terminal session `"llama_cpp_server"`
5. Server prints to terminal but is non-blocking (async session)
6. Status check via HTTP GET `/health`

---

## OpenAI Compatibility

Once running, llama-server exposes an OpenAI-compatible endpoint:

```
POST http://127.0.0.1:<port>/v1/chat/completions
```

The `OllamaModel` (formatter) is configured with this URL, so the agent framework speaks to it as if it were any standard OpenAI API — no special logic needed.

---

## See Also

- [Terminal Infrastructure](../terminal-infrastructure/terminal-infrastructure.md) — `TerminalSessionManager` provides the sessions used here
- [Plugin System](../plugin-system/plugin-system.md) — `FormatterPlugin` wraps the API adapter that speaks to this server
- [Models Layer](../models-layer/models-layer.md) — `OllamaModel` / `ChatModel` sends requests to the server

