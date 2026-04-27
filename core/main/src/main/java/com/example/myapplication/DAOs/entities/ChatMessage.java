package com.example.myapplication.DAOs.entities;


import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "chat_messages",
    foreignKeys = @ForeignKey(
        entity = ChatSession.class,
        parentColumns = "id",
        childColumns = "session_id",
        onDelete = ForeignKey.CASCADE
    ),
    indices = @Index(value = "session_id")
)
public class ChatMessage {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "session_id")
    public int sessionId;

    @ColumnInfo(name = "role")
    public String role;

    @ColumnInfo(name = "content")
    public String content;

    @ColumnInfo(name = "timestamp")
    public long timestamp;

    public ChatMessage(int sessionId, String role, String content, long timestamp) {
        this.sessionId = sessionId;
        this.role = role;
        this.content = content;
        this.timestamp = timestamp;
    }

    // ── Getters ──

    public long getId() { return id; }
    public int getSessionId() { return sessionId; }
    public String getRole() { return role; }
    public String getContent() { return content; }
    public long getTimestamp() { return timestamp; }

    // ── Setters ──

    public void setId(long id) { this.id = id; }
    public void setSessionId(int sessionId) { this.sessionId = sessionId; }
    public void setRole(String role) { this.role = role; }
    public void setContent(String content) { this.content = content; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
