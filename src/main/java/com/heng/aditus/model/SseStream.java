package com.heng.aditus.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Incremental UTF-8 SSE reader used by the model adapter.
 * <p>Joins data lines within each event and ignores other SSE fields. Closing the response body
 * releases the connection and unblocks a pending read when a subscriber cancels the stream.
 */
final class SseStream {
    private final InputStream body;
    private final BufferedReader reader;

    private SseStream(InputStream body) {
        this.body = body;
        this.reader = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8));
    }

    /**
     * Opens an HTTP response, closing its body if the status or content type is unsuitable.
     * @param client the HTTP client
     * @param request the streaming model request
     * @return a reader owning the response body
     * @throws IOException if the HTTP exchange fails
     * @throws InterruptedException if the exchange is interrupted
     * @throws IllegalStateException if the response is unsuccessful or is not an event stream
     */
    static SseStream open(HttpClient client, HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        SseStream stream = new SseStream(response.body());
        if (response.statusCode() / 100 != 2) {
            stream.close();
            throw new IllegalStateException("Model stream request failed (" + response.statusCode() + ")");
        }
        if (!response.headers().firstValue("Content-Type").orElse("").toLowerCase(java.util.Locale.ROOT).startsWith("text/event-stream")) {
            stream.close();
            throw new IllegalStateException("Expected text/event-stream from model");
        }
        return stream;
    }

    /**
     * Reads through the blank line terminating the next data event.
     * @return joined data lines, or null at EOF; an unterminated final event is discarded
     * @throws IOException if reading the response fails
     */
    String nextEvent() throws IOException {
        StringBuilder data = new StringBuilder();
        boolean hasData = false;
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty()) {
                if (hasData) return data.toString();
            } else if (line.equals("data") || line.startsWith("data:")) {
                String value = line.equals("data") ? "" : line.substring(5);
                if (value.startsWith(" ")) value = value.substring(1);
                if (hasData) data.append('\n');
                data.append(value);
                hasData = true;
            }
        }
        return null; // An event without its terminating blank line is incomplete.
    }

    /** Closes the body directly to unblock reads, ignoring errors from an already disconnected response. */
    void close() {
        try { body.close(); } catch (IOException ignored) { /* already disconnected */ }
    }
}
