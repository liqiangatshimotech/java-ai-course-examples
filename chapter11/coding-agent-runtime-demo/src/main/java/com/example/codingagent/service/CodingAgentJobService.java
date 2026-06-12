package com.example.codingagent.service;

import com.example.codingagent.config.ModelSettings;
import com.example.codingagent.domain.ApprovalDecision;
import com.example.codingagent.domain.CodingTask;
import com.example.codingagent.domain.RuntimeReport;
import com.example.codingagent.model.RuleBasedCodingModel;
import com.example.codingagent.runtime.ApprovalService;
import com.example.codingagent.runtime.AuditLogger;
import com.example.codingagent.runtime.CodingAgentRuntime;
import com.example.codingagent.runtime.ToolRiskPolicy;
import com.example.codingagent.tool.GitDiffTool;
import com.example.codingagent.tool.ListFilesTool;
import com.example.codingagent.tool.ReadFileTool;
import com.example.codingagent.tool.RunTestsTool;
import com.example.codingagent.tool.SearchCodeTool;
import com.example.codingagent.tool.ToolRegistry;
import com.example.codingagent.tool.WriteFileTool;
import com.example.codingagent.workspace.DemoWorkspaceFactory;
import com.example.codingagent.workspace.WorkspaceBoundary;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Coding Agent 任务中心。
 * 这里模拟企业系统里的任务表、审批流和执行编排，UI 与 REST API 都围绕这个服务工作。
 */
@Service
public class CodingAgentJobService {

    private static final DateTimeFormatter ID_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            .withZone(ZoneId.systemDefault());

    private final AtomicInteger sequence = new AtomicInteger(1000);
    private final Map<String, CodingAgentJob> jobs = new ConcurrentHashMap<>();
    private final Path workspaceBase = Path.of("target/coding-agent-enterprise-workspaces").toAbsolutePath().normalize();

    public CodingAgentJob createJob(String repositoryName, String branchName, String description,
                                    String operator) throws IOException {
        String taskId = "CA-" + ID_TIME.format(Instant.now()) + "-" + sequence.incrementAndGet();
        Path workspaceRoot = workspaceBase.resolve("coding-agent-task-" + taskId.toLowerCase(Locale.ROOT));
        DemoWorkspaceFactory.createFreshWorkspace(workspaceRoot);
        CodingAgentJob job = new CodingAgentJob(
                taskId,
                firstNonBlank(repositoryName, "billing-service"),
                firstNonBlank(branchName, "feature/member-discount-limit"),
                description,
                firstNonBlank(operator, "course-demo"),
                workspaceRoot
        );
        jobs.put(taskId, job);
        return job;
    }

    public CodingAgentJob approve(String taskId, String approver, String comment) {
        CodingAgentJob job = requireJob(taskId);
        if (job.status() != CodingTaskStatus.WAITING_APPROVAL) {
            throw new IllegalStateException("只有 WAITING_APPROVAL 状态的任务可以审批");
        }
        job.approve(ApprovalDecision.approved(firstNonBlank(approver, "reviewer"),
                firstNonBlank(comment, "同意执行受控写入和回归测试")));
        return job;
    }

    public CodingAgentJob run(String taskId) {
        CodingAgentJob job = requireJob(taskId);
        if (job.status() != CodingTaskStatus.APPROVED) {
            throw new IllegalStateException("任务必须先审批通过，当前状态=" + job.status());
        }
        job.markRunning();
        try {
            RuntimeReport report = buildRuntime(job).run(
                    new CodingTask(job.taskId(), job.description(), job.operator()),
                    job.workspaceRoot()
            );
            job.markCompleted(report);
        } catch (Exception exception) {
            job.markFailed(exception.getMessage());
        }
        return job;
    }

    public List<CodingAgentJob> listJobs() {
        return jobs.values().stream()
                .sorted(Comparator.comparing(CodingAgentJob::createdAt).reversed())
                .toList();
    }

    public CodingAgentJob requireJob(String taskId) {
        CodingAgentJob job = jobs.get(taskId);
        if (job == null) {
            throw new CodingAgentJobNotFoundException(taskId);
        }
        return job;
    }

    public String readWorkspaceFile(String taskId, String relativePath) throws IOException {
        CodingAgentJob job = requireJob(taskId);
        Path path = new WorkspaceBoundary(job.workspaceRoot()).resolve(relativePath);
        return Files.readString(path);
    }

    public List<String> registeredToolNames() {
        return defaultToolRegistry().names();
    }

    public Path workspaceBase() {
        return workspaceBase;
    }

    private CodingAgentRuntime buildRuntime(CodingAgentJob job) {
        ApprovalDecision approved = job.approvalDecision();
        ApprovalService approvalService = (task, proposal) -> approved;
        AuditLogger auditLogger = (session, event, detail) -> job.addAudit(event + " | " + detail);
        return new CodingAgentRuntime(
                ModelSettings.fromEnvironment(),
                new RuleBasedCodingModel(),
                defaultToolRegistry(),
                approvalService,
                new ToolRiskPolicy(),
                auditLogger
        );
    }

    private ToolRegistry defaultToolRegistry() {
        return new ToolRegistry()
                .register(new ListFilesTool())
                .register(new SearchCodeTool())
                .register(new ReadFileTool())
                .register(new WriteFileTool())
                .register(new RunTestsTool())
                .register(new GitDiffTool());
    }

    private String firstNonBlank(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
