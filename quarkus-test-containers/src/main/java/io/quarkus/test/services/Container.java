package io.quarkus.test.services;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.test.bootstrap.ManagedResourceBuilder;
import io.quarkus.test.services.containers.ContainerManagedResourceBuilder;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Container {
    String image();

    int port();

    int tlsPort() default -1;

    boolean ssl() default false;

    String expectedLog() default "";

    String[] command() default {};

    /**
     * If true, forwards Docker ports from localhost to Docker host on Windows.
     * This works around issue when certificates are only generated for localhost.
     */
    boolean portDockerHostToLocalhost() default false;

    Class<? extends ManagedResourceBuilder> builder() default ContainerManagedResourceBuilder.class;

    Mount[] mounts() default {};
}
