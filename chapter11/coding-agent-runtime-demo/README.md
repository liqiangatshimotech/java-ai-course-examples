# coding-agent-runtime-demo

项目名称：`coding-agent-runtime-demo`

这个项目对应 `11｜自研 Java Coding Agent`。我把一个 Coding Agent 拆成了几块能落地的 Java 代码：模型配置、任务会话、工具注册、文件边界、安全审批、代码写入、测试执行和 diff 交付。现在它不再只是命令行 demo，而是一个可以直接打开的 Spring Boot 管理台，能从页面提交任务、审批、运行 Agent、查看步骤、审计日志、源码和 diff。

示例不会真实请求外部模型，默认用 `RuleBasedCodingModel` 模拟 DeepSeek 返回的改动建议，这样在没有 API Key 的机器上也能跑完整链路。

## 业务场景

示例会创建一个临时业务代码仓库：`target/coding-agent-workspace`。里面有一个计费类 `PriceCalculator`，会员折扣上限原来是 20%。提交任务后，Agent 会把折扣上限调整为 30%，并且保持方法签名不变。

完整链路包括：

- 在管理台提交代码任务，任务先进入 `WAITING_APPROVAL`。
- 审批通过后，任务进入 `APPROVED`，才允许运行 Agent。
- 扫描工作区文件，建立 Agent 对代码库的第一层认识。
- 搜索 `MAX_DISCOUNT`，定位真正要改的业务文件。
- 读取文件内容，并把任务、代码片段和模型配置组装成 Prompt。
- 生成代码改动提案，调用高风险 `write_file` 前再次检查审批结果。
- 执行测试工具，验证 30% 折扣边界。
- 输出本次改动的 diff，页面展示工具步骤和审计日志。

## 代码结构

- `config`：DeepSeek、Ollama、ChatGPT 三类模型配置。
- `domain`：任务、会话、步骤、审批和改动提案。
- `model`：模型接口、Prompt 构造和规则模型实现。
- `runtime`：Agent Runtime 主流程、审批服务、审计日志和风险策略。
- `tool`：`list_files`、`search_code`、`read_file`、`write_file`、`run_tests`、`git_diff`。
- `workspace`：工作区边界、快照、diff 和演示仓库生成。
- `service`：任务中心、状态流转和工作区管理。
- `web`：Spring Boot REST API、请求响应对象和异常处理。
- `static`：企业化管理台页面。

## 运行

在 `java-ai-course-examples` 目录下执行：

```bash
mvn -f chapter11/coding-agent-runtime-demo/pom.xml spring-boot:run
```

打开管理台：

```text
http://localhost:18081
```

健康检查：

```bash
curl -s http://localhost:18081/actuator/health
```

如果只想运行原来的命令行链路：

```bash
mvn -q -f chapter11/coding-agent-runtime-demo/pom.xml exec:java
```

## HTTP API

查看运行配置：

```bash
curl -s http://localhost:18081/api/coding-agent/runtime
```

提交任务：

```bash
curl -s -X POST http://localhost:18081/api/coding-agent/tasks \
  -H 'Content-Type: application/json' \
  -d '{
    "repositoryName": "billing-service",
    "branchName": "feature/member-discount-limit",
    "operator": "course-demo",
    "description": "把会员折扣上限从 20% 调整为 30%，同时不要修改 calculateFinalPrice 的方法签名。"
  }'
```

审批任务：

```bash
curl -s -X POST http://localhost:18081/api/coding-agent/tasks/CA-XXXXXXXXXXXXXX-1001/approve \
  -H 'Content-Type: application/json' \
  -d '{
    "approver": "tech-lead",
    "comment": "确认需求明确，允许在隔离工作区执行写入和测试。"
  }'
```

运行任务：

```bash
curl -s -X POST http://localhost:18081/api/coding-agent/tasks/CA-XXXXXXXXXXXXXX-1001/run
```

默认供应商是 DeepSeek：

```bash
export AI_PROVIDER=deepseek
export DEEPSEEK_MODEL=deepseek-chat
export DEEPSEEK_BASE_URL=https://api.deepseek.com
```

切换到 Ollama：

```bash
export AI_PROVIDER=ollama
export OLLAMA_MODEL=qwen3
export OLLAMA_BASE_URL=http://localhost:11434/v1
```

切换到 ChatGPT / OpenAI：

```bash
export AI_PROVIDER=chatgpt
export OPENAI_MODEL=gpt-4o-mini
export OPENAI_BASE_URL=https://api.openai.com/v1
```

## 测试

```bash
mvn -pl chapter11/coding-agent-runtime-demo -am test
```

测试覆盖：

- 模型配置默认值和供应商切换。
- 工作区路径穿越拦截。
- Agent 主流程能完成审批、写入、测试和 diff。

## 打包

```bash
mvn -pl chapter11/coding-agent-runtime-demo -am clean package
```

运行 JAR：

```bash
SERVER_PORT=18081 java -jar chapter11/coding-agent-runtime-demo/target/coding-agent-runtime-demo-0.0.1-SNAPSHOT.jar
```
