package com.example.myapplication.network;

import okhttp3.*;

public class HttpClient {

    private static final OkHttpClient client = new OkHttpClient();

    public static Response callApi(
            String url,
            String method,
            String body,
            String contentType,
            Headers headers
    ) throws Exception {

        RequestBody requestBody = null;

        // For GET, DELETE (without body)
        if (body != null && !body.isEmpty()) {
            requestBody = RequestBody.create(
                    body,
                    MediaType.parse(contentType)
            );
        }

        Request.Builder builder = new Request.Builder()
                .url(url);

        // Add headers
        if (headers != null) {
            builder.headers(headers);
        }

        // Select HTTP method dynamically
        switch (method.toUpperCase()) {
            case "POST":
                builder.post(requestBody);
                break;
            case "PUT":
                builder.put(requestBody);
                break;
            case "PATCH":
                builder.patch(requestBody);
                break;
            case "DELETE":
                if (requestBody != null) builder.delete(requestBody);
                else builder.delete();
                break;
            case "GET":
                builder.get();
                break;
            case "HEAD":
                builder.head();
                break;
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }

        Request request = builder.build();
        return client.newCall(request).execute();
    }
}
