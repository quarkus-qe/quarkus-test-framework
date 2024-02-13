package io.quarkus.test.scenarios;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.test.bootstrap.QuarkusScenarioBootstrap;
import io.quarkus.test.listener.QuarkusTestExceptionFilter;
import io.quarkus.test.services.Operator;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(QuarkusScenarioBootstrap.class)
@ExtendWith(QuarkusTestExceptionFilter.class)
@Inherited
public @interface OpenShiftScenario {
    OpenShiftDeploymentStrategy deployment() default OpenShiftDeploymentStrategy.Build;

    Operator[] operators() default {};
}
