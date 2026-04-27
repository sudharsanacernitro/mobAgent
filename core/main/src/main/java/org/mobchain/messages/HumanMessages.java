package org.mobchain.messages;

import org.json.JSONObject;

import org.mobAgent.plugin.interfaces.Messages;
import org.mobAgent.plugin.constants.MessageType;


public class HumanMessages implements Messages {


    private MessageType messageType = MessageType.HUMAN_MESSAGES;
    private String content;

    //want to declare tools specifications


    public HumanMessages( String content ) {

        this.content = content;

    }

    @Override
    public MessageType getRole( ) {

        return this.messageType;

    }

    @Override
    public String getContent() {

        return this.content;

    }

    @Override
    public void setContent( String content ) {

        this.content = content;

    }

    @Override
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
