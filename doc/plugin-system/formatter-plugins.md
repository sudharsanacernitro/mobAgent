# Formatter Plugins

> [← Back to Plugin System](./plugin-system.md) | [← Back to Docs Root](../plugin-system.md)

## Purpose

Formatter plugins allow the app to communicate with **different LLM APIs** that use different request/response formats. By implementing a custom formatter plugin, users can add support for:
- Anthropic Claude API
- Google Gemini API
- Custom enterprise LLM endpoints
- APIs with non-standard authentication

MobAgent also ships a **built-in OpenAI formatter** that works out of the box with Ollama, llama-server, OpenAI, and any compatible endpoint — no upload needed. See [BuiltInFormatters](./built-in-formatters.md).

---

## FormatterBuilder Interface

Plugin `.dex` files must implement `FormatterBuilder`:

```java
// in sharedToolInterface.jar
public interface FormatterBuilder {
    FormatterInterface build();
    // or with configuration:
    FormatterInterface build(Context context, JSONObject config);
}
```

The concrete class in the plugin **must** be named `org.mobAgent.FormatterBuilderImpl`.

---

## FormatterPlugin Entity

Formatter plugins are tracked in Room DB:

```java
@Entity
public class FormatterPlugin extends Plugin {
    // inherits: id, name, path, description from Plugin
    String apiEndpoint;    // the LLM API endpoint this formatter targets
    String modelName;      // default model name
}
```

---

## ModelPlugin with Formatter

The `ModelPlugin` entity stores a complete model configuration that references a `FormatterPlugin`:

```java
@Entity
public class ModelPlugin {
    @PrimaryKey(autoGenerate = true)
    int id;
    String name;
    String formatterPluginId;   // references a formatter plugin
    String modelName;           // specific model variant
    String baseUrl;             // API endpoint override
    // config headers stored in ConfigHeader table
}
```

`ModelPluginWithFormatterPath` is a Room relation that joins `ModelPlugin` with the path of its `FormatterPlugin` — used when loading all data needed to instantiate the model.

---

## ConfigHeader

Config headers allow adding HTTP headers to model requests (e.g., API keys):

```java
@Entity
public class ConfigHeader {
    @PrimaryKey(autoGenerate = true)
    int id;
    int modelPluginId;    // FK to ModelPlugin
    String headerName;    // e.g., "Authorization"
    String headerValue;   // e.g., "Bearer sk-..."
}
```

These headers are loaded and passed to `OllamaModel.builder().headers(headersMap)` when constructing the model.

---

## Loading a Formatter Plugin

In `MainActivity`:

```java
DexLoader dexLoader = new DexLoader(this);
FormatterBuilder formatterBuilder = dexLoader.loadFormatter(formatterPluginId);
FormatterInterface formatter = formatterBuilder.build();

ModelInterface agent = ModelInterface.builder()
    .setModel(formatter)
    .setMemory(memory)
    .addTools(tools)
    .build();
```

---

## See Also

- [BuiltInFormatters](./built-in-formatters.md) — code-bundled formatters, no upload required
- [DexLoader](./dex-loader.md)
- [Model Plugins](./model-plugins.md)
- [Plugin Database](./plugin-database.md)
- [ModelPluginDetailActivity](../ui-layer/ui-layer.md)

