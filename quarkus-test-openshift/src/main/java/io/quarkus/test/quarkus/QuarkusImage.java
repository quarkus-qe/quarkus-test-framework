package io.quarkus.test.quarkus;

import org.apache.commons.lang3.StringUtils;

import io.quarkus.test.ServiceContext;

enum QuarkusImage {
    UBI_QUARKUS_JVM_S2I("quarkus.s2i.base-jvm-image", "registry.access.redhat.com/ubi8/openjdk-11", "latest"),
    UBI_QUARKUS_NATIVE_S2I("quarkus.s2i.base-native-image", "quay.io/quarkus/ubi-quarkus-native-binary-s2i", "1.0");

    private final String propertyKey;
    private final String defaultValue;
    private final String version;

    QuarkusImage(String propertyKey, String defaultValue, String version) {
        this.propertyKey = propertyKey;
        this.defaultValue = defaultValue;
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    /**
     * Try to get the value from the Service context, then from system property. If not provided, it will use the default value.
     *
     * @return
     */
    public String get(ServiceContext service) {
        String value = service.get(propertyKey);
        if (StringUtils.isNotBlank(value)) {
            return value;
        }

        value = System.getProperty(propertyKey);
        if (StringUtils.isNotBlank(value)) {
            return value;
        }

        return defaultValue;
    }
}
