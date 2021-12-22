package io.quarkus.test.bootstrap;

public class KafkaService extends BaseService<KafkaService> {

    public static final String KAFKA_REGISTRY_URL_PROPERTY = "ts.kafka.registry.url";

    public String getBootstrapUrl() {
        var host = getURI();
        return host.getHost() + ":" + host.getPort();
    }

    public String getRegistryUrl() {
        return getPropertyFromContext(KAFKA_REGISTRY_URL_PROPERTY);
    }
}
