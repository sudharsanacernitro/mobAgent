# Dependencies

> [← Back to Build Configuration](./build-configuration.md) | [← Documentation Root](../build-configuration.md)

All dependency versions are managed centrally via the **Gradle Version Catalog** at `gradle/libs.versions.toml`.

---

## Core Dependencies (`:core:main`)

### Android Jetpack

| Library | Version | Purpose |
|---|---|---|
| `androidx.appcompat` | 1.7.1 | AppCompat activity base classes |
| `androidx.core:core-ktx` | 1.17.0 | Kotlin Android extensions |
| `androidx.constraintlayout` | 2.2.1 | ConstraintLayout for UIs |
| `androidx.recyclerview` | — | RecyclerView (via appcompat transitive) |
| `androidx.activity` | 1.12.0 | `ActivityResultLauncher`, modern activity APIs |

### Room (ORM)

| Library | Version | Purpose |
|---|---|---|
| `androidx.room:room-runtime` | 2.8.4 | Room persistence library |
| `androidx.room:room-compiler` | 2.8.4 | Annotation processor (kapt/ksp) |
| `androidx.room:room-common-jvm` | 2.8.4 | Room common JVM module |

### Networking

| Library | Version | Purpose |
|---|---|---|
| `com.squareup.okhttp3:okhttp` | 5.3.2 | HTTP client for LLM API calls |

### Material Design

| Library | Version | Purpose |
|---|---|---|
| `com.google.android.material:material` | 1.13.0 | Material 3 components |

### Lifecycle

| Library | Version | Purpose |
|---|---|---|
| `androidx.lifecycle:lifecycle-viewmodel-ktx` | 2.10.0 | ViewModel |
| `androidx.lifecycle:lifecycle-runtime-ktx` | 2.10.0 | Lifecycle-aware coroutines |
| `androidx.lifecycle:lifecycle-process` | 2.10.0 | Process lifecycle |

### Kotlin

| Library | Version | Purpose |
|---|---|---|
| `org.jetbrains.kotlin:kotlin-stdlib` | 2.2.21 | Kotlin standard library |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.10.2 | Coroutines for async operations |

### JSON

| Library | Purpose |
|---|---|
| `org.json` (built-in Android) | JSON parsing throughout agent framework |
| `com.google.code.gson:gson` (2.13.2) | Gson for some serialisation |
| `com.squareup.moshi:moshi` (1.15.2) | Moshi for some parsing |

---

## Local JAR Dependency

| File | Purpose |
|---|---|
| `app/libs/sharedToolInterface.jar` | Shared interfaces for plugin system (`FormatterInterface`, `Memory`, `Tool`, `DBMessageStore`, `Messages`, `FormatterBuilder`, `Response`) |

This JAR is compiled against by:
- `:core:main` (the app)
- Plugin JAR authors (to implement `FormatterBuilderImpl`)

---

## Build Plugins

| Plugin | Version | Purpose |
|---|---|---|
| `com.android.application` | 8.13.1 | Android app build |
| `com.android.library` | 8.13.1 | Android library build |
| `org.jetbrains.kotlin.android` | 2.2.21 | Kotlin compilation |
| `org.jetbrains.kotlin.plugin.compose` | 2.2.21 | Jetpack Compose compiler |
| `org.jetbrains.kotlin.plugin.parcelize` | 2.2.21 | Parcelable code generation |

---

## Test Dependencies

| Library | Version | Purpose |
|---|---|---|
| `junit:junit` | 4.13.2 | Unit tests |
| `androidx.test.ext:junit` | 1.3.0 | Android JUnit integration |
| `androidx.test.espresso:espresso-core` | 3.7.0 | UI tests |

---

## See Also

- [Module Structure](./module-structure.md) — which module uses which dependencies
- [Build & Release](./build-and-release.md) — build commands
- [Plugin System](../plugin-system/plugin-system.md) — `sharedToolInterface.jar` role

