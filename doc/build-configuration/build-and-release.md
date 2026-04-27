# Build & Release

> [← Back to Build Configuration](./build-configuration.md) | [← Documentation Root](../build-configuration.md)

This document explains how to build MobAgent — both debug and release variants — and how the release signing process works.

---

## Prerequisites

- Android Studio Ladybug or later  
- JDK 17+  
- Gradle 8.x (via wrapper — no manual install needed)

---

## Build Commands

### Debug Build (for development/testing)

```bash
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

### Release Build

```bash
./gradlew assembleRelease
```

Or use the provided script:

```bash
./release-build.sh
```

Output: `app/build/outputs/apk/release/app-release.apk`

---

## Release Signing

Release APKs are signed with the keystore at `app/testkey.keystore`.

The signing configuration in `app/build.gradle.kts`:

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("testkey.keystore")
            storePassword = "..."
            keyAlias = "..."
            keyPassword = "..."
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

> ⚠️ **Warning:** `testkey.keystore` is a test key committed to the repository. For a production release, replace this with a properly secured keystore and store credentials in environment variables or a CI secrets vault — never in source control.

---

## ProGuard Rules

`app/proguard-rules.pro` contains rules to prevent minification from breaking:
- Room entity classes (field names must match column names)
- `DexClassLoader`-loaded plugin classes (`org.mobAgent.*`)
- JSON parsing (field names must survive minification)

Key rules:
```proguard
# Keep Room entities
-keep class com.example.myapplication.DAOs.entities.** { *; }

# Keep plugin interfaces
-keep interface org.mobAgent.plugin.interfaces.** { *; }
-keep class org.mobAgent.** { *; }

# Keep JSON-parsed classes
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
```

---

## CI/CD

The `release-build.sh` script is designed for use in CI pipelines:

```bash
#!/bin/bash
./gradlew assembleRelease
```

For GitHub Actions, see `.github/workflows/` (if present) or create a workflow that:
1. Checks out the repo
2. Sets up JDK 17
3. Runs `./gradlew assembleRelease`
4. Uploads the APK as an artifact / GitHub release

---

## Fastlane

The `fastlane/` directory contains metadata for F-Droid / Play Store submission, including:
- `fastlane/metadata/android/en-US/` — app description, changelogs, screenshots

---

## See Also

- [Module Structure](./module-structure.md) — understand which module produces the APK
- [Dependencies](./dependencies.md) — library versions used in the build

