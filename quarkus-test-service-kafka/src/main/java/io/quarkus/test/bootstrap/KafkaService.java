package io.quarkus.test.bootstrap;

import static java.util.Objects.requireNonNull;

import java.util.Map;

public class KafkaService extends BaseService<KafkaService> {

    public static final String KAFKA_REGISTRY_URL_PROPERTY = "ts.kafka.registry.url";
    public static final String KAFKA_SSL_PROPERTIES = "ts.kafka.ssl.properties";

    public String getBootstrapUrl() {
        var host = getURI();
        return host.getHost() + ":" + host.getPort();
    }

    public String getRegistryUrl() {
        return getPropertyFromContext(KAFKA_REGISTRY_URL_PROPERTY);
    }

    public Map<String, String> getSslProperties() {
        return requireNonNull(getPropertyFromContext(KAFKA_SSL_PROPERTIES));
    }
}
