package com.heng.aditus.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.heng.aditus.config.AditusProperties;
import com.heng.aditus.tool.AditusTool;
import com.heng.aditus.tool.ToolRegistry;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.List;

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
        ArrayNode messages = mapper.createArrayNode();
        for (ChatMessage input : inputMessages) {
            messages.addObject().put("role", input.role()).put("content", input.content());
        }
        for (int round = 0; round <= properties.getChat().getMaxToolRounds(); round++) {
            JsonNode response = request(messages, false);
            JsonNode messageNode = response.path("choices").path(0).path("message");
            JsonNode calls = messageNode.path("tool_calls");
            if (!calls.isArray() || calls.isEmpty()) return messageNode.path("content").asText("");
            messages.add(messageNode);
            for (JsonNode call : calls) executeTool(messages, call);
        }
        throw new IllegalStateException("Maximum tool-call rounds exceeded");
    }

    @Override
    public Flux<String> stream(String message) {
        return Flux.defer(() -> Flux.just(chat(message)));
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

    private JsonNode request(ArrayNode messages, boolean stream) {
        if (properties.getModel().getApiKey() == null || properties.getModel().getApiKey().isBlank()) {
            throw new IllegalStateException("Set aditus-cavum.model.api-key before calling the model");
        }
        ObjectNode body = mapper.createObjectNode();
        body.put("model", properties.getModel().getModel());
        body.set("messages", messages);
        ArrayNode tools = body.putArray("tools");
        for (AditusTool tool : registry.all()) tools.add(mapper.valueToTree(tool.asOpenAiTool()));
        body.put("tool_choice", "auto");
        body.put("stream", stream);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.getModel().getBaseUrl().replaceAll("/$", "") + "/chat/completions"))
                .timeout(Duration.ofMinutes(2))
                .header("Authorization", "Bearer " + properties.getModel().getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) throw new IllegalStateException("Model request failed (" + response.statusCode() + "): " + response.body());
            return mapper.readTree(response.body());
        } catch (IOException | InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Model request failed", exception);
        }
    }
}
