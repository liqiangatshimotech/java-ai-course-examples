package com.example.chapter09.mimocode;

import java.util.Locale;
import java.util.Map;

/**
 * 课程里的所有示例默认以 DeepSeek 作为首选模型，同时保留 Ollama
 * 和 ChatGPT 的配置位。这个 demo 不直接请求模型服务，而是把配置
 * 结构保留下来，方便后续把规则版 checkpoint writer 替换成真实 LLM。
 */
public record AiProviderConfig(
        String provider,
        String model,
        String baseUrl,
        String apiKey
) {

    public static AiProviderConfig fromEnvironment(Map<String, String> env) {
        String provider = env.getOrDefault("APP_AI_DEFAULT_PROVIDER", "deepseek")
                .trim()
                .toLowerCase(Locale.ROOT);

        return switch (provider) {
            case "ollama" -> new AiProviderConfig(
                    "ollama",
                    env.getOrDefault("OLLAMA_MODEL", "deepseek-r1:8b"),
                    env.getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434"),
                    env.getOrDefault("OLLAMA_API_KEY", "")
            );
            case "chatgpt", "openai" -> new AiProviderConfig(
                    "chatgpt",
                    env.getOrDefault("CHATGPT_MODEL", "gpt-4.1-mini"),
                    env.getOrDefault("CHATGPT_BASE_URL", "https://api.openai.com/v1"),
                    env.getOrDefault("CHATGPT_API_KEY", "")
            );
            default -> new AiProviderConfig(
                    "deepseek",
                    env.getOrDefault("DEEPSEEK_MODEL", "deepseek-chat"),
                    env.getOrDefault("DEEPSEEK_BASE_URL", "https://api.deepseek.com"),
                    env.getOrDefault("DEEPSEEK_API_KEY", "")
            );
        };
    }

    public String maskedApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            return "(not configured)";
        }
        if (apiKey.length() <= 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}
