# Chapter 09 Claude Code / OpenClaw Examples

This chapter contains Java examples for:

- `claude-code-openclaw-demo`: scans the reconstructed Claude Code source tree, shows how a multi-entry Gateway can normalize requests into one Agent session, demonstrates a file-based Agent memory workflow with `MEMORY.md` plus topic files, and models Hermes Agent's entry-point, tool, skill, memory, and session architecture.

Default model profile follows the rest of the course:

- `APP_AI_DEFAULT_PROVIDER=deepseek`
- `DEEPSEEK_MODEL`, `DEEPSEEK_BASE_URL`
- `OLLAMA_MODEL`, `OLLAMA_BASE_URL`
- `CHATGPT_MODEL`, `CHATGPT_BASE_URL`

Run from the chapter module:

```bash
mvn -q -f chapter09/claude-code-openclaw-demo/pom.xml compile exec:java
```
