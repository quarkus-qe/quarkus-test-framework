package io.quarkus.test.services;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.test.bootstrap.ManagedResourceBuilder;
import io.quarkus.test.services.containers.KeycloakContainerManagedResourceBuilder;

/**
 * KeycloakContainer annotation is supported since Keycloak 18.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface KeycloakContainer {
    String image() default "quay.io/keycloak/keycloak:26.0";

    int port() default 8080;

    String expectedLog() default "started in";

    String[] command() default {};

    long memoryLimitMiB() default 1000;

    Class<? extends ManagedResourceBuilder> builder() default KeycloakContainerManagedResourceBuilder.class;
}
