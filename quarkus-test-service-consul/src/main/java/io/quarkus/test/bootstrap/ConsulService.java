package io.quarkus.test.bootstrap;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

import com.google.common.net.HostAndPort;
import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;

public class ConsulService extends BaseService<ConsulService> {

    public void loadPropertiesFromString(String key, String content) {
        KeyValueClient kvClient = consulClient().keyValueClient();
        kvClient.putValue(key, content);
    }

    public void loadPropertiesFromFile(String key, String file) {
        KeyValueClient kvClient = consulClient().keyValueClient();
        try {
            String properties = IOUtils.toString(
                    ConsulService.class.getClassLoader().getResourceAsStream(file),
                    StandardCharsets.UTF_8);
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
