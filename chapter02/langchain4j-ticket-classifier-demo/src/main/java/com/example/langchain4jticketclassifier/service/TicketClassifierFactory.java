package com.example.langchain4jticketclassifier.service;

import com.example.langchain4jticketclassifier.assistant.ObservableTicketClassifierAssistant;
import com.example.langchain4jticketclassifier.assistant.TicketClassifierAssistant;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;

import java.time.Duration;

public class TicketClassifierFactory {

    public TicketClassifierAssistant create(String baseUrl, String modelName) {
        return AiServices.builder(TicketClassifierAssistant.class)
            .chatModel(chatModel(baseUrl, modelName))
            .build();
    }

    public ObservableTicketClassifierAssistant createObservable(String baseUrl, String modelName) {
        return AiServices.builder(ObservableTicketClassifierAssistant.class)
            .chatModel(chatModel(baseUrl, modelName))
            .build();
    }

    private ChatModel chatModel(String baseUrl, String modelName) {
        return OllamaChatModel.builder()
            .baseUrl(baseUrl)
            .modelName(modelName)
            .temperature(0.0)
            .timeout(Duration.ofSeconds(60))
            .build();
    }
}
