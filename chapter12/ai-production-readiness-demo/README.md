# AI Production Readiness Demo

项目名称：`ai-production-readiness-demo`

这个项目对应 `12｜AI 应用生产化`，用纯 Java 演示 AI 应用上线前必须补齐的治理层：

- `CostControlDemo`：Token 估算、模型路由和预算检查。
- `ObservabilityDemo`：TraceId、Agent Step、Tool Call 和审计日志。
- `EvaluationDemo`：离线评测集、工具调用评测和回归测试结果。
- `SecurityComplianceDemo`：敏感信息脱敏、Prompt Injection 检测和高风险工具审批。
- `ProductionReadinessDemo`：把成本、观测、评测、安全合并成上线前检查报告。

默认模型供应商是 DeepSeek，同时保留 Ollama 和 ChatGPT 配置入口。这个项目不真实请求模型，方便在没有 API Key 的情况下验证生产化代码结构。

## 运行

```bash
mvn -pl chapter12/ai-production-readiness-demo exec:java
```

切换供应商只影响配置摘要，不会发起真实模型请求：

```bash
export AI_PROVIDER=deepseek
export DEEPSEEK_MODEL=deepseek-chat

export AI_PROVIDER=ollama
export OLLAMA_MODEL=qwen3

export AI_PROVIDER=chatgpt
export OPENAI_MODEL=gpt-4o-mini
```

## 测试

```bash
mvn -pl chapter12/ai-production-readiness-demo test
```

