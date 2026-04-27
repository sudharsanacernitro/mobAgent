package org.mobchain.models;

import org.json.JSONObject;
import org.mobAgent.plugin.interfaces.FormatterInterface;
import org.mobchain.agentLoopEngine.Handler;
import org.mobchain.agentLoopEngine.Handlers.ExecutorChain;
import org.mobchain.agentLoopEngine.MsgContext;
import org.mobAgent.plugin.interfaces.Memory;
import org.mobchain.messages.HumanMessages;
import org.mobAgent.plugin.interfaces.Tool;

import java.util.ArrayList;
import java.util.List;

public class ModelInterface {

    private FormatterInterface model;
    private Memory memory;

    private List<Tool> tools = new ArrayList<>();

    public ModelInterface(Builder builder ) {

        this.model = builder.model;
        this.memory = builder.memory;

        for( Tool tool : builder.tools ) {

            try {

                JSONObject updatedToolStructure = model.getStrcturedTool(tool.getStructuredTool());
                tool.setStructuredTool(updatedToolStructure);

            }catch( Exception e ){

            }

        }

        this.tools = builder.tools;

    }

    public FormatterInterface getModel(){

        return this.model;

    }

    public Memory getMemory() {
        return this.memory;
    }

    public List<Tool> getTools() {
        return this.tools;
    }


    public String chat( HumanMessages message  )  {



        try{

            memory.addHumanMessage( message );

            MsgContext ctx = new MsgContext();

            ctx.put("model" , getModel() );
            ctx.put("toolsArray" ,  tools );
            ctx.put("memory" , getMemory() );


            System.out.println("before chain");
            Handler handler = ExecutorChain.getChain();


            handler.handle(ctx);

            System.out.println("After chain");

            return ctx.get( "ResponseContent" , String.class );

        } catch( Exception e ) {

            System.out.println( e.getMessage() );

        }

        return "";

    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder{

        FormatterInterface model;
        Memory memory;
        private List<Tool> tools = new ArrayList<>();


        public Builder setModel( FormatterInterface model ) {
            this.model = model;
            return this;
        }

        public Builder setMemory( Memory memory ) {
            this.memory = memory;
            return this;
        }

        public Builder addTool( Tool tool ) {
            this.tools.add( tool );
            return this;
        }

        public Builder addTools( List<Tool> tools ) {
            this.tools.addAll( tools );
            return this;
        }

        public ModelInterface build() {
            if ( this.model == null ) {
                throw new IllegalStateException( "Model must be set" );
            }
            if ( this.memory == null ) {
                throw new IllegalStateException( "Memory must be set" );
            }
            return new ModelInterface( this );
        }


    }

}
