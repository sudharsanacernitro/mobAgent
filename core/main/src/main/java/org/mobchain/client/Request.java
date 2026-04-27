package org.mobchain.client;

import okhttp3.*;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class Request {


    public static synchronized String sendRequest(
            JSONObject jsonBody,
            String endpoint,
            Map<String, String> headers   // ✅ added
    ) throws Exception {

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        RequestBody body = RequestBody.create(
                jsonBody.toString(),
                MediaType.parse("application/json")
        );

        // ✅ Build request with headers
        okhttp3.Request.Builder requestBuilder = new okhttp3.Request.Builder()
                .url(endpoint)
                .post(body);

        // ✅ Apply headers dynamically
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                requestBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }

        okhttp3.Request request = requestBuilder.build();

        ExecutorService executor = Executors.newFixedThreadPool(2);

        Future<String> result = executor.submit(() -> {
            try (okhttp3.Response response = client.newCall(request).execute()) {

                if (!response.isSuccessful()) {
                    System.out.println(jsonBody.toString());
                    throw new IOException("Unexpected response: " + response);
                }

                String responseBody = response.body().string();
                return responseBody.isEmpty() ? "⚠️ Empty body received!" : responseBody;

            } catch (Exception e) {
                e.printStackTrace();
                return "Error: " + e.getMessage();
            }
        });

        executor.shutdown();

        return result.get();
    }

}
