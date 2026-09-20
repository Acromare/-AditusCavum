package com.heng.aditus.model;

public record ChatMessage(String role, String content) {
    public static ChatMessage system(String content) { return new ChatMessage("system", content); }
    public static ChatMessage user(String content) { return new ChatMessage("user", content); }
    public static ChatMessage assistant(String content) { return new ChatMessage("assistant", content); }
}
