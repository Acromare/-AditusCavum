# Interface clients

Starting with v0.2.1, declare a public interface extending `AditusOperations` and annotate it with `@AditusClient`. The framework creates its implementation at application startup.

```java
package com.example.app.ai;

import com.heng.aditus.AditusOperations;
import com.heng.aditus.annotation.AditusClient;

@AditusClient
public interface ExamAI extends AditusOperations {
}
```

No implementation class, `@Service`, `super` call, or forwarding methods are required. Method names, argument types, and return types are predefined:

```java
String chat(String message);
String chat(String conversationId, String message);
Flux<String> stream(String message);
Flux<String> stream(String conversationId, String message);
void clearConversation(String conversationId);
```

`Flux` is `reactor.core.publisher.Flux`, provided by the framework's existing Reactor dependency. Argument values are supplied when invoking a method, not in the interface declaration.

## Inject and call

```java
package com.example.app.web;

import com.example.app.ai.ExamAI;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/ai")
public class ChatController {
    private final ExamAI examAI;

    public ChatController(ExamAI examAI) {
        this.examAI = examAI;
    }

    @GetMapping("/chat")
    public String chat(@RequestParam String conversationId, @RequestParam String message) {
        return examAI.chat(conversationId, message);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestParam String conversationId, @RequestParam String message) {
        return examAI.stream(conversationId, message);
    }
}
```

The framework implements the Java client; HTTP routes still belong to the consuming application. For POST streaming, browser decoding, and explicit completion/error events, use the [streaming guide](streaming.md), replacing assistant injection with your client interface.

## Package discovery

By default, clients are scanned under Spring Boot's auto-configuration base packages, normally the package of `@SpringBootApplication` and its descendants. Interfaces must be public, non-sealed, and extend `AditusOperations`. Unannotated interfaces are ignored.

For clients in other packages, set a comma-separated list:

```yaml
aditus-cavum:
  client:
    base-packages: com.example.app.ai,com.example.shared.ai
```

This list replaces the default packages. `@SpringBootApplication(scanBasePackages=...)` configures ordinary component scanning and does not by itself change these client scan packages. For contexts without Boot base-package registration, configure `base-packages` explicitly.

Each client is a singleton bean named after its fully qualified interface name, for example `com.example.app.ai.ExamAI`. Inject the concrete interface type rather than `AditusOperations`: the assistant, inherited services, and other clients also implement that shared contract. Existing beans with conflicting client names cause a startup error rather than silent replacement.

## What the proxy does

1. A static auto-configured registry post-processor discovers annotated interfaces and validates their method contracts before consumer beans are created.
2. A Spring `FactoryBean` creates one JDK dynamic proxy per interface and advertises that interface as its object type for constructor injection.
3. Calling a predefined operation resolves the existing `AditusAssistant` and delegates the call. Assistant lookup is delayed until invocation to avoid creating model/tool dependencies during client discovery.
4. Model exceptions retain their original cause; the proxy does not retry calls. `toString`, `equals`, and `hashCode` operate on the proxy without calling a model.

The proxy does not generate Java source, infer custom method semantics, generate prompts from method names, or turn these methods into model tools. `@MarkTheRuins` and `@Need` remain the separate tool-registration mechanism.

## Custom methods

Unknown abstract methods fail validation at startup. For a convenience method, implement it explicitly as a Java `default` method that calls the inherited operations:

```java
@AditusClient
public interface ExamAI extends AditusOperations {
    default String ask(String message) {
        return chat("exam-session", message);
    }
}
```

The fixed conversation ID above is only a convenience example; use a distinct authorized ID per conversation in a multi-user application. Default methods execute Java code locally. They are not interpreted by the model. If you need injected business dependencies or complex overrides, keep using a concrete `@Service` extending `AditusService`.

## Migration and behavior

Choose the style that suits the application: interface proxy, inherited service, or direct `AditusAssistant` injection. Existing approaches remain supported. Replace only forwarding services; retain any existing business validation or authorization logic.

All clients use the same auto-configured assistant and configuration. Separate interfaces do not create separate model configurations or memory namespaces: identical conversation IDs share history. Calls without an ID remain stateless. Streaming remains lazy and retains the existing successful-completion memory behavior. Serialize turns for each conversation; cancellation cannot undo a tool side effect that has already happened.
