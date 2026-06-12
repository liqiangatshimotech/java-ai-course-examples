# Spring AI Ticket Classifier Demo

本模块对应课程 `2.3｜Spring AI 结构化输出实战`。

它演示同一个工单分类场景的三种接口：

| 接口 | 说明 |
|---|---|
| `POST /tickets/classify/text` | 返回模型原始文本 |
| `POST /tickets/classify/structured` | 使用 `ChatClient.entity(TicketClassification.class)` |
| `POST /tickets/classify/converter` | 使用 `BeanOutputConverter` 显式格式说明 |

## 运行

```bash
ollama pull qwen2.5:7b
mvn spring-boot:run
```

调用：

```bash
curl -sS http://localhost:8082/tickets/classify/structured \
  -H 'Content-Type: application/json' \
  -d '{"content":"昨天扣费两次，发票也开不出来，客户经理一直没回复。"}'
```
