package com.example.langchain4jchat.config;

import java.util.Arrays;

public enum AiProvider {
    OLLAMA("ollama"),
    OPENAI("openai");

    private final String id;

    AiProvider(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static AiProvider from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("provider must not be blank");
        }
        return Arrays.stream(values())
                .filter(provider -> provider.id.equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unsupported provider: " + value));
    }
}
