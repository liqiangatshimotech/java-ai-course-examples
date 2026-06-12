# LangChain4j Ticket Classifier Demo

本模块对应课程 `2.4｜LangChain4j 结构化输出实战`。

它演示：

- 用 AI Services 把工单分类能力定义成 Java 接口。
- 方法直接返回 `TicketClassification`。
- 可选返回 `Result<TicketClassification>` 以获取 token 和结束原因等元数据。
- 业务层拿到结构化结果后继续做 Bean Validation。

## 运行

```bash
ollama pull qwen2.5:7b
mvn exec:java
```

可通过环境变量调整模型：

```bash
export OLLAMA_BASE_URL=http://localhost:11434
export OLLAMA_MODEL=qwen2.5:7b
```
