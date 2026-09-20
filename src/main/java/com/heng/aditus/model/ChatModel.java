package com.heng.aditus.model;

import reactor.core.publisher.Flux;

import java.util.List;

public interface ChatModel {
    String chat(List<ChatMessage> messages);

    default String chat(String message) {
        return chat(List.of(ChatMessage.user(message)));
    }

    default Flux<String> stream(String message) {
        return Flux.just(chat(message));
    }
}
