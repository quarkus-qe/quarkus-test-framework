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

    String expectedLog() default "";

    String[] command() default {};

    Class<? extends ManagedResourceBuilder> builder() default ContainerManagedResourceBuilder.class;
}
