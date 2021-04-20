package io.quarkus.test.services;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.test.bootstrap.ManagedResourceBuilder;
import io.quarkus.test.services.containers.AmqContainerManagedResourceBuilder;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AmqContainer {

    String image() default "quay.io/artemiscloud/activemq-artemis-broker:0.1.2";

    String expectedLog() default "Artemis Console available";

    int port() default 61616;

    Class<? extends ManagedResourceBuilder> builder() default AmqContainerManagedResourceBuilder.class;
}
