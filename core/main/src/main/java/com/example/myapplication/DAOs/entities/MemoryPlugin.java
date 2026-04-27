package com.example.myapplication.DAOs.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "memory_plugin",
    foreignKeys = @ForeignKey(
        entity = Plugin.class,
        parentColumns = "id",
        childColumns = "plugin_id",
        onDelete = ForeignKey.CASCADE
    )
)
public class MemoryPlugin {

    @PrimaryKey
    @ColumnInfo(name = "plugin_id")
    public int pluginId;

    public MemoryPlugin(int pluginId) {
        this.pluginId = pluginId;
    }

    public int getPluginId() { return pluginId; }
    public void setPluginId(int pluginId) { this.pluginId = pluginId; }
}
