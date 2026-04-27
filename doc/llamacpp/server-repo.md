# LlamaCppServerRepo

> [← Back to LlamaCpp](./llamacpp.md) | [← Documentation Root](../llamacpp.md)

`LlamaCppServerRepo` is the central repository class that manages the full lifecycle of the `llama-server` process inside Alpine Linux. It is a singleton, initialized in `MainActivity`.

---

## Class Overview

```java
package com.example.myapplication.repo;

public class LlamaCppServerRepo {

    private final Context context;
    private final MainActivity mainActivity;
    private static LlamaCppServerRepo instance = null;
    private Process process;

    public LlamaCppServerRepo(MainActivity context) { ... }

    public static LlamaCppServerRepo getInstance() { return instance; }
}
```

---

## Methods

### `startLlama(int port, String model)`

Spawns a background thread and calls `runServer(port, model)`:

```java
public void startLlama(int port, String model) {
    new Thread(() -> runServer(port, model)).start();
}
```

### `stopLlama()`

Destroys the running process:

```java
public void stopLlama() {
    if (process != null) {
        process.destroy();
    }
}
```

### `isServerOnline(int port)` — static

Performs an HTTP health check:

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

Returns `true` if the server responds with HTTP 2xx.

### `downloadLlamaCppBinaries(String rootDir, String dir)`

Downloads and installs the llama.cpp server binary package via a shell script:

```java
public boolean downloadLlamaCppBinaries(String rootDir, String dir) {
    String command = "bash llamaCppServerSetup.sh " + dir +
                     " http://100.118.114.83:8000/llama-server.zip";
    TerminalSynchronousSessionHandler headlessSession =
        (TerminalSynchronousSessionHandler) TerminalSessionManager
            .getInstance().getSession("temp_sync");
    String output = headlessSession.executeCommandSync(command, 90000);
    return output != null && output.length() != 0;
}
```

- Runs inside a synchronous terminal session with a **90-second timeout**
- Uses `TerminalSessionManager` session named `"temp_sync"`
- The script downloads a `.zip`, unzips it into the Alpine filesystem

### `runServer(int port, String modelName)` — internal

Sends the start command to the async terminal session `"llama_cpp_server"`:

```java
String modelPath = PropertiesReader.getProperty(context, "llmFilePath") + "/" + modelName;
byte[] command = ("cd llamaCpp && LD_LIBRARY_PATH=$PWD/lib ./llama-server -m "
                  + modelPath + " --port " + port + " --host 0.0.0.0 \n").getBytes();
headlessSession.write(command, command.length);
```

Key points:
- `LD_LIBRARY_PATH=$PWD/lib` ensures that shared `.so` libraries bundled alongside `llama-server` are found
- `--host 0.0.0.0` allows localhost connections from the Android app
- Runs asynchronously — the method returns immediately after sending the command

---

## Session Names Used

| Session Name | Type | Purpose |
|---|---|---|
| `"llama_cpp_server"` | `TerminalAsynchronousSessionHandler` | Runs llama-server (long-lived) |
| `"temp_sync"` | `TerminalSynchronousSessionHandler` | Runs binary download script |

---

## See Also

- [LlamaCppActivity](./llamacpp-activity.md) — UI that calls these methods
- [Binary Setup](./binary-setup.md) — how binaries get installed
- [Terminal Infrastructure](../terminal-infrastructure/terminal-infrastructure.md) — `TerminalSessionManager` and session types

