package com.example.myapplication.DAOs;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.myapplication.DAOs.entities.ModelPlugin;
import com.example.myapplication.DAOs.entities.ModelPluginWithFormatterPath;
import com.example.myapplication.DAOs.entities.ModelPluginWithPluginName;

import java.util.List;

@Dao
public interface ModelPluginDao {

    @Insert
    long insert(ModelPlugin modelPlugin);

    @Update
    void update(ModelPlugin modelPlugin);

    @Delete
    void delete(ModelPlugin modelPlugin);

    @Query("SELECT * FROM model_plugin")
    List<ModelPlugin> getAll();

    @Query("SELECT mp.*, p.name AS plugin_name FROM model_plugin mp INNER JOIN plugins p ON mp.plugin_id = p.id")
    List<ModelPluginWithPluginName> getAllWithPluginName();

    @Query("SELECT mp.*, p.Path AS formatter_path FROM model_plugin mp LEFT JOIN plugins p ON mp.formatter_id = p.id WHERE mp.plugin_id = :modelPluginId")
    ModelPluginWithFormatterPath getModelPluginWithFormatterPath(int modelPluginId);

    @Query("SELECT * FROM model_plugin WHERE plugin_id = :pluginId")
    ModelPlugin getByPluginId(int pluginId);
}
