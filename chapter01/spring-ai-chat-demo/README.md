# Spring AI Chat Demo

This project implements module 1.2 "First Spring AI Application":

- `POST /chat` synchronous question answering.
- `GET /chat/stream` SSE streaming output.
- Local Ollama provider enabled by default.
- OpenAI-compatible commercial provider enabled when `OPENAI_API_KEY` is set.
- API key management through environment variables.
- Basic validation and exception handling.

## Versions

- Java 21
- Spring Boot 3.5.14
- Spring AI 1.1.7

Spring Boot 4.x is newer, but Spring AI stable documentation currently targets Spring Boot 3.4.x and 3.5.x. This demo uses the current 3.5.x line for Spring AI compatibility.

## Run With Ollama

Install and start Ollama, then pull the configured model:

```bash
ollama pull qwen2.5:7b
```

Start the application:

```bash
mvn spring-boot:run
```

Call the synchronous API:

```bash
curl -sS http://localhost:8080/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"用三句话解释 Spring AI 是什么"}'
```

Call the SSE API:

```bash
curl -N 'http://localhost:8080/chat/stream?message=写一个Java学习计划'
```

## Run With OpenAI

Set the API key and switch the default provider:

```bash
export OPENAI_API_KEY=sk-your-key
export APP_AI_DEFAULT_PROVIDER=openai
mvn spring-boot:run
```

Or choose provider per request:

```bash
curl -sS http://localhost:8080/chat \
  -H 'Content-Type: application/json' \
  -d '{"provider":"openai","message":"解释一下 SSE 流式输出"}'
```

The OpenAI model can be changed with:

```bash
export OPENAI_MODEL=gpt-4o-mini
```

## Configuration

Main configuration lives in `src/main/resources/application.yml`.

Important environment variables:

- `APP_AI_DEFAULT_PROVIDER`: `ollama` or `openai`
- `OLLAMA_BASE_URL`: default `http://localhost:11434`
- `OLLAMA_MODEL`: default `qwen2.5:7b`
- `OPENAI_API_KEY`: required only for OpenAI
- `OPENAI_BASE_URL`: can point to an OpenAI-compatible gateway
- `OPENAI_MODEL`: default `gpt-4o-mini`

## API Shape

`POST /chat`

```json
{
  "provider": "ollama",
  "message": "你好，介绍一下 Spring AI"
}
```

Response:

```json
{
  "provider": "ollama",
  "content": "..."
}
```

`GET /chat/stream?provider=ollama&message=你好`

The endpoint returns `text/event-stream`, with `message` events for chunks and a final `done` event.
