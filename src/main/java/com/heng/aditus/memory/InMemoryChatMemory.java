package com.heng.aditus.memory;

import com.heng.aditus.model.ChatMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryChatMemory implements ChatMemory {
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<ChatMessage>> conversations = new ConcurrentHashMap<>();
    private final int maxMessages;

    public InMemoryChatMemory(int maxMessages) {
        if (maxMessages < 2) throw new IllegalArgumentException("maxMessages must be at least 2");
        this.maxMessages = maxMessages;
    }

    @Override
    public List<ChatMessage> messages(String conversationId) {
        return List.copyOf(conversations.getOrDefault(conversationId, new CopyOnWriteArrayList<>()));
    }

    @Override
    public void append(String conversationId, ChatMessage message) {
        CopyOnWriteArrayList<ChatMessage> messages = conversations.computeIfAbsent(conversationId, key -> new CopyOnWriteArrayList<>());
        messages.add(message);
        while (messages.size() > maxMessages) messages.remove(0);
    }

    @Override
    public void clear(String conversationId) {
        conversations.remove(conversationId);
    }
}
