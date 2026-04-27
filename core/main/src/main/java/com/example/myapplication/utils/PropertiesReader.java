package com.example.myapplication.utils;

import android.content.Context;

import com.rk.terminal.R;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;

public class PropertiesReader {

    // Cache (same as before)
    private static HashMap<String, String> properties;

    public static HashMap<String, String> getProperties(Context context) {

        // Return cached copy
        if (properties != null) {
            return new HashMap<>(properties);
        }

        properties = new HashMap<>();

        Properties props = new Properties();

        try (InputStream is =
                     context.getResources().openRawResource(R.raw.app)) {

            props.load(is);

            for (String name : props.stringPropertyNames()) {
                properties.put(name, props.getProperty(name));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new HashMap<>(properties);
    }

    public static String getProperty(Context context, String key) {

        if (properties == null) {
            getProperties(context);
        }

        return properties.get(key);
    }
}
