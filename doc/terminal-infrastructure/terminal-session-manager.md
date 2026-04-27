# TerminalSessionManager

> [← Back to Terminal Infrastructure](./terminal-infrastructure.md) | [← Back to Docs Root](../terminal-infrastructure.md)

## Purpose

`TerminalSessionManager` (`com.rk.terminal.service`) is a **singleton** that creates, tracks, and provides access to all terminal sessions used by the application. Each session is a live pseudoterminal (pty) process connected to the Alpine Linux environment.

---

## Singleton Pattern

```java
public static synchronized TerminalSessionManager getInstance(MainActivity activity) {
    if (instance == null) {
        instance = new TerminalSessionManager(activity);
    }
    return instance;
}

public static TerminalSessionManager getInstance() {
    return instance;   // null if not yet initialized
}
```

The manager is initialized once at app startup with a `MainActivity` reference (needed for creating terminal views and handling UI callbacks).

---

## Session Initialization

```java
public void initSessions() {
    addSession(SessionCodes.ssh);
    addSession(SessionCodes.llamaCppServer);
    addSession(SessionCodes.tools);
    addSession(SessionCodes.tempAsync);
    addSession(SessionCodes.tempSync);
    addSession(SessionCodes.userTerminalSession);
}
```

Each session is created via `addSession(int sessionCode)` which uses `MkSession` to create the appropriate session type.

---

## Session Types by Code

| Session Code | Type | Name Key | Purpose |
|---|---|---|---|
| `SessionCodes.ssh` | Async | `"ssh"` | SSH client connection |
| `SessionCodes.llamaCppServer` | Async | `"llamaCppServer"` | Running `llama-server` process |
| `SessionCodes.tools` | **Sync** | `"tools"` | Tool execution (blocks for result) |
| `SessionCodes.tempAsync` | Async | `"tempAsync"` | Temporary async operations |
| `SessionCodes.tempSync` | **Sync** | `"tempSync"` | Temporary sync operations |
| `SessionCodes.userTerminalSession` | Async | `"userTerminalSession"` | Interactive user terminal |

---

## `addSession(int sessionCode)`

The switch statement in `addSession()` creates the correct session type:

```java
switch (sessionCode) {
    case SessionCodes.tools:
    case SessionCodes.tempSync:
        session = new TerminalSynchronousSessionHandler(...);
        break;

    case SessionCodes.ssh:
    case SessionCodes.llamaCppServer:
    case SessionCodes.tempAsync:
    case SessionCodes.userTerminalSession:
        session = new TerminalAsynchronousSessionHandler(...);
        break;
}
sessionMapper.put(sessionName, session);
```

---

## `getSession(String name)`

```java
public TerminalSessionHandler getSession(String name) {
    if (sessionMapper.containsKey(name)) {
        return sessionMapper.get(name);
    }
    throw new NotFoundException("Session not found: " + name);
}
```

Usage in `TerminalTool`:
```java
TerminalSynchronousSessionHandler session =
    (TerminalSynchronousSessionHandler) terminalSessionManager.getSession("tools");
```

The cast is safe because `"tools"` is always created as a `TerminalSynchronousSessionHandler`.

---

## MkSession Factory

`MkSession` (`com.rk.terminal.ui.screens.terminal`) creates terminal sessions with the correct startup command that invokes the proot/Alpine environment. It sets:
- The shell executable (`/bin/sh` inside Alpine)
- The proot command wrapper
- Working directory
- Environment variables

---

## Session Lifecycle

Sessions are created at app startup and **persist for the application lifetime**. They are not destroyed unless the app process dies. This means:
- The Alpine environment is always running in the background
- Tool execution can happen at any time without setup delay
- The `llama-server` process remains running between chat messages

---

## See Also

- [Session Codes](./session-codes.md)
- [Alpine Rootfs](./alpine-rootfs.md)
- [TerminalTool](../tools-system/terminal-tool.md) — uses the "tools" session
- [LlamaCppServerRepo](../models-layer/llamacpp-server.md) — uses the "llamaCppServer" session

