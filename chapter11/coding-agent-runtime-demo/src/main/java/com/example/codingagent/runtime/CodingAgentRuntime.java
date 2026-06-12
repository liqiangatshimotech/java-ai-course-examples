package com.example.codingagent.runtime;

import com.example.codingagent.config.ModelSettings;
import com.example.codingagent.domain.AgentSession;
import com.example.codingagent.domain.ApprovalDecision;
import com.example.codingagent.domain.CodeChangeProposal;
import com.example.codingagent.domain.CodingTask;
import com.example.codingagent.domain.RuntimeReport;
import com.example.codingagent.model.CodingModel;
import com.example.codingagent.tool.AgentTool;
import com.example.codingagent.tool.ToolContext;
import com.example.codingagent.tool.ToolRegistry;
import com.example.codingagent.tool.ToolResult;
import com.example.codingagent.workspace.WorkspaceBoundary;
import com.example.codingagent.workspace.WorkspaceSnapshot;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * 一个最小但完整的 Coding Agent Runtime。
 * 它没有把模型当成万能黑盒，而是用工具、安全策略和审批服务约束模型产生的动作。
 */
public final class CodingAgentRuntime {

    private final ModelSettings settings;
    private final CodingModel model;
    private final ToolRegistry toolRegistry;
    private final ApprovalService approvalService;
    private final ToolRiskPolicy riskPolicy;
    private final AuditLogger auditLogger;

    public CodingAgentRuntime(ModelSettings settings, CodingModel model, ToolRegistry toolRegistry,
                              ApprovalService approvalService, ToolRiskPolicy riskPolicy,
                              AuditLogger auditLogger) {
        this.settings = settings;
        this.model = model;
        this.toolRegistry = toolRegistry;
        this.approvalService = approvalService;
        this.riskPolicy = riskPolicy;
        this.auditLogger = auditLogger;
    }

    public RuntimeReport run(CodingTask task, Path workspaceRoot) throws IOException {
        Path normalizedRoot = workspaceRoot.toAbsolutePath().normalize();
        AgentSession session = new AgentSession(UUID.randomUUID().toString(), task, settings.summary(),
                normalizedRoot.toString());
        ToolContext context = new ToolContext(normalizedRoot, new WorkspaceBoundary(normalizedRoot),
                WorkspaceSnapshot.capture(normalizedRoot));

        auditLogger.log(session, "session_started", task.description());

        execute(session, context, "扫描工作区文件", "list_files", Map.of());
        ToolResult searchResult = execute(session, context, "搜索折扣常量", "search_code",
                Map.of("query", "MAX_DISCOUNT"));

        String targetPath = firstSearchHitPath(searchResult.output());
        ToolResult readResult = execute(session, context, "读取目标文件", "read_file",
                Map.of("path", targetPath));

        CodeChangeProposal proposal = model.proposeChange(task, targetPath, readResult.output(), settings);
        ApprovalDecision decision = approvalService.requestApproval(task, proposal);
        session.approvalDecision(decision);
        auditLogger.log(session, "approval_decision", decision.comment());
        if (!decision.approved()) {
            return new RuntimeReport(session, proposal, false, "审批未通过，未写入代码。");
        }

        execute(session, context, "写入模型生成的代码", "write_file",
                Map.of("path", proposal.targetPath(), "content", proposal.newContent()));
        ToolResult testResult = execute(session, context, "执行回归测试", "run_tests", Map.of());
        ToolResult diffResult = execute(session, context, "生成交付 diff", "git_diff", Map.of());

        auditLogger.log(session, "session_finished", "testsPassed=" + testResult.success());
        return new RuntimeReport(session, proposal, testResult.success(), diffResult.output());
    }

    private ToolResult execute(AgentSession session, ToolContext context, String stepName, String toolName,
                               Map<String, String> arguments) throws IOException {
        AgentTool tool = toolRegistry.require(toolName);
        if (riskPolicy.requiresApproval(tool.risk()) && !isApproved(session.approvalDecision())) {
            throw new IllegalStateException("Tool " + toolName + " requires approval before execution");
        }

        Instant startedAt = Instant.now();
        ToolResult result = tool.execute(context, arguments);
        Instant endedAt = Instant.now();
        session.recordStep(stepName, toolName, startedAt, endedAt, result);
        auditLogger.log(session, "tool_call", toolName + " -> " + result.summary());
        return result;
    }

    private boolean isApproved(ApprovalDecision decision) {
        return decision != null && decision.approved();
    }

    private String firstSearchHitPath(String searchOutput) {
        if (searchOutput == null || searchOutput.isBlank()) {
            throw new IllegalStateException("No search result found for target code");
        }
        String firstLine = searchOutput.lines().findFirst()
                .orElseThrow(() -> new IllegalStateException("No search result found for target code"));
        int separator = firstLine.indexOf(':');
        if (separator < 0) {
            throw new IllegalStateException("Unexpected search result format: " + firstLine);
        }
        return firstLine.substring(0, separator);
    }
}
