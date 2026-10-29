package io.quarkus.test.scenarios.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Disables the annotated test class or test method when running in a FIPS enabled environment
 * on a Java runtime whose feature version falls into the {@code [min, max]} range (both inclusive).
 *
 * Useful when a failure is specific to FIPS and only happens on some JDKs, for example
 * {@code @DisabledOnFipsAndJavaVersion(max = 24, reason = "...")} disables the test on FIPS for every
 * Java version older than 25.
 */
@Inherited
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DisabledOnFipsAndJavaVersionCondition.class)
public @interface DisabledOnFipsAndJavaVersion {

    int min() default 0;

    int max() default Integer.MAX_VALUE;

    String reason();
}
