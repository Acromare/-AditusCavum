package com.heng.aditus.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aditus-cavum")
public class AditusProperties {
    private final Model model = new Model();
    private final Chat chat = new Chat();

    public Model getModel() { return model; }
    public Chat getChat() { return chat; }

    public static class Model {
        private String baseUrl = "https://api.openai.com/v1";
        private String apiKey;
        private String model = "gpt-4o-mini";
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
    }

    public static class Chat {
        private int maxToolRounds = 5;
        private int maxMemoryMessages = 20;
        private String systemPrompt = "";
        public int getMaxToolRounds() { return maxToolRounds; }
        public void setMaxToolRounds(int maxToolRounds) { this.maxToolRounds = maxToolRounds; }
        public int getMaxMemoryMessages() { return maxMemoryMessages; }
        public void setMaxMemoryMessages(int maxMemoryMessages) { this.maxMemoryMessages = maxMemoryMessages; }
        public String getSystemPrompt() { return systemPrompt; }
        public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
    }
}
