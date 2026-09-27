package com.heng.aditus.web;

/** JSON response returned by the built-in blocking chat endpoint. */
public record AditusChatResponse(String conversationId, String answer) { }
