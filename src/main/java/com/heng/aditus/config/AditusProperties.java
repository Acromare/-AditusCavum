package com.heng.aditus.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;

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
    private final Web web = new Web();

    public Model getModel() { return model; }
    public Chat getChat() { return chat; }
    public Web getWeb() { return web; }

    /**
     * Applies conventional environment variables when the corresponding nested
     * property was not supplied. Explicit Spring configuration always wins.
     */
    public void applyEnvironment(Environment environment) {
        model.baseUrl = first(environment, model.baseUrl, "ADITUS_BASE_URL", "ADITUS_MODEL_BASE_URL");
        model.apiKey = first(environment, model.apiKey, "ADITUS_API_KEY", "ADITUS_MODEL_API_KEY");
        model.model = first(environment, model.model, "ADITUS_MODEL", "ADITUS_MODEL_NAME");
        chat.systemPrompt = first(environment, chat.systemPrompt, "ADITUS_SYSTEM_PROMPT");
        chat.maxToolRounds = integer(environment, chat.maxToolRounds, "ADITUS_MAX_TOOL_ROUNDS");
        chat.maxMemoryMessages = integer(environment, chat.maxMemoryMessages, "ADITUS_MAX_MEMORY_MESSAGES");
    }

    private static String first(Environment environment, String current, String... names) {
        for (String name : names) {
            String value = environment.getProperty(name);
            if (value != null && !value.isBlank() && (current == null || current.isBlank()
                    || current.equals(defaultFor(name)))) return value;
        }
        return current;
    }

    private static int integer(Environment environment, int current, String name) {
        String value = environment.getProperty(name);
        if (value == null || value.isBlank()) return current;
        try { return Integer.parseInt(value); }
        catch (NumberFormatException ignored) { return current; }
    }

    private static String defaultFor(String name) {
        return switch (name) {
            case "ADITUS_BASE_URL", "ADITUS_MODEL_BASE_URL" -> "https://api.openai.com/v1";
            case "ADITUS_MODEL", "ADITUS_MODEL_NAME" -> "gpt-4o-mini";
            default -> "";
        };
    }

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
        /** Maximum retrieved knowledge chunks added to one request. */
        private int knowledgeTopK = 4;
        public int getMaxToolRounds() { return maxToolRounds; }
        public void setMaxToolRounds(int maxToolRounds) { this.maxToolRounds = maxToolRounds; }
        public int getMaxMemoryMessages() { return maxMemoryMessages; }
        public void setMaxMemoryMessages(int maxMemoryMessages) { this.maxMemoryMessages = maxMemoryMessages; }
        public String getSystemPrompt() { return systemPrompt; }
        public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
        public int getKnowledgeTopK() { return knowledgeTopK; }
        public void setKnowledgeTopK(int knowledgeTopK) { this.knowledgeTopK = knowledgeTopK; }
    }

    /** Built-in HTTP API and developer console settings. */
    public static class Web {
        private boolean enabled = true;
        private String path = "/aditus";
        private boolean consoleEnabled = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public boolean isConsoleEnabled() { return consoleEnabled; }
        public void setConsoleEnabled(boolean consoleEnabled) { this.consoleEnabled = consoleEnabled; }
    }
}
