# Session Codes & SSH Service

> [← Back to Terminal Infrastructure](./terminal-infrastructure.md) | [← Back to Docs Root](../terminal-infrastructure.md)

## SessionCodes

`SessionCodes` (`com.rk.terminal.utils`) defines integer constants used to identify each terminal session type:

```java
public class SessionCodes {
    public static final int ssh                  = 0;
    public static final int llamaCppServer       = 1;
    public static final int tools                = 2;
    public static final int tempAsync            = 3;
    public static final int tempSync             = 4;
    public static final int userTerminalSession  = 5;
}
```

These constants are used in:
- `TerminalSessionManager.addSession(int sessionCode)` — to create the right session type
- `MkSession` — to configure each session's startup command appropriately

---

## SshService

`SshService` (`com.rk.terminal.service`) manages SSH connectivity for the MobAgent app.

### Features

- **SSH client** — connects to a remote server or another device
- **SSH server** — can expose the Alpine Linux environment over SSH, allowing users to connect from a desktop computer
- Started/stopped via the **Settings screen** (`SettingsActivity` has an SSH toggle)

### Usage Pattern

```java
// Starting SSH service (from SettingsActivity)
SshService.start(context, host, port, username, password);

// The SSH session is managed via SessionCodes.ssh terminal session
TerminalAsynchronousSessionHandler sshSession =
    (TerminalAsynchronousSessionHandler) terminalSessionManager.getSession("ssh");
```

### SSH Server Mode

When the app acts as an SSH server, it starts an SSH daemon inside the Alpine environment (e.g., `dropbear` or `openssh-server`). Remote users can then:
- SSH into the Android device's Alpine environment
- Run the same tools and commands the agents use
- Inspect and modify the tools/skills filesystem

---

## TerminalActivity

`TerminalActivity` (`com.example.myapplication.ui`) provides the **user-facing terminal screen**. It renders the `userTerminalSession` in a `TerminalView`, giving users direct command-line access to the Alpine Linux environment. This is accessible from Settings → "Access Terminal".

---

## See Also

- [TerminalSessionManager](./terminal-session-manager.md)
- [Alpine Rootfs](./alpine-rootfs.md)
- [UI Layer](../ui-layer/ui-layer.md) — TerminalActivity, SettingsActivity

