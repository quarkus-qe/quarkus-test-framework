package io.quarkus.test.services;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.test.bootstrap.ManagedResourceBuilder;
import io.quarkus.test.services.containers.AmqContainerManagedResourceBuilder;
import io.quarkus.test.services.containers.model.AmqProtocol;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AmqContainer {

    String image() default "quay.io/artemiscloud/activemq-artemis-broker:1.0.25";

    String expectedLog() default "Artemis Console available";

    AmqProtocol protocol() default AmqProtocol.TCP;

    Class<? extends ManagedResourceBuilder> builder() default AmqContainerManagedResourceBuilder.class;
}
