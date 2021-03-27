package io.quarkus.test.services;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.test.bootstrap.ManagedResourceBuilder;
import io.quarkus.test.services.quarkus.QuarkusApplicationManagedResourceBuilder;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface QuarkusApplication {
    // By default, it will load all the classes in the classpath.
    Class<?>[] classes() default {};

    Class<? extends ManagedResourceBuilder> builder() default QuarkusApplicationManagedResourceBuilder.class;
}
