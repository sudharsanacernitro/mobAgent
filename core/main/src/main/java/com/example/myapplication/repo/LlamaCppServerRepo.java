package com.example.myapplication.repo;

import android.content.Context;

import com.example.myapplication.MainActivity;
import com.example.myapplication.network.HttpClient;
import com.example.myapplication.utils.PropertiesReader;
import com.rk.terminal.service.TerminalSessionManager;
import com.rk.terminal.ui.screens.terminal.MkSession;
import com.termux.terminal.TerminalAsynchronousSessionHandler;
import com.termux.terminal.TerminalSynchronousSessionHandler;

import okhttp3.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class LlamaCppServerRepo {

    private final Context context;
    private final MainActivity mainActivity;

    private static LlamaCppServerRepo instance = null;

    private  Process process;


    public LlamaCppServerRepo(MainActivity context ) {

        this.context = context;
        this.mainActivity = context;
        instance = this;

    }

    public static LlamaCppServerRepo getInstance( ) {

        return instance;

    }

    public void startLlama( int port , String model ) {

        Thread LlamaThread = new Thread() {
            @Override
            public void run() {
                super.run();
                runServer( port , model );
            }
        };

        LlamaThread.start();

    }

    public void stopLlama( ) {

        if( process != null ) {

            process.destroy();

        }

    }

    public static boolean isServerOnline( int port ) {

        String healthURL = "http://127.0.0.1:"+port+"/health";

        try {

            Response res = HttpClient.callApi( healthURL , "GET" , null , "application/json" , null  );

            return res.isSuccessful();

        } catch( Exception e ) {

            System.out.println(e);
            return false;

        }

    }

    public boolean downloadLlamaCppBinaries( String rootDir , String dir) {

        try {

            // Copy llama-server binary

//            File destinationFolder = new File(rootDir+dir);
//
//            if ( destinationFolder.exists() && destinationFolder.isDirectory() ) {
//                System.out.println("File already exists locally. Skipping copy.");
//                return true;
//            }


            TerminalSynchronousSessionHandler headlessSession;

            String CreateLlamaCppDircommand = "bash llamaCppServerSetup.sh "+dir+" http://100.118.114.83:8000/llama-server.zip";
            
            headlessSession = (TerminalSynchronousSessionHandler) TerminalSessionManager.getInstance().getSession("temp_sync");

            String output = headlessSession.executeCommandSync( CreateLlamaCppDircommand , 90000 );

            System.out.println("COMMAND OUTPUT:\n" + output);
            System.out.println("command executed");

            if( output != null && output.length() != 0 ) {

                return true;

            }

            return false;
        } catch( Exception e ) {

            return false;

        }

        }


    public void runServer(int port , String modelName) {

        try {



            System.out.println(PropertiesReader.getProperty( context , "setUpFileName" ));
            System.out.println(PropertiesReader.getProperty( context , "rootDirFromLocalDir" ));
            System.out.println(PropertiesReader.getProperty( context , "LlamaCppDir" ));


//            if( !moveSetupFile( PropertiesReader.getProperty( context , "setUpFileName" ) ) ) {
//                throw new RuntimeException("can't copy Llama-setup file to rootfs");
//            }
//
//            if(    !downloadLlamaCppBinaries(
//                    PropertiesReader.getProperty( context , "rootDirFromLocalDir" ) ,
//                    PropertiesReader.getProperty( context , "LlamaCppDir" ),
//                    "")
//            ) {
//
//                System.out.println("can't download llamacpp files in rootfs");
//
//            }

            TerminalSessionManager terminalSessionManager = TerminalSessionManager.getInstance();

            TerminalAsynchronousSessionHandler headlessSession = (TerminalAsynchronousSessionHandler) terminalSessionManager.getSession("llama_cpp_server");

            String modelPath= PropertiesReader.getProperty( context , "llmFilePath" ) + "/"+modelName;
            

            byte[] command = ("cd llamaCpp && LD_LIBRARY_PATH=$PWD/lib ./llama-server -m "+modelPath+"  --port " + port + " --host 0.0.0.0 \n").getBytes();


            headlessSession.write(command,command.length);
            System.out.println("llama server command started");
            System.out.println("llama server command completed");




        } catch (Exception e) {
            e.printStackTrace();
        }

    }



    // Copy a single file from assets
    private File copyAssetFile(String assetPath, String outputName) throws IOException {
        File outFile = new File(context.getFilesDir(), outputName);

        if (outFile.exists()) return outFile;

        InputStream is = context.getAssets().open(assetPath);
        FileOutputStream fos = new FileOutputStream(outFile);

        byte[] buffer = new byte[4096];
        int read;
        while ((read = is.read(buffer)) != -1) {
            fos.write(buffer, 0, read);
        }

        is.close();
        fos.close();

        return outFile;
    }

    // Copy a folder from assets (for lib/)
    private void copyAssetFolder(String assetFolder, File outDir) throws IOException {
        String[] assets = context.getAssets().list(assetFolder);
        if (assets == null) return;

        if (!outDir.exists()) outDir.mkdirs();

        for (String file : assets) {
            String fullPath = assetFolder + "/" + file;
            String[] sub = context.getAssets().list(fullPath);

            if (sub != null && sub.length > 0) {
                // Folder
                copyAssetFolder(fullPath, new File(outDir, file));
            } else {
                // File
                File outFile = new File(outDir, file);
                InputStream is = context.getAssets().open(fullPath);
                FileOutputStream fos = new FileOutputStream(outFile);

                byte[] buffer = new byte[4096];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                }

                is.close();
                fos.close();
            }
        }
    }

}
