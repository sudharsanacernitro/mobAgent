package com.example.myapplication.DAOs.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "formatter_plugin",
    foreignKeys = @ForeignKey(
        entity = Plugin.class,
        parentColumns = "id",
        childColumns = "plugin_id",
        onDelete = ForeignKey.CASCADE
    )
)
public class FormatterPlugin {

    @PrimaryKey
    @ColumnInfo(name = "plugin_id")
    public int pluginId;

    public FormatterPlugin(int pluginId) {
        this.pluginId = pluginId;
    }

    // ── Getters ──

    public int getPluginId() { return pluginId; }

    // ── Setters ──

    public void setPluginId(int pluginId) { this.pluginId = pluginId; }
}
