package com.example.myapplication.DAOs;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.myapplication.DAOs.entities.FormatterPlugin;

import java.util.List;

@Dao
public interface FormatterPluginDao {

    @Insert
    long insert(FormatterPlugin formatter);

    @Update
    void update(FormatterPlugin formatter);

    @Delete
    void delete(FormatterPlugin formatter);

    @Query("SELECT * FROM formatter_plugin")
    List<FormatterPlugin> getAll();

    @Query("SELECT * FROM formatter_plugin WHERE plugin_id = :pluginId")
    FormatterPlugin getByPluginId(int pluginId);
}
