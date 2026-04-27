package org.mobchain.client;

import org.json.JSONObject;

import java.util.List;

public class Response {

   private String content;
   private List< Function > functions;
   private JSONObject jsonAIMessage ;


   public Response( String content , List<Function>  functions , JSONObject jsonAIMessage ) {

       this.content = content;
       this.functions = functions;
       this.jsonAIMessage = jsonAIMessage;

   }

    public JSONObject getJsonAIMessage() {
        return jsonAIMessage;
    }

    public void setJsonAIMessage(JSONObject jsonAIMessage) {
        this.jsonAIMessage = jsonAIMessage;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List< Function > getFunctions() {
        return functions;
    }

    public void setFunctions(List< Function > functions) {
        this.functions = functions;
    }

    public void addFunction( Function function ) {

        this.functions.add( function );

    }

    @Override
    public String toString( ) {

       return "Content : "+ content
               + " Functions : "+functions;

    }

    public static class Function {

       String functionName;
       JSONObject arg;

       public Function( String functionName , JSONObject arg ) {

           this.functionName = functionName;
           this.arg = arg;

       }

       public String getFunctionName() {
           return functionName;
       }

       public void setFunctionName(String functionName) {
           this.functionName = functionName;
       }

       public JSONObject getArg() {
           return arg;
       }

       public void setArg(JSONObject arg) {
           this.arg = arg;
       }

       @Override
        public String toString() {

           return "functionName : " + functionName
                   + "Arguments :" +arg;


       }

   }

}
