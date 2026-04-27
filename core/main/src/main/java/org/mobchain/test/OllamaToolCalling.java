package org.mobchain.test;
//package org.example.ollama;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class OllamaToolCalling {

    private static final String OLLAMA_URL = "http://172.21.228.49:11434/api/chat";
    private static final OkHttpClient client = new OkHttpClient();

    public static void call() throws Exception {
        // 1️⃣ Step 1: Send a user message and tool definition
        JSONObject payload = new JSONObject();
        payload.put("model", "qwen2.5-coder:3b");

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject()
                .put("role", "user")
                .put("content", "What's the weather in Chennai right now?"));
        payload.put("messages", messages);

        // Define available tools
        JSONArray tools = new JSONArray();
        JSONObject getWeatherTool = new JSONObject()
                .put("type", "function")
                .put("function", new JSONObject()
                        .put("name", "get_weather")
                        .put("description", "Get current weather for a city")
                        .put("parameters", new JSONObject()
                                .put("type", "object")
                                .put("properties", new JSONObject()
                                        .put("city", new JSONObject()
                                                .put("type", "string")
                                                .put("description", "City name")))
                                .put("required", new JSONArray().put("city"))
                        ));
        tools.put(getWeatherTool);
        payload.put("tools", tools);

        JSONObject toolCallResponse = callOllama(payload);
        System.out.println("\n🧩 Tool Call Response:\n" + toolCallResponse.toString(2));

        // 2️⃣ Step 2: Extract tool call info
        System.out.println( toolCallResponse.toString() );

        JSONObject message = toolCallResponse.optJSONObject("message");
        if (message == null || !message.has("tool_calls")) {

            System.out.println("❌ No tool call detected.");
            return;
        }

        JSONArray toolCalls = message.getJSONArray("tool_calls");
        JSONObject toolCall = toolCalls.getJSONObject(0);
        String toolName = toolCall.getJSONObject("function").getString("name");
        JSONObject arguments = toolCall.getJSONObject("function").getJSONObject("arguments");

        String city = arguments.getString("city");
        System.out.println("🔧 Tool to call: " + toolName + " with city: " + city);

        // 3️⃣ Step 3: Execute tool (mock result)
        JSONObject toolResult = new JSONObject()
                .put("city", city)
                .put("temperature", "32°C")
                .put("condition", "Sunny");

        // 4️⃣ Step 4: Send back tool result for final answer
        JSONArray updatedMessages = new JSONArray();
        updatedMessages.put(messages.getJSONObject(0)); // user msg
        updatedMessages.put(message);                    // model's tool call
        updatedMessages.put(new JSONObject()             // tool output
                .put("role", "tool")
                .put("content", toolResult.toString()));

        JSONObject finalPayload = new JSONObject()
                .put("model", "llama3.1")
                .put("messages", updatedMessages);

        JSONObject finalResponse = callOllama(finalPayload);
        System.out.println("\n💬 Final Response:\n" + finalResponse.toString(2));
    }

    private static JSONObject callOllama(JSONObject payload) throws IOException, JSONException {
        RequestBody body = RequestBody.create(
                payload.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(OLLAMA_URL)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            return new JSONObject(response.body().string());
        }
    }
}
