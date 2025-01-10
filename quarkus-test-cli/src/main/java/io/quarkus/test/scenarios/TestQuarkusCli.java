package io.quarkus.test.scenarios;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.test.extensions.TestQuarkusCliExtension;

/**
 * Facilitates Quarkus CLI testing. When Quarkus snapshot is tested,
 * the annotated test method is called twice, once for the snapshot version,
 * once for the latest released version.
 * This behavior can be disabled by setting the 'ts.global.cli.snapshot.test-released-quarkus'
 * configuration property to 'false'.
 * With such option, the test method is only called once, so that we test the snapshot version.
 * There are multiple options how to set up Quarkus CLI test with this annotation, the easiest one
 * is to inject the QuarkusVersionAwareCliClient and let our framework deal with versions.
 * Other option is to inject Quarkus version as a {@link String} method argument and set up your tests manually.
 */
@Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@TestTemplate
@ExtendWith(TestQuarkusCliExtension.class)
public @interface TestQuarkusCli {
}
