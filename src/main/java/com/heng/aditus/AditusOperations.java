package com.heng.aditus;

import reactor.core.publisher.Flux;

/**
 * Shared contract for assistant injection, service inheritance, and client interfaces.
 *
 * <p>The application assigns conversation IDs and checks access to them. Clients using the
 * same assistant and ID share history, even across different client interfaces. An ID is
 * not an authorization boundary. Serialize requests within a conversation to avoid interleaved turns.
 *
 * @see AditusAssistant
 * @see AditusService
 * @see com.heng.aditus.annotation.AditusClient
 */
public interface AditusOperations {
    /**
     * Sends a blocking request with the configured system prompt and no conversation history.
     * @param message the current user input
     * @return the complete model reply
     */
    String chat(String message);
    /**
     * Sends a blocking request with conversation history and saves messages.
     * @param conversationId the application-assigned ID; null or blank disables memory
     * @param message the current user input
     * @return the complete model reply
     */
    String chat(String conversationId, String message);
    /**
     * Streams a reply with the configured system prompt and no conversation history.
     * @param message the current user input
     * @return a lazy text stream; each subscription starts a new request
     */
    Flux<String> stream(String message);
    /**
     * Streams a reply with conversation history, saving the user message and reply on normal completion.
     * <p>Each element is a text fragment, not necessarily one character or token. Partial text
     * may have been emitted before an error. Cancellation and errors do not undo executed tools.
     * @param conversationId the application-assigned ID; null or blank disables memory
     * @param message the current user input
     * @return a lazy text stream; each subscription starts a new request
     */
    Flux<String> stream(String conversationId, String message);
    /**
     * Removes conversation history without undoing tool results or cancelling active requests.
     * @param conversationId the non-null ID of the conversation to clear
     */
    void clearConversation(String conversationId);
}
