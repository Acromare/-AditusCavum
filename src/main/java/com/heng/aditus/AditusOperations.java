package com.heng.aditus;

import reactor.core.publisher.Flux;

/** Shared operations for assistant injection, service inheritance, and client interfaces. */
public interface AditusOperations {
    String chat(String message);
    String chat(String conversationId, String message);
    Flux<String> stream(String message);
    Flux<String> stream(String conversationId, String message);
    void clearConversation(String conversationId);
}
