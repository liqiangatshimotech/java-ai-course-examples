# 4.3 LangChain4j Tool 实战

这个模块演示 LangChain4j Tool Calling 的核心工程结构：

- `CommerceTools` 用 `@Tool` 暴露订单、库存、CRM 三类只读业务能力。
- `CommerceAssistant` 是 AI Service 接口，业务代码通过接口调用 AI 能力。
- `CommerceAssistantFactory` 用 `AiServices.builder(...).tools(...)` 把工具注入 AI Service。
- `ToolAuditLog` 展示工具调用审计应该在后端工具层完成。

运行本地测试：

```bash
mvn -pl chapter04/langchain4j-tool-demo test
```

如果本地已有 Ollama 和 `qwen2.5:7b`，可以运行真实模型演示：

```bash
mvn -pl chapter04/langchain4j-tool-demo exec:java
```
