package io.quarkus.test.extensions;

import java.util.List;

import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;

import io.quarkus.test.bootstrap.QuarkusCliClient;
import io.quarkus.test.bootstrap.QuarkusVersionAwareCliClient;
import io.quarkus.test.bootstrap.QuarkusVersionAwareCliClient.SetCliPlatformVersionMode;
import io.quarkus.test.bootstrap.ScenarioContext;

/**
 * Basically represents a Quarkus CLI test instance (method invocation).
 * Also allows optionally inject tested Quarkus version as a method parameter.
 */
final class TestQuarkusCliTemplateContext implements TestTemplateInvocationContext, ParameterResolver {

    record QuarkusVersion(String versionName, SetCliPlatformVersionMode mode) {

        static QuarkusVersion snapshot(String quarkusVersion) {
            return new QuarkusVersion(quarkusVersion, SetCliPlatformVersionMode.SNAPSHOT);
        }

        static QuarkusVersion latestReleased() {
            return new QuarkusVersion(null, SetCliPlatformVersionMode.NO_VERSION);
        }

        static QuarkusVersion fixedVersion(String quarkusVersion) {
            return new QuarkusVersion(quarkusVersion, SetCliPlatformVersionMode.FIXED_VERSION);
        }
    }

    private final String quarkusVersion;
    private final SetCliPlatformVersionMode mode;

    TestQuarkusCliTemplateContext(QuarkusVersion quarkusVersion) {
        this.quarkusVersion = quarkusVersion.versionName;
        this.mode = quarkusVersion.mode;
    }

    @Override
    public String getDisplayName(int invocationIndex) {
        return (quarkusVersion == null ? "" : quarkusVersion) + mode.getQuarkusVersionPostfix();
    }

    @Override
    public List<Extension> getAdditionalExtensions() {
        return List.of(this);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        var paramType = parameterContext.getParameter().getType();
        return paramType == String.class || paramType == QuarkusCliClient.class
                || paramType == QuarkusVersionAwareCliClient.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        var paramType = parameterContext.getParameter().getType();
        if (paramType == QuarkusCliClient.class) {
            return getQuarkusCliClient(extensionContext);
        } else if (paramType == QuarkusVersionAwareCliClient.class) {
            QuarkusCliClient cliClient = getQuarkusCliClient(extensionContext);
            return new QuarkusVersionAwareCliClient(cliClient, mode, quarkusVersion);
        } else if (paramType == String.class) {
            return quarkusVersion;
        } else {
            throw new ParameterResolutionException("Unsupported parameter type: " + paramType);
        }
    }

    private static QuarkusCliClient getQuarkusCliClient(ExtensionContext extensionContext) {
        var testNamespace = ExtensionContext.Namespace.create(ScenarioContext.class);
        var scenarioContextStore = extensionContext.getStore(testNamespace);
        return (QuarkusCliClient) scenarioContextStore.get(QuarkusCliClient.class.getName());
    }

}
