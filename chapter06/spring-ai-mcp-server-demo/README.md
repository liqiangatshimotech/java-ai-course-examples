# 6.2 Spring AI MCP Server Demo

本模块对应课程 `6.2｜Spring AI MCP Server`。

示例使用 Spring AI 1.1.7 MCP Server WebMVC starter，默认以 Streamable HTTP 暴露 MCP endpoint：

- Tool：`query_order_status`、`suggest_refund_action`
- Resource：`policy://refund/{tenantId}`
- Prompt：`refund_assistant`
- 工程关注点：参数校验、租户权限、结果裁剪、审计日志

运行：

```bash
mvn -pl chapter06/spring-ai-mcp-server-demo spring-boot:run
```

默认 MCP endpoint：

```text
http://localhost:8082/mcp
```

测试：

```bash
mvn -pl chapter06/spring-ai-mcp-server-demo test
```
