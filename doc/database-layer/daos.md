# DAOs

> [← Back to Database Layer](./database-layer.md) | [← Back to Docs Root](../database-layer.md)

## Overview

Each entity has a corresponding **DAO** (Data Access Object) interface annotated with Room's `@Dao`. Room generates the implementation at compile time.

---

## PluginDao

```java
@Dao
public interface PluginDao {
    @Insert
    long insert(Plugin plugin);

    @Query("SELECT * FROM plugins")
    List<Plugin> getAll();

    @Query("SELECT * FROM plugins WHERE id = :id")
    Plugin getById(int id);

    @Query("SELECT * FROM plugins WHERE type = :type")
    List<Plugin> getByType(String type);

    @Delete
    void delete(Plugin plugin);
}
```

Used by `DexLoader` to find the filesystem path for a plugin by its ID.

---

## ModelPluginDao

```java
@Dao
public interface ModelPluginDao {
    @Insert
    long insert(ModelPlugin modelPlugin);

    @Query("SELECT * FROM model_plugins")
    List<ModelPlugin> getAll();

    @Transaction
    @Query("SELECT * FROM model_plugins WHERE id = :id")
    ModelPluginWithFormatterPath getWithFormatterPath(int id);

    @Transaction
    @Query("SELECT * FROM model_plugins")
    List<ModelPluginWithPluginName> getAllWithPluginName();

    @Delete
    void delete(ModelPlugin modelPlugin);
}
```

`getWithFormatterPath()` is a transactional query that joins `ModelPlugin` with the associated `Plugin` (formatter) to retrieve its `.path` in one call.

---

## ConfigHeaderDao

```java
@Dao
public interface ConfigHeaderDao {
    @Insert
    long insert(ConfigHeader header);

    @Query("SELECT * FROM config_headers WHERE modelPluginId = :modelPluginId")
    List<ConfigHeader> getHeadersForModel(int modelPluginId);

    @Delete
    void delete(ConfigHeader header);
}
```

Used in `ModelPluginDetailActivity` to add/remove HTTP headers for a model configuration.

---

## ChatMessageDao

```java
@Dao
public interface ChatMessageDao {
    @Insert
    long insert(ChatMessage message);

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    List<ChatMessage> getMessagesForSession(int sessionId);

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    void deleteMessagesForSession(int sessionId);
}
```

Used by `ChatMessageStore` to retrieve and persist conversation messages.

---

## ChatSessionDao

```java
@Dao
public interface ChatSessionDao {
    @Insert
    long insert(ChatSession session);

    @Query("SELECT * FROM chat_sessions ORDER BY createdAt DESC")
    List<ChatSession> getAll();

    @Delete
    void delete(ChatSession session);
}
```

Used by `ChatSessionListActivity` to display and manage sessions.

---

## MemoryPluginDao

```java
@Dao
public interface MemoryPluginDao {
    @Insert
    long insert(MemoryPlugin plugin);

    @Query("SELECT * FROM memory_plugins")
    List<MemoryPlugin> getAll();

    @Delete
    void delete(MemoryPlugin plugin);
}
```

---

## FormatterPluginDao

```java
@Dao
public interface FormatterPluginDao {
    @Insert
    long insert(FormatterPlugin plugin);

    @Query("SELECT * FROM formatter_plugins")
    List<FormatterPlugin> getAll();

    @Delete
    void delete(FormatterPlugin plugin);
}
```

---

## DefaultPluginDao

```java
@Dao
public interface DefaultPluginDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void setDefault(DefaultPlugin defaultPlugin);

    @Query("SELECT * FROM default_plugins WHERE pluginType = :type")
    DefaultPlugin getDefault(String type);
}
```

Used at agent startup to determine which plugins are active.

---

## Threading

All DAO calls in Android Room **must be called off the main thread** (or use `allowMainThreadQueries()` in the builder — not recommended). In MobAgent, database access is wrapped in:
- `Executors.newSingleThreadExecutor()` for background operations
- `LiveData` for observable data in some screens
- Direct calls on background threads for agent initialization

---

## See Also

- [PluginDatabase](./plugin-database.md)
- [Entities](./entities.md)
- [ChatMessageStore](./chat-message-store.md)

