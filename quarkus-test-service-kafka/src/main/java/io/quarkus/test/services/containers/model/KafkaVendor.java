package io.quarkus.test.services.containers.model;

public enum KafkaVendor {
    CONFLUENT("confluentinc/cp-kafka", "7.5.3", 9093, KafkaRegistry.CONFLUENT),
    STRIMZI("quay.io/strimzi/kafka", "0.39.0-kafka-3.6.1", 9092, KafkaRegistry.APICURIO);

    private final String image;
    private final String defaultVersion;
    private final int port;
    private final KafkaRegistry registry;

    KafkaVendor(String image, String defaultVersion, int port, KafkaRegistry registry) {
        this.image = image;
        this.defaultVersion = defaultVersion;
        this.port = port;
        this.registry = registry;
    }

    public String getImage() {
        return image;
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
