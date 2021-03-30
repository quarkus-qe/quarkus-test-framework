package io.quarkus.test.bootstrap;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

import com.google.common.net.HostAndPort;
import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;

public class ConsulService extends BaseService<ConsulService> {

    private static final String KEY = "config/app";

    public void loadPropertiesFromString(String content) {
        KeyValueClient kvClient = consulClient().keyValueClient();
        kvClient.putValue(KEY, content);
    }

    public void loadPropertiesFromFile(String file) {
        KeyValueClient kvClient = consulClient().keyValueClient();
        try {
            String properties = IOUtils.toString(
                    ConsulService.class.getClassLoader().getResourceAsStream(file),
                    StandardCharsets.UTF_8);
            kvClient.putValue(KEY, properties);
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
        return getHost().replace("http://", "") + ":" + getPort();
    }
}
