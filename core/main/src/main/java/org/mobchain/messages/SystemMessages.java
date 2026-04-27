package org.mobchain.messages;


import org.json.JSONObject;
import org.mobAgent.plugin.interfaces.Messages;
import org.mobAgent.plugin.constants.MessageType;

public class SystemMessages implements Messages {

    private final MessageType type = MessageType.SYSTEM_MESSAGES ;
    private String content;


    public SystemMessages( String content ) {

        this.content = content;

    }

    @Override
    public MessageType getRole() {

        return this.type;

    }

    @Override
    public String getContent( ) {

        return this.content;

    }

    @Override
    public void setContent( String content ) {

        this.content = content;

    }


    public JSONObject getFunctionCalls() {
        return null;
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


}
