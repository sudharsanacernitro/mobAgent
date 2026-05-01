package org.mobchain.models;

import org.mobAgent.plugin.interfaces.FormatterBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registry of formatters that ship as part of the app code (not as uploadable
 * DEX plugins). These are always available — users do not need to upload a
 * formatter JAR to add their first model plugin.
 *
 * Built-in formatters use negative sentinel IDs that cannot collide with
 * Room's auto-generated positive primary keys.
 *
 * Mirrors the way {@code InMemory} is the default memory implementation
 * loaded from code rather than a DEX plugin.
 */
public final class BuiltInFormatters {

    /** Sentinel ID for the built-in OpenAI-compatible formatter. */
    public static final int OPENAI_FORMATTER_ID = -1;

    /** Display name shown in the formatter spinner / settings UI. */
    public static final String OPENAI_FORMATTER_NAME = "OpenAI Formatter (Built-in)";

    private static final Map<Integer, String> BUILTIN_NAMES = new LinkedHashMap<>();

    static {
        BUILTIN_NAMES.put(OPENAI_FORMATTER_ID, OPENAI_FORMATTER_NAME);
    }

    private BuiltInFormatters() { }

    /**
     * @return ordered map of built-in formatter ID → display name.
     *         Use this to populate UI spinners.
     */
    public static Map<Integer, String> getAll() {
        return BUILTIN_NAMES;
    }

    /** @return {@code true} if the given formatter id refers to a built-in formatter. */
    public static boolean isBuiltIn(int id) {
        return BUILTIN_NAMES.containsKey(id);
    }

    /**
     * Instantiates a fresh {@link FormatterBuilder} for the given built-in id.
     *
     * @return a new builder instance, or {@code null} if the id is not a built-in.
     */
    public static FormatterBuilder getBuilder(int id) {
        if (id == OPENAI_FORMATTER_ID) {
            return new OpenAIFormatterBuilder();
        }
        return null;
    }
}

