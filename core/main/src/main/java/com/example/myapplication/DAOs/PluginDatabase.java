package com.example.myapplication.DAOs;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.myapplication.DAOs.entities.ChatMessage;
import com.example.myapplication.DAOs.entities.ChatSession;
import com.example.myapplication.DAOs.entities.ConfigHeader;
import com.example.myapplication.DAOs.entities.FormatterPlugin;
import com.example.myapplication.DAOs.entities.MemoryPlugin;
import com.example.myapplication.DAOs.entities.ModelPlugin;
import com.example.myapplication.DAOs.entities.DefaultPlugin;
import com.example.myapplication.DAOs.entities.Plugin;

@Database(
    entities = {Plugin.class, ModelPlugin.class, ConfigHeader.class, ChatMessage.class, ChatSession.class, MemoryPlugin.class, FormatterPlugin.class, DefaultPlugin.class},
    version = 8,
    exportSchema = false
)
public abstract class PluginDatabase extends RoomDatabase {

    private static volatile PluginDatabase INSTANCE;

    public abstract PluginDao pluginDao();
    public abstract ModelPluginDao modelPluginDao();
    public abstract ConfigHeaderDao configHeaderDao();
    public abstract ChatMessageDao chatMessageDao();
    public abstract MemoryPluginDao memoryPluginDao();
    public abstract FormatterPluginDao formatterDao();
    public abstract DefaultPluginDao defaultPluginDao();
    public abstract ChatSessionDao chatSessionDao();

    public static PluginDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (PluginDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        PluginDatabase.class,
                        "plugin_database"
                    ).fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }
}
