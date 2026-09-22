package com.heng.aditus.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds model and conversation settings from the application's {@code aditus-cavum} configuration.
 * <p>Examples include {@code aditus-cavum.model.api-key} and {@code aditus-cavum.chat.system-prompt}.
 * Applications supply their own credentials; the framework does not bundle application keys.
 * The client scanner reads {@code aditus-cavum.client.base-packages} separately.
 */
@ConfigurationProperties(prefix = "aditus-cavum")
public class AditusProperties {
    private final Model model = new Model();
    private final Chat chat = new Chat();

    public Model getModel() { return model; }
    public Chat getChat() { return chat; }

    /** Connection settings for the OpenAI-compatible API under {@code aditus-cavum.model}. */
    public static class Model {
        /** API base URL; requests append {@code /chat/completions}. */
        private String baseUrl = "https://api.openai.com/v1";
        /** Provider credential supplied through configuration or environment variables; do not log it. */
        private String apiKey;
        /** Model name supported by the provider. */
        private String model = "gpt-4o-mini";
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
    }

    /** Conversation settings under {@code aditus-cavum.chat}. */
    public static class Chat {
        /** Maximum tool execution rounds per request; defaults to 5, while 0 prevents execution. */
        private int maxToolRounds = 5;
        /** Retained messages per conversation: default 20, minimum 2; not a turn count or reset threshold. */
        private int maxMemoryMessages = 20;
        /** System prompt added by the assistant on each request; omitted when blank. */
        private String systemPrompt = "";
        public int getMaxToolRounds() { return maxToolRounds; }
        public void setMaxToolRounds(int maxToolRounds) { this.maxToolRounds = maxToolRounds; }
        public int getMaxMemoryMessages() { return maxMemoryMessages; }
        public void setMaxMemoryMessages(int maxMemoryMessages) { this.maxMemoryMessages = maxMemoryMessages; }
        public String getSystemPrompt() { return systemPrompt; }
        public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
    }
}
