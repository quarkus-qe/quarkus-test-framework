package io.quarkus.test.services.containers.model;

public enum KafkaVendor {
    CONFLUENT("confluentinc/cp-kafka", "8.3.0", 9092, KafkaRegistry.CONFLUENT),
    /**
     * Single Strimzi container started through {@code ExtendedStrimziKafkaContainer}.
     *
     * @deprecated use {@link #STRIMZI_CLUSTER} instead, once the setup you need is supported by it
     */
    @Deprecated
    STRIMZI(Strimzi.IMAGE, Strimzi.VERSION, 9092, KafkaRegistry.APICURIO),
    /**
     * Default vendor. Same image as {@link #STRIMZI}, but started through the upstream {@code StrimziKafkaCluster}.
     * Supports plain text, SSL, SASL (PLAIN) and SASL_SSL (SCRAM-SHA-512), with or without a schema registry. Custom
     * server properties and config resources are not supported so far.
     */
    STRIMZI_CLUSTER(Strimzi.IMAGE, Strimzi.VERSION, 9092, KafkaRegistry.APICURIO);

    private static final class Strimzi {
        // When updating strimzi kafka also update version in examples-kafka pom
        private static final String IMAGE = "quay.io/strimzi/kafka";
        private static final String VERSION = "1.1.0-kafka-4.3.0";
    }

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

    public boolean isStrimzi() {
        return this == STRIMZI || this == STRIMZI_CLUSTER;
    }
}
