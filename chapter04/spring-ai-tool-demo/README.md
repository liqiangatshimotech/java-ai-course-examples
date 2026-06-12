# 4.2 Spring AI Tool 实战

这个模块演示 Spring AI Tool Calling 的核心工程结构：

- `CommerceTools` 用 `@Tool` 暴露订单、库存、CRM 三类只读业务能力。
- `ToolConfiguration` 用 `MethodToolCallbackProvider` 把工具方法注册为 Spring AI 可调用的 `ToolCallback`。
- `ToolCallingCopilot` 在 `ChatClient` 调用中通过 `toolCallbacks(...)` 挂载工具。
- `ToolAuditLog` 展示工具调用审计不应该交给模型，而应该由后端工具层负责。

运行本地业务工具演示：

```bash
mvn -pl chapter04/spring-ai-tool-demo exec:java
```

运行测试：

```bash
mvn -pl chapter04/spring-ai-tool-demo test
```
