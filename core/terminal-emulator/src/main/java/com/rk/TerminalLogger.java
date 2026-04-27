package com.rk;

import android.content.Context;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class TerminalLogger {

    private static File logFile;

    public static void init(Context context) {
        logFile = new File(context.getFilesDir(), "terminal.log");
    }

    public static synchronized void log(String data) {
        if (logFile == null) return;

        try (FileWriter writer = new FileWriter(logFile, true)) {
            writer.append(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}