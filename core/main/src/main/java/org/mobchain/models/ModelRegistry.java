package org.mobchain.models;

import java.util.HashMap;

public class ModelRegistry {


    private static HashMap<String, ChatModel> registry = new HashMap<>();

    public static void registerModel( ChatModel model ) {
        registry.put(model.getModelName(), model);
    }

    public static ChatModel getModel( String modelName ) {
        return registry.get(modelName);
    }


    public static HashMap<String, ChatModel> getRegistry() {
        return new HashMap<>(registry);
    }


}
