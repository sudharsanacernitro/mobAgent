package org.mobchain.memory;

import org.mobAgent.plugin.interfaces.DBMessageStore;
import org.mobAgent.plugin.interfaces.Memory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registry of memory implementations that ship as part of the app code (not as
 * uploadable DEX plugins). These are always available — users do not need to
 * upload a memory JAR to start chatting.
 *
 * Built-in memory implementations use negative sentinel IDs that cannot collide
 * with Room's auto-generated positive primary keys.
 *
 * Mirrors {@code BuiltInFormatters} for the formatter layer.
 */
public final class BuiltInMemory {

    /** Sentinel ID for the built-in InMemory (sliding-window + DB persistence). */
    public static final int IN_MEMORY_ID = -1;

    /** Display name shown in the memory spinner / settings UI. */
    public static final String IN_MEMORY_NAME = "InMemory (Built-in)";

    private static final Map<Integer, String> BUILTIN_NAMES = new LinkedHashMap<>();

    static {
        BUILTIN_NAMES.put(IN_MEMORY_ID, IN_MEMORY_NAME);
    }

    private BuiltInMemory() { }

    /**
     * @return ordered map of built-in memory ID → display name.
     *         Use this to populate UI spinners.
     */
    public static Map<Integer, String> getAll() {
        return BUILTIN_NAMES;
    }

    /** @return {@code true} if the given memory id refers to a built-in memory. */
    public static boolean isBuiltIn(int id) {
        return BUILTIN_NAMES.containsKey(id);
    }

    /**
     * Instantiates the built-in {@link Memory} for the given id.
     *
     * @param id             the built-in memory sentinel ID
     * @param dbMessageStore the DB store for message persistence
     * @return a new Memory instance, or {@code null} if the id is not a built-in.
     */
    public static Memory getInstance(int id, DBMessageStore dbMessageStore) {
        if (id == IN_MEMORY_ID) {
            return new InMemory(dbMessageStore);
        }
        return null;
    }
}

