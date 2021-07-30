package io.quarkus.qe;

import static io.quarkus.test.utils.AwaitilityUtils.untilAsserted;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.ConsulService;
import io.quarkus.test.bootstrap.LookupService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.bootstrap.Service;
import io.quarkus.test.services.QuarkusApplication;

public abstract class BaseGreetingResourceIT {

    private static final String CUSTOM_PROPERTY = "my.property";
    private static final String KEY = "config/examples-consul";

    @LookupService
    static ConsulService consul;

    @QuarkusApplication
    static RestService app = new RestService().withProperty("quarkus.consul-config.agent.host-port",
            () -> consul.getConsulEndpoint());

    @Test
    public void shouldUpdateCustomProperty() {
        thenGreetingsApiReturns("Hello Default");

        whenUpdateCustomPropertyTo("Test");
        thenGreetingsApiReturns("Hello Test");
    }

    protected static final void onLoadConfigureConsul(Service service) {
        consul.loadPropertiesFromFile(KEY, "application.properties");
    }

    private void whenUpdateCustomPropertyTo(String newValue) {
        consul.loadPropertiesFromString(KEY, CUSTOM_PROPERTY + "=" + newValue);

        app.stop();
        app.start();
    }

    private void thenGreetingsApiReturns(String expected) {
        untilAsserted(() -> app.given().get("/api").then().statusCode(HttpStatus.SC_OK).extract().asString(),
                actual -> assertEquals(expected, actual, "Unexpected response from service"));
    }
}
