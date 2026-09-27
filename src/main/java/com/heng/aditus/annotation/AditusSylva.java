package com.heng.aditus.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the application's knowledge tree for automatic retrieval-augmented context.
 * Place it on the Spring Boot application class or another configuration class.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AditusSylva {
    /** Classpath directory or file to index, for example {@code classpath:knowledge}. */
    String value() default "classpath:knowledge";

    /** Maximum characters in each indexed chunk. */
    int chunkSize() default 800;

    /** Number of chunks added to each request. */
    int topK() default 4;
}
