package com.example.myapplication.DAOs;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.myapplication.DAOs.entities.Plugin;

import java.util.List;

@Dao
public interface PluginDao {

    @Insert
    long insert(Plugin plugin);

    @Update
    void update(Plugin plugin);

    @Delete
    void delete(Plugin plugin);

    @Query("SELECT * FROM plugins")
    List<Plugin> getAll();

    @Query("SELECT * FROM plugins WHERE id = :id")
    Plugin getById(int id);

    @Query("SELECT * FROM plugins WHERE enabled = 1")
    List<Plugin> getEnabled();

    @Query("SELECT * FROM plugins WHERE type = :type")
    List<Plugin> getByType(int type);

    @Query("UPDATE plugins SET enabled = :enabled WHERE id = :id")
    void setEnabled(int id, boolean enabled);
}
