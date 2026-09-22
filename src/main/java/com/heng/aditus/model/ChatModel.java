package com.heng.aditus.model;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * Model adapter contract for sending prepared messages and returning text.
 * <p>This layer does not add system prompts or load conversation history; those belong to
 * {@link com.heng.aditus.AditusAssistant}. Register a custom {@code ChatModel} bean to replace
 * the default OpenAI-compatible implementation.
 */
public interface ChatModel {
    /**
     * Calls the model synchronously.
     * @param messages the complete context in chronological order
     * @return the complete reply text
     */
    String chat(List<ChatMessage> messages);

    /**
     * Calls the model with a single user message.
     * @param message the user input
     * @return the complete reply text
     */
    default String chat(String message) {
        return chat(List.of(ChatMessage.user(message)));
    }

    /**
     * Requests a reply stream for a single user message.
     * @param message the user input
     * @return the stream provided by the list-based overload
     */
    default Flux<String> stream(String message) {
        return stream(List.of(ChatMessage.user(message)));
    }

    /**
     * Returns a reply stream; custom models can override this for incremental output.
     * <p>The default implementation schedules a blocking call on boundedElastic after subscription
     * and emits the complete reply once. It does not receive incremental text from the provider.
     * @param messages the complete context in chronological order
     * @return the reply text stream
     */
    default Flux<String> stream(List<ChatMessage> messages) {
        return reactor.core.publisher.Mono.fromCallable(() -> chat(messages))
                .subscribeOn(Schedulers.boundedElastic()).flux();
    }
}
