package io.quarkus.test.services;

import static io.quarkus.test.services.containers.DockerContainersNetwork.NetworkType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.test.bootstrap.ManagedResourceBuilder;
import io.quarkus.test.services.containers.ContainerManagedResourceBuilder;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Container {

    String DEFAULT_NETWORK_ID = "NONE";

    String image();

    int port();

    String expectedLog() default "";

    String[] command() default {};

    String[] networkAlias() default {};

    NetworkType networkType() default NetworkType.NEW;

    String networkId() default DEFAULT_NETWORK_ID;

    Class<? extends ManagedResourceBuilder> builder() default ContainerManagedResourceBuilder.class;
}
