package com.heng.aditus;

import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;

/**
 * Optional base class that implements all conversation operations for application services.
 *
 * <p>Annotate a concrete subclass with {@code @Service} and let Spring create it to inject
 * the assistant. No forwarding methods are required. Constructing a subclass with {@code new}
 * does not perform injection. Override only the behavior you need to customize.
 * Single-argument {@code chat/stream} methods delegate to their two-argument counterparts,
 * so overriding those also affects single-argument calls. For an interface-only client, use
 * {@link com.heng.aditus.annotation.AditusClient}.
 */
public abstract class AditusService implements AditusOperations {
    @Autowired
    private AditusAssistant assistant;

    public String chat(String message) {
        return chat(null, message);
    }

    public String chat(String conversationId, String message) {
        return assistant.chat(conversationId, message);
    }

    public Flux<String> stream(String message) {
        return stream(null, message);
    }

    public Flux<String> stream(String conversationId, String message) {
        return assistant.stream(conversationId, message);
    }

    public void clearConversation(String conversationId) {
        assistant.clearConversation(conversationId);
    }
}
