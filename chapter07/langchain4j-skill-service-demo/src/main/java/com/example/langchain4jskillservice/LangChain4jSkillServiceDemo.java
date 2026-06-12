package com.example.langchain4jskillservice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * A LangChain4j shaped Skill demo.
 *
 * <p>Real LangChain4j code can replace {@link SkillAwareAssistantFactory} with AiServices.builder().
 * The routing idea stays the same: choose a Skill before the AI Service call, then put the selected
 * instruction into the prompt context.</p>
 */
public class LangChain4jSkillServiceDemo {

    public static void main(String[] args) {
        ModelSettings settings = ModelSettings.fromEnv();
        SkillRepository repository = new SkillRepository(List.of(
                new SkillDefinition("spring-boot-api-review",
                        List.of("Spring Boot 接口", "接口变更", "Controller", "订单接口"),
                        "先确认接口契约，再检查兼容性、权限、异常处理和测试覆盖。")
        ));

        Assistant assistant = new SkillAwareAssistantFactory(repository, settings).create();
        String answer = assistant.chat("tenant-1", "帮我评审订单接口变更，重点看权限和兼容性");
        System.out.println(answer);
    }

    interface Assistant {
        String chat(@MemoryId String tenantId, String userMessage);
    }

    static class SkillAwareAssistantFactory {
        private final SkillRepository repository;
        private final ModelSettings settings;

        SkillAwareAssistantFactory(SkillRepository repository, ModelSettings settings) {
            this.repository = repository;
            this.settings = settings;
        }

        Assistant create() {
            return (tenantId, userMessage) -> {
                Optional<SkillDefinition> skill = repository.route(userMessage);
                SkillInvocation invocation = SkillInvocation.from(tenantId, userMessage, skill);
                return generate(invocation);
            };
        }

        private String generate(SkillInvocation invocation) {
            // Real code can pass invocation.systemPrompt() to a LangChain4j AI Service method.
            String skillName = invocation.skillName().orElse("none");
            return "[" + settings.provider().id() + "] 使用 Skill=" + skillName + " 处理任务：" + invocation.userMessage();
        }
    }

    static class SkillRepository {
        private final List<SkillDefinition> skills;

        SkillRepository(List<SkillDefinition> skills) {
            this.skills = List.copyOf(skills);
        }

        Optional<SkillDefinition> route(String userMessage) {
            String normalized = userMessage.toLowerCase(Locale.ROOT);
            return skills.stream()
                    .filter(skill -> skill.triggers().stream()
                            .map(trigger -> trigger.toLowerCase(Locale.ROOT))
                            .anyMatch(normalized::contains))
                    .findFirst();
        }
    }

    record SkillInvocation(String tenantId, String userMessage, Optional<String> skillName, String systemPrompt) {
        static SkillInvocation from(String tenantId, String userMessage, Optional<SkillDefinition> skill) {
            String prompt = skill
                    .map(item -> "你正在使用 Skill: " + item.name() + "\n执行要求: " + item.instruction())
                    .orElse("没有匹配到 Skill，按普通助手方式回答。");
            return new SkillInvocation(tenantId, userMessage, skill.map(SkillDefinition::name), prompt);
        }
    }

    record SkillDefinition(String name, List<String> triggers, String instruction) {
    }

    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    @interface MemoryId {
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
