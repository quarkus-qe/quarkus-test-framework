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
    String image() default "quay.io/keycloak/keycloak:26.1";

    int port() default 8080;

    int tlsPort() default 8443;

    String expectedLog() default "started in";

    String[] command() default { "start", "--import-realm", "--hostname-strict=false" };

    long memoryLimitMiB() default 1000;

    boolean runKeycloakInProdMode() default false;

    /**
     * Will expose Tls port on container or HTTPS route on OCP.
     * But will not generate certificates, test needs to manage certificates on their own.
     */
    boolean exposeRawTls() default false;

    Certificate.Format certificateFormat() default Certificate.Format.PKCS12;

    Class<? extends ManagedResourceBuilder> builder() default KeycloakContainerManagedResourceBuilder.class;
}
