package com.example.myapplication.DAOs.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "plugins", indices = {@Index(value = "name", unique = true)})
public class Plugin {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "version")
    public String version;

    @ColumnInfo(name = "enabled")
    public boolean enabled;

    // Allowed values: 1, 2, 3, 4
    @ColumnInfo(name = "type")
    public int type;

    @ColumnInfo(name = "Path")
    public String path;

    public Plugin(String name, String version, boolean enabled, int type , String path) {
        this.name = name;
        this.version = version;
        this.enabled = enabled;
        this.type = type;
        this.path = path;
    }

    // ── Getters ──

    public int getId() { return id; }
    public String getName() { return name; }
    public String getVersion() { return version; }
    public boolean isEnabled() { return enabled; }
    public int getType() { return type; }
    public String getPath() { return path; }

    // ── Setters ──

    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setVersion(String version) { this.version = version; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setType(int type) { this.type = type; }
    public void setPath(String path) { this.path = path; }
}
