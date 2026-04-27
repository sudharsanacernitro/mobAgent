package com.example.myapplication.DAOs;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.myapplication.DAOs.entities.MemoryPlugin;

import java.util.List;

@Dao
public interface MemoryPluginDao {

    @Insert
    long insert(MemoryPlugin memoryPlugin);

    @Update
    void update(MemoryPlugin memoryPlugin);

    @Delete
    void delete(MemoryPlugin memoryPlugin);

    @Query("SELECT * FROM memory_plugin")
    List<MemoryPlugin> getAll();

    @Query("SELECT * FROM memory_plugin WHERE plugin_id = :pluginId")
    MemoryPlugin getByPluginId(int pluginId);

    @Query("DELETE FROM memory_plugin WHERE plugin_id = :pluginId")
    void deleteByPluginId(int pluginId);
}
