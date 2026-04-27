package com.example.myapplication.DAOs.entities;


import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "config_headers",
    foreignKeys = @ForeignKey(
        entity = ModelPlugin.class,
        parentColumns = "plugin_id",
        childColumns = "config_id",
        onDelete = ForeignKey.CASCADE
    ),
    indices = @Index(value = "config_id")
)
public class ConfigHeader {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "config_id")
    public int configId;

    @ColumnInfo(name = "header_key")
    public String headerKey;

    @ColumnInfo(name = "header_value")
    public String headerValue;

    public ConfigHeader(int configId, String headerKey, String headerValue) {
        this.configId = configId;
        this.headerKey = headerKey;
        this.headerValue = headerValue;
    }

    // ── Getters ──

    public int getId() { return id; }
    public int getConfigId() { return configId; }
    public String getHeaderKey() { return headerKey; }
    public String getHeaderValue() { return headerValue; }

    // ── Setters ──

    public void setId(int id) { this.id = id; }
    public void setConfigId(int configId) { this.configId = configId; }
    public void setHeaderKey(String headerKey) { this.headerKey = headerKey; }
    public void setHeaderValue(String headerValue) { this.headerValue = headerValue; }
}
