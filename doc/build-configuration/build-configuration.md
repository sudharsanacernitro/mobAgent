# Build Configuration

> [← Back to Documentation Root](../README.md)

MobAgent uses a **multi-module Gradle** build with Kotlin DSL (`build.gradle.kts`), a centralized version catalog (`libs.versions.toml`), and a signing configuration for release builds.

---

## Sub-Features

| File | Description |
|---|---|
| [Module Structure](./module-structure.md) | All Gradle modules and their roles |
| [Dependencies](./dependencies.md) | Key libraries and version catalog |
| [Build & Release](./build-and-release.md) | Release signing and build scripts |

---

## Project Modules

```
settings.gradle.kts defines:
    :app                     ← Application entry (APK packaging)
    :core:main               ← All Java source code
    :core:components         ← Shared UI components
    :core:resources          ← String resources, layouts, drawables
    :core:terminal-emulator  ← Terminal emulator library (Termux fork)
    :core:terminal-view      ← Terminal UI widget
```

### Dependency Flow

```
:app
 └── :core:main
      ├── :core:components
      ├── :core:resources
      ├── :core:terminal-emulator
      └── :core:terminal-view
```

---

## Key Dependencies

| Library | Purpose |
|---|---|
| `androidx.room` | Room ORM for SQLite |
| `okhttp3` | HTTP client for LLM API calls |
| `org.json` | JSON parsing |
| `androidx.appcompat` | Android compatibility |
| `androidx.recyclerview` | RecyclerView lists |
| `androidx.databinding` | Data binding for layouts |
| `sharedToolInterface.jar` | Local JAR: plugin interfaces |

---

## libs.versions.toml

The centralized version catalog at `gradle/libs.versions.toml` defines all dependency versions:

```toml
[versions]
room = "2.6.x"
okhttp = "4.x.x"
# ...

[libraries]
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
# ...
```

---

## Release Build

`release-build.sh` automates the release APK build and signing:

```bash
./gradlew assembleRelease
# Signs with app/testkey.keystore
```

The `testkey.keystore` in `app/` is used for signing. For production, this should be replaced with a properly secured keystore.

---

## See Also

- [Plugin System](../plugin-system/plugin-system.md) — `sharedToolInterface.jar` dependency
- [Terminal Infrastructure](../terminal-infrastructure/terminal-infrastructure.md) — terminal-emulator and terminal-view modules

