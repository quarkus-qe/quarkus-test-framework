package io.quarkus.test.services;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DevModeQuarkusApplication {
    // By default, it will load all the classes in the classpath.
    Class<?>[] classes() default {};
}
