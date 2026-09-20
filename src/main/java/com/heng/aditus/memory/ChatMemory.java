package com.heng.aditus.memory;

import com.heng.aditus.model.ChatMessage;

import java.util.List;

public interface ChatMemory {
    List<ChatMessage> messages(String conversationId);
    void append(String conversationId, ChatMessage message);
    void clear(String conversationId);
}
