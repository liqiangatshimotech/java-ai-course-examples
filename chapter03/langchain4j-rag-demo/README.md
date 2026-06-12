# LangChain4j RAG Demo

本模块对应课程 `3.4｜LangChain4j RAG 小项目实战`。

它演示一个可在本地稳定运行的企业客服政策 RAG 小项目：

- 从 `src/main/resources/knowledge-base/*.md` 加载 Markdown 知识库。
- 解析 front matter 元数据，包括 `tenantId`、`title`、`source` 和 `tags`。
- 使用 LangChain4j `DocumentSplitter` 切分 `TextSegment`。
- 使用本地 `HashingEmbeddingModel` 生成可测试向量。
- 写入 LangChain4j `InMemoryEmbeddingStore`。
- 用 `EmbeddingStoreContentRetriever` 按租户过滤并召回 TopK。
- 用 `DefaultRetrievalAugmentor` 把检索内容注入上下文。
- 用 LangChain4j `AiServices` 把 RAG 能力封装成 Java 接口。
- 默认使用确定性 `GroundedChatModel`，不依赖 API Key 或 Ollama。

## 运行测试

```bash
mvn test
```

## 运行 Demo

```bash
mvn exec:java
```

指定租户和问题：

```bash
mvn exec:java -Dexec.args="acme 企业版客户重复扣费怎么办？"
```

## 代码入口

| 文件 | 讲解重点 |
|---|---|
| `PolicyAssistantDemo` | 命令行入口 |
| `PolicyRagService` | RAG 业务编排和 AI Service 创建 |
| `KnowledgeBaseLoader` | Document Loader + Parser + metadata 解析 |
| `HashingEmbeddingModel` | 本地可测试 EmbeddingModel |
| `GroundedChatModel` | 无外部模型环境下的确定性 ChatModel |
| `PolicyAssistant` | LangChain4j AI Service 接口 |
| `knowledge-base/*.md` | 多租户知识库样例 |

## 课堂提示

这个项目不是为了追求模型效果，而是为了让学员看清 LangChain4j RAG 的组件边界：

1. 离线索引链路：`Document Loader -> Parser -> Splitter -> EmbeddingModel -> EmbeddingStore`
2. 在线问答链路：`ContentRetriever -> RetrievalAugmentor -> AiServices -> ChatModel`
3. 企业工程点：metadata filter、source 引用、可替换模型、可替换向量库
