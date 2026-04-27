package com.example.myapplication.services;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import org.json.JSONObject;
import org.mobAgent.plugin.interfaces.Tool;
import org.mobchain.tools.ToolsManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.Toast;

import com.example.myapplication.repo.SessionHandlingRepo;
import com.example.myapplication.repo.ToolHandlingRepo;

import dalvik.system.DexClassLoader;

public class ToolHandlingService extends ToolsManager {

    private final Context context;
    private final String toolsPath;


    private ToolHandlingRepo toolRepo;
    private SessionHandlingRepo sessionRepo;

    public ToolHandlingService(Context context, String toolsPath) {

        this.context = context.getApplicationContext();
        this.toolsPath = toolsPath;

        this.toolRepo = ToolHandlingRepo.getInstance( context );
        this.sessionRepo = SessionHandlingRepo.getInstance( context );


    }


    public int createSession() {

        return  sessionRepo.createSession();

    }


    public boolean containsInSessionTool( int sessionId ,  int toolId ) {

        System.out.println("checking tool :"+toolId+" to session : "+sessionId);


        return sessionRepo.find( sessionId , toolId );



    }



    public boolean addSessionTool( int sessionId , int toolId ) {


        try {

            sessionRepo.addToolToSession( sessionId , toolId );


            return true;

        } catch( Exception e ) {

            return false;

        }


    }


    public List<JSONObject> getSessionTools( int sessionId ) {

        return sessionRepo.getSessionTools(sessionId);

    }



    public boolean removeSessionTool( int sessionId , int toolId ) {

        System.out.println("removing tool from session");


        boolean isRemoved = sessionRepo.removeToolFromSession( sessionId , toolId );

        System.out.println("tool removed : "+isRemoved);

        return isRemoved;

    }



    public boolean add(Uri jarUri) {

        System.out.println("adding tool");

        File jarDir = new File(context.getFilesDir(), toolsPath);
        if (!jarDir.exists() && !jarDir.mkdirs()) {
            return false;
        }

        String fileName = getFileName(jarUri);
        if (fileName == null || !fileName.toLowerCase().endsWith(".jar")) {
            return false;
        }

        File destFile = new File(jarDir, fileName);

        try (InputStream in = context.getContentResolver().openInputStream(jarUri);
             OutputStream out = new FileOutputStream(destFile)) {

            if (in == null) return false;

            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }

            out.flush();

            if( toolRepo.addTool(fileName.split("\\.")[0]) != -1 ) {

                System.out.println("Successfully tool uploaded");
                return true;

            }

            return false;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }




    public boolean delete(int toolId , String toolName) {

        System.out.println("deleting tool");

        if (toolName == null ) {
            return false;
        }

        String toolFileName = toolName+".jar";

        File jarDir = new File(context.getFilesDir(), toolsPath);
        if (!jarDir.exists()) {
            return false;
        }

        File jarFile = new File(jarDir, toolFileName );


        return jarFile.exists() && jarFile.delete() && toolRepo.deleteTools( toolId);

    }



    public List<JSONObject> listTools() {


        System.out.println("Listing tool");

        return toolRepo.getToolsList();

    }

    public ToolsManager getToolObjects( int sessionId ) {


        ToolsManager cachedSessionTools = new ToolsManager();

        List<JSONObject> toolsList = sessionRepo.getSessionTools( sessionId );

        for( JSONObject toolDetails : toolsList ) {

            try{

                String toolName = toolDetails.getString("toolName");

                Tool tool = getToolObject(toolName);

                System.out.println("Adding "+toolName+" to session");

                cachedSessionTools.addTools("",tool);

            } catch( Exception e ) {

                Toast.makeText(
                        context,
                        "Error in Tool : "+toolDetails,
                        Toast.LENGTH_SHORT
                ).show();

            }

        }



        return cachedSessionTools;

    }


    public Tool getToolObject(String toolName) {

        String toolJarName = toolName+".jar";

        File jarFile = new File(
                new File(context.getFilesDir(), toolsPath),
                toolJarName
        );

        if (!jarFile.exists()) {
            Log.e("Plugin", "Failed to load tool");
            return null;
        }

        jarFile.setReadable(true, false);
        jarFile.setWritable(false, false);


        try{

            File optimizedDir = context.getDir("dex_opt", MODE_PRIVATE);

            DexClassLoader classLoader = new DexClassLoader(
                    jarFile.getAbsolutePath(),
                    optimizedDir.getAbsolutePath(),
                    null,
                    context.getClassLoader() // VERY IMPORTANT
            );

            Class<?> clazz = classLoader.loadClass("org.example."+toolName);

            Object instance = clazz.getDeclaredConstructor().newInstance();

            return (Tool)instance;


        } catch( Exception e ) {

            Log.e("Plugin", "Failed to load tool", e);
            return null;

        }


    }


    private String getFileName(Uri uri) {

        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = context.getContentResolver()
                    .query(uri, null, null, null, null)) {

                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        return cursor.getString(index);
                    }
                }
            }
        }

        return uri.getLastPathSegment();
    }


}
