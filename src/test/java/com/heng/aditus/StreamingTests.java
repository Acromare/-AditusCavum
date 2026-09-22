package com.heng.aditus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heng.aditus.annotation.MarkTheRuins;
import com.heng.aditus.annotation.Need;
import com.heng.aditus.config.AditusProperties;
import com.heng.aditus.memory.InMemoryChatMemory;
import com.heng.aditus.model.ChatMessage;
import com.heng.aditus.model.ChatModel;
import com.heng.aditus.model.OpenAiCompatibleChatModel;
import com.heng.aditus.tool.ToolRegistry;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class StreamingTests {
    private final ObjectMapper mapper = new ObjectMapper();
    private final AditusProperties properties = new AditusProperties();
    private final Tools tools = new Tools();
    private HttpServer server;
    private ExecutorService executor;
    private GenericApplicationContext context;
    private OpenAiCompatibleChatModel model;

    @BeforeEach
    void setup() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        executor = Executors.newCachedThreadPool();
        server.setExecutor(executor);
        server.start();
        properties.getModel().setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.getModel().setApiKey("test-key");
        context = new GenericApplicationContext();
        context.registerBean(Tools.class, () -> tools);
        context.refresh();
        model = new OpenAiCompatibleChatModel(mapper, new ToolRegistry(context, mapper), properties);
    }

    @AfterEach
    void cleanup() {
        server.stop(0);
        executor.shutdownNow();
        context.close();
    }

    @Test
    void emitsFirstChunkBeforeServerFinishesAndSendsStreamTrue() throws Exception {
        var first = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var request = new AtomicReference<JsonNode>();
        server.createContext("/chat/completions", exchange -> {
            request.set(mapper.readTree(exchange.getRequestBody()));
            start(exchange);
            // Multi-line SSE, comments, CRLF, and non-ASCII content.
            write(exchange, ": ping\r\nevent: message\r\ndata: {\"choices\":\r\ndata: [{\"delta\":{\"content\":\"你\"}}]}\r\n\r\n");
            try { release.await(5, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            write(exchange, text("好\n") + finish("stop") + done());
            exchange.close();
        });
        var result = model.stream("hello").doOnNext(value -> first.countDown()).collectList().toFuture();
        try {
            assertTrue(first.await(4, TimeUnit.SECONDS), "first token must arrive before response completion");
            assertFalse(result.isDone());
            assertTrue(request.get().path("stream").asBoolean());
        } finally { release.countDown(); }
        assertEquals(List.of("你", "好\n"), result.get(5, TimeUnit.SECONDS));
    }

    @Test
    void assemblesInterleavedToolCallsAndContinuesStreaming() {
        var count = new AtomicInteger();
        var secondRequest = new AtomicReference<JsonNode>();
        server.createContext("/chat/completions", exchange -> {
            JsonNode body = mapper.readTree(exchange.getRequestBody());
            start(exchange);
            if (count.getAndIncrement() == 0) {
                write(exchange, call(0, "one", "ec", "{\"value\":")
                        + call(1, "two", "echo", "{\"value\":\"B\"}")
                        + call(0, null, "ho", "\"A\"}") + finish("tool_calls") + done());
            } else {
                secondRequest.set(body);
                write(exchange, text("Saved") + text("!") + finish("stop") + done());
            }
            exchange.close();
        });
        assertEquals(List.of("Saved", "!"), model.stream("run tools").collectList().block(Duration.ofSeconds(5)));
        assertEquals(2, tools.calls.get());
        JsonNode messages = secondRequest.get().path("messages");
        assertEquals("one", messages.path(2).path("tool_call_id").asText());
        assertEquals("\"A\"", messages.path(2).path("content").asText());
        assertEquals("two", messages.path(3).path("tool_call_id").asText());
        assertEquals("\"B\"", messages.path(3).path("content").asText());
    }

    @Test
    void refusesTruncatedToolArgumentsWithoutExecutingAnything() throws IOException {
        respond(call(0, "one", "echo", "{\"value\":\"A\"}")
                + call(1, "two", "echo", "{\"value\":") + finish("tool_calls") + done());
        assertThrows(IllegalStateException.class, () -> model.stream("go").collectList().block(Duration.ofSeconds(5)));
        assertEquals(0, tools.calls.get());
    }

    @Test
    void enforcesToolRoundLimitBeforeSideEffects() throws IOException {
        properties.getChat().setMaxToolRounds(0);
        respond(call(0, "one", "echo", "{\"value\":\"A\"}") + finish("tool_calls") + done());
        var error = assertThrows(IllegalStateException.class, () -> model.stream("go").collectList().block(Duration.ofSeconds(5)));
        assertTrue(error.getMessage().contains("Maximum tool-call rounds"));
        assertEquals(0, tools.calls.get());
    }

    @Test
    void reportsPrematureEofAndDoesNotCommitMemory() throws IOException {
        respond(text("partial"));
        var memory = new InMemoryChatMemory(20);
        var assistant = new AditusAssistant(model, memory, properties);
        assertThrows(IllegalStateException.class, () -> assistant.stream("id", "hello").collectList().block(Duration.ofSeconds(5)));
        assertTrue(memory.messages("id").isEmpty());
    }

    @Test
    void propagatesHttpErrors() {
        server.createContext("/chat/completions", exchange -> {
            exchange.getRequestBody().readAllBytes();
            exchange.sendResponseHeaders(401, -1);
            exchange.close();
        });
        var error = assertThrows(IllegalStateException.class, () -> model.stream("hello").collectList().block(Duration.ofSeconds(5)));
        assertTrue(error.getMessage().contains("401"));
    }

    @Test
    void propagatesSseErrors() {
        respond("data: {\"error\":{\"message\":\"unavailable\"}}\n\n");
        var error = assertThrows(IllegalStateException.class, () -> model.stream("hello").collectList().block(Duration.ofSeconds(5)));
        assertTrue(error.getMessage().contains("Model stream error"));
    }

    @Test
    void cancellationClosesUpstreamAndLeavesMemoryUntouched() throws Exception {
        var closed = new CountDownLatch(1);
        server.createContext("/chat/completions", exchange -> {
            exchange.getRequestBody().readAllBytes();
            start(exchange);
            try {
                write(exchange, text("first"));
                for (int i = 0; i < 100; i++) {
                    write(exchange, ":" + "x".repeat(8192) + "\n\n");
                    Thread.sleep(20);
                }
            } catch (IOException e) { closed.countDown(); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            finally { exchange.close(); }
        });
        var memory = new InMemoryChatMemory(20);
        var assistant = new AditusAssistant(model, memory, properties);
        assertEquals("first", assistant.stream("id", "hello").take(1).blockLast(Duration.ofSeconds(5)));
        assertTrue(memory.messages("id").isEmpty());
        assertTrue(closed.await(4, TimeUnit.SECONDS), "cancellation must close the response body");
    }

    @Test
    void includesSystemPromptAndIsolatesConversationMemory() {
        var seen = new java.util.ArrayList<List<ChatMessage>>();
        ChatModel fake = new ChatModel() {
            public String chat(List<ChatMessage> messages) { throw new AssertionError("must use stream"); }
            public Flux<String> stream(List<ChatMessage> messages) {
                seen.add(List.copyOf(messages));
                return Flux.just("hello", " world");
            }
        };
        properties.getChat().setSystemPrompt("system");
        var memory = new InMemoryChatMemory(20);
        var assistant = new AditusAssistant(fake, memory, properties);
        Flux<String> lazy = assistant.stream("a", "first");
        assertTrue(seen.isEmpty());
        lazy.blockLast();
        assistant.stream("a", "next").blockLast();
        assistant.stream("b", "isolated").blockLast();
        assistant.stream("stateless").blockLast();
        assertEquals(List.of(ChatMessage.system("system"), ChatMessage.user("first"),
                ChatMessage.assistant("hello world"), ChatMessage.user("next")), seen.get(1));
        assertEquals(2, seen.get(2).size());
        assertEquals(ChatMessage.system("system"), seen.get(3).get(0));
        assertEquals(4, memory.messages("a").size());
    }

    @Test
    void customChatOnlyModelStillWorksLazily() {
        var count = new AtomicInteger();
        ChatModel fake = messages -> { count.incrementAndGet(); return "fallback"; };
        Flux<String> stream = fake.stream("hello");
        assertEquals(0, count.get());
        assertEquals("fallback", stream.blockLast(Duration.ofSeconds(3)));
    }

    private void respond(String events) {
        server.createContext("/chat/completions", exchange -> {
            exchange.getRequestBody().readAllBytes();
            start(exchange);
            write(exchange, events);
            exchange.close();
        });
    }

    private void start(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/event-stream; charset=utf-8");
        exchange.sendResponseHeaders(200, 0);
    }

    private void write(HttpExchange exchange, String text) throws IOException {
        exchange.getResponseBody().write(text.getBytes(StandardCharsets.UTF_8));
        exchange.getResponseBody().flush();
    }

    private String text(String text) throws IOException {
        return "data: {\"choices\":[{\"delta\":{\"content\":" + mapper.writeValueAsString(text) + "}}]}\n\n";
    }

    private String call(int index, String id, String name, String arguments) throws IOException {
        var part = mapper.createObjectNode().put("index", index);
        if (id != null) part.put("id", id);
        part.putObject("function").put("name", name).put("arguments", arguments);
        return "data: {\"choices\":[{\"delta\":{\"tool_calls\":[" + part + "]}}]}\n\n";
    }

    private String finish(String reason) { return "data: {\"choices\":[{\"delta\":{},\"finish_reason\":\"" + reason + "\"}]}\n\n"; }
    private String done() { return "data: [DONE]\n\n"; }

    @MarkTheRuins
    static class Tools {
        final AtomicInteger calls = new AtomicInteger();
        @Need("Echo the value")
        public String echo(String value) { calls.incrementAndGet(); return value; }
    }
}
