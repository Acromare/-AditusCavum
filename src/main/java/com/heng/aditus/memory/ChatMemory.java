package com.heng.aditus.memory;

import com.heng.aditus.model.ChatMessage;

import java.util.List;

/**
 * Extension point for storing messages by conversation ID.
 * <p>The default implementation uses process memory. Register a custom implementation bean
 * to use a database or Redis. This contract does not handle authorization, request ordering,
 * or rollback of tool side effects.
 */
public interface ChatMemory {
    /**
     * Reads conversation history in insertion order.
     * @param conversationId the non-null conversation ID
     * @return the history, or an empty list for an unknown conversation
     */
    List<ChatMessage> messages(String conversationId);
    /**
     * Appends a message; retention limits are implementation-specific.
     * @param conversationId the non-null conversation ID
     * @param message the message to save
     */
    void append(String conversationId, ChatMessage message);
    /**
     * Clears history, doing nothing if the conversation does not exist.
     * @param conversationId the non-null conversation ID
     */
    void clear(String conversationId);
}
