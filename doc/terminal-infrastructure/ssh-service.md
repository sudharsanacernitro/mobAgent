# SSH Service

> [← Back to Terminal Infrastructure](./terminal-infrastructure.md) | [← Documentation Root](../terminal-infrastructure.md)

`SshService` provides an optional **SSH server** running inside the Alpine Linux rootfs. This allows users to connect to their MobAgent session from external devices (like a laptop) over a network, using any standard SSH client.

---

## Overview

The SSH server is set up via the `sshServerSetup.sh` script, which is copied to the Alpine rootfs on first launch by `MainActivity.moveSetupFiles()`. The service can then be started inside a terminal session.

---

## Setup Script: `sshServerSetup.sh`

The script (in `assets/`) handles:
1. Installing OpenSSH inside Alpine if not present
2. Generating host keys
3. Configuring `/etc/ssh/sshd_config`
4. Starting the `sshd` daemon on a configured port

---

## SshService Class

```java
package com.rk.terminal.service;

public class SshService {
    // Manages SSH server lifecycle within the terminal session
}
```

`SshService` interacts with `TerminalSessionManager` to start a dedicated SSH session that runs `sshd` in the Alpine proot environment.

---

## Use Cases

| Use Case | Description |
|---|---|
| **Remote access** | Connect from a PC/Mac using `ssh user@<device_ip>` |
| **File transfer** | Use `scp` or `sftp` to transfer files to/from the device |
| **Development** | Use VS Code Remote SSH extension to code directly on-device |
| **Agent tool execution** | Remote scripts can trigger API calls back to the LLM agent |

---

## Security Notes

- SSH runs **inside the proot** Alpine environment — it does not need root access on Android
- The SSH daemon only binds to the local network interface
- Host keys are stored in the Alpine filesystem (not Android keystore)
- Recommended to use key-based authentication rather than passwords

---

## See Also

- [Alpine Rootfs](./alpine-rootfs.md) — where the SSH daemon lives
- [Terminal Session Manager](./terminal-session-manager.md) — manages the terminal session used by SSH
- [Session Codes](./session-codes.md) — session name constants

