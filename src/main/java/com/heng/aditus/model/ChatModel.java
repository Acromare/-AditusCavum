package com.heng.aditus.model;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.List;

public interface ChatModel {
    String chat(List<ChatMessage> messages);

    default String chat(String message) {
        return chat(List.of(ChatMessage.user(message)));
    }

    default Flux<String> stream(String message) {
        return stream(List.of(ChatMessage.user(message)));
    }

    /** Custom models can override this to emit incremental text. */
    default Flux<String> stream(List<ChatMessage> messages) {
        return reactor.core.publisher.Mono.fromCallable(() -> chat(messages))
                .subscribeOn(Schedulers.boundedElastic()).flux();
    }
}
