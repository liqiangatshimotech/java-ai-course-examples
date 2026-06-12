# Chapter 11 Self-built Java Coding Agent

第 11 章对应 `11｜自研 Java Coding Agent`。

配套项目：

- `coding-agent-runtime-demo`：用 Spring Boot + 纯 Java Runtime 实现一个企业化 Coding Agent 管理台，覆盖代码库索引、任务提交、人工审批、工具调用、安全边界、受控写入、测试执行、审计日志和 diff 交付。

默认模型供应商是 DeepSeek，同时保留 Ollama 和 ChatGPT 配置入口。示例为了便于本地稳定运行，默认使用规则模型模拟真实大模型输出；真正接入模型时，可以替换 `CodingModel` 接口实现。

运行管理台：

```bash
mvn -f chapter11/coding-agent-runtime-demo/pom.xml spring-boot:run
```

打开：

```text
http://localhost:18081
```
