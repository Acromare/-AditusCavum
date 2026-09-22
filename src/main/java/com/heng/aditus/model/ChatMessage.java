package com.heng.aditus.model;

/**
 * Immutable text message used to assemble context and store conversation history.
 * @param role the message role, typically system, user, or assistant
 * @param content the message text
 */
public record ChatMessage(String role, String content) {
    /**
     * Creates a system instruction message.
     * @param content the system prompt
     * @return a message with the system role
     */
    public static ChatMessage system(String content) { return new ChatMessage("system", content); }
    /**
     * Creates a user message.
     * @param content the user input
     * @return a message with the user role
     */
    public static ChatMessage user(String content) { return new ChatMessage("user", content); }
    /**
     * Creates a model reply message.
     * @param content the model reply
     * @return a message with the assistant role
     */
    public static ChatMessage assistant(String content) { return new ChatMessage("assistant", content); }
}
