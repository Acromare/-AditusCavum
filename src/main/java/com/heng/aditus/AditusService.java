package com.heng.aditus;

import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;

/**
 * Optional base for application-owned Spring services.
 * Register the subclass with {@code @Service}; override only the operations you customize.
 */
public abstract class AditusService {
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
