package com.heng.aditus.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Describes a tool in natural language to help the model decide when to call the Java method.
 * <p>Requires {@link MarkTheRuins} on the declaring type. The method name becomes the tool name
 * and must be unique in the registry. Descriptions guide the model; the business method must
 * still implement rules such as phone validation, duplicate detection, and database writes.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Need {
    /**
     * Describes when to use the tool, what its parameters mean, and what it returns.
     * @return the natural-language description supplied to the model
     */
    String value();
}
