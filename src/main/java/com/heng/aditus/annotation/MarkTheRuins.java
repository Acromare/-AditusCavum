package com.heng.aditus.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Marks a type whose @Need methods are exposed to the model. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MarkTheRuins {
}
