# Java AI Course Examples

这个目录把课程第一章到第六章中已落地的示例代码整理成一个 Maven 多模块项目。根 `pom.xml` 只聚合章节，每个章节目录下再用自己的 `pom.xml` 聚合该章小节代码。

## 模块结构

| 章节 | 模块 | 对应课次 | 内容 |
|---|---|---|---|
| 第 1 章 | `chapter01/spring-ai-chat-demo` | 1.2 | Spring AI 聊天后端，同步和 SSE 流式输出 |
| 第 1 章 | `chapter01/langchain4j-chat-demo` | 1.3 | LangChain4j ChatModel、StreamingChatModel、AI Services |
| 第 2 章 | `chapter02/prompt-contract-demo` | 2.1 | Prompt 契约模板和渲染示例 |
| 第 2 章 | `chapter02/ticket-classification-structured-output-demo` | 2.2 | 不绑定框架的结构化输出、校验、重试和兜底 |
| 第 2 章 | `chapter02/spring-ai-ticket-classifier-demo` | 2.3 | Spring AI `ChatClient.entity(...)` 和 `BeanOutputConverter` |
| 第 2 章 | `chapter02/langchain4j-ticket-classifier-demo` | 2.4 | LangChain4j AI Services 结构化输出 |
| 第 2 章 | `chapter02/support-ticket-copilot-demo` | 2.6 | 综合实战：智能客服工单助手，结构化分析和 SSE 回复草稿 |
| 第 3 章 | `chapter03/spring-ai-rag-demo` | 3.2 | Spring AI RAG 企业知识库实战，文档加载、切分、检索、Prompt 注入和引用来源 |
| 第 3 章 | `chapter03/langchain4j-rag-demo` | 3.4 | LangChain4j RAG 小项目实战，组件化 RAG、租户过滤和 AI Services 封装 |
| 第 4 章 | `chapter04/spring-ai-tool-demo` | 4.2 | Spring AI Tool Calling，`@Tool`、`ToolCallbackProvider`、参数校验和审计日志 |
| 第 4 章 | `chapter04/langchain4j-tool-demo` | 4.3 | LangChain4j Tool Calling，`@Tool`、AI Services 工具注入和多工具规格验证 |
| 第 5 章 | `chapter05/memory-concepts-mini-demo` | 5.1 | History、Memory、业务状态的边界拆分 |
| 第 5 章 | `chapter05/spring-ai-chat-memory-demo` | 5.2 | Spring AI Advisor 风格的短期会话记忆和 conversationId 隔离 |
| 第 5 章 | `chapter05/spring-ai-persistent-memory-demo` | 5.3 | JDBC `ChatMemoryRepository`、会话记忆持久化、窗口裁剪和长期偏好记忆 |
| 第 5 章 | `chapter05/langchain4j-chat-memory-demo` | 5.4 | LangChain4j `@MemoryId`、`ChatMemoryProvider` 和多用户记忆隔离 |
| 第 5 章 | `chapter05/langchain4j-persistent-memory-demo` | 5.5 | `ChatMemoryStore` 持久化和 RAG 内容写入边界 |
| 第 5 章 | `chapter05/memory-service-final-demo` | 5.6 | 客户支持记忆服务的权限、过期、删除和污染过滤 |
| 第 6 章 | `chapter06/mcp-protocol-mini-demo` | 6.1 | MCP JSON-RPC 最小协议模拟，工具发现、工具调用、资源读取和安全边界 |
| 第 6 章 | `chapter06/spring-ai-mcp-server-demo` | 6.2 | Spring AI MCP Server，注解式 Tool、Resource、Prompt 和 Streamable HTTP |
| 第 6 章 | `chapter06/existing-service-to-mcp-demo` | 6.2.1 | 把已有 Spring Service 通过 Adapter 层改造成 MCP Server |

## Maven 层级

