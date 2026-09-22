package com.heng.aditus;

import com.heng.aditus.model.ChatModel;
import com.heng.aditus.config.AditusProperties;
import com.heng.aditus.memory.ChatMemory;
import com.heng.aditus.model.ChatMessage;
import reactor.core.publisher.Flux;

public class AditusAssistant {
    private final ChatModel model;
    private final ChatMemory memory;
    private final AditusProperties properties;

    public AditusAssistant(ChatModel model, ChatMemory memory, AditusProperties properties) {
        this.model = model;
        this.memory = memory;
        this.properties = properties;
    }

    public String chat(String message) {
        java.util.List<ChatMessage> messages = new java.util.ArrayList<>();
        if (!properties.getChat().getSystemPrompt().isBlank()) {
            messages.add(ChatMessage.system(properties.getChat().getSystemPrompt()));
        }
        messages.add(ChatMessage.user(message));
        return model.chat(messages);
    }

    public Flux<String> stream(String message) {
        return stream(null, message);
    }

    /** Each subscription is a new request; save only successfully completed turns. */
    public Flux<String> stream(String conversationId, String message) {
        return Flux.defer(() -> {
            boolean remember = conversationId != null && !conversationId.isBlank();
            java.util.List<ChatMessage> messages = new java.util.ArrayList<>();
            if (!properties.getChat().getSystemPrompt().isBlank()) {
                messages.add(ChatMessage.system(properties.getChat().getSystemPrompt()));
            }
            if (remember) messages.addAll(memory.messages(conversationId));
            messages.add(ChatMessage.user(message));
            StringBuilder answer = new StringBuilder();
            return model.stream(messages)
                    .doOnNext(answer::append)
                    .doOnComplete(() -> {
                        if (remember) {
                            memory.append(conversationId, ChatMessage.user(message));
                            memory.append(conversationId, ChatMessage.assistant(answer.toString()));
                        }
                    });
        });
    }

    public String chat(String conversationId, String message) {
        if (conversationId == null || conversationId.isBlank()) return chat(message);
        memory.append(conversationId, ChatMessage.user(message));
        java.util.List<ChatMessage> messages = new java.util.ArrayList<>(memory.messages(conversationId));
        if (!properties.getChat().getSystemPrompt().isBlank()) {
            messages.add(0, ChatMessage.system(properties.getChat().getSystemPrompt()));
        }
        String answer = model.chat(messages);
        memory.append(conversationId, ChatMessage.assistant(answer));
        return answer;
    }

    public void clearConversation(String conversationId) {
        memory.clear(conversationId);
    }
}
