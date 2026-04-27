# LlamaCppActivity

> [← Back to LlamaCpp](./llamacpp.md) | [← Documentation Root](../llamacpp.md)

`LlamaCppActivity` is the Settings sub-screen dedicated to managing the local llama.cpp server. It is reached via `SettingsActivity → LlamaCpp`.

---

## UI Controls

| Control | Action |
|---|---|
| **Check Status** button | Checks if server is running via `/health` HTTP endpoint |
| **Start Server** button | Starts the llama-server with selected model and port |
| **Stop Server** button | Stops the running llama-server process |
| **Port** input field | Lets user change the server port (persisted in `app.properties`) |
| **Model** spinner | Populated from `.gguf` files found at `llmFilePath` |
| **Download Binaries** button | Downloads and installs llama.cpp ARM64 binaries with overwrite warning |

---

## Model Discovery

On activity creation, `LlamaCppActivity` reads `llmFilePath` from `app.properties` and lists all `.gguf` files in that directory:

```java
String llmDir = PropertiesReader.getProperty(this, "llmFilePath");
File dir = new File(llmDir);
if (dir.exists() && dir.isDirectory()) {
    for (File f : dir.listFiles()) {
        if (f.getName().endsWith(".gguf")) {
            modelFiles.add(f.getName());
        }
    }
}
```

These file names populate the spinner. The selected file name is passed to `startLlama(port, modelName)`.

---

## Download Binaries Flow

When the user taps **Download Binaries**:

1. An `AlertDialog` is shown:
   > "If it is already installed, this will download and **overwrite** the existing binary."

2. On "Proceed":
   ```java
   LlamaCppServerRepo repo = LlamaCppServerRepo.getInstance();
   new Thread(() -> {
       boolean success = repo.downloadLlamaCppBinaries(rootDir, llamaCppDir);
       runOnUiThread(() -> Toast.makeText(this,
           success ? "Download complete" : "Download failed",
           Toast.LENGTH_SHORT).show());
   }).start();
   ```

3. The download script fetches `llama-server.zip` from a configured URL and extracts it into the Alpine filesystem under the `LlamaCppDir` directory.

---

## Accessing from Settings

`LlamaCppActivity` is opened from `SettingsActivity`:

```java
// In activity_settings.xml, one of the settings rows triggers:
startActivity(new Intent(SettingsActivity.this, LlamaCppActivity.class));
```

---

## See Also

- [LlamaCppServerRepo](./server-repo.md) — backend logic called by this activity
- [Binary Setup](./binary-setup.md) — what the download script does
- [UI Layer](../ui-layer/ui-layer.md) — Settings screen and other activities