```text
java-ai-course-examples/pom.xml
├── chapter01/pom.xml
│   ├── spring-ai-chat-demo
│   └── langchain4j-chat-demo
├── chapter02/pom.xml
│   ├── prompt-contract-demo
│   ├── ticket-classification-structured-output-demo
│   ├── spring-ai-ticket-classifier-demo
│   ├── langchain4j-ticket-classifier-demo
│   └── support-ticket-copilot-demo
├── chapter03/pom.xml
│   ├── spring-ai-rag-demo
│   └── langchain4j-rag-demo
├── chapter04/pom.xml
│   ├── spring-ai-tool-demo
│   └── langchain4j-tool-demo
├── chapter05/pom.xml
│   ├── memory-concepts-mini-demo
│   ├── spring-ai-chat-memory-demo
│   ├── spring-ai-persistent-memory-demo
│   ├── langchain4j-chat-memory-demo
│   ├── langchain4j-persistent-memory-demo
│   └── memory-service-final-demo
└── chapter06/pom.xml
    ├── mcp-protocol-mini-demo
    ├── spring-ai-mcp-server-demo
    └── existing-service-to-mcp-demo
```

## 构建

```bash
mvn test
```

只构建某一章：

```bash
mvn -f chapter02/pom.xml test
```

只构建单个小节模块：

```bash
mvn -pl chapter02/ticket-classification-structured-output-demo test
```

## 运行示例

运行 2.1 Prompt 契约示例：

```bash
mvn -pl chapter02/prompt-contract-demo exec:java
```

运行 2.2 结构化输出闭环示例：

```bash
mvn -pl chapter02/ticket-classification-structured-output-demo exec:java
```

运行 2.6 智能客服工单助手：

```bash
mvn -pl chapter02/support-ticket-copilot-demo spring-boot:run
```

运行 3.2 Spring AI RAG 企业知识库实战：

```bash
mvn -pl chapter03/spring-ai-rag-demo spring-boot:run
```

运行 3.4 LangChain4j RAG 小项目实战：

```bash
mvn -pl chapter03/langchain4j-rag-demo exec:java
```

运行 4.2 Spring AI Tool Calling 本地工具演示：

```bash
mvn -pl chapter04/spring-ai-tool-demo exec:java
```

运行 4.3 LangChain4j Tool Calling 真实模型演示：

```bash
mvn -pl chapter04/langchain4j-tool-demo exec:java
```

运行 5.1 记忆概念拆分示例：

```bash
mvn -pl chapter05/memory-concepts-mini-demo exec:java
```

运行 5.2 Spring AI 会话记忆示例：

```bash
mvn -pl chapter05/spring-ai-chat-memory-demo exec:java
```

运行 5.3 Spring AI 持久化记忆示例：

```bash
mvn -pl chapter05/spring-ai-persistent-memory-demo exec:java
```

运行 5.4 LangChain4j 会话记忆示例：

```bash
mvn -pl chapter05/langchain4j-chat-memory-demo exec:java
```

运行 5.5 LangChain4j 持久化记忆示例：

```bash
mvn -pl chapter05/langchain4j-persistent-memory-demo exec:java
```

运行 5.6 记忆服务边界示例：

```bash
mvn -pl chapter05/memory-service-final-demo exec:java
```

运行 6.1 MCP JSON-RPC 最小协议演示：

```bash
mvn -pl chapter06/mcp-protocol-mini-demo exec:java
```

运行 6.2 Spring AI MCP Server：

```bash
mvn -pl chapter06/spring-ai-mcp-server-demo spring-boot:run
```

运行 6.2.1 把现有服务改造成 MCP Server：

```bash
mvn -pl chapter06/existing-service-to-mcp-demo spring-boot:run
```

运行 Spring AI 或 LangChain4j 示例前，先准备本地 Ollama：

```bash
ollama pull qwen2.5:7b
```

然后进入对应模块运行 `mvn spring-boot:run` 或 `mvn exec:java`。
