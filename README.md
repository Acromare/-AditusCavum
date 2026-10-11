# AditusCavum

> Connect natural language to real Java capabilities with only a few lines of code.

## v0.2.3 — knowledge joins every conversation

Enjoy knowledge-aware replies in both chat and streaming! This patch adds retrieval to stateless chat and all streaming overloads. Retrieved context stays request-local, outside conversation history.

Use `@AditusSylva("classpath:rag/rag.txt")` for `src/main/resources/rag/rag.txt`. External files use `file:C:/path/rag.txt`, never `classpath:C:/...`. Retrieval remains lexical; configure Top-K using `aditus-cavum.chat.knowledge-top-k`.

See [v0.2.3 release notes](docs/releases/v0.2.3.md).

## v0.2.2 is here: less wiring, more enjoying

This is the release where AditusCavum starts feeling like a companion instead of
another integration task. Add one dependency, expose one interface, point one
annotation at your knowledge folder, and enjoy a complete chat surface: memory,
streaming, RAG context, tools, a browser UI, and a live console are ready when
your application starts.

This release is intentionally practical. Every item below is implemented in the
framework, documented here, and kept compatible with the existing v0.2.1 API.

### What changed in v0.2.2

1. **Knowledge-aware chat by declaration.** `@AditusSylva` discovers classpath
   files, splits them into bounded chunks, retrieves relevant pieces for each
   question, and adds them to the model context. No RAG pipeline, loader, or
   prompt assembly is required in application code.
2. **A replaceable retrieval boundary.** The built-in retriever is a small,
   dependency-free lexical index for local text. Replace the `KnowledgeRetriever`
   bean with an embedding or vector-database implementation without changing
   `AditusAssistant`, controllers, or callers.
3. **A default HTTP surface.** Web applications automatically receive chat,
   SSE streaming, and conversation cleanup endpoints. There is no repeated
   `/chat`, `/stream`, or SSE controller boilerplate to write.
4. **A browser experience included in the dependency.** `/aditus/` is a ready
   chat page. `/aditus-console/` is a Sentinel-style runtime view for model,
   tools, knowledge chunks, and retrieval settings. Credentials are never
   returned by the status endpoint.
5. **Tools need less ceremony.** `@CavumOperis("description")` is the concise
   tool declaration. The established `@MarkTheRuins` and `@Need` annotations
   continue to work unchanged.
6. **Memory is safer under real traffic.** Conversations remain isolated by
   ID; requests for the same ID are serialized; a user/assistant turn is stored
   atomically only after success; failed or cancelled model calls do not add
   half a turn to history.
7. **Configuration can be zero-YAML.** `ADITUS_API_KEY`, `ADITUS_BASE_URL`,
   `ADITUS_MODEL`, and related aliases configure the provider from the
   environment. Explicit Spring properties still win.
8. **Compatibility is explicit.** The framework is compiled for JDK 17 and
   avoids newer Java APIs, builds with Maven 3.8+ and Maven 4.x, and keeps its
   Spring API usage suitable for Boot 3.2+ and Boot 4.x consumer applications.
   MyBatis-Plus 3.x and 4.x remain application-owned and are not coupled to this
   framework.

Enjoy the short path from a Spring Boot application to a useful AI feature.

AditusCavum is a lightweight Java chat, streaming, RAG, and Tool Calling framework for Spring Boot. Declare a client interface, inject it, and call the predefined methods. The framework handles model requests, tool schemas, argument conversion, tool loops, conversation memory, knowledge retrieval, and a ready-to-use web console.

Declare your AI client:

```java
import com.heng.aditus.AditusOperations;
import com.heng.aditus.annotation.AditusClient;

@AditusClient
public interface ExamAI extends AditusOperations {
}
```

Inject it into your application:

```java
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class ExamSupport {
    private final ExamAI examAI;

    public ExamSupport(ExamAI examAI) {
        this.examAI = examAI;
    }

    public Flux<String> answer(String conversationId, String question) {
        return examAI.stream(conversationId, question);
    }
}
```

The client implementation is created automatically: no implementation class or `super` call is required. Place the interface under your Spring Boot application package and configure your model using the Quick Start below. `chat(...)` returns a complete answer; `stream(...)` emits text incrementally. The surrounding service above is optional: a controller can inject `ExamAI` directly.

**v0.2.2 highlights:** automatic knowledge/RAG loading with `@AditusSylva`, built-in chat and SSE endpoints, bundled browser pages and a Sentinel-style runtime console, zero-YAML environment setup, `@CavumOperis` tools, isolated and serialized conversation memory, and the injectable `@AditusClient` API from v0.2.1. Existing `extends AditusService`, `@Need`, `@MarkTheRuins`, and direct `AditusAssistant` injection remain supported. See the [v0.2.2 release notes](docs/releases/v0.2.2.md) for the complete change list.

### 3. Declare knowledge with one annotation

