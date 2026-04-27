package org.mobchain.tools.OwnTools;

import org.mobchain.client.parsers.Parser;

import org.mobAgent.plugin.interfaces.Tool;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RAGTool implements Tool{


    public static final String toolName = "rag_tool";
    public static final String description = "Tool to give weather report for a city";


    private JSONObject structuredTool;


     public RAGTool(){

        Map<String, List<String>> parameters = new HashMap<>();
        parameters.put("city", Arrays.asList("string", "city mentioned in the input"));
        parameters.put("country", Arrays.asList("string", "country mentioned in the input"));

        try {

            structuredTool = Parser.createOpenAICompatibleFunctionTool(
                    toolName,
                    description,
                    parameters,
                    parameters.keySet().toArray(new String[0])
            );

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

    }


    @Override
    public String getToolName( ) {

        return toolName;

    }


    @Override
    public String getDescription( ) {

        return description;

    }

    @Override
    public JSONObject getStructuredTool( ) {

        return structuredTool;

    }

    @Override
    public void setStructuredTool(JSONObject structuredTool) {

    }

    @Override
    public String getSkillName() {
        return "";
    }


    @Override
    public JSONObject runTool( JSONObject args) {

        if( ! args.has("city") || ! args.has( "country" ) ) {

            System.out.println(" Error in tool ");

            throw new IllegalArgumentException("The inputs 'city' and 'country' is not present");

        }

        JSONObject res = new JSONObject();


        try{

            String city = args.getString("city");
            String country = args.getString("country");

            System.out.println("RAG Tool called: "+city+" : "+country);


            res.put("weather" , "it seems to be rainy.");

        } catch(  JSONException e ) {

            return  null;

        }

        return res;


    }

}
