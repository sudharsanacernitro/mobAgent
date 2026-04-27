package com.example.myapplication.DAOs;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.myapplication.DAOs.entities.DefaultPlugin;

@Dao
public interface DefaultPluginDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void setDefault(DefaultPlugin defaultPlugin);

    @Query("SELECT plugin_id FROM default_plugins WHERE plugin_type = :type")
    Integer getDefaultPluginId(String type);

    @Query("DELETE FROM default_plugins WHERE plugin_type = :type")
    void clearDefault(String type);
}

