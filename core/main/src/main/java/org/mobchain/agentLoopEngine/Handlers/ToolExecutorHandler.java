package org.mobchain.agentLoopEngine.Handlers;

import org.mobAgent.plugin.Response;
import org.mobAgent.plugin.interfaces.Memory;

import org.mobchain.agentLoopEngine.Handler;
import org.mobchain.agentLoopEngine.MsgContext;
import org.mobchain.tools.executor.Executor;

public class ToolExecutorHandler extends Handler {

    @Override
    public boolean canHandle(MsgContext ctx) {

       try{

           ctx.get("responseObject" , Response.class );
           ctx.get("memory" , Memory.class );


           return true;

       } catch( Exception e ) {

           return false;

       }

    }

    @Override
    public void process( MsgContext ctx ) throws Exception {

        Response res= ctx.get("responseObject" , Response.class );
        Memory memory =  ctx.get("memory" , Memory.class );


        Executor.execute( res ,  memory  );

        ctx.remove("responseObject");

    }



}
