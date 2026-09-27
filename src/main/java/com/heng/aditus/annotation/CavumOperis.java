package com.heng.aditus.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Marks a Spring bean method as an operation that the model may call. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CavumOperis {
    /** Natural-language description shown to the model. */
    String value();
}
