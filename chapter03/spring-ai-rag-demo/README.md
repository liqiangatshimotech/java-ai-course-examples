# Spring AI RAG Demo

本模块对应课程 `3.2｜Spring AI RAG 实战`。

它演示一个可运行的企业知识库 RAG 链路：

- 从 `src/main/resources/knowledge-base/*.md` 加载企业文档。
- 解析 front matter 元数据，包括租户、来源和标签。
- 按段落切分 Chunk，并带少量 overlap。
- 用本地 Hashing Embedding 生成可测试的向量。
- 写入内存向量库，并按 tenant 做 Metadata Filter。
- 检索 TopK 相关片段，构造带引用来源的 RAG Prompt。
- 默认使用 Fake 模型稳定运行；设置 `RAG_AI_MODE=spring-ai` 后用 Spring AI `ChatClient` 调用 Ollama。

## 运行测试

```bash
mvn test
```

## 启动服务

默认不依赖 Ollama，使用 Fake 模型：

```bash
mvn spring-boot:run
```

调用：

```bash
curl -sS http://localhost:8084/rag/ask \
  -H 'Content-Type: application/json' \
  -d '{"question":"企业版客户重复扣费怎么办？","tenantId":"acme"}'
```

切换到 Spring AI + Ollama：

```bash
ollama pull qwen2.5:7b
export RAG_AI_MODE=spring-ai
export OLLAMA_BASE_URL=http://localhost:11434
export OLLAMA_MODEL=qwen2.5:7b
mvn spring-boot:run
```

## 代码入口

| 文件 | 讲解重点 |
|---|---|
| `MarkdownKnowledgeDocumentLoader` | 文档加载与 metadata 解析 |
| `SimpleTextSplitter` | Chunk 切分和 overlap |
| `HashingEmbeddingModel` | 本地可测试 Embedding |
| `InMemoryVectorStore` | 内存向量库、相似度检索和 tenant filter |
| `RagRetriever` | 索引构建和检索编排 |
| `RagPromptBuilder` | 把检索结果注入 Prompt |
| `RagService` | RAG 业务流程总编排 |
| `SpringAiGateway` | Spring AI `ChatClient` 生产接入 |
| `FakeGroundedAiGateway` | 无模型环境下稳定演示和测试 |
