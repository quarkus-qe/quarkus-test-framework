package io.quarkus.test.services;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GitRepositoryQuarkusApplication {
    String repo();

    String branch() default "";

    String contextDir() default "";

    String mavenArgs() default "-DskipTests=true -DskipITs=true";
}
