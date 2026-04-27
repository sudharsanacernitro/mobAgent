package com.example.myapplication.utils;

import static com.rk.libcommons.UtilsKt.runOnUiThread;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.example.myapplication.DAOs.PluginDatabase;

import org.mobAgent.plugin.interfaces.FormatterBuilder;
import org.mobAgent.plugin.interfaces.FormatterInterface;
import org.mobAgent.plugin.interfaces.Memory;

import com.example.myapplication.DAOs.entities.Plugin;

import java.io.File;

import dalvik.system.DexClassLoader;

public class DexLoader {


    private Context context;

    public DexLoader(Context context) {
        this.context = context;
    }


    public final FormatterBuilder loadFormatter(int formatterPluginId) {

        try {
            PluginDatabase db = PluginDatabase.getInstance(this.context);
            Plugin plugin = db.pluginDao().getById(formatterPluginId);

            String pluginPath = plugin.getPath();

            File jarFile = new File(pluginPath);

            if (!jarFile.exists()) {
                Log.e("Plugin", "Failed to load tool");
                return null;
            }

            jarFile.setReadable(true, false);
            jarFile.setWritable(false, false);

            String optimizedDexOutputPath = context.getDir("outdex", Context.MODE_PRIVATE).getAbsolutePath();

            DexClassLoader classLoader = new DexClassLoader(
                    jarFile.getAbsolutePath(),
                    optimizedDexOutputPath,
                    null,
                    context.getClassLoader() // VERY IMPORTANT
            );
            Class<?> clazz = classLoader.loadClass("org.mobAgent.FormatterBuilderImpl");
            Object instance = clazz.getDeclaredConstructor().newInstance();

            System.out.println("Formatterbuilder class loaded: " + clazz.getName());
            return (FormatterBuilder) instance;

        }
         catch (Exception e) {
            Log.e("Plugin", "Failed to load formatter", e);

            runOnUiThread(() -> {
                Toast.makeText(context, "Failed to load formatter plugin: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });

            return null;
        }



    }


}
