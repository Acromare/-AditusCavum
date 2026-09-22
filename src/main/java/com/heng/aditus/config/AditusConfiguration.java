package com.heng.aditus.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heng.aditus.AditusAssistant;
import com.heng.aditus.model.ChatModel;
import com.heng.aditus.model.OpenAiCompatibleChatModel;
import com.heng.aditus.tool.ToolRegistry;
import com.heng.aditus.memory.ChatMemory;
import com.heng.aditus.memory.InMemoryChatMemory;

@AutoConfiguration
@EnableConfigurationProperties(AditusProperties.class)
public class AditusConfiguration {
    @Bean
    static AditusClientScanner aditusClientScanner() {
        return new AditusClientScanner();
    }

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper aditusObjectMapper() {
        return new ObjectMapper();
    }

    @Bean
    @ConditionalOnMissingBean
    public ToolRegistry aditusToolRegistry(org.springframework.context.ApplicationContext context,
                                           ObjectMapper mapper) {
        return new ToolRegistry(context, mapper);
    }

    @Bean
    @ConditionalOnMissingBean(ChatModel.class)
    public ChatModel aditusChatModel(ObjectMapper mapper, ToolRegistry registry,
                                     AditusProperties properties) {
        return new OpenAiCompatibleChatModel(mapper, registry, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public AditusAssistant aditusAssistant(ChatModel model, ChatMemory memory,
                                           AditusProperties properties) {
        return new AditusAssistant(model, memory, properties);
    }

    @Bean
    @ConditionalOnMissingBean(ChatMemory.class)
    public ChatMemory aditusChatMemory(AditusProperties properties) {
        return new InMemoryChatMemory(properties.getChat().getMaxMemoryMessages());
    }
}
