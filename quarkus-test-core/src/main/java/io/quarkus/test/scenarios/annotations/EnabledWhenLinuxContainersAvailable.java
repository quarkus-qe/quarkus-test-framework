package io.quarkus.test.scenarios.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Provides option to explicitly enable test only when Linux containers are available.
 * The framework automatically detects when Linux containers are needed and only run tests
 * when environment supports them. However, you may need to annotate tests with this annotation
 * when our framework is not used (like when the QuarkusTest annotation is used).
 */
@Inherited
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(EnabledWhenLinuxContainersAvailableCondition.class)
public @interface EnabledWhenLinuxContainersAvailable {
    /**
     * Why is the annotated test class or test method disabled.
     */
    String reason() default "";
}
