package com.heng.aditus.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heng.aditus.annotation.MarkTheRuins;
import com.heng.aditus.annotation.Need;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class ToolRegistry {
    private final Map<String, AditusTool> tools = new LinkedHashMap<>();

    public ToolRegistry(ApplicationContext context, ObjectMapper mapper) {
        for (Object bean : context.getBeansOfType(Object.class).values()) {
            Class<?> type = AopUtils.getTargetClass(bean);
            Set<Method> candidateMethods = new HashSet<>();
            boolean marked = type.isAnnotationPresent(MarkTheRuins.class);
            if (marked) candidateMethods.addAll(Arrays.asList(type.getDeclaredMethods()));
            for (Class<?> contract : type.getInterfaces()) {
                if (!contract.isAnnotationPresent(MarkTheRuins.class)) continue;
                marked = true;
                candidateMethods.addAll(Arrays.asList(contract.getDeclaredMethods()));
            }
            if (!marked) continue;
            for (Method method : candidateMethods) {
                if (!method.isAnnotationPresent(Need.class)) continue;
                AditusTool tool = new AditusTool(bean, method, mapper);
                if (tools.putIfAbsent(tool.name(), tool) != null) {
                    throw new IllegalStateException("Duplicate Aditus tool name: " + tool.name());
                }
            }
        }
    }

    public Collection<AditusTool> all() { return tools.values(); }
    public AditusTool get(String name) { return tools.get(name); }
}
