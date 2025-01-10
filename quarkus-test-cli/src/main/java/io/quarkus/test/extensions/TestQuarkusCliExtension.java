package io.quarkus.test.extensions;

import static io.quarkus.test.extensions.TestQuarkusCliTemplateContext.QuarkusVersion.fixedVersion;
import static io.quarkus.test.extensions.TestQuarkusCliTemplateContext.QuarkusVersion.latestReleased;
import static io.quarkus.test.extensions.TestQuarkusCliTemplateContext.QuarkusVersion.snapshot;
import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;

import io.quarkus.test.configuration.PropertyLookup;
import io.quarkus.test.extensions.TestQuarkusCliTemplateContext.QuarkusVersion;
import io.quarkus.test.scenarios.TestQuarkusCli;
import io.quarkus.test.services.quarkus.model.QuarkusProperties;

/**
 * Supports testing snapshot (999-SNAPSHOT, ...) Quarkus CLI with both released and snapshot Quarkus version.
 * Allows you to test creating of applications with 999-SNAPSHOT and xyz Quarkus versions using one test method.
 * Exactly like parametrized tests, but without need to declare argument source explicitly.
 */
public class TestQuarkusCliExtension implements TestTemplateInvocationContextProvider {

    private static final String QUARKUS_UPSTREAM_VERSION = "999-SNAPSHOT";
    private static final PropertyLookup RUN_WITH_LATEST_RELEASED = new PropertyLookup("cli.snapshot.test-released-quarkus",
            "true");

    @Override
    public boolean supportsTestTemplate(ExtensionContext context) {
        if (context.getTestMethod().isEmpty()) {
            return false;
        }

        Method templateMethod = context.getTestMethod().get();
        Optional<TestQuarkusCli> annotation = findAnnotation(templateMethod, TestQuarkusCli.class);
        return annotation.isPresent();
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext extensionContext) {
        return getTestedQuarkusVersions().map(TestQuarkusCliTemplateContext::new);
    }

    private static Stream<QuarkusVersion> getTestedQuarkusVersions() {
        String quarkusVersion = QuarkusProperties.getVersion();

        final Stream<QuarkusVersion> testedQuarkusVersions;
        if (isUpstream(quarkusVersion)) {
            if (RUN_WITH_LATEST_RELEASED.getAsBoolean()) {
                testedQuarkusVersions = Stream.of(snapshot(quarkusVersion), latestReleased());
            } else {
                testedQuarkusVersions = Stream.of(snapshot(quarkusVersion));
            }
        } else {
            testedQuarkusVersions = Stream.of(fixedVersion(quarkusVersion));
        }

        return testedQuarkusVersions;
    }

    private static boolean isUpstream(String version) {
        return version.contains(QUARKUS_UPSTREAM_VERSION);
    }
}
