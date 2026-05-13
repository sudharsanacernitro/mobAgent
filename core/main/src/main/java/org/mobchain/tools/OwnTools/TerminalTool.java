package org.mobchain.tools.OwnTools;

import com.rk.terminal.service.TerminalSessionManager;
import com.rk.terminal.utils.SessionCodes;
import com.termux.terminal.TerminalSynchronousSessionHandler;

import org.json.JSONArray;
import org.json.JSONObject;

import org.mobAgent.plugin.interfaces.Tool;

public class TerminalTool implements Tool {

    private String toolName, description , toolPath;
    private JSONObject structuredTool;

    private JSONArray requiredParameters;

    private String skillName;

    public TerminalTool(String toolName , String toolPath , String description , JSONObject structuredTool , JSONArray requiredParameters , String skillName ) {


        this.toolName = toolName;
        this.toolPath = toolPath;
        this.description = description;
        this.structuredTool = structuredTool;
        this.requiredParameters = requiredParameters;
        this.skillName = skillName;

    }



    @Override
    public JSONObject runTool(JSONObject args) {

        // Validate required parameters
        JSONObject errorObj = new JSONObject();
        for (int i = 0; i < requiredParameters.length(); i++) {
            String paramName = requiredParameters.optString(i);
            if (!args.has(paramName)) {
                try {
                    errorObj.put("error", "Missing required parameter: " + paramName);
                } catch (Exception jsonException) {
                    // Handle potential JSON exceptions here
                }
            }
        }
        if( errorObj.length() != 0 ) return errorObj;


        //Tool calling starts here
        String RESULT_DELIMITER = "==Result==";

        TerminalSessionManager terminalSessionManager = TerminalSessionManager.getInstance();

        TerminalSynchronousSessionHandler session = (TerminalSynchronousSessionHandler) terminalSessionManager.getSession("tools");

        StringBuilder commandBuilder = new StringBuilder("sh /root/ToolsWrapper.sh ");


        commandBuilder.append(toolPath).append(" ").append("'").append(args.toString()).append("'");

        String output = session.executeCommandSync( commandBuilder.toString() ,20000 );

        int firstIdx = output.indexOf(RESULT_DELIMITER);      // FIRST occurrence
        int lastIdx  = output.lastIndexOf(RESULT_DELIMITER);  // LAST occurrence

        if (firstIdx != -1 && lastIdx != -1 && lastIdx > firstIdx) {
            output =  output
                    .substring(firstIdx + RESULT_DELIMITER.length(), lastIdx)
                    .trim();
        }

        System.out.println(output);

        try {

            return new JSONObject(output);

        }catch (Exception e) {

            try {
                errorObj.put("error", "Failed to parse output: " + e.getMessage());
                errorObj.put("raw_output", output);
            } catch (Exception jsonException) {
                // Handle potential JSON exceptions here
            }
            return errorObj;

        }


    }

    public String getToolPath( ) {
        return toolPath;
    }

    public String getToolName( ) {
        return toolName;
    }

    public String getDescription( ) {
        return description;
    }

    public String getSkillName(){
        return skillName;
    }

    @Override
    public JSONObject getStructuredTool() {
        return structuredTool;
    }

    @Override
    public void setStructuredTool(JSONObject structuredTool) {
        this.structuredTool = structuredTool;
    }



}
