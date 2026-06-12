package com.example.springbootagentruntime;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public class SpringBootAgentRuntimeDemo {

    public static void main(String[] args) {
        ModelSettings settings = ModelSettings.fromEnv();
        AfterSalesFixtures fixtures = new AfterSalesFixtures();
        AfterSalesTools afterSalesTools = new AfterSalesTools(
                new InMemoryOrderRepository(fixtures.orders()),
                new InMemoryLogisticsGateway(fixtures.logistics()),
                new InMemoryCompensationTicketRepository()
        );

        AgentController controller = new AgentController(new AgentRuntimeService(
                settings,
                new DeterministicAfterSalesPlanner(settings),
                ToolRegistry.afterSales(afterSalesTools),
                new InMemoryAgentTraceRepository(),
                new ConsoleAgentEventPublisher(),
                new ToolPermissionGuard(),
                new StopCondition(8)
        ));

        AgentRunResponse response = controller.run(new AgentRunRequest(
                "u-1001",
                "帮我检查订单 A20260001 为什么还没送到，如果符合规则就生成补偿处理方案",
                "A20260001",
                false
        ));

        System.out.println("Run id: " + response.runId());
        System.out.println("Status: " + response.status());
        response.steps().forEach(step -> System.out.println(step.toLogLine()));
        System.out.println("Pending approval: " + response.pendingApproval().orElse("none"));
        System.out.println("Final answer: " + response.finalAnswer());
    }

    static class AgentController {
        private final AgentRuntimeService runtimeService;

        AgentController(AgentRuntimeService runtimeService) {
            this.runtimeService = runtimeService;
        }

        AgentRunResponse run(AgentRunRequest request) {
            // 真实 Spring Boot 项目里，这里可以是 @PostMapping("/api/agent/runs")。
            // Controller 只做参数接收和返回，不直接拼 Prompt，也不直接执行工具。
            return runtimeService.run(request);
        }
    }

    static class AgentRuntimeService {
        private final ModelSettings settings;
        private final AgentPlanner planner;
        private final ToolRegistry toolRegistry;
        private final AgentTraceRepository traceRepository;
        private final AgentEventPublisher eventPublisher;
        private final ToolPermissionGuard permissionGuard;
        private final StopCondition stopCondition;

        AgentRuntimeService(ModelSettings settings, AgentPlanner planner, ToolRegistry toolRegistry,
                            AgentTraceRepository traceRepository, AgentEventPublisher eventPublisher,
                            ToolPermissionGuard permissionGuard, StopCondition stopCondition) {
            this.settings = settings;
            this.planner = planner;
            this.toolRegistry = toolRegistry;
            this.traceRepository = traceRepository;
            this.eventPublisher = eventPublisher;
            this.permissionGuard = permissionGuard;
            this.stopCondition = stopCondition;
        }

        AgentRunResponse run(AgentRunRequest request) {
            String runId = UUID.randomUUID().toString();
            AgentState state = new AgentState(runId, request.userId(), request.goal(), request.orderNo());
            AgentRunTrace trace = AgentRunTrace.started(runId, request.userId(), request.goal());
            traceRepository.save(trace);
            eventPublisher.publish(runId, "started provider=" + settings.provider().id());

            while (!stopCondition.shouldStop(state)) {
                AgentDecision decision = planner.next(state);
                if (decision.finalAnswer().isPresent()) {
                    trace.finish(decision.finalAnswer().orElseThrow(), state.pendingApproval());
                    traceRepository.save(trace);
                    eventPublisher.publish(runId, "finished");
                    return AgentRunResponse.from(trace);
                }

                ToolCall toolCall = decision.toolCall().orElseThrow();
                ToolDefinition tool = toolRegistry.get(toolCall.toolName());
                ToolAuthorization authorization = permissionGuard.authorize(request, tool);
                ToolObservation observation = authorization.allowed()
                        ? toolRegistry.call(toolCall)
                        : ToolObservation.blocked(authorization.reason(), Map.of("pendingApproval", authorization.reason()));

                AgentStepRecord step = new AgentStepRecord(
                        state.nextStepNo(),
                        decision.thought(),
                        toolCall.toolName(),
                        toolCall.arguments(),
                        observation.status(),
                        observation.content(),
                        Instant.now()
                );
                state.record(step, observation.facts());
                trace.addStep(step);
                traceRepository.save(trace);
                eventPublisher.publish(runId, "step=" + step.stepNo() + " tool=" + step.toolName() + " status=" + step.status());
            }

            trace.finish("达到最大执行步数，已停止并等待人工复核。", state.pendingApproval());
            traceRepository.save(trace);
            eventPublisher.publish(runId, "stopped by max steps");
            return AgentRunResponse.from(trace);
        }
    }

    interface AgentPlanner {
        AgentDecision next(AgentState state);
    }

    static class DeterministicAfterSalesPlanner implements AgentPlanner {
        private final ModelSettings settings;

        DeterministicAfterSalesPlanner(ModelSettings settings) {
            this.settings = settings;
        }

        @Override
        public AgentDecision next(AgentState state) {
            if (!state.hasFact("orderStatus")) {
                return AgentDecision.callTool(
                        "[" + settings.provider().id() + "] 先查询订单基础状态，确认是否已经出库和是否超过承诺时间。",
                        new ToolCall("queryOrder", Map.of("orderNo", state.orderNo())));
            }
            if (!state.hasFact("logisticsDelayHours")) {
                return AgentDecision.callTool(
                        "订单已经出库，继续查询物流轨迹，判断是否停滞。",
                        new ToolCall("queryLogistics", Map.of("orderNo", state.orderNo())));
            }
            if (!state.hasFact("compensationDecision")) {
                return AgentDecision.callTool(
                        "订单和物流信息都齐了，开始套补偿规则。",
                        new ToolCall("evaluateCompensation", Map.of("orderNo", state.orderNo())));
            }
            if (!state.hasFact("customerReply")) {
                return AgentDecision.callTool(
                        "先生成给用户看的处理口径，避免只给内部结论。",
                        new ToolCall("draftCustomerReply", Map.of("orderNo", state.orderNo())));
            }
            if ("ELIGIBLE".equals(state.fact("compensationDecision"))
                    && !state.hasFact("ticketNo")
                    && state.pendingApproval().isEmpty()) {
                return AgentDecision.callTool(
                        "符合补偿规则，但创建补偿工单是写操作，需要经过权限守卫。",
                        new ToolCall("createCompensationTicket", Map.of("orderNo", state.orderNo())));
            }
            return AgentDecision.finish(buildFinalAnswer(state));
        }

        private String buildFinalAnswer(AgentState state) {
            return "订单 " + state.orderNo() + " 处理完成："
                    + state.fact("compensationReason") + "；用户回复口径："
                    + state.fact("customerReply");
        }
    }

    static class ToolRegistry {
        private final Map<String, ToolDefinition> tools = new LinkedHashMap<>();

        static ToolRegistry afterSales(AfterSalesTools afterSalesTools) {
            ToolRegistry registry = new ToolRegistry();
            registry.register(new ToolDefinition("queryOrder", "查询订单状态", ToolRisk.READ_ONLY, afterSalesTools::queryOrder));
            registry.register(new ToolDefinition("queryLogistics", "查询物流轨迹", ToolRisk.READ_ONLY, afterSalesTools::queryLogistics));
            registry.register(new ToolDefinition("evaluateCompensation", "评估补偿规则", ToolRisk.READ_ONLY, afterSalesTools::evaluateCompensation));
            registry.register(new ToolDefinition("draftCustomerReply", "生成用户回复口径", ToolRisk.READ_ONLY, afterSalesTools::draftCustomerReply));
            registry.register(new ToolDefinition("createCompensationTicket", "创建补偿工单", ToolRisk.WRITE_REQUIRES_CONFIRMATION, afterSalesTools::createCompensationTicket));
            return registry;
        }

        void register(ToolDefinition tool) {
            tools.put(tool.name(), tool);
        }

        ToolDefinition get(String name) {
            ToolDefinition tool = tools.get(name);
            if (tool == null) {
                throw new IllegalArgumentException("unknown tool: " + name);
            }
            return tool;
        }

        ToolObservation call(ToolCall call) {
            try {
                return get(call.toolName()).handler().apply(call);
            } catch (RuntimeException e) {
                return ToolObservation.failed("工具执行失败：" + e.getMessage());
            }
        }
    }

    static class ToolPermissionGuard {
        ToolAuthorization authorize(AgentRunRequest request, ToolDefinition tool) {
            if (tool.risk() == ToolRisk.WRITE_REQUIRES_CONFIRMATION && !request.allowWrite()) {
                return ToolAuthorization.blocked("工具 " + tool.name() + " 需要人工确认后才能执行");
            }
            return ToolAuthorization.permitted();
        }
    }

    static class AfterSalesTools {
        private final OrderRepository orderRepository;
        private final LogisticsGateway logisticsGateway;
        private final CompensationTicketRepository ticketRepository;

        AfterSalesTools(OrderRepository orderRepository, LogisticsGateway logisticsGateway,
                        CompensationTicketRepository ticketRepository) {
            this.orderRepository = orderRepository;
            this.logisticsGateway = logisticsGateway;
            this.ticketRepository = ticketRepository;
        }

        ToolObservation queryOrder(ToolCall call) {
            OrderSnapshot order = orderRepository.findByOrderNo(call.arguments().get("orderNo"));
            return ToolObservation.success(
                    "订单已出库 " + order.shippedHoursAgo() + " 小时，承诺 " + order.promiseHours() + " 小时送达。",
                    Map.of(
                            "orderStatus", order.status(),
                            "shippedHoursAgo", String.valueOf(order.shippedHoursAgo()),
                            "promiseHours", String.valueOf(order.promiseHours())
                    )
            );
        }

        ToolObservation queryLogistics(ToolCall call) {
            LogisticsSnapshot logistics = logisticsGateway.query(call.arguments().get("orderNo"));
            return ToolObservation.success(
                    "物流停留在 " + logistics.currentNode() + "，最近 " + logistics.hoursSinceLastUpdate() + " 小时没有更新。",
                    Map.of(
                            "logisticsNode", logistics.currentNode(),
                            "logisticsDelayHours", String.valueOf(logistics.hoursSinceLastUpdate())
                    )
            );
        }

        ToolObservation evaluateCompensation(ToolCall call) {
            OrderSnapshot order = orderRepository.findByOrderNo(call.arguments().get("orderNo"));
            LogisticsSnapshot logistics = logisticsGateway.query(call.arguments().get("orderNo"));
            boolean eligible = order.shippedHoursAgo() > order.promiseHours() && logistics.hoursSinceLastUpdate() >= 12;
            String decision = eligible ? "ELIGIBLE" : "WAIT";
            String reason = eligible
                    ? "订单超过承诺送达时间且物流停滞超过 12 小时，符合补偿候选条件"
                    : "暂未达到补偿条件，继续观察物流";
            return ToolObservation.success(reason, Map.of(
                    "compensationDecision", decision,
                    "compensationReason", reason,
                    "compensationAmount", eligible ? "20" : "0"
            ));
        }

        ToolObservation draftCustomerReply(ToolCall call) {
            String orderNo = call.arguments().get("orderNo");
            String reply = "订单 " + orderNo + " 当前物流更新异常，我已经为你记录处理方案，补偿权益会在人工确认后发放。";
            return ToolObservation.success(reply, Map.of("customerReply", reply));
        }

        ToolObservation createCompensationTicket(ToolCall call) {
            CompensationTicket ticket = ticketRepository.create(call.arguments().get("orderNo"), "物流超时补偿候选");
            return ToolObservation.success("已创建补偿工单 " + ticket.ticketNo(), Map.of("ticketNo", ticket.ticketNo()));
        }
    }

    static class AgentState {
        private final String runId;
        private final String userId;
        private final String goal;
        private final String orderNo;
        private final List<AgentStepRecord> steps = new ArrayList<>();
        private final Map<String, String> facts = new LinkedHashMap<>();

        AgentState(String runId, String userId, String goal, String orderNo) {
            this.runId = runId;
            this.userId = userId;
            this.goal = goal;
            this.orderNo = orderNo;
        }

        int nextStepNo() {
            return steps.size() + 1;
        }

        void record(AgentStepRecord step, Map<String, String> newFacts) {
            steps.add(step);
            facts.putAll(newFacts);
        }

        boolean hasFact(String key) {
            return facts.containsKey(key);
        }

        String fact(String key) {
            return facts.getOrDefault(key, "");
        }

        Optional<String> pendingApproval() {
            return Optional.ofNullable(facts.get("pendingApproval"));
        }

        String orderNo() {
            return orderNo;
        }
    }

    interface AgentTraceRepository {
        void save(AgentRunTrace trace);

        Optional<AgentRunTrace> findByRunId(String runId);
    }

    static class InMemoryAgentTraceRepository implements AgentTraceRepository {
        private final Map<String, AgentRunTrace> traces = new LinkedHashMap<>();

        @Override
        public void save(AgentRunTrace trace) {
            traces.put(trace.runId(), trace);
        }

        @Override
        public Optional<AgentRunTrace> findByRunId(String runId) {
            return Optional.ofNullable(traces.get(runId));
        }
    }

    interface AgentEventPublisher {
        void publish(String runId, String message);
    }

    static class ConsoleAgentEventPublisher implements AgentEventPublisher {
        @Override
        public void publish(String runId, String message) {
            // 真实项目可以换成 SSE、WebSocket、消息队列或 OpenTelemetry span。
            System.out.println("[event] " + runId + " " + message);
        }
    }

    interface OrderRepository {
        OrderSnapshot findByOrderNo(String orderNo);
    }

    static class InMemoryOrderRepository implements OrderRepository {
        private final Map<String, OrderSnapshot> orders;

        InMemoryOrderRepository(Map<String, OrderSnapshot> orders) {
            this.orders = orders;
        }

        @Override
        public OrderSnapshot findByOrderNo(String orderNo) {
            OrderSnapshot order = orders.get(orderNo);
            if (order == null) {
                throw new IllegalArgumentException("order not found: " + orderNo);
            }
            return order;
        }
    }

    interface LogisticsGateway {
        LogisticsSnapshot query(String orderNo);
    }

    static class InMemoryLogisticsGateway implements LogisticsGateway {
        private final Map<String, LogisticsSnapshot> logistics;

        InMemoryLogisticsGateway(Map<String, LogisticsSnapshot> logistics) {
            this.logistics = logistics;
        }

        @Override
        public LogisticsSnapshot query(String orderNo) {
            LogisticsSnapshot snapshot = logistics.get(orderNo);
            if (snapshot == null) {
                throw new IllegalArgumentException("logistics not found: " + orderNo);
            }
            return snapshot;
        }
    }

    interface CompensationTicketRepository {
        CompensationTicket create(String orderNo, String reason);
    }

    static class InMemoryCompensationTicketRepository implements CompensationTicketRepository {
        private int sequence = 10086;

        @Override
        public CompensationTicket create(String orderNo, String reason) {
            return new CompensationTicket("T-" + sequence++, orderNo, reason);
        }
    }

    static class AfterSalesFixtures {
        Map<String, OrderSnapshot> orders() {
            return Map.of("A20260001", new OrderSnapshot("A20260001", "SHIPPED", 24, 40));
        }

        Map<String, LogisticsSnapshot> logistics() {
            return Map.of("A20260001", new LogisticsSnapshot("杭州分拨中心", 18));
        }
    }

    record AgentRunRequest(String userId, String goal, String orderNo, boolean allowWrite) {
    }

    record AgentRunResponse(String runId, String status, String finalAnswer, Optional<String> pendingApproval,
                            List<AgentStepRecord> steps) {
        static AgentRunResponse from(AgentRunTrace trace) {
            return new AgentRunResponse(trace.runId(), trace.status(), trace.finalAnswer(),
                    trace.pendingApproval(), trace.steps());
        }
    }

    static class AgentRunTrace {
        private final String runId;
        private final String userId;
        private final String goal;
        private final Instant createdAt;
        private final List<AgentStepRecord> steps = new ArrayList<>();
        private String status = "RUNNING";
        private String finalAnswer = "";
        private Optional<String> pendingApproval = Optional.empty();

        static AgentRunTrace started(String runId, String userId, String goal) {
            return new AgentRunTrace(runId, userId, goal, Instant.now());
        }

        AgentRunTrace(String runId, String userId, String goal, Instant createdAt) {
            this.runId = runId;
            this.userId = userId;
            this.goal = goal;
            this.createdAt = createdAt;
        }

        void addStep(AgentStepRecord step) {
            steps.add(step);
        }

        void finish(String finalAnswer, Optional<String> pendingApproval) {
            this.status = pendingApproval.isPresent() ? "WAITING_APPROVAL" : "FINISHED";
            this.finalAnswer = finalAnswer;
            this.pendingApproval = pendingApproval;
        }

        String runId() {
            return runId;
        }

        String status() {
            return status;
        }

        String finalAnswer() {
            return finalAnswer;
        }

        Optional<String> pendingApproval() {
            return pendingApproval;
        }

        List<AgentStepRecord> steps() {
            return List.copyOf(steps);
        }
    }

    record AgentStepRecord(int stepNo, String thought, String toolName, Map<String, String> toolArgs,
                           String status, String observation, Instant createdAt) {
        String toLogLine() {
            return "step=" + stepNo + ", tool=" + toolName + ", status=" + status
                    + ", thought=" + thought + ", observation=" + observation;
        }
    }

    record ToolDefinition(String name, String description, ToolRisk risk, Function<ToolCall, ToolObservation> handler) {
    }

    enum ToolRisk {
        READ_ONLY,
        WRITE_REQUIRES_CONFIRMATION
    }

    record ToolCall(String toolName, Map<String, String> arguments) {
    }

    record ToolObservation(String status, String content, Map<String, String> facts) {
        static ToolObservation success(String content, Map<String, String> facts) {
            return new ToolObservation("SUCCESS", content, facts);
        }

        static ToolObservation failed(String content) {
            return new ToolObservation("FAILED", content, Map.of());
        }

        static ToolObservation blocked(String content, Map<String, String> facts) {
            return new ToolObservation("BLOCKED", content, facts);
        }
    }

    record ToolAuthorization(boolean allowed, String reason) {
        static ToolAuthorization permitted() {
            return new ToolAuthorization(true, "");
        }

        static ToolAuthorization blocked(String reason) {
            return new ToolAuthorization(false, reason);
        }
    }

    record AgentDecision(String thought, Optional<ToolCall> toolCall, Optional<String> finalAnswer) {
        static AgentDecision callTool(String thought, ToolCall toolCall) {
            return new AgentDecision(thought, Optional.of(toolCall), Optional.empty());
        }

        static AgentDecision finish(String finalAnswer) {
            return new AgentDecision("任务已经可以收束。", Optional.empty(), Optional.of(finalAnswer));
        }
    }

    record StopCondition(int maxSteps) {
        boolean shouldStop(AgentState state) {
            return state.nextStepNo() > maxSteps;
        }
    }

    record OrderSnapshot(String orderNo, String status, int promiseHours, int shippedHoursAgo) {
    }

    record LogisticsSnapshot(String currentNode, int hoursSinceLastUpdate) {
    }

    record CompensationTicket(String ticketNo, String orderNo, String reason) {
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
