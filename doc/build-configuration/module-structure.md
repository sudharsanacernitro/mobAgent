# Module Structure

> [← Back to Build Configuration](./build-configuration.md) | [← Documentation Root](../build-configuration.md)

MobAgent is a **multi-module Gradle project**. Each module has a specific responsibility, keeping concerns cleanly separated.

---

## Module Map

```
MobAgent/
├── app/                       ← :app
├── core/
│   ├── main/                  ← :core:main
│   ├── components/            ← :core:components
│   ├── resources/             ← :core:resources
│   ├── terminal-emulator/     ← :core:terminal-emulator
│   └── terminal-view/         ← :core:terminal-view
└── settings.gradle.kts        ← declares all modules
```

---

## Module Descriptions

### `:app`
- **Type:** Android Application (`com.android.application`)
- **Purpose:** Packaging only — generates the final APK. Contains `proguard-rules.pro`, `testkey.keystore`, and signing configuration.
- **Key files:** `app/build.gradle.kts`, `app/proguard-rules.pro`

### `:core:main`
- **Type:** Android Library (`com.android.library`)
- **Purpose:** The **primary source of all Java code** — all agent framework classes, DAOs, UI activities, repositories, and utilities.
- **Key packages:**
  - `com.example.myapplication` — Android app code (MainActivity, activities, DAOs, repos, utils)
  - `com.rk.terminal` — terminal infrastructure (TerminalSessionManager, AlpineWrapper)
  - `org.mobchain` — AI agent framework (agent loop, memory, tools, skills, models, messages)
- **Dependencies:** Room, OkHttp, sharedToolInterface.jar, terminal-emulator, terminal-view, resources, components

### `:core:components`
- **Type:** Android Library
- **Purpose:** Shared UI components reused across activities (custom views, compound UI elements)

### `:core:resources`
- **Type:** Android Library
- **Purpose:** All XML resources — layouts (`activity_*.xml`, `dialog_*.xml`), drawables, strings, colors, themes

### `:core:terminal-emulator`
- **Type:** Android Library (Gradle `.gradle` not `.kts`)
- **Purpose:** **Forked Termux terminal emulator** — the core VT100/xterm emulator logic. Provides `TerminalSession`, `TerminalSynchronousSessionHandler`, `TerminalAsynchronousSessionHandler`, and associated classes.
- **Note:** This module predates Kotlin DSL adoption, so it uses `build.gradle` (Groovy).

### `:core:terminal-view`
- **Type:** Android Library
- **Purpose:** The `TerminalView` widget that renders terminal output on screen. Handles touch/keyboard input and paints terminal cells.

---

## Dependency Graph

```
:app
  └── :core:main
        ├── :core:components
        ├── :core:resources
        ├── :core:terminal-emulator
        └── :core:terminal-view
```

`:core:main` `build.gradle.kts` declares:
```kotlin
dependencies {
    implementation(project(":core:terminal-emulator"))
    implementation(project(":core:terminal-view"))
    implementation(project(":core:resources"))
    implementation(project(":core:components"))
    implementation(files("../app/libs/sharedToolInterface.jar"))
    // ... Room, OkHttp, etc.
}
```

---

## Build Variants

Both `:app` and `:core:main` define:
- **debug** — for development
- **release** — minified + obfuscated via ProGuard

---

## See Also

- [Dependencies](./dependencies.md) — library versions and catalog
- [Build & Release](./build-and-release.md) — how to build the release APK

