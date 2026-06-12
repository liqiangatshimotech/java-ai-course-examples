package com.example.agentconcepts;

import java.util.List;
import java.util.Locale;

public class AgentConceptsDemo {

    public static void main(String[] args) {
        ModelSettings settings = ModelSettings.fromEnv();

        AgentBlueprint blueprint = new AgentBlueprint(
                settings,
                List.of(
                        new AgentCapability("LLM", "理解目标、拆解下一步、生成最终回复"),
                        new AgentCapability("Tools", "调用订单、物流、工单等 Java 业务能力"),
                        new AgentCapability("Memory", "保存会话上下文、短期状态和必要的长期偏好"),
                        new AgentCapability("Planning", "决定先做什么、后做什么，以及什么时候停止"),
                        new AgentCapability("Feedback", "根据工具结果、校验结果或人工确认修正下一步")
                )
        );

        System.out.println("Provider: " + blueprint.settings().provider().id());
        System.out.println("Agent = " + blueprint.formula());
        blueprint.capabilities().forEach(capability ->
                System.out.println("- " + capability.name() + ": " + capability.responsibility()));
    }

    record AgentBlueprint(ModelSettings settings, List<AgentCapability> capabilities) {
        String formula() {
            return "LLM + Tools + Memory + Planning + Feedback";
        }
    }

    record AgentCapability(String name, String responsibility) {
    }

    record ModelSettings(AiProvider provider, String model, String baseUrl) {
        static ModelSettings fromEnv() {
            AiProvider provider = AiProvider.parseOrDefault(System.getenv("APP_AI_DEFAULT_PROVIDER"));
            return switch (provider) {
                case DEEPSEEK -> new ModelSettings(provider,
                        env("DEEPSEEK_MODEL", "deepseek-chat"),
                        env("DEEPSEEK_BASE_URL", "https://api.deepseek.com"));
                case OLLAMA -> new ModelSettings(provider,
                        env("OLLAMA_MODEL", "qwen2.5:7b"),
                        env("OLLAMA_BASE_URL", "http://localhost:11434"));
                case CHATGPT -> new ModelSettings(provider,
                        env("CHATGPT_MODEL", "gpt-4.1-mini"),
                        env("CHATGPT_BASE_URL", "https://api.openai.com/v1"));
            };
        }

        private static String env(String name, String fallback) {
            String value = System.getenv(name);
            return value == null || value.isBlank() ? fallback : value;
        }
    }

    enum AiProvider {
        DEEPSEEK("deepseek"),
        OLLAMA("ollama"),
        CHATGPT("chatgpt");

        private final String id;

        AiProvider(String id) {
            this.id = id;
        }

        String id() {
            return id;
        }

        static AiProvider parseOrDefault(String raw) {
            if (raw == null || raw.isBlank()) {
                return DEEPSEEK;
            }
            String normalized = raw.trim().toLowerCase(Locale.ROOT);
            for (AiProvider provider : values()) {
                if (provider.id.equals(normalized)) {
                    return provider;
                }
            }
            throw new IllegalArgumentException("provider only supports deepseek, ollama, chatgpt");
        }
    }
}
