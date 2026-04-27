package com.rk.terminal.init;

import com.example.myapplication.MainActivity;
import com.example.myapplication.network.HttpClient;
import com.example.myapplication.utils.PropertiesReader;

import okhttp3.Response;

public class RootfsFrameworkIntializer {

    MainActivity activity;

    RootfsFrameworkIntializer( MainActivity activity ) {
        this.activity = activity;
    }

    public String checkRootFSFrameworkVersion() {

        try {
            Response res = HttpClient.callApi(PropertiesReader.getProperty(activity, "remoteServerURL") + "/version", "GET", null, "text/plain", null);

            res.body();

            return "";

        } catch ( Exception e ) {

            return "-1";

        }


    }

}
