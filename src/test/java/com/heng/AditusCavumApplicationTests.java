package com.heng;

import com.heng.aditus.config.AditusProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class AditusCavumApplicationTests {

    @Autowired
    private AditusProperties properties;

    @Autowired
    private Environment environment;

    @Test
    void contextLoads() {
    }

    @Test
    void consumerYamlOverridesLibraryDefaults() {
        assertEquals("consumer-application", environment.getProperty("spring.application.name"));
        assertEquals("https://consumer.example/v1", properties.getModel().getBaseUrl());
        assertEquals("consumer-test-key", properties.getModel().getApiKey());
        assertEquals("consumer-model", properties.getModel().getModel());
        assertEquals(2, properties.getChat().getMaxToolRounds());
        assertEquals(6, properties.getChat().getMaxMemoryMessages());
        assertEquals("Consumer-owned system prompt.", properties.getChat().getSystemPrompt());
    }
}
