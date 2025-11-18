package io.quarkus.test.bootstrap;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.kiwiproject.consul.Consul;
import org.kiwiproject.consul.KeyValueClient;

import com.google.common.net.HostAndPort;

public class ConsulService extends BaseService<ConsulService> {

    public void loadPropertiesFromString(String key, String content) {
        KeyValueClient kvClient = consulClient().keyValueClient();
        kvClient.putValue(key, content);
    }

    public void loadPropertiesFromFile(String key, String file) {
        KeyValueClient kvClient = consulClient().keyValueClient();
        try {
            String properties;
            try (var is = ConsulService.class.getClassLoader().getResourceAsStream(file)) {
                Objects.requireNonNull(is, "Failed to find file " + file);
                properties = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
            kvClient.putValue(key, properties);
        } catch (IOException e) {
            fail("Failed to load properties. Caused by " + e.getMessage());
        }
    }

    public Consul consulClient() {
        return Consul.builder()
                .withHostAndPort(
                        HostAndPort.fromString(getConsulEndpoint()))
                .build();
    }

    public String getConsulEndpoint() {
        var host = getURI();
        return host.getHost() + ":" + host.getPort();
    }
}
