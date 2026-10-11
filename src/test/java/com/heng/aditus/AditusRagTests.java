package com.heng.aditus;

import com.heng.aditus.config.AditusProperties;
import com.heng.aditus.memory.InMemoryChatMemory;
import com.heng.aditus.model.ChatMessage;
import com.heng.aditus.model.ChatModel;
import com.heng.aditus.rag.KnowledgeDocument;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import java.util.ArrayList;
import java.util.List;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

class AditusRagTests {
    @Test
    void allChatPathsRetrieveWithoutPersistingKnowledge() {
        List<List<ChatMessage>> requests = new ArrayList<>();
        ChatModel model = messages -> { requests.add(List.copyOf(messages)); return "answer"; };
        var memory = new InMemoryChatMemory(20);
        var assistant = new AditusAssistant(model, memory, new AditusProperties(),
                (query, topK) -> List.of(new KnowledgeDocument("reference", "unique knowledge")));
        assistant.chat("question");
        assistant.chat("session", "question");
        Flux<String> stream = assistant.stream("question");
        assertEquals(2, requests.size());
        stream.blockLast(Duration.ofSeconds(3));
        assistant.stream("session", "question").blockLast(Duration.ofSeconds(3));
        assertEquals(4, requests.size());
        for (var request : requests) {
            assertTrue(request.stream().anyMatch(message -> message.content().contains("unique knowledge")));
        }
        assertEquals(4, memory.messages("session").size());
        assertFalse(memory.messages("session").stream()
                .anyMatch(message -> message.content().contains("unique knowledge")));
    }
}
