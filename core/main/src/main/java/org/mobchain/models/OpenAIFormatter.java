package org.mobchain.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.mobAgent.plugin.Response;
import org.mobAgent.plugin.interfaces.Memory;
import org.mobAgent.plugin.interfaces.FormatterInterface;
import org.mobAgent.plugin.interfaces.Tool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class OpenAIFormatter implements FormatterInterface {

    private String apiUrl = "http://localhost:11434";
    private String model ;
    private HashMap<String,String> headers = null;
    private boolean stream = false;



    private OpenAIFormatter(Builder builder ) {

        this.apiUrl = builder.base_url;
        this.model = builder.modelName;
        this.stream = builder.isStream;
        this.headers = builder.headers;


    }

    @Override
    public String getApiURL() {
        return apiUrl;
    }

    @Override
    public String getModelName() {

        return model;

    }

    @Override
    public HashMap<String, String> getHeaders() {
        return headers;
    }

    public boolean isStream() {
        return stream;
    }


    @Override
    public JSONObject getStrcturedTool( JSONObject tool ) throws JSONException {

        //It is already in OpenAI format, so we can return it as is.
        //If upi need to convert to another format then use this function to convert it.

        try {
            return tool;
        }catch (Exception e) {
            throw new JSONException("Invalid tool format: " + tool.toString());
        }

    }

    @Override
    public Response fromJSONString(String jsonString) throws JSONException {
        System.out.print(jsonString);

        JSONObject root = new JSONObject(jsonString);

        JSONObject message = root.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message");

        String content = message.optString("content", null);
        JSONArray toolCalls = message.optJSONArray("tool_calls");

        List<Response.Function> functionObjects = new ArrayList<>();

        if (toolCalls != null) {
            for (int index = 0; index < toolCalls.length(); index++) {
                JSONObject toolCall = toolCalls.getJSONObject(index);
                JSONObject function = toolCall.getJSONObject("function");

                String functionName = function.getString("name");

                // ⭐ IMPORTANT FIX ⭐
                String argumentsString = function.getString("arguments");

                // Convert "arguments" string → JSONObject
                JSONObject arguments;
                try {
                    arguments = new JSONObject(argumentsString);
                } catch (Exception ex) {
                    throw new JSONException("Invalid JSON in tool arguments: " + argumentsString);
                }

                Response.Function functionObject = new Response.Function(functionName, arguments);
                functionObjects.add(functionObject);
            }
        }

        return new Response(content, functionObjects, message);
    }


    @Override
    public  JSONObject toJSON(Memory memory , List<Tool> toolsList ) throws JSONException {

        List<JSONObject> memoryAsJSON = memory.getAllMessages();

        JSONArray toolsArray = new JSONArray();
        for (Tool tool : toolsList) {
            toolsArray.put(tool.getStructuredTool());
        }

        // Convert memory to JSONArray
        JSONArray memoryArray = new JSONArray();
        for (JSONObject msg : memoryAsJSON) {
            memoryArray.put(msg);
        }

        JSONObject jsonOutput = new JSONObject();
        jsonOutput.put("model", model);
        jsonOutput.put("messages", memoryArray);
        jsonOutput.put("stream", stream);
        jsonOutput.put("tools", toolsArray);

        return jsonOutput;
    }



    public static Builder builder( ) {

        return new Builder();

    }


    public static class Builder {

        private String base_url = "";
        private String modelName;
        private boolean isStream = false;

        private HashMap<String,String> headers = null;

        public Builder baseURL( String base_url ) {

            this.base_url = base_url;
            return this;

        }

        public Builder model( String model ) {

            this.modelName = model;
            return this;

        }

        public Builder stream( boolean allowStream ) {

            this.isStream = allowStream;
            return this;

        }

        public Builder addHeader( String key , String value ) {

            if(this.headers == null){
                this.headers = new HashMap<>();
            }

            this.headers.put(key, value);
            return this;

        }



        public OpenAIFormatter build() throws IllegalArgumentException{

            if( modelName == null ) {

                throw new IllegalArgumentException( "Model is not initialized" );

            }

            return new OpenAIFormatter( this );


        }


    }
}
