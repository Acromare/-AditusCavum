package com.heng.aditus;

import clientfixtures.valid.ExamAI;
import clientfixtures.valid.OtherAI;
import clientfixtures.unmarked.UnmarkedAI;
import com.heng.aditus.config.AditusConfiguration;
import com.heng.aditus.memory.ChatMemory;
import com.heng.aditus.model.ChatMessage;
import com.heng.aditus.model.ChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AditusClientTests {
    @Test
    void scansBootPackagesAndSupportsConstructorInjectionAndAllOperations() {
        var seen = new ArrayList<List<ChatMessage>>();
        ChatModel model = new ChatModel() {
            public String chat(List<ChatMessage> messages) { seen.add(List.copyOf(messages)); return "answer"; }
            public Flux<String> stream(List<ChatMessage> messages) {
                return Flux.defer(() -> { seen.add(List.copyOf(messages)); return Flux.just("a", "b"); });
            }
        };
        runner("clientfixtures.valid").withBean(ChatModel.class, () -> model).withBean(Consumer.class).run(context -> {
            assertNull(context.getStartupFailure());
            ExamAI client = context.getBean(Consumer.class).client;
            assertSame(client, context.getBean(ExamAI.class));
            assertEquals("answer", client.chat("a", "first"));
            Flux<String> stream = client.stream("a", "next");
            assertEquals(1, seen.size());
            assertEquals(List.of("a", "b"), stream.collectList().block(Duration.ofSeconds(3)));
            assertEquals(List.of(ChatMessage.system("exam"), ChatMessage.user("first"),
                    ChatMessage.assistant("answer"), ChatMessage.user("next")), seen.get(1));
            client.chat("b", "isolated");
            assertEquals(2, seen.get(2).size());
            client.chat("stateless");
            client.stream("stateless stream").blockLast(Duration.ofSeconds(3));
            assertEquals(2, seen.get(3).size());
            assertEquals(2, seen.get(4).size());
            var memory = context.getBean(ChatMemory.class);
            client.clearConversation("a");
            assertTrue(memory.messages("a").isEmpty());
            assertEquals(2, memory.messages("b").size());
            assertEquals("answer", client.ask("default method"));
            assertEquals(2, memory.messages("exam").size());
            assertEquals(1, context.getBeansOfType(OtherAI.class).size());
        });
    }

    @Test
    void createsClientsWithDefaultModelWithoutCallingProviderOrCircularInitialization() {
        runner("clientfixtures.valid").withBean(Consumer.class).run(context -> {
            assertNull(context.getStartupFailure());
            var client = context.getBean(ExamAI.class);
            assertEquals("AditusClient[clientfixtures.valid.ExamAI]", client.toString());
            assertTrue(client.equals(client));
            assertFalse(client.equals(context.getBean(OtherAI.class)));
            assertFalse(client.equals(null));
            assertEquals(System.identityHashCode(client), client.hashCode());
            // Missing key is only detected when calling the model, never during proxy discovery.
            assertThrows(IllegalStateException.class, () -> client.chat("hello"));
        });
    }

    @Test
    void propagatesOriginalExceptionsAndDoesNotRetry() {
        var calls = new AtomicInteger();
        var failure = new IllegalArgumentException("model failure");
        ChatModel model = messages -> { calls.incrementAndGet(); throw failure; };
        runner("clientfixtures.valid").withBean(ChatModel.class, () -> model).run(context -> {
            assertNull(context.getStartupFailure());
            var client = context.getBean(ExamAI.class);
            assertSame(failure, assertThrows(IllegalArgumentException.class, () -> client.chat("hello")));
            assertSame(failure, assertThrows(IllegalArgumentException.class,
                    () -> client.stream("hello").blockLast(Duration.ofSeconds(3))));
            assertEquals(2, calls.get());
        });
    }

    @Test
    void explicitPackagesOverrideBootPackagesAndOverlapsDoNotDuplicateClients() {
        runner("clientfixtures.invalidtype")
                .withPropertyValues("aditus-cavum.client.base-packages=clientfixtures.valid, clientfixtures.valid, clientfixtures.unmarked")
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    assertEquals(1, context.getBeansOfType(ExamAI.class).size());
                    assertTrue(context.getBeansOfType(UnmarkedAI.class).isEmpty());
                });
    }

    @Test
    void rejectsUnknownMethodsAtStartup() {
        runner("clientfixtures.invalidmethod").run(context -> {
            assertNotNull(context.getStartupFailure());
            assertTrue(context.getStartupFailure().toString().contains("Unsupported Aditus client method"));
        });
    }

    @Test
    void rejectsInterfacesWithoutTheOperationsContract() {
        runner("clientfixtures.invalidtype").run(context -> {
            assertNotNull(context.getStartupFailure());
            assertTrue(context.getStartupFailure().toString().contains("extending AditusOperations"));
        });
    }

    @Test
    void unannotatedInterfacesAreNotRegistered() {
        runner("clientfixtures.unmarked").run(context -> {
            assertNull(context.getStartupFailure());
            assertTrue(context.getBeansOfType(UnmarkedAI.class).isEmpty());
            assertEquals(1, context.getBeansOfType(AditusAssistant.class).size());
        });
    }

    private ApplicationContextRunner runner(String basePackage) {
        return new ApplicationContextRunner()
                .withInitializer(context -> AutoConfigurationPackages.register(
                        (BeanDefinitionRegistry) context.getBeanFactory(), basePackage))
                .withConfiguration(AutoConfigurations.of(AditusConfiguration.class))
                .withPropertyValues("aditus-cavum.chat.system-prompt=exam");
    }

    static class Consumer {
        final ExamAI client;
        Consumer(ExamAI client) { this.client = client; }
    }
}
