package org.mobchain.models;

import org.mobAgent.plugin.interfaces.FormatterBuilder;
import org.mobAgent.plugin.interfaces.FormatterInterface;

import java.util.HashMap;

/**
 * In-process {@link FormatterBuilder} implementation that wraps the built-in
 * {@link OpenAIFormatter}. Used as the default formatter so users do not have
 * to upload a DEX formatter plugin to start chatting with any OpenAI-compatible
 * endpoint (OpenAI, Ollama, llama-server, etc.).
 *
 * Instantiated directly from app code — never loaded via DexClassLoader.
 */
public class OpenAIFormatterBuilder implements FormatterBuilder {

    private final OpenAIFormatter.Builder delegate = OpenAIFormatter.builder();

    private String baseURL = "";
    private String modelName = "";
    private boolean isStream = false;
    private final HashMap<String, String> headers = new HashMap<>();

    @Override
    public FormatterBuilder baseURL(String baseURL) {
        this.baseURL = baseURL;
        delegate.baseURL(baseURL);
        return this;
    }

    @Override
    public FormatterBuilder model(String modelName) {
        this.modelName = modelName;
        delegate.model(modelName);
        return this;
    }

    @Override
    public FormatterBuilder stream(boolean stream) {
        this.isStream = stream;
        delegate.stream(stream);
        return this;
    }

    @Override
    public FormatterBuilder addHeader(String key, String value) {
        headers.put(key, value);
        delegate.addHeader(key, value);
        return this;
    }

    @Override
    public String getBaseURL() {
        return baseURL;
    }

    @Override
    public String getModelName() {
        return modelName;
    }

    @Override
    public boolean getIsStream() {
        return isStream;
    }

    @Override
    public HashMap<String, String> getHeader() {
        return headers;
    }

    @Override
    public FormatterInterface build() {
        return delegate.build();
    }
}

