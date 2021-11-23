package io.quarkus.test.services;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.test.bootstrap.ManagedResourceBuilder;
import io.quarkus.test.services.containers.JaegerContainerManagedResourceBuilder;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JaegerContainer {
    String image() default "quay.io/jaegertracing/all-in-one:1.21.0";

    int tracePort() default 16686;

    int restPort() default 14268;

    String expectedLog() default "server started";

    Class<? extends ManagedResourceBuilder> builder() default JaegerContainerManagedResourceBuilder.class;
}
