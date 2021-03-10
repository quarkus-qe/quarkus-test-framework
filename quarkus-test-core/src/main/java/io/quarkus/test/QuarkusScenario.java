package io.quarkus.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

@Target(ElementType.TYPE)
@ExtendWith(QuarkusScenarioBootstrap.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface QuarkusScenario {

}
