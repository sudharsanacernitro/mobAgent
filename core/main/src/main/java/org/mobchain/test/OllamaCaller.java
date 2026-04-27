package org.mobchain.test;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.*;

public class OllamaCaller {

    private static final String OLLAMA_URL = "http://localhost:11434/api/chat";

    public static void callOllama() throws Exception {
        OkHttpClient client = new OkHttpClient();
        //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
        // to see how IntelliJ IDEA suggests fixing it.
//        System.out.printf("Hello and welcome!");


        // ✅ Correct Ollama Chat format
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", "what is your name and what is your role");


        JSONObject  prompt = new JSONObject();
        prompt.put("role", "system");
        prompt.put("content", "your name is BOB , an agri assitant ");



        JSONArray messages = new JSONArray();
        messages.put(prompt);
        messages.put(message);

        JSONObject json = new JSONObject();
        json.put("model", "qwen2.5-coder:3b");
        json.put("messages", messages);
        json.put("stream", false);

        RequestBody body = RequestBody.create(
                json.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(OLLAMA_URL)
                .post(body)
                .build();

        ExecutorService executor = Executors.newFixedThreadPool(2);

        Future<String> result = executor.submit(() -> {
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected response: " + response);
                }
                String responseBody = response.body().string();
                return responseBody.isEmpty() ? "⚠️ Empty body received!" : responseBody;
            } catch (Exception e) {
                e.printStackTrace();
                return "Error: " + e.getMessage();
            }
        });

        System.out.println("Waiting for Ollama response...");
        System.out.println("Response:\n" + result.get());

        executor.shutdown();
    }

    public static void main(String[] args) {
        try {
            callOllama();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
