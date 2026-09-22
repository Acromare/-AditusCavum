# AditusCavum

> Connect natural language to real Java capabilities with only a few lines of code.

AditusCavum is a lightweight Java model-calling and Tool Calling framework for Spring Boot. It follows the proven ideas behind LangChain4j while keeping model requests, tool schemas, argument conversion, tool loops, and conversation messages inside the framework.

Define a tool:

```java
@MarkTheRuins
@Component
public class WeatherTools {

    @Need("Query the weather for a city")
    public Weather queryWeather(String city) {
        return weatherService.query(city);
    }
}
```

Then call the assistant:

```java
String answer = assistant.chat("What is the weather like in Shanghai today?");
```

The model decides whether to call `queryWeather` from the `@Need` description. AditusCavum converts the model arguments into Java values, invokes the method, sends the result back to the model, and returns the final answer.

## Why AditusCavum?

I like Java for its clarity, stability, and engineering discipline. I also like what large language models make possible through natural language. In real applications, however, connecting a model often means repeating the same request objects, message lists, tool schemas, JSON conversion, and multi-round call loops. Business code gets surrounded by glue code, and the actual intent becomes difficult to see.

That is why I started AditusCavum.

The name comes from the Latin-inspired phrase “Aditus Cavum”: an entrance and a space to explore. To me, it represents an entrance into a new way of building Java applications and a space that is still being discovered. The project is small and far from complete, but every annotation and every model call is a step from an idea toward a real tool. I hope it helps Java developers spend less time on repetitive integration code and more time on the work that matters.

## Features

- OpenAI-compatible chat completion API
- DeepSeek, Qwen, and other compatible providers
- `@MarkTheRuins` tool discovery marker
- `@Need` natural-language tool descriptions
- Function schema generation from Java method signatures
- String, number, boolean, and Java Bean argument conversion
- Model-selected Java tool execution
- Multi-round tool-call loop with result handoff
- Maximum tool-call round limit
- In-memory conversation memory keyed by conversation ID
- Configurable system prompt
- Spring Boot auto-configuration
- Optional `AditusService` base class with inherited chat, streaming, and conversation cleanup
- Incremental SSE text streaming through `Flux<String>`, including tool-call continuation

## Current Scope

AditusCavum currently focuses on the model and tool-calling foundation. It does not generate SQL or implement database CRUD automatically. Database, HTTP, order, payment, and other business logic remain ordinary Java methods owned by the application.

`stream()` emits text deltas as the provider sends them. Tool-call argument fragments are assembled internally before execution; tool results are sent back to the model and text streaming continues. A delta is not necessarily one character or one token.

## Quick Start

### 1. Add the Maven dependency

The current release is available through JitPack:

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
    <version>v0.2.0</version>
</dependency>
```

JitPack build page: <https://jitpack.io/#Acromare/-AditusCavum/v0.2.0>

### 2. Configure a model

Keep API keys in environment variables:

```yaml
aditus-cavum:
  model:
    base-url: https://api.deepseek.com/v1
    api-key: ${DEEPSEEK_API_KEY}
    model: deepseek-chat
  chat:
    max-tool-rounds: 5
    max-memory-messages: 20
    system-prompt: You are a reliable Java assistant.
