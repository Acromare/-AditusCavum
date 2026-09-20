# AditusCavum（入墟）

> 让 Java 开发者用少量代码，把自然语言接入真实的 Java 能力。

AditusCavum 是一个面向 Spring Boot 的轻量 Java 大模型调用与 Tool Calling 框架。它借鉴了 LangChain4j 中成熟的工具调用思路，但尽量把模型请求、工具 Schema、参数转换、调用循环和会话消息管理隐藏在框架内部。

你只需要声明一个工具：

```java
@MarkTheRuins
@Component
public class WeatherTools {

    @Need("查询指定城市的天气")
    public Weather queryWeather(String city) {
        return weatherService.query(city);
    }
}
```

然后正常调用：

```java
String answer = assistant.chat("查询上海今天的天气");
```

模型会根据 `@Need` 的中文描述决定是否调用 `queryWeather`。入墟负责把模型返回的参数转换成 Java 参数、执行方法，再把工具结果交回模型生成最终回答。

## 为什么做入墟

我很喜欢 Java 的明确、稳定和工程化，也很喜欢大模型带来的自然语言交互。但在实际项目中，接入模型往往要重复编写请求对象、消息列表、工具 Schema、JSON 解析和多轮调用循环。很多业务代码最后被这些胶水代码包围，真正的业务逻辑反而不明显。

所以我开始做入墟。

这个名字来自拉丁语 `Aditus Cavum`：入口与空间。对我来说，它代表一个入口，也代表一个还在不断挖掘的空间。项目现在还很小，功能也远未完善，但每一个注解、每一次模型调用，都是我把一个想法变成真实工具的过程。希望它最终能让 Java 开发者少写一些重复代码，把注意力放回业务本身。

## 当前能力

- OpenAI 兼容接口调用
- DeepSeek、Qwen 等兼容接口模型接入
- `@MarkTheRuins` 标记工具类或工具接口
- `@Need` 中文工具描述
- 根据 Java 方法签名自动生成 Function Schema
- 字符串、数字、布尔值和 Java Bean 参数转换
- 模型选择工具并执行 Java 方法
- 工具结果回传模型的多轮调用循环
- 最大工具调用轮数限制
- 会话 ID 对应的内存消息记忆
- 系统提示词配置
- Spring Boot 自动装配
- 响应式 `Flux<String>` 调用入口

## 当前版本的边界

入墟目前专注于模型调用和工具调用底座，不自动生成 SQL，也不自动实现数据库 CRUD。数据库、HTTP、订单、退款等业务逻辑仍然由使用者自己实现为 Java 方法。

`stream()` 当前提供响应式调用接口，但内部会先完成工具调用，再返回最终文本；真正的 SSE 增量 token 流正在后续计划中。

## 快速开始

### 1. 添加 Maven 依赖

当前版本通过 JitPack 发布：

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.Acromare</groupId>
    <artifactId>-AditusCavum</artifactId>
    <version>v0.1.0</version>
</dependency>
```

JitPack 构建页面：

<https://jitpack.io/#Acromare/-AditusCavum/v0.1.0>

### 2. 配置模型

推荐使用环境变量保存密钥：

```yaml
aditus-cavum:
  model:
    base-url: https://api.deepseek.com/v1
    api-key: ${DEEPSEEK_API_KEY}
    model: deepseek-chat
  chat:
    max-tool-rounds: 5
    max-memory-messages: 20
    system-prompt: 你是一个可靠的 Java 助手。
```

也可以使用 `application.properties`：

```properties
aditus-cavum.model.base-url=https://api.deepseek.com/v1
aditus-cavum.model.api-key=${DEEPSEEK_API_KEY}
aditus-cavum.model.model=deepseek-chat
aditus-cavum.chat.max-tool-rounds=5
```

### 3. 声明工具

工具是普通 Spring Bean，业务实现完全由应用自己掌握：

```java
package com.example.demo;

import com.heng.aditus.annotation.MarkTheRuins;
import com.heng.aditus.annotation.Need;
import org.springframework.stereotype.Component;

@MarkTheRuins
@Component
public class OrderTools {

    private final OrderService orderService;

    public OrderTools(OrderService orderService) {
        this.orderService = orderService;
    }

