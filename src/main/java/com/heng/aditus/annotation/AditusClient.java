package com.heng.aditus.annotation;

import java.lang.annotation.*;

/**
 * Registers a public, non-sealed interface extending {@link com.heng.aditus.AditusOperations}
 * as an injectable Spring client. The generated proxy forwards standard conversation methods,
 * so no implementation class is needed. For example:
 * <pre>{@code
 * @AditusClient
 * public interface ExamAi extends AditusOperations {
 * }
 * }</pre>
 * <p>Custom composition belongs in Java {@code default} methods; arbitrary abstract methods
 * are not implemented automatically. Scanning defaults to Spring Boot auto-configuration packages.
 * Set {@code aditus-cavum.client.base-packages} to a comma-separated list to replace that scope.
 * This annotation defines a client for calling the model. To expose business tools to the model,
 * use {@link MarkTheRuins} and {@link Need} instead.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AditusClient {
}
