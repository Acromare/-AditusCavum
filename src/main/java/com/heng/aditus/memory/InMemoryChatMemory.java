package com.heng.aditus.memory;

import com.heng.aditus.model.ChatMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Stores conversations in JVM memory and removes the oldest messages when capacity is exceeded.
 * <p>Capacity counts messages, usually two per question-and-answer turn. There is no whole-history
 * reset by turn count, expiration, or persistence; restarting the application loses history.
 * Concurrent containers do not make an entire turn atomic. Avoid concurrent writes or clearing
 * a conversation while another request is updating it.
 */
public class InMemoryChatMemory implements ChatMemory {
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<ChatMessage>> conversations = new ConcurrentHashMap<>();
    private final int maxMessages;

    /**
     * Sets the maximum number of retained messages per conversation.
     * @param maxMessages the per-conversation capacity, at least 2
     * @throws IllegalArgumentException if capacity is less than 2
     */
    public InMemoryChatMemory(int maxMessages) {
        if (maxMessages < 2) throw new IllegalArgumentException("maxMessages must be at least 2");
        this.maxMessages = maxMessages;
    }

    /**
     * {@inheritDoc}
     * @return an immutable history snapshot, or an empty list if no history exists
     */
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
