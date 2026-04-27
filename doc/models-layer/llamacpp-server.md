# LlamaCpp Server

> [← Back to Models Layer](./models-layer.md) | [← Back to Docs Root](../models-layer.md)

## Purpose

`LlamaCppServerRepo` (`com.example.myapplication.repo`) manages the lifecycle of the `llama-server` process — a local HTTP server that provides an OpenAI-compatible API for running GGUF model files directly on the Android device using ARM64 llama.cpp binaries.

---

## Architecture

```
Android App
    │
    ├── llamaCppBinariesForAndroid.zip  ← shipped with app
    │       └── llama-server (ARM64 native binary)
    │
    ├── LlamaCppServerRepo
    │       ├── startLlama(port, modelPath)  → starts the server process
    │       ├── stopLlama()                  → kills the process
    │       └── isServerOnline(port)         → health check via HTTP GET /health
    │
    └── OllamaModel.baseURL("http://127.0.0.1:<port>/v1/chat/completions")
```

---

## How the Server Starts

```java
public void startLlama(int port, String model) {
    Thread LlamaThread = new Thread(() -> runServer(port, model));
    LlamaThread.start();
}
```

`startLlama()` spawns a background thread that calls `runServer()`. Inside `runServer()`:

1. The `llama-server` binary is executed as a native process
2. It binds to `127.0.0.1:<port>` 
3. It loads the specified `.gguf` model file
4. It serves the OpenAI-compatible `/v1/chat/completions` endpoint

The app uses a `CountDownLatch serverReady` in `MainActivity` to wait until the server responds healthy before enabling the chat UI.

### Health Check

```java
public static boolean isServerOnline(int port) {
    String healthURL = "http://127.0.0.1:" + port + "/health";
    try {
        Response res = HttpClient.callApi(healthURL, "GET", null, "application/json", null);
        return res.isSuccessful();
    } catch (Exception e) {
        return false;
    }
}
```

---

## Binary Extraction

At app startup, the `llamaCppBinariesForAndroid.zip` file (bundled in `app/` as assets or raw resources) is extracted to the app's internal files directory using `ZipUtils`. The binaries are then made executable via `File.setExecutable(true)`.

---

## LlamaCpp Settings Screen

`LlamaCppActivity` provides a UI for:
- Choosing which `.gguf` model file to load (from `LocalLLMActivity`)
- Setting the server port
- Starting/stopping the server
- Viewing server status

---

## Session Assignment

The `llama-server` process runs outside the terminal session system. However, `TerminalSessionManager` has a dedicated `llamaCppServer` session slot (via `SessionCodes.llamaCppServer`) for alternative approaches to managing the server lifecycle via the Alpine environment.

---

## Model Files

`.gguf` model files are managed through `LocalLLMActivity`. Users can:
1. Upload model files to the device storage
2. Select a model in the LlamaCpp settings
3. The selected model path is saved in `SharedPreferences` / `LlamaCppConstants`

---

## LlamaCppConstants

```java
// com.example.myapplication.utils.constants.LlamaCppConstants
public class LlamaCppConstants {
    public static final String DEFAULT_PORT = "8080";
    public static final String PREF_PORT = "llama_port";
    public static final String PREF_MODEL = "llama_model";
    // ...
}
```

---

## See Also

- [Terminal Infrastructure](../terminal-infrastructure/terminal-infrastructure.md) — binary extraction, process management
- [UI Layer](../ui-layer/ui-layer.md) — LlamaCppActivity, LocalLLMActivity
- [FormatterInterface & OllamaModel](./formatter-and-ollama.md) — connects to the server

