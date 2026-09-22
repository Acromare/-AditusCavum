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

/**
 * Spring Boot auto-configuration for client scanning, tools, the model, memory, and the assistant.
 * <p>Loaded through {@code AutoConfiguration.imports}; applications do not need to scan framework packages.
 * Components guarded by {@code ConditionalOnMissingBean} can be replaced with application beans.
 */
@AutoConfiguration
@EnableConfigurationProperties(AditusProperties.class)
public class AditusConfiguration {
    /** Registers the scanner early; static avoids premature initialization of the configuration class. */
    @Bean
    static AditusClientScanner aditusClientScanner() {
        return new AditusClientScanner();
    }

    /**
     * Provides the Jackson 2 mapper used for the model protocol.
     * @return the default JSON mapper
     */
    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper aditusObjectMapper() {
        return new ObjectMapper();
    }

    /**
     * Collects annotated tool methods from Spring beans.
     * @param context the application context
     * @param mapper the JSON mapper
     * @return the registry indexed by method name
     */
    @Bean
    @ConditionalOnMissingBean
    public ToolRegistry aditusToolRegistry(org.springframework.context.ApplicationContext context,
                                           ObjectMapper mapper) {
        return new ToolRegistry(context, mapper);
    }

    /**
     * Uses the OpenAI-compatible protocol when no custom model bean is provided.
     * @param mapper the JSON mapper
     * @param registry the business tools available to the model
     * @param properties provider and conversation settings
     * @return the default model adapter
     */
    @Bean
    @ConditionalOnMissingBean(ChatModel.class)
    public ChatModel aditusChatModel(ObjectMapper mapper, ToolRegistry registry,
                                     AditusProperties properties) {
        return new OpenAiCompatibleChatModel(mapper, registry, properties);
    }

    /**
     * Assembles the assistant that applications can inject directly.
     * @param model the model adapter
     * @param memory the conversation store
     * @param properties conversation settings
     * @return the conversation assistant
     */
    @Bean
    @ConditionalOnMissingBean
    public AditusAssistant aditusAssistant(ChatModel model, ChatMemory memory,
                                           AditusProperties properties) {
        return new AditusAssistant(model, memory, properties);
    }

    /**
     * Uses process memory when no custom conversation store is provided.
     * @param properties settings containing the per-conversation message limit
     * @return the bounded in-memory store
     */
    @Bean
    @ConditionalOnMissingBean(ChatMemory.class)
    public ChatMemory aditusChatMemory(AditusProperties properties) {
        return new InMemoryChatMemory(properties.getChat().getMaxMemoryMessages());
    }
}