```

The equivalent properties are:

```properties
aditus-cavum.model.base-url=https://api.deepseek.com/v1
aditus-cavum.model.api-key=${DEEPSEEK_API_KEY}
aditus-cavum.model.model=deepseek-chat
aditus-cavum.chat.max-tool-rounds=5
```

### Upgrading from v0.1.7

Version v0.1.8 removes the library's `application.properties`, which could override a consumer's YAML and replace a configured API key with an empty value. Defaults remain in `AditusProperties`; application configuration belongs to the consumer. No Java API changes are required.

If you used the old `ADITUS_MODEL_*`, `ADITUS_MAX_TOOL_ROUNDS`, `ADITUS_MAX_MEMORY_MESSAGES`, or `ADITUS_SYSTEM_PROMPT` environment aliases, map them explicitly in your application's configuration. For example:

```yaml
aditus-cavum:
  model:
    base-url: ${ADITUS_MODEL_BASE_URL:https://api.openai.com/v1}
    api-key: ${ADITUS_MODEL_API_KEY}
    model: ${ADITUS_MODEL_NAME:gpt-4o-mini}
  chat:
    max-tool-rounds: ${ADITUS_MAX_TOOL_ROUNDS:5}
    max-memory-messages: ${ADITUS_MAX_MEMORY_MESSAGES:20}
    system-prompt: ${ADITUS_SYSTEM_PROMPT:}
```

After updating the dependency to `v0.1.8`, rebuild and restart your application. When building the framework from source, use `./mvnw clean verify` to remove stale resources from previous builds.

### 3. Declare a tool

Tools are ordinary Spring beans. The application owns the implementation:

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

    @Need("Find the status and delivery information for an order")
    public OrderStatus queryOrder(String orderId) {
        return orderService.query(orderId);
    }
}
```

`@MarkTheRuins` controls which types are scanned. Only marked types are registered as tools. The `@Need` value becomes the description sent to the model, so describe the action, object, and important conditions clearly.

### 4. Call the assistant

For an application-owned service, extend `AditusService`. Spring injects the assistant into the base class; the subclass needs no fields, constructor, Lombok annotation, or forwarding methods:

```java
import com.heng.aditus.AditusService;
import org.springframework.stereotype.Service;

@Service
public class AIService extends AditusService {
}
```

Inject `AIService` into your controller and call `chat(message)`, `chat(conversationId, message)`, `stream(message)`, `stream(conversationId, message)`, or `clearConversation(conversationId)` directly. Register the subclass as a Spring bean; do not instantiate it with `new`.

Override only the behavior your application needs. For example, add a business validation rule and retain framework behavior through `super`:

```java
@Override
public String chat(String conversationId, String message) {
    if (message == null || message.isBlank()) {
        return "Please enter a question.";
    }
    return super.chat(conversationId, message);
}
```

The one-argument methods delegate to the corresponding two-argument method with a null conversation ID, so a two-argument override also applies to stateless calls. Chat and streaming are separate operations: override both when a rule must apply to both. Streaming customization should preserve lazy execution using Reactor operators or `Flux.defer`.

This base class is optional. Direct assistant injection remains supported, including when your service already extends another class:

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

## Streaming

```java
Flux<String> deltas = assistant.stream("Explain virtualization.");
Flux<String> conversation = assistant.stream("user-1001", "What did I ask earlier?");
```

Both methods apply the configured system prompt. Calling a method does not start a request: each subscription starts a new request. Do not subscribe in your controller; return the publisher to Spring. Do not wrap `chat()` in `Flux.just(...)`, as that waits for the entire answer.

A minimal Spring MVC endpoint is:

```java
@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> stream(@RequestParam String conversationId,
                           @RequestParam String message) {
    return assistant.stream(conversationId, message);
}
```

The controller owns the HTTP API; the framework provides the Java API. For POST requests or explicit `token`, `done`, and `error` events, see the [streaming integration guide](docs/streaming.md). Read SSE incrementally in the browser rather than calling `response.json()`. Reverse proxies must allow streaming without buffering.

The default model uses `stream: true` and reads UTF-8 SSE events on Reactor's bounded-elastic scheduler. Cancellation closes the upstream response body. An idle event stream times out after two minutes. An incomplete response, malformed event, HTTP error, or provider error terminates the publisher with an error; emitted text may therefore be partial. Model calls and tools are never automatically retried.

For a conversation stream, memory is updated only after successful completion. Errors and cancellation do not save the current user/assistant pair. Tool side effects that already occurred cannot be rolled back by cancelling the stream. Serialize requests for the same conversation ID; concurrent turns and clearing memory during an active stream are not coordinated.

Custom `ChatModel` implementations can override `stream(List<ChatMessage>)` to provide incremental output. Its default implementation lazily runs `chat(messages)` on bounded elastic and emits one complete answer. Existing `stream(String)` calls remain available; the assistant uses the message-list overload so the system prompt and history are preserved.

## Conversation Memory

Calls without a conversation ID are stateless:

```java
assistant.chat("My name is Alex");
```

Pass a stable conversation ID when context should be retained:

```java
assistant.chat("user-1001", "My name is Alex");
assistant.chat("user-1001", "What is my name?");
assistant.clearConversation("user-1001");
```

The default implementation stores messages in process memory. It is useful for development and single-instance applications. Production applications can provide their own `ChatMemory` implementation backed by Redis, a database, or another store.

## Model Compatibility

Any provider exposing an OpenAI-compatible `/chat/completions` endpoint can be configured directly.

### DeepSeek

```yaml
aditus-cavum:
  model:
    base-url: https://api.deepseek.com/v1
    api-key: ${DEEPSEEK_API_KEY}
    model: deepseek-chat
```

### Qwen

```yaml
aditus-cavum:
  model:
    base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
    api-key: ${DASHSCOPE_API_KEY}
    model: qwen-plus
```

### Claude

The native Claude API uses a different request format and cannot yet use the default OpenAI-compatible client directly. Claude can be connected through an OpenAI-compatible gateway. A native Claude adapter is planned.

## How It Works

```text
Spring starts
    -> discover @MarkTheRuins
    -> find @Need methods
    -> build JSON schemas from method signatures

User message
    -> send messages and tools to the model
    -> receive normal text or a tool_call
    -> deserialize arguments and invoke the Java method
    -> send the tool result back to the model
    -> return the final answer
```

The framework never executes arbitrary SQL and never gives the model direct access to the Spring container. The model can only choose registered tools. Authorization, transactions, validation, and side effects remain under application control.

## Design Principles

### Keep the user-facing API small

The user-facing API is intentionally built around two annotations:

- `@MarkTheRuins`: mark the entry points that the framework may discover
- `@Need`: describe a capability that the model may call

Model clients, message loops, schemas, memory, and error handling stay inside the framework whenever possible.

### Describe in natural language, execute in Java

Natural language is useful for describing business intent. Java methods are useful for deterministic business behavior. The model understands and selects; the application validates and executes.

### Learn from the ecosystem without adding unnecessary concepts

AditusCavum takes inspiration from mature frameworks such as LangChain4j. The goal is not to invent more abstractions, but to make common model and Tool Calling scenarios easier to enter from an ordinary Spring Boot project.

## Build and Test

Java 17 or newer is required:

```bash
./mvnw test
```

Windows:

```powershell
./mvnw.cmd test
```

The current tests cover tool schema generation, Java Bean argument invocation, in-memory conversation memory, Spring context startup, consumer YAML binding, incremental SSE delivery, fragmented tool calls, cancellation, errors, tool limits, streaming conversation isolation, inherited service injection, and method overrides. Streaming tests use a local HTTP server and require no provider API key.

## Roadmap

- [x] OpenAI-compatible model calls
- [x] `@Need` tool registration and execution
- [x] Spring Boot auto-configuration
- [x] Basic conversation memory
- [x] True SSE incremental streaming
- [ ] Provider-specific DeepSeek and Qwen options
- [ ] Native Claude adapter
- [ ] More complete argument validation and tool error recovery
- [ ] Pluggable Redis and database conversation memory
- [ ] MCP tool adapter
- [ ] A more complete starter and release workflow

## Contributing

This is a small open-source project that is still growing quickly. Issues, suggestions, examples, and pull requests are welcome. A small, focused Spring Boot Tool Calling example is often more useful than a long theoretical discussion, so practical feedback is especially valuable.

## License

The final project license has not been selected yet. A license file will be added before the project is considered ready for broad redistribution.
