package com.example.langchain4jtool.assistant;

import com.example.langchain4jtool.tool.CommerceTools;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;

public final class CommerceAssistantFactory {

    private CommerceAssistantFactory() {
    }

    public static CommerceAssistant create(ChatModel chatModel, CommerceTools tools) {
        return AiServices.builder(CommerceAssistant.class)
            .chatModel(chatModel)
            // 这是 LangChain4j Tool Calling 的关键：把带 @Tool 的对象注入 AI Service。
            .tools(tools)
            .build();
    }

    public static CommerceAssistant createWithOllama(CommerceTools tools) {
        ChatModel model = OllamaChatModel.builder()
            .baseUrl("http://localhost:11434")
            .modelName("qwen2.5:7b")
            .temperature(0.2)
            .build();
        return create(model, tools);
    }
}
