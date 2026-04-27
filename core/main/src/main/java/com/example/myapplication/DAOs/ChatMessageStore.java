package com.example.myapplication.DAOs;

import android.content.Context;

import com.example.myapplication.DAOs.entities.ChatMessage;

import org.mobAgent.plugin.interfaces.Messages;
import org.mobAgent.plugin.interfaces.DBMessageStore;

import org.mobchain.messages.AiMessages;
import org.mobchain.messages.HumanMessages;

import java.util.ArrayList;
import java.util.List;

public class ChatMessageStore implements DBMessageStore {

    private final ChatMessageDao dao;
    private int sessionId;

    public ChatMessageStore(Context context) {
        this.dao = PluginDatabase.getInstance(context).chatMessageDao();
        this.sessionId = -1;
    }

    public ChatMessageStore(Context context, int sessionId) {
        this.dao = PluginDatabase.getInstance(context).chatMessageDao();
        this.sessionId = sessionId;
    }

    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }


    public int getSessionId() {
        return sessionId;
    }

    public void saveHumanMessage(String content) {
        ChatMessage msg = new ChatMessage(sessionId, "user", content, System.currentTimeMillis());
        dao.insert(msg);
    }

    public void saveAiMessage(String content) {
        ChatMessage msg = new ChatMessage(sessionId, "assistant", content, System.currentTimeMillis());
        dao.insert(msg);
    }

    public List<Messages> getSessionMessages() {
        List<ChatMessage> rows = dao.getBySessionId(sessionId);
        return toMessagesList(rows);
    }

    public List<Messages> getAllMessages() {
        List<ChatMessage> rows = dao.getAll();
        return toMessagesList(rows);
    }

    public List<Messages> getHumanMessages() {
        List<ChatMessage> rows = dao.getBySessionAndRole(sessionId, "user");
        return toMessagesList(rows);
    }

    public List<Messages> getAiMessages() {
        List<ChatMessage> rows = dao.getBySessionAndRole(sessionId, "assistant");
        return toMessagesList(rows);
    }

    public void clearSession() {
        dao.deleteBySessionId(sessionId);
    }

    public void clearAll() {
        dao.deleteAll();
    }

    private List<Messages> toMessagesList(List<ChatMessage> rows) {
        List<Messages> result = new ArrayList<>();
        for (ChatMessage row : rows) {
            if ("user".equals(row.role)) {
                result.add(new HumanMessages(row.content));
            } else if ("assistant".equals(row.role)) {
                result.add(new AiMessages(row.content, null));
            }
        }
        return result;
    }
}
