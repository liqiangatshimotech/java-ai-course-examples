# Chapter 05 AI Memory Examples

第五章示例围绕 AI 应用里的记忆系统展开。代码默认使用 `deepseek` 作为 provider 标识，同时保留 `ollama` 和 `chatgpt` 的配置入口。

```bash
APP_AI_DEFAULT_PROVIDER=deepseek mvn -f chapter05/pom.xml -pl memory-concepts-mini-demo exec:java
APP_AI_DEFAULT_PROVIDER=ollama mvn -f chapter05/pom.xml -pl spring-ai-chat-memory-demo exec:java
APP_AI_DEFAULT_PROVIDER=chatgpt mvn -f chapter05/pom.xml -pl langchain4j-chat-memory-demo exec:java
```

## Modules

| 课次 | 模块 | 核心内容 |
|---|---|---|
| 5.1 | `memory-concepts-mini-demo` | History、Memory、业务状态分开建模 |
| 5.2 | `spring-ai-chat-memory-demo` | Advisor 风格的会话记忆和 conversationId 隔离 |
| 5.3 | `spring-ai-persistent-memory-demo` | 窗口裁剪、文件持久化仓库、长期偏好提炼 |
| 5.4 | `langchain4j-chat-memory-demo` | `@MemoryId`、`ChatMemoryProvider`、多用户隔离 |
| 5.5 | `langchain4j-persistent-memory-demo` | `ChatMemoryStore` 和 RAG 内容写入边界 |
| 5.6 | `memory-service-final-demo` | 记忆作用域、权限、TTL、清空、污染过滤 |
