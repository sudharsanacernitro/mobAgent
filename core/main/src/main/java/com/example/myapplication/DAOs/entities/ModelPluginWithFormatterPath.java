package com.example.myapplication.DAOs.entities;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;

public class ModelPluginWithFormatterPath {

    @Embedded
    public ModelPlugin modelPlugin;


    @ColumnInfo(name = "formatter_path")
    public String formatterPath;

    public ModelPlugin getModelPlugin() { return modelPlugin; }
    public String getPluginName() { return formatterPath; }
}
