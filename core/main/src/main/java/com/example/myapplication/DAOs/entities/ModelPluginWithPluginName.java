package com.example.myapplication.DAOs.entities;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;

public class ModelPluginWithPluginName {

    @Embedded
    public ModelPlugin modelPlugin;

    @ColumnInfo(name = "plugin_name")
    public String pluginName;

    public ModelPlugin getModelPlugin() { return modelPlugin; }
    public String getPluginName() { return pluginName; }
}

