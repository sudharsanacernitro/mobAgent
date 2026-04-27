# DexLoader

> [← Back to Plugin System](./plugin-system.md) | [← Back to Docs Root](../plugin-system.md)

## Purpose

`DexLoader` (`com.example.myapplication.utils`) is the **runtime class loading engine** for MobAgent's plugin system. It uses Android's `DexClassLoader` to load compiled Java/Kotlin code from `.jar`/`.dex` files stored on the device, and instantiates plugin implementations without the need to rebuild the APK.

---

## How It Works

```java
public class DexLoader {

    private Context context;

    public DexLoader(Context context) {
        this.context = context;
    }

    public final FormatterBuilder loadFormatter(int formatterPluginId) {

        // 1. Look up plugin in Room DB
        Plugin plugin = PluginDatabase.getInstance(context).pluginDao().getById(formatterPluginId);
        String pluginPath = plugin.getPath();

        // 2. Verify file exists
        File jarFile = new File(pluginPath);
        jarFile.setReadable(true, false);

        // 3. Create DexClassLoader with app class loader as parent
        String optimizedDexOutputPath = context.getDir("outdex", Context.MODE_PRIVATE).getAbsolutePath();
        DexClassLoader classLoader = new DexClassLoader(
                jarFile.getAbsolutePath(),
                optimizedDexOutputPath,        // directory for optimized .dex files
                null,                          // no native library directory
                context.getClassLoader()       // CRITICAL: app class loader as parent
        );

        // 4. Load the well-known implementation class
        Class<?> clazz = classLoader.loadClass("org.mobAgent.FormatterBuilderImpl");
        Object instance = clazz.getDeclaredConstructor().newInstance();

        return (FormatterBuilder) instance;
    }
}
```

---

## The `outdex` Directory

When `DexClassLoader` loads a `.jar`, it **optimizes** the DEX bytecode for the current device's ART runtime and stores the result in the `outdex` directory (`context.getDir("outdex", Context.MODE_PRIVATE)`). This:
- Happens only once per plugin file
- Improves subsequent load performance
- Is stored in the app's private internal storage

---

## Class Name Convention

Plugins must define their implementation class at the **exact, well-known class name** `org.mobAgent.FormatterBuilderImpl`. This is the contract between the app and all plugin authors:

| Plugin Type | Required Class Name |
|---|---|
| Formatter | `org.mobAgent.FormatterBuilderImpl` |
| Memory | (similar convention, implementation-specific) |
| Model | `org.mobAgent.FormatterBuilderImpl` (same, via FormatterBuilder) |

---

## Why `context.getClassLoader()` as Parent?

The parent class loader is set to the **app's class loader** so that when the plugin's class references `FormatterBuilder` (from `sharedToolInterface.jar`), it resolves to the **same `FormatterBuilder` class** that the app uses. Without this, the cast `(FormatterBuilder) instance` would throw a `ClassCastException` because the plugin's `FormatterBuilder` class and the app's `FormatterBuilder` class would be different objects in different class loaders.

---

## Error Handling

```java
catch (Exception e) {
    Log.e("Plugin", "Failed to load formatter", e);
    runOnUiThread(() -> {
        Toast.makeText(context,
            "Failed to load formatter plugin: " + e.getMessage(),
            Toast.LENGTH_LONG).show();
    });
    return null;
}
```

Any failure (file not found, class not found, instantiation error) shows a Toast to the user and returns `null`. The calling code must handle `null` gracefully.

---

## File Permissions

Before loading, the file permissions are explicitly set:
```java
jarFile.setReadable(true, false);   // readable by all
jarFile.setWritable(false, false);  // not writable
```

This is important for security — plugins should be read-only once installed.

---

## See Also

- [Formatter Plugins](./formatter-plugins.md)
- [Plugin Database](./plugin-database.md)
- [FormatterInterface](../models-layer/formatter-and-ollama.md)