    @Need("根据订单号查询订单状态和物流信息")
    public OrderStatus queryOrder(String orderId) {
        return orderService.query(orderId);
    }
}
```

`@MarkTheRuins` 控制框架扫描范围，只有被标记的类型才会进入工具注册表。`@Need` 的值会作为工具描述发送给模型，建议写清楚动作、对象和必要条件。

### 4. 调用助手

```java
import com.heng.aditus.AditusAssistant;
import org.springframework.stereotype.Service;

@Service
public class ChatService {
    private final AditusAssistant assistant;

    public ChatService(AditusAssistant assistant) {
        this.assistant = assistant;
    }

    public String ask(String message) {
        return assistant.chat(message);
    }
}
```

## 会话记忆

无会话 ID 的调用是无状态的：

```java
assistant.chat("我叫张三");
```

需要记忆时，传入稳定的会话 ID：

```java
assistant.chat("user-1001", "我叫张三");
assistant.chat("user-1001", "我叫什么？");
assistant.clearConversation("user-1001");
```

默认实现是进程内内存存储，适合开发和单实例应用。生产环境可以实现 `ChatMemory` 接口，接入 Redis、数据库或其他持久化存储。

## 模型兼容性

只要服务提供 OpenAI 兼容的 `/chat/completions` 接口，就可以通过配置接入。

### DeepSeek

```yaml
aditus-cavum:
  model:
    base-url: https://api.deepseek.com/v1
    api-key: ${DEEPSEEK_API_KEY}
    model: deepseek-chat
```

### Qwen / 通义千问

```yaml
aditus-cavum:
  model:
    base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
    api-key: ${DASHSCOPE_API_KEY}
    model: qwen-plus
```

### Claude

Claude 原生接口使用不同的请求格式，目前不能直接使用默认的 OpenAI 兼容客户端。可以通过 OpenAI 兼容网关接入；原生 Claude 适配器会在后续版本中加入。

## 工作原理

```text
Spring 启动
    -> 扫描 @MarkTheRuins
    -> 找到 @Need 方法
    -> 根据方法签名生成 JSON Schema

用户消息
    -> 发送消息和工具列表给模型
    -> 模型返回普通文本或 tool_call
    -> 入墟反序列化参数并调用 Java 方法
    -> 工具结果回传模型
    -> 模型生成最终回答
```

工具调用不会执行任意 SQL，也不会让模型直接访问 Spring 容器。模型只能选择已经注册的工具，业务方法的权限、事务和数据校验仍然由应用负责。

## 设计原则

### 少量用户概念

用户侧优先保持两个核心注解：

- `@MarkTheRuins`：标记框架识别的入口
- `@Need`：描述一个可被模型调用的能力

模型客户端、消息循环、Schema、记忆和错误处理尽量隐藏在框架内部。

### 中文描述，Java 执行

中文适合描述业务意图，Java 方法适合承载确定的业务逻辑。模型负责理解和选择，应用负责校验和执行。

### 兼容，而不是重新发明

入墟会参考 LangChain4j 等成熟框架的基础能力，但不追求增加新的概念。目标是让常见的模型和 Tool Calling 场景更轻量、更容易进入普通 Spring Boot 项目。

## 开发与验证

项目要求 Java 17 或更高版本：

```bash
./mvnw test
```

Windows：

```powershell
./mvnw.cmd test
```

当前测试覆盖工具 Schema、Java Bean 参数调用、内存会话记忆和 Spring 上下文启动。

## 路线图

- [x] OpenAI 兼容模型调用
- [x] `@Need` 工具注册和调用
- [x] Spring Boot 自动装配
- [x] 基础会话记忆
- [ ] 真正的 SSE 增量流
- [ ] DeepSeek、Qwen 原生参数差异适配
- [ ] Claude 原生适配器
- [ ] 更完整的参数校验和工具异常恢复
- [ ] 可插拔的 Redis / 数据库会话记忆
- [ ] MCP 工具适配
- [ ] 更完善的 starter 与版本发布流程

## 参与项目

这是一个仍在快速生长的个人开源项目。欢迎提交 Issue、建议、示例和 Pull Request。尤其欢迎真实的 Spring Boot 使用场景：一个小而清晰的 Tool Calling 示例，往往比一份长篇理论更能帮助项目变好。

## 许可证

当前仓库尚未确定最终许可证。正式发布前请补充许可证文件，以便其他开发者明确使用、修改和分发边界。
