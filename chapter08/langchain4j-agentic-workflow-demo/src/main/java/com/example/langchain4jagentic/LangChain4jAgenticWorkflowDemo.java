package com.example.langchain4jagentic;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class LangChain4jAgenticWorkflowDemo {

    public static void main(String[] args) {
        ModelSettings settings = ModelSettings.fromEnv();
        AgenticScope scope = new AgenticScope();
        scope.writeInput("orderNo", "A20260001");
        scope.writeInput("userGoal", "判断订单是否需要补偿，并生成处理摘要");

        AfterSalesAgenticWorkflow workflow = new AfterSalesAgenticWorkflow(
                new LocalIntentAgent(settings),
                new LocalOrderAgent(),
                new LocalLogisticsAgent(),
                new LocalCompensationAgent(),
                new LocalReplyAgent(settings),
                new LocalReplyQualityAgent(),
                new LocalSummaryAgent(settings)
        );

        String answer = workflow.run(scope);
        System.out.println("Provider: " + settings.provider().id());
        System.out.println(answer);
        System.out.println("Scope: " + scope.values());
        scope.invocations().forEach(invocation -> System.out.println(invocation.toLogLine()));
    }

    static class AfterSalesAgenticWorkflow {
        private final IntentAgent intentAgent;
        private final OrderAgent orderAgent;
        private final LogisticsAgent logisticsAgent;
        private final CompensationAgent compensationAgent;
        private final ReplyAgent replyAgent;
        private final ReplyQualityAgent qualityAgent;
        private final SummaryAgent summaryAgent;

        AfterSalesAgenticWorkflow(IntentAgent intentAgent, OrderAgent orderAgent, LogisticsAgent logisticsAgent,
                                  CompensationAgent compensationAgent, ReplyAgent replyAgent,
                                  ReplyQualityAgent qualityAgent, SummaryAgent summaryAgent) {
            this.intentAgent = intentAgent;
            this.orderAgent = orderAgent;
            this.logisticsAgent = logisticsAgent;
            this.compensationAgent = compensationAgent;
            this.replyAgent = replyAgent;
            this.qualityAgent = qualityAgent;
            this.summaryAgent = summaryAgent;
        }

        String run(AgenticScope scope) {
            scope.writeFromAgent("intentAgent", "intent", intentAgent.classify(scope.read("userGoal")));
            runOrderAndLogisticsInParallel(scope);
            scope.writeFromAgent("compensationAgent", "compensationDecision",
                    compensationAgent.evaluate(scope.read("orderStatus"), scope.read("logisticsStatus")));
            refineReplyInLoop(scope, 3, 0.85);
            return summaryAgent.summarize(scope.values());
        }

        private void runOrderAndLogisticsInParallel(AgenticScope scope) {
            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                Future<?> orderFuture = executor.submit(() ->
                        scope.writeFromAgent("orderAgent", "orderStatus", orderAgent.query(scope.read("orderNo"))));
                Future<?> logisticsFuture = executor.submit(() ->
                        scope.writeFromAgent("logisticsAgent", "logisticsStatus", logisticsAgent.check(scope.read("orderNo"))));
                orderFuture.get();
                logisticsFuture.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("agentic workflow interrupted", e);
            } catch (ExecutionException e) {
                throw new IllegalStateException("parallel agent failed", e);
            } finally {
                executor.shutdown();
            }
        }

        private void refineReplyInLoop(AgenticScope scope, int maxIterations, double passScore) {
            for (int i = 1; i <= maxIterations; i++) {
                String reply = replyAgent.draft(scope.values());
                scope.writeFromAgent("replyAgent", "customerReply", reply);
                double score = qualityAgent.score(reply);
                scope.writeFromAgent("replyQualityAgent", "replyScore", "%.2f".formatted(score));
                if (score >= passScore) {
                    scope.writeFromAgent("replyQualityAgent", "replyReview", "回复质量达标，循环结束。");
                    return;
                }
                scope.writeFromAgent("replyQualityAgent", "replyInstruction", "补充人工确认和补偿发放条件。");
            }
        }
    }

    interface IntentAgent {
        @Agent(name = "intentAgent", description = "识别用户目标", outputKey = "intent")
        String classify(@V("userGoal") String userGoal);
    }

    interface OrderAgent {
        @Agent(name = "orderAgent", description = "查询订单基础状态", outputKey = "orderStatus")
        String query(@V("orderNo") String orderNo);
    }

    interface LogisticsAgent {
        @Agent(name = "logisticsAgent", description = "查询物流停滞情况", outputKey = "logisticsStatus")
        String check(@V("orderNo") String orderNo);
    }

    interface CompensationAgent {
        @Agent(name = "compensationAgent", description = "判断补偿候选条件", outputKey = "compensationDecision")
        String evaluate(@V("orderStatus") String orderStatus, @V("logisticsStatus") String logisticsStatus);
    }

    interface ReplyAgent {
        @Agent(name = "replyAgent", description = "生成用户沟通口径", outputKey = "customerReply")
        String draft(Map<String, String> scopeValues);
    }

    interface ReplyQualityAgent {
        @Agent(name = "replyQualityAgent", description = "给用户回复打分", outputKey = "replyScore")
        double score(@V("customerReply") String customerReply);
    }

    interface SummaryAgent {
        @Agent(name = "summaryAgent", description = "汇总工作流结果", outputKey = "finalAnswer")
        String summarize(Map<String, String> scopeValues);
    }

    static class LocalIntentAgent implements IntentAgent {
        private final ModelSettings settings;

        LocalIntentAgent(ModelSettings settings) {
            this.settings = settings;
        }

        @Override
        public String classify(String userGoal) {
            return "[" + settings.provider().id() + "] after-sales-compensation";
        }
    }

    static class LocalOrderAgent implements OrderAgent {
        @Override
        public String query(String orderNo) {
            return "订单 " + orderNo + " 已出库 40 小时，承诺 24 小时送达。";
        }
    }

    static class LocalLogisticsAgent implements LogisticsAgent {
        @Override
        public String check(String orderNo) {
            return "物流停留在杭州分拨中心，18 小时未更新。";
        }
    }

    static class LocalCompensationAgent implements CompensationAgent {
        @Override
        public String evaluate(String orderStatus, String logisticsStatus) {
            if (orderStatus.contains("40 小时") && logisticsStatus.contains("18 小时")) {
                return "ELIGIBLE：订单超时且物流停滞，建议创建补偿工单，发放前需要人工确认。";
            }
            return "WAIT：暂未达到补偿条件，继续观察。";
        }
    }

    static class LocalReplyAgent implements ReplyAgent {
        private final ModelSettings settings;

        LocalReplyAgent(ModelSettings settings) {
            this.settings = settings;
        }

        @Override
        public String draft(Map<String, String> scopeValues) {
            String instruction = scopeValues.getOrDefault("replyInstruction", "");
            if (instruction.isBlank()) {
                return "[" + settings.provider().id() + "] 已确认订单物流异常，我们会继续处理。";
            }
            return "[" + settings.provider().id() + "] 已确认订单物流异常，当前符合补偿候选条件；补偿工单需要人工确认后发放权益。";
        }
    }

    static class LocalReplyQualityAgent implements ReplyQualityAgent {
        @Override
        public double score(String customerReply) {
            double score = 0.55;
            if (customerReply.contains("补偿")) {
                score += 0.2;
            }
            if (customerReply.contains("人工确认")) {
                score += 0.2;
            }
            if (customerReply.contains("发放权益")) {
                score += 0.1;
            }
            return Math.min(score, 1.0);
        }
    }

    static class LocalSummaryAgent implements SummaryAgent {
        private final ModelSettings settings;

        LocalSummaryAgent(ModelSettings settings) {
            this.settings = settings;
        }

        @Override
        public String summarize(Map<String, String> scopeValues) {
            return "[" + settings.provider().id() + "] Agentic workflow completed: "
                    + scopeValues.get("compensationDecision") + "；用户回复："
                    + scopeValues.get("customerReply");
        }
    }

    static class AgenticScope {
        private final Map<String, String> values = new LinkedHashMap<>();
        private final List<AgentInvocation> invocations = new ArrayList<>();

        void writeInput(String key, String value) {
            values.put(key, value);
        }

        void writeFromAgent(String agentName, String outputKey, String value) {
            values.put(outputKey, value);
            invocations.add(new AgentInvocation(agentName, outputKey, value));
        }

        String read(String key) {
            return values.getOrDefault(key, "");
        }

        Map<String, String> values() {
            return Map.copyOf(values);
        }

        List<AgentInvocation> invocations() {
            return List.copyOf(invocations);
        }
    }

    record AgentInvocation(String agentName, String outputKey, String value) {
        String toLogLine() {
            return agentName + " -> " + outputKey + " = " + value;
        }
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Agent {
        String name();

        String description();

        String outputKey();
    }

    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    @interface V {
        String value();
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
