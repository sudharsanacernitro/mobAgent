package org.mobchain.memory;

import org.mobAgent.plugin.interfaces.Memory;

import java.util.HashMap;

public class MemoryPluginRegistry {

    private static HashMap<String, Memory> memoryRegistry = new HashMap<>();

    public static void registerMemory( String name , Memory memory ) {
        memoryRegistry.put( name , memory );
    }

    public static Memory getMemory( String name ) {

        try {

            if( memoryRegistry.containsKey( name ) ) {
                return memoryRegistry.get( name );
            }
            return null;

        } catch(Exception ex) {
            return null;
        }

    }

}
