package org.mobchain.client.parsers;

import org.json.JSONArray;
import org.mobAgent.plugin.interfaces.Tool;
import org.mobchain.client.Response;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

public interface Parser {

    public Response fromJSONString(String jsonString) throws JSONException;


    public  static JSONObject createOpenAICompatibleFunctionTool(
            String functionName,
            String description,
            Map<String, List<String>> parameters,
            String[] requiredParams
    ) throws JSONException {
        JSONObject properties = new JSONObject();

        for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
            String paramName = entry.getKey();
            List<String> values = entry.getValue();

            String paramType = values.size() > 0 ? values.get(0) : "string";
            String paramDescription = values.size() > 1 ? values.get(1) : "No description provided";

            JSONObject paramObject = new JSONObject()
                    .put("type", paramType)
                    .put("description", paramDescription);

            properties.put(paramName, paramObject);
        }

        JSONObject parametersObject = new JSONObject()
                .put("type", "object")
                .put("required", new JSONArray(requiredParams))
                .put("properties", properties);

        JSONObject functionObject = new JSONObject()
                .put("name", functionName)
                .put("description", description)
                .put("parameters", parametersObject);

        return new JSONObject()
                .put("type", "function")
                .put("function", functionObject);
    }


    public default JSONObject toJSON(String modelName , Boolean isStream , List<Tool> toolsList, List<JSONObject> memory) throws JSONException {
        // Convert tools to JSONArray
        JSONArray toolsArray = new JSONArray();
        for (Tool tool : toolsList) {
            toolsArray.put(tool.getStructuredTool());
        }

        // Convert memory to JSONArray
        JSONArray memoryArray = new JSONArray();
        for (JSONObject msg : memory) {
            memoryArray.put(msg);
        }

        JSONObject jsonOutput = new JSONObject();
        jsonOutput.put("model", modelName);
        jsonOutput.put("messages", memoryArray);
        jsonOutput.put("stream", isStream.booleanValue());
        jsonOutput.put("tools", toolsArray);

        return jsonOutput;
    }

}

