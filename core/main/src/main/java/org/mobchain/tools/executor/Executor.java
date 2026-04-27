package org.mobchain.tools.executor;

import org.mobAgent.plugin.Response;
import org.mobAgent.plugin.interfaces.Memory;
import org.mobAgent.plugin.interfaces.Tool;
import org.mobchain.messages.ToolMessages;
import org.mobchain.tools.ToolsManager;
import org.json.JSONException;
import org.json.JSONObject;



import java.util.List;

public class Executor {


    public static void execute(Response res ,  Memory memory ) throws JSONException {


        List< Response.Function > functionList = res.getFunctions();

        if( functionList.isEmpty() ) return;

        Response.Function function = functionList.get(0);

        Tool tool = ToolsManager.getToolByName( function.getFunctionName() );

        assert tool != null;

        JSONObject toolOutput = tool.runTool( function.getArg() );


        memory.addToolMessage( new ToolMessages( function.getFunctionName() , toolOutput ) );




    }

}
