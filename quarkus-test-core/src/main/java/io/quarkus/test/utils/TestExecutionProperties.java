package io.quarkus.test.utils;

import io.quarkus.test.bootstrap.Service;
import io.quarkus.test.configuration.PropertyLookup;
import io.quarkus.test.services.quarkus.model.QuarkusProperties;

public final class TestExecutionProperties {

    /**
     * Internal configuration property signalling that Management interface TLS support is enabled.
     * This is just a way to propagate the information within framework, users don't need to be concern with it.
     */
    public static final String MANAGEMENT_INTERFACE_ENABLED = "ts-internal.management.interface";
    private static final String DEFAULT_SERVICE_NAME = "quarkus_test_framework";
    private static final String DEFAULT_BUILD_NUMBER = "777-default";
    private static final TestExecutionProperties INSTANCE = new TestExecutionProperties();

    private final String serviceName;
    private final String buildNumber;
    private final String versionNumber;
    private final boolean openshift;
    private final boolean kubernetes;

    private TestExecutionProperties() {
        serviceName = new PropertyLookup("service-name", DEFAULT_SERVICE_NAME).get();
        buildNumber = new PropertyLookup("build.number", DEFAULT_BUILD_NUMBER).get();
        versionNumber = new PropertyLookup("version-number", QuarkusProperties.getVersion()).get();
        openshift = new PropertyLookup("openshift").getAsBoolean();
        kubernetes = new PropertyLookup("kubernetes").getAsBoolean();
    }

    public static String getServiceName() {
        return INSTANCE.serviceName;
    }

    public static String getVersionNumber() {
        return INSTANCE.versionNumber;
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

    public static String getBuildNumber() {
        return INSTANCE.buildNumber;
    }

    public static boolean useManagementSsl(Service service) {
        return service.getProperty(MANAGEMENT_INTERFACE_ENABLED).map(Boolean::parseBoolean).orElse(false);
    }
}
