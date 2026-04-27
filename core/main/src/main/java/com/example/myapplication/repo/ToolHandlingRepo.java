package com.example.myapplication.repo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.myapplication.utils.DBHelper;
import com.example.myapplication.utils.exceptions.NotFoundException;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ToolHandlingRepo {

    private static ToolHandlingRepo instance;

    private SQLiteDatabase db;

    private ToolHandlingRepo( Context context ) {

        DBHelper dbHelper = new DBHelper(context);
        this.db = dbHelper.getWritableDatabase();

    }


    public static synchronized ToolHandlingRepo getInstance( Context context ) {

        if( instance == null ) {

            instance = new ToolHandlingRepo( context );

        }

        return instance;

    }


    public int addTool( String toolName ) {

        ContentValues values = new ContentValues();
        values.put("toolName", toolName );

        int toolId = -1;

        toolId = (int)db.insertOrThrow("tools", null, values);

        return toolId;

    }

    public Integer getTool(String toolName ) {

        Cursor cursor = db.rawQuery(
                "SELECT toolId FROM tools WHERE toolName = ?",
                new String[]{toolName}
        );

        try {

            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }

            throw new NotFoundException("Tool not found"); // tool not found

        } finally {

            cursor.close();

        }

    }

    public String getTool( int toolId ) {

        Cursor cursor = db.rawQuery(
                "SELECT toolName FROM tools WHERE toolId = ?",
                new String[]{ String.valueOf(toolId) }
        );

        try {

            if (cursor.moveToFirst()) {
                return cursor.getString(0);
            }

            throw new NotFoundException("Tool not found"); // tool not found

        } finally {

            cursor.close();

        }

    }




    public boolean deleteTools( int toolId ) {

        int rows = db.delete(
                "tools",
                "toolId = ?",
                new String[]{
                        String.valueOf(toolId)
                }
        );

        return rows > 0;

    }


    public List<JSONObject> getToolsList() {

        List<JSONObject> toolsList = new ArrayList<>();

        Cursor cursor = db.rawQuery(
                "SELECT t.toolId, t.toolName " +
                        "FROM tools t ",
                null);

        try {

            while (cursor.moveToNext()) {

                JSONObject obj = new JSONObject();

                obj.put("toolId", cursor.getInt(0));
                obj.put("toolName", cursor.getString(1));

                toolsList.add(obj);

            }

        } catch (JSONException e) {

            cursor.close();
            return null;

        } finally {

            cursor.close();

        }

        return toolsList;

    }

}

