package org.mobchain.agentLoopEngine.Handlers;

import org.mobchain.agentLoopEngine.Handler;

public class ExecutorChain {


    public static Handler getChain() {

        LlmHandler llmHandler = new LlmHandler();
        ToolExecutorHandler toolExecutorHandler = new ToolExecutorHandler();

        llmHandler.link( toolExecutorHandler );
        toolExecutorHandler.link(llmHandler);

        return llmHandler;

    }


}
