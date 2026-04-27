package org.mobchain.agentLoopEngine.Handlers.llmHandler;

import org.json.JSONException;
import org.json.JSONObject;

import org.mobAgent.plugin.interfaces.FormatterInterface;
import org.mobAgent.plugin.interfaces.Memory;
import org.mobchain.agentLoopEngine.Handler;
import org.mobchain.agentLoopEngine.MsgContext;
import org.mobchain.models.ChatModel;
import org.mobAgent.plugin.interfaces.Tool;

import java.util.Formatter;
import java.util.List;

public class RequestGeneratorHandler extends Handler {

    protected boolean canHandle(MsgContext ctx) {

        try {

            ctx.get("model" , ChatModel.class);
            ctx.get( "memory" , Memory.class );
            ctx.get("toolsArray" , List.class);

            return true;

        } catch( Exception e ) {

            return false;

        }

    }


    protected void process(MsgContext ctx) throws JSONException {

        FormatterInterface model = ctx.get("model" , FormatterInterface.class);

        Memory memory = ctx.get( "memory" , Memory.class );
        List<Tool> toolsArray = ctx.get("toolsArray" , List.class);


        JSONObject requestObject = model.toJSON(
                                    memory,
                                    toolsArray
                                    );


        ctx.put( "requestBody" , requestObject );

        System.out.println("RequestGenerator : "+requestObject);


    }



}
