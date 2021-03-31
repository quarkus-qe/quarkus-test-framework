package io.quarkus.qe;

import static io.quarkus.test.utils.AwaitilityUtils.untilAsserted;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.ConsulService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.bootstrap.Service;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Container;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
public class GreetingResourceIT {

    private static final String CUSTOM_PROPERTY = "my.property";

    @Container(image = "quay.io/bitnami/consul:1.9.3", expectedLog = "Synced node info", port = 8500)
    static final ConsulService consul = new ConsulService().onPostStart(GreetingResourceIT::onLoadConfigureConsul);

    @QuarkusApplication
    static final RestService app = new RestService().withProperty("quarkus.consul-config.agent.host-port",
            consul::getConsulEndpoint);

    @Test
    public void shouldUpdateCustomProperty() {
        thenGreetingsApiReturns("Hello Default");

        whenUpdateCustomPropertyTo("Test");
        thenGreetingsApiReturns("Hello Test");
    }

    private void whenUpdateCustomPropertyTo(String newValue) {
        consul.loadPropertiesFromString(CUSTOM_PROPERTY + "=" + newValue);

        app.stop();
        app.start();
    }

    private void thenGreetingsApiReturns(String expected) {
        untilAsserted(() -> app.given().get("/api").then().statusCode(HttpStatus.SC_OK).extract().asString(),
                actual -> assertEquals(expected, actual, "Unexpected response from service"));
    }

    private static final void onLoadConfigureConsul(Service service) {
        consul.loadPropertiesFromFile("application.properties");
    }
}
