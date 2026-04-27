package org.mobchain.agentLoopEngine.Handlers;

import org.mobAgent.plugin.Response;
import org.mobAgent.plugin.interfaces.Memory;
import org.mobAgent.plugin.interfaces.FormatterInterface;

import org.mobchain.agentLoopEngine.Handler;
import org.mobchain.agentLoopEngine.Handlers.llmHandler.RequestGeneratorHandler;
import org.mobchain.agentLoopEngine.Handlers.llmHandler.RequestHandler;
import org.mobchain.agentLoopEngine.Handlers.llmHandler.ResponseHandler;
import org.mobchain.agentLoopEngine.MsgContext;

public class LlmHandler extends Handler {


    @Override
    public boolean canHandle(MsgContext ctx) {

        try {


            ctx.get("model" , FormatterInterface.class);
            ctx.get( "memory" , Memory.class );

            return true;

        } catch( Exception e ) {

            return false;

        }

    }




    @Override
    public void process( MsgContext ctx ) throws Exception {

        RequestGeneratorHandler requestGeneratorHandler = new RequestGeneratorHandler();
        RequestHandler requestHandler = new RequestHandler();
        ResponseHandler responseHandler = new ResponseHandler();

        requestGeneratorHandler.link(requestHandler);
        requestHandler.link(responseHandler);

        MsgContext clonedCtx = ctx.clone();

        requestGeneratorHandler.handle( clonedCtx );

        try {

            Response res =  clonedCtx.get("responseObject" , Response.class);

            ctx.get( "memory" , Memory.class ).addAiMessage( res.getJsonAIMessage() );

            if( res.getFunctions().isEmpty() ) {

                ctx.put( "ResponseContent" , res.getContent() );

                System.out.println("NO function call : End");

                return;

            }

            System.out.println("Function call present : "+res.getFunctions());
            ctx.put( "responseObject" , res );



        } catch( Exception e ) {

            ctx.put( "exception" , true );

        }

    }


}
