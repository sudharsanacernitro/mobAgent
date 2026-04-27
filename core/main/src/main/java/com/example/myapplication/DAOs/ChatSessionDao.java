package com.example.myapplication.DAOs;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.myapplication.DAOs.entities.ChatSession;

import java.util.List;

@Dao
public interface ChatSessionDao {

    @Insert
    long insert(ChatSession session);

    @Update
    void update(ChatSession session);

    @Delete
    void delete(ChatSession session);

    @Query("SELECT * FROM chat_sessions ORDER BY created_at DESC")
    List<ChatSession> getAll();

    @Query("SELECT * FROM chat_sessions WHERE id = :id")
    ChatSession getById(int id);

    @Query("DELETE FROM chat_sessions WHERE id = :id")
    void deleteById(int id);

    @Query("UPDATE chat_sessions SET model_plugin_id = :modelId, memory_plugin_id = :memoryId WHERE id = :sessionId")
    void updatePlugins(int sessionId, int modelId, Integer memoryId);
}

