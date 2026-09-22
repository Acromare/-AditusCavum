package com.heng.aditus.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.heng.aditus.config.AditusProperties;
import com.heng.aditus.tool.AditusTool;
import com.heng.aditus.tool.ToolRegistry;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.TreeMap;

public class OpenAiCompatibleChatModel implements ChatModel {
    private final ObjectMapper mapper;
    private final ToolRegistry registry;
    private final AditusProperties properties;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();

    public OpenAiCompatibleChatModel(ObjectMapper mapper, ToolRegistry registry, AditusProperties properties) {
        this.mapper = mapper;
        this.registry = registry;
        this.properties = properties;
    }

    @Override
    public String chat(List<ChatMessage> inputMessages) {
        ArrayNode messages = messages(inputMessages);
        for (int round = 0; round <= properties.getChat().getMaxToolRounds(); round++) {
            JsonNode response = request(messages, false);
            JsonNode messageNode = response.path("choices").path(0).path("message");
            JsonNode calls = messageNode.path("tool_calls");
            if (!calls.isArray() || calls.isEmpty()) return messageNode.path("content").asText("");
            checkToolRound(round);
            messages.add(messageNode);
            for (JsonNode call : calls) executeTool(messages, call);
        }
        throw new IllegalStateException("Maximum tool-call rounds exceeded");
    }

    @Override
    public Flux<String> stream(String message) {
        return stream(List.of(ChatMessage.user(message)));
    }

    @Override
    public Flux<String> stream(List<ChatMessage> inputMessages) {
        return Flux.defer(() -> streamRound(messages(inputMessages), 0));
    }

    private ArrayNode messages(List<ChatMessage> inputMessages) {
        ArrayNode messages = mapper.createArrayNode();
        for (ChatMessage input : inputMessages) {
            messages.addObject().put("role", input.role()).put("content", input.content());
        }
        return messages;
    }

    private Flux<String> streamRound(ArrayNode messages, int round) {
        return Flux.defer(() -> {
            var calls = new TreeMap<Integer, ObjectNode>();
            var content = new StringBuilder();
            String[] finishReason = {null};
            boolean[] done = {false};
            Flux<String> events = Flux.using(
                    () -> SseStream.open(client, buildRequest(messages, true)),
                    response -> Flux.<String>generate(sink -> {
                        try {
                            String event = response.nextEvent();
                            if (event == null) sink.complete();
                            else sink.next(event);
                        } catch (IOException exception) {
                            sink.error(new IllegalStateException("Model stream read failed", exception));
                        }
                    }), SseStream::close)
                    .subscribeOn(Schedulers.boundedElastic())
                    .timeout(Duration.ofMinutes(2));

            Flux<String> tokens = events.takeUntil("[DONE]"::equals).<String>handle((event, sink) -> {
                if (event.equals("[DONE]")) {
                    done[0] = true;
                    return;
                }
                try {
                    JsonNode chunk = mapper.readTree(event);
                    if (chunk == null || !chunk.isObject()) throw new IllegalStateException("Invalid model stream event");
                    if (chunk.has("error")) throw new IllegalStateException("Model stream error: " + chunk.path("error"));
                    JsonNode choice = chunk.path("choices").path(0);
                    if (choice.isMissingNode()) return; // e.g. a usage-only event
                    if (!choice.path("finish_reason").isNull() && !choice.path("finish_reason").isMissingNode()) {
                        finishReason[0] = choice.path("finish_reason").asText();
                    }
                    JsonNode delta = choice.path("delta");
                    for (JsonNode part : delta.path("tool_calls")) {
                        if (!part.path("index").canConvertToInt() || part.path("index").asInt() < 0) {
                            throw new IllegalStateException("Missing or invalid tool-call index");
                        }
                        ObjectNode call = calls.computeIfAbsent(part.path("index").asInt(), index -> {
                            ObjectNode node = mapper.createObjectNode().put("type", "function").put("id", "");
                            node.putObject("function").put("name", "").put("arguments", "");
                            return node;
                        });
                        append(call, "id", part.path("id"));
                        ObjectNode function = (ObjectNode) call.path("function");
                        append(function, "name", part.path("function").path("name"));
                        append(function, "arguments", part.path("function").path("arguments"));
                    }
                    if (delta.path("content").isTextual() && !delta.path("content").asText().isEmpty()) {
                        String text = delta.path("content").asText();
                        content.append(text);
                        sink.next(text);
                    }
                } catch (Exception exception) {
                    sink.error(exception);
                }
            });
            return tokens.concatWith(Flux.defer(() -> {
                if (!done[0] || finishReason[0] == null) {
                    return Flux.error(new IllegalStateException("Model stream ended before completion"));
                }
                if (calls.isEmpty()) {
                    if (!"stop".equals(finishReason[0])) {
                        return Flux.error(new IllegalStateException("Model stream did not finish normally: " + finishReason[0]));
                    }
                    return Flux.empty();
                }
                if (!"tool_calls".equals(finishReason[0])) {
                    return Flux.error(new IllegalStateException("Incomplete model tool calls: " + finishReason[0]));
                }
                checkToolRound(round);
                // Validate every call before any side effect is allowed.
                for (ObjectNode call : calls.values()) {
                    if (call.path("id").asText().isBlank() || registry.get(call.path("function").path("name").asText()) == null) {
                        return Flux.error(new IllegalStateException("Invalid or unknown model tool call"));
                    }
                    try {
                        JsonNode arguments = mapper.readTree(call.path("function").path("arguments").asText());
                        if (arguments == null || !arguments.isObject()) throw new IllegalArgumentException("Expected a JSON object");
                    } catch (IOException | IllegalArgumentException exception) {
                        return Flux.error(new IllegalStateException("Invalid model tool arguments", exception));
                    }
                }
                ObjectNode assistant = messages.addObject().put("role", "assistant").put("content", content.toString());
                ArrayNode toolCalls = assistant.putArray("tool_calls");
                calls.values().forEach(toolCalls::add);
                for (ObjectNode call : calls.values()) executeTool(messages, call);
                return streamRound(messages, round + 1);
            }));
        });
    }

