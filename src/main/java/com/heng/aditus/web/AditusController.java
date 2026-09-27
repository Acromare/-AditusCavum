package com.heng.aditus.web;

import com.heng.aditus.AditusAssistant;
import com.heng.aditus.config.AditusProperties;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/** Zero-configuration HTTP API for chat, streaming, and conversation cleanup. */
@RestController
@RequestMapping("${aditus-cavum.web.path:/aditus}/api")
public class AditusController {
    private final AditusAssistant assistant;

    public AditusController(AditusAssistant assistant) {
        this.assistant = assistant;
    }

    @PostMapping("/chat")
    public AditusChatResponse chat(@RequestBody AditusChatRequest request) {
        requireMessage(request);
        return new AditusChatResponse(request.conversationId(),
                assistant.chat(request.conversationId(), request.message()));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestParam(required = false) String conversationId,
                               @RequestParam String message) {
        if (message == null || message.isBlank()) throw new IllegalArgumentException("message must not be blank");
        return assistant.stream(conversationId, message);
    }

    @DeleteMapping("/conversation/{conversationId}")
    public void clear(@PathVariable String conversationId) {
        assistant.clearConversation(conversationId);
    }

    private static void requireMessage(AditusChatRequest request) {
        if (request == null || request.message() == null || request.message().isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
    }
}
