package com.heng.aditus.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heng.aditus.annotation.Need;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Wraps a Java tool method with a model-readable description and JSON-to-Java argument conversion.
 * <p>The generated schema describes basic parameter structure; this is not a complete JSON Schema
 * validator and does not automatically run Bean Validation. Business methods must enforce rules
 * such as phone format, authorization, and duplicate registration checks.
 */
public final class AditusTool {
    private final Object target;
    private final Method method;
    private final String name;
    private final String description;
    private final Map<String, Object> parametersSchema;

    /**
     * Builds a description from the tool annotation and reflected parameters.
     * @param target the business bean receiving invocations
     * @param method the tool method annotated with {@link Need}
     * @param mapper the JSON mapper; current basic schema generation does not inspect its settings
     */
    public AditusTool(Object target, Method method, ObjectMapper mapper) {
        this.target = target;
        this.method = method;
        this.method.setAccessible(true);
        this.name = method.getName();
        this.description = method.getAnnotation(Need.class).value();
        this.parametersSchema = buildSchema(mapper);
    }

    /** @return the Java method name used as the tool name */
    public String name() { return name; }
    /** @return the natural-language description supplied to the model */
    public String description() { return description; }
    /** @return the basic parameter schema, which callers should not modify */
    public Map<String, Object> parametersSchema() { return parametersSchema; }

    /**
     * Converts arguments and invokes the business method, propagating its original Exception.
     * <p>Values are normally resolved by Java parameter name, with an {@code arg0}-style fallback.
     * A single object parameter accepts either a wrapper named after the parameter or the object's
     * fields directly. Conversion does not replace business validation.
     * @param arguments the non-null JSON argument node produced by the model
     * @param mapper the mapper used to convert JSON to Java parameter types
     * @return the business result, later serialized and sent back to the model
     * @throws Exception if conversion, reflection, or business execution fails
     */
    public Object invoke(JsonNode arguments, ObjectMapper mapper) throws Exception {
        Parameter[] parameters = method.getParameters();
        Object[] values = new Object[parameters.length];
        if (parameters.length == 1 && !arguments.isObject()) {
            values[0] = mapper.treeToValue(arguments, parameters[0].getType());
        } else if (parameters.length == 1 && !isSimple(parameters[0].getType())) {
            JsonNode value = arguments.get(parameters[0].getName());
            values[0] = mapper.treeToValue(value == null ? arguments : value, parameters[0].getType());
        } else {
            for (int i = 0; i < parameters.length; i++) {
                String parameterName = parameters[i].getName();
                JsonNode value = arguments == null ? null : arguments.get(parameterName);
                if (value == null && arguments != null) {
                    value = arguments.get("arg" + i);
                }
                values[i] = value == null || value.isNull()
                        ? null : mapper.treeToValue(value, parameters[i].getType());
            }
        }
        try {
            return method.invoke(target, values);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof Exception e) throw e;
            throw exception;
        }
    }

    /**
     * Exports the tool in OpenAI function tool format.
     * @return a map containing the type, function name, description, and parameter schema
     */
    public Map<String, Object> asOpenAiTool() {
        Map<String, Object> function = new LinkedHashMap<>();
        function.put("name", name);
        function.put("description", description);
        function.put("parameters", parametersSchema);
        return Map.of("type", "function", "function", function);
    }

    /** Builds the parameter schema; current rules mark non-primitive method parameters as required. */
    private Map<String, Object> buildSchema(ObjectMapper mapper) {
        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();
        for (Parameter parameter : method.getParameters()) {
            properties.put(parameter.getName(), schemaFor(parameter.getType()));
            if (!parameter.getType().isPrimitive()) required.add(parameter.getName());
        }
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        if (!required.isEmpty()) schema.put("required", required);
        schema.put("additionalProperties", false);
        return schema;
    }

    private static boolean isSimple(Class<?> type) {
        return type.isPrimitive() || type == String.class || Number.class.isAssignableFrom(type)
                || type == Boolean.class || type.isEnum();
    }

    private static String jsonType(Class<?> type) {
        if (type == boolean.class || type == Boolean.class) return "boolean";
        if (Number.class.isAssignableFrom(type) || type.isPrimitive() && type != boolean.class && type != char.class) return "number";
        if (type.isArray() || Iterable.class.isAssignableFrom(type)) return "array";
        if (type == char.class || type == Character.class || type == String.class || type.isEnum()) return "string";
        return "object";
    }

    /**
     * Expands one level of declared object fields, skipping static and synthetic fields.
     * Nested objects are not expanded recursively; arrays and collections only declare their type.
     * Primitive fields are marked as required.
     */
    private static Map<String, Object> schemaFor(Class<?> type) {
        if (isSimple(type) || type.isPrimitive() || type.isArray() || Iterable.class.isAssignableFrom(type)) {
            return Map.of("type", jsonType(type));
        }
        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();
        for (Field field : type.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) continue;
            properties.put(field.getName(), Map.of("type", jsonType(field.getType())));
            if (field.getType().isPrimitive()) required.add(field.getName());
        }
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        if (!required.isEmpty()) schema.put("required", required);
        return schema;
    }
}
