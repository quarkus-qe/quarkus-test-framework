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
    public static final String PACKAGE_TYPE_NAME = "quarkus.package.type";
    public static final String MUTABLE_JAR = "mutable-jar";
    public static final PropertyLookup PACKAGE_TYPE = new PropertyLookup(PACKAGE_TYPE_NAME);
    public static final List<String> PACKAGE_TYPE_NATIVE_VALUES = Arrays.asList("native", "native-sources");
    public static final List<String> PACKAGE_TYPE_LEGACY_JAR_VALUES = Arrays.asList("legacy-jar", "uber-jar", "mutable-jar");
    public static final List<String> PACKAGE_TYPE_JVM_VALUES = Arrays.asList("fast-jar", "jar");
    public static final PropertyLookup QUARKUS_JVM_S2I = new PropertyLookup("quarkus.s2i.base-jvm-image",
            "registry.access.redhat.com/ubi8/openjdk-11:latest");
    public static final PropertyLookup QUARKUS_NATIVE_S2I = new PropertyLookup("quarkus.s2i.base-native-image",
            "quay.io/quarkus/ubi-quarkus-native-binary-s2i:1.0");

    private QuarkusProperties() {

    }

    public static String getVersion() {
        String version = PLATFORM_VERSION.get();
        if (StringUtils.isEmpty(version)) {
            version = Version.getVersion();
        }

        return version;
    }

    public static boolean isNativePackageType() {
        return PACKAGE_TYPE_NATIVE_VALUES.contains(PACKAGE_TYPE.get());
    }

    public static boolean isNativePackageType(ServiceContext context) {
        return PACKAGE_TYPE_NATIVE_VALUES.contains(PACKAGE_TYPE.get(context));
    }

    public static boolean isLegacyJarPackageType(ServiceContext context) {
        return PACKAGE_TYPE_LEGACY_JAR_VALUES.contains(PACKAGE_TYPE.get(context));
    }

    public static boolean isJvmPackageType(ServiceContext context) {
        return PACKAGE_TYPE_JVM_VALUES.contains(PACKAGE_TYPE.get(context));
    }
}
