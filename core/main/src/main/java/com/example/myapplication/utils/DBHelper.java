package com.example.myapplication.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "mob_chain.db";
    private static final int DB_VERSION = 3;

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }


    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // Tools table
        db.execSQL(
                "CREATE TABLE tools (" +
                        "toolId INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "toolName TEXT NOT NULL UNIQUE)"
        );

        // Sessions table
        db.execSQL(
                "CREATE TABLE sessions (" +
                        "sessionId INTEGER PRIMARY KEY AUTOINCREMENT , isActive INTEGER DEFAULT 1 )"
        );

        // Mapping table
        db.execSQL(
                "CREATE TABLE session_tools (" +
                        "sessionId INTEGER NOT NULL, " +
                        "toolId INTEGER NOT NULL, " +
                        "PRIMARY KEY (sessionId, toolId), " +
                        "FOREIGN KEY (sessionId) REFERENCES sessions(sessionId) ON DELETE CASCADE, " +
                        "FOREIGN KEY (toolId) REFERENCES tools(toolId) ON DELETE CASCADE)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS session_tools");
        db.execSQL("DROP TABLE IF EXISTS sessions");
        db.execSQL("DROP TABLE IF EXISTS tools");
        onCreate(db);
    }
}

