package com.heng.aditus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heng.aditus.annotation.Need;
import com.heng.aditus.tool.AditusTool;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AditusToolTests {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void createsSchemaAndInvokesBeanArgument() throws Exception {
        Method method = Tools.class.getDeclaredMethod("save", User.class);
        AditusTool tool = new AditusTool(new Tools(), method, mapper);

        JsonNode schema = mapper.valueToTree(tool.asOpenAiTool());
        assertEquals("save", schema.at("/function/name").asText());
        assertEquals("object", schema.at("/function/parameters/properties/user/type").asText());
        assertTrue(schema.at("/function/parameters/properties/user/properties/name").isObject());

        Object result = tool.invoke(mapper.readTree("{\"user\":{\"name\":\"张三\"}}"), mapper);
        assertEquals("张三", result);
    }

    static class Tools {
        @Need("保存用户信息")
        String save(User user) {
            return user.name;
        }
    }

    static class User {
        public String name;
    }
}
