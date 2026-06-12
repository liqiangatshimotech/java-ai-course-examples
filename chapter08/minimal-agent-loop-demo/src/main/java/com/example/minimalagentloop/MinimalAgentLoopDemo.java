package com.example.minimalagentloop;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class MinimalAgentLoopDemo {

    public static void main(String[] args) {
        ModelSettings settings = ModelSettings.fromEnv();
        ToolRegistry tools = new ToolRegistry();
        tools.register(new ToolDefinition("queryOrder", "查询订单状态", argsMap ->
                Observation.success("订单 A20260001 已出库，承诺送达时间已超 16 小时。")));
        tools.register(new ToolDefinition("queryLogistics", "查询物流轨迹", argsMap ->
                Observation.success("物流停留在杭州分拨中心，最近 18 小时没有更新。")));
        tools.register(new ToolDefinition("createCompensationTicket", "创建补偿工单", argsMap ->
                Observation.success("已创建补偿工单 T-10086，等待人工确认后发放优惠券。")));

        AgentRuntime runtime = new AgentRuntime(
                new DeterministicPlanner(settings),
                tools,
                new StopCondition(5)
        );

        AgentResult result = runtime.run("帮我处理订单 A20260001 迟迟没到的问题，判断是否需要补偿");
        System.out.println("Provider: " + settings.provider().id());
        result.steps().forEach(step -> System.out.println(step.toLogLine()));
        System.out.println("Final answer: " + result.finalAnswer());
    }

    static class AgentRuntime {
        private final Planner planner;
        private final ToolRegistry tools;
        private final StopCondition stopCondition;

        AgentRuntime(Planner planner, ToolRegistry tools, StopCondition stopCondition) {
            this.planner = planner;
            this.tools = tools;
            this.stopCondition = stopCondition;
        }

        AgentResult run(String userGoal) {
            AgentState state = new AgentState(userGoal);
            while (!stopCondition.shouldStop(state)) {
                AgentDecision decision = planner.next(state);
                if (decision.finalAnswer().isPresent()) {
                    return new AgentResult(state.steps(), decision.finalAnswer().orElseThrow());
                }

                ToolCall toolCall = decision.toolCall().orElseThrow();
                Observation observation = tools.call(toolCall);
                state.record(new AgentStep(
                        state.nextStepNo(),
                        decision.thought(),
                        toolCall.toolName(),
                        toolCall.arguments(),
                        observation.content()
                ));
            }
            return new AgentResult(state.steps(), "达到最大执行步数，先停止并交给人工确认。");
        }
    }

    interface Planner {
        AgentDecision next(AgentState state);
    }

    static class DeterministicPlanner implements Planner {
        private final ModelSettings settings;

        DeterministicPlanner(ModelSettings settings) {
            this.settings = settings;
        }

        @Override
        public AgentDecision next(AgentState state) {
            int stepNo = state.nextStepNo();
            return switch (stepNo) {
                case 1 -> AgentDecision.callTool(
                        "[" + settings.provider().id() + "] 先确认订单是否真的超时。",
                        new ToolCall("queryOrder", Map.of("orderNo", "A20260001")));
                case 2 -> AgentDecision.callTool(
                        "订单已超时，需要继续看物流是否停滞。",
                        new ToolCall("queryLogistics", Map.of("orderNo", "A20260001")));
                case 3 -> AgentDecision.callTool(
                        "物流长时间无更新，补偿动作需要留痕并等待人工确认。",
                        new ToolCall("createCompensationTicket", Map.of("orderNo", "A20260001", "reason", "物流超时")));
                default -> AgentDecision.finish("订单已超时且物流停滞，已创建补偿工单，下一步等待人工确认。");
            };
        }
    }

    static class ToolRegistry {
        private final Map<String, ToolDefinition> tools = new LinkedHashMap<>();

        void register(ToolDefinition tool) {
            tools.put(tool.name(), tool);
        }

        Observation call(ToolCall call) {
            ToolDefinition tool = tools.get(call.toolName());
            if (tool == null) {
                return Observation.failed("工具不存在: " + call.toolName());
            }
            try {
                return tool.handler().apply(call.arguments());
            } catch (RuntimeException e) {
                return Observation.failed("工具执行失败: " + e.getMessage());
            }
        }
    }

    static class AgentState {
        private final String userGoal;
        private final List<AgentStep> steps = new ArrayList<>();

        AgentState(String userGoal) {
            this.userGoal = userGoal;
        }

        int nextStepNo() {
            return steps.size() + 1;
        }

        void record(AgentStep step) {
            steps.add(step);
        }

        List<AgentStep> steps() {
            return List.copyOf(steps);
        }

        String userGoal() {
            return userGoal;
        }
    }

    record ToolDefinition(String name, String description, Function<Map<String, String>, Observation> handler) {
    }

    record ToolCall(String toolName, Map<String, String> arguments) {
    }

    record Observation(boolean success, String content) {
        static Observation success(String content) {
            return new Observation(true, content);
        }

        static Observation failed(String content) {
            return new Observation(false, content);
        }
    }

    record AgentDecision(String thought, Optional<ToolCall> toolCall, Optional<String> finalAnswer) {
        static AgentDecision callTool(String thought, ToolCall toolCall) {
            return new AgentDecision(thought, Optional.of(toolCall), Optional.empty());
        }

        static AgentDecision finish(String answer) {
            return new AgentDecision("任务已经可以收束。", Optional.empty(), Optional.of(answer));
        }
    }

    record AgentStep(int stepNo, String thought, String toolName, Map<String, String> toolArgs, String observation) {
        String toLogLine() {
            return "step=" + stepNo + ", tool=" + toolName + ", thought=" + thought + ", observation=" + observation;
        }
    }

    record StopCondition(int maxSteps) {
        boolean shouldStop(AgentState state) {
            return state.nextStepNo() > maxSteps;
        }
    }

    record AgentResult(List<AgentStep> steps, String finalAnswer) {
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
