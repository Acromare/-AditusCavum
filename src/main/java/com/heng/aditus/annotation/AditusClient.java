package com.heng.aditus.annotation;

import java.lang.annotation.*;

/** Register a public interface extending AditusOperations as an injectable client. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AditusClient {
}
