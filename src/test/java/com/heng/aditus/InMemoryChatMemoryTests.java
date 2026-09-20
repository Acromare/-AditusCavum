package com.heng.aditus;

import com.heng.aditus.memory.InMemoryChatMemory;
import com.heng.aditus.model.ChatMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InMemoryChatMemoryTests {
    @Test
    void keepsOnlyConfiguredNumberOfMessages() {
        InMemoryChatMemory memory = new InMemoryChatMemory(2);
        memory.append("conversation", ChatMessage.user("one"));
        memory.append("conversation", ChatMessage.assistant("two"));
        memory.append("conversation", ChatMessage.user("three"));

        assertEquals(2, memory.messages("conversation").size());
        assertEquals("two", memory.messages("conversation").get(0).content());
        assertEquals("three", memory.messages("conversation").get(1).content());
    }
}