    private static void append(ObjectNode target, String field, JsonNode part) {
        if (part.isTextual()) target.put(field, target.path(field).asText() + part.asText());
    }

    private void checkToolRound(int round) {
        if (round >= properties.getChat().getMaxToolRounds()) {
            throw new IllegalStateException("Maximum tool-call rounds exceeded");
        }
    }

    private void executeTool(ArrayNode messages, JsonNode call) {
        String name = call.path("function").path("name").asText();
        AditusTool tool = registry.get(name);
        if (tool == null) throw new IllegalArgumentException("Unknown Aditus tool: " + name);
        try {
            JsonNode arguments = mapper.readTree(call.path("function").path("arguments").asText("{}"));
            Object result = tool.invoke(arguments, mapper);
            ObjectNode toolMessage = messages.addObject();
            toolMessage.put("role", "tool");
            toolMessage.put("tool_call_id", call.path("id").asText());
            toolMessage.put("content", mapper.writeValueAsString(result));
        } catch (Exception exception) {
            throw new IllegalStateException("Tool execution failed: " + name, exception);
        }
    }

    private HttpRequest buildRequest(ArrayNode messages, boolean stream) {
        if (properties.getModel().getApiKey() == null || properties.getModel().getApiKey().isBlank()) {
            throw new IllegalStateException("Set aditus-cavum.model.api-key before calling the model");
        }
        ObjectNode body = mapper.createObjectNode();
        body.put("model", properties.getModel().getModel());
        body.set("messages", messages);
        if (!registry.all().isEmpty()) {
            ArrayNode tools = body.putArray("tools");
            for (AditusTool tool : registry.all()) tools.add(mapper.valueToTree(tool.asOpenAiTool()));
            body.put("tool_choice", "auto");
        }
        body.put("stream", stream);
        return HttpRequest.newBuilder()
                .uri(URI.create(properties.getModel().getBaseUrl().replaceAll("/$", "") + "/chat/completions"))
                .timeout(Duration.ofMinutes(2))
                .header("Authorization", "Bearer " + properties.getModel().getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
    }

    private JsonNode request(ArrayNode messages, boolean stream) {
        try {
            HttpResponse<String> response = client.send(buildRequest(messages, stream), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) throw new IllegalStateException("Model request failed (" + response.statusCode() + "): " + response.body());
            return mapper.readTree(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Model request interrupted", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Model request failed", exception);
        }
    }
}
