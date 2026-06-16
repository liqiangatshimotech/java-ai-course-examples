# 9.6 MiMo-Code Infinite Context Demo

This demo models the engineering idea behind MiMo-Code's "infinite context":

- one model request still has a bounded context window;
- checkpoint writers run before the window is full;
- rebuild context is assembled from checkpoint, project memory, notes, memory index, and recent raw messages;
- large repeatable tool results are micro-compacted before the next cycle.

Run it from the repository root:

```bash
mvn -q -f java-ai-course-examples/chapter09/mimo-code-infinite-context-demo/pom.xml compile exec:java
```

The default provider profile follows the course convention:

- `APP_AI_DEFAULT_PROVIDER=deepseek`
- `DEEPSEEK_MODEL`, `DEEPSEEK_BASE_URL`
- `OLLAMA_MODEL`, `OLLAMA_BASE_URL`
- `CHATGPT_MODEL`, `CHATGPT_BASE_URL`
