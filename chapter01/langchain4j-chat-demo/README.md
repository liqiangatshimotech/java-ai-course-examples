# LangChain4j Chat Demo

本工程对应课程 `1.3｜第一个 LangChain4j 应用`，演示三种用法：

- `ChatModel`：低层同步模型调用。
- `StreamingChatModel`：低层 SSE 流式输出。
- `AI Services`：把 AI 能力封装成 Java 接口。

## 版本

- Java 21
- Spring Boot 4.1.0-RC1
- LangChain4j 1.15.0

说明：Maven Central 当前把 Spring Boot 最新版本标记为 `4.1.0-RC1`。如果课堂环境只使用 GA 版本，可把 `pom.xml` 里的 Spring Boot 版本改成 `4.0.6`。

## 接口

| 接口 | 说明 |
|---|---|
| `POST /chat` | 使用 `ChatModel` 同步问答 |
| `GET /chat/stream` | 使用 `StreamingChatModel` SSE 流式问答 |
| `POST /assistant` | 使用 AI Services 接口同步问答 |
| `GET /assistant/stream` | 使用 AI Services 接口流式问答 |

## 运行 Ollama

```bash
ollama pull qwen2.5:7b
mvn spring-boot:run
```

同步调用：

```bash
curl -sS http://localhost:8081/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"用三句话解释 LangChain4j 的 AI Services"}'
```

流式调用：

```bash
curl -N 'http://localhost:8081/chat/stream?message=解释一下StreamingChatModel'
```

AI Services 调用：

```bash
curl -sS http://localhost:8081/assistant \
  -H 'Content-Type: application/json' \
  -d '{"message":"给一个 Assistant 接口封装模型调用的例子"}'
```

## 运行 OpenAI-compatible Provider

```bash
export OPENAI_API_KEY=sk-your-key
export APP_AI_DEFAULT_PROVIDER=openai
mvn spring-boot:run
```

也可以逐次指定 provider：

```bash
curl -sS http://localhost:8081/assistant \
  -H 'Content-Type: application/json' \
  -d '{"provider":"openai","message":"对比 Spring AI 和 LangChain4j 的设计差异"}'
```

可配置项：

- `APP_AI_DEFAULT_PROVIDER`: `ollama` 或 `openai`
- `OLLAMA_BASE_URL`: 默认 `http://localhost:11434`
- `OLLAMA_MODEL`: 默认 `qwen2.5:7b`
- `OPENAI_API_KEY`: 使用 OpenAI-compatible provider 时必填
- `OPENAI_BASE_URL`: 默认 `https://api.openai.com/v1`
- `OPENAI_MODEL`: 默认 `gpt-4.1-mini`

## 代码入口

- `ChatController`: HTTP API 和 SSE 封装。
- `ChatService`: 对比低层模型调用和 AI Services 调用。
- `Assistant`: 章节中的 `interface Assistant { String chat(String message); }` 形式。
- `ChatClientRegistry`: 创建 Ollama / OpenAI-compatible 的同步和流式模型。
