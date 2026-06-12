package com.example.springaiskilladvisor;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * A small Spring AI shaped Skill demo.
 *
 * <p>The class keeps the demo free of API keys and network calls. In a real Spring AI application,
 * {@link SkillAdvisor#advise(ChatRequest)} sits at the same point as a ChatClient Advisor: before
 * the model call, it selects a Skill and adds the Skill instruction to the request context.</p>
 */
public class SpringAiSkillAdvisorDemo {

    public static void main(String[] args) {
        ModelSettings settings = ModelSettings.fromEnv();
        SkillCatalog catalog = new SkillCatalog(List.of(
                new SkillDocument("spring-boot-api-review", "Spring Boot 接口变更评审",
                        List.of("Spring Boot 接口", "接口变更", "Controller", "订单接口"),
                        "先确认接口契约，再检查兼容性、权限、异常处理和测试覆盖。")
        ));

        SkillAdvisor advisor = new SkillAdvisor(catalog, new SkillPromptRenderer());
        DeterministicChatClient chatClient = new DeterministicChatClient(settings);

        ChatRequest request = new ChatRequest("帮我评审订单 Spring Boot 接口变更，重点看兼容性、权限和测试");
        PromptEnvelope envelope = advisor.advise(request);
        ChatResponse response = chatClient.call(envelope);

        System.out.println("Provider: " + settings.provider().id());
        System.out.println("Skill: " + envelope.skillName().orElse("none"));
        System.out.println(response.content());
    }

    static class SkillAdvisor {
        private final SkillCatalog catalog;
        private final SkillPromptRenderer renderer;

        SkillAdvisor(SkillCatalog catalog, SkillPromptRenderer renderer) {
            this.catalog = catalog;
            this.renderer = renderer;
        }

        PromptEnvelope advise(ChatRequest request) {
            // This is the Spring AI Advisor position: inspect the user task before ChatClient.call().
            Optional<SkillDocument> matched = catalog.route(request.userMessage());
            String systemText = matched
                    .map(skill -> renderer.render(skill, request.userMessage()))
                    .orElse("没有匹配到 Skill，按普通 Java AI 助手方式回答。");
            return new PromptEnvelope(matched.map(SkillDocument::name), systemText, request.userMessage());
        }
    }

    static class SkillCatalog {
        private final List<SkillDocument> skills;

        SkillCatalog(List<SkillDocument> skills) {
            this.skills = List.copyOf(skills);
        }

        Optional<SkillDocument> route(String userMessage) {
            String normalized = userMessage.toLowerCase(Locale.ROOT);
            return skills.stream()
                    .filter(skill -> skill.triggers().stream()
                            .map(trigger -> trigger.toLowerCase(Locale.ROOT))
                            .anyMatch(normalized::contains))
                    .findFirst();
        }
    }

    static class SkillPromptRenderer {
        String render(SkillDocument skill, String userMessage) {
            return """
                    你正在使用一个可复用 Skill。
                    Skill 名称：%s
                    Skill 说明：%s
                    执行要求：%s
                    用户任务：%s
                    """.formatted(skill.name(), skill.description(), skill.instruction(), userMessage);
        }
    }

    static class DeterministicChatClient {
        private final ModelSettings settings;

        DeterministicChatClient(ModelSettings settings) {
            this.settings = settings;
        }

        ChatResponse call(PromptEnvelope envelope) {
            // Real code can replace this class with Spring AI ChatClient.
            String content = "[" + settings.provider().id() + "] 已按 Skill 上下文生成回答："
                    + envelope.systemText().lines().findFirst().orElse("普通回答");
            return new ChatResponse(content);
        }
    }

    record ChatRequest(String userMessage) {
    }

    record PromptEnvelope(Optional<String> skillName, String systemText, String userMessage) {
    }

    record ChatResponse(String content) {
    }

    record SkillDocument(String name, String description, List<String> triggers, String instruction) {
    }

    record ModelSettings(AiProvider provider, String model, String baseUrl) {
        static ModelSettings fromEnv() {
            AiProvider provider = AiProvider.parseOrDefault(System.getenv("APP_AI_DEFAULT_PROVIDER"));
            return switch (provider) {
                case DEEPSEEK -> new ModelSettings(provider, env("DEEPSEEK_MODEL", "deepseek-chat"), env("DEEPSEEK_BASE_URL", "https://api.deepseek.com"));
                case OLLAMA -> new ModelSettings(provider, env("OLLAMA_MODEL", "qwen2.5:7b"), env("OLLAMA_BASE_URL", "http://localhost:11434"));
                case CHATGPT -> new ModelSettings(provider, env("CHATGPT_MODEL", "gpt-4.1-mini"), env("CHATGPT_BASE_URL", "https://api.openai.com/v1"));
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
