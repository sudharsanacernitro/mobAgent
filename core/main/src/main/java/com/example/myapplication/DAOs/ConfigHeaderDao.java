package com.example.myapplication.DAOs;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.myapplication.DAOs.entities.ConfigHeader;

import java.util.List;

@Dao
public interface ConfigHeaderDao {

    @Insert
    long insert(ConfigHeader header);

    @Insert
    void insertAll(List<ConfigHeader> headers);

    @Update
    void update(ConfigHeader header);

    @Delete
    void delete(ConfigHeader header);

    @Query("SELECT * FROM config_headers WHERE config_id = :configId")
    List<ConfigHeader> getByConfigId(int configId);

    @Query("DELETE FROM config_headers WHERE config_id = :configId")
    void deleteByConfigId(int configId);
}
