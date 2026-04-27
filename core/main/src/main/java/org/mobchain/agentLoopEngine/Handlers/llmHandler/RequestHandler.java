package org.mobchain.agentLoopEngine.Handlers.llmHandler;

import org.json.JSONObject;
import org.mobchain.client.Request;
import org.mobchain.agentLoopEngine.Handler;
import org.mobchain.agentLoopEngine.MsgContext;
import org.mobchain.models.ChatModel;
import org.mobAgent.plugin.interfaces.FormatterInterface;

public class RequestHandler extends Handler {


    @Override
    public boolean canHandle(MsgContext ctx) {

        try {
            ctx.get("model", FormatterInterface.class);
            ctx.get( "requestBody" , JSONObject.class );

            return true;

        } catch( Exception e ) {

            return false;

        }

    }

    @Override
    public void process( MsgContext ctx ) throws Exception {


        JSONObject requestObject = ctx.get( "requestBody" , JSONObject.class );

        FormatterInterface model = ctx.get("model", FormatterInterface.class);

        String apiURL = model.getApiURL();

        String responseString = Request.sendRequest( requestObject , apiURL , model.getHeaders() );

        ctx.put( "responseString", responseString );

    }


}
