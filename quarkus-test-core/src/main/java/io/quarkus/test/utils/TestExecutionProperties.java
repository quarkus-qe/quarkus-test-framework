package io.quarkus.test.utils;

import static io.quarkus.test.configuration.Configuration.Property.CUSTOM_BUILD_REQUIRED;

import io.quarkus.test.bootstrap.Service;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.configuration.PropertyLookup;

public final class TestExecutionProperties {

    /**
     * Internal configuration property signalling that Management interface TLS support is enabled.
     * This is just a way to propagate the information within framework, users don't need to be concern with it.
     */
    public static final String MANAGEMENT_INTERFACE_ENABLED = "ts-internal.management.interface";
    private static final TestExecutionProperties INSTANCE = new TestExecutionProperties();
    private static final String CLI_APP_PROPERTY_KEY = "ts-internal.is-cli-app";
    private static final String APP_STARTED_KEY = "ts-internal.app-started";
    /**
     * You can enforce custom artifact build by setting this property for 'app' service with
     * this 'ts.app.custom-build.required=true' or for all the services within a test class
     * with 'ts.global.custom-build.required=true'.
     */
    private static final PropertyLookup REQUIRE_CUSTOM_BUILD = new PropertyLookup(CUSTOM_BUILD_REQUIRED.toString(), "false");

    private final boolean openshift;
    private final boolean kubernetes;

    private TestExecutionProperties() {
        openshift = new PropertyLookup("openshift").getAsBoolean();
        kubernetes = new PropertyLookup("kubernetes").getAsBoolean();
    }

    public static boolean isKubernetesPlatform() {
        return INSTANCE.kubernetes;
    }

    public static boolean isOpenshiftPlatform() {
        return INSTANCE.openshift;
    }

    public static boolean isBareMetalPlatform() {
        return !isKubernetesPlatform() && !isOpenshiftPlatform();
    }

    public static boolean useManagementSsl(Service service) {
        return service.getProperty(MANAGEMENT_INTERFACE_ENABLED).map(Boolean::parseBoolean).orElse(false);
    }

    public static void rememberThisIsCliApp(ServiceContext context) {
        context.put(CLI_APP_PROPERTY_KEY, Boolean.TRUE.toString());
    }

    public static boolean isThisCliApp(ServiceContext context) {
        return Boolean.parseBoolean(context.get(CLI_APP_PROPERTY_KEY));
    }

    public static boolean isThisStartedCliApp(ServiceContext context) {
        return isThisCliApp(context) && Boolean.parseBoolean(context.get(APP_STARTED_KEY));
    }

    public static void rememberThisAppStarted(ServiceContext context) {
        context.put(APP_STARTED_KEY, Boolean.TRUE.toString());
    }

    public static boolean isCustomBuildRequired(ServiceContext context) {
        return Boolean.parseBoolean(REQUIRE_CUSTOM_BUILD.get(context));
    }
}
