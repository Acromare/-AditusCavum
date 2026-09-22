# Streaming integration

Use `assistant.stream(message)` for a stateless request or `assistant.stream(conversationId, message)` for conversation memory. The return value is `Flux<String>` containing incremental text, not a complete JSON response. The application owns its HTTP routes and event envelope.

## A small POST endpoint

This Spring MVC example encodes each text fragment as JSON so whitespace and newlines survive the SSE transport. The terminal event distinguishes successful completion from a disconnected socket.

```java
import com.heng.aditus.AditusAssistant;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class ChatController {
    private final AditusAssistant assistant;

    public ChatController(AditusAssistant assistant) {
        this.assistant = assistant;
    }

    public record ChatRequest(String conversationId, String message) {}

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Map<String, String>>> stream(@RequestBody ChatRequest request) {
        return assistant.stream(request.conversationId(), request.message())
                .map(text -> event("token", text))
                .concatWithValues(event("done", ""))
                .onErrorResume(error -> Flux.just(event("error", "The reply was interrupted.")));
    }

    private ServerSentEvent<Map<String, String>> event(String name, String text) {
        return ServerSentEvent.<Map<String, String>>builder(Map.of("text", text)).event(name).build();
    }
}
```

Log the original exception on the server when handling errors. Once response headers are sent, HTTP status cannot reliably describe a later failure; the `error` event is the application-level signal. Configure the hosting application's Spring MVC async timeout to fit its maximum expected stream duration, and disable reverse-proxy response buffering if present.

## Browser consumption

Send the request with `fetch`, set `Accept: text/event-stream`, and read `response.body.getReader()`. Decode byte chunks with `TextDecoder.decode(value, {stream: true})`, retain incomplete event frames between reads, split frames on blank lines, and parse joined `data:` lines as JSON. UTF-8 characters and SSE events can span arbitrary network chunks. Preserve `data.text` exactly, including spaces and newlines.

- `token`: append `data.text` to the existing reply with `textContent`.
- `done`: mark the reply complete and release the reader.
- `error`: keep any partial text, display a failure message, and release the reader.
- Socket EOF before `done`: treat the reply as incomplete.

Do not automatically retry a whole conversation request: a tool may already have changed application data. Disable duplicate submissions while a turn is active. Cancelling the reader or aborting `fetch` disconnects the HTTP client; when Spring propagates that cancellation, the framework closes the model stream. Cancellation does not undo a tool that has already run.

## Tool calls and memory

The framework reconstructs tool calls by index, concatenating function-name and JSON-argument fragments. It requires a completed `tool_calls` response and validates every call before executing any of that round's tools. It then appends the assistant tool-call message and tool results, and sends another streaming model request. Text from every round is emitted in order.

`max-tool-rounds` limits rounds that actually execute tools. Zero permits a plain model response but prevents tool execution. This limit also applies to synchronous chat.

Compile application tool classes with `-parameters` so Java reflection exposes names such as `name` and `phone` instead of `arg0` and `arg1`:

```xml
<properties>
    <maven.compiler.parameters>true</maven.compiler.parameters>
</properties>
```

Successful streams save one user message and the concatenated assistant text. Errors and cancelled streams save neither. Serialize calls for the same conversation ID and avoid clearing a conversation while its stream is active. In-memory storage remains local to one application process.

Providers must return OpenAI-compatible `choices[].delta` SSE events with a `finish_reason` and `[DONE]`. This release reports `length`, `content_filter`, malformed events, missing completion markers, and provider errors as failed/incomplete streams. The UI may already have received partial text.
