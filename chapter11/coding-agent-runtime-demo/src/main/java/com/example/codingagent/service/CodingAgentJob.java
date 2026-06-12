package com.example.codingagent.service;

import com.example.codingagent.domain.ApprovalDecision;
import com.example.codingagent.domain.RuntimeReport;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理台里的任务实例。
 * 真实企业平台里这类对象通常会落到数据库，这里用内存保存，方便课程示例直接运行。
 */
public final class CodingAgentJob {

    private final String taskId;
    private final String repositoryName;
    private final String branchName;
    private final String description;
    private final String operator;
    private final Path workspaceRoot;
    private final Instant createdAt;
    private final List<String> auditEvents = new ArrayList<>();
    private CodingTaskStatus status;
    private Instant updatedAt;
    private ApprovalDecision approvalDecision;
    private RuntimeReport report;
    private String errorMessage;

    public CodingAgentJob(String taskId, String repositoryName, String branchName, String description,
                          String operator, Path workspaceRoot) {
        this.taskId = taskId;
        this.repositoryName = repositoryName;
        this.branchName = branchName;
        this.description = description;
        this.operator = operator;
        this.workspaceRoot = workspaceRoot;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
        this.status = CodingTaskStatus.WAITING_APPROVAL;
        addAudit("任务已创建，等待审批");
    }

    public void approve(ApprovalDecision decision) {
        this.approvalDecision = decision;
        this.status = CodingTaskStatus.APPROVED;
        this.updatedAt = Instant.now();
        addAudit("审批通过：" + decision.comment());
    }

    public void markRunning() {
        this.status = CodingTaskStatus.RUNNING;
        this.updatedAt = Instant.now();
        addAudit("Agent 开始执行");
    }

    public void markCompleted(RuntimeReport report) {
        this.report = report;
        this.status = CodingTaskStatus.COMPLETED;
        this.updatedAt = Instant.now();
        addAudit("Agent 执行完成，测试结果=" + report.testsPassed());
    }

    public void markFailed(String errorMessage) {
        this.errorMessage = errorMessage;
        this.status = CodingTaskStatus.FAILED;
        this.updatedAt = Instant.now();
        addAudit("Agent 执行失败：" + errorMessage);
    }

    public void addAudit(String message) {
        auditEvents.add(Instant.now() + " " + message);
    }

    public String taskId() {
        return taskId;
    }

    public String repositoryName() {
        return repositoryName;
    }

    public String branchName() {
        return branchName;
    }

    public String description() {
        return description;
    }

    public String operator() {
        return operator;
    }

    public Path workspaceRoot() {
        return workspaceRoot;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public CodingTaskStatus status() {
        return status;
    }

    public ApprovalDecision approvalDecision() {
        return approvalDecision;
    }

    public RuntimeReport report() {
        return report;
    }

    public String errorMessage() {
        return errorMessage;
    }

    public List<String> auditEvents() {
        return List.copyOf(auditEvents);
    }
}
