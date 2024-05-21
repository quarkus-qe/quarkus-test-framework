package io.quarkus.test.services.quarkus.model;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import io.quarkus.builder.Version;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.configuration.PropertyLookup;

public final class QuarkusProperties {

    public static final PropertyLookup PLATFORM_GROUP_ID = new PropertyLookup("quarkus.platform.group-id", "io.quarkus");
    public static final PropertyLookup PLATFORM_VERSION = new PropertyLookup("quarkus.platform.version");
    public static final PropertyLookup PLUGIN_VERSION = new PropertyLookup("quarkus-plugin.version");
    public static final PropertyLookup NATIVE_ENABLED = new PropertyLookup("quarkus.native.enabled");
    public static final String QUARKUS_ANALYTICS_DISABLED_LOCAL_PROP_KEY = "quarkus.analytics.disabled";
    public static final PropertyLookup QUARKUS_ANALYTICS_DISABLED_LOCAL_PROP = new PropertyLookup(
            QUARKUS_ANALYTICS_DISABLED_LOCAL_PROP_KEY, "true");
    public static final String PACKAGE_TYPE_NAME = "quarkus.package.jar.type";
    public static final String MUTABLE_JAR = "mutable-jar";
    public static final PropertyLookup PACKAGE_TYPE = new PropertyLookup(PACKAGE_TYPE_NAME);
    public static final List<String> PACKAGE_TYPE_LEGACY_JAR_VALUES = Arrays.asList("legacy-jar", "uber-jar", "mutable-jar");
    public static final List<String> PACKAGE_TYPE_JVM_VALUES = Arrays.asList("fast-jar", "jar");
    public static final PropertyLookup QUARKUS_JVM_S2I = new PropertyLookup("quarkus.s2i.base-jvm-image",
            "registry.access.redhat.com/ubi8/openjdk-17:latest");
    public static final PropertyLookup QUARKUS_NATIVE_S2I = new PropertyLookup("quarkus.s2i.base-native-image",
            "quay.io/quarkus/ubi-quarkus-native-binary-s2i:2.0");

    private QuarkusProperties() {

    }

    public static String getVersion() {
        return defaultVersionIfEmpty(PLATFORM_VERSION.get());
    }

    public static String getPluginVersion() {
        return defaultVersionIfEmpty(PLUGIN_VERSION.get());
    }

    public static boolean disableBuildAnalytics() {
        return QUARKUS_ANALYTICS_DISABLED_LOCAL_PROP.getAsBoolean();
    }

    public static boolean isNativeEnabled() {
        return Boolean.parseBoolean(NATIVE_ENABLED.get());
    }

    public static boolean isNativeEnabled(ServiceContext context) {
        return Boolean.parseBoolean(NATIVE_ENABLED.get(context));
    }

    public static boolean isLegacyJarPackageType(ServiceContext context) {
        return !isNativeEnabled() && PACKAGE_TYPE_LEGACY_JAR_VALUES.contains(PACKAGE_TYPE.get(context));
    }

    public static boolean isJvmPackageType(ServiceContext context) {
        return !isNativeEnabled() && PACKAGE_TYPE_JVM_VALUES.contains(PACKAGE_TYPE.get(context));
    }

    private static String defaultVersionIfEmpty(String version) {
        if (StringUtils.isEmpty(version)) {
            version = Version.getVersion();
        }

        return version;
    }
}
