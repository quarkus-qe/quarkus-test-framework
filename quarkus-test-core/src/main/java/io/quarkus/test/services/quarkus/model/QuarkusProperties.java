package io.quarkus.test.services.quarkus.model;

import static java.lang.String.format;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import io.quarkus.builder.Version;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.configuration.PropertyLookup;

public final class QuarkusProperties {

    public static final PropertyLookup USE_SEPARATE_GRPC_SERVER = new PropertyLookup("quarkus.grpc.server.use-separate-server",
            "true");
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
            "quay.io/jdowland/ubi9-openjdk-21:OPENJDK-3142");
    public static final PropertyLookup QUARKUS_NATIVE_S2I = new PropertyLookup("quarkus.s2i.base-native-image",
            "quay.io/quarkus/ubi9-quarkus-native-binary-s2i:2.0");

    private QuarkusProperties() {

    }

    public static boolean isRHBQ() {
        return QuarkusProperties.getVersion().contains("redhat");
    }

    public static String getVersion() {
        return defaultVersionIfEmpty(PLATFORM_VERSION.get());
    }

    public static String getPluginVersion() {
        return defaultVersionIfEmpty(PLUGIN_VERSION.get());
    }

    public static boolean disableBuildAnalytics(ServiceContext context) {
        if (context == null) {
            return disableBuildAnalytics();
        }
        return QUARKUS_ANALYTICS_DISABLED_LOCAL_PROP.getAsBoolean(context);
    }

    public static boolean disableBuildAnalytics() {
        return QUARKUS_ANALYTICS_DISABLED_LOCAL_PROP.getAsBoolean();
    }

    public static String createDisableBuildAnalyticsProperty() {
        return format("-D%s=%s", QUARKUS_ANALYTICS_DISABLED_LOCAL_PROP_KEY, Boolean.TRUE);
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

    public static boolean useSeparateGrpcServer(ServiceContext context) {
        return Boolean.parseBoolean(USE_SEPARATE_GRPC_SERVER.get(context));
    }

    private static String defaultVersionIfEmpty(String version) {
        if (StringUtils.isEmpty(version)) {
            version = Version.getVersion();
        }

        return version;
    }
}
