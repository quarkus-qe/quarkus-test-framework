package io.quarkus.test.services.containers.model;

import static io.quarkus.test.utils.ImageUtil.getImageName;
import static io.quarkus.test.utils.ImageUtil.getImageVersion;

public enum KafkaVendor {
    CONFLUENT("kafka.vendor.confluent.image", 9093, KafkaRegistry.CONFLUENT),
    STRIMZI("kafka.vendor.strimzi.image", 9092, KafkaRegistry.APICURIO);

    private final String imageName;
    private final String defaultVersion;
    private final int port;
    private final KafkaRegistry registry;

    KafkaVendor(String imagePropertyName, int port, KafkaRegistry registry) {
        this.imageName = getImageName(imagePropertyName);
        this.defaultVersion = getImageVersion(imagePropertyName);
        this.port = port;
        this.registry = registry;
    }

    public String getImage() {
        return imageName;
    }

    public String getDefaultVersion() {
        return defaultVersion;
    }

    public int getPort() {
        return port;
    }

    public KafkaRegistry getRegistry() {
        return registry;
    }
}
