package io.quarkus.test.services.containers.model;

public enum KafkaVendor {
    CONFLUENT("confluentinc/cp-kafka", "6.1.1"),
    STRIMZI("quay.io/strimzi/kafka", "0.22.1-kafka-2.5.0");

    private final String image;
    private final String defaultVersion;

    KafkaVendor(String image, String defaultVersion) {
        this.image = image;
        this.defaultVersion = defaultVersion;
    }

    public String getImage() {
        return image;
    }

    public String getDefaultVersion() {
        return defaultVersion;
    }
}
