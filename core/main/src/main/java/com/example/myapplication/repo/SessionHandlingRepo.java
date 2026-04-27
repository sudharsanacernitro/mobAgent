package com.example.myapplication.repo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteConstraintException;

import com.example.myapplication.utils.DBHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SessionHandlingRepo {

    private static SessionHandlingRepo instance;
    private SQLiteDatabase db;

    private SessionHandlingRepo(Context context) {
        DBHelper dbHelper = new DBHelper(context.getApplicationContext());
        this.db = dbHelper.getWritableDatabase();
    }

    public static synchronized SessionHandlingRepo getInstance(Context context) {
        if (instance == null) {
            instance = new SessionHandlingRepo(context);
        }
        return instance;
    }

    /* -------------------------------
       SESSION
       ------------------------------- */

    public int createSession() {
        ContentValues values = new ContentValues();
        values.put("isActive", 1);

        return (int) db.insertOrThrow("sessions", null, values);
    }

    /* -------------------------------
       SESSION ↔ TOOL MAPPING
       ------------------------------- */

    public void addToolToSession(int sessionId, int toolId) {

        ContentValues values = new ContentValues();
        values.put("sessionId", sessionId);
        values.put("toolId", toolId);

        try {
            db.insertOrThrow("session_tools", null, values);
        } catch (SQLiteConstraintException e) {
            throw new IllegalStateException(
                    "Invalid sessionId / toolId OR tool already added to session",
                    e
            );
        }
    }

    public boolean removeToolFromSession(int sessionId, int toolId) {

        int rows = db.delete(
                "session_tools",
                "sessionId = ? AND toolId = ?",
                new String[]{
                        String.valueOf(sessionId),
                        String.valueOf(toolId)
                }
        );

        return rows > 0;
    }

    /* -------------------------------
       QUERY
       ------------------------------- */

    public List<JSONObject> getSessionTools(int sessionId) {

        List<JSONObject> toolsInSession = new ArrayList<>();

        Cursor cursor = db.rawQuery(
                "SELECT t.toolId, t.toolName " +
                        "FROM tools t " +
                        "JOIN session_tools st ON t.toolId = st.toolId " +
                        "WHERE st.sessionId = ?",
                new String[]{String.valueOf(sessionId)}
        );

        try {
            while (cursor.moveToNext()) {

                JSONObject obj = new JSONObject();
                obj.put("toolId", cursor.getInt(0));
                obj.put("toolName", cursor.getString(1));

                toolsInSession.add(obj);

            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        } finally {
            cursor.close();
        }

        return toolsInSession;
    }


    public boolean find( int sessionId , int toolId ) {

        Cursor cursor = db.rawQuery(
                "SELECT * " +
                        "FROM " +
                        " session_tools  " +
                        "WHERE sessionId = ? AND toolId = ? ",
                new String[]{String.valueOf(sessionId) , String.valueOf(toolId)}
        );

        try {
            return cursor.moveToFirst();
        } finally {
            cursor.close();
        }

    }
}
