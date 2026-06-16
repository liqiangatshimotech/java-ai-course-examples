# 9.5 MiMo-Code Memory Demo

This module uses Java to model the engineering ideas behind MiMo-Code:

- raw history as an evidence ledger;
- Markdown memory files as a readable long-term cache;
- a simple full-text index as an SQLite FTS5 stand-in;
- checkpoint generation for long-running sessions;
- Dream/Distill style consolidation;
- task progress files and write-scope guards.

Run:

```bash
mvn -q -f chapter09/mimo-code-memory-demo/pom.xml compile exec:java
```

The demo writes temporary artifacts under:

```text
chapter09/mimo-code-memory-demo/target/mimo-code-memory-demo/
```

Default provider profile follows the course convention:

- `APP_AI_DEFAULT_PROVIDER=deepseek`
- `DEEPSEEK_MODEL`, `DEEPSEEK_BASE_URL`, `DEEPSEEK_API_KEY`
- `OLLAMA_MODEL`, `OLLAMA_BASE_URL`
- `CHATGPT_MODEL`, `CHATGPT_BASE_URL`, `CHATGPT_API_KEY`
