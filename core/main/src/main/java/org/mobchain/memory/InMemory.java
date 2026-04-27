package org.mobchain.memory;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import org.mobAgent.plugin.interfaces.Memory;
import org.mobAgent.plugin.interfaces.DBMessageStore;
import org.mobAgent.plugin.interfaces.Messages;


public class InMemory implements Memory{

    List<JSONObject> memory ;

    int MEMORY_SIZE = 30;
    int sessionId = 0;

    DBMessageStore dbMessageStore;

    public InMemory(DBMessageStore dbMessageStore) {
        this.memory = new ArrayList<>();
        this.dbMessageStore = dbMessageStore;
    }


    public void setSessionId(int sessionId) {

        this.sessionId = sessionId;
        memory.clear();
        dbMessageStore.setSessionId(sessionId);

        List<Messages> msgList = dbMessageStore.getSessionMessages();

        for (Messages msg : msgList) {
            memory.add(msg.toJson());
        }



    }


    @Override
    public void setSystemPrompt(Messages systemPrompt) {


        if (!memory.isEmpty() && memory.get(0).optString("role").equals("system")) {

            memory.set(0, systemPrompt.toJson());

        } else {

            memory.add(0, systemPrompt.toJson());

        }
    }

    @Override
    public void addHumanMessage(Messages humanMessage) throws JSONException {

        try{



            memory.add(humanMessage.toJson());

        } catch (Exception e){
            return;
        }
    }

    @Override
    public void addAiMessage(Messages aiMessage) throws JSONException {


        memory.add(aiMessage.toJson());

        if (memory.size() > MEMORY_SIZE && memory.size() > 1) {
            memory.remove(1);
        }

    }

    @Override
    public void addAiMessage(JSONObject aiMessage) {
        memory.add(aiMessage);
        if (memory.size() > MEMORY_SIZE && memory.size() > 1) {
            memory.remove(1);
        }

    }

    @Override
    public void addToolMessage(Messages toolMessage) throws JSONException {

            memory.add(toolMessage.toJson());

    }

    @Override
    public List<JSONObject> getAllMessages() {
        return memory;
    }

    @Override
    public void clearMemory() {

    }
}
