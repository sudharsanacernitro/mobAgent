package org.mobchain.messages;

import org.json.JSONObject;

import org.mobAgent.plugin.interfaces.Messages;
import org.mobAgent.plugin.constants.MessageType;


public class AiMessages implements Messages {

    private MessageType messageType = MessageType.AI_MESSAGES;
    private String content;
    private JSONObject functionCalls;

    //want to  selected tool


    public AiMessages( String content , JSONObject functionCalls ) {

        this.content = content;
        this.functionCalls = functionCalls;

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

    public JSONObject getFunctionCalls( ) {

        return functionCalls;

    }

    @Override
    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("role", getRole().getType());
            obj.put("content", getContent());
            obj.put("function_calls", getFunctionCalls());
        } catch (Exception e) {
            // Handle exception
            return new JSONObject();
        }

        return obj;

    }


}
