package io.quarkus.test.scenarios.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The {@code @DisabledOnQuarkus} annotation can be used to selectively enable or disable certain tests
 * based on version of Quarkus used in the test suite.
 *
 * @see #reason()
 */
@Inherited
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DisabledOnQuarkusSnapshotCondition.class)
public @interface DisabledOnQuarkusSnapshot {

    /**
     * Why is the annotated test class or test method disabled.
     */
    String reason();
}
