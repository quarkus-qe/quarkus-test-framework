package io.quarkus.test.utils;

import io.quarkus.builder.Version;
import io.quarkus.test.configuration.PropertyLookup;

public final class TestExecutionProperties {

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
        versionNumber = new PropertyLookup("version-number", Version.getVersion()).get();
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
}
