package io.quarkus.test.services.containers.model;

public enum KafkaRegistry {
    CONFLUENT("docker.io/confluentinc/cp-schema-registry", "6.1.1", "/", 8081),
    APICURIO("quay.io/apicurio/apicurio-registry-mem", "2.2.5.Final", "/apis", 8080);

    private final String image;
    private final String defaultVersion;
    private final String path;
    private final int port;

    KafkaRegistry(String image, String defaultVersion, String path, int port) {
        this.image = image;
        this.defaultVersion = defaultVersion;
        this.path = path;
        this.port = port;
    }

    public String getImage() {
        return image;
    }

    public String getDefaultVersion() {
        return defaultVersion;
    }

    public String getPath() {
        return path;
    }

    public int getPort() {
        return port;
    }
}
