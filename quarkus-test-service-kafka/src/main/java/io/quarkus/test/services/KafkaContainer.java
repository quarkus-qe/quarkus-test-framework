package io.quarkus.test.services;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.test.bootstrap.ManagedResourceBuilder;
import io.quarkus.test.services.containers.KafkaContainerManagedResourceBuilder;
import io.quarkus.test.services.containers.model.KafkaVendor;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface KafkaContainer {

    KafkaVendor vendor() default KafkaVendor.STRIMZI;

    String image() default "";

    String version() default "";

    boolean withRegistry() default false;

    String registryImage() default "";

    String registryPath() default "";

    Class<? extends ManagedResourceBuilder> builder() default KafkaContainerManagedResourceBuilder.class;
}