Put knowledge files under `src/main/resources/knowledge/` and annotate the
application class. The framework loads, chunks, indexes, and retrieves relevant
pieces automatically for every chat request:

```java
@SpringBootApplication
@AditusSylva("classpath:knowledge")
public class Application {
}
```

The default retriever is dependency-free and works well for small text
knowledge bases. Advanced applications can replace the `KnowledgeRetriever`
bean with an embedding or vector-database implementation. Tune the number of
retrieved chunks with `aditus-cavum.chat.knowledge-top-k`.

### 4. Open the built-in surfaces

With the default web settings, start the application and open:

```text
http://localhost:8080/aditus/
http://localhost:8080/aditus-console/
```

The corresponding API is:

```text
POST   /aditus/api/chat
GET    /aditus/api/stream?conversationId=user-1&message=Hello
DELETE /aditus/api/conversation/{conversationId}
```

Change the prefix with `aditus-cavum.web.path`. Set
`aditus-cavum.web.enabled=false` to disable the default API, or
`aditus-cavum.web.console-enabled=false` to disable only the console.

### 5. Declare a tool

Optionally define a model-callable tool:

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

Then call your injected client:

```java
String answer = examAI.chat("What is the weather like in Shanghai today?");
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
- Injectable `@AditusClient` interfaces backed by automatic proxies
- Incremental SSE text streaming through `Flux<String>`, including tool-call continuation
- `@AditusSylva` automatic classpath knowledge loading, chunking, and retrieval-augmented context
- Replaceable `KnowledgeRetriever` extension point for embeddings and vector databases
- Built-in `/aditus/api/chat`, `/aditus/api/stream`, and conversation cleanup endpoints
- Bundled `/aditus/` chat page and `/aditus-console/` runtime console
- `@CavumOperis` one-annotation tool declaration, with legacy tool annotations preserved
- Conversation-level serialization so chat, stream, and clear operations cannot corrupt one another
- Environment-only model configuration with no mandatory YAML block

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
    <version>v0.2.3</version>
</dependency>
```

JitPack build page: <https://jitpack.io/#Acromare/-AditusCavum/v0.2.3>

### 2. Configure a model

The shortest setup uses environment variables only. With the defaults, you can
start the application without an AditusCavum YAML block:

```text
ADITUS_API_KEY=your-provider-key
ADITUS_BASE_URL=https://api.deepseek.com/v1
ADITUS_MODEL=deepseek-chat
```

The framework also accepts the legacy names `ADITUS_MODEL_API_KEY`,
`ADITUS_MODEL_BASE_URL`, and `ADITUS_MODEL_NAME`. Explicit
`aditus-cavum.model.*` and `aditus-cavum.chat.*` properties take precedence.

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

### 6. Call the assistant

Declare an interface when you only need the predefined operations:

```java
import com.heng.aditus.AditusOperations;
import com.heng.aditus.annotation.AditusClient;

@AditusClient
public interface ExamAI extends AditusOperations {
}
```

Put it under your Spring Boot application package, then inject `ExamAI` and call `chat`, `stream`, or `clearConversation`. The framework creates its implementation; no `@Service`, implementation class, or `super` call is needed. Method signatures are predefined, while argument values come from each invocation. Unknown abstract methods are rejected at startup; explicitly implemented Java `default` methods are supported. See the [interface-client guide](docs/clients.md) for a complete controller example, scan configuration, implementation details, and limitations.

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

The public annotations have separate responsibilities:

- `@MarkTheRuins`: mark the entry points that the framework may discover
- `@Need`: describe a capability that the model may call
- `@AditusClient`: register an injectable interface for calling the assistant; this does not register a model-callable tool

Model clients, message loops, schemas, memory, and error handling stay inside the framework whenever possible.

### Describe in natural language, execute in Java

Natural language is useful for describing business intent. Java methods are useful for deterministic business behavior. The model understands and selects; the application validates and executes.

### Learn from the ecosystem without adding unnecessary concepts

AditusCavum takes inspiration from mature frameworks such as LangChain4j. The goal is not to invent more abstractions, but to make common model and Tool Calling scenarios easier to enter from an ordinary Spring Boot project.

## Build and Test

Compatibility baseline:

- JDK 17 or newer. The library is compiled for Java 17 and avoids newer Java APIs.
- Maven 3.8+ and Maven 4.x. The Maven Wrapper currently pins Maven 3.9 for reproducible builds; a system Maven 4 can build the same POM.
- Spring Boot 3.2+ and Spring Boot 4.x when the consuming application supplies its own Boot dependency management. AditusCavum uses Spring APIs shared by these generations and does not require Boot 4-only classes.
- MyBatis-Plus 3.x and 4.x are supported as application-side dependencies. AditusCavum does not depend on MyBatis-Plus, so its version is selected by the host application and remains isolated from the chat/RAG modules.

The supported matrix is intentionally based on APIs rather than a fixed application stack: keep the framework dependency unversioned by importing it after your application's Spring Boot BOM/parent, and let the application choose its MyBatis-Plus line.

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
