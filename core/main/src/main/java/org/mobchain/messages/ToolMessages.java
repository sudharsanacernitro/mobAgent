package org.mobchain.messages;

import org.json.JSONObject;
import org.mobAgent.plugin.interfaces.Messages;
import org.mobAgent.plugin.constants.MessageType;
public class ToolMessages  implements Messages{


    private MessageType role = MessageType.TOOL_MESSAGES;
    private String toolName;
    private JSONObject toolMessage;

    public ToolMessages(String toolName, JSONObject content) {

        this.toolName = toolName;
        this.toolMessage = content;

    }

    public MessageType getRole() {
        return role;
    }



    public JSONObject getFunctionCalls() {
        return toolMessage;
    }

    public String getToolName( ) {

        return this.toolName;

    }

    @Override
    public JSONObject toJson() {

        JSONObject obj = new JSONObject();
        try {
            obj.put("role", getRole().getType());
            obj.put("content", getContent());
        } catch (Exception e) {
            // Handle exception
            return obj;
        }

        return obj;

    }

    public String getContent( ) {

        return toolMessage.toString();

    }

    public void setContent( String setContent ) {

    }



}
