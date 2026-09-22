package com.heng.aditus.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a type that provides model-callable tools, together with {@link Need} on its methods.
 * <p>Place it on a Spring bean's implementation class or a directly implemented interface.
 * Only declared methods annotated with {@code @Need} on marked types are registered.
 * This annotation does not create a Spring bean: register the implementation through
 * {@code @Service}, {@code @Component}, or {@code @Bean}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MarkTheRuins {
}
