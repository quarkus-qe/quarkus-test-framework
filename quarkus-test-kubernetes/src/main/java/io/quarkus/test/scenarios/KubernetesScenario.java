package io.quarkus.test.scenarios;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.test.bootstrap.QuarkusScenarioBootstrap;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(QuarkusScenarioBootstrap.class)
@Inherited
public @interface KubernetesScenario {
    KubernetesDeploymentStrategy deployment() default KubernetesDeploymentStrategy.UsingContainerRegistry;
}
