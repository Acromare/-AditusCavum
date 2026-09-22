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

/**
 * Collects tool methods from Spring beans for lookup by the model adapter.
 * <p>Inspects implementation classes and directly implemented interfaces marked with
 * {@link MarkTheRuins}, registering their declared methods annotated with {@link Need}.
 * Tool names must be globally unique within this registry. Compile business code with
 * {@code -parameters} to expose meaningful parameter names to the model.
 */
public class ToolRegistry {
    private final Map<String, AditusTool> tools = new LinkedHashMap<>();

    /**
     * Discovers and wraps tools; discovery may initialize application beans.
     * @param context the Spring context containing business beans
     * @param mapper the JSON mapper supplied to tool wrappers
     * @throws IllegalStateException if tool names are duplicated
     */
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

    /**
     * Returns the tools used to build model request descriptions.
     * @return a registry collection view that callers should not modify
     */
    public Collection<AditusTool> all() { return tools.values(); }
    /**
     * Looks up a tool by the function name returned by the model.
     * @param name the Java method name
     * @return the registered tool, or null if unknown
     */
    public AditusTool get(String name) { return tools.get(name); }
}
