package io.quarkus.test.services.containers.model;

import static io.quarkus.test.utils.ImageUtil.getImageName;
import static io.quarkus.test.utils.ImageUtil.getImageVersion;

public enum KafkaRegistry {
    CONFLUENT("kafka.registry.confluent.image", "/", 8081),
    APICURIO("kafka.registry.apicurio.image", "/apis", 8080);

    private final String imageName;
    private final String defaultVersion;
    private final String path;
    private final int port;

    KafkaRegistry(String imagePropertyName, String path, int port) {
        this.imageName = getImageName(imagePropertyName);
        this.defaultVersion = getImageVersion(imagePropertyName);
        this.path = path;
        this.port = port;
    }

    public String getImage() {
        return imageName;
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
