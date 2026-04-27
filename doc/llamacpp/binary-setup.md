# LlamaCpp Binary Setup

> [← Back to LlamaCpp](./llamacpp.md) | [← Documentation Root](../llamacpp.md)

This document explains how llama.cpp ARM64 binaries are installed into the Alpine Linux rootfs environment inside MobAgent.

---

## Setup Script: `llamaCppServerSetup.sh`

The script `llamaCppServerSetup.sh` is a shell script shipped in the app's `assets/` folder. During first run, it is copied to the Alpine rootfs `/root/` directory by `MainActivity.moveSetupFiles()`.

### What the Script Does

```bash
#!/bin/bash
# Usage: llamaCppServerSetup.sh <target_dir> <download_url>
# Example: llamaCppServerSetup.sh llamaCpp http://.../llama-server.zip

TARGET_DIR=$1
DOWNLOAD_URL=$2

mkdir -p $TARGET_DIR
wget -O /tmp/llama-server.zip $DOWNLOAD_URL
unzip /tmp/llama-server.zip -d $TARGET_DIR
chmod +x $TARGET_DIR/llama-server
```

After setup, the Alpine rootfs directory structure becomes:

```
/root/
└── llamaCpp/
    ├── llama-server      ← main executable
    └── lib/
        ├── libggml.so
        ├── libllama.so
        └── ...           ← shared libraries
```

---

## Directory Configuration

The installation directory is configured via `app.properties`:

```properties
LlamaCppDir=llamaCpp
rootDirFromLocalDir=/data/data/<package>/files/local/alpine/root/
llmFilePath=/data/data/<package>/files/local/alpine/root/models
LlamaServerport=8080
```

---

## Running the Binary

The binary **cannot** be executed normally on Android due to **W^X (Write XOR Execute)** restrictions. It must be launched **inside proot** (the Alpine Linux environment) where the filesystem has proper execute permissions.

MobAgent handles this transparently — the `llama_cpp_server` terminal session is a proot shell session. Writing the launch command to this session automatically runs inside proot.

---

## ARM64 Binaries

MobAgent targets ARM64 (AArch64) Android devices. The llama-server binary is compiled with CPU-optimised kernel dispatch for:
- **NEON** SIMD instructions (4-bit quantized inference)
- **Metal/OpenCL** GPU compute (where available on Android)

The pre-built binary package (`llama-server.zip`) is hosted externally and downloaded on demand rather than bundled in the APK (to keep APK size manageable).

---

## Included in App: `llamaCppBinariesForAndroid.zip`

The repository root contains `llamaCppBinariesForAndroid.zip` — this is the binary package that can be manually extracted into the Alpine environment if the automatic download is unavailable.

---

## See Also

- [LlamaCppServerRepo](./server-repo.md) — calls the download script via terminal
- [Terminal Infrastructure](../terminal-infrastructure/terminal-infrastructure.md) — proot environment where the binary runs

