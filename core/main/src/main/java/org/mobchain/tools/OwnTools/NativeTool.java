package org.mobchain.tools.OwnTools;

import org.json.JSONObject;
import org.mobAgent.plugin.interfaces.Tool;

public class NativeTool implements Tool {


    @Override
    public JSONObject runTool(JSONObject args) {
        return null;
    }

    @Override
    public String getToolName() {
        return "";
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public JSONObject getStructuredTool() {
        return null;
    }

    @Override
    public void setStructuredTool(JSONObject structuredTool) {

    }

    @Override
    public String getSkillName() {
        return "";
    }


}
