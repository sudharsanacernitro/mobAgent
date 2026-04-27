package com.example.myapplication.DAOs.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "default_plugins",
    foreignKeys = {
        @ForeignKey(
            entity = Plugin.class,
            parentColumns = "id",
            childColumns = "plugin_id",
            onDelete = ForeignKey.CASCADE
        )
    },
    indices = {@Index(value = "plugin_id")}
)
public class DefaultPlugin {

    // "model" or "memory"
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "plugin_type")
    public String pluginType;

    @ColumnInfo(name = "plugin_id")
    public int pluginId;

    public DefaultPlugin(@NonNull String pluginType, int pluginId) {
        this.pluginType = pluginType;
        this.pluginId = pluginId;
    }

    @NonNull
    public String getPluginType() { return pluginType; }
    public int getPluginId() { return pluginId; }

    public void setPluginType(@NonNull String pluginType) { this.pluginType = pluginType; }
    public void setPluginId(int pluginId) { this.pluginId = pluginId; }
}

