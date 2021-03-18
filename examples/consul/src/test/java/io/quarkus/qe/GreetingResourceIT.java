package io.quarkus.qe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import com.google.common.net.HostAndPort;
import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;

import io.quarkus.test.QuarkusScenario;
import io.quarkus.test.Service;
import io.quarkus.test.annotation.Container;
import io.quarkus.test.annotation.QuarkusApplication;

@QuarkusScenario
public class GreetingResourceIT {

    private static final String CUSTOM_PROPERTY = "my.property";

    @Container(image = "quay.io/bitnami/consul:1.9.3", expectedLog = "Synced node info", port = 8500)
    static final Service consul = new Service("consul").onPostStart(GreetingResourceIT::onLoadConfigureConsul);

    @QuarkusApplication
    static final Service app = new Service("app").withProperty("quarkus.consul-config.agent.host-port",
            GreetingResourceIT::normalizeConsulEndpoint);

    @Test
    public void shouldUpdateCustomProperty() {
        thenGreetingsApiReturns("Hello Default");

        whenUpdateCustomPropertyTo("Test");
        thenGreetingsApiReturns("Hello Test");
    }

    private void whenUpdateCustomPropertyTo(String newValue) {
        KeyValueClient kvClient = consulClient().keyValueClient();
        String properties = CUSTOM_PROPERTY + "=" + newValue;
        kvClient.putValue("config/app", properties);

        app.stop();
        app.start();
    }

    private void thenGreetingsApiReturns(String expected) {
        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            String actual = app.restAssured().get("/api").then().statusCode(HttpStatus.SC_OK).extract().asString();

            assertEquals(expected, actual, "Unexpected response from service");
        });
    }

    private static final void onLoadConfigureConsul(Service service) {
        KeyValueClient kvClient = consulClient().keyValueClient();
        try {
            String properties = IOUtils.toString(
                    GreetingResourceIT.class.getClassLoader().getResourceAsStream("application.properties"),
                    StandardCharsets.UTF_8);
            kvClient.putValue("config/app", properties);
        } catch (IOException e) {
            fail("Failed to load properties. Caused by " + e.getMessage());
        }
    }

    private static final Consul consulClient() {
        return Consul.builder()
                .withHostAndPort(
                        HostAndPort.fromString(normalizeConsulEndpoint()))
                .build();
    }

    private static final String normalizeConsulEndpoint() {
        return consul.getHost().replace("http://", "") + ":" + consul.getPort();
    }
}
