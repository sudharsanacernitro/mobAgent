package com.example.myapplication.DAOs.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "chat_sessions",
    foreignKeys = {
        @ForeignKey(
            entity = Plugin.class,
            parentColumns = "id",
            childColumns = "model_plugin_id",
            onDelete = ForeignKey.CASCADE
        ),
        @ForeignKey(
            entity = Plugin.class,
            parentColumns = "id",
            childColumns = "memory_plugin_id",
            onDelete = ForeignKey.SET_NULL
        )
    },
    indices = {
        @Index(value = "model_plugin_id"),
        @Index(value = "memory_plugin_id")
    }
)
public class ChatSession {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "session_name")
    public String sessionName;

    @ColumnInfo(name = "model_plugin_id")
    public int modelPluginId;

    @ColumnInfo(name = "memory_plugin_id")
    public Integer memoryPluginId;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    public ChatSession(String sessionName, int modelPluginId, Integer memoryPluginId, long createdAt) {
        this.sessionName = sessionName;
        this.modelPluginId = modelPluginId;
        this.memoryPluginId = memoryPluginId;
        this.createdAt = createdAt;
    }

    // ── Getters ──
    public int getId() { return id; }
    public String getSessionName() { return sessionName; }
    public int getModelPluginId() { return modelPluginId; }
    public Integer getMemoryPluginId() { return memoryPluginId; }
    public long getCreatedAt() { return createdAt; }

    // ── Setters ──
    public void setId(int id) { this.id = id; }
    public void setSessionName(String sessionName) { this.sessionName = sessionName; }
    public void setModelPluginId(int modelPluginId) { this.modelPluginId = modelPluginId; }
    public void setMemoryPluginId(Integer memoryPluginId) { this.memoryPluginId = memoryPluginId; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}

