package org.mobchain.agentLoopEngine.Handlers.llmHandler;

import org.mobchain.agentLoopEngine.Handler;
import org.mobchain.agentLoopEngine.MsgContext;
import org.mobAgent.plugin.interfaces.FormatterInterface;
import org.mobAgent.plugin.Response;

public class ResponseHandler extends Handler {

    @Override
    public boolean canHandle(MsgContext ctx) {

        try {

            ctx.get("responseString" , String.class );
            ctx.get("model" , FormatterInterface.class ) ;

            return true;

        } catch ( Exception e ) {

            return false;

        }

    }

    @Override
    public void process( MsgContext ctx ) throws Exception {


        try {

            String responseString = ctx.get("responseString" , String.class );

            Response res = ctx.get("model" , FormatterInterface.class ) .fromJSONString( responseString );

            System.out.println("responseHandler :"+res);

            ctx.put( "responseObject" , res );

        } catch( Exception e ){

        }


    }



}
