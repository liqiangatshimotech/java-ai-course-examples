# Support Ticket Copilot Demo

本模块对应课程 `2.6｜实战项目：智能客服工单助手`，把第一章和第二章的知识点合成一个接近企业项目结构的 Spring Boot 示例。

它演示：

- 第一章：Spring AI `ChatClient`、同步调用、SSE 流式输出。
- 第二章：Prompt 契约、结构化输出 DTO、Bean Validation、失败重试、兜底结果。
- 工程化分层：Controller、Service、AI Gateway、Prompt Builder、DTO、异常处理、配置和单元测试。

## 运行

```bash
ollama pull qwen2.5:7b
mvn spring-boot:run
```

默认端口是 `8083`。

## 接口

结构化分析：

```bash
curl -sS http://localhost:8083/support/tickets/analyze \
  -H 'Content-Type: application/json' \
  -d '{
    "content": "企业版客户反馈昨天扣费两次，发票也开不出来，采购负责人今天要报销。",
    "channel": "WEB",
    "customerTier": "ENTERPRISE"
  }'
```

流式生成客服回复草稿：

```bash
curl -N 'http://localhost:8083/support/tickets/reply/stream?content=企业版客户反馈昨天扣费两次，发票也开不出来，采购负责人今天要报销。&channel=WEB'
```

## 代码入口

| 文件 | 讲解重点 |
|---|---|
| `TicketController` | HTTP API、请求校验、SSE 事件封装 |
| `TicketCopilotService` | 业务编排、重试、修复提示、兜底 |
| `TicketAiGateway` | AI 调用接口隔离，方便替换模型或测试 |
| `SpringAiTicketAiGateway` | Spring AI `ChatClient.entity(...)` 和流式输出 |
| `TicketPromptBuilder` | Prompt 契约集中管理 |
| `TicketAnalysis` | 结构化输出 DTO 和字段约束 |
| `TicketCopilotServiceTest` | 不依赖真实模型的重试和兜底测试 |

## 配置

```bash
export OLLAMA_BASE_URL=http://localhost:11434
export OLLAMA_MODEL=qwen2.5:7b
export TICKET_COPILOT_MAX_ATTEMPTS=2
```

生产项目里应继续补充模型调用超时、限流、审计日志、敏感信息脱敏和可观测指标；这些内容会放到后续生产化章节。
