package com.example.myapplication.DAOs.entities;


import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "model_plugin",
    foreignKeys = {
        @ForeignKey(
            entity = Plugin.class,
            parentColumns = "id",
            childColumns = "plugin_id",
            onDelete = ForeignKey.CASCADE
        ),
        @ForeignKey(
            entity = FormatterPlugin.class,
            parentColumns = "plugin_id",
            childColumns = "formatter_id",
            onDelete = ForeignKey.SET_NULL
        )
    },
    indices = {@Index(value = "formatter_id")}
)
public class ModelPlugin {

    @PrimaryKey
    @ColumnInfo(name = "plugin_id")
    public int pluginId;

    @ColumnInfo(name = "model_name")
    public String modelName;

    @ColumnInfo(name = "api_url")
    public String apiUrl;

    @ColumnInfo(name = "is_stream")
    public boolean isStream;

    @ColumnInfo(name = "timeout_ms")
    public int timeoutMs;

    @ColumnInfo(name = "formatter_id")
    public Integer formatterId;


    public ModelPlugin(int pluginId, String modelName, String apiUrl, boolean isStream, int timeoutMs) {
        this.pluginId = pluginId;
        this.modelName = modelName;
        this.apiUrl = apiUrl;
        this.isStream = isStream;
        this.timeoutMs = timeoutMs;
    }

    // ── Getters ──

    public int getPluginId() { return pluginId; }
    public String getModelName() { return modelName; }
    public String getApiUrl() { return apiUrl; }
    public boolean isStream() { return isStream; }
    public int getTimeoutMs() { return timeoutMs; }
    public Integer getFormatterId() { return formatterId; }

    // ── Setters ──

    public void setPluginId(int pluginId) { this.pluginId = pluginId; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
    public void setStream(boolean stream) { isStream = stream; }
    public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
    public void setFormatterId(Integer formatterId) { this.formatterId = formatterId; }
}
