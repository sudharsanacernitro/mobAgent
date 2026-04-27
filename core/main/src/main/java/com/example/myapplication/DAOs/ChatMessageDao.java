package com.example.myapplication.DAOs;


import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.myapplication.DAOs.entities.ChatMessage;

import java.util.List;

@Dao
public interface ChatMessageDao {

    @Insert
    long insert(ChatMessage message);

    @Insert
    void insertAll(List<ChatMessage> messages);

    @Delete
    void delete(ChatMessage message);

    @Query("SELECT * FROM chat_messages WHERE session_id = :sessionId ORDER BY timestamp ASC")
    List<ChatMessage> getBySessionId(int sessionId);

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    List<ChatMessage> getAll();

    @Query("SELECT * FROM chat_messages WHERE session_id = :sessionId AND role = :role ORDER BY timestamp ASC")
    List<ChatMessage> getBySessionAndRole(int sessionId, String role);

    @Query("DELETE FROM chat_messages WHERE session_id = :sessionId")
    void deleteBySessionId(int sessionId);

    @Query("DELETE FROM chat_messages")
    void deleteAll();
}
