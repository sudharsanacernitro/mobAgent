package org.mobchain.models;

import org.json.JSONException;
import org.json.JSONObject;
import org.mobAgent.plugin.interfaces.Tool;
import org.mobchain.client.Response;
import org.mobAgent.plugin.interfaces.Memory;

import java.util.HashMap;
import java.util.List;

public interface ChatModel {


    public String getApiURL();

    public String getModelName();

    public HashMap<String,String> getHeaders();

    public JSONObject toJSON(Memory memory , List<Tool> toolList) throws JSONException;

    public Response fromJSONString( String jsonString ) throws JSONException;

    public JSONObject getStrcturedTool( JSONObject tool ) throws JSONException;


}
