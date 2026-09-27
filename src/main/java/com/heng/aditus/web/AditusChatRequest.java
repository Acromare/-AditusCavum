package com.heng.aditus.web;

/** JSON request accepted by the built-in chat endpoint. */
public record AditusChatRequest(String conversationId, String message) { }
