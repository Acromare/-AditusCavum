package com.heng.aditus;

import com.heng.aditus.config.AditusConfiguration;
import com.heng.aditus.memory.ChatMemory;
import com.heng.aditus.model.ChatMessage;
import com.heng.aditus.model.ChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AditusServiceTests {
    @Test
    void emptySubclassInheritsChatStreamingAndCleanupThroughAutoConfiguration() {
        var model = new RecordingModel();
        runner(model).withBean(ExamService.class).run(context -> {
            assertNull(context.getStartupFailure());
            var service = context.getBean(ExamService.class);
            var memory = context.getBean(ChatMemory.class);
            assertEquals("answer", service.chat("a", "first"));
            Flux<String> stream = service.stream("a", "next");
            assertEquals(1, model.seen.size(), "stream must remain lazy");
            assertEquals(List.of("hello", " world"), stream.collectList().block(Duration.ofSeconds(3)));
            assertEquals(List.of(ChatMessage.system("exam system"), ChatMessage.user("first"),
                    ChatMessage.assistant("answer"), ChatMessage.user("next")), model.seen.get(1));
            service.chat("b", "isolated");
            assertEquals(List.of(ChatMessage.system("exam system"), ChatMessage.user("isolated")), model.seen.get(2));
            service.chat("stateless");
            service.stream("stateless stream").blockLast(Duration.ofSeconds(3));
            assertEquals(2, model.seen.get(3).size());
            assertEquals(2, model.seen.get(4).size());
            assertEquals(4, memory.messages("a").size());
            service.clearConversation("a");
            assertTrue(memory.messages("a").isEmpty());
            assertEquals(2, memory.messages("b").size());
        });
    }

    @Test
    void overridesCanCallSuperAndAlsoApplyToSingleArgumentConvenienceMethods() {
        var model = new RecordingModel();
        runner(model).withBean(CustomService.class).run(context -> {
            assertNull(context.getStartupFailure());
            var service = context.getBean(CustomService.class);
            assertEquals("answer", service.chat("hello"));
            service.stream("hello").blockLast(Duration.ofSeconds(3));
            assertEquals(ChatMessage.user("custom: hello"), model.seen.get(0).get(1));
            assertEquals(ChatMessage.user("custom: hello"), model.seen.get(1).get(1));
        });
    }

    @Test
    void baseServiceIsOptionalAndDoesNotReplaceTheAssistant() {
        runner(new RecordingModel()).run(context -> {
            assertNull(context.getStartupFailure());
            assertTrue(context.getBeansOfType(AditusService.class).isEmpty());
            assertEquals(1, context.getBeansOfType(AditusAssistant.class).size());
            assertEquals("answer", context.getBean(AditusAssistant.class).chat("hello"));
        });
    }

    private ApplicationContextRunner runner(RecordingModel model) {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(AditusConfiguration.class))
                .withPropertyValues("aditus-cavum.chat.system-prompt=exam system")
                .withBean(ChatModel.class, () -> model);
    }

    static class ExamService extends AditusService {}

    static class CustomService extends AditusService {
        @Override
        public String chat(String conversationId, String message) {
            return super.chat(conversationId, "custom: " + message);
        }

        @Override
        public Flux<String> stream(String conversationId, String message) {
            return super.stream(conversationId, "custom: " + message);
        }
    }

    static class RecordingModel implements ChatModel {
        final List<List<ChatMessage>> seen = new ArrayList<>();

        public String chat(List<ChatMessage> messages) {
            seen.add(List.copyOf(messages));
            return "answer";
        }

        public Flux<String> stream(List<ChatMessage> messages) {
            return Flux.defer(() -> {
                seen.add(List.copyOf(messages));
                return Flux.just("hello", " world");
            });
        }
    }
}
