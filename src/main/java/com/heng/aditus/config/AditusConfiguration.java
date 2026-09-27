package com.heng.aditus.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heng.aditus.AditusAssistant;
import com.heng.aditus.model.ChatModel;
import com.heng.aditus.model.OpenAiCompatibleChatModel;
import com.heng.aditus.tool.ToolRegistry;
import com.heng.aditus.memory.ChatMemory;
import com.heng.aditus.memory.InMemoryChatMemory;
import com.heng.aditus.rag.InMemoryKnowledgeRetriever;
import com.heng.aditus.rag.KnowledgeDocument;
import com.heng.aditus.rag.KnowledgeLoader;
import com.heng.aditus.rag.KnowledgeRetriever;
import com.heng.aditus.annotation.AditusSylva;
import com.heng.aditus.web.AditusController;
import com.heng.aditus.web.AditusConsoleController;

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

    @Autowired
    void applyEnvironment(AditusProperties properties, Environment environment) {
        properties.applyEnvironment(environment);
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
                                           AditusProperties properties, KnowledgeRetriever knowledgeRetriever) {
        return new AditusAssistant(model, memory, properties, knowledgeRetriever);
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

    @Bean
    @ConditionalOnMissingBean
    public KnowledgeRetriever aditusKnowledgeRetriever(org.springframework.context.ApplicationContext context) {
        AditusSylva sylva = findSylva(context);
        if (sylva == null) return new InMemoryKnowledgeRetriever(java.util.List.of());
        java.util.List<KnowledgeDocument> documents = KnowledgeLoader.load(sylva.value(), sylva.chunkSize());
        return new InMemoryKnowledgeRetriever(documents);
    }

    @Bean
    @ConditionalOnMissingBean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
    @ConditionalOnProperty(prefix = "aditus-cavum.web", name = "enabled", havingValue = "true", matchIfMissing = true)
    public AditusController aditusController(AditusAssistant assistant) {
        return new AditusController(assistant);
    }

    @Bean
    @ConditionalOnMissingBean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
    @ConditionalOnProperty(prefix = "aditus-cavum.web", name = "console-enabled", havingValue = "true", matchIfMissing = true)
    public AditusConsoleController aditusConsoleController(AditusProperties properties,
                                                             ToolRegistry tools,
                                                             KnowledgeRetriever knowledge) {
        return new AditusConsoleController(properties, tools, knowledge);
    }

    private static AditusSylva findSylva(org.springframework.context.ApplicationContext context) {
        for (String name : context.getBeanNamesForAnnotation(AditusSylva.class)) {
            AditusSylva annotation = context.findAnnotationOnBean(name, AditusSylva.class);
            if (annotation != null) return annotation;
        }
        return null;
    }
}
