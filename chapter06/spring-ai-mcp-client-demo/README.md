# 6.3 Spring AI MCP Client Demo

本模块对应课程 `6.3｜Spring AI MCP Client`。

示例演示 Spring AI 应用如何连接 6.2 的 MCP Server，自动发现远端工具，并把 MCP 工具挂到 `ChatClient` 上。

## 先启动 MCP Server

在一个终端启动 6.2 服务端：

```bash
cd java-ai-course-examples
mvn -pl chapter06/spring-ai-mcp-server-demo spring-boot:run
```

默认 MCP endpoint：

```text
http://localhost:8082/mcp
```

## 启动 MCP Client

另一个终端启动 6.3 客户端：

```bash
cd java-ai-course-examples
export DEEPSEEK_API_KEY=sk-...
mvn -pl chapter06/spring-ai-mcp-client-demo spring-boot:run
```

默认使用 DeepSeek：

```yaml
app.ai.default-provider: deepseek
app.ai.deepseek.base-url: https://api.deepseek.com
app.ai.deepseek.model: deepseek-chat
```

也可以切换到 Ollama 或 ChatGPT：

```bash
APP_AI_DEFAULT_PROVIDER=ollama OLLAMA_MODEL=qwen3.5:9b mvn -pl chapter06/spring-ai-mcp-client-demo spring-boot:run
APP_AI_DEFAULT_PROVIDER=chatgpt CHATGPT_API_KEY=sk-... mvn -pl chapter06/spring-ai-mcp-client-demo spring-boot:run
```

## 调用示例

```bash
curl -X POST http://localhost:8083/support/ask \
  -H 'Content-Type: application/json' \
  -d '{"question":"客户询问 ORD-1001 是否可以退款，请先查订单和退款政策再回答。"}'
```

查看当前发现到的 MCP 工具：

```bash
curl http://localhost:8083/support/tools
```

## 测试

```bash
mvn -pl chapter06/spring-ai-mcp-client-demo test
```
